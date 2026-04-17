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
    private final com.inversionlibre.backend.repository.SavingsGoalRepository savingsGoalRepository;

    public List<Expense> getUserExpenses(String userId) {
        return expenseRepository.findByUserId(userId);
    }

    public Expense saveExpense(Expense expense, String userId) {
        expense.setUserId(userId);
        Expense saved = expenseRepository.save(expense);
        
        // Handle Goal Integration
        if (expense.getLinkedGoalId() != null) {
            recordGoalContribution(expense.getLinkedGoalId(), expense.getAmount(), userId);
        }
        
        return saved;
    }

    private void recordGoalContribution(String goalId, Double amount, String userId) {
        try {
            com.inversionlibre.backend.model.SavingsGoal goal = savingsGoalRepository.findById(goalId).orElse(null);
            if (goal != null && goal.getUserId().equals(userId)) {
                goal.setCurrentAmount(goal.getCurrentAmount() + Math.abs(amount));
                goal.setUpdatedAt(java.time.LocalDateTime.now());
                savingsGoalRepository.save(goal);
            }
        } catch (Exception e) {
            System.err.println("Error updating goal amount: " + e.getMessage());
        }
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
        
        // Handle Goal Integration if amount changed
        if (expense.getLinkedGoalId() != null && !expense.getAmount().equals(expenseDetails.getAmount())) {
            double diff = expenseDetails.getAmount() - expense.getAmount();
            recordGoalContribution(expense.getLinkedGoalId(), diff, userId);
        }

        expense.setAmount(expenseDetails.getAmount());
        expense.setDate(expenseDetails.getDate());
        expense.setCategory(expenseDetails.getCategory());
        expense.setSubcategory(expenseDetails.getSubcategory());
        expense.setIsRecurring(expenseDetails.getIsRecurring());
        expense.setRecurringPeriod(expenseDetails.getRecurringPeriod());
        expense.setLinkedGoalId(expenseDetails.getLinkedGoalId());

        return expenseRepository.save(expense);
    }

    public void deleteExpense(String id, String userId) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        
        if (!expense.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        // Revert Goal Contribution if deleted
        if (expense.getLinkedGoalId() != null) {
            recordGoalContribution(expense.getLinkedGoalId(), -expense.getAmount(), userId);
        }
        
        expenseRepository.delete(expense);
    }
    
    public List<Expense> getRecurringTemplates(String userId) {
        return expenseRepository.findByUserId(userId).stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsRecurring()))
                .toList();
    }

    public List<Expense> getExpensesByGoal(String goalId) {
        return expenseRepository.findByLinkedGoalId(goalId);
    }
}
