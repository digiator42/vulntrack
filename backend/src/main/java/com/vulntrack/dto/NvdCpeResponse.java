package com.vulntrack.dto;

import java.util.List;

public record NvdCpeResponse(
    List<ProductWrapper> products,
    int totalResults
) {
    public record ProductWrapper(
        CpeDetails cpe
    ) {
    }

    public record CpeDetails(
        String cpeName,
        String lastModified,
        String created,
        List<Title> titles
    ) {
    }

    public record Title(
        String title,
        String lang
    ) {
    }
}