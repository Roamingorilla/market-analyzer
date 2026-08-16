package com.marketanalyser.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exchanges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Exchange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String mic;

    @Column(nullable = false)
    private String name;

    @Column(length = 20)
    private String acronym;

    private String country;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    private String city;

    private String website;

    private String timezone;

    @Column(name = "currency_code", length = 10)
    private String currencyCode;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "exchange", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Stock> stocks = new ArrayList<>();
}
