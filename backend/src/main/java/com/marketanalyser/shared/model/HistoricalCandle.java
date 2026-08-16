package com.marketanalyser.shared.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Normalized internal model — decoupled from Marketstack response format.
 * All providers must map their data to this model.
 */
@Data
@Builder
public class HistoricalCandle {
    private LocalDate date;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal adjOpen;
    private BigDecimal adjHigh;
    private BigDecimal adjLow;
    private BigDecimal adjClose;
    private Long volume;
    private Long adjVolume;
    private BigDecimal splitFactor;
    private BigDecimal dividend;
    private String symbol;
    private String exchange;
    private String dataSource;
    private OffsetDateTime fetchedAt;
}
