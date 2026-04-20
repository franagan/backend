package com.inversionlibre.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "net_worth_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetWorthEntry implements Serializable {

    @Id
    private String id;

    private String userId;

    private String categoryId;

    private LocalDate date; // Usaremos el día 1 de cada mes para normalizar

    private BigDecimal amount;

    private String notes;
}
