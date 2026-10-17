package com.vulntrack.repository;

import com.vulntrack.entity.OsvAdvisory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OsvAdvisoryRepository extends JpaRepository<OsvAdvisory, String> {

    /** Look up advisories by CVE id stored in the aliases CSV. */
    @Query("""
            SELECT a FROM OsvAdvisory a
            WHERE a.aliases LIKE CONCAT('%', :cveId, '%')
            """)
    List<OsvAdvisory> findByAliasContaining(@Param("cveId") String cveId);
}