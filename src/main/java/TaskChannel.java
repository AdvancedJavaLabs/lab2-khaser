import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

class TaskChannel implements AutoCloseable {
    static final String taskQueueName = "tasks_queue";
    Channel ch;
    TaskChannel(Connection con) throws IOException {
        this.ch = con.createChannel();
        ch.queueDeclare(taskQueueName, false, false, true, null);
    }

    void send(Task task) throws IOException {
        ch.basicPublish("", taskQueueName, null, task.toBytes());
    }

    void receive(DeliverCallback callback) throws IOException {
        ch.basicConsume(taskQueueName, false, callback, consumerTag -> { });
    }

    void ack(long dtag) throws IOException {
        ch.basicAck(dtag, false);
    }

    public void close() throws TimeoutException, IOException {
        ch.close();
    }
}
