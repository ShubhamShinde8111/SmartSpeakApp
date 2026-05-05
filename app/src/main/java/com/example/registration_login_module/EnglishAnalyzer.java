package com.example.registration_login_module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A lightweight, heuristic-based English analyzer for Android.
 * Performs local processing without external APIs or libraries.
 */
public class EnglishAnalyzer {

    public static class AnalysisResult {
        public String level;
        public List<String> strengths = new ArrayList<>();
        public List<String> weaknesses = new ArrayList<>();
        public int overallScore; // 0-100

        @Override
        public String toString() {
            return "Level: " + level + "\nStrengths: " + strengths + "\nWeaknesses: " + weaknesses;
        }
    }

    // Vocabulary lists for leveling
    private static final Set<String> ADVANCED_WORDS = new HashSet<>(Arrays.asList(
        "nevertheless", "furthermore", "consequently", "ambiguous", "pragmatic", 
        "resilient", "ubiquitous", "eloquent", "scrutinize", "advocate", "implementation"
    ));

    private static final Set<String> INTERMEDIATE_WORDS = new HashSet<>(Arrays.asList(
        "important", "however", "because", "although", "different", "experience", 
        "knowledge", "understand", "believe", "possible", "together"
    ));

    public AnalysisResult analyze(String text) {
        AnalysisResult result = new AnalysisResult();
        if (text == null || text.trim().isEmpty()) {
            result.level = "N/A";
            result.weaknesses.add("No input detected.");
            return result;
        }

        text = text.trim();
        String[] words = text.toLowerCase().split("\\s+");
        
        checkGrammar(text, result);
        checkClarity(text, words, result);
        determineLevel(words, result);
        
        calculateScore(result);
        
        return result;
    }

    private void checkGrammar(String text, AnalysisResult result) {
        // Subject-Verb Agreement (Simple heuristics)
        String lower = text.toLowerCase();
        
        // Check "I is/was" vs "I am"
        if (Pattern.compile("\\bi is\\b").matcher(lower).find()) {
            result.weaknesses.add("Grammar: Use 'I am' instead of 'I is'.");
        }
        if (Pattern.compile("\\bi are\\b").matcher(lower).find()) {
            result.weaknesses.add("Grammar: Use 'I am' instead of 'I are'.");
        }
        
        // Check "He/She/It are"
        if (Pattern.compile("\\b(he|she|it) are\\b").matcher(lower).find()) {
            result.weaknesses.add("Grammar: Use 'is' with third-person singular subjects (He/She/It).");
        }
        
        // Article check: "a" vs "an"
        Matcher articleMatcher = Pattern.compile("\\b(a|an)\\s+(\\w+)").matcher(lower);
        while (articleMatcher.find()) {
            String article = articleMatcher.group(1);
            String nextWord = articleMatcher.group(2);
            boolean startsWithVowel = "aeiou".indexOf(nextWord.charAt(0)) != -1;
            
            if (article.equals("a") && startsWithVowel) {
                result.weaknesses.add("Grammar: Use 'an' before words starting with a vowel sound ('" + nextWord + "').");
            } else if (article.equals("an") && !startsWithVowel) {
                // Heuristic: doesn't handle silent 'h' or 'u' as 'y' sound perfectly
                if (!nextWord.startsWith("h") && !nextWord.startsWith("u")) {
                    result.weaknesses.add("Grammar: Use 'a' before words starting with a consonant ('" + nextWord + "').");
                }
            }
        }
        
        if (result.weaknesses.isEmpty()) {
            result.strengths.add("Good basic grammar structure.");
        }
    }

    private void checkClarity(String text, String[] words, AnalysisResult result) {
        // Sentence length
        if (words.length < 3) {
            result.weaknesses.add("Clarity: Sentence is very short or incomplete.");
        } else if (words.length > 8) {
            result.strengths.add("Good sentence length and detail.");
        }

        // Check for ending punctuation (if transcription includes it)
        if (text.length() > 0 && !text.matches(".*[.!?]$")) {
            // Speech transcription often lacks punctuation, so this is a soft warning
            // result.weaknesses.add("Note: Punctuation is missing.");
        }
    }

    private void determineLevel(String[] words, AnalysisResult result) {
        int advancedCount = 0;
        int intermediateCount = 0;

        for (String word : words) {
            if (ADVANCED_WORDS.contains(word)) advancedCount++;
            else if (INTERMEDIATE_WORDS.contains(word)) intermediateCount++;
        }

        if (advancedCount > 0 || words.length > 15) {
            result.level = "Advanced";
            result.strengths.add("Uses complex vocabulary or structures.");
        } else if (intermediateCount > 0 || words.length > 7) {
            result.level = "Intermediate";
            result.strengths.add("Clear communication with moderate vocabulary.");
        } else {
            result.level = "Basic";
            result.weaknesses.add("Vocabulary is quite simple.");
        }
    }

    private void calculateScore(AnalysisResult result) {
        int score = 70; // Start with base score
        
        score -= result.weaknesses.size() * 10;
        score += result.strengths.size() * 5;
        
        if ("Advanced".equals(result.level)) score += 10;
        if ("Intermediate".equals(result.level)) score += 5;
        
        result.overallScore = Math.max(0, Math.min(100, score));
    }
}