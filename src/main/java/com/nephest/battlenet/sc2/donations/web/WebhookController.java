package com.nephest.battlenet.sc2.donations.web;

import com.nephest.battlenet.sc2.donations.model.Donation;
import com.nephest.battlenet.sc2.donations.service.DonationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook controller for receiving and processing donations from Ko-fi and Patreon.
 *
 * <p><strong>Ko-fi Webhook:</strong>
 * <ul>
 *   <li>Endpoint: POST /api/webhook/kofi</li>
 *   <li>Authentication: Token in the request body must match donations.kofi.key</li>
 *   <li>Expected fields in payload: from_name, amount, currency, timestamp</li>
 * </ul>
 *
 * <p><strong>Patreon Webhook:</strong>
 * <ul>
 *   <li>Endpoint: POST /api/webhook/patreon</li>
 *   <li>Authentication: X-Patreon-Signature header with HMAC-MD5 signature</li>
 *   <li>Expected fields in payload: attributes.patron_name, attributes.amount_cents, attributes.currency</li>
 * </ul>
 *
 * <p><strong>Configuration Requirements:</strong>
 * <ul>
 *   <li>donations.kofi.key: Ko-fi verification token</li>
 *   <li>donations.patreon.key: Patreon webhook secret for HMAC signature verification</li>
 *   <li>donations.exchangerate-api-key: API key for exchangerate-api.com</li>
 *   <li>donations.exchangerate-base-url: Base URL for exchangerate-api (default:
 *       https://v6.exchangerate-api.com/v6)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/webhook")
public class WebhookController
{
    private static final Logger LOG = LoggerFactory.getLogger(WebhookController.class);

    private final DonationService donationService;

    @Autowired
    public WebhookController(DonationService donationService)
    {
        this.donationService = donationService;
    }

    /**
     * Receives and processes a Ko-fi donation webhook.
     *
     * <p>Expected JSON body format:
     * <pre>
     * {
     *   "token": "ko-fi-verification-token",
     *   "from_name": "Donor Name",
     *   "amount": 5.00,
     *   "currency": "USD",
     *   "timestamp": 1234567890
     * }
     * </pre>
     *
     * @param payload The Ko-fi webhook payload
     * @return 200 OK if successful, 400 Bad Request if verification fails or data is missing
     */
    @PostMapping("/kofi")
    @Operation(
        summary = "Ko-fi Webhook",
        description = "Receives and processes Ko-fi donation notifications",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Webhook processed successfully"
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Verification failed or invalid payload",
                content = @Content()
            )
        }
    )
    public ResponseEntity<Map<String, String>> kofiWebhook(
        @RequestBody Map<String, Object> payload
    )
    {
        try
        {
            String token = (String) payload.get("token");

            if (!donationService.verifyKofiWebhook(token))
            {
                LOG.warn("Ko-fi webhook verification failed");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Verification failed"));
            }

            Donation donation = donationService.createDonationFromKofi(payload);
            if (donation == null)
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to process donation"));
            }

            LOG.info("Ko-fi donation processed: id={}, amount={}", donation.getId(), donation.getUsdAmount());
            return ResponseEntity.ok(Map.of("status", "success", "id", donation.getId().toString()));
        }
        catch (Exception e)
        {
            LOG.error("Error processing Ko-fi webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Receives and processes a Patreon donation webhook.
     *
     * <p>Expected JSON body format:
     * <pre>
     * {
     *   "data": {
     *     "attributes": {
     *       "patron_name": "Patron Name",
     *       "amount_cents": 500,
     *       "currency": "USD"
     *     }
     *   }
     * }
     * </pre>
     *
     * <p>The X-Patreon-Signature header must contain the HMAC-MD5 signature of the raw request body.
     *
     * @param payload The Patreon webhook payload
     * @param signature The X-Patreon-Signature header value
     * @return 200 OK if successful, 400 Bad Request if verification fails or data is missing
     */
    @PostMapping("/patreon")
    @Operation(
        summary = "Patreon Webhook",
        description = "Receives and processes Patreon donation notifications",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Webhook processed successfully"
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Verification failed or invalid payload",
                content = @Content()
            )
        }
    )
    public ResponseEntity<Map<String, String>> patreonWebhook(
        @RequestBody String rawPayload,
        @RequestHeader(value = "X-Patreon-Signature", required = false) String signature
    )
    {
        try
        {
            if (!donationService.verifyPatreonWebhook(rawPayload, signature))
            {
                LOG.warn("Patreon webhook verification failed");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Verification failed"));
            }

            // Parse JSON manually since we already have the raw payload
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> jsonPayload = mapper.readValue(rawPayload, Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) jsonPayload.get("data");
            if (data == null)
            {
                LOG.warn("Missing 'data' field in Patreon payload");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid payload structure"));
            }

            Donation donation = donationService.createDonationFromPatreon(data);
            if (donation == null)
            {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to process donation"));
            }

            LOG.info("Patreon donation processed: id={}, amount={}", donation.getId(), donation.getUsdAmount());
            return ResponseEntity.ok(Map.of("status", "success", "id", donation.getId().toString()));
        }
        catch (Exception e)
        {
            LOG.error("Error processing Patreon webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Internal server error"));
        }
    }
}
