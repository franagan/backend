package com.inversionlibre.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO para reporte de integridad de datos después de traducción/mapeo
 * 
 * @author Francisco Palero
 * @version 1.0
 * @since 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataIntegrityReport {
    
    private ValidationSummary summary;
    private List<ValidationIssue> issues;
    private CountComparison counts;
    private RelationshipValidation relationships;
    private DataLossValidation dataLoss;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationSummary {
        private Boolean isValid;
        private Integer totalIssues;
        private Integer criticalIssues;
        private Integer warnings;
        private String validationTimestamp;
        private Long validationDurationMs;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationIssue {
        private String entityType;
        private String entityId;
        private String issueType; // MISSING_DATA, BROKEN_RELATIONSHIP, COUNT_MISMATCH, DATA_LOSS
        private String severity; // CRITICAL, WARNING, INFO
        private String description;
        private String field;
        private Object expectedValue;
        private Object actualValue;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CountComparison {
        private EntityCount before;
        private EntityCount after;
        private Boolean countsMatch;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class EntityCount {
            private Long users;
            private Long portfolios;
            private Long investments;
            private Long transactions;
            private Long stocks;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RelationshipValidation {
        private Boolean allRelationshipsValid;
        private Integer totalRelationships;
        private Integer validRelationships;
        private Integer brokenRelationships;
        private List<BrokenRelationship> brokenRelationshipsList;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class BrokenRelationship {
            private String fromEntity;
            private String fromId;
            private String toEntity;
            private String toId;
            private String relationshipType;
            private String description;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataLossValidation {
        private Boolean dataLossDetected;
        private Integer lostRecords;
        private List<LostRecord> lostRecordsList;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class LostRecord {
            private String entityType;
            private String entityId;
            private String reason;
        }
    }
    
    /**
     * Añade un issue al reporte
     */
    public void addIssue(ValidationIssue issue) {
        if (this.issues == null) {
            this.issues = new ArrayList<>();
        }
        this.issues.add(issue);
    }
    
    /**
     * Verifica si hay issues críticos
     */
    public boolean hasCriticalIssues() {
        return issues != null && issues.stream()
            .anyMatch(issue -> "CRITICAL".equals(issue.getSeverity()));
    }
}







