package com.vulntrack.service;

import com.vulntrack.dto.ParsedDependency;
import com.vulntrack.model.PackageCpeMapping;
import com.vulntrack.repository.PackageCpeMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DependencyNormalizer {
    
    private final PackageCpeMappingRepository mappingRepository;

    // Common suffixes to strip for fallback matching
    private static final List<String> COMMON_SUFFIXES = Arrays.asList(
        "-core", "-api", "-client", "-server", "-common", "-util", "-utils",
        "-spring-boot-starter", "-starter", "-autoconfigure", "-web", "-jdbc",
        "-reactive", "-async", "-sync", "-impl", "-runtime"
    );

    public NormalizationResult normalize(ParsedDependency dep) {
        String packageName = extractPackageName(dep);
        String ecosystem = dep.ecosystem();
        String groupId = extractGroupId(dep); // For Maven

        log.debug("Normalizing: {} (ecosystem: {}, groupId: {})", packageName, ecosystem, groupId);

        // === LEVEL 1: Exact Product Match ===
        Optional<PackageCpeMapping> exact = mappingRepository.findByProductCaseInsensitive(packageName);
        if (exact.isPresent()) {
            return buildResult(exact.get(), packageName, "Level 1: Exact product match");
        }

        // === LEVEL 2: Vendor Match → Filter Products Containing Package Name ===
        String inferredVendor = inferVendor(packageName, groupId, ecosystem);
        if (inferredVendor != null) {
            List<PackageCpeMapping> vendorMatches = mappingRepository.findByVendorCaseInsensitive(inferredVendor);
            
            // Filter: product contains packageName OR packageName contains product
            Optional<PackageCpeMapping> bestVendorMatch = vendorMatches.stream()
                .filter(m -> 
                    m.getProduct().toLowerCase().contains(packageName.toLowerCase()) ||
                    packageName.toLowerCase().contains(m.getProduct().toLowerCase())
                )
                .findFirst();
                
            if (bestVendorMatch.isPresent()) {
                return buildResult(bestVendorMatch.get(), packageName, 
                    "Level 2: Vendor match + product containment");
            }
        }

        // === LEVEL 3: Strip Suffixes → Retry Exact Match ===
        String stripped = stripCommonSuffixes(packageName);
        if (!stripped.equals(packageName)) {
            Optional<PackageCpeMapping> strippedMatch = mappingRepository.findByProductCaseInsensitive(stripped);
            if (strippedMatch.isPresent()) {
                return buildResult(strippedMatch.get(), packageName, 
                    "Level 3: Suffix stripped → exact match: " + stripped);
            }
            
            // Also try vendor + stripped
            if (inferredVendor != null) {
                List<PackageCpeMapping> vendorStripped = mappingRepository.findByVendorCaseInsensitive(inferredVendor);
                Optional<PackageCpeMapping> best = vendorStripped.stream()
                    .filter(m -> m.getProduct().equalsIgnoreCase(stripped))
                    .findFirst();
                if (best.isPresent()) {
                    return buildResult(best.get(), packageName, 
                        "Level 3b: Vendor + stripped exact match");
                }
            }
        }

        // === LEVEL 4: Group ID + Artifact Hint (Maven) ===
        if ("MAVEN".equalsIgnoreCase(ecosystem) && groupId != null) {
            String groupIdVendor = inferVendorFromGroupId(groupId);
            if (groupIdVendor != null) {
                // Try: vendor from groupId + original artifact
                Optional<PackageCpeMapping> groupIdMatch = mappingRepository
                    .findByVendorAndProduct(groupIdVendor, packageName);
                if (groupIdMatch.isPresent()) {
                    return buildResult(groupIdMatch.get(), packageName, "Level 4: Group ID hint");
                }
                
                // Try: vendor from groupId + stripped artifact
                if (!stripped.equals(packageName)) {
                    Optional<PackageCpeMapping> groupIdStripped = mappingRepository
                        .findByVendorAndProduct(groupIdVendor, stripped);
                    if (groupIdStripped.isPresent()) {
                        return buildResult(groupIdStripped.get(), packageName, 
                            "Level 4b: Group ID + stripped");
                    }
                }
            }
        }

        // === LEVEL 5: Fallback to Cleaned Name ===
        String cleaned = cleanPackageName(packageName, ecosystem);
        log.debug("Fallback: {} -> {}", packageName, cleaned);
        return new NormalizationResult(cleaned, cleaned, packageName, ecosystem, "Level 5: Fallback cleanup");
    }

    private String inferVendor(String packageName, String groupId, String ecosystem) {
        // Try to infer vendor from known patterns or groupId
        if ("MAVEN".equalsIgnoreCase(ecosystem) && groupId != null) {
            return inferVendorFromGroupId(groupId);
        }
        
        // NPM scope hints
        if ("NPM".equalsIgnoreCase(ecosystem) && packageName.startsWith("@")) {
            String scope = packageName.split("/")[0].substring(1);
            return scope.toLowerCase();
        }
        
        // Common vendor keywords in package name
        if (packageName.contains("apache")) return "apache";
        if (packageName.contains("spring")) return "spring";
        if (packageName.contains("google")) return "google";
        if (packageName.contains("amazon") || packageName.contains("aws")) return "amazon";
        if (packageName.contains("microsoft")) return "microsoft";
        if (packageName.contains("oracle")) return "oracle";
        if (packageName.contains("eclipse")) return "eclipse";
        
        return null;
    }

    private String inferVendorFromGroupId(String groupId) {
        // Common Maven groupId → CPE vendor mappings
        if (groupId.contains("apache")) return "apache";
        if (groupId.contains("springframework") || groupId.contains("spring")) return "spring";
        if (groupId.contains("google")) return "google";
        if (groupId.contains("amazonaws") || groupId.contains("aws")) return "amazon";
        if (groupId.contains("microsoft")) return "microsoft";
        if (groupId.contains("oracle")) return "oracle";
        if (groupId.contains("eclipse")) return "eclipse";
        if (groupId.contains("jetbrains")) return "jetbrains";
        if (groupId.contains("square")) return "square";
        
        // Fallback: last part of groupId
        String[] parts = groupId.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1].toLowerCase() : null;
    }

    private String stripCommonSuffixes(String name) {
        String result = name.toLowerCase();
        for (String suffix : COMMON_SUFFIXES) {
            if (result.endsWith(suffix)) {
                return result.substring(0, result.length() - suffix.length());
            }
        }
        return result;
    }

    private String extractPackageName(ParsedDependency dep) {
        String name = dep.name();
        if ("MAVEN".equalsIgnoreCase(dep.ecosystem()) && name.contains(":")) {
            return name.split(":")[1]; // artifactId
        }
        if ("NPM".equalsIgnoreCase(dep.ecosystem()) && name.startsWith("@")) {
            return name.substring(1).replace("/", "_"); // @scope/pkg -> scope_pkg
        }
        return name.toLowerCase();
    }

    private String extractGroupId(ParsedDependency dep) {
        if ("MAVEN".equalsIgnoreCase(dep.ecosystem()) && dep.name().contains(":")) {
            return dep.name().split(":")[0];
        }
        return null;
    }

    private String cleanPackageName(String name, String ecosystem) {
        String cleaned = name.toLowerCase().trim()
            .replaceAll("[^a-z0-9_-]", "_")
            .replaceAll("_+", "_");
        
        if ("NPM".equalsIgnoreCase(ecosystem)) {
            cleaned = cleaned.replace("/", "_");
        }
        return cleaned;
    }

    private NormalizationResult buildResult(PackageCpeMapping mapping, String original, String reason) {
        // Increment usage count for analytics
        mapping.setUsageCount(mapping.getUsageCount() + 1);
        mappingRepository.save(mapping);
        
        log.info("✓ Normalized '{}' -> {}:{} via {}", 
            original, mapping.getVendor(), mapping.getProduct(), reason);
        
        return new NormalizationResult(
            mapping.getProduct(),
            mapping.getVendor(),
            original,
            mapping.getEcosystem(), // Store original ecosystem from mapping
            reason
        );
    }

    public record NormalizationResult(
        String cpeProduct,
        String cpeVendor,
        String originalName,
        String mappedEcosystem,
        String matchReason
    ) {}
}