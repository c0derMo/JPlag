package de.jplag.commenthandling;

public class DamerauLevensthein {
    public static int calculateDistance(String word1, String word2) {

        int word1Length = word1.length();
        int word2Length = word2.length();

        if (word1Length == 0)
            return word2Length;
        if (word2Length == 0)
            return word1Length;

        int[][] dist = new int[word1Length + 1][word2Length + 1];
        for (int i = 0; i < word1Length + 1; i++) {
            dist[i][0] = i;
        }
        for (int j = 0; j < word2Length + 1; j++) {
            dist[0][j] = j;
        }
        for (int i = 1; i < word1Length + 1; i++) {
            for (int j = 1; j < word2Length + 1; j++) {
                int cost = word1.charAt(i - 1) == word2.charAt(j - 1) ? 0 : 1;
                dist[i][j] = Math.min(Math.min(dist[i - 1][j] + 1, dist[i][j - 1] + 1), dist[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && word1.charAt(i - 1) == word2.charAt(j - 2) && word1.charAt(i - 2) == word2.charAt(j - 1)) {
                    dist[i][j] = Math.min(dist[i][j], dist[i - 2][j - 2] + cost);
                }
            }
        }
        return dist[word1Length][word2Length];
    }
}