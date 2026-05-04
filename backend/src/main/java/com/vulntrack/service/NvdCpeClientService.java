package com.vulntrack.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vulntrack.dto.NvdCpeResponse;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NvdCpeClientService {

    private final RestClient restClient = RestClient.builder()
        .baseUrl("https://services.nvd.nist.gov/rest/json/cpes/2.0")
        .build();

    public List<String> searchCpeInNvd(String productKeyword) {
        try {
            NvdCpeResponse response = restClient.get()
                .uri(
                    uriBuilder -> uriBuilder
                        .queryParam("keywordSearch", productKeyword)

                        .build()
                )
                .retrieve()
                .body(NvdCpeResponse.class);

            if (response == null || response.products() == null)
                return List.of();

            return response.products().stream()
                .map(p -> resolveActiveCpeName(p.cpe()))
                .filter(name -> name != null && !name.isBlank())
                .distinct() // multiple deprecated CPEs may share the same deprecatedBy
                .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("NVD CPE API error for '{}': {}", productKeyword, e.getMessage());
        }
        return List.of();
    }

    private String resolveActiveCpeName(NvdCpeResponse.Cpe cpe) {
        if (!cpe.deprecated()) {
            return cpe.cpeName();
        }
        // Prefer the first deprecatedBy entry (NVD typically provides exactly one)
        if (cpe.deprecatedBy() != null && !cpe.deprecatedBy().isEmpty()) {
            String replacement = cpe.deprecatedBy().get(0).cpeName();
            log.debug("CPE deprecated: '{}' → resolved to '{}'", cpe.cpeName(), replacement);
            return replacement;
        }
        // Deprecated with no known replacement — discard
        log.debug("CPE deprecated with no replacement, discarding: '{}'", cpe.cpeName());
        return null;
    }
}