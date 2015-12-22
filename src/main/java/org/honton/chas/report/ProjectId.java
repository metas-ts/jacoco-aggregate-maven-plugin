package org.honton.chas.report;

import org.apache.maven.project.MavenProject;

/**
 * Project identity is group:artifact.  This class is usable as a key in a HashMap.
 */
class ProjectId {
    private final String id;

    ProjectId(MavenProject project) {
        id = project.getGroupId() +":" +project.getArtifactId();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return id.equals(((ProjectId) obj).id);
    }

    @Override
    public String toString() {
        return id;
    }
}