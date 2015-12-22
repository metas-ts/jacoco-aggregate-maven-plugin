package org.honton.chas.report;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jacoco.core.tools.ExecFileLoader;

/**
 * Goal which merges aggregate coverage data
 */
@Mojo(name = "merge", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class JacocoMergeMojo extends AbstractAggrateMojo
{
    /**
     * Path to the output file for execution data.
     */
    @Parameter(property="jacoco.destFile", defaultValue="${project.build.directory}/jacoco.exec")
    File destFile;

    @Override
    boolean shouldSkip() {
        if (skip) {
            getLog().info("skip=true");
            return true;
        }
        if (!destFile.canRead()) {
            getLog().info("skipping, cannot read " + destFile.getAbsolutePath());
            return true;
        }
        return false;
    }

    @Override
    public void nonAggregateMode(Object... arguments) {
        // nothing to do
    }

    @Override
    public void aggregateMode( Map<ProjectId, MultiModeMojo> projectConfigurations, Object... arguments) {
        final ExecFileLoader loader = new ExecFileLoader();
        for(MultiModeMojo sp : projectConfigurations.values()) {
            load(loader, sp);
            getLog().info(sp.toString());
        }
        save(loader);
    }

    private void load(final ExecFileLoader loader, MultiModeMojo merge) {
        File destFile = ((JacocoMergeMojo)merge).destFile;
        if(destFile.exists()) {
            try {
                loader.load(destFile);
            } catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }
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
