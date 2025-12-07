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
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "savings_goals")
public class SavingsGoal {

    @Id
    private String id;

    @Indexed
    @NotBlank(message = "El ID de usuario es obligatorio")
    private String userId;

    @NotBlank(message = "El nombre del objetivo es obligatorio")
    private String name;

    @NotNull(message = "El monto actual no puede ser nulo")
    @Builder.Default
    private Double currentAmount = 0.0;

    @NotNull(message = "El objetivo de ahorro no puede ser nulo")
    private Double targetAmount;

    @Builder.Default
    private String icon = "💰";

    private LocalDate deadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
