package com.marketanalyser.api.v1.controller;

import com.marketanalyser.api.v1.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/watchlists")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Watchlists", description = "Watchlist management endpoints")
public class WatchlistController {

    @GetMapping
    @Operation(summary = "Get user watchlists")
    public ResponseEntity<ApiResponse<List<Object>>> getWatchlists() {
        // Phase 1: Public mock response until auth is implemented
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @PostMapping
    @Operation(summary = "Create a new watchlist")
    public ResponseEntity<ApiResponse<Object>> createWatchlist(@RequestBody Object request) {
        // Phase 1: Public mock response
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/stocks/{symbol}")
    @Operation(summary = "Add a stock to a watchlist")
    public ResponseEntity<ApiResponse<Object>> addStockToWatchlist(
            @PathVariable Long id,
            @PathVariable String symbol
    ) {
        // Phase 1: Public mock response
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}/stocks/{symbol}")
    @Operation(summary = "Remove a stock from a watchlist")
    public ResponseEntity<ApiResponse<Object>> removeStockFromWatchlist(
            @PathVariable Long id,
            @PathVariable String symbol
    ) {
        // Phase 1: Public mock response
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
