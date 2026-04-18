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
public class RevolutParser implements BrokerParser {

    @Override
    public boolean canParse(String fileName, String headerLine) {
        if (headerLine == null) return false;
        String normalizedHeader = headerLine.toLowerCase();
        return normalizedHeader.contains("ticker") && normalizedHeader.contains("price per share") && normalizedHeader.contains("type");
    }

    @Override
    public List<Transaction> parse(InputStream inputStream, String portfolioId, String userId) {
        List<Transaction> transactions = new ArrayList<>();
        // Revolut format: 2024-03-21 14:05:32
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (parts.length < 8) continue;

                try {
                    // 0:Date, 1:Ticker, 2:Type, 3:Quantity, 4:Price, 5:Total, 6:Currency, 7:Fees
                    String dateStr = clean(parts[0]);
                    String ticker = clean(parts[1]);
                    String typeStr = clean(parts[2]).toUpperCase();
                    String quantityStr = clean(parts[3]);
                    String priceStr = clean(parts[4]);
                    String totalStr = clean(parts[5]);
                    String currency = clean(parts[6]);
                    String feesStr = parts.length > 7 ? clean(parts[7]) : "0";

                    Transaction.TransactionType type;
                    if (typeStr.contains("BUY")) type = Transaction.TransactionType.BUY;
                    else if (typeStr.contains("SELL")) type = Transaction.TransactionType.SELL;
                    else if (typeStr.contains("DIVIDEND")) type = Transaction.TransactionType.DIVIDEND;
                    else continue;

                    BigDecimal quantity = new BigDecimal(quantityStr);
                    BigDecimal price = new BigDecimal(priceStr);
                    BigDecimal netAmount = new BigDecimal(totalStr);
                    BigDecimal fees = new BigDecimal(feesStr);

                    transactions.add(Transaction.builder()
                        .userId(userId)
                        .portfolioId(portfolioId)
                        .stockSymbol(ticker)
                        .stockName(ticker)
                        .type(type)
                        .status(Transaction.TransactionStatus.EXECUTED)
                        .quantity(quantity.abs())
                        .unitPrice(price.abs())
                        .netAmount(netAmount.abs())
                        .fees(fees.abs())
                        .currency(currency)
                        .executedAt(LocalDateTime.parse(dateStr, formatter))
                        .brokerName("REVOLUT")
                        .source("IMPORT_REVOLUT")
                        .assetType(Stock.AssetType.STOCK)
                        .build());

                } catch (Exception e) {
                    log.warn("Error parseando línea de Revolut: {}. Error: {}", line, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error leyendo archivo de Revolut", e);
        }

        return transactions;
    }

    private String clean(String s) {
        if (s == null) return "";
        return s.replace("\"", "").trim();
    }
}
