import java.io.IOException;
import com.rabbitmq.client.DeliverCallback;


class ResultsCollector {
    private final Result aggregated;
    private Thread collector;

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
                    // System.out.println(" [x] Received '" + recv.toString() + "'");
                    if (recv.endFlag) {
                        shouldTerminate = true;
                        return;
                    }
                    resultChannel.ack(delivery.getEnvelope().getDeliveryTag());
                    aggregated.add(recv);
                } catch (Exception err) {
                    System.err.println("Exception: " + err);
                }
            };
            while (!shouldTerminate) {
                try {
                    resultChannel.receive(deliverCallback);
                } catch (IOException err) {
                    System.err.println(err);
                }
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
