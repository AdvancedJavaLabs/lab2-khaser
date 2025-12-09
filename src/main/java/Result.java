import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Scanner;

class Result implements Serializable {
    int cntWords;
    boolean endFlag;

    public Result() {
        this(0);
    }

    private Result(int cntWords) {
        this(cntWords, false);
    }

    private Result(int cntWords, boolean endFlag) {
        this.cntWords = cntWords;
        this.endFlag = endFlag;
    }

    static Result fromText(String text) {
        return new Result(countWords(text));
    }

    void add(Result oth) {
        cntWords += oth.cntWords;
    }

    private static int countWords(String text) {
        int res = 0;
        Scanner scanner = new Scanner(text.trim());
        while (scanner.hasNext()) {
            scanner.next();
            res++;
        }
        scanner.close();
        return res;

    }

    static Result stopSignal() {
        return new Result(0, true);
    }

    @Override
    public String toString() {
        return String.format("Result[cndWords=%d, endFlag=%b]", cntWords, endFlag);
    }

    byte[] toBytes() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(this);
            return baos.toByteArray();
        }
    }

    static Result fromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (Result) ois.readObject();
        }
    }
}
