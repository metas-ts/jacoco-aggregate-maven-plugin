package org.honton.chas.report;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.project.MavenProject;

/**
 * Track modules underneath an aggregate project
 */
public class Aggregate<T extends MultiModeMojo<T>> {

    final private T report;
    final private int collectedSize;
    final private Map<ProjectId,T> modules = new HashMap<>();

    /**
     * Create an Aggregate
     * 
     * @param project The aggregate project.
     * @param report The report to run once all sub-modules are done.
     * @param executionId The id which specifies execution context.
     */
    public Aggregate(MavenProject project, T report, String executionId, Object... arguments) {
        collectedSize = project.getCollectedProjects().size();
        this.report = report;
        ExecutionContext<T> executionContext = ExecutionContext.getExecutionContext(executionId);
        collectDependents(project, executionContext, arguments);
    }

    /**
     * Add all sub-modules of project to modules.
     * @param project
     * @param executionContext
     */
    private void collectDependents(MavenProject project, ExecutionContext<T> executionContext, Object[] arguments) {
        for(MavenProject collected : project.getCollectedProjects()) {
            ProjectId collectedId = new ProjectId(collected);
            MultiModeMojo<T> module = executionContext.findModule(collectedId, this);
            if(module!=null) {
                addModule(collectedId, report, arguments);
            }
        }
    }

    /**
     * Add a sub-module to the report to run for the aggregate project.  Once all sub-modules
     * have been run, run this aggregator's report.
     * 
     * @param projectId The sub-module projectId.
     * @param module The sub-module's report.
     */
    void addModule(ProjectId projectId, T module, Object[] arguments) {
        boolean done;
        synchronized(modules) {
            modules.put(projectId, module);
            done = modules.size() == collectedSize;
        }
        if(done) {
            Collection<T> values = modules.values();
            report.aggregateMode(values, arguments);
        }
    }

    /**
     * Signal the parent project that one of it's sub-modules is complete.
     * 
     * @param project The sub-module's project.
     * @param mojo The report that was completed.
     * @param executionId The identity of the execution context.
     */
    static <T extends MultiModeMojo<T>> void projectReady(MavenProject project, T mojo, String executionId, Object... arguments) {
        ExecutionContext<T> ex= ExecutionContext.getExecutionContext(executionId);
        ex.projectReady(new ProjectId(project), mojo, arguments);
    }
}
