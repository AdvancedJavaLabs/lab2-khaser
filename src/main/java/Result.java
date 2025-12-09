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
}
