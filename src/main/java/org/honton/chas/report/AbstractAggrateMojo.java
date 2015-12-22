package org.honton.chas.report;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.lang.reflect.UndeclaredThrowableException;

/**
 * Goal which aggregates mojo actions
 */
public abstract class AbstractAggrateMojo extends AbstractMojo implements MultiModeMojo
{
    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    MojoExecution mojoExecution;

    @Parameter(defaultValue = "${session.currentProject}", readonly = true)
    MavenProject project;

    /**
     * Flag used to suppress execution.
     */
    @Parameter(property="jacoco.skip", defaultValue = "false")
    boolean skip;

    abstract boolean shouldSkip();

    /**
     * is this project an aggregate?
     * @return true, if project has sub-modules.
     */
    boolean isAggregator() {
        return project.getCollectedProjects().size()>0;
    }

    @Override
    public void execute() throws MojoExecutionException {
        try {
            aggregate();
        }
        catch (UndeclaredThrowableException ute) {
            throw new MojoExecutionException(ute.getMessage(), ute.getCause());
        }
    }

    void aggregate(Object... arguments) throws UndeclaredThrowableException {
        if(shouldSkip()) {
            return;
        }
        String executionId = mojoExecution.getExecutionId();
        if( isAggregator() ) {
            new Aggregate(project, this, executionId, arguments);
        }
        else {
            nonAggregateMode();
        }
        Aggregate.projectReady(project, this, executionId);
    }
}
