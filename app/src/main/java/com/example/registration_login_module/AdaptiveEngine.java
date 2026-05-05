package com.example.registration_login_module;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * AdaptiveEngine — Intelligent difficulty adjustment system.
 *
 * Tracks per-category accuracy using a weighted moving average of the last 5 scores.
 * Adjusts the next mission based on performance:
 * - If accuracy < 60% on a category → repeat that category next
 * - If accuracy > 90% → skip to harder content, unlock bonus challenges
 * - Normal range → continue standard progression
 *
 * Categories tracked: vocabulary, tense, preposition, article, conversation, speech
 */
public class AdaptiveEngine {

    private static final String TAG = "AdaptiveEngine";
    private static final int MAX_HISTORY = 5; // weighted moving average window
    private final FirebaseFirestore db;

    public AdaptiveEngine() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ===== SCORE RECORDING =====

    /**
     * Record a score for a specific category. Maintains a rolling window of last 5 scores.
     *
     * @param userId   Firebase UID
     * @param category Category: "basic", "tense", "preposition", "article", "conversation", "speech"
     * @param score    Score as percentage (0-100)
     */
    public void recordScore(String userId, String category, int score) {
        if (userId == null || userId.isEmpty() || category == null) return;

        // Normalize category name
        String normalizedCategory = normalizeCategory(category);

        db.collection("users").document(userId)
                .collection("gamification").document("adaptive")
                .get()
                .addOnSuccessListener(doc -> {
                    Map<String, Object> data = doc.exists() ? new HashMap<>(doc.getData()) : new HashMap<>();

                    // Get existing scores for this category
                    String scoresKey = normalizedCategory + "_scores";
                    String countKey = normalizedCategory + "_count";
                    String avgKey = normalizedCategory + "_avg";

                    // Store the latest score and running average
                    // Using a simple approach: total_sum and count, plus latest score
                    long existingCount = data.containsKey(countKey) && data.get(countKey) != null
                            ? (Long) data.get(countKey) : 0;
                    double existingAvg = data.containsKey(avgKey) && data.get(avgKey) != null
                            ? ((Number) data.get(avgKey)).doubleValue() : 0;

                    // Weighted moving average: give more weight to recent scores
                    double newAvg;
                    if (existingCount == 0) {
                        newAvg = score;
                    } else {
                        // Exponential moving average with alpha = 0.4 (recent scores weigh more)
                        double alpha = 0.4;
                        newAvg = alpha * score + (1 - alpha) * existingAvg;
                    }

                    data.put(countKey, existingCount + 1);
                    data.put(avgKey, newAvg);
                    data.put(normalizedCategory + "_latest", score);
                    data.put(normalizedCategory + "_lastUpdated", System.currentTimeMillis());

                    db.collection("users").document(userId)
                            .collection("gamification").document("adaptive")
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener(v -> Log.d(TAG, "Score recorded: " + normalizedCategory + " = " + score))
                            .addOnFailureListener(e -> Log.e(TAG, "Error recording score", e));
                });
    }

    // ===== CATEGORY ANALYSIS =====

    /**
     * Get the weakest category (lowest average score).
     */
    public void getWeakestCategory(String userId, CategoryCallback callback) {
        db.collection("users").document(userId)
                .collection("gamification").document("adaptive")
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onResult("basic", 0); // Default to basics
                        return;
                    }

                    String[] categories = {"basic", "tense", "preposition", "article"};
                    String weakest = "basic";
                    double lowestAvg = 100;

                    for (String cat : categories) {
                        Double avg = doc.getDouble(cat + "_avg");
                        if (avg != null && avg < lowestAvg) {
                            lowestAvg = avg;
                            weakest = cat;
                        }
                    }

                    callback.onResult(weakest, (int) lowestAvg);
                })
                .addOnFailureListener(e -> callback.onResult("basic", 0));
    }

    /**
     * Get the strongest category (highest average score).
     */
    public void getStrongestCategory(String userId, CategoryCallback callback) {
        db.collection("users").document(userId)
                .collection("gamification").document("adaptive")
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onResult("basic", 0);
                        return;
                    }

                    String[] categories = {"basic", "tense", "preposition", "article"};
                    String strongest = "basic";
                    double highestAvg = 0;

                    for (String cat : categories) {
                        Double avg = doc.getDouble(cat + "_avg");
                        if (avg != null && avg > highestAvg) {
                            highestAvg = avg;
                            strongest = cat;
                        }
                    }

                    callback.onResult(strongest, (int) highestAvg);
                })
                .addOnFailureListener(e -> callback.onResult("basic", 0));
    }

    /**
     * Get all category averages for the adaptive dashboard.
     */
    public void getAllCategoryScores(String userId, AllCategoriesCallback callback) {
        db.collection("users").document(userId)
                .collection("gamification").document("adaptive")
                .get()
                .addOnSuccessListener(doc -> {
                    Map<String, Integer> scores = new HashMap<>();
                    String[] categories = {"basic", "tense", "preposition", "article", "conversation", "speech"};

                    for (String cat : categories) {
                        Double avg = doc.exists() ? doc.getDouble(cat + "_avg") : null;
                        scores.put(cat, avg != null ? avg.intValue() : 0);
                    }

                    callback.onResult(scores);
                })
                .addOnFailureListener(e -> callback.onResult(new HashMap<>()));
    }

    /**
     * Determine difficulty adjustment for a category.
     *
     * @return -1 (repeat/easier), 0 (normal), 1 (advance/harder)
     */
    public void getDifficultyAdjustment(String userId, String category, DifficultyCallback callback) {
        String normalizedCategory = normalizeCategory(category);

        db.collection("users").document(userId)
                .collection("gamification").document("adaptive")
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onResult(0, 0); // No data, use normal difficulty
                        return;
                    }

                    Double avg = doc.getDouble(normalizedCategory + "_avg");
                    if (avg == null) {
                        callback.onResult(0, 0);
                        return;
                    }

                    int adjustment;
                    if (avg < 60) {
                        adjustment = -1; // Repeat / easier
                    } else if (avg > 90) {
                        adjustment = 1;  // Advance / harder
                    } else {
                        adjustment = 0;  // Normal
                    }

                    callback.onResult(adjustment, avg.intValue());
                })
                .addOnFailureListener(e -> callback.onResult(0, 0));
    }

    /**
     * Check if user should repeat a specific category in their next mission.
     */
    public void shouldRepeatCategory(String userId, String category, RepeatCallback callback) {
        getDifficultyAdjustment(userId, category, (adjustment, avgScore) -> {
            callback.onResult(adjustment == -1, avgScore);
        });
    }

    // ===== HELPERS =====

    /**
     * Convert test type names to normalized category names.
     */
    private String normalizeCategory(String category) {
        if (category == null) return "basic";
        switch (category.toLowerCase()) {
            case "basic":
            case "basics":
            case "vocabulary":
                return "basic";
            case "tense":
            case "tenses":
                return "tense";
            case "preposition":
            case "prepositions":
                return "preposition";
            case "article":
            case "articles":
                return "article";
            case "conversation":
            case "ai_conversation":
                return "conversation";
            case "speech":
            case "speech_practice":
                return "speech";
            default:
                return category.toLowerCase();
        }
    }

    /**
     * Convert quiz score (correct out of total) to percentage.
     */
    public static int toPercentage(int correct, int total) {
        if (total == 0) return 0;
        return (int) ((correct / (double) total) * 100);
    }

    // ===== CALLBACKS =====

    public interface CategoryCallback {
        void onResult(String category, int avgScore);
    }

    public interface AllCategoriesCallback {
        void onResult(Map<String, Integer> categoryScores);
    }

    public interface DifficultyCallback {
        void onResult(int adjustment, int avgScore);
    }

    public interface RepeatCallback {
        void onResult(boolean shouldRepeat, int avgScore);
    }
}
