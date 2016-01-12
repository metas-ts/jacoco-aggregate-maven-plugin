package org.honton.chas.report;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jacoco.core.tools.ExecFileLoader;

/**
 * Goal which merges aggregate coverage data
 */
@Mojo(name = "merge", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, aggregator=true)
public class JacocoMergeMojo extends JacocoAbstractMojo
{
    /**
     * Path to the output file for execution data.
     */
    @Parameter(property="jacoco.destFile", defaultValue="${project.build.directory}/jacoco.exec")
    File destFile;

    /**
     * Path to the input data file for execution data in each sub-module
     */
    @Parameter(property = "jacoco.dataFile", defaultValue = "target/jacoco.exec")
    String dataFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        verifyValidPhase();

        if ( skip || project.getCollectedProjects().size()==0 ) {
            return;
        }

        try {
            final ExecFileLoader loader = new ExecFileLoader();
            for(MavenProject module : project.getCollectedProjects()) {
                merge(loader, module);
            }
            save(loader);
        }
        catch(IOException io) {
            throw new MojoExecutionException(io.getMessage(), io);
        }
    }

    private void merge(ExecFileLoader loader, MavenProject module) throws IOException {
        File moduleDataFile = new File(module.getBasedir(), dataFile);
        if(moduleDataFile.canRead()) {
            getLog().info("merging data file " + moduleDataFile);
            loader.load(moduleDataFile);
        }
        else {
            getLog().info("missing data file " + moduleDataFile);
        }
    }

    private void save(final ExecFileLoader loader) throws IOException {
        if (loader.getExecutionDataStore().getContents().isEmpty()) {
            getLog().info("No merged data to save");
            return;
        }
        getLog().info("Saving merged data to " + destFile.getCanonicalPath());
        loader.save(destFile, false);
    }
}
