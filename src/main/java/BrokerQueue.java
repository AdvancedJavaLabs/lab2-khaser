import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeoutException;

class BrokerQueue<T extends Serializable> implements AutoCloseable {
    final String queueName;
    Channel ch;
    BrokerQueue(Connection con, String queueName) throws IOException {
        this.queueName = queueName;
        this.ch = con.createChannel();
        ch.queueDeclare(queueName, false, false, true, null);
    }

    void send(T result) throws IOException {
        var serializer = new ByteSerializer<T>();
        ch.basicPublish("", queueName, null, serializer.toBytes(result));
    }

    void receive(DeliverCallback callback) throws IOException {
        ch.basicConsume(queueName, false, callback, consumerTag -> { });
    }

    void ack(long dtag) throws IOException {
        ch.basicAck(dtag, false);
    }

    void requeue(long dtag) throws IOException {
        ch.basicReject(dtag, true);
    }

    public void close() throws TimeoutException, IOException {
        ch.close();
    }
}
