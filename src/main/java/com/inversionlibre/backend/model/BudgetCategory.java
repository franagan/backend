package com.inversionlibre.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "budget_categories")
public class BudgetCategory {

    @Id
    private String id;

    @Indexed
    @NotBlank(message = "El ID de usuario es obligatorio")
    private String userId;

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    private String name;

    @NotNull(message = "El monto gastado no puede ser nulo")
    @Builder.Default
    private Double spent = 0.0;

    @NotNull(message = "El límite del presupuesto no puede ser nulo")
    private Double limit;

    @Builder.Default
    private String color = "bg-blue-500"; // Default Tailwind color class

    private String description;

    private String subcategory;

    public enum BudgetPeriod {
        MONTHLY,
        ANNUAL
    }

    @Builder.Default
    private BudgetPeriod period = BudgetPeriod.MONTHLY;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
