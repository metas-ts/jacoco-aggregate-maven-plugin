package org.honton.chas.report;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jacoco.core.tools.ExecFileLoader;

/**
 * Goal which merges aggregate coverage data
 */
@Mojo(name = "merge", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class JacocoMergeMojo extends AbstractAggregateMojo<JacocoMergeMojo>
{
    /**
     * Path to the output file for execution data.
     */
    @Parameter(property="jacoco.destFile", defaultValue="${project.build.directory}/jacoco.exec")
    File destFile;
    
    /**
     * Path to the input data file for execution data in each sub-module
     */
    @Parameter(property = "jacoco.dataFile", defaultValue = "${project.build.directory}/jacoco.exec")
    File dataFile;

    @Override
    boolean shouldSkip() {
        if (skip) {
            getLog().info("skip=true");
            return true;
        }
        return false;
    }

    @Override
    public void nonAggregateMode(Object... arguments) {
        // nothing to do
    }

    @Override
    public void aggregateMode(Collection<JacocoMergeMojo> subModules, Object... arguments) {
        final ExecFileLoader loader = new ExecFileLoader();
        for(JacocoMergeMojo merge : subModules) {
            merge.load(loader);
        }
        save(loader);
    }

    private void load(final ExecFileLoader loader) {
        if(dataFile.canRead()) {
            try {
                getLog().info("merging data file " + dataFile);
                loader.load(dataFile);
            } catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
        }
        else {
            getLog().info("missing data file " + dataFile);
        }
    }

    private void save(final ExecFileLoader loader) {
        if (loader.getExecutionDataStore().getContents().isEmpty()) {
            getLog().info("No merged data to save");
            return;
        }
        try {
            getLog().info("Saving merged data to " + destFile.getCanonicalPath());
            loader.save(destFile, false);
        } catch (IOException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

}
