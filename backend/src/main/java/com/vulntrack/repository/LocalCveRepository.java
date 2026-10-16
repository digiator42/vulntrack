package com.vulntrack.repository;

import com.vulntrack.model.LocalCve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LocalCveRepository extends JpaRepository<LocalCve, Long> {

    Optional<LocalCve> findByCveId(String cveId);

    /** Batch existence check: one query per page instead of one per CVE. */
    @Query("SELECT c.cveId FROM LocalCve c WHERE c.cveId IN :ids")
    List<String> findExistingIds(@Param("ids") Collection<String> ids);

    List<LocalCve> findByCpeProduct(String cpeProduct);

    List<LocalCve> findByLastModifiedAfter(LocalDateTime lastModified);

    @Query("""
            SELECT c FROM LocalCve c
            WHERE c.cpeProduct = :product
            AND (
                (c.versionStartIncluding IS NULL OR :version >= c.versionStartIncluding)
                AND (c.versionStartExcluding IS NULL OR :version > c.versionStartExcluding)
                AND (c.versionEndIncluding IS NULL OR :version <= c.versionEndIncluding)
                AND (c.versionEndExcluding IS NULL OR :version < c.versionEndExcluding)
            )
        """)
    List<LocalCve> findVulnerabilitiesForVersion(
        @Param("product") String product,
        @Param("version") String version
    );

    @Query("SELECT MAX(c.lastModified) FROM LocalCve c")
    Optional<LocalDateTime> findLastModifiedDate();

    // Contains match (case insensitive)
    @Query("SELECT c FROM LocalCve c WHERE LOWER(c.cpeProduct) LIKE LOWER(CONCAT('%', :product, '%'))")
    List<LocalCve> findByCpeProductContaining(@Param("product") String product);

    // Match by product OR vendor
    @Query("""
            SELECT c FROM LocalCve c
            WHERE LOWER(c.cpeProduct) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.cpeVendor) LIKE LOWER(CONCAT('%', :keyword, '%'))
        """)
    List<LocalCve> findByProductOrVendorContaining(@Param("keyword") String keyword);

    List<LocalCve> findByCpeVendorContaining(String vendor);

    List<LocalCve> findByDescriptionContaining(String keyword);

    List<LocalCve> findByCpeVendorAndCpeProduct(String vendor, String product);

    long countByCpeProduct(String cpeProduct);

}