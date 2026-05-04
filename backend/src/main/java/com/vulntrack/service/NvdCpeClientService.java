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

    /**
     * Searches NVD CPE Dictionary for a given product keyword. Returns the first
     * match's CPE Name (e.g., cpe:2.3:a:apache:log4j:*:*:*:*:*:*:*:*)
     */
    public String searchCpeInNvd(String productKeyword) {
        try {
            NvdCpeResponse response = restClient.get()
                .uri(
                    uriBuilder -> uriBuilder
                        .queryParam("keywordSearch", productKeyword)
                        .build()
                )
                .retrieve()
                .body(NvdCpeResponse.class);

            if (response != null && !response.products().isEmpty()) {
                return response.products().get(0).cpe().cpeName();
            }
        } catch (Exception e) {
            log.error("NVD API Error: {}", e.getMessage());
        }
        return null;
    }
}