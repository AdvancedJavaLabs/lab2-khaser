import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

class Task implements Serializable {
    static AtomicInteger NEXT_ID = new AtomicInteger(0);
    final int id;
    final String text;
    final boolean endFlag;

    Task(String text) {
        this(text, false);
    }

    static Task stopSignal() {
        return new Task(null, true);
    }

    private Task(String text, boolean endFlag) {
        this.id = NEXT_ID.incrementAndGet();
        this.text = text;
        this.endFlag = endFlag;
    }

    @Override
    public String toString() {
        return String.format("Task[id=%d, text=..., endFlag=%b]", id, endFlag);
    }

    byte[] toBytes() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(this);
            return baos.toByteArray();
        }
    }

    static Task fromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (Task) ois.readObject();
        }
    }
}
