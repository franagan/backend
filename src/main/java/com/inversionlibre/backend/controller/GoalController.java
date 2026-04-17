package com.inversionlibre.backend.controller;

import com.inversionlibre.backend.dto.ApiResponse;
import com.inversionlibre.backend.model.SavingsGoal;
import com.inversionlibre.backend.model.User;
import com.inversionlibre.backend.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final com.inversionlibre.backend.service.ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SavingsGoal>>> getUserGoals(@AuthenticationPrincipal User user) {
        List<SavingsGoal> goals = goalService.getUserGoals(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Objetivos cargados correctamente", goals));
    }

    @GetMapping("/{id}/movements")
    public ResponseEntity<ApiResponse<List<com.inversionlibre.backend.model.Expense>>> getGoalMovements(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        List<com.inversionlibre.backend.model.Expense> movements = expenseService.getExpensesByGoal(id);
        return ResponseEntity.ok(ApiResponse.success("Movimientos cargados correctamente", movements));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SavingsGoal>> createGoal(@AuthenticationPrincipal User user, @RequestBody SavingsGoal goal) {
        SavingsGoal createdGoal = goalService.createGoal(goal, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Objetivo creado con éxito", createdGoal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingsGoal>> updateGoal(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody SavingsGoal goal) {
        SavingsGoal updatedGoal = goalService.updateGoal(id, goal, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Objetivo actualizado con éxito", updatedGoal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(@AuthenticationPrincipal User user, @PathVariable String id) {
        goalService.deleteGoal(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Objetivo eliminado con éxito"));
    }
}
