package com.marketanalyser.api.v1.dto.response;

import com.marketanalyser.service.market.ApiQuotaManager;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data @Builder
public class DataFreshnessResponse {
    private String symbol;
    private OffsetDateTime lastUpdated;
    private long totalBars;
    private boolean hasIntraday;
    private boolean hasEod;
    private ApiQuotaManager.QuotaStatus quotaStatus;
}
