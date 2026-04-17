package com.inversionlibre.backend.controller;

import com.inversionlibre.backend.dto.ApiResponse;
import com.inversionlibre.backend.model.BudgetCategory;
import com.inversionlibre.backend.model.User;
import com.inversionlibre.backend.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetCategory>>> getUserBudgets(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) BudgetCategory.BudgetPeriod period,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        List<BudgetCategory> budgets = budgetService.getUserBudgets(user.getId(), period, year, month);
        return ResponseEntity.ok(ApiResponse.success("Presupuestos cargados correctamente", budgets));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetCategory>> createBudget(@AuthenticationPrincipal User user, @RequestBody BudgetCategory budget) {
        BudgetCategory createdBudget = budgetService.createBudget(budget, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Presupuesto creado con éxito", createdBudget));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetCategory>> updateBudget(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody BudgetCategory budget) {
        BudgetCategory updatedBudget = budgetService.updateBudget(id, budget, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Presupuesto actualizado con éxito", updatedBudget));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(@AuthenticationPrincipal User user, @PathVariable String id) {
        budgetService.deleteBudget(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Presupuesto eliminado con éxito"));
    }
}
