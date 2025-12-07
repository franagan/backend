package com.inversionlibre.backend.controller;

import com.inversionlibre.backend.dto.ApiResponse;
import com.inversionlibre.backend.model.IncomeSource;
import com.inversionlibre.backend.model.User;
import com.inversionlibre.backend.service.IncomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
public class IncomeController {

    private final IncomeService incomeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<IncomeSource>>> getUserIncomes(@AuthenticationPrincipal User user) {
        List<IncomeSource> incomes = incomeService.getUserIncomes(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Ingresos cargados correctamente", incomes));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IncomeSource>> createIncome(@AuthenticationPrincipal User user, @RequestBody IncomeSource income) {
        IncomeSource createdIncome = incomeService.createIncome(income, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Ingreso creado con éxito", createdIncome));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<IncomeSource>> updateIncome(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody IncomeSource income) {
        IncomeSource updatedIncome = incomeService.updateIncome(id, income, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Ingreso actualizado con éxito", updatedIncome));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteIncome(@AuthenticationPrincipal User user, @PathVariable String id) {
        incomeService.deleteIncome(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Ingreso eliminado con éxito"));
    }
}
