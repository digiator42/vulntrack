package com.vulntrack.dto;

import java.util.List;

public record NvdCpeResponse(
    List<CpeMatch> products,
    int totalResults
) {

    public static record CpeMatch(
        Cpe cpe
    ) {
    }

    public static record Cpe(
        String cpeName,
        String title,
        List<Reference> references,
        Deprecated deprecated
    ) {
    }

    public static record Reference(
        String href
    ) {
    }

    public static record Deprecated(
        boolean isDeprecated
    ) {
    }
}