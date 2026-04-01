package com.inversionlibre.backend.controller;

import com.inversionlibre.backend.service.ReportService;
import com.inversionlibre.backend.model.User;
import com.inversionlibre.backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controlador para generación de reportes financieros
 *
 * Endpoints disponibles:
 * - GET /api/reports/portfolio/pdf - Reporte PDF del portafolio
 * - GET /api/reports/investments/excel - Reporte Excel de inversiones
 * - GET /api/reports/transactions/csv - Reporte CSV de transacciones
 * - GET /api/reports/financial-summary/pdf - Resumen financiero PDF
 *
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reportes", description = "API para generación de reportes financieros")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    /**
     * Genera un reporte PDF completo del portafolio del usuario
     *
     * @param userDetails Usuario autenticado
     * @return PDF del reporte de portafolio
     */
    @GetMapping("/portfolio/pdf")
    @Operation(summary = "Generar reporte PDF del portafolio",
               description = "Genera un documento PDF con el detalle completo del portafolio de inversiones")
    public ResponseEntity<byte[]> generatePortfolioPdfReport(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Generando reporte PDF de portafolio");

        String userId = getUserId(userDetails);
        byte[] pdfContent = reportService.generatePortfolioPdfReport(userId);

        String filename = generateFilename("portafolio", "pdf");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" +
                        URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }

    /**
     * Genera un reporte Excel con todas las inversiones del usuario
     *
     * @param userDetails Usuario autenticado
     * @return Excel con detalle de inversiones
     */
    @GetMapping("/investments/excel")
    @Operation(summary = "Generar reporte Excel de inversiones",
               description = "Genera un documento Excel con el detalle de todas las inversiones")
    public ResponseEntity<byte[]> generateInvestmentsExcelReport(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Generando reporte Excel de inversiones");

        String userId = getUserId(userDetails);
        byte[] excelContent = reportService.generateInvestmentsExcelReport(userId);

        String filename = generateFilename("inversiones", "xlsx");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" +
                        URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelContent);
    }

    /**
     * Genera un reporte CSV con todas las transacciones del usuario
     *
     * @param userDetails Usuario autenticado
     * @return CSV con historial de transacciones
     */
    @GetMapping("/transactions/csv")
    @Operation(summary = "Generar reporte CSV de transacciones",
               description = "Genera un archivo CSV con el historial completo de transacciones")
    public ResponseEntity<String> generateTransactionsCsvReport(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Generando reporte CSV de transacciones");

        String userId = getUserId(userDetails);
        String csvContent = reportService.generateTransactionsCsvReport(userId);

        String filename = generateFilename("transacciones", "csv");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" +
                        URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent);
    }

    /**
     * Genera un resumen financiero en PDF (gastos e ingresos)
     *
     * @param userDetails Usuario autenticado
     * @return PDF con resumen financiero
     */
    @GetMapping("/financial-summary/pdf")
    @Operation(summary = "Generar resumen financiero PDF",
               description = "Genera un documento PDF con el resumen de gastos e ingresos")
    public ResponseEntity<byte[]> generateFinancialSummaryPdfReport(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("Generando resumen financiero PDF");

        String userId = getUserId(userDetails);
        byte[] pdfContent = reportService.generateFinancialSummaryPdfReport(userId);

        String filename = generateFilename("resumen_financiero", "pdf");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" +
                        URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }

    // =====================================================
    // MÉTODOS AUXILIARES
    // =====================================================

    /**
     * Obtiene el ID del usuario a partir del UserDetails
     */
    private String getUserId(UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));
        return user.getId();
    }

    /**
     * Genera un nombre de archivo con timestamp
     */
    private String generateFilename(String baseName, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("inversion_libre_%s_%s.%s", baseName, timestamp, extension);
    }
}