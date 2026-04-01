package com.inversionlibre.backend.service;

import com.inversionlibre.backend.dto.investment.InvestmentResponse;
import com.inversionlibre.backend.model.Expense;
import com.inversionlibre.backend.model.Investment;
import com.inversionlibre.backend.model.Portfolio;
import com.inversionlibre.backend.model.Transaction;
import com.inversionlibre.backend.model.User;
import com.inversionlibre.backend.repository.ExpenseRepository;
import com.inversionlibre.backend.repository.InvestmentRepository;
import com.inversionlibre.backend.repository.PortfolioRepository;
import com.inversionlibre.backend.repository.TransactionRepository;
import com.inversionlibre.backend.repository.UserRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para generación de reportes en PDF y Excel
 *
 * Proporciona funcionalidades de exportación para:
 * - Portafolios de inversión
 * - Transacciones
 * - Gastos e ingresos
 * - Resúmenes financieros
 *
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final PortfolioRepository portfolioRepository;
    private final InvestmentRepository investmentRepository;
    private final TransactionRepository transactionRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    // Colores corporativos
    private static final int PRIMARY_COLOR_R = 234; // Amarillo/Naranja
    private static final int PRIMARY_COLOR_G = 179;
    private static final int PRIMARY_COLOR_B = 8;
    private static final int HEADER_BG_R = 45;
    private static final int HEADER_BG_G = 45;
    private static final int HEADER_BG_B = 45;

    /**
     * Genera un reporte PDF completo del portafolio del usuario
     *
     * @param userId ID del usuario
     * @return Array de bytes del PDF
     */
    public byte[] generatePortfolioPdfReport(String userId) {
        log.info("Generando reporte PDF de portafolio para usuario: {}", userId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);

            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // Fuentes
            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24,
                    new java.awt.Color(PRIMARY_COLOR_R, PRIMARY_COLOR_G, PRIMARY_COLOR_B));
            com.lowagie.text.Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            com.lowagie.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.WHITE);

            // Encabezado
            Paragraph title = new Paragraph("Inversion Libre", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Reporte de Portafolio", subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            document.add(new Paragraph(" "));

            // Información del usuario
            PdfPTable userInfoTable = new PdfPTable(2);
            userInfoTable.setWidthPercentage(100);
            addUserInfo(userInfoTable, user, normalFont);
            document.add(userInfoTable);

            document.add(new Paragraph(" "));

            // Resumen general
            Paragraph summaryTitle = new Paragraph("Resumen General", subtitleFont);
            document.add(summaryTitle);
            document.add(new Paragraph(" "));

            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            addSummaryHeader(summaryTable, headerFont);
            addSummaryRow(summaryTable, portfolios, normalFont);
            document.add(summaryTable);

            document.add(new Paragraph(" "));

            // Detalle de portafolios
            for (Portfolio portfolio : portfolios) {
                addPortfolioSection(document, portfolio, subtitleFont, normalFont, headerFont);
                document.add(new Paragraph(" "));
            }

            // Pie de página
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph(
                    "Generado el " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            writer.close();

            log.info("Reporte PDF generado exitosamente para usuario: {}", userId);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generando reporte PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el reporte PDF", e);
        }
    }

    /**
     * Genera un reporte Excel de inversiones
     *
     * @param userId ID del usuario
     * @return Array de bytes del Excel
     */
    public byte[] generateInvestmentsExcelReport(String userId) {
        log.info("Generando reporte Excel de inversiones para usuario: {}", userId);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Estilos
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // Obtener datos
            List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);

            // Hoja de resumen
            Sheet summarySheet = workbook.createSheet("Resumen");
            createSummarySheet(summarySheet, portfolios, headerStyle, currencyStyle);

            // Hoja de inversiones detalladas
            Sheet investmentsSheet = workbook.createSheet("Inversiones");
            createInvestmentsSheet(investmentsSheet, portfolios, headerStyle, currencyStyle, percentStyle, dateStyle);

            // Hoja de rendimiento
            Sheet performanceSheet = workbook.createSheet("Rendimiento");
            createPerformanceSheet(performanceSheet, portfolios, headerStyle, percentStyle);

            workbook.write(baos);
            log.info("Reporte Excel generado exitosamente para usuario: {}", userId);
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Error generando reporte Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el reporte Excel", e);
        }
    }

    /**
     * Genera un reporte CSV de transacciones
     *
     * @param userId ID del usuario
     * @return String CSV
     */
    public String generateTransactionsCsvReport(String userId) {
        log.info("Generando reporte CSV de transacciones para usuario: {}", userId);

        List<Transaction> transactions = transactionRepository.findByUserId(userId);

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Fecha,Tipo,Simbolo,Cantidad,Precio,Total,Estado\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Transaction tx : transactions) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",
                    tx.getId(),
                    tx.getCreatedAt() != null ? tx.getCreatedAt().format(formatter) : "",
                    tx.getType() != null ? tx.getType().name() : "",
                    tx.getStockSymbol() != null ? tx.getStockSymbol() : "",
                    tx.getQuantity() != null ? tx.getQuantity().toPlainString() : "",
                    tx.getUnitPrice() != null ? tx.getUnitPrice().toPlainString() : "",
                    tx.getNetAmount() != null ? tx.getNetAmount().toPlainString() : "",
                    tx.getStatus() != null ? tx.getStatus().name() : ""
            ));
        }

        log.info("Reporte CSV generado: {} transacciones", transactions.size());
        return csv.toString();
    }

    /**
     * Genera un reporte PDF de gastos e ingresos
     *
     * @param userId ID del usuario
     * @return Array de bytes del PDF
     */
    public byte[] generateFinancialSummaryPdfReport(String userId) {
        log.info("Generando reporte PDF de resumen financiero para usuario: {}", userId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

            List<Expense> expenses = expenseRepository.findByUserId(userId);

            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // Fuentes
            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24,
                    new java.awt.Color(PRIMARY_COLOR_R, PRIMARY_COLOR_G, PRIMARY_COLOR_B));
            com.lowagie.text.Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            com.lowagie.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.WHITE);

            // Encabezado
            Paragraph title = new Paragraph("Inversion Libre", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Resumen Financiero", subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            document.add(new Paragraph(" "));

            // Información del usuario
            PdfPTable userInfoTable = new PdfPTable(2);
            userInfoTable.setWidthPercentage(100);
            addUserInfo(userInfoTable, user, normalFont);
            document.add(userInfoTable);

            document.add(new Paragraph(" "));

            // Resumen de gastos
            PdfPTable expenseTable = new PdfPTable(5);
            expenseTable.setWidthPercentage(100);
            addExpenseHeader(expenseTable, headerFont);
            addExpenseRows(expenseTable, expenses, normalFont);
            document.add(expenseTable);

            document.add(new Paragraph(" "));

            // Pie de página
            Paragraph footer = new Paragraph(
                    "Generado el " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            writer.close();

            log.info("Reporte financiero PDF generado exitosamente para usuario: {}", userId);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generando reporte financiero PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el reporte financiero", e);
        }
    }

    // =====================================================
    // MÉTODOS AUXILIARES PARA PDF
    // =====================================================

    private void addUserInfo(PdfPTable table, User user, com.lowagie.text.Font font) {
        table.addCell(createCell("Usuario:", font, true));
        table.addCell(createCell(user.getFullName() != null ? user.getFullName() : user.getEmail(), font, false));
        table.addCell(createCell("Email:", font, true));
        table.addCell(createCell(user.getEmail(), font, false));
        table.addCell(createCell("Fecha Reporte:", font, true));
        table.addCell(createCell(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), font, false));
    }

    private void addSummaryHeader(PdfPTable table, com.lowagie.text.Font headerFont) {
        String[] headers = {"Total Portafolios", "Valor Total", "Total Invertido", "Ganancia/Pérdida"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new java.awt.Color(HEADER_BG_R, HEADER_BG_G, HEADER_BG_B));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }
    }

    private void addSummaryRow(PdfPTable table, List<Portfolio> portfolios, com.lowagie.text.Font font) {
        // Total portafolios
        table.addCell(createCell(String.valueOf(portfolios.size()), font, false));

        // Valores totales
        BigDecimal totalValue = portfolios.stream()
                .map(Portfolio::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        table.addCell(createCell(formatCurrency(totalValue), font, false));

        BigDecimal totalInvested = portfolios.stream()
                .map(Portfolio::getTotalInvested)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        table.addCell(createCell(formatCurrency(totalInvested), font, false));

        BigDecimal totalGainLoss = portfolios.stream()
                .map(Portfolio::getTotalGainLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        table.addCell(createCell(formatCurrency(totalGainLoss), font, false));
    }

    private void addPortfolioSection(Document document, Portfolio portfolio, com.lowagie.text.Font subtitleFont,
                                      com.lowagie.text.Font normalFont, com.lowagie.text.Font headerFont) throws DocumentException {
        // Título del portafolio
        Paragraph portfolioTitle = new Paragraph(portfolio.getName(), subtitleFont);
        document.add(portfolioTitle);

        if (portfolio.getDescription() != null) {
            document.add(new Paragraph(portfolio.getDescription(), normalFont));
        }
        document.add(new Paragraph(" "));

        // Información del portafolio
        PdfPTable portfolioInfo = new PdfPTable(2);
        portfolioInfo.setWidthPercentage(100);
        portfolioInfo.addCell(createCell("Valor Total:", normalFont, true));
        portfolioInfo.addCell(createCell(formatCurrency(portfolio.getTotalValue()), normalFont, false));
        portfolioInfo.addCell(createCell("Total Invertido:", normalFont, true));
        portfolioInfo.addCell(createCell(formatCurrency(portfolio.getTotalInvested()), normalFont, false));
        portfolioInfo.addCell(createCell("Ganancia/Pérdida:", normalFont, true));
        portfolioInfo.addCell(createCell(formatCurrency(portfolio.getTotalGainLoss()) +
                " (" + formatPercent(portfolio.getTotalGainLossPercentage()) + ")", normalFont, false));
        document.add(portfolioInfo);

        // Inversiones del portafolio
        if (portfolio.getInvestmentIds() != null && !portfolio.getInvestmentIds().isEmpty()) {
            document.add(new Paragraph(" "));
            Paragraph investmentsTitle = new Paragraph("Inversiones:", normalFont);
            document.add(investmentsTitle);

            PdfPTable investmentsTable = new PdfPTable(5);
            investmentsTable.setWidthPercentage(100);
            addInvestmentsHeader(investmentsTable, headerFont);

            for (String invId : portfolio.getInvestmentIds()) {
                investmentRepository.findById(invId).ifPresent(inv -> {
                    addInvestmentRow(investmentsTable, inv, normalFont);
                });
            }
            document.add(investmentsTable);
        }
    }

    private void addInvestmentsHeader(PdfPTable table, com.lowagie.text.Font headerFont) {
        String[] headers = {"Simbolo", "Cantidad", "Precio Prom.", "Valor Actual", "Ganancia"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new java.awt.Color(HEADER_BG_R, HEADER_BG_G, HEADER_BG_B));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }
    }

    private void addInvestmentRow(PdfPTable table, Investment inv, com.lowagie.text.Font font) {
        table.addCell(createCell(inv.getStockSymbol(), font, false));
        table.addCell(createCell(inv.getQuantity() != null ? inv.getQuantity().toString() : "0", font, false));
        table.addCell(createCell(formatCurrency(inv.getAveragePrice()), font, false));
        table.addCell(createCell(formatCurrency(inv.getCurrentValue()), font, false));
        table.addCell(createCell(formatCurrency(inv.getGainLoss()) +
                " (" + formatPercent(inv.getGainLossPercentage()) + ")", font, false));
    }

    private void addExpenseHeader(PdfPTable table, com.lowagie.text.Font headerFont) {
        String[] headers = {"Fecha", "Descripcion", "Categoria", "Monto", "Estado"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new java.awt.Color(HEADER_BG_R, HEADER_BG_G, HEADER_BG_B));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }
    }

    private void addExpenseRows(PdfPTable table, List<Expense> expenses, com.lowagie.text.Font font) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Expense expense : expenses) {
            table.addCell(createCell(expense.getCreatedAt() != null ?
                    expense.getCreatedAt().format(formatter) : "", font, false));
            table.addCell(createCell(expense.getConcept() != null ?
                    expense.getConcept() : "", font, false));
            table.addCell(createCell(expense.getCategory() != null ?
                    expense.getCategory() : "", font, false));
            table.addCell(createCell(formatCurrency(BigDecimal.valueOf(expense.getAmount())), font, false));
            table.addCell(createCell("COMPLETADO", font, false));
        }
    }

    private PdfPCell createCell(String text, com.lowagie.text.Font font, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(5);
        if (isHeader) {
            cell.setBackgroundColor(new java.awt.Color(240, 240, 240));
        }
        return cell;
    }

    // =====================================================
    // MÉTODOS AUXILIARES PARA EXCEL
    // =====================================================

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00 €"));
        return style;
    }

    private CellStyle createPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.00%"));
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("dd/mm/yyyy"));
        return style;
    }

    private void createSummarySheet(Sheet sheet, List<Portfolio> portfolios,
                                     CellStyle headerStyle, CellStyle currencyStyle) {
        // Encabezados
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        String[] headers = {"Metrica", "Valor"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
            headerRow.getCell(i).setCellStyle(headerStyle);
        }

        // Datos
        BigDecimal totalValue = portfolios.stream()
                .map(Portfolio::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInvested = portfolios.stream()
                .map(Portfolio::getTotalInvested)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGainLoss = portfolios.stream()
                .map(Portfolio::getTotalGainLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        org.apache.poi.ss.usermodel.Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("Numero de Portafolios");
        row1.createCell(1).setCellValue(portfolios.size());

        org.apache.poi.ss.usermodel.Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("Valor Total");
        row2.createCell(1).setCellValue(totalValue.doubleValue());
        row2.getCell(1).setCellStyle(currencyStyle);

        org.apache.poi.ss.usermodel.Row row3 = sheet.createRow(3);
        row3.createCell(0).setCellValue("Total Invertido");
        row3.createCell(1).setCellValue(totalInvested.doubleValue());
        row3.getCell(1).setCellStyle(currencyStyle);

        org.apache.poi.ss.usermodel.Row row4 = sheet.createRow(4);
        row4.createCell(0).setCellValue("Ganancia/Pérdida Total");
        row4.createCell(1).setCellValue(totalGainLoss.doubleValue());
        row4.getCell(1).setCellStyle(currencyStyle);

        // Ajustar anchos de columna
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createInvestmentsSheet(Sheet sheet, List<Portfolio> portfolios,
                                         CellStyle headerStyle, CellStyle currencyStyle,
                                         CellStyle percentStyle, CellStyle dateStyle) {
        // Encabezados
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        String[] headers = {"Portafolio", "Simbolo", "Nombre", "Cantidad", "Precio Prom.",
                           "Precio Actual", "Valor Actual", "Ganancia", "Rentabilidad"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
            headerRow.getCell(i).setCellStyle(headerStyle);
        }

        // Datos
        int rowNum = 1;
        for (Portfolio portfolio : portfolios) {
            if (portfolio.getInvestmentIds() != null) {
                for (String invId : portfolio.getInvestmentIds()) {
                    java.util.Optional<Investment> investmentOpt = investmentRepository.findById(invId);
                    if (investmentOpt.isPresent()) {
                        Investment inv = investmentOpt.get();
                        org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(portfolio.getName());
                        row.createCell(1).setCellValue(inv.getStockSymbol());
                        row.createCell(2).setCellValue(inv.getStockName() != null ? inv.getStockName() : "");
                        row.createCell(3).setCellValue(inv.getQuantity() != null ? inv.getQuantity().doubleValue() : 0);

                        org.apache.poi.ss.usermodel.Cell priceAvg = row.createCell(4);
                        priceAvg.setCellValue(inv.getAveragePrice() != null ? inv.getAveragePrice().doubleValue() : 0);
                        priceAvg.setCellStyle(currencyStyle);

                        org.apache.poi.ss.usermodel.Cell priceCurrent = row.createCell(5);
                        priceCurrent.setCellValue(inv.getCurrentPrice() != null ? inv.getCurrentPrice().doubleValue() : 0);
                        priceCurrent.setCellStyle(currencyStyle);

                        org.apache.poi.ss.usermodel.Cell valueCurrent = row.createCell(6);
                        valueCurrent.setCellValue(inv.getCurrentValue() != null ? inv.getCurrentValue().doubleValue() : 0);
                        valueCurrent.setCellStyle(currencyStyle);

                        org.apache.poi.ss.usermodel.Cell gain = row.createCell(7);
                        gain.setCellValue(inv.getGainLoss() != null ? inv.getGainLoss().doubleValue() : 0);
                        gain.setCellStyle(currencyStyle);

                        org.apache.poi.ss.usermodel.Cell returnPct = row.createCell(8);
                        returnPct.setCellValue(inv.getGainLossPercentage() != null ?
                                inv.getGainLossPercentage().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP).doubleValue() : 0);
                        returnPct.setCellStyle(percentStyle);
                    }
                }
            }
        }

        // Ajustar anchos de columna
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createPerformanceSheet(Sheet sheet, List<Portfolio> portfolios,
                                         CellStyle headerStyle, CellStyle percentStyle) {
        // Encabezados
        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
        String[] headers = {"Portafolio", "Tipo", "Nivel de Riesgo", "Rentabilidad Total",
                           "Volatilidad", "Ratio Sharpe", "Diversificacion"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
            headerRow.getCell(i).setCellStyle(headerStyle);
        }

        // Datos
        int rowNum = 1;
        for (Portfolio portfolio : portfolios) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum);
            row.createCell(0).setCellValue(portfolio.getName());
            row.createCell(1).setCellValue(portfolio.getType() != null ? portfolio.getType().name() : "");
            row.createCell(2).setCellValue(portfolio.getRiskLevel() != null ? portfolio.getRiskLevel().name() : "");

            org.apache.poi.ss.usermodel.Cell returnCell = row.createCell(3);
            returnCell.setCellValue(portfolio.getTotalGainLossPercentage() != null ?
                    portfolio.getTotalGainLossPercentage().doubleValue() : 0);
            returnCell.setCellStyle(percentStyle);

            if (portfolio.getAnalytics() != null) {
                org.apache.poi.ss.usermodel.Cell volCell = row.createCell(4);
                volCell.setCellValue(portfolio.getAnalytics().getVolatility() != null ?
                        portfolio.getAnalytics().getVolatility().doubleValue() : 0);
                volCell.setCellStyle(percentStyle);

                org.apache.poi.ss.usermodel.Cell sharpeCell = row.createCell(5);
                sharpeCell.setCellValue(portfolio.getAnalytics().getSharpeRatio() != null ?
                        portfolio.getAnalytics().getSharpeRatio().doubleValue() : 0);
            } else {
                row.createCell(4).setCellValue(0);
                row.createCell(5).setCellValue(0);
            }

            row.createCell(6).setCellValue(portfolio.isDiversified() ? "Si" : "No");
            rowNum++;
        }

        // Ajustar anchos de columna
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // =====================================================
    // UTILIDADES
    // =====================================================

    private String formatCurrency(BigDecimal value) {
        if (value == null) return "0.00 €";
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " €";
    }

    private String formatPercent(BigDecimal value) {
        if (value == null) return "0.00%";
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }
}