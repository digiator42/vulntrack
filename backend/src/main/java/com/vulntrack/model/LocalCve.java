package com.vulntrack.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "local_cve", indexes = {
        @Index(name = "idx_cve_id", columnList = "cveId"),
        @Index(name = "idx_cpe_product", columnList = "cpeProduct"),
        @Index(name = "idx_last_modified", columnList = "lastModified")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalCve {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = false)
    private String cveId;

    @Column(columnDefinition = "TEXT")
    private String cpeProduct; // e.g., "snakeyaml"

    @Column(columnDefinition = "TEXT")
    private String cpeVendor; // e.g., "snakeyaml_project"

    @Column(columnDefinition = "TEXT")
    private String description;

    private Double cvssScore;

    @Column(length = 10)
    private String cvssVersion; // "2.0", "3.0", "3.1"

    private String severity;

    private String vectorString;

    private String versionStartIncluding;

    private String versionStartExcluding;

    private String versionEndIncluding;

    private String versionEndExcluding;

    private LocalDateTime publishedDate;

    private LocalDateTime lastModified;

    @Column(columnDefinition = "TEXT")
    private String rawJson; // Store original JSON for reference

    @Column(nullable = false)
    private LocalDateTime syncedAt;
}