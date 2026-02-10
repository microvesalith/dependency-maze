package org.vladv.dependency.dependencymaze.service;

import org.springframework.stereotype.Service;
import org.vladv.dependency.dependencymaze.dto.DependencyNode;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DependencyResolverService {
    private static final Logger logger = LoggerFactory.getLogger(DependencyResolverService.class);

    private String runMavenDependencyTree(Path workingDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("mvn", "dependency:tree", "-DoutputType=dot"); // Use DOT output
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Strip [INFO] prefix for frontend parsing
                if (line.startsWith("[INFO]")) {
                    line = line.substring(6).trim();
                }
                output.append(line).append("\n");
            }
        }
        int exitCode = process.waitFor();
        // Log the full Maven output to the console
        System.out.println("==== Maven dependency:tree output (stripped) ====");
        System.out.println(output);
        System.out.println("==== End of Maven output ====");
        if (exitCode != 0) {
            logger.error("Maven dependency:tree failed with exit code {}\nOutput:\n{}", exitCode, output);
            throw new RuntimeException("Maven dependency:tree failed with exit code " + exitCode + "\nOutput:\n" + output);
        }
        return output.toString();
    }

    private List<DependencyNode> parseDotDependencyTree(String dotOutput) {
        Map<String, DependencyNode> nodeMap = new HashMap<>();
        Map<String, List<String>> childrenMap = new HashMap<>();
        Set<String> allChildren = new java.util.HashSet<>();
        List<String> lines = List.of(dotOutput.split("\n"));
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("digraph") || line.startsWith("}") || line.isEmpty()) continue;
            if (line.startsWith("[INFO]")) line = line.substring(6).trim();
            if (line.contains("->")) {
                String[] parts = line.split("->");
                String parent = parts[0].replaceAll("[\" ]", "").trim();
                String child = parts[1].replaceAll("[\" ;]", "").trim();
                childrenMap.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
                allChildren.add(child);
                nodeMap.put(parent, parseDotNode(parent));
                nodeMap.put(child, parseDotNode(child));
            }
        }
        List<DependencyNode> roots = new ArrayList<>();
        for (String nodeId : nodeMap.keySet()) {
            if (!allChildren.contains(nodeId)) {
                roots.add(nodeMap.get(nodeId));
            }
        }
        for (DependencyNode root : roots) {
            buildDotTree(root, childrenMap, nodeMap, getDotNodeId(root), new HashSet<>());
        }
        return roots;
    }

    private String getDotNodeId(DependencyNode node) {
        // Compose the DOT nodeId as in the DOT output: groupId:artifactId:type:version:scope
        String base = node.getGroupId() + ":" + node.getArtifactId() + ":jar:" + node.getVersion();
        if (node.getScope() != null && !node.getScope().isEmpty()) {
            return base + ":" + node.getScope();
        }
        return base;
    }

    private void buildDotTree(DependencyNode node, Map<String, List<String>> childrenMap, Map<String, DependencyNode> nodeMap, String nodeId, Set<String> visited) {
        if (visited.contains(nodeId)) return;
        visited.add(nodeId);
        List<String> childrenIds = childrenMap.get(nodeId);
        if (childrenIds != null) {
            for (String childId : childrenIds) {
                DependencyNode child = nodeMap.get(childId);
                node.addChild(child);
                buildDotTree(child, childrenMap, nodeMap, childId, visited);
            }
        }
    }

    private DependencyNode parseDotNode(String nodeId) {
        // Example: org.example:my-app:jar:1.0:compile
        String[] parts = nodeId.split(":");
        String groupId = parts.length > 0 ? parts[0] : nodeId;
        String artifactId = parts.length > 1 ? parts[1] : "";
        String type = parts.length > 2 ? parts[2] : "jar";
        String version = parts.length > 3 ? parts[3] : "";
        String scope = parts.length > 4 ? parts[4] : "compile";
        DependencyNode node = new DependencyNode(groupId, artifactId, version, scope);
        // Optionally, store type if you want to extend DependencyNode
        return node;
    }

    public List<DependencyNode> resolveDependencies(InputStream pomInputStream) throws Exception {
        Path tempDir = Files.createTempDirectory("dependency-maze-");
        Path pomFile = tempDir.resolve("pom.xml");
        try {
            Files.copy(pomInputStream, pomFile);
            String output = runMavenDependencyTree(tempDir);
            return parseDotDependencyTree(output);
        } catch (Exception e) {
            logger.error("Exception while resolving dependencies", e);
            throw e;
        } finally {
            try {
                Files.deleteIfExists(pomFile);
                Files.deleteIfExists(tempDir);
            } catch (Exception e) {
                logger.warn("Exception while cleaning up temporary files", e);
            }
        }
    }

    public String resolveDependenciesRaw(InputStream pomInputStream) throws Exception {
        Path tempDir = Files.createTempDirectory("dependency-maze-");
        Path pomFile = tempDir.resolve("pom.xml");
        try {
            Files.copy(pomInputStream, pomFile);
            return runMavenDependencyTree(tempDir);
        } catch (Exception e) {
            logger.error("Exception while resolving dependencies", e);
            throw e;
        } finally {
            try {
                Files.deleteIfExists(pomFile);
                Files.deleteIfExists(tempDir);
            } catch (Exception e) {
                logger.warn("Exception while cleaning up temporary files", e);
            }
        }
    }
}
