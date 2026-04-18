package com.inversionlibre.backend.controller;

import com.inversionlibre.backend.model.Transaction;
import com.inversionlibre.backend.service.BrokerImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BrokerImportController {

    private final BrokerImportService brokerImportService;

    @PostMapping("/{portfolioId}/import")
    public ResponseEntity<List<Transaction>> importFromBroker(
            @PathVariable String portfolioId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) String userId) {
        
        log.info("Petición de importación de broker para portfolio: {}", portfolioId);
        
        try {
            // En producción el userId vendría del SecurityContext
            String finalUserId = (userId != null) ? userId : "user-123";
            
            List<Transaction> imported = brokerImportService.importFromBroker(file, portfolioId, finalUserId);
            return ResponseEntity.ok(imported);
            
        } catch (Exception e) {
            log.error("Error en la importación: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
