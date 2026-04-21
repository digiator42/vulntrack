package com.vulntrack.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vulntrack.model.CweEntry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CweService {

    private Map<String, CweEntry> cweMap;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try {
            var resource = new ClassPathResource("data/cwe_top25.json");
            List<CweEntry> cweList = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<List<CweEntry>>() {
                }
            );

            // Convert to Map for O(1) lookup by ID
            this.cweMap = cweList.stream()
                .collect(Collectors.toMap(CweEntry::getId, entry -> entry));

            log.info("Loaded {} CWE entries into memory", cweMap.size());
        } catch (IOException e) {
            log.error("Failed to load CWE data", e);
            this.cweMap = Map.of();
        }
    }

    /**
     * Get full CWE details by ID (e.g., "CWE-79")
     */
    public CweEntry getCweById(String cweId) {
        if (cweId == null || !cweId.startsWith("CWE-")) {
            return null;
        }
        return cweMap.get(cweId.toUpperCase());
    }

    /**
     * Get a list of all available CWEs for a dropdown menu
     */
    public List<CweEntry> getAllCwes() { return cweMap.values().stream().toList(); }
}