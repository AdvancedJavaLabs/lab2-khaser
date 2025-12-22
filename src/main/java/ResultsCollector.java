import java.io.IOException;
import java.util.HashMap;

import com.rabbitmq.client.DeliverCallback;


class ResultsCollector {
    private final Result aggregated;
    private Thread collector;
    private int nextExpectedResult = 1;

    class Worker implements Runnable {
        final BrokerQueue<Result> resultChannel;
        boolean shouldTerminate = false;
        final HashMap<Integer, Result> cache;

        Worker(BrokerQueue<Result> resultChannel) {
            this.resultChannel = resultChannel;
            this.cache = new HashMap<>();
        }

        @Override
        public void run() {
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                try {
                    var serializer = new ByteSerializer<Result>();
                    Result recv = serializer.fromBytes(delivery.getBody());
                    resultChannel.ack(delivery.getEnvelope().getDeliveryTag());
                    // System.out.println(" [x] Received '" + recv.toString() + "'");
                    if (recv.endFlag) {
                        shouldTerminate = true;
                        return;
                    }
                    cache.put(recv.id, recv);
                    while (cache.containsKey(nextExpectedResult)) {
                        var nextPart = cache.get(nextExpectedResult);
                        aggregated.add(nextPart);
                        // System.out.println(" [x] Aggregated '" + nextPart.toString() + "'");
                        cache.remove(nextExpectedResult++);
                    }
                } catch (Exception err) {
                    System.err.println("Exception: " + err);
                }
            };
            try {
                while (!shouldTerminate) {
                    resultChannel.receive(deliverCallback);
                }
                resultChannel.close();
            } catch (Exception err) {
                System.err.println(err);
            }
        }
    }

    ResultsCollector(Broker broker) throws IOException {
        this.aggregated = new Result();
        collector = new Thread(new Worker(broker.getResultChannel()), "ResultsCollector");
    }

    void start() {
        collector.start();
    }

    void waitUntilProcessed() throws InterruptedException {
        collector.join();
    }

    Result getResults() {
        return aggregated;
    }
}
