package com.vulntrack.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "nvd_sync_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NvdSyncState {
    @Id
    private Integer id;

    /** Next NVD startIndex page to fetch (resume position). */
    @Column(nullable = false)
    private Integer lastStartIndex;

    /** Total catalog size reported by NVD on the last fetched page. */
    private Integer totalResults;

    /** IDLE, RUNNING. */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
