package com.example.registration_login_module;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Local fluency scoring system for speech-to-text input.
 * Evaluates fluency based on structural and linguistic patterns without external APIs.
 */
public class FluencyScorer {

    public static class FluencyResult {
        public int score;
        public String reason;

        public FluencyResult(int score, String reason) {
            this.score = score;
            this.reason = reason;
        }
    }

    // Filler words proxy for pauses/hesitation
    private static final Set<String> FILLERS = new HashSet<>(Arrays.asList(
            "uh", "um", "ah", "er", "like", "you know", "well", "so"
    ));

    // Common simple words to ignore in repetition checks
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "the", "a", "an", "and", "is", "of", "to", "in", "it"
    ));

    public FluencyResult calculateFluency(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new FluencyResult(0, "No speech detected.");
        }

        String[] words = text.toLowerCase().trim().split("\\s+");
        int baseScore = 100;
        StringBuilder reasonBuilder = new StringBuilder();

        // 1. Length Factor
        int lengthScore = calculateLengthPenalty(words);
        baseScore -= lengthScore;
        if (lengthScore > 0) reasonBuilder.append("Short sentence. ");

        // 2. Pause/Hesitation Factor (via filler words)
        int fillerPenalty = calculateFillerPenalty(words);
        baseScore -= fillerPenalty;
        if (fillerPenalty > 10) reasonBuilder.append("Frequent hesitation. ");

        // 3. Repetition Factor
        int repetitionPenalty = calculateRepetitionPenalty(words);
        baseScore -= repetitionPenalty;
        if (repetitionPenalty > 0) reasonBuilder.append("Word repetition. ");

        // 4. Vocabulary Bonus
        int vocabBonus = calculateVocabBonus(words);
        baseScore += vocabBonus;
        if (vocabBonus > 5) reasonBuilder.append("Good vocabulary choice. ");

        // Final score normalization
        int finalScore = Math.max(0, Math.min(100, baseScore));
        
        if (reasonBuilder.length() == 0) {
            reasonBuilder.append("Clear and natural delivery.");
        }

        return new FluencyResult(finalScore, reasonBuilder.toString().trim());
    }

    private int calculateLengthPenalty(String[] words) {
        if (words.length < 4) return 30; // Very short
        if (words.length < 7) return 15; // Moderate
        return 0;
    }

    private int calculateFillerPenalty(String[] words) {
        int count = 0;
        for (String w : words) {
            if (FILLERS.contains(w)) count++;
        }
        // Penalty: 8 points per filler, max 40
        return Math.min(40, count * 8);
    }

    private int calculateRepetitionPenalty(String[] words) {
        int penalty = 0;
        for (int i = 0; i < words.length - 1; i++) {
            // Check for immediate repetition (e.g., "I I like...")
            if (words[i].equals(words[i+1]) && !STOP_WORDS.contains(words[i])) {
                penalty += 15;
            }
        }
        return Math.min(30, penalty);
    }

    private int calculateVocabBonus(String[] words) {
        int bonus = 0;
        for (String w : words) {
            if (w.length() > 7) bonus += 2; // Bonus for longer, likely complex words
        }
        return Math.min(15, bonus);
    }
}
