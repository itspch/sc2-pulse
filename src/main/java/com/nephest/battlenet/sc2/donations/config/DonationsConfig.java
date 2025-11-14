package com.nephest.battlenet.sc2.donations.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class DonationsConfig
{
    @Bean
    public RestTemplate donationsRestTemplate(RestTemplateBuilder builder)
    {
        return builder.build();
    }
}
