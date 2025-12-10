import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Producer {
    final Broker broker;

    Producer(Broker broker) {
        this.broker = broker;
    }

    void emitTasksForFile(Path path, int nLinesBlockSize) {
        try (Stream<String> stream = Files.lines(path);
             var ch = broker.getTaskChannel()) {
            stream.collect(blockCollector(nLinesBlockSize))
                  .forEach((nLines) -> { sendBatch(ch, nLines); });
            ch.send(Task.stopSignal());
        } catch (Exception x) {
            System.err.format("Exception: %s%n", x);
        }
    }

    private void sendBatch(BrokerQueue<Task> ch, List<String> nLines) {
        try {
            Task task = new Task(nLines.stream().collect(Collectors.joining("\n")));
            ch.send(task);
        } catch (IOException err) {
            System.err.println(err);
        }
    }

    private static Collector<String, List<List<String>>, List<List<String>>> blockCollector(int blockSize) {
        return Collector.of(
                ArrayList<List<String>>::new,
                (list, value) -> {
                    List<String> block = (list.isEmpty() ? null : list.get(list.size() - 1));
                    if (block == null || block.size() == blockSize)
                        list.add(block = new ArrayList<>(blockSize));
                    block.add(value);
                },
                (r1, r2) -> { throw new UnsupportedOperationException("Parallel processing not supported"); }
        );
    }
}

