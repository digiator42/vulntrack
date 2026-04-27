package com.vulntrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.vulntrack.dto.ParsedDependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DependencyParserService {

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final TomlMapper tomlMapper = new TomlMapper();

    /**
     * Parse a manifest file based on its filename.
     * 
     * @param fileContent input stream of the file
     * @param fileName    e.g., "pom.xml", "package.json", "requirements.txt",
     *                    "build.gradle", "Pipfile", "project.csproj"
     * @return list of dependencies
     */
    public List<ParsedDependency> parse(InputStream fileContent, String fileName) throws Exception {
        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith("pom.xml")) {
            return parsePom(fileContent);
        } else if (lowerName.endsWith("package.json")) {
            return parsePackageJson(fileContent);
        } else if (lowerName.endsWith("requirements.txt")) {
            return parseRequirementsTxt(fileContent);
        } else if (lowerName.endsWith("pipfile")) {
            return parsePipfile(fileContent);
        } else if (lowerName.endsWith("build.gradle")) {
            return parseBuildGradle(fileContent);
        } else if (lowerName.endsWith(".csproj")) {
            return parseCsProj(fileContent);
        } else {
            throw new IllegalArgumentException("Unsupported manifest file: " + fileName);
        }
    }

    // ------------------- Maven pom.xml -------------------
    private List<ParsedDependency> parsePom(InputStream input) throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(input);
        List<ParsedDependency> deps = new ArrayList<>();

        Properties properties = model.getProperties();

        // Direct dependencies
        if (model.getDependencies() != null) {
            for (var dep : model.getDependencies()) {
                String groupId = dep.getGroupId();
                String artifactId = dep.getArtifactId();

                // Skip if missing critical info
                if (groupId == null || artifactId == null) {
                    continue;
                }

                String name = groupId + ":" + artifactId;
                String version = dep.getVersion();

                // Resolve property placeholders like ${spring.version}
                if (version != null && version.startsWith("${") && version.endsWith("}")) {
                    String propertyKey = version.substring(2, version.length() - 1);

                    if (properties != null && propertyKey != null) {
                        String resolvedVersion = properties.getProperty(propertyKey);
                        if (resolvedVersion != null) {
                            version = resolvedVersion;
                        }
                    }
                }

                // Only add if we have a valid version
                if (version != null && !version.isEmpty()) {
                    deps.add(new ParsedDependency(name, version, "MAVEN"));
                }
            }
        }

        // Managed dependencies (dependencyManagement)
        if (model.getDependencyManagement() != null
            && model.getDependencyManagement().getDependencies() != null) {
            model.getDependencyManagement().getDependencies().forEach(dep -> {
                String name = dep.getGroupId() + ":" + dep.getArtifactId();
                String version = dep.getVersion();
                if (version != null && !version.isEmpty()) {
                    // Avoid duplicates
                    boolean exists = deps.stream().anyMatch(d -> d.getName().equals(name));
                    if (!exists) {
                        deps.add(new ParsedDependency(name, version, "MAVEN"));
                    }
                }
            });
        }
        return deps;
    }

    // ------------------- package.json (NPM) -------------------
    private List<ParsedDependency> parsePackageJson(InputStream input) throws Exception {
        JsonNode root = jsonMapper.readTree(input);
        List<ParsedDependency> deps = new ArrayList<>();

        // dependencies + devDependencies
        extractNpmDeps(root.get("dependencies"), deps);
        extractNpmDeps(root.get("devDependencies"), deps);
        return deps;
    }

    private void extractNpmDeps(JsonNode depsNode, List<ParsedDependency> out) {
        if (depsNode == null || !depsNode.isObject())
            return;
        depsNode.fields().forEachRemaining(entry -> {
            String name = entry.getKey();
            String version = entry.getValue().asText();
            out.add(new ParsedDependency(name, version, "NPM"));
        });
    }

    // ------------------- requirements.txt (Python) -------------------
    private List<ParsedDependency> parseRequirementsTxt(InputStream input) throws Exception {
        List<ParsedDependency> deps = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(input, StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                // Handle: package==1.2.3, package>=1.0, package@git+... etc.
                int eqIdx = line.indexOf("==");
                int gtIdx = line.indexOf(">=");
                int leIdx = line.indexOf("<=");
                int sep = -1;
                if (eqIdx > 0)
                    sep = eqIdx;
                else if (gtIdx > 0)
                    sep = gtIdx;
                else if (leIdx > 0)
                    sep = leIdx;
                if (sep > 0) {
                    String name = line.substring(0, sep).trim();
                    String version = line.substring(sep + 2).trim();
                    deps.add(new ParsedDependency(name, version, "PYPI"));
                } else {
                    // no version specified – skip or keep with null version? We'll skip.
                }
            }
        }
        return deps;
    }

    // ------------------- Pipfile (Python) TOML -------------------
    private List<ParsedDependency> parsePipfile(InputStream input) throws Exception {
        JsonNode root = tomlMapper.readTree(input);
        List<ParsedDependency> deps = new ArrayList<>();

        // pipfile has [packages] and [dev-packages]
        extractPipfileSection(root.get("packages"), deps);
        extractPipfileSection(root.get("dev-packages"), deps);
        return deps;
    }

    private void extractPipfileSection(JsonNode section, List<ParsedDependency> out) {
        if (section == null || !section.isObject())
            return;
        section.fields().forEachRemaining(entry -> {
            String name = entry.getKey();
            JsonNode value = entry.getValue();
            String version = null;
            if (value.isTextual()) {
                version = value.asText();
            } else if (value.has("version")) {
                version = value.get("version").asText();
            }
            if (version != null && !version.isEmpty()) {
                out.add(new ParsedDependency(name, version, "PYPI"));
            }
        });
    }

    // ------------------- build.gradle (Gradle) -------------------
    // Simple regex-based. Supports: implementation 'group:artifact:version'
    // implementation group: 'group', name: 'artifact', version: 'version'
    private List<ParsedDependency> parseBuildGradle(InputStream input) throws Exception {
        String content;
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(input, StandardCharsets.UTF_8)
        )) {
            content = reader.lines().collect(Collectors.joining("\n"));
        }
        List<ParsedDependency> deps = new ArrayList<>();
        // Pattern 1: implementation 'com.example:lib:1.0.0'
        Pattern patternShort = Pattern
            .compile("(?:implementation|api|compile|runtimeOnly)\\s+'([^']+)':([^']+)'");
        Matcher m = patternShort.matcher(content);
        while (m.find()) {
            String groupArtifact = m.group(1);
            String version = m.group(2);
            deps.add(new ParsedDependency(groupArtifact, version, "GRADLE"));
        }
        // Pattern 2: implementation group: 'com.example', name: 'lib', version: '1.0.0'
        Pattern patternLong = Pattern.compile(
            "(?:implementation|api|compile|runtimeOnly)\\s+group:\\s*'([^']+)',\\s*name:\\s*'([^']+)',\\s*version:\\s*'([^']+)'"
        );
        m = patternLong.matcher(content);
        while (m.find()) {
            String group = m.group(1);
            String artifact = m.group(2);
            String version = m.group(3);
            deps.add(new ParsedDependency(group + ":" + artifact, version, "GRADLE"));
        }
        return deps;
    }

    // ------------------- .csproj (NuGet) -------------------
    private List<ParsedDependency> parseCsProj(InputStream input) throws Exception {
        Document doc = Jsoup.parse(input, StandardCharsets.UTF_8.name(), "");
        Elements packageRefs = doc.select("PackageReference");
        List<ParsedDependency> deps = new ArrayList<>();
        for (Element ref : packageRefs) {
            String name = ref.attr("Include");
            String version = ref.attr("Version");
            if (name != null && !name.isEmpty() && version != null && !version.isEmpty()) {
                deps.add(new ParsedDependency(name, version, "NUGET"));
            }
        }
        return deps;
    }
}