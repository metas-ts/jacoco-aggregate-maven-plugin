# jacoco-aggregation-maven-plugin
jacoco-aggregation-maven-plugin supports aggregate projects allowing code coverage reports across projects.  This plugin solves the scenario described in the pull request https://github.com/jacoco/jacoco/pull/97.  Configuration is similar to that of jacoco-maven-plugin supplied by eclemma.

There are two goals supported: merge and report.  Both goals are active only for aggregator projects, (those having a `<modules>` element.)

### merge
Merge the results of all sub-modules into a single data file.

**Parameters**

| Name | Description | Default | User Property |
| - | - | - | - |
| skip | Skip this goal | jacoco.skip | false |
| dataFile | The input data file location. | jacoco.dataFile | ${project.build.directory}/jacoco.exec |
| destFile | The output data file location. | jacoco.destFile | ${project.build.directory}/jacoco.exec |

### report
Report the results of all sub-modules into single report. The report will be created in xml, csv, and html formats.

**Parameters**

| Name | Description | User Property | Default |
| - | - | - | - |
| skip | Skip this goal | jacoco.skip | false |
| dataFile | The data file location.  This should be the merged data file from merge goal. | jacoco.dataFile | ${project.build.directory}/jacoco.exec |
| outputDirectory | The location for the report. | jacoco.outputDirectory | ${project.reporting.outputDirectory}/jacoco |
| sourceEncoding | The source code encoding. | project.reporting.sourceEncoding | UTF-8 |
| outputEncoding | The output report encoding. | project.reporting.outputEncoding | UTF-8 |
