package com.vulntrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsedDependency {
    private String name;      // "groupId:artifactId" for Maven, "package-name" for others
    private String version;
    private String ecosystem; // "MAVEN", "NPM", "PYPI", "NUGET", "GRADLE"
}