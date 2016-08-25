package com.godaddy.sonar.ruby.metricfu;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.RangeDistributionBuilder;
import org.sonar.api.scan.filesystem.PathResolver;

import com.godaddy.sonar.ruby.RubyPlugin;
import com.google.common.collect.Lists;

public class MetricfuComplexitySensor implements Sensor {
    private static final Logger LOG = LoggerFactory
            .getLogger(MetricfuComplexitySensor.class);

    private MetricfuComplexityYamlParser metricfuComplexityYamlParser;
    private Settings settings;
    private FileSystem fs;

    private static final Number[] FILES_DISTRIB_BOTTOM_LIMITS = {0, 5, 10, 20, 30, 60, 90};
    private static final Number[] FUNCTIONS_DISTRIB_BOTTOM_LIMITS = {1, 2, 4, 6, 8, 10, 12, 20, 30};

    private String reportPath = "tmp/metric_fu/report.yml";
    private PathResolver pathResolver;

    public MetricfuComplexitySensor(Settings settings, FileSystem fs,
                                    PathResolver pathResolver,
                                    MetricfuComplexityYamlParser metricfuComplexityYamlParser) {
        this.settings = settings;
        this.fs = fs;
        this.metricfuComplexityYamlParser = metricfuComplexityYamlParser;
        this.pathResolver = pathResolver;
        String reportpath_prop = settings.getString(RubyPlugin.METRICFU_REPORT_PATH_PROPERTY);
        if (null != reportpath_prop) {
            this.reportPath = reportpath_prop;
        }
    }

    /**
     * Enables the plugin only if the language of the project is 'ruby'
     *
     * @return true if ruby is set as the project language and false otherwise
     */
    public boolean shouldExecuteOnProject() {
        return fs.hasFiles(fs.predicates().hasLanguage("ruby"));
    }
    
    /**
     * Set API description of the sensor
     * @param sensorDescriptor sensor description object
     */
    @Override
    public void describe(SensorDescriptor sensorDescriptor) {
        sensorDescriptor
                .onlyOnLanguage("rb")
                .name("Metric_fu complexity sensor");
    }
    
    /**
     * Ruby plugin driver function, initiates the metrics analysis process
     *
     * @param context context in which the sensor is ran (test)
     */
    @Override
    public void execute(SensorContext context) {
        
        // locates the metric_fu report file and validates it
        File report = pathResolver.relativeFile(fs.baseDir(), reportPath);
        LOG.info("Calling analyse for report results: " + report.getPath());
        if (!report.isFile()) {
            LOG.warn("MetricFu report not found at {}", report);
            return;
        }

        // lists the source files to survey and evaluate
        List<InputFile> sourceFiles = Lists.newArrayList(fs.inputFiles(fs.predicates().hasLanguage("ruby")));

        // iterate source files and apply analysis to it
        for (InputFile inputFile : sourceFiles) {
            LOG.debug("analyzing functions for classes in the file: " + inputFile.file().getName());
            try {
                analyzeFile(inputFile, context, report);
            } catch (IOException e) {
                LOG.error("Can not analyze the file " + inputFile.absolutePath() + " for complexity", e);
            }
        }
    }
    
    /**
     * evaluates complexity metrics for a given source file
     * @param inputFile file to evaluate
     * @param sensorContext context in which the analysis is ran(test)
     * @param resultsFile metrics result file to draw metrics from
     * @throws IOException
     */
    private void analyzeFile(InputFile inputFile, SensorContext sensorContext, File resultsFile)
            throws IOException {
        
        // get the function complexity results from metric_fu
        // results file for the given file to evaluate (shakedl: not very efficient..)
        LOG.debug("functions are set");
        String complexityType = settings.getString(RubyPlugin.METRICFU_COMPLEXITY_METRIC_PROPERTY);
        List<RubyFunction> functions = metricfuComplexityYamlParser.parseFunctions(inputFile.file().getName(), resultsFile,
                complexityType);

        // if function list is empty, then return, do not compute any complexity
        // on that file
        if (functions.isEmpty() || functions.size() == 0 || functions == null) {
            return;
        }

        // sum function complexities to create file complexity metric
        LOG.debug("COMPLEXITY are set" + functions.toString());
        int fileComplexity = 0;
        for (RubyFunction function : functions) {
            fileComplexity += function.getComplexity();
            LOG.debug("File complexity " + fileComplexity);
        }

        // save file complexity distribution metrics
        // compute complexity metrics
        RangeDistributionBuilder fileDistribution = new RangeDistributionBuilder(
                CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION,
                FILES_DISTRIB_BOTTOM_LIMITS);
        fileDistribution.add((double) fileComplexity);
        
        // save the distribution as a new FILE_COMPLEXITY_DISTRIBUTION metric
        sensorContext.<String>newMeasure()
                .on(inputFile)
                .forMetric(CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION)
                .withValue(String.valueOf(fileDistribution.build()))
                .save();
        
        // save the mean file complexity
        sensorContext.<Integer>newMeasure()
                .on(inputFile)
                .forMetric(CoreMetrics.COMPLEXITY)
                .withValue(fileComplexity / functions.size())
                .save();
        
        // save function complexity distribution metrics
        // compute complexity metrics
        RangeDistributionBuilder functionDistribution =
                new RangeDistributionBuilder(
                        CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION,
                        FUNCTIONS_DISTRIB_BOTTOM_LIMITS);
        for (RubyFunction function : functions) {
            functionDistribution.add((double) function.getComplexity());
        }
        
        // save the function distribution as a new FUNCTION_COMPLEXITY_DISTRIBUTION metric
        sensorContext.<String>newMeasure()
                .on(inputFile)
                .forMetric(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION)
                .withValue(String.valueOf(functionDistribution.build()))
                .save();
    }
}
