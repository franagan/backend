package com.inversionlibre.backend.controller;

import com.inversionlibre.backend.dto.ApiResponse;
import com.inversionlibre.backend.dto.networth.NetWorthSummaryDTO;
import com.inversionlibre.backend.model.NetWorthCategory;
import com.inversionlibre.backend.model.NetWorthEntry;
import com.inversionlibre.backend.model.User;
import com.inversionlibre.backend.service.NetWorthService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/net-worth")
@RequiredArgsConstructor
public class NetWorthController {

    private final NetWorthService netWorthService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<NetWorthSummaryDTO>> getSummary(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.<NetWorthSummaryDTO>builder()
                .success(true)
                .data(netWorthService.getSummary(user.getId()))
                .build());
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<NetWorthCategory>>> getCategories(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.<List<NetWorthCategory>>builder()
                .success(true)
                .data(netWorthService.getCategories(user.getId()))
                .build());
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<NetWorthCategory>> createCategory(
            @AuthenticationPrincipal User user,
            @RequestBody NetWorthCategory category) {
        return ResponseEntity.ok(ApiResponse.<NetWorthCategory>builder()
                .success(true)
                .data(netWorthService.createCategory(user.getId(), category))
                .build());
    }
    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<NetWorthCategory>> updateCategory(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody NetWorthCategory category) {
        return ResponseEntity.ok(ApiResponse.<NetWorthCategory>builder()
                .success(true)
                .data(netWorthService.updateCategory(user.getId(), id, category))
                .build());
    }

    @PostMapping("/entries")
    public ResponseEntity<ApiResponse<NetWorthEntry>> saveEntry(
            @AuthenticationPrincipal User user,
            @RequestParam String categoryId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(ApiResponse.<NetWorthEntry>builder()
                .success(true)
                .data(netWorthService.saveEntry(user.getId(), categoryId, date, amount, notes))
                .build());
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        netWorthService.deleteCategory(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .build());
    }
}
