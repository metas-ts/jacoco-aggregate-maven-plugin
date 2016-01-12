package org.honton.chas.report;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Some shared configuration
 */
public abstract class JacocoAbstractMojo extends AbstractMojo
{
    @Component
    private MojoExecution mojoExecution;

    @Parameter(property = "session.currentProject", readonly = true)
    MavenProject project;

    /**
     * Flag used to suppress execution.
     */
    @Parameter(property="jacoco.skip", defaultValue = "false")
    boolean skip;

    private static final String[] VALID_PHASES = {
        "prepare-package",
        "package",
        "pre-integration-test",
        "integration-test",
        "post-integration-test",
        "verify",
        "install",
        "deploy"
    };

    void verifyValidPhase() throws MojoFailureException{
        String phase = mojoExecution.getLifecyclePhase();
        for(String valid : VALID_PHASES) {
            if(valid.equals(phase)) {
                return;
            }
        }
        throw new MojoFailureException("goal must be bound to one of following phases; "
                + "prepare-package, package, "
                + "pre-integration-test, integration-test, post-integration-test, "
                + "verify, install, deploy");
    }
}
