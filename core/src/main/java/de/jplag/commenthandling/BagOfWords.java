package de.jplag.commenthandling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BagOfWords {
    private final Map<String, Integer> wordOccurances;

    public BagOfWords(List<String> words) {
        wordOccurances = new HashMap<>();

        for (String word : words) {
            this.addWord(word);
        }
    }

    public BagOfWords(Map<String, Integer> wordOccurances) {
        this.wordOccurances = new HashMap<>(wordOccurances);
    }

    public void addWord(String word) {
        if (wordOccurances.containsKey(word)) {
            wordOccurances.put(word, wordOccurances.get(word) + 1);
        } else {
            wordOccurances.put(word, 1);
        }
    }

    public List<String> getWords() {
        return new ArrayList<>(wordOccurances.keySet());
    }

    public void extendVocabulary(BagOfWords otherBag) {
        for (String word : otherBag.getWords()) {
            if (!wordOccurances.containsKey(word)) {
                wordOccurances.put(word, 0);
            }
        }
    }

    public Integer[] getSortedOccuranceVector() {
        return wordOccurances.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toArray(Integer[]::new);
    }

    public double cosineSimilarityTo(BagOfWords otherBag) {
        otherBag.extendVocabulary(this);
        this.extendVocabulary(otherBag);

        double dotProduct = 0;
        double thisLength = 0;
        double otherLength = 0;

        Integer[] thisOccurances = getSortedOccuranceVector();
        Integer[] otherOccurances = otherBag.getSortedOccuranceVector();
        for (int i = 0; i < thisOccurances.length; i++) {
            dotProduct += thisOccurances[i] * otherOccurances[i];
            thisLength += Math.pow(thisOccurances[i], 2);
            otherLength += Math.pow(otherOccurances[i], 2);
        }

        thisLength = Math.sqrt(thisLength);
        otherLength = Math.sqrt(otherLength);

        if (thisLength == 0 || otherLength == 0) {
            return 0;
        }

        return dotProduct / (thisLength * otherLength);
    }

    public BagOfWords copy() {
        return new BagOfWords(wordOccurances);
    }
}
