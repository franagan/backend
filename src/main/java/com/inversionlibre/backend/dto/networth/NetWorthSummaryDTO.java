package com.inversionlibre.backend.dto.networth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetWorthSummaryDTO {
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal netWorth;
    private BigDecimal monthlyChange;
    private BigDecimal monthlyChangePercent;
    
    private List<HistoryEntry> history;
    private Map<String, BigDecimal> assetsDistribution;
    private Map<String, BigDecimal> liabilitiesDistribution;
    private Map<String, BigDecimal> categoryBalances; // Map of categoryId -> amount

    @Data
    @AllArgsConstructor
    public static class HistoryEntry {
        private String date; // YYYY-MM
        private BigDecimal assets;
        private BigDecimal liabilities;
        private BigDecimal netWorth;
    }
}
