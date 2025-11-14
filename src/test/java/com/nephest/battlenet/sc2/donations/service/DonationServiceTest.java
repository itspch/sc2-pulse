package com.nephest.battlenet.sc2.donations.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nephest.battlenet.sc2.donations.config.DonationsProperties;
import com.nephest.battlenet.sc2.donations.model.Donation;
import com.nephest.battlenet.sc2.donations.repository.DonationRepository;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest
{
    @Mock
    private DonationRepository donationRepository;

    @Mock
    private DonationsProperties donationsProperties;

    @Mock
    private RestTemplate donationsRestTemplate;

    @InjectMocks
    private DonationService donationService;

    @BeforeEach
    void setUp()
    {
        when(donationsProperties.getKofiKey()).thenReturn("test-kofi-key");
        when(donationsProperties.getPatreonKey()).thenReturn("test-patreon-key");
        when(donationsProperties.getExchangerateApiKey()).thenReturn("test-api-key");
        when(donationsProperties.getExchangerateBaseUrl()).thenReturn(
            "https://v6.exchangerate-api.com/v6"
        );
    }

    // Ko-fi Verification Tests
    @Test
    void testVerifyKofiWebhookSuccess()
    {
        assertTrue(donationService.verifyKofiWebhook("test-kofi-key"));
    }

    @Test
    void testVerifyKofiWebhookFailureWrongToken()
    {
        assertFalse(donationService.verifyKofiWebhook("wrong-token"));
    }

    @Test
    void testVerifyKofiWebhookFailureNullToken()
    {
        assertFalse(donationService.verifyKofiWebhook(null));
    }

    @Test
    void testVerifyKofiWebhookFailureNullKey()
    {
        when(donationsProperties.getKofiKey()).thenReturn(null);
        assertFalse(donationService.verifyKofiWebhook("test-token"));
    }

    // Patreon Verification Tests
    @Test
    void testVerifyPatreonWebhookSuccess()
    {
        String payload = "test-payload";
        // Pre-computed HMAC-MD5 of "test-payload" with key "test-patreon-key"
        String expectedSignature = "7f1e3b8c8c8c8c8c8c8c8c8c8c8c8c8c";

        // For this test, we'll compute it
        try
        {
            java.nio.charset.StandardCharsets.UTF_8.name();
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacMD5");
            javax.crypto.spec.SecretKeySpec keySpec =
                new javax.crypto.spec.SecretKeySpec(
                    "test-patreon-key".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    0,
                    "test-patreon-key".length(),
                    "HmacMD5"
                );
            mac.init(keySpec);
            byte[] result = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String computedSignature = bytesToHex(result);

            assertTrue(donationService.verifyPatreonWebhook(payload, computedSignature));
        }
        catch (Exception e)
        {
            fail("Test setup failed: " + e.getMessage());
        }
    }

    @Test
    void testVerifyPatreonWebhookFailureWrongSignature()
    {
        String payload = "test-payload";
        String wrongSignature = "0000000000000000000000000000000000";

        assertFalse(donationService.verifyPatreonWebhook(payload, wrongSignature));
    }

    @Test
    void testVerifyPatreonWebhookFailureNullPayload()
    {
        assertFalse(donationService.verifyPatreonWebhook(null, "signature"));
    }

    @Test
    void testVerifyPatreonWebhookFailureNullSignature()
    {
        assertFalse(donationService.verifyPatreonWebhook("payload", null));
    }

    @Test
    void testVerifyPatreonWebhookFailureNullKey()
    {
        when(donationsProperties.getPatreonKey()).thenReturn(null);
        assertFalse(donationService.verifyPatreonWebhook("payload", "signature"));
    }

    // Exchange Rate Tests
    @Test
    void testFetchExchangeRateUSD()
    {
        // USD should always return 1.0
        Double rate = donationService.fetchExchangeRate("USD");
        assertEquals(1.0, rate);
    }

    @Test
    void testFetchExchangeRateNull()
    {
        Double rate = donationService.fetchExchangeRate(null);
        assertEquals(1.0, rate);
    }

    @Test
    void testFetchExchangeRateSuccess()
    {
        Map<String, Object> response = new HashMap<>();
        response.put("result", "success");
        Map<String, Double> rates = new HashMap<>();
        rates.put("USD", 1.1);
        response.put("conversion_rates", rates);

        when(donationsRestTemplate.getForObject(
            "https://v6.exchangerate-api.com/v6/test-api-key/latest/EUR",
            Map.class
        )).thenReturn(response);

        Double rate = donationService.fetchExchangeRate("EUR");
        assertEquals(1.1, rate);
    }

    @Test
    void testFetchExchangeRateFailureUnsupportedCurrency()
    {
        Map<String, Object> response = new HashMap<>();
        response.put("result", "error");
        response.put("error-type", "unsupported-code");

        when(donationsRestTemplate.getForObject(
            anyString(),
            eq(Map.class)
        )).thenReturn(response);

        Double rate = donationService.fetchExchangeRate("XYZ");
        assertNull(rate);
    }

    @Test
    void testFetchExchangeRateFailureNullResponse()
    {
        when(donationsRestTemplate.getForObject(
            anyString(),
            eq(Map.class)
        )).thenReturn(null);

        Double rate = donationService.fetchExchangeRate("EUR");
        assertNull(rate);
    }

    @Test
    void testFetchExchangeRateFailureException()
    {
        when(donationsRestTemplate.getForObject(
            anyString(),
            eq(Map.class)
        )).thenThrow(new RuntimeException("Network error"));

        Double rate = donationService.fetchExchangeRate("EUR");
        assertNull(rate);
    }

    // Currency Conversion Tests
    @Test
    void testConvertToUsdUSD()
    {
        Integer usd = donationService.convertToUsd(500, "USD");
        assertEquals(500, usd);
    }

    @Test
    void testConvertToUsdNull()
    {
        Integer usd = donationService.convertToUsd(500, null);
        assertEquals(500, usd);
    }

    @Test
    void testConvertToUsdInvalidAmount()
    {
        Integer usd = donationService.convertToUsd(0, "EUR");
        assertNull(usd);

        usd = donationService.convertToUsd(-100, "EUR");
        assertNull(usd);

        usd = donationService.convertToUsd(null, "EUR");
        assertNull(usd);
    }

    @Test
    void testConvertToUsdSuccess()
    {
        Map<String, Object> response = new HashMap<>();
        response.put("result", "success");
        Map<String, Double> rates = new HashMap<>();
        rates.put("USD", 1.1);
        response.put("conversion_rates", rates);

        when(donationsRestTemplate.getForObject(
            "https://v6.exchangerate-api.com/v6/test-api-key/latest/EUR",
            Map.class
        )).thenReturn(response);

        Integer usd = donationService.convertToUsd(1000, "EUR");
        assertEquals(1100, usd);
    }

    // Ko-fi Donation Creation Tests
    @Test
    void testCreateDonationFromKofiSuccess()
    {
        Map<String, Object> kofiData = new HashMap<>();
        kofiData.put("from_name", "John Donor");
        kofiData.put("amount", 5.0);
        kofiData.put("currency", "USD");
        kofiData.put("timestamp", 1234567890L);

        Map<String, Object> ratesResponse = new HashMap<>();
        ratesResponse.put("result", "success");
        Map<String, Double> rates = new HashMap<>();
        rates.put("USD", 1.0);
        ratesResponse.put("conversion_rates", rates);

        when(donationsRestTemplate.getForObject(
            anyString(),
            eq(Map.class)
        )).thenReturn(ratesResponse);

        Donation mockDonation = new Donation();
        mockDonation.setId(1L);
        mockDonation.setDonorName("John Donor");
        mockDonation.setOriginalAmount(500);
        mockDonation.setOriginalCurrency("USD");
        mockDonation.setUsdAmount(500);

        when(donationRepository.save(any(Donation.class))).thenReturn(mockDonation);

        Donation result = donationService.createDonationFromKofi(kofiData);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John Donor", result.getDonorName());
        assertEquals(500, result.getOriginalAmount());
        assertEquals(500, result.getUsdAmount());

        verify(donationRepository, times(1)).save(any(Donation.class));
    }

    @Test
    void testCreateDonationFromKofiFailureMissingAmount()
    {
        Map<String, Object> kofiData = new HashMap<>();
        kofiData.put("from_name", "John Donor");
        kofiData.put("currency", "USD");

        Donation result = donationService.createDonationFromKofi(kofiData);

        assertNull(result);
        verify(donationRepository, never()).save(any(Donation.class));
    }

    @Test
    void testCreateDonationFromKofiFailureMissingCurrency()
    {
        Map<String, Object> kofiData = new HashMap<>();
        kofiData.put("from_name", "John Donor");
        kofiData.put("amount", 5.0);

        Donation result = donationService.createDonationFromKofi(kofiData);

        assertNull(result);
        verify(donationRepository, never()).save(any(Donation.class));
    }

    @Test
    void testCreateDonationFromKofiFailureConversionFails()
    {
        Map<String, Object> kofiData = new HashMap<>();
        kofiData.put("from_name", "John Donor");
        kofiData.put("amount", 5.0);
        kofiData.put("currency", "XXX");

        when(donationsRestTemplate.getForObject(
            anyString(),
            eq(Map.class)
        )).thenThrow(new RuntimeException("API Error"));

        Donation result = donationService.createDonationFromKofi(kofiData);

        assertNull(result);
        verify(donationRepository, never()).save(any(Donation.class));
    }

    // Patreon Donation Creation Tests
    @Test
    void testCreateDonationFromPatreonSuccess()
    {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("patron_name", "Jane Patron");
        attributes.put("amount_cents", 500.0);
        attributes.put("currency", "USD");

        Map<String, Object> patreonData = new HashMap<>();
        patreonData.put("attributes", attributes);

        Map<String, Object> ratesResponse = new HashMap<>();
        ratesResponse.put("result", "success");
        Map<String, Double> rates = new HashMap<>();
        rates.put("USD", 1.0);
        ratesResponse.put("conversion_rates", rates);

        when(donationsRestTemplate.getForObject(
            anyString(),
            eq(Map.class)
        )).thenReturn(ratesResponse);

        Donation mockDonation = new Donation();
        mockDonation.setId(2L);
        mockDonation.setDonorName("Jane Patron");
        mockDonation.setOriginalAmount(500);
        mockDonation.setOriginalCurrency("USD");
        mockDonation.setUsdAmount(500);

        when(donationRepository.save(any(Donation.class))).thenReturn(mockDonation);

        Donation result = donationService.createDonationFromPatreon(patreonData);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("Jane Patron", result.getDonorName());
        assertEquals(500, result.getOriginalAmount());
        assertEquals(500, result.getUsdAmount());

        verify(donationRepository, times(1)).save(any(Donation.class));
    }

    @Test
    void testCreateDonationFromPatreonFailureMissingAttributes()
    {
        Map<String, Object> patreonData = new HashMap<>();

        Donation result = donationService.createDonationFromPatreon(patreonData);

        assertNull(result);
        verify(donationRepository, never()).save(any(Donation.class));
    }

    @Test
    void testCreateDonationFromPatreonFailureMissingAmountCents()
    {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("patron_name", "Jane Patron");
        attributes.put("currency", "USD");

        Map<String, Object> patreonData = new HashMap<>();
        patreonData.put("attributes", attributes);

        Donation result = donationService.createDonationFromPatreon(patreonData);

        assertNull(result);
        verify(donationRepository, never()).save(any(Donation.class));
    }

    // Helper method
    private String bytesToHex(byte[] bytes)
    {
        String HEX_CHARS = "0123456789abcdef";
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
        {
            sb.append(HEX_CHARS.charAt((b >> 4) & 0xf));
            sb.append(HEX_CHARS.charAt(b & 0xf));
        }
        return sb.toString();
    }
}
