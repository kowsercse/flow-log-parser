package flow.log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class FlowLogParserTest {

  private static final String FLOW_LOG = "/flow.log";
  private static final String LOOKUP_TABLE_CSV = "/lookup-table.csv";
  private static final String TAG = "Tag";
  private static final String PORT = "Port";
  private static final String PROTOCOL = "Protocol";
  private static final String COUNT = "Count";
  private static final String UNNAMED = "Unnamed";
  private static final List<String> ENDPOINT_REPORT_HEADER = List.of(PORT, PROTOCOL, COUNT);
  private static final List<String> TAG_REPORT_HEADER = List.of(TAG, COUNT);
  private static final Map<String, Protocol> PROTOCOLS_BY_TAG =
      Map.ofEntries(
          Map.entry("6", Protocol.TCP),
          Map.entry("1", Protocol.ICMP),
          Map.entry("17", Protocol.UDP));

  @Test
  void generateReport() throws URISyntaxException, IOException {
    var lookupTable = getLookupTable(LOOKUP_TABLE_CSV);
    var frequencyByEndpoint = getFrequencyByEndpoint(FLOW_LOG);
    var frequencyByTag = frequencyByEndpoint.entrySet().stream().collect(
        Collectors.groupingBy(entry -> lookupTable.getOrDefault(entry.getKey(), UNNAMED),
            Collectors.summingLong(Entry::getValue)));

    var endpointCounts = frequencyByEndpoint.entrySet().stream().map(FlowLogParserTest::toEndpointRow).toList();
    writeCsv("out/endpoint-counts.csv", ENDPOINT_REPORT_HEADER, endpointCounts);

    var tagCounts = frequencyByTag.entrySet().stream().map(FlowLogParserTest::toTagCsvRow).toList();
    writeCsv("out/tag-counts.csv", TAG_REPORT_HEADER, tagCounts);
  }

  private Map<NetworkEndpoint, Long> getFrequencyByEndpoint(String fileName) throws URISyntaxException, IOException {
    Path path = Path.of(Objects.requireNonNull(getClass().getResource(fileName)).toURI());

    return Files.readAllLines(path).stream().map(line -> line.split(" "))
        .map(parts -> new NetworkEndpoint(PROTOCOLS_BY_TAG.get(parts[7]), parts[5] /* port */))
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
  }

  private Map<NetworkEndpoint, String> getLookupTable(String fileName) throws URISyntaxException, IOException {
    Path path = Path.of(Objects.requireNonNull(getClass().getResource(fileName)).toURI());
    return Files.readAllLines(path)
        .stream()
        .skip(1)
        .map(line -> line.split(","))
        .collect(
            Collectors.toMap(
                parts -> new NetworkEndpoint(Protocol.valueOf(parts[1].toUpperCase()), parts[0] /* port */),
                parts -> parts[2] /* tag */));
  }

  private static void writeCsv(String fileName, List<String> headers, List<Map<String, String>> rows)
      throws FileNotFoundException {
    try (PrintStream printStream = new PrintStream(fileName)) {
      printStream.println(String.join(",", headers));
      for (Map<String, String> map : rows) {
        var line = headers.stream().map(map::get).collect(Collectors.joining(","));
        printStream.println(line);
        printStream.flush();
      }
    }
  }

  private static Map<String, String> toTagCsvRow(Entry<String, Long> entry) {
    return Map.ofEntries(Map.entry(TAG, entry.getKey()), Map.entry(COUNT, String.valueOf(entry.getValue())));
  }

  private static Map<String, String> toEndpointRow(Entry<NetworkEndpoint, Long> entry) {
    return Map.ofEntries(
        Map.entry(PORT, entry.getKey().port()),
        Map.entry(PROTOCOL, entry.getKey().protocol().toString()),
        Map.entry(COUNT, String.valueOf(entry.getValue())));
  }
}
