package org.honton.chas.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
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

@Mojo(name = "report", defaultPhase = LifecyclePhase.VERIFY)
public class JacocoAggregateReport extends AbstractAggregateReport {
    /**
     * Path to the output file for execution data.
     */
    @Parameter(property = "jacoco.dataFile", defaultValue = "${project.build.directory}/jacoco.exec")
    File dataFile;

    /**
     * Path to the output file for execution data.
     */
    @Parameter(property = "jacoco.outputDirectory", defaultValue = "${project.reporting.outputDirectory}/jacoco")
    File outputDirectory;

    /**
     * Encoding of the generated reports.
     */
    @Parameter(property = "project.reporting.outputEncoding", defaultValue = "UTF-8")
    String outputEncoding;

    /**
     * Encoding of the source files.
     */
    @Parameter(property = "project.reporting.sourceEncoding", defaultValue = "UTF-8")
    String sourceEncoding;

    @Override
    boolean shouldSkip() {
        if (skip) {
            getLog().info("skip=true");
            return true;
        }
        if (!dataFile.canRead()) {
            getLog().info("skipping, cannot read "+dataFile.getAbsolutePath());
            return true;
        }
        return false;
    }

    @Override
    public void nonAggregateMode(Object... arguments) {
        // nothing to do...
    }

    private Collection<JacocoAggregateReport> subModules;
    private ExecutionDataStore executionDataStore;

    private File getClassesFolder() {
        return new File(project.getBuild().getOutputDirectory());
    }

    @Override
    public void aggregateMode(Collection<MultiModeMojo> projectConfigurations, Object... arguments) {
        subModules = projectConfigurations;
        try {
            ExecFileLoader loader = new ExecFileLoader();
            loader.load(dataFile);
            executionDataStore = loader.getExecutionDataStore();

            Locale locale = arguments.length > 1 ? (Locale) arguments[1] : Locale.getDefault();
            final IReportVisitor visitor = createVisitor(locale);
            createReport(visitor);
            visitor.visitEnd();
        } catch (final IOException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    private IBundleCoverage createBundle() throws IOException {
        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(executionDataStore, builder);
        analyzer.analyzeAll(getClassesFolder());
        return builder.getBundle(project.getName());
    }

    private void createReport(IReportGroupVisitor visitor) throws IOException {
        if (subModules==null || subModules.isEmpty()) {
            IBundleCoverage bundle = createBundle();
            visitor.visitBundle(bundle, new SourceFileCollection());
        } else {
            final IReportGroupVisitor groupVisitor = visitor.visitGroup(project.getName());
            for (JacocoAggregateReport module : subModules) {
                module.createReport(groupVisitor);
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

    private File resolvePath(String path) {
        File file = new File(path);
        return file.isAbsolute() ? file : new File(project.getBasedir(), path);
    }

    private static String fullClassName(final String packageName, final String fileName) {
        return packageName.isEmpty() ?fileName :packageName + '/' + fileName;
    }

    private class SourceFileCollection implements ISourceFileLocator {

        public Reader getSourceFile(final String packageName, final String fileName) throws IOException {
            final String fullName = fullClassName(packageName, fileName);

            for (String sourceRoot : project.getCompileSourceRoots()) {
                final File file = new File(resolvePath(sourceRoot), fullName);
                if (file.exists() && file.isFile()) {
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
