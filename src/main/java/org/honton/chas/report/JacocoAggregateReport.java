package org.honton.chas.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportGroupVisitor;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.ISourceFileLocator;
import org.jacoco.report.MultiReportVisitor;
import org.jacoco.report.csv.CSVFormatter;
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.report.xml.XMLFormatter;

/**
 * Goal to create aggregate jacoco coverage report
 */
@Mojo(name = "report", defaultPhase = LifecyclePhase.VERIFY, aggregator= true)
public class JacocoAggregateReport extends JacocoAbstractMojo implements MavenReport {

    /**
     * Path to the output file for execution data.
     */
    @Parameter(property = "jacoco.outputDirectory", defaultValue = "${project.reporting.outputDirectory}/jacoco")
    private File outputDirectory;

    /**
     * Path to the output file for execution data.
     */
    @Parameter(property = "jacoco.dataFile", defaultValue = "${project.build.directory}/jacoco.exec")
    private File dataFile;

    /**
     * Encoding of the generated reports.
     */
    @Parameter(property = "project.reporting.outputEncoding", defaultValue = "UTF-8")
    private String outputEncoding;

    /**
     * Encoding of the source files.
     */
    @Parameter(property = "project.reporting.sourceEncoding", defaultValue = "UTF-8")
    private String sourceEncoding;

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
        return !skip;
    }

    /**
     * Generate the report depending the wanted locale. <br/>
     * Mainly used for external reports like javadoc.
     *
     * @param sink
     *            the sink to use for the generation.
     * @param locale
     *            the wanted locale to generate the report, could be null.
     * @throws MavenReportException
     *             if any
     */
    @Override
    public void generate(@SuppressWarnings("deprecation") org.codehaus.doxia.sink.Sink sink, Locale locale)
            throws MavenReportException {
        try {
            report(null);
        } catch (IOException io) {
            throw new MavenReportException(io.getMessage(), io);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            verifyValidPhase();
            report(null);
        } catch (IOException io) {
            throw new MojoExecutionException(io.getMessage(), io);
        }
    }

    void report(Locale locale) throws IOException {

        if (skip) {
            getLog().info("skipping");
            return;
        }

        if (!dataFile.canRead()) {
            getLog().info("cannot read " + dataFile.getAbsolutePath());
            return;
        }

        if (locale == null) {
            locale = Locale.getDefault();
        }

        ExecFileLoader loader = new ExecFileLoader();
        loader.load(dataFile);
        ExecutionDataStore executionDataStore = loader.getExecutionDataStore();

        final IReportVisitor visitor = createVisitor(locale);
        visitor.visitInfo(loader.getSessionInfoStore().getInfos(), executionDataStore.getContents());

        createReport(project, executionDataStore, visitor);
        visitor.visitEnd();
    }

    private static IBundleCoverage createBundle(MavenProject project, ExecutionDataStore executionDataStore) throws IOException {
        File classesFolder = new File(project.getBuild().getOutputDirectory());
        if (!classesFolder.canRead()) {
            return null;
        }
        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(executionDataStore, builder);
        analyzer.analyzeAll(classesFolder);
        return builder.getBundle(project.getName());
    }

    private void createReport(MavenProject project, ExecutionDataStore executionDataStore, IReportGroupVisitor visitor)
            throws IOException {
        if (project.getCollectedProjects().size() == 0) {
            IBundleCoverage bundle = createBundle(project, executionDataStore);
            if (bundle != null) {
                visitor.visitBundle(bundle, new SourceFileCollection(project));
            }
        } else {
            final IReportGroupVisitor groupVisitor = visitor.visitGroup(project.getName());
            for (MavenProject module : project.getCollectedProjects()) {
                createReport(module, executionDataStore, groupVisitor);
            }
        }
    }

    // TODO: boolean flags to create sub-set of reports
    private IReportVisitor createVisitor(Locale locale) throws IOException {
        final List<IReportVisitor> visitors = new ArrayList<IReportVisitor>();
        outputDirectory.mkdirs();

        XMLFormatter xmlFormatter = new XMLFormatter();
        xmlFormatter.setOutputEncoding(outputEncoding);
        visitors.add(xmlFormatter.createVisitor(new FileOutputStream(new File(outputDirectory, "jacoco.xml"))));

        CSVFormatter csvFormatter = new CSVFormatter();
        csvFormatter.setOutputEncoding(outputEncoding);
        visitors.add(csvFormatter.createVisitor(new FileOutputStream(new File(outputDirectory, "jacoco.csv"))));

        HTMLFormatter htmlFormatter = new HTMLFormatter();
        htmlFormatter.setOutputEncoding(outputEncoding);
        htmlFormatter.setLocale(locale);
        visitors.add(htmlFormatter.createVisitor(new FileMultiReportOutput(outputDirectory)));

        return new MultiReportVisitor(visitors);
    }

    private class SourceFileCollection implements ISourceFileLocator {

        final List<File> sourceRoots;

        SourceFileCollection(MavenProject project) {
            List<String> projectSourceRoots = project.getCompileSourceRoots();
            sourceRoots = new ArrayList<>(projectSourceRoots.size());
            for (String sourceRoot : projectSourceRoots) {
                sourceRoots.add(resolvePath(project, sourceRoot));
            }
        }

        private File resolvePath(MavenProject project, String path) {
            File file = new File(path);
            return file.isAbsolute() ? file : new File(project.getBasedir(), path);
        }

        private String fullClassName(final String packageName, final String fileName) {
            return packageName.isEmpty() ? fileName : packageName + '/' + fileName;
        }

        public Reader getSourceFile(final String packageName, final String fileName) throws IOException {
            final String fullName = fullClassName(packageName, fileName);

            for (File sourceRoot : sourceRoots) {
                File file = new File(sourceRoot, fullName);
                if (file.canRead()) {
                    return new InputStreamReader(new FileInputStream(file), sourceEncoding);
                }
            }
            return null;
        }

        public int getTabWidth() {
            return 4;
        }
    }

}
