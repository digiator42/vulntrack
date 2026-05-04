package com.vulntrack.repository;

import com.vulntrack.model.PackageCpeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PackageCpeMappingRepository extends JpaRepository<PackageCpeMapping, Long> {

    Optional<PackageCpeMapping> findByProduct(String product);

    Optional<PackageCpeMapping> findByProductAndEcosystem(String product, String ecosystem);

    List<PackageCpeMapping> findByProductContainingIgnoreCase(String product);

    // Level 1: Exact product match (case-insensitive)
    @Query("SELECT m FROM PackageCpeMapping m WHERE LOWER(m.product) = LOWER(:product)")
    List<PackageCpeMapping> findByProductCaseInsensitive(String product);

    // Level 2: Exact vendor match (case-insensitive)
    @Query("SELECT m FROM PackageCpeMapping m WHERE LOWER(m.vendor) = LOWER(:vendor)")
    List<PackageCpeMapping> findByVendorCaseInsensitive(@Param("vendor") String vendor);

    // Level 4: Exact vendor + product match
    @Query("SELECT m FROM PackageCpeMapping m WHERE LOWER(m.vendor) = LOWER(:vendor) AND LOWER(m.product) = LOWER(:product)")
    Optional<PackageCpeMapping> findByVendorAndProduct(
        @Param("vendor") String vendor,
        @Param("product") String product
    );

    // Analytics: Most used mappings
    @Query("SELECT m FROM PackageCpeMapping m ORDER BY m.usageCount DESC LIMIT 100")
    List<PackageCpeMapping> findTopUsedMappings();

    List<PackageCpeMapping> findByVendor(String vendor);

    @Query("SELECT m FROM PackageCpeMapping m WHERE m.enabled = true ORDER BY m.usageCount DESC")
    List<PackageCpeMapping> findMostUsedMappings();

    @Query("SELECT m FROM PackageCpeMapping m WHERE LOWER(m.product) = LOWER(:product)")
    List<PackageCpeMapping> findAllByProductCaseInsensitive(@Param("product") String product);

    @Query("SELECT m FROM PackageCpeMapping m WHERE LOWER(m.vendor) IN :vendors")
    List<PackageCpeMapping> findByVendorInCaseInsensitive(@Param("vendors") List<String> vendors);
}