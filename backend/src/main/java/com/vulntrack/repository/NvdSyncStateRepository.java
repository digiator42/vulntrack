package com.vulntrack.repository;

import com.vulntrack.model.NvdSyncState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

public interface NvdSyncStateRepository extends JpaRepository<NvdSyncState, Integer> {

    /**
     * Cross-instance mutex: exactly one JVM can flip the row to RUNNING.
     * A RUNNING row whose heartbeat is older than {@code staleBefore} belongs
     * to a dead container (deploy, recycle) and may be stolen.
     *
     * @return 1 when this caller now owns the sync, 0 otherwise
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE NvdSyncState s
        SET s.status = 'RUNNING', s.updatedAt = CURRENT_TIMESTAMP
        WHERE s.id = 1
          AND (s.status = 'IDLE'
               OR (s.status = 'RUNNING' AND s.updatedAt < :staleBefore))
        """)
    int tryAcquireLease(@Param("staleBefore") LocalDateTime staleBefore);
}

