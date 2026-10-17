package com.vulntrack.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "osv_advisory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OsvAdvisory {

    /** OSV identifier: GHSA-*, PYSEC-*, OSV-*, RUSTSEC-*. */
    @Id
    @Column(length = 64)
    private String id;

    /** Comma separated alias ids; CVE ids live here. */
    @Column(columnDefinition = "TEXT")
    private String aliases;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(length = 32)
    private String severity;

    @Column(name = "cvss_vector", columnDefinition = "TEXT")
    private String cvssVector;

    @Column(name = "cvss_score")
    private Double cvssScore;

    @Column(name = "cwe_ids", columnDefinition = "TEXT")
    private String cweIds;

    /** JSON array of affected package names. */
    @Column(name = "affected_refs", columnDefinition = "TEXT")
    private String affectedRefs;

    @Column(name = "published_date")
    private LocalDateTime publishedDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;
}