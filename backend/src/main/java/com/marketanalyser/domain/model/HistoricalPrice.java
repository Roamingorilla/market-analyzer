package com.marketanalyser.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "historical_prices",
    uniqueConstraints = @UniqueConstraint(columnNames = {"stock_id", "date"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistoricalPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private LocalDate date;

    @Column(precision = 18, scale = 6)
    private BigDecimal open;

    @Column(precision = 18, scale = 6)
    private BigDecimal high;

    @Column(precision = 18, scale = 6)
    private BigDecimal low;

    @Column(precision = 18, scale = 6)
    private BigDecimal close;

    @Column(name = "adj_open", precision = 18, scale = 6)
    private BigDecimal adjOpen;

    @Column(name = "adj_high", precision = 18, scale = 6)
    private BigDecimal adjHigh;

    @Column(name = "adj_low", precision = 18, scale = 6)
    private BigDecimal adjLow;

    @Column(name = "adj_close", precision = 18, scale = 6)
    private BigDecimal adjClose;

    private Long volume;

    @Column(name = "adj_volume")
    private Long adjVolume;

    @Column(name = "split_factor", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal splitFactor = BigDecimal.ONE;

    @Column(precision = 12, scale = 6)
    @Builder.Default
    private BigDecimal dividend = BigDecimal.ZERO;

    @Column(name = "data_source", length = 50)
    @Builder.Default
    private String dataSource = "marketstack";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
