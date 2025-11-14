package com.nephest.battlenet.sc2.donations.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nephest.battlenet.sc2.donations.model.Donation;
import com.nephest.battlenet.sc2.donations.service.DonationService;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WebhookController.class)
class WebhookControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DonationService donationService;

    private Donation mockDonation;

    @BeforeEach
    void setUp()
    {
        mockDonation = new Donation();
        mockDonation.setId(1L);
        mockDonation.setCreated(OffsetDateTime.now());
        mockDonation.setDonorName("Test Donor");
        mockDonation.setOriginalAmount(500);
        mockDonation.setOriginalCurrency("USD");
        mockDonation.setUsdAmount(500);
    }

    // Ko-fi Webhook Tests
    @Test
    void testKofiWebhookSuccess()
        throws Exception
    {
        Map<String, Object> payload = new HashMap<>();
        payload.put("token", "test-token");
        payload.put("from_name", "Test Donor");
        payload.put("amount", 5.0);
        payload.put("currency", "USD");

        when(donationService.verifyKofiWebhook("test-token")).thenReturn(true);
        when(donationService.createDonationFromKofi(any(Map.class))).thenReturn(mockDonation);

        mockMvc.perform(
            post("/api/webhook/kofi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.id").value("1"));

        verify(donationService, times(1)).verifyKofiWebhook("test-token");
        verify(donationService, times(1)).createDonationFromKofi(any(Map.class));
    }

    @Test
    void testKofiWebhookVerificationFailure()
        throws Exception
    {
        Map<String, Object> payload = new HashMap<>();
        payload.put("token", "wrong-token");
        payload.put("from_name", "Test Donor");
        payload.put("amount", 5.0);
        payload.put("currency", "USD");

        when(donationService.verifyKofiWebhook("wrong-token")).thenReturn(false);

        mockMvc.perform(
            post("/api/webhook/kofi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Verification failed"));

        verify(donationService, times(1)).verifyKofiWebhook("wrong-token");
        verify(donationService, never()).createDonationFromKofi(any(Map.class));
    }

    @Test
    void testKofiWebhookProcessingFailure()
        throws Exception
    {
        Map<String, Object> payload = new HashMap<>();
        payload.put("token", "test-token");
        payload.put("from_name", "Test Donor");
        payload.put("amount", 5.0);
        payload.put("currency", "UNSUPPORTED");

        when(donationService.verifyKofiWebhook("test-token")).thenReturn(true);
        when(donationService.createDonationFromKofi(any(Map.class))).thenReturn(null);

        mockMvc.perform(
            post("/api/webhook/kofi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Failed to process donation"));

        verify(donationService, times(1)).verifyKofiWebhook("test-token");
        verify(donationService, times(1)).createDonationFromKofi(any(Map.class));
    }

    @Test
    void testKofiWebhookMissingToken()
        throws Exception
    {
        Map<String, Object> payload = new HashMap<>();
        payload.put("from_name", "Test Donor");
        payload.put("amount", 5.0);
        payload.put("currency", "USD");

        when(donationService.verifyKofiWebhook(null)).thenReturn(false);

        mockMvc.perform(
            post("/api/webhook/kofi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Verification failed"));
    }

    // Patreon Webhook Tests
    @Test
    void testPatreonWebhookSuccess()
        throws Exception
    {
        String rawPayload = "{\"data\":{\"attributes\":{\"patron_name\":\"Test Patron\",\"amount_cents\":500,\"currency\":\"USD\"}}}";

        when(donationService.verifyPatreonWebhook(rawPayload, "test-signature")).thenReturn(true);
        when(donationService.createDonationFromPatreon(any(Map.class))).thenReturn(mockDonation);

        mockMvc.perform(
            post("/api/webhook/patreon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawPayload)
                .header("X-Patreon-Signature", "test-signature")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.id").value("1"));

        verify(donationService, times(1)).verifyPatreonWebhook(rawPayload, "test-signature");
        verify(donationService, times(1)).createDonationFromPatreon(any(Map.class));
    }

    @Test
    void testPatreonWebhookVerificationFailure()
        throws Exception
    {
        String rawPayload = "{\"data\":{\"attributes\":{\"patron_name\":\"Test Patron\",\"amount_cents\":500,\"currency\":\"USD\"}}}";

        when(donationService.verifyPatreonWebhook(rawPayload, "wrong-signature")).thenReturn(false);

        mockMvc.perform(
            post("/api/webhook/patreon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawPayload)
                .header("X-Patreon-Signature", "wrong-signature")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Verification failed"));

        verify(donationService, times(1)).verifyPatreonWebhook(rawPayload, "wrong-signature");
        verify(donationService, never()).createDonationFromPatreon(any(Map.class));
    }

    @Test
    void testPatreonWebhookMissingSignature()
        throws Exception
    {
        String rawPayload = "{\"data\":{\"attributes\":{\"patron_name\":\"Test Patron\",\"amount_cents\":500,\"currency\":\"USD\"}}}";

        when(donationService.verifyPatreonWebhook(rawPayload, null)).thenReturn(false);

        mockMvc.perform(
            post("/api/webhook/patreon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawPayload)
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Verification failed"));

        verify(donationService, times(1)).verifyPatreonWebhook(rawPayload, null);
    }

    @Test
    void testPatreonWebhookProcessingFailure()
        throws Exception
    {
        String rawPayload = "{\"data\":{\"attributes\":{\"patron_name\":\"Test Patron\",\"amount_cents\":500,\"currency\":\"UNSUPPORTED\"}}}";

        when(donationService.verifyPatreonWebhook(rawPayload, "test-signature")).thenReturn(true);
        when(donationService.createDonationFromPatreon(any(Map.class))).thenReturn(null);

        mockMvc.perform(
            post("/api/webhook/patreon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawPayload)
                .header("X-Patreon-Signature", "test-signature")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Failed to process donation"));

        verify(donationService, times(1)).verifyPatreonWebhook(rawPayload, "test-signature");
        verify(donationService, times(1)).createDonationFromPatreon(any(Map.class));
    }

    @Test
    void testPatreonWebhookInvalidJsonStructure()
        throws Exception
    {
        String rawPayload = "{\"invalid_field\":{\"attributes\":{\"patron_name\":\"Test Patron\"}}}";

        when(donationService.verifyPatreonWebhook(rawPayload, "test-signature")).thenReturn(true);

        mockMvc.perform(
            post("/api/webhook/patreon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawPayload)
                .header("X-Patreon-Signature", "test-signature")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid payload structure"));

        verify(donationService, never()).createDonationFromPatreon(any(Map.class));
    }

    @Test
    void testPatreonWebhookMalformedJson()
        throws Exception
    {
        String malformedPayload = "{invalid json}";

        when(donationService.verifyPatreonWebhook(malformedPayload, "test-signature")).thenReturn(true);

        mockMvc.perform(
            post("/api/webhook/patreon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedPayload)
                .header("X-Patreon-Signature", "test-signature")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Internal server error"));
    }
}
