package org.vladv.dependency.dependencymaze.dto;

import java.util.ArrayList;
import java.util.List;

public class DependencyNode {
    private String groupId;
    private String artifactId;
    private String version;
    private String scope;
    private boolean conflict;
    private String conflictVersion;
    private List<DependencyNode> children = new ArrayList<>();

    public DependencyNode() {
    }

    public DependencyNode(String groupId, String artifactId, String version, String scope) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = scope;
        this.conflict = false;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isConflict() {
        return conflict;
    }

    public void setConflict(boolean conflict) {
        this.conflict = conflict;
    }

    public String getConflictVersion() {
        return conflictVersion;
    }

    public void setConflictVersion(String conflictVersion) {
        this.conflictVersion = conflictVersion;
    }

    public List<DependencyNode> getChildren() {
        return children;
    }

    public void setChildren(List<DependencyNode> children) {
        this.children = children;
    }

    public void addChild(DependencyNode child) {
        this.children.add(child);
    }

    public String getCoordinates() {
        return groupId + ":" + artifactId;
    }
}
