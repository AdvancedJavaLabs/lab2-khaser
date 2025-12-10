import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.*;

class Result implements Serializable {
    int id;
    int wordsCnt;
    Map<String, Integer> wordFrequency;
    int positiveWordsCnt;
    int negativeWordsCnt;
    boolean endFlag;
    StringBuilder modifiedText;
    private final static HashSet<String> POSITIVE_WORDS =
        new HashSet<>(parseWordList(Main.class.getResourceAsStream("positive_words.txt")));
    private final static HashSet<String> NEGATIVE_WORDS =
        new HashSet<>(parseWordList(Main.class.getResourceAsStream("negative_words.txt")));

    private final static String PROPER_NOUN_REPLACEMENT = "CENSORED";

    static ArrayList<String> parseWordList(InputStream is) {
        Scanner scanner = new Scanner(is);

        var res = new ArrayList<String>();
        while (scanner.hasNext()) {
            String word = scanner.next().toLowerCase();
            if (!word.isEmpty()) {
                res.add(word);
            }
        }
        scanner.close();
        return res;
    }

    public Result() {
        this(0, 0, 0, 0, new HashMap<>(), new StringBuilder());
    }

    private Result(int id, int cntWords, int positiveWordsCnt, int negativeWordsCnt, Map<String, Integer> wordFrequency, StringBuilder modifiedText) {
        this(id, cntWords, positiveWordsCnt, negativeWordsCnt, wordFrequency, modifiedText, false);
    }

    private Result(int id, int cntWords, int positiveWordsCnt, int negativeWordsCnt, Map<String, Integer> wordFrequency, StringBuilder modifiedText, boolean endFlag) {
        this.id = id;
        this.wordsCnt = cntWords;
        this.positiveWordsCnt = positiveWordsCnt;
        this.negativeWordsCnt = negativeWordsCnt;
        this.modifiedText = modifiedText;
        this.wordFrequency = wordFrequency;
        this.endFlag = endFlag;
    }

    static Result fromText(int id, String text) {
        int wordsCnt = 0;
        int positiveWordsCnt = 0;
        int negativeWordsCnt = 0;
        Map<String, Integer> frequencyMap = new HashMap<>();

        List<String> words = parseWordList(new ByteArrayInputStream(text.getBytes()));
        for (var word : words) {
            wordsCnt++;
            frequencyMap.put(word, frequencyMap.getOrDefault(word, 0) + 1);
            positiveWordsCnt += (POSITIVE_WORDS.contains(word) ? 1 : 0);
            negativeWordsCnt += (NEGATIVE_WORDS.contains(word) ? 1 : 0);
        }

        String modifiedString = text.lines()
            .map((line) -> replaceProperNouns(line, PROPER_NOUN_REPLACEMENT))
            .collect(Collectors.joining("\n"));

        return new Result(id, wordsCnt, positiveWordsCnt, negativeWordsCnt, frequencyMap, new StringBuilder(modifiedString));
    }

    void add(Result oth) {
        wordsCnt += oth.wordsCnt;
        positiveWordsCnt += oth.positiveWordsCnt;
        negativeWordsCnt += oth.negativeWordsCnt;
        modifiedText.append("\n").append(oth.modifiedText);

        for (Map.Entry<String, Integer> entry : oth.wordFrequency.entrySet()) {
            wordFrequency.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    public List<Map.Entry<String, Integer>> getTopFrequentWords(int topN) {
        return wordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer> comparingByValue().reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    static Result stopSignal(int id) {
        return new Result(id, 0, 0, 0, null, null, true);
    }

    @Override
    public String toString() {
        return String.format("Result[id=%d, wordsCnt=%d, positiveWordsCnt=%d, negativeWordsCnt=%d endFlag=%b]",
                                id,
                                wordsCnt,
                                positiveWordsCnt,
                                negativeWordsCnt,
                                endFlag);
    }

    String getModifiedText() {
        return modifiedText.toString();
    }


    public static String replaceProperNouns(String text, String replacementWord) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String regex = "\\b[A-Z][a-z]*\\b";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String word = matcher.group();

            boolean shouldReplace = true;

            int start = matcher.start();
            if (start > 0) {
                char prevChar = text.charAt(start - 1);
                if (prevChar == '"') {
                    shouldReplace = false;
                }
                if (start > 1) {
                    char ppChar = text.charAt(start - 2);
                    if (ppChar == '.' || ppChar == '!' || ppChar == '?') {
                        shouldReplace = false;
                    }
                }
            } else {
                shouldReplace = false;
            }

            if (shouldReplace) {
                matcher.appendReplacement(result, replacementWord);
            } else {
                matcher.appendReplacement(result, word);
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

}
