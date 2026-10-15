package com.vulntrack.service;

import com.vulntrack.model.PackageCpeMapping;
import com.vulntrack.repository.PackageCpeMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CpeMappingImportService implements ApplicationRunner {

    private final PackageCpeMappingRepository mappingRepository;
    private final JdbcTemplate jdbcTemplate;

    // Multi-row INSERT chunk size. With JPA IDENTITY ids Hibernate cannot
    // batch, so saveAll() issued one INSERT per row -- an hours-long import
    // over a remote (Neon) database. 500 rows => 7 params x 500 values.
    private static final int CHUNK = 500;

    // Runs after Tomcat has bound the port (was @PostConstruct, which
    // blocked context refresh and kept the port closed during the import).
    @Override
    public void run(ApplicationArguments args) {
        importCsvMappings();
    }

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

                    // Flush every CHUNK rows as a single multi-row INSERT
                    if (mappings.size() >= CHUNK) {
                        bulkInsert(mappings);
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
                bulkInsert(mappings);
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
     * Insert a chunk of mappings with a single multi-row INSERT statement.
     * repository.saveAll() cannot batch here: with IDENTITY generation
     * Hibernate must execute each INSERT separately, which is one network
     * round trip per row and unbearably slow against a remote database.
     */
    private void bulkInsert(List<PackageCpeMapping> mappings) {
        if (mappings.isEmpty()) {
            return;
        }

        StringBuilder sql = new StringBuilder(
            "INSERT INTO package_cpe_mapping "
            + "(vendor, product, raw_cpe_name, ecosystem, created_at, usage_count, enabled) VALUES ");
        List<Object> params = new ArrayList<>();

        for (int i = 0; i < mappings.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("(?, ?, ?, ?, ?, ?, ?)");

            PackageCpeMapping m = mappings.get(i);
            params.add(m.getVendor());
            params.add(m.getProduct());
            params.add(m.getRawCpeName());
            params.add(m.getEcosystem());
            params.add(Timestamp.valueOf(m.getCreatedAt()));
            params.add(m.getUsageCount());
            params.add(m.isEnabled());
        }

        jdbcTemplate.update(sql.toString(), params.toArray());
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