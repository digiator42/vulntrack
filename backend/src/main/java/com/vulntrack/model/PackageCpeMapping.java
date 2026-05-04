package com.vulntrack.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "package_cpe_mapping", indexes = {
        @Index(name = "idx_product", columnList = "product"),
        @Index(name = "idx_vendor", columnList = "vendor"),
        @Index(name = "idx_product_vendor", columnList = "product, vendor")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageCpeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String vendor;

    @Column(nullable = false, length = 255)
    private String product;

    @Column(name = "raw_cpe_name")
    private String rawCpeName; // Store "cpe:2.3:a:apache:log4j:*:*:*:*:*:*:*:*"

    @Column(length = 50)
    private String ecosystem; // MAVEN, NPM, PYPI, etc.

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private int usageCount; // Track how many times this mapping is used

    @Column(nullable = false)
    private boolean enabled; // Enable/disable mappings
}