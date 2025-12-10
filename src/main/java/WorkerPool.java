import java.util.ArrayList;

import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.lang.Thread;

class WorkerPool {

    class Worker implements Runnable {
        final BrokerQueue<Task> taskChannel;
        final BrokerQueue<Result> resultChannel;
        boolean shouldTerminate = false;
        Worker(BrokerQueue<Task>  taskChannel, BrokerQueue<Result> resultChannel) {
            this.taskChannel = taskChannel;
            this.resultChannel = resultChannel;
        }
        @Override
        public void run() {
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                try {
                    var serializer = new ByteSerializer<Task>();
                    Task recv = serializer.fromBytes(delivery.getBody());
                    if (recv.endFlag) {
                        shouldTerminate = true;
                        stopSignalId = recv.id;
                        return;
                    }
                    taskChannel.ack(delivery.getEnvelope().getDeliveryTag());
                    resultChannel.send(Result.fromText(recv.id, recv.text));
                } catch (Exception err) {
                    System.err.println("Exception: " + err);
                }
            };

            try {
                while (!shouldTerminate) {
                    taskChannel.receive(deliverCallback);
                }
                taskChannel.close();
                resultChannel.close();
            } catch (Exception err) {
                System.err.println(err);
            }
        }
    }

    final ArrayList<Thread> workers;
    final Broker broker;
    int stopSignalId;

    WorkerPool(int nWorkers, Broker broker) throws IOException {
        this.broker = broker;
        workers = new ArrayList<Thread>(nWorkers);
        for (int i = 0; i < nWorkers; ++i) {
            workers.add(new Thread(
                            new Worker(broker.getTaskChannel(), broker.getResultChannel()),
                            "Worker-" + i));
        }
    }

    void start() {
        workers.forEach((th) -> th.start());
    }

    void waitUntilProcessed() throws InterruptedException {
        for (var th : workers) {
            th.join();
        }
        try (var result_ch = broker.getResultChannel()) {
            result_ch.send(Result.stopSignal(stopSignalId));
        } catch (Exception err) {
            System.err.println("Exception: " + err);
        }
    }

}

