package com.vulntrack.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "cpe_cache")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpeCache {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String dependencyName;

    @Column(nullable = false, unique = true)
    private String ecosystem;

    @Column(nullable = false)
    private String cpeName; // The matching CPE string

    @Column(nullable = false)
    private LocalDateTime lastUpdated;
}