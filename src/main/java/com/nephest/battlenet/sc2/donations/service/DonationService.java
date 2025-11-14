package com.nephest.battlenet.sc2.donations.service;

import com.nephest.battlenet.sc2.donations.config.DonationsProperties;
import com.nephest.battlenet.sc2.donations.model.Donation;
import com.nephest.battlenet.sc2.donations.repository.DonationRepository;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DonationService
{
    private static final Logger LOG = LoggerFactory.getLogger(DonationService.class);
    private static final String HEX_CHARS = "0123456789abcdef";

    private final DonationRepository donationRepository;
    private final DonationsProperties donationsProperties;
    private final RestTemplate donationsRestTemplate;

    @Autowired
    public DonationService(
        DonationRepository donationRepository,
        DonationsProperties donationsProperties,
        RestTemplate donationsRestTemplate
    )
    {
        this.donationRepository = donationRepository;
        this.donationsProperties = donationsProperties;
        this.donationsRestTemplate = donationsRestTemplate;
    }

    /**
     * Verifies a Ko-fi webhook using direct key comparison.
     *
     * @param token The token from the webhook payload
     * @return true if verification succeeds, false otherwise
     */
    public boolean verifyKofiWebhook(String token)
    {
        if (token == null || donationsProperties.getKofiKey() == null)
        {
            LOG.warn("Ko-fi verification failed: token or configured key is null");
            return false;
        }
        boolean verified = token.equals(donationsProperties.getKofiKey());
        if (!verified)
        {
            LOG.warn("Ko-fi verification failed: token mismatch");
        }
        return verified;
    }

    /**
     * Verifies a Patreon webhook using HMAC-MD5 signature.
     *
     * @param payload The raw webhook payload (body as string)
     * @param signature The X-Patreon-Signature header value
     * @return true if verification succeeds, false otherwise
     */
    public boolean verifyPatreonWebhook(String payload, String signature)
    {
        if (payload == null || signature == null || donationsProperties.getPatreonKey() == null)
        {
            LOG.warn("Patreon verification failed: payload, signature, or configured key is null");
            return false;
        }

        try
        {
            String computed = computeHmacMd5(payload, donationsProperties.getPatreonKey());
            boolean verified = computed.equals(signature);
            if (!verified)
            {
                LOG.warn("Patreon verification failed: signature mismatch");
            }
            return verified;
        }
        catch (Exception e)
        {
            LOG.error("Patreon verification error", e);
            return false;
        }
    }

    /**
     * Computes HMAC-MD5 signature for Patreon verification.
     *
     * @param data The data to sign
     * @param key The signing key
     * @return Hex-encoded signature
     * @throws Exception if HMAC computation fails
     */
    private String computeHmacMd5(String data, String key)
        throws Exception
    {
        Mac mac = Mac.getInstance("HmacMD5");
        SecretKeySpec keySpec = new SecretKeySpec(
            key.getBytes(StandardCharsets.UTF_8),
            0,
            key.length(),
            "HmacMD5"
        );
        mac.init(keySpec);
        byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(result);
    }

    /**
     * Converts bytes to hex string.
     *
     * @param bytes The bytes to convert
     * @return Hex string representation
     */
    private String bytesToHex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
        {
            sb.append(HEX_CHARS.charAt((b >> 4) & 0xf));
            sb.append(HEX_CHARS.charAt(b & 0xf));
        }
        return sb.toString();
    }

    /**
     * Fetches the exchange rate from the source currency to USD.
     *
     * @param sourceCurrency The source currency code (e.g., "EUR")
     * @return The exchange rate, or null if fetch fails or currency unsupported
     */
    public Double fetchExchangeRate(String sourceCurrency)
    {
        if (sourceCurrency == null || "USD".equalsIgnoreCase(sourceCurrency))
        {
            return 1.0;
        }

        try
        {
            String url = String.format(
                "%s/%s/latest/%s",
                donationsProperties.getExchangerateBaseUrl(),
                donationsProperties.getExchangerateApiKey(),
                sourceCurrency.toUpperCase()
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = donationsRestTemplate.getForObject(url, Map.class);

            if (response == null)
            {
                LOG.warn("Empty response from exchangerate-api for currency: {}", sourceCurrency);
                return null;
            }

            String resultType = (String) response.get("result");
            if (!"success".equals(resultType))
            {
                LOG.warn(
                    "Exchangerate-api error for currency {}: {}",
                    sourceCurrency,
                    response.get("error-type")
                );
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Double> rates = (Map<String, Double>) response.get("conversion_rates");
            if (rates == null)
            {
                LOG.warn("No conversion_rates in response for currency: {}", sourceCurrency);
                return null;
            }

            Double rate = rates.get("USD");
            if (rate == null)
            {
                LOG.warn("USD rate not found in conversion_rates for currency: {}", sourceCurrency);
                return null;
            }

            return rate;
        }
        catch (Exception e)
        {
            LOG.error("Error fetching exchange rate for currency: {}", sourceCurrency, e);
            return null;
        }
    }

    /**
     * Converts an amount from source currency to USD.
     *
     * @param amountCents The amount in cents
     * @param sourceCurrency The source currency code
     * @return The converted amount in USD cents, or null if conversion fails
     */
    public Integer convertToUsd(Integer amountCents, String sourceCurrency)
    {
        if (amountCents == null || amountCents <= 0)
        {
            return null;
        }

        if (sourceCurrency == null || "USD".equalsIgnoreCase(sourceCurrency))
        {
            return amountCents;
        }

        Double rate = fetchExchangeRate(sourceCurrency);
        if (rate == null)
        {
            LOG.warn("Failed to convert {} {}", amountCents, sourceCurrency);
            return null;
        }

        // Convert: amountCents / 100 * rate * 100 = amountCents * rate
        Integer usdCents = Math.round(amountCents * rate.floatValue());
        return usdCents;
    }

    /**
     * Creates and persists a Donation from a Ko-fi webhook payload.
     *
     * @param kofiData The Ko-fi webhook data
     * @return The persisted Donation, or null if conversion fails
     */
    public Donation createDonationFromKofi(Map<String, Object> kofiData)
    {
        try
        {
            String donorName = (String) kofiData.get("from_name");
            Double amountDouble = (Double) kofiData.get("amount");
            String currency = (String) kofiData.get("currency");
            Long timestamp = (Long) kofiData.get("timestamp");

            if (amountDouble == null || currency == null)
            {
                LOG.warn("Missing required Ko-fi fields: amount or currency");
                return null;
            }

            Integer amountCents = Math.round(amountDouble.floatValue() * 100);
            Integer usdAmount = convertToUsd(amountCents, currency);

            if (usdAmount == null)
            {
                LOG.warn("Failed to convert Ko-fi donation amount to USD");
                return null;
            }

            Donation donation = new Donation();
            donation.setCreated(
                timestamp != null
                    ? OffsetDateTime.now()
                    : OffsetDateTime.now()
            );
            donation.setDonorName(donorName);
            donation.setOriginalAmount(amountCents);
            donation.setOriginalCurrency(currency);
            donation.setUsdAmount(usdAmount);

            return donationRepository.save(donation);
        }
        catch (Exception e)
        {
            LOG.error("Error creating donation from Ko-fi payload", e);
            return null;
        }
    }

    /**
     * Creates and persists a Donation from a Patreon webhook payload.
     *
     * @param patreonData The Patreon webhook data
     * @return The persisted Donation, or null if conversion fails
     */
    public Donation createDonationFromPatreon(Map<String, Object> patreonData)
    {
        try
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = (Map<String, Object>) patreonData.get("attributes");
            if (attributes == null)
            {
                LOG.warn("Missing attributes in Patreon payload");
                return null;
            }

            String patronName = (String) attributes.get("patron_name");
            Double amountCents = (Double) attributes.get("amount_cents");
            String currency = (String) attributes.get("currency");

            if (amountCents == null || currency == null)
            {
                LOG.warn("Missing required Patreon fields: amount_cents or currency");
                return null;
            }

            Integer amountCentsInt = Math.round(amountCents.floatValue());
            Integer usdAmount = convertToUsd(amountCentsInt, currency);

            if (usdAmount == null)
            {
                LOG.warn("Failed to convert Patreon donation amount to USD");
                return null;
            }

            Donation donation = new Donation();
            donation.setCreated(OffsetDateTime.now());
            donation.setDonorName(patronName);
            donation.setOriginalAmount(amountCentsInt);
            donation.setOriginalCurrency(currency);
            donation.setUsdAmount(usdAmount);

            return donationRepository.save(donation);
        }
        catch (Exception e)
        {
            LOG.error("Error creating donation from Patreon payload", e);
            return null;
        }
    }
}
