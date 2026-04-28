package com.vulntrack.dto;

import lombok.Data;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
public class NvdCveResponse {
    private List<Vulnerability> vulnerabilities;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Vulnerability { private Cve cve; }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cve {
        private String id;
        private String sourceIdentifier;
        private String published;
        private String lastModified;
        private String vulnStatus;
        private List<Description> descriptions;
        private Metrics metrics;
        private List<Reference> references;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Description {
        private String lang;
        private String value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metrics { private List<CvssMetricV31> cvssMetricV31; }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CvssMetricV31 {
        private CvssData cvssData;
        private String severity;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CvssData {
        private String version;
        private String vectorString;
        private double baseScore;
        private String baseSeverity;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Reference {
        private String url;
        private String source;
    }
}