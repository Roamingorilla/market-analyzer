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
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Portfolios", description = "Portfolio tracking endpoints")
public class PortfolioController {

    @GetMapping
    @Operation(summary = "Get user portfolios")
    public ResponseEntity<ApiResponse<List<Object>>> getPortfolios() {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @PostMapping
    @Operation(summary = "Create a new portfolio")
    public ResponseEntity<ApiResponse<Object>> createPortfolio(@RequestBody Object request) {
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/transactions")
    @Operation(summary = "Add a transaction to a portfolio")
    public ResponseEntity<ApiResponse<Object>> addTransaction(
            @PathVariable Long id,
            @RequestBody Object request
    ) {
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
