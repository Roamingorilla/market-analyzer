package com.marketanalyser.api.v1.controller;

import com.marketanalyser.api.v1.dto.response.ApiResponse;
import com.marketanalyser.domain.repository.ExchangeRepository;
import com.marketanalyser.domain.repository.HistoricalPriceRepository;
import com.marketanalyser.domain.repository.StockRepository;
import com.marketanalyser.service.market.ApiQuotaManager;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Internal monitoring endpoints")
public class AdminController {

    private final ApiQuotaManager quotaManager;
    private final StockRepository stockRepository;
    private final ExchangeRepository exchangeRepository;
    private final HistoricalPriceRepository historicalPriceRepository;

    @GetMapping("/quota")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQuota() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "marketstack", quotaManager.getStatus("marketstack"),
                "alphavantage", quotaManager.getStatus("alphavantage")
        )));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "totalStocks", stockRepository.count(),
                "totalExchanges", exchangeRepository.count(),
                "totalHistoricalBars", historicalPriceRepository.count(),
                "marketstackQuota", quotaManager.getStatus("marketstack"),
                "alphaVantageQuota", quotaManager.getStatus("alphavantage")
        )));
    }
}
