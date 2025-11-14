package com.nephest.battlenet.sc2.donations.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "donation")
public class Donation
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created", nullable = false)
    private OffsetDateTime created;

    @Column(name = "donor_name")
    private String donorName;

    /** Original amount in cents (integer) */
    @Column(name = "original_amount")
    private Integer originalAmount;

    @Column(name = "original_currency")
    private String originalCurrency;

    /** USD amount in cents (integer) */
    @Column(name = "usd_amount")
    private Integer usdAmount;

    public Donation() {}

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public OffsetDateTime getCreated()
    {
        return created;
    }

    public void setCreated(OffsetDateTime created)
    {
        this.created = created;
    }

    public String getDonorName()
    {
        return donorName;
    }

    public void setDonorName(String donorName)
    {
        this.donorName = donorName;
    }

    public Integer getOriginalAmount()
    {
        return originalAmount;
    }

    public void setOriginalAmount(Integer originalAmount)
    {
        this.originalAmount = originalAmount;
    }

    public String getOriginalCurrency()
    {
        return originalCurrency;
    }

    public void setOriginalCurrency(String originalCurrency)
    {
        this.originalCurrency = originalCurrency;
    }

    public Integer getUsdAmount()
    {
        return usdAmount;
    }

    public void setUsdAmount(Integer usdAmount)
    {
        this.usdAmount = usdAmount;
    }

}
