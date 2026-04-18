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
public class TradeRepublicParser implements BrokerParser {

    @Override
    public boolean canParse(String fileName, String headerLine) {
        if (headerLine == null) return false;
        String normalizedHeader = headerLine.toLowerCase();
        return normalizedHeader.contains("isin") && normalizedHeader.contains("betrag") && normalizedHeader.contains("gebühr");
    }

    @Override
    public List<Transaction> parse(InputStream inputStream, String portfolioId, String userId) {
        List<Transaction> transactions = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] parts = line.split(";");
                if (parts.length < 10) continue;

                try {
                    // 0:Datum, 1:Zeit, 2:ISIN, 4:Typ, 5:Anzahl, 6:Kurs, 10:Gebühr, 14:Summe, 7:Währung
                    String dateStr = clean(parts[0]);
                    String timeStr = parts.length > 1 ? clean(parts[1]) : "12:00:00";
                    String isin = clean(parts[2]);
                    String name = clean(parts[3]);
                    String typeStr = clean(parts[4]).toLowerCase();
                    String quantityStr = clean(parts[5]);
                    
                    Stock.AssetType assetType = Stock.AssetType.STOCK;
                    if (name.toUpperCase().contains("BITCOIN") || name.toUpperCase().contains("ETHEREUM") || name.toUpperCase().contains("CRYPTO")) {
                        assetType = Stock.AssetType.CRYPTO;
                    } else if (name.toUpperCase().contains("ETF") || name.toUpperCase().contains("ISHARES")) {
                        assetType = Stock.AssetType.ETF;
                    }
                    String priceStr = clean(parts[6]);
                    String feeStr = parts.length > 10 ? clean(parts[10]) : "0";
                    String totalStr = parts.length > 14 ? clean(parts[14]) : "0";
                    String currency = parts.length > 7 ? clean(parts[7]) : "EUR";

                    Transaction.TransactionType type;
                    if (typeStr.contains("kauf") || typeStr.contains("buy")) type = Transaction.TransactionType.BUY;
                    else if (typeStr.contains("verkauf") || typeStr.contains("sell")) type = Transaction.TransactionType.SELL;
                    else if (typeStr.contains("dividende")) type = Transaction.TransactionType.DIVIDEND;
                    else continue;

                    BigDecimal quantity = new BigDecimal(quantityStr.replace(",", "."));
                    BigDecimal price = new BigDecimal(priceStr.replace(",", "."));
                    BigDecimal fees = new BigDecimal(feeStr.replace(",", "."));
                    BigDecimal netAmount = new BigDecimal(totalStr.replace(",", "."));

                    transactions.add(Transaction.builder()
                        .userId(userId)
                        .portfolioId(portfolioId)
                        .stockSymbol(isin)
                        .stockName(name)
                        .type(type)
                        .status(Transaction.TransactionStatus.EXECUTED)
                        .quantity(quantity.abs())
                        .unitPrice(price.abs())
                        .netAmount(netAmount.abs())
                        .fees(fees.abs())
                        .currency(currency)
                        .executedAt(LocalDateTime.of(LocalDate.parse(dateStr, dateFormatter), LocalTime.parse(timeStr)))
                        .brokerName("Trade Republic")
                        .source("IMPORT_TR")
                        .assetType(assetType)
                        .build());

                } catch (Exception e) {
                    log.warn("Error parseando línea de Trade Republic: {}. Error: {}", line, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error leyendo archivo de Trade Republic", e);
        }

        return transactions;
    }

    private String clean(String s) {
        if (s == null) return "";
        return s.replace("\"", "").trim();
    }
}
