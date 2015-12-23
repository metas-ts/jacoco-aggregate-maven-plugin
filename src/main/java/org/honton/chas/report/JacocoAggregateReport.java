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
public class JacocoAggregateReport extends AbstractAggregateReport<JacocoAggregateReport> {
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

    private Collection<JacocoAggregateReport> subModules;
    private File classesFolder;
    private SourceFileCollection sourceFileCollection;

    private void initialize(Collection<JacocoAggregateReport> subModules) {
        this.subModules = subModules;
        classesFolder = new File(project.getBuild().getOutputDirectory());
        sourceFileCollection = new SourceFileCollection();
    }

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
        initialize(null);
    }

    @Override
    public void aggregateMode(Collection<JacocoAggregateReport> subModules, Object... arguments) {
        getLog().info("skipping, cannot read "+dataFile.getAbsolutePath());

        initialize(subModules);

        if (!dataFile.canRead()) {
            getLog().info("skipping, cannot read "+dataFile.getAbsolutePath());
            return;
        }

        try {
            ExecFileLoader loader = new ExecFileLoader();
            loader.load(dataFile);
            ExecutionDataStore executionDataStore = loader.getExecutionDataStore();

            Locale locale = arguments.length > 1 ? (Locale) arguments[1] : Locale.getDefault();
            final IReportVisitor visitor = createVisitor(locale);
            visitor.visitInfo(loader.getSessionInfoStore().getInfos(), executionDataStore.getContents());

            createReport(executionDataStore, visitor);
            visitor.visitEnd();
        } catch (final IOException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    private IBundleCoverage createBundle(ExecutionDataStore executionDataStore) throws IOException {
        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(executionDataStore, builder);
        analyzer.analyzeAll(classesFolder);
        return builder.getBundle(project.getName());
    }

    private void createReport(ExecutionDataStore executionDataStore, IReportGroupVisitor visitor) throws IOException {
        if (subModules==null || subModules.isEmpty()) {
            if(dataFile.canRead()) {
                IBundleCoverage bundle = createBundle(executionDataStore);
                visitor.visitBundle(bundle, sourceFileCollection);
            }
        } else {
            final IReportGroupVisitor groupVisitor = visitor.visitGroup(project.getName());
            for (JacocoAggregateReport module : subModules) {
                module.createReport(executionDataStore, groupVisitor);
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

        final List<File> sourceRoots;

        SourceFileCollection() {
            List<String> projectSourceRoots = project.getCompileSourceRoots();
            sourceRoots = new ArrayList<>(projectSourceRoots.size());
            for (String sourceRoot : projectSourceRoots) {
                sourceRoots.add(resolvePath(sourceRoot));
            }
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
