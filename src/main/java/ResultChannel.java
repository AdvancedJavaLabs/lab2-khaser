import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

// TODO Rewrite to template class Channel<Result/Task>
class ResultChannel implements AutoCloseable {
    static final String resultQueueName = "results_queue";
    Channel ch;
    ResultChannel(Connection con) throws IOException {
        this.ch = con.createChannel();
        ch.queueDeclare(resultQueueName, false, false, true, null);
    }

    void send(Result result) throws IOException {
        ch.basicPublish("", resultQueueName, null, result.toBytes());
    }

    void receive(DeliverCallback callback) throws IOException {
        ch.basicConsume(resultQueueName, false, callback, consumerTag -> { });
    }

    void ack(long dtag) throws IOException {
        ch.basicAck(dtag, false);
    }

    public void close() throws TimeoutException, IOException {
        ch.close();
    }
}
