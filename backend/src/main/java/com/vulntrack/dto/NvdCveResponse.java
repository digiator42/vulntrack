package com.vulntrack.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NvdCveResponse {
    private int totalResults;
    private int startIndex;
    private int resultsPerPage;
    private List<Vulnerability> vulnerabilities;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Vulnerability {
        private Cve cve;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cve {
        private String id;
        private String published;
        private String lastModified;
        private List<Description> descriptions;
        private Metrics metrics;
        private List<Weakness> weaknesses; // for CWE
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Description {
        private String lang;
        private String value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metrics {
        @JsonProperty("cvssMetricV31")
        private List<CvssMetricV31> cvssMetricV31;
        
        @JsonProperty("cvssMetricV2")
        private List<CvssMetricV2> cvssMetricV2;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CvssMetricV31 {
        private String type; // "Primary" or "Secondary"
        private CvssDataV31 cvssData;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CvssDataV31 {
        private String version;
        private String vectorString;
        private double baseScore;
        private String baseSeverity;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CvssMetricV2 {
        private String type;
        private CvssDataV2 cvssData;
        private String baseSeverity; // V2 severity is at metric level
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CvssDataV2 {
        private String version;
        private String vectorString;
        private double baseScore;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Weakness {
        private List<Description> description; // Contains "CWE-79"
    }
}