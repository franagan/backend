package com.inversionlibre.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "expenses")
public class Expense {

    @Id
    private String id;

    @Indexed
    @NotBlank(message = "El ID de usuario es obligatorio")
    private String userId;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate date;

    @NotBlank(message = "El concepto es obligatorio")
    private String concept;

    @NotNull(message = "El monto no puede ser nulo")
    private Double amount;

    private String category; // Optional: linked to BudgetCategory name

    private String subcategory; // Optional: linked to BudgetCategory subcategory

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
