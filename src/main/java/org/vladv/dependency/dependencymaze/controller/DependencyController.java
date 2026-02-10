package org.vladv.dependency.dependencymaze.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.vladv.dependency.dependencymaze.dto.DependencyNode;
import org.vladv.dependency.dependencymaze.service.DependencyParseException;
import org.vladv.dependency.dependencymaze.service.DependencyResolverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.io.InputStream;

@RestController
@RequestMapping("/api")
public class DependencyController {

    private final DependencyResolverService dependencyResolverService;

    private static final Logger logger = LoggerFactory.getLogger(DependencyController.class);

    public DependencyController(DependencyResolverService dependencyResolverService) {
        this.dependencyResolverService = dependencyResolverService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzePom(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please upload a pom.xml file");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".xml")) {
            return ResponseEntity.badRequest().body("Please upload a valid XML file");
        }

        try {
            List<DependencyNode> dependencyTrees = dependencyResolverService.resolveDependencies(file.getInputStream());
            return ResponseEntity.ok(dependencyTrees);
        } catch (DependencyParseException e) {
            logger.error("Error analyzing pom.xml", e);
            return ResponseEntity.internalServerError()
                    .body("Error analyzing pom.xml: " + e.getMessage() + "\n\nMaven Output:\n" + e.getMavenOutput());
        } catch (Exception e) {
            logger.error("Error analyzing pom.xml", e);
            return ResponseEntity.internalServerError()
                    .body("Error analyzing pom.xml: " + e.getMessage());
        }
    }

    @PostMapping(value = "/analyze-raw", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> analyzeRaw(@RequestParam("file") MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            String dot = dependencyResolverService.resolveDependenciesRaw(is);
            return ResponseEntity.ok(dot);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
