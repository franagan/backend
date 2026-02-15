package com.inversionlibre.backend.controller;

import com.inversionlibre.backend.dto.ApiResponse;
import com.inversionlibre.backend.model.Expense;
import com.inversionlibre.backend.model.User;
import com.inversionlibre.backend.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Expense>>> getUserExpenses(@AuthenticationPrincipal User user) {
        List<Expense> expenses = expenseService.getUserExpenses(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Gastos cargados correctamente", expenses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Expense>> createExpense(
            @AuthenticationPrincipal User user,
            @RequestBody Expense expense) {
        Expense savedExpense = expenseService.saveExpense(expense, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Gasto creado con éxito", savedExpense));
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<List<Expense>>> importExpenses(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        try {
            List<Expense> importedExpenses = expenseService.importExpenses(file, user.getId());
            return ResponseEntity.ok(ApiResponse.success("Gastos importados con éxito: " + importedExpenses.size(), importedExpenses));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Error al importar el archivo: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Expense>> updateExpense(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody Expense expenseDetails) {
        Expense updatedExpense = expenseService.updateExpense(id, expenseDetails, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Gasto actualizado con éxito", updatedExpense));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@AuthenticationPrincipal User user, @PathVariable String id) {
        expenseService.deleteExpense(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Gasto eliminado con éxito"));
    }
}
