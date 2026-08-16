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
@RequestMapping("/api/v1/annotations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Annotations", description = "Chart drawing annotations endpoints")
public class AnnotationController {

    @GetMapping("/{symbol}")
    @Operation(summary = "Get chart annotations for a stock")
    public ResponseEntity<ApiResponse<List<Object>>> getAnnotations(@PathVariable String symbol) {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @PostMapping("/{symbol}")
    @Operation(summary = "Save a chart annotation")
    public ResponseEntity<ApiResponse<Object>> saveAnnotation(
            @PathVariable String symbol,
            @RequestBody Object request
    ) {
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
