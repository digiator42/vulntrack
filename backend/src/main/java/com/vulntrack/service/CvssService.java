package com.vulntrack.service;

import com.vulntrack.enums.Severity;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.HashMap;

@Service
public class CvssService {

    /**
     * Calculates the CVSS v3.1 Base Score from a Vector String. Example Vector:
     * "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H"
     */
    public double calculateBaseScore(String vector) {
        if (vector == null || vector.isEmpty()) {
            return 0.0;
        }

        Map<String, String> metrics = parseVector(vector);

        // 1. Extract Metrics with Defaults (as per CVSS spec, missing metrics are not
        // valid for Base,
        // but we provide safe defaults for UX purposes)
        String av = metrics.getOrDefault("AV", "N"); // Attack Vector
        String ac = metrics.getOrDefault("AC", "L"); // Attack Complexity
        String pr = metrics.getOrDefault("PR", "N"); // Privileges Required
        String ui = metrics.getOrDefault("UI", "N"); // User Interaction
        String s = metrics.getOrDefault("S", "U"); // Scope
        String c = metrics.getOrDefault("C", "N"); // Confidentiality
        String i = metrics.getOrDefault("I", "N"); // Integrity
        String a = metrics.getOrDefault("A", "N"); // Availability

        // 2. Calculate Metric Values
        double avVal = getMetricValue("AV", av);
        double acVal = getMetricValue("AC", ac);
        double prVal = getMetricValue(pr, s); // PR depends on Scope
        double uiVal = getMetricValue("UI", ui);
        double cVal = getMetricValue("CIA", c);
        double iVal = getMetricValue("CIA", i);
        double aVal = getMetricValue("CIA", a);

        // 3. Calculate Impact Sub-Score (ISS)
        // ISS = 1 - [(1-C) * (1-I) * (1-A)]
        double iss = 1 - ((1 - cVal) * (1 - iVal) * (1 - aVal));

        // 4. Calculate Impact Score based on Scope
        double impact;
        if ("U".equals(s)) { // Unchanged Scope
            impact = 6.42 * iss;
        } else { // Changed Scope
            impact = 7.52 * (iss - 0.029) - 3.25 * Math.pow(iss - 0.02, 15);
        }

        // 5. Calculate Exploitability Sub-Score
        // 8.22 * AV * AC * PR * UI
        double exploitability = 8.22 * avVal * acVal * prVal * uiVal;

        // 6. Calculate Base Score
        double baseScore;
        if (impact <= 0) {
            baseScore = 0.0;
        } else if ("U".equals(s)) {
            baseScore = roundUp(Math.min(impact + exploitability, 10));
        } else {
            baseScore = roundUp(Math.min(1.08 * (impact + exploitability), 10));
        }

        return baseScore;
    }

    /**
     * Returns the numeric weight for a specific metric value.
     */
    private double getMetricValue(String type, String val) {
        return switch (type) {
            case "AV" -> switch (val) {
                case "N" -> 0.85; // Network
                case "A" -> 0.62; // Adjacent
                case "L" -> 0.55; // Local
                case "P" -> 0.2; // Physical
                default -> 0.85;
            };
            case "AC" -> switch (val) {
                case "L" -> 0.77; // Low
                case "H" -> 0.44; // High
                default -> 0.77;
            };
            case "PR" -> {
                // Privileges Required depends on Scope (S)
                yield switch (val) {
                    case "N" -> 0.85; // None
                    case "L" -> 0.62; // Low
                    case "H" -> 0.27; // High
                    default -> 0.85;
                };
            }
            case "UI" -> switch (val) {
                case "N" -> 0.85; // None
                case "R" -> 0.62; // Required
                default -> 0.85;
            };
            case "CIA" -> switch (val) {
                case "H" -> 0.56; // High
                case "L" -> 0.22; // Low
                case "N" -> 0.0; // None
                default -> 0.0;
            };
            default -> 0.0;
        };
    }

    /**
     * CVSS requires rounding up to 1 decimal place. Example: 7.21 becomes 7.3
     */
    private double roundUp(double score) {
        int intScore = (int) Math.ceil(score * 10);
        return (double) intScore / 10;
    }

    private Map<String, String> parseVector(String vector) {
        Map<String, String> map = new HashMap<>();
        // Remove prefix if present
        String cleanVector = vector.startsWith("CVSS:3.1/") ? vector.substring(9) : vector;

        String[] parts = cleanVector.split("/");
        for (String part : parts) {
            String[] kv = part.split(":");
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }

    public Severity getSeverity(double score) {
        if (score >= 9.0)
            return Severity.CRITICAL;
        if (score >= 7.0)
            return Severity.HIGH;
        if (score >= 4.0)
            return Severity.MEDIUM;
        return Severity.LOW;
    }
}