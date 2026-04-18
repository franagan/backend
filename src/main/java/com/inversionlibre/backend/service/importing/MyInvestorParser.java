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
public class MyInvestorParser implements BrokerParser {

    @Override
    public boolean canParse(String fileName, String headerLine) {
        if (headerLine == null) return false;
        String normalizedHeader = headerLine.toLowerCase();
        return normalizedHeader.contains("myinvestor") || (normalizedHeader.contains("títulos") && normalizedHeader.contains("importe neto"));
    }

    @Override
    public List<Transaction> parse(InputStream inputStream, String portfolioId, String userId) {
        List<Transaction> transactions = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] parts = line.split(",|;");
                if (parts.length < 8) continue;

                try {
                    // 0:Fecha, 2:Operacion, 3:Fondo/Producto, 4:Cantidad, 5:Precio, 9:Importe Neto
                    String dateStr = clean(parts[0]);
                    String opType = clean(parts[2]).toUpperCase();
                    String productName = clean(parts[3]);
                    String quantityStr = clean(parts[4]);
                    String priceStr = clean(parts[5]);
                    String currency = clean(parts[6]);
                    String netAmountStr = clean(parts[9]);

                    Transaction.TransactionType type;
                    if (opType.contains("SUSCRIP") || opType.contains("COMPRA")) type = Transaction.TransactionType.BUY;
                    else if (opType.contains("REEMBOLSO") || opType.contains("VENTA")) type = Transaction.TransactionType.SELL;
                    else if (opType.contains("DIVIDENDO")) type = Transaction.TransactionType.DIVIDEND;
                    else continue;

                    BigDecimal quantity = new BigDecimal(quantityStr.replace(".", "").replace(",", "."));
                    BigDecimal price = new BigDecimal(priceStr.replace(".", "").replace(",", "."));
                    BigDecimal netAmount = new BigDecimal(netAmountStr.replace(".", "").replace(",", "."));

                    transactions.add(Transaction.builder()
                        .userId(userId)
                        .portfolioId(portfolioId)
                        .stockSymbol(productName) // MyInvestor context usually uses the fund name
                        .stockName(productName)
                        .type(type)
                        .status(Transaction.TransactionStatus.EXECUTED)
                        .quantity(quantity.abs())
                        .unitPrice(price.abs())
                        .netAmount(netAmount.abs())
                        .currency(currency.equals("€") ? "EUR" : currency)
                        .executedAt(LocalDateTime.of(LocalDate.parse(dateStr, dateFormatter), LocalTime.MIDNIGHT))
                        .brokerName("MyInvestor")
                        .source("IMPORT_MYINVESTOR")
                        .assetType(Stock.AssetType.MUTUAL_FUND)
                        .build());

                } catch (Exception e) {
                    log.warn("Error parseando línea de MyInvestor: {}. Error: {}", line, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error leyendo archivo de MyInvestor", e);
        }

        return transactions;
    }

    private String clean(String s) {
        if (s == null) return "";
        return s.replace("\"", "").trim();
    }
}
