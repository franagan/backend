package com.inversionlibre.backend.service;

import com.inversionlibre.backend.model.Transaction;
import com.inversionlibre.backend.service.importing.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrokerImportService {

    private final TransactionService transactionService;
    private final List<BrokerParser> parsers = List.of(
        new DegiroParser(),
        new IbkrParser(),
        new RevolutParser(),
        new TradeRepublicParser(),
        new MyInvestorParser()
    );

    public List<Transaction> importFromBroker(MultipartFile file, String portfolioId, String userId) throws IOException {
        String fileName = file.getOriginalFilename();
        
        // Peek at the first line to identify the broker
        String headerLine;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            headerLine = reader.readLine();
        }

        BrokerParser selectedParser = null;
        for (BrokerParser parser : parsers) {
            if (parser.canParse(fileName, headerLine)) {
                selectedParser = parser;
                break;
            }
        }

        if (selectedParser == null) {
            throw new IllegalArgumentException("No se pudo identificar el formato del broker para el archivo: " + fileName);
        }

        log.info("Importando transacciones desde {} para el portfolio {}", selectedParser.getClass().getSimpleName(), portfolioId);
        
        List<Transaction> parsedTransactions = selectedParser.parse(file.getInputStream(), portfolioId, userId);
        List<Transaction> savedTransactions = new ArrayList<>();

        for (Transaction transaction : parsedTransactions) {
            try {
                Transaction saved = transactionService.createTransaction(transaction);
                savedTransactions.add(saved);
            } catch (Exception e) {
                log.error("Error importando transacción: {}. Error: {}", transaction.getFormattedSummary(), e.getMessage());
            }
        }

        return savedTransactions;
    }
}
