package com.inversionlibre.backend.service.importing;

import com.inversionlibre.backend.model.Transaction;
import com.inversionlibre.backend.model.Stock;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DegiroParser implements BrokerParser {

    @Override
    public boolean canParse(String fileName, String headerLine) {
        if (headerLine == null) return false;
        String normalizedHeader = headerLine.toLowerCase();
        return normalizedHeader.contains("fecha") && normalizedHeader.contains("isin") && normalizedHeader.contains("id orden");
    }

    @Override
    public List<Transaction> parse(InputStream inputStream, String portfolioId, String userId) {
        List<Transaction> transactions = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (parts.length < 10) continue;

                try {
                    String dateStr = clean(parts[0]);
                    String timeStr = clean(parts[1]);
                    String productName = clean(parts[2]);
                    String isin = clean(parts[3]);
                    
                    // Smart Asset Type Detection
                    Stock.AssetType assetType = Stock.AssetType.STOCK;
                    String upperProduct = productName.toUpperCase();
                    if (upperProduct.contains("ETF") || upperProduct.contains("ISHARES") || upperProduct.contains("VANGUARD") || upperProduct.contains("LYXOR")) {
                        assetType = Stock.AssetType.ETF;
                    }
                    
                    String quantityStr = clean(parts[7]); 
                    String priceStr = clean(parts[8]);
                    String totalAmountStr = clean(parts[12]);
                    String orderId = clean(parts[13]);

                    BigDecimal quantity = new BigDecimal(quantityStr.replace(".", "").replace(",", "."));
                    BigDecimal price = new BigDecimal(priceStr.replace(".", "").replace(",", "."));
                    BigDecimal totalAmount = new BigDecimal(totalAmountStr.replace(".", "").replace(",", "."));

                    Transaction.TransactionType type = totalAmount.compareTo(BigDecimal.ZERO) < 0 
                        ? Transaction.TransactionType.BUY 
                        : Transaction.TransactionType.SELL;

                    LocalDate date = LocalDate.parse(dateStr, dateFormatter);
                    LocalTime time = LocalTime.parse(timeStr, timeFormatter);

                    transactions.add(Transaction.builder()
                        .userId(userId)
                        .portfolioId(portfolioId)
                        .stockSymbol(isin)
                        .stockName(productName)
                        .type(type)
                        .status(Transaction.TransactionStatus.EXECUTED)
                        .quantity(quantity.abs())
                        .unitPrice(price.abs())
                        .grossAmount(quantity.multiply(price).abs())
                        .netAmount(totalAmount.abs())
                        .commission(totalAmount.abs().subtract(quantity.multiply(price).abs()))
                        .currency("EUR")
                        .executedAt(LocalDateTime.of(date, time))
                        .brokerName("DEGIRO")
                        .brokerOrderId(orderId)
                        .source("IMPORT_DEGIRO")
                        .assetType(assetType)
                        .build());

                } catch (Exception e) {
                    log.warn("Error parseando línea de Degiro: {}. Error: {}", line, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error leyendo archivo de Degiro", e);
        }

        return transactions;
    }

    private String clean(String s) {
        if (s == null) return "";
        return s.replace("\"", "").trim();
    }
}
