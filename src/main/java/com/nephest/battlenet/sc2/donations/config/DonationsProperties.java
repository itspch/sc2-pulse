package com.nephest.battlenet.sc2.donations.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "donations")
public class DonationsProperties
{
    private String kofiKey;
    private String patreonKey;
    private String exchangerateApiKey;
    private String exchangerateBaseUrl = "https://v6.exchangerate-api.com/v6";

    public String getKofiKey()
    {
        return kofiKey;
    }

    public void setKofiKey(String kofiKey)
    {
        this.kofiKey = kofiKey;
    }

    public String getPatreonKey()
    {
        return patreonKey;
    }

    public void setPatreonKey(String patreonKey)
    {
        this.patreonKey = patreonKey;
    }

    public String getExchangerateApiKey()
    {
        return exchangerateApiKey;
    }

    public void setExchangerateApiKey(String exchangerateApiKey)
    {
        this.exchangerateApiKey = exchangerateApiKey;
    }

    public String getExchangerateBaseUrl()
    {
        return exchangerateBaseUrl;
    }

    public void setExchangerateBaseUrl(String exchangerateBaseUrl)
    {
        this.exchangerateBaseUrl = exchangerateBaseUrl;
    }
}
