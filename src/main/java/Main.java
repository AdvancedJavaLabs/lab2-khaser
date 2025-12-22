import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public class Main {
    final static Path DUNE_TEXT = Path.of("data", "Herbert Frank - Dune.txt");

    public static void main(String[] args) throws Exception {

        // Warmup iteration
        var results = processPath(DUNE_TEXT, 4, 512);

        System.out.println(results.toString());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("modifiedText.txt"))) {
            writer.write("Modified text:\n");
            writer.write(results.getModifiedText());
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("longestSequences.txt"))) {
            writer.write("Longest sequences:\n");
            for (String sent : results.getLongestSentences()) {
                writer.write(sent + "\n\n");
            }
        }

        // Additional runs to measure performance
        for (var workers : new int[] {1, 2, 4}) {
            for (var linesInBatch : new int[] {8, 32, 128, 512}) {
                processPath(DUNE_TEXT, workers, linesInBatch);
            }
        }
    }

    public static Result processPath(Path inputFile, int nWorkers, int linesInBatch) throws Exception {
        try (final var broker = new Broker()) {
             final var prod = new Producer(broker);
             final var workers = new WorkerPool(nWorkers, broker);
             final var collector = new ResultsCollector(broker);

             Instant start = Instant.now();
             workers.start();
             collector.start();
             prod.emitTasksForFile(inputFile, linesInBatch);
             workers.waitUntilProcessed();
             collector.waitUntilProcessed();
             Instant end = Instant.now();

             Duration duration = Duration.between(start, end);
             System.out.printf("Benchmark(workers=%d, linesInBatch=%d): %d ms\n",
                               nWorkers, linesInBatch, duration.toMillis());
             return collector.getResults();
        }
    }
}
