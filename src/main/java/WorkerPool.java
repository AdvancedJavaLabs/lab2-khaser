import java.util.ArrayList;

import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.lang.Thread;

class WorkerPool {

    class Worker implements Runnable {
        final TaskChannel taskChannel;
        final ResultChannel resultChannel;
        boolean shouldTerminate = false;
        Worker(TaskChannel taskChannel, ResultChannel resultChannel) {
            this.taskChannel = taskChannel;
            this.resultChannel = resultChannel;
        }
        @Override
        public void run() {
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                try {
                    Task recv = Task.fromBytes(delivery.getBody());
                    // System.out.println(" [x] Received '" + recv.toString() + "'");
                    if (recv.endFlag) {
                        shouldTerminate = true;
                        return;
                    }
                    taskChannel.ack(delivery.getEnvelope().getDeliveryTag());
                    resultChannel.send(Result.fromText(recv.text));
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
            result_ch.send(Result.stopSignal());
        } catch (Exception err) {
            System.err.println("Exception: " + err);
        }
    }

}

