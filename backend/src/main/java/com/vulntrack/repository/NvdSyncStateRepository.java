package com.vulntrack.repository;

import com.vulntrack.model.NvdSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NvdSyncStateRepository extends JpaRepository<NvdSyncState, Integer> {
}
