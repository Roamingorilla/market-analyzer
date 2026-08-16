package com.marketanalyser.api.v1.controller;

import com.marketanalyser.api.v1.dto.response.ApiResponse;
import com.marketanalyser.api.v1.dto.response.StockSearchResult;
import com.marketanalyser.domain.model.Stock;
import com.marketanalyser.domain.repository.StockRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/screener")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Screener", description = "Stock screener endpoints")
public class ScreenerController {

    private final StockRepository stockRepository;

    @GetMapping
    @Operation(summary = "Screen stocks by criteria")
    public ResponseEntity<ApiResponse<Page<StockSearchResult>>> screenStocks(
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) String exchange,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Stock> results = stockRepository.findByScreenerCriteria(
                sector,
                exchange,
                country,
                PageRequest.of(page, Math.min(size, 100), Sort.by("symbol").ascending())
        );
        
        Page<StockSearchResult> mapped = results.map(StockSearchResult::from);
        return ResponseEntity.ok(ApiResponse.success(mapped));
    }
}
