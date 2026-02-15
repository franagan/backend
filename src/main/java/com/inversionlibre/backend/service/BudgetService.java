package com.inversionlibre.backend.service;

import com.inversionlibre.backend.model.BudgetCategory;
import com.inversionlibre.backend.repository.BudgetCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetCategoryRepository budgetCategoryRepository;
    private final com.inversionlibre.backend.repository.ExpenseRepository expenseRepository;

    public List<BudgetCategory> getUserBudgets(String userId) {
        List<BudgetCategory> budgets = budgetCategoryRepository.findByUserId(userId);
        List<com.inversionlibre.backend.model.Expense> allExpenses = expenseRepository.findByUserId(userId);

        // Map existing budgets by category and subcategory for easy lookup
        java.util.Map<String, BudgetCategory> existingBudgetsMap = new java.util.HashMap<>();
        for (BudgetCategory b : budgets) {
            String key = b.getName().toLowerCase() + (b.getSubcategory() != null ? "|" + b.getSubcategory().toLowerCase() : "");
            existingBudgetsMap.put(key, b);
        }

        // Find all expenses that don't belong to an existing budget and create "virtual" categories
        for (com.inversionlibre.backend.model.Expense expense : allExpenses) {
            String key = expense.getCategory().toLowerCase() + (expense.getSubcategory() != null ? "|" + expense.getSubcategory().toLowerCase() : "");
            if (!existingBudgetsMap.containsKey(key)) {
                BudgetCategory virtualBudget = BudgetCategory.builder()
                        .userId(userId)
                        .name(expense.getCategory())
                        .subcategory(expense.getSubcategory())
                        .limit(0.0)
                        .spent(0.0)
                        .color("bg-gray-500") // Default for unplanned
                        .build();
                budgets.add(virtualBudget);
                existingBudgetsMap.put(key, virtualBudget);
            }
        }

        // Reset spent to 0 and recalculate based on expenses
        for (BudgetCategory budget : budgets) {
            double totalSpent = allExpenses.stream()
                .filter(e -> e.getCategory() != null && e.getCategory().equalsIgnoreCase(budget.getName()))
                .filter(e -> {
                    if (budget.getSubcategory() != null && !budget.getSubcategory().isEmpty()) {
                        return budget.getSubcategory().equalsIgnoreCase(e.getSubcategory());
                    }
                    return e.getSubcategory() == null || e.getSubcategory().isEmpty();
                })
                .mapToDouble(com.inversionlibre.backend.model.Expense::getAmount)
                .sum();
            
            budget.setSpent(totalSpent);
        }
        
        return budgets;
    }

    public BudgetCategory createBudget(BudgetCategory budget, String userId) {
        // Validation removed to allow multiple expenses with the same category name
        
        budget.setUserId(userId);
        // budget.setSpent(0.0); // Spent is now calculated, initial 0 is fine but it won't persist as manual value anymore effectively.
        budget.setCreatedAt(LocalDateTime.now());
        budget.setUpdatedAt(LocalDateTime.now());
        return budgetCategoryRepository.save(budget);
    }

    public BudgetCategory updateBudget(String id, BudgetCategory budgetDetails, String userId) {
        BudgetCategory budget = budgetCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget category not found"));

        if (!budget.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        String newName = budgetDetails.getName().trim();
        // Validation removed to allow renaming to an existing category name

        budget.setName(newName);
        budget.setSubcategory(budgetDetails.getSubcategory()); // Allow updating subcategory
        
        // budget.setSpent(budgetDetails.getSpent()); // DON'T update spent manually anymore
        
        budget.setLimit(budgetDetails.getLimit());
        budget.setColor(budgetDetails.getColor());
        budget.setUpdatedAt(LocalDateTime.now());

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
