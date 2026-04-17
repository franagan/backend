package com.inversionlibre.backend.service;

import com.inversionlibre.backend.model.BudgetCategory;
import com.inversionlibre.backend.repository.BudgetCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetCategoryRepository budgetCategoryRepository;
    private final com.inversionlibre.backend.repository.ExpenseRepository expenseRepository;

    public List<BudgetCategory> getUserBudgets(String userId, BudgetCategory.BudgetPeriod requestedPeriod, Integer year, Integer month) {
        // Fetch ALL budgets regardless of period to allow cross-period scaling
        List<BudgetCategory> allBudgetsInDb = budgetCategoryRepository.findByUserId(userId);
        
        int targetYear = year != null ? year : LocalDate.now().getYear();
        int targetMonthValue = month != null ? month : LocalDate.now().getMonthValue();
        java.time.Month targetMonth = java.time.Month.of(targetMonthValue);
        
        // We will work with a "translated" list for the view
        List<BudgetCategory> displayBudgets = new java.util.ArrayList<>();
        java.util.Set<String> budgetedCategoryNames = new java.util.HashSet<>();

        for (BudgetCategory dbBudget : allBudgetsInDb) {
            BudgetCategory displayBudget = BudgetCategory.builder()
                    .id(dbBudget.getId())
                    .userId(dbBudget.getUserId())
                    .name(dbBudget.getName())
                    .subcategory(dbBudget.getSubcategory())
                    .color(dbBudget.getColor())
                    .description(dbBudget.getDescription())
                    .period(requestedPeriod != null ? requestedPeriod : BudgetCategory.BudgetPeriod.MONTHLY)
                    .createdAt(dbBudget.getCreatedAt())
                    .updatedAt(dbBudget.getUpdatedAt())
                    .build();

            // Scale the limit
            double originalLimit = dbBudget.getLimit();
            BudgetCategory.BudgetPeriod originalPeriod = dbBudget.getPeriod();
            
            if (requestedPeriod == BudgetCategory.BudgetPeriod.ANNUAL) {
                if (originalPeriod == BudgetCategory.BudgetPeriod.MONTHLY) {
                    displayBudget.setLimit(originalLimit * 12);
                } else {
                    displayBudget.setLimit(originalLimit);
                }
            } else { // MONTHLY or null
                if (originalPeriod == BudgetCategory.BudgetPeriod.ANNUAL) {
                    displayBudget.setLimit(originalLimit / 12);
                } else {
                    displayBudget.setLimit(originalLimit);
                }
            }
            
            displayBudgets.add(displayBudget);
            budgetedCategoryNames.add(displayBudget.getName().toLowerCase());
        }

        List<com.inversionlibre.backend.model.Expense> allExpenses = expenseRepository.findByUserId(userId);

        // Add virtual categories only for expenses whose category NAME is not budgeted at all
        for (com.inversionlibre.backend.model.Expense expense : allExpenses) {
            boolean inPeriod = false;
            LocalDate now = LocalDate.now();
            BudgetCategory.BudgetPeriod periodForCalc = requestedPeriod != null ? requestedPeriod : BudgetCategory.BudgetPeriod.MONTHLY;
            
            if (periodForCalc == BudgetCategory.BudgetPeriod.MONTHLY) {
                if (expense.getDate().getMonth() == targetMonth && expense.getDate().getYear() == targetYear) {
                    inPeriod = true;
                }
            } else if (periodForCalc == BudgetCategory.BudgetPeriod.ANNUAL) {
                if (expense.getDate().getYear() == targetYear) {
                    inPeriod = true;
                }
            }

            if (inPeriod && expense.getCategory() != null) {
                String catNameLower = expense.getCategory().toLowerCase();
                if (!budgetedCategoryNames.contains(catNameLower)) {
                    BudgetCategory virtualBudget = BudgetCategory.builder()
                            .userId(userId)
                            .name(expense.getCategory())
                            .limit(0.0)
                            .spent(0.0)
                            .period(periodForCalc)
                            .color("bg-gray-400")
                            .build();
                    displayBudgets.add(virtualBudget);
                    budgetedCategoryNames.add(catNameLower);
                }
            }
        }


        for (BudgetCategory budget : displayBudgets) {
            String budgetName = budget.getName() != null ? budget.getName().trim() : "";
            String budgetSubcat = budget.getSubcategory() != null ? budget.getSubcategory().trim() : "";

            double totalSpent = allExpenses.stream()
                .filter(e -> e.getCategory() != null && e.getCategory().trim().equalsIgnoreCase(budgetName))
                .filter(e -> {
                    // If budget defines a subcategory, only match those. 
                    // If it's blank/empty, match everything in that category.
                    if (budgetSubcat != null && !budgetSubcat.isEmpty() && !budgetSubcat.trim().isEmpty()) {
                        return e.getSubcategory() != null && e.getSubcategory().trim().equalsIgnoreCase(budgetSubcat.trim());
                    }
                    return true;
                })
                .filter(e -> {
                    if (requestedPeriod == BudgetCategory.BudgetPeriod.ANNUAL) {
                        return e.getDate().getYear() == targetYear;
                    } else { // MONTHLY default
                        return e.getDate().getMonth() == targetMonth &&
                               e.getDate().getYear() == targetYear;
                    }
                })
                .mapToDouble(e -> Math.abs(e.getAmount())) // Always sum absolute values
                .sum();
            
            budget.setSpent(totalSpent);
        }
        
        return displayBudgets;
    }

    public BudgetCategory createBudget(BudgetCategory budget, String userId) {
        // Validation removed to allow multiple expenses with the same category name
        
        budget.setUserId(userId);
        budget.setName(budget.getName().trim());
        budget.setCreatedAt(LocalDateTime.now());
        budget.setUpdatedAt(LocalDateTime.now());
        
        // Propagation: If there are other budgets with the same name, they should have the same color
        List<BudgetCategory> sameNameBudgets = budgetCategoryRepository.findByUserId(userId).stream()
                .filter(b -> b.getName().equalsIgnoreCase(budget.getName()))
                .toList();
        if (!sameNameBudgets.isEmpty()) {
            budget.setColor(sameNameBudgets.get(0).getColor());
        }

        return budgetCategoryRepository.save(budget);
    }

    public BudgetCategory updateBudget(String id, BudgetCategory budgetDetails, String userId) {
        BudgetCategory budget = budgetCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget category not found"));

        if (!budget.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        String oldColor = budget.getColor();
        String newColor = budgetDetails.getColor();
        String newName = budgetDetails.getName().trim();

        budget.setName(newName);
        budget.setSubcategory(budgetDetails.getSubcategory());
        budget.setLimit(budgetDetails.getLimit());
        budget.setColor(newColor);
        budget.setPeriod(budgetDetails.getPeriod());
        budget.setUpdatedAt(LocalDateTime.now());

        // Bulk update color for SAME CATEGORY NAME (Propagation)
        if (!newColor.equals(oldColor)) {
            List<BudgetCategory> others = budgetCategoryRepository.findByUserId(userId).stream()
                    .filter(b -> b.getName().equalsIgnoreCase(newName))
                    .toList();
            for (BudgetCategory other : others) {
                other.setColor(newColor);
                budgetCategoryRepository.save(other);
            }
        }

        return budgetCategoryRepository.save(budget);
    }

    public void deleteBudget(String id, String userId) {
        BudgetCategory budget = budgetCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget category not found"));

        if (!budget.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        budgetCategoryRepository.delete(budget);
    }
}
