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

    public List<BudgetCategory> getUserBudgets(String userId) {
        return budgetCategoryRepository.findByUserId(userId);
    }

    public BudgetCategory createBudget(BudgetCategory budget, String userId) {
        // Validation removed to allow multiple expenses with the same category name
        
        budget.setUserId(userId);
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
        budget.setSpent(budgetDetails.getSpent());
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
