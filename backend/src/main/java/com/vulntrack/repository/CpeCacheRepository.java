package com.vulntrack.repository;

import com.vulntrack.model.CpeCache;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CpeCacheRepository extends JpaRepository<CpeCache, Long> {
    Optional<CpeCache> findByDependencyNameAndEcosystem(String dependencyName, String ecosystem);
}