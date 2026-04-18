package com.inversionlibre.backend.service.importing;

import com.inversionlibre.backend.model.Transaction;
import com.inversionlibre.backend.model.Stock;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class IbkrParser implements BrokerParser {

    @Override
    public boolean canParse(String fileName, String headerLine) {
        if (headerLine == null) return false;
        String normalizedHeader = headerLine.toLowerCase();
        return normalizedHeader.contains("discriminator") || normalizedHeader.contains("ibkr") || normalizedHeader.contains("trades");
    }

    @Override
    public List<Transaction> parse(InputStream inputStream, String portfolioId, String userId) {
        List<Transaction> transactions = new ArrayList<>();
        // IBKR format: 2024-05-15, 14:30:00 or similar
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Trades") && line.contains("Data,Order")) {
                    String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    if (parts.length < 15) continue;

                    try {
                        // IBKR structure varies. This is a common one for 'Trades' export
                        // Symbol is usually at index 5 or 6
                        String symbol = clean(parts[5]);
                        String assetClass = clean(parts[3]); // Usually index 3 or 4
                        String dateTimeStr = clean(parts[6]);
                        
                        Stock.AssetType assetType = Stock.AssetType.STOCK;
                        if (assetClass.contains("STK")) assetType = Stock.AssetType.STOCK;
                        else if (assetClass.contains("OPT")) assetType = Stock.AssetType.STOCK; // Map options to stock context for now
                        else if (assetClass.contains("BOND")) assetType = Stock.AssetType.STOCK;
                        
                        String quantityStr = clean(parts[7]);
                        String priceStr = clean(parts[8]);
                        String commStr = clean(parts[11]);
                        String currency = clean(parts[10]);

                        BigDecimal quantity = new BigDecimal(quantityStr);
                        BigDecimal price = new BigDecimal(priceStr);
                        BigDecimal comm = new BigDecimal(commStr).abs();

                        Transaction.TransactionType type = quantity.compareTo(BigDecimal.ZERO) > 0 
                            ? Transaction.TransactionType.BUY 
                            : Transaction.TransactionType.SELL;

                        transactions.add(Transaction.builder()
                            .userId(userId)
                            .portfolioId(portfolioId)
                            .stockSymbol(symbol)
                            .stockName(symbol)
                            .type(type)
                            .status(Transaction.TransactionStatus.EXECUTED)
                            .quantity(quantity.abs())
                            .unitPrice(price.abs())
                            .commission(comm)
                            .currency(currency)
                            .executedAt(LocalDateTime.parse(dateTimeStr, formatter))
                            .brokerName("IBKR")
                            .source("IMPORT_IBKR")
                            .assetType(assetType)
                            .build());

                    } catch (Exception e) {
                        log.warn("Error parseando línea de IBKR: {}. Error: {}", line, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error leyendo archivo de IBKR", e);
        }

        return transactions;
    }

    private String clean(String s) {
        if (s == null) return "";
        return s.replace("\"", "").trim();
    }
}
