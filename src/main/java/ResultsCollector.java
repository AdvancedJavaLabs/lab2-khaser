import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.rabbitmq.client.DeliverCallback;


class ResultsCollector {
    private final Result aggregated;
    private Thread collector;
    private int nextExpectedResult = 1;

    class Worker implements Runnable {
        BrokerQueue<Result> resultChannel;
        boolean shouldTerminate = false;

        Worker(BrokerQueue<Result> resultChannel) {
            this.resultChannel = resultChannel;
        }

        @Override
        public void run() {
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                try {
                    var serializer = new ByteSerializer<Result>();
                    Result recv = serializer.fromBytes(delivery.getBody());
                    if (recv.id != nextExpectedResult) {
                        // We got this segment too early, delay it
                        resultChannel.requeue(delivery.getEnvelope().getDeliveryTag());
                        return;
                    }
                    if (recv.endFlag) {
                        shouldTerminate = true;
                        return;
                    }

                    resultChannel.ack(delivery.getEnvelope().getDeliveryTag());
                    nextExpectedResult++;
                    System.out.println(" [x] Received '" + recv.toString() + "'");
                    aggregated.add(recv);
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
