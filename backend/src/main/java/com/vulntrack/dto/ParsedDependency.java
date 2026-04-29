package com.vulntrack.dto;

public record ParsedDependency(

    String name, // "groupId:artifactId" for Maven, "package-name" for others
    String version,
    String ecosystem, // "MAVEN", "NPM", "PYPI", "NUGET", "GRADLE"
    String manifestFile // Name of the manifest file (e.g., "pom.xml", "package.json")
) {
}