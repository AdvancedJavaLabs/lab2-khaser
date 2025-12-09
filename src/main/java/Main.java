import java.nio.file.Path;

public class Main {
    final static Path DUNE_TEXT = Path.of("data", "Herbert Frank - Dune.txt");
    final static int nWorkers = 2;
    final static int linesInBatch = 64;

    public static void main(String[] args) throws Exception {

        try (final var broker = new Broker()) {
             final var prod = new Producer(broker);
             final var workers = new WorkerPool(nWorkers, broker);
             final var collector = new ResultsCollector(broker);
             workers.start();
             collector.start();
             prod.emitTasksForFile(DUNE_TEXT, linesInBatch);
             workers.waitUntilProcessed();
             collector.waitUntilProcessed();
             System.out.println(collector.getResults().toString());
        }

    }
}
