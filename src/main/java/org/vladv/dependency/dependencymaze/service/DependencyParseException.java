package org.vladv.dependency.dependencymaze.service;

public class DependencyParseException extends RuntimeException {
    private final String mavenOutput;
    public DependencyParseException(String message, String mavenOutput) {
        super(message);
        this.mavenOutput = mavenOutput;
    }
    public String getMavenOutput() {
        return mavenOutput;
    }
}

