package com.inversionlibre.backend.service;

import com.inversionlibre.backend.model.Expense;
import com.inversionlibre.backend.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public List<Expense> getUserExpenses(String userId) {
        return expenseRepository.findByUserId(userId);
    }

    public Expense saveExpense(Expense expense, String userId) {
        expense.setUserId(userId);
        return expenseRepository.save(expense);
    }

    public List<Expense> importExpenses(MultipartFile file, String userId) throws IOException {
        List<Expense> expenses = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;
            
            // Basic CSV parsing
            // Expected format: Date (YYYY-MM-DD or DD/MM/YYYY), Concept, Amount
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    // Skip header if it looks like a header
                    if (line.toLowerCase().contains("date") || line.toLowerCase().contains("fecha")) {
                        isFirstLine = false;
                        continue;
                    }
                    isFirstLine = false;
                }

                String[] parts = line.split(",|;"); // Support comma or semicolon
                if (parts.length >= 3) {
                    try {
                        String dateStr = parts[0].trim();
                        String concept = parts[1].trim();
                        String amountStr = parts[2].trim().replace("€", "").replace("$", "");
                        
                        // Handle localized numbers (1.000,00 vs 1000.00)
                        if (amountStr.contains(".") && amountStr.contains(",")) {
                             amountStr = amountStr.replace(".", "").replace(",", ".");
                        } else if (amountStr.contains(",")) {
                             amountStr = amountStr.replace(",", ".");
                        }

                        double amount = Double.parseDouble(amountStr);
                        
                        // Parse date (try standard ISO first, then European)
                        LocalDate date;
                        try {
                             date = LocalDate.parse(dateStr);
                        } catch (Exception e) {
                             date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        }

                        expenses.add(Expense.builder()
                                .userId(userId)
                                .date(date)
                                .concept(concept)
                                .amount(Math.abs(amount)) // Store as positive value for expenses
                                .build());
                    } catch (Exception e) {
                        System.err.println("Error parsing line: " + line + " - " + e.getMessage());
                        // Continue processing other lines
                    }
                }
            }
        }
        
        return expenseRepository.saveAll(expenses);
    }

    public Expense updateExpense(String id, Expense expenseDetails, String userId) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (!expense.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        expense.setConcept(expenseDetails.getConcept());
        expense.setAmount(expenseDetails.getAmount());
        expense.setDate(expenseDetails.getDate());
        expense.setCategory(expenseDetails.getCategory());
        expense.setSubcategory(expenseDetails.getSubcategory());

        return expenseRepository.save(expense);
    }

    public void deleteExpense(String id, String userId) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        
        if (!expense.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        expenseRepository.delete(expense);
    }
}
