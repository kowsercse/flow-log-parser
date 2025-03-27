# Flow log parser

## Requirement

* JDK 21

# Overview

Code is written on `flow.log.FlowLogParserTest` test class.

* Though not an actual unit/integration test.
* `FlowLogParserTest.generateReport` is the driver code
    * Populates the lookup table
    * Prepare the frequency count by network endpoint
    * Prepare the frequency count by tag
    * Writes the frequency count to csv file
    * Writes the tag count to csv file
* No javadoc added

## Assumption

Protocol is mapped to following values:

* TCP: 6
* UDP: 17
* ICMP: 1

### Considerations

Very naive implementation to parse log files.

* Not designed for large size input
* Following requirements is easily testable
    * The flow log file size can be up to 10 MB
    * The lookup file can have up to 10000 mappings

## How to run

* Run following command

    ```shell
    ./mvnw test
    ```
* Reports are available on [code-base/out](out) directory
    * [endpoint-counts.csv](out/endpoint-counts.csv)
    * [tag-counts.csv](out/tag-counts.csv)

* To change input files change followings on `flow.log.FlowLogParserTest` as necessary

    ```java
    private static final String FLOW_LOG = "/flow.log";
    private static final String LOOKUP_TABLE_CSV = "/lookup-table.csv";
    ```
    * These resources are available on test [resources](src/test/resources) directory
