package com.marketanalyser.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "stocks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"symbol", "exchange_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(length = 500)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id")
    private Exchange exchange;

    @Column(name = "has_intraday")
    @Builder.Default
    private Boolean hasIntraday = false;

    @Column(name = "has_eod")
    @Builder.Default
    private Boolean hasEod = true;

    @Column(name = "asset_type", length = 50)
    private String assetType;

    @Column(name = "currency_code", length = 10)
    private String currencyCode;

    private String country;

    // Enriched fields (from Alpha Vantage or seed data)
    private String sector;
    private String industry;

    @Column(name = "market_cap")
    private Long marketCap;

    @Column(name = "pe_ratio", precision = 12, scale = 4)
    private BigDecimal peRatio;

    @Column(precision = 12, scale = 4)
    private BigDecimal eps;

    @Column(precision = 8, scale = 4)
    private BigDecimal beta;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "company_website", length = 500)
    private String companyWebsite;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "fundamentals_fetched_at")
    private OffsetDateTime fundamentalsFetchedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
