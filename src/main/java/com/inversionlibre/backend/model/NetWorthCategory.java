package com.inversionlibre.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;
import java.io.Serializable;

@Document(collection = "net_worth_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetWorthCategory implements Serializable {

    @Id
    private String id;

    private String userId;

    private String name;

    private CategoryType type;

    private String icon;

    private String color;

    private String groupName; // e.g. "Cuentas Bancarias", "Brokers", "Inmuebles"

    public enum CategoryType {
        ASSET,      // Activo
        LIABILITY   // Pasivo
    }
}
