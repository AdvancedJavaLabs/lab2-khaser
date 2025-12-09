import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import com.rabbitmq.client.Connection;

class Broker implements AutoCloseable {
    final static ConnectionFactory factory = new ConnectionFactory();
    final Connection connection;

    Broker() throws IOException, TimeoutException {
        this.connection = factory.newConnection();
    }
    static {
        factory.setHost("localhost");
    }

    TaskChannel getTaskChannel() throws IOException {
        return new TaskChannel(connection);
    }

    ResultChannel getResultChannel() throws IOException {
        return new ResultChannel(connection);
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }
}
