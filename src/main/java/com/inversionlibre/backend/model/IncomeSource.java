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
@Document(collection = "income_sources")
public class IncomeSource {

    @Id
    private String id;

    @Indexed
    @NotBlank(message = "El ID de usuario es obligatorio")
    private String userId;

    @NotBlank(message = "El nombre de la fuente de ingresos es obligatorio")
    private String name;

    @NotNull(message = "El monto no puede ser nulo")
    @Builder.Default
    private Double amount = 0.0;

    @Builder.Default
    private String frequency = "MONTHLY"; // MONTHLY, ANNUALLY

    @Builder.Default
    private String color = "bg-green-500"; 

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
