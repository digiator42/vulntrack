package com.vulntrack.service;

import com.vulntrack.model.PackageCpeMapping;
import com.vulntrack.repository.PackageCpeMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CpeMappingImportService {

    private final PackageCpeMappingRepository mappingRepository;

    @PostConstruct
    @Transactional
    public void importCsvMappings() {
        if (mappingRepository.count() > 0) {
            log.info(
                "CPE mappings already exist ({} records), skipping import",
                mappingRepository.count()
            );
            return;
        }

        log.info("Importing CPE mappings from CSV...");

        try {
            ClassPathResource resource = new ClassPathResource("/data/cpes.csv");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
            );

            // Read header
            String headerLine = reader.readLine();
            if (headerLine == null) {
                log.error("Empty CSV file");
                return;
            }

            List<PackageCpeMapping> mappings = new ArrayList<>();
            int count = 0;
            int errorCount = 0;
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                try {
                    // Parse line manually
                    String[] parts = parseCsvLine(line);
                    if (parts.length >= 2) {
                        String vendor = cleanValue(parts[0]);
                        String product = cleanValue(parts[1]);

                        if (vendor != null && !vendor.isEmpty() && product != null
                            && !product.isEmpty()) {
                            PackageCpeMapping mapping = PackageCpeMapping.builder()
                                .vendor(vendor.trim().toLowerCase())
                                .product(product.trim().toLowerCase())
                                .ecosystem(null)
                                .createdAt(LocalDateTime.now())
                                .usageCount(0)
                                .enabled(true)
                                .build();

                            mappings.add(mapping);
                            count++;
                        }
                    }

                    // Batch save every 1000 records
                    if (mappings.size() >= 1000) {
                        mappingRepository.saveAll(mappings);
                        log.info("Saved {} mappings so far...", count);
                        mappings.clear();
                    }

                } catch (Exception e) {
                    errorCount++;
                    if (errorCount <= 10) {
                        log.warn("Failed to parse line {}: {}", lineNumber, e.getMessage());
                        log.warn("Problem line: {}", line);
                    }
                }
            }

            // Save remaining
            if (!mappings.isEmpty()) {
                mappingRepository.saveAll(mappings);
            }

            reader.close();

            log.info(
                "Successfully imported {} CPE mappings ({} errors skipped)", count, errorCount
            );

        } catch (Exception e) {
            log.error("Failed to import CPE mappings", e);
        }
    }

    /**
     * Manually parse a CSV line handling quotes and escaped quotes
     */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escapeNext = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (escapeNext) {
                current.append(c);
                escapeNext = false;
                continue;
            }

            if (c == '\\') {
                escapeNext = true;
                continue;
            }

            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }

            if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
                continue;
            }

            current.append(c);
        }

        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    /**
     * Clean quoted value
     */
    private String cleanValue(String value) {
        if (value == null)
            return null;
        // Remove surrounding quotes if present
        String cleaned = value.trim();
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        return cleaned;
    }
}