package com.inversionlibre.backend.service.importing;

import com.inversionlibre.backend.model.Transaction;
import java.io.InputStream;
import java.util.List;

public interface BrokerParser {
    /**
     * Parsea el archivo del broker y devuelve una lista de transacciones
     */
    List<Transaction> parse(InputStream inputStream, String portfolioId, String userId);

    /**
     * Determina si este parser puede manejar el archivo basándose en el nombre y la cabecera
     */
    boolean canParse(String fileName, String headerLine);
}
