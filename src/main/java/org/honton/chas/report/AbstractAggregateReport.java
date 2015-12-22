package org.honton.chas.report;

import java.io.File;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Locale;

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;

/**
 * Goal which produces aggregate reports
 */
public abstract class AbstractAggregateReport
    extends AbstractAggrateMojo implements MavenReport
{
    /**
     * Output directory for the reports. Note that this parameter is only
     * relevant if the goal is run from the command line or from the default
     * build lifecycle. If the goal is run indirectly as part of a site
     * generation, the output directory configured in the Maven Site Plugin is
     * used instead.
     */
    @Parameter(defaultValue = "${project.reporting.outputDirectory}/jacoco")
    private File outputDirectory;

    /**
     * @param locale the wanted locale to return the report's description, could be null.
     * @return the description of this report.
     */
    @Override
    public String getDescription(Locale locale) {
        return "jacoco aggregate report";
    }

    /**
     * @param locale the wanted locale to return the report's name, could be null.
     * @return the name of this report.
     */
    @Override
    public String getName(Locale locale) {
        return "jacoco";
    }

    /**
     * @return the output name of this report.
     */
    @Override
    public String getOutputName() {
        return "jacoco/index";
    }

    /**
     * Get the category name for this report.
     *
     * @return the category name of this report.
     */
    @Override
    public String getCategoryName() {
        return CATEGORY_PROJECT_REPORTS;
    }

    /**
     * Set a new output directory. Useful for staging.
     *
     * @param outputDirectory the new output directory
     */
    @Override
    public void setReportOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * @return the current report output directory.
     */
    @Override
    public File getReportOutputDirectory() {
        return outputDirectory;
    }

    /**
     * An external report is a report which calls a third party program which generates reports.
     * A good example is javadoc.
     *
     * @return <tt>true</tt> if this report is external, <tt>false</tt> otherwise.
     */
    @Override
    public boolean isExternalReport() {
        return true;
    }

    /**
     * Verify some conditions before generate the report.
     *
     * @return <tt>true</tt> if this report could be generated, <tt>false</tt> otherwise.
     */
    @Override
    public boolean canGenerateReport() {
        return !shouldSkip();
    }

    /**
     * Generate the report depending the wanted locale.
     * <br/>
     * Mainly used for external reports like javadoc.
     *
     * @param sink the sink to use for the generation.
     * @param locale the wanted locale to generate the report, could be null.
     * @throws MavenReportException if any
     */
    @Override
    public void generate(Sink sink, Locale locale) throws MavenReportException {
        try {
            aggregate(sink, locale);
        } catch (UndeclaredThrowableException ute) {
            throw new MavenReportException(ute.getMessage(), (Exception) ute.getCause());
        }
    }
}
