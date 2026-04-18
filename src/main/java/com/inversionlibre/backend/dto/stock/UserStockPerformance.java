package com.inversionlibre.backend.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import com.inversionlibre.backend.model.Transaction;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStockPerformance {
    private BigDecimal totalValue;
    private BigDecimal investedAmount;
    private BigDecimal priceGain;
    private BigDecimal priceGainPercent;
    private BigDecimal realizedGain;
    private BigDecimal totalReturn;
    private BigDecimal averageBuyPrice;
    private Double quantity;
    private BigDecimal totalDividends;
    private List<Transaction> latestTransactions;
}
