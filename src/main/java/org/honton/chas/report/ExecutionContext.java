package org.honton.chas.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Track the set of executions.
 *
 */
class ExecutionContext {

    // executionId to ExecutionContext
    static ConcurrentMap<String,ExecutionContext> contexts = new ConcurrentHashMap<>();

    /**
     * Get the ExecutionContext for a given identity. 
     * @param executionId
     * @return The newly or previously associated context.
     */
    static ExecutionContext getExecutionContext(String executionId) {
        ExecutionContext ec = contexts.get(executionId);
        if(ec==null) {
            ec = new ExecutionContext();
            ExecutionContext prior = contexts.putIfAbsent(executionId, ec);
            if(prior!=null) {
                ec= prior;
            }
        }
        return ec;
    }

    
    // from already visited projects to project Configuration
    private Map<ProjectId,MultiModeMojo> visited = new HashMap<>();

    // from needed projects to already visited projects
    private Map<ProjectId, List<Aggregate>> neededToVisited = new HashMap<>();

    /**
     * Signal the execution context that a sub-module just completed a report.
     * 
     * @param projectId The sub-module's project identity.
     * @param dependent The report which was completed
     */
    void projectReady(ProjectId projectId, MultiModeMojo dependent, Object... arguments) {
        List<Aggregate> needs;
        synchronized(visited) {
            visited.put(projectId, dependent);
            needs = neededToVisited.remove(projectId);
        }
        if(needs!=null) {
            for(Aggregate later : needs) {
                later.addModule(projectId, dependent, arguments);
            }
        }
    }

    /**
     * Find a previously defined report.  If not found, create a new report.
     * @param collectedId The project's identity
     * @param aggregator The report generator.
     * @return If found, the previously created report; null, if not found (Does not return newly created report)
     */
    MultiModeMojo findModule(ProjectId collectedId, Aggregate aggregator) {
        synchronized(visited) {
            MultiModeMojo foundDependency = visited.get(collectedId);
            if(foundDependency!=null) {
                return foundDependency;
            }

            List<Aggregate> aggregators= neededToVisited.get(collectedId);
            if(aggregators==null) {
                aggregators = new ArrayList<>();
                neededToVisited.put(collectedId, aggregators);
            }
            aggregators.add(aggregator);
            return null;
        }
    }
}