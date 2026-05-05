package com.example.registration_login_module;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * MissionAIEngine — AI-powered backend for the Mission Hub.
 *
 * Uses Groq API (LLaMA 3.1) to:
 * 1. Generate dynamic grammar content (explanations, examples)
 * 2. Generate practice sentences (Hindi → English word banks)
 * 3. Generate translate/speak exercises
 * 4. Evaluate user answers with intelligent similarity matching
 * 5. Adapt difficulty based on performance
 */
public class MissionAIEngine {

    private static final String TAG = "MissionAIEngine";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final Context context;
    private final String apiKey;

    public MissionAIEngine(Context context) {
        this.context = context;
        this.apiKey = context.getString(R.string.groq_api_key);
    }

    // ===== CONTENT GENERATION =====

    /**
     * Generate Learn content (grammar rules + examples) for a chapter.
     */
    public void generateLearnContent(String grammarTopic, String difficulty,
                                     LearnContentCallback callback) {
        String prompt = "You are an English grammar teacher for Hindi-speaking students.\n\n"
                + "Generate a lesson on: \"" + grammarTopic + "\"\n"
                + "Difficulty: " + difficulty + "\n\n"
                + "Return EXACTLY this JSON format (no extra text):\n"
                + "{\n"
                + "  \"title\": \"...\",\n"
                + "  \"rules\": [\"rule1 with examples\", \"rule2 with examples\", \"rule3\"],\n"
                + "  \"examples\": [\"Example sentence 1\", \"Example sentence 2\", \"Example sentence 3\"]\n"
                + "}\n\n"
                + "Rules should be clear with bullet points. Include Hindi translations in parentheses where helpful.";

        callAI(prompt, response -> {
            try {
                JSONObject json = extractJSON(response);
                String title = json.getString("title");
                JSONArray rulesArr = json.getJSONArray("rules");
                JSONArray exArr = json.getJSONArray("examples");

                String[] rules = new String[rulesArr.length()];
                for (int i = 0; i < rulesArr.length(); i++) rules[i] = rulesArr.getString(i);

                String[] examples = new String[exArr.length()];
                for (int i = 0; i < exArr.length(); i++) examples[i] = exArr.getString(i);

                callback.onContent(new GrammarDataProvider.LearnContent(title, 1, "Learn", 20, "", new java.util.ArrayList<>()));
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse learn content", e);
                callback.onError("Failed to generate content: " + e.getMessage());
            }
        });
    }

    /**
     * Generate Practice sentences (Hindi → English with word bank).
     */
    public void generatePracticeSentences(String grammarTopic, String difficulty,
                                          int count, PracticeCallback callback) {
        String prompt = "You are an English grammar exercise generator for Hindi students.\n\n"
                + "Grammar topic: \"" + grammarTopic + "\"\n"
                + "Difficulty: " + difficulty + "\n"
                + "Generate " + count + " sentences.\n\n"
                + "Return EXACTLY this JSON format (no extra text):\n"
                + "{\n"
                + "  \"sentences\": [\n"
                + "    {\n"
                + "      \"hindi\": \"Hindi sentence here\",\n"
                + "      \"english\": \"Correct English translation\",\n"
                + "      \"words\": [\"correct\", \"words\", \"plus\", \"2\", \"distractor\", \"words\"]\n"
                + "    }\n"
                + "  ]\n"
                + "}\n\n"
                + "The words array must contain ALL words from the correct English sentence PLUS 2-3 distractor words. "
                + "All words should be individual (split by spaces).";

        callAI(prompt, response -> {
            try {
                JSONObject json = extractJSON(response);
                JSONArray arr = json.getJSONArray("sentences");
                List<GrammarDataProvider.PracticeSentence> sentences = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject s = arr.getJSONObject(i);
                    JSONArray wordsArr = s.getJSONArray("words");
                    String[] words = new String[wordsArr.length()];
                    for (int j = 0; j < wordsArr.length(); j++) words[j] = wordsArr.getString(j);

                    sentences.add(new GrammarDataProvider.PracticeSentence(
                            s.getString("hindi"), s.getString("english"), words));
                }
                callback.onSentences(sentences);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse practice content", e);
                callback.onError("Failed to generate: " + e.getMessage());
            }
        });
    }

    /**
     * Generate Translate sentences (Hindi → English speech).
     */
    public void generateTranslateSentences(String grammarTopic, String difficulty,
                                           int count, TranslateCallback callback) {
        String prompt = "Generate " + count + " Hindi-to-English translation exercises.\n\n"
                + "Grammar topic: \"" + grammarTopic + "\"\n"
                + "Difficulty: " + difficulty + "\n\n"
                + "Return EXACTLY this JSON (no extra text):\n"
                + "{\n"
                + "  \"sentences\": [\n"
                + "    {\"hindi\": \"Hindi sentence\", \"english\": \"Expected English translation\"}\n"
                + "  ]\n"
                + "}\n"
                + "Keep sentences natural and conversational.";

        callAI(prompt, response -> {
            try {
                JSONObject json = extractJSON(response);
                JSONArray arr = json.getJSONArray("sentences");
                List<GrammarDataProvider.TranslateSentence> sentences = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject s = arr.getJSONObject(i);
                    sentences.add(new GrammarDataProvider.TranslateSentence(
                            s.getString("hindi"), s.getString("english")));
                }
                callback.onSentences(sentences);
            } catch (Exception e) {
                callback.onError("Failed to generate: " + e.getMessage());
            }
        });
    }

    /**
     * Generate Speaking questions for Q&A exercise.
     */
    public void generateSpeakQuestions(String grammarTopic, String difficulty,
                                       int count, SpeakCallback callback) {
        String prompt = "Generate " + count + " speaking practice questions for English learners.\n\n"
                + "Grammar topic: \"" + grammarTopic + "\"\n"
                + "Difficulty: " + difficulty + "\n\n"
                + "Return EXACTLY this JSON (no extra text):\n"
                + "{\n"
                + "  \"questions\": [\n"
                + "    {\"question\": \"Question in English?\", \"keywords\": \"expected key words in answer\"}\n"
                + "  ]\n"
                + "}\n"
                + "Questions should encourage using the grammar topic. "
                + "Keywords are key words you expect in a correct answer.";

        callAI(prompt, response -> {
            try {
                JSONObject json = extractJSON(response);
                JSONArray arr = json.getJSONArray("questions");
                List<GrammarDataProvider.SpeakQuestion> questions = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject q = arr.getJSONObject(i);
                    questions.add(new GrammarDataProvider.SpeakQuestion(
                            q.getString("question"), q.getString("keywords")));
                }
                callback.onQuestions(questions);
            } catch (Exception e) {
                callback.onError("Failed to generate: " + e.getMessage());
            }
        });
    }

    // ===== ANSWER EVALUATION =====

    /**
     * Evaluate a user's spoken/typed answer using AI.
     * Accepts multiple valid variations, not just one fixed answer.
     * Handles both Speak (open Q&A with keyword hints) and Translate (exact sentence) missions.
     */
    public void evaluateAnswer(String userAnswer, String expectedAnswer,
                               String grammarTopic, EvaluationCallback callback) {
        String prompt = "You are a precise English grammar evaluator for a language learning app. Hindi-speaking students are learning English.\n\n"
                + "Grammar topic: \"" + grammarTopic + "\"\n"
                + "Reference answer: \"" + expectedAnswer + "\"\n"
                + "Student said: \"" + userAnswer + "\"\n\n"
                + "STEP 1 — Decide if the answer is correct:\n"
                + "- The answer IS CORRECT if it is grammatically valid English AND answers the question appropriately.\n"
                + "- The reference answer may be PARTIAL (e.g., 'I wake up at'). In that case, any valid completion is correct (e.g., 'I wake up at 7 AM' ✓, 'I wake up at six o clock' ✓).\n"
                + "- Accept synonyms: 'get up' = 'wake up', 'mom' = 'mother', 'big' = 'large'.\n"
                + "- Accept extra details: 'I wake up early in the morning at 6' for expected 'I wake up at' is CORRECT.\n"
                + "- Accept minor speech-to-text noise: 'dont' = 'don\'t', no punctuation, etc.\n"
                + "- The answer is WRONG only if: wrong tense, wrong grammar structure, or completely unrelated meaning.\n\n"
                + "STEP 2 — Score:\n"
                + "- 90-100: Perfect or near-perfect answer\n"
                + "- 70-89: Correct meaning but minor issues\n"
                + "- 40-69: Partially correct, some errors\n"
                + "- 0-39: Mostly wrong or unrelated\n\n"
                + "STEP 3 — Highlight each word the student said:\n"
                + "- Mark 'correct' if the word fits grammatically.\n"
                + "- Mark 'wrong' ONLY for genuinely incorrect words (wrong preposition, wrong verb form, extra wrong word).\n"
                + "- Do NOT mark additional correct words as wrong just because they don't appear in the reference.\n\n"
                + "STEP 4 — Always provide the ideal correct full sentence in 'correction', even when the student is correct.\n\n"
                + "STEP 5 — Give 1-2 specific tips. If the answer is correct, give a tip about the grammar topic. If wrong, explain the specific error simply.\n\n"
                + "Return this exact JSON:\n"
                + "{\n"
                + "  \"correct\": true,\n"
                + "  \"score\": 85,\n"
                + "  \"summary\": \"Short 1 line feedback\",\n"
                + "  \"highlightedUserAnswer\": [{\"word\": \"I\", \"status\": \"correct\"}, {\"word\": \"wake\", \"status\": \"correct\"}],\n"
                + "  \"correction\": \"I wake up at 7 AM every day.\",\n"
                + "  \"tips\": [\"Use 'at' before specific times.\"]\n"
                + "}";

        callAI(prompt, response -> {
            try {
                JSONObject json = extractJSON(response);
                EvaluationResult result = new EvaluationResult();
                result.isCorrect = json.getBoolean("correct");
                result.score = json.getInt("score");
                result.summary = json.optString("summary", result.isCorrect ? "Great job!" : "Needs practice");
                result.correction = json.optString("correction", expectedAnswer);
                
                // Ensure correction is never empty — always show the model answer
                if (result.correction == null || result.correction.trim().isEmpty()) {
                    result.correction = expectedAnswer;
                }
                
                JSONArray highlights = json.optJSONArray("highlightedUserAnswer");
                if (highlights != null) {
                    for (int i = 0; i < highlights.length(); i++) {
                        JSONObject h = highlights.getJSONObject(i);
                        result.highlightedUserAnswer.add(new HighlightedWord(
                                h.getString("word"), h.getString("status")));
                    }
                }
                
                // If AI returned no highlights, build them from user's words
                if (result.highlightedUserAnswer.isEmpty() && userAnswer != null) {
                    for (String w : userAnswer.trim().split("\\s+")) {
                        result.highlightedUserAnswer.add(new HighlightedWord(w,
                                result.isCorrect ? "correct" : "wrong"));
                    }
                }
                
                JSONArray tips = json.optJSONArray("tips");
                if (tips != null) {
                    for (int i = 0; i < tips.length(); i++)
                        result.tips.add(tips.getString(i));
                }
                
                // Ensure at least one tip
                if (result.tips.isEmpty()) {
                    result.tips.add(result.isCorrect
                            ? "Great use of " + grammarTopic + "!"
                            : "Review the correct answer and try again.");
                }
                
                callback.onEvaluated(result);
            } catch (Exception e) {
                Log.w(TAG, "AI parse failed, using fallback", e);
                EvaluationResult fallback = simpleFallbackEval(userAnswer, expectedAnswer);
                callback.onEvaluated(fallback);
            }
        });
    }

    /**
     * Get adaptive difficulty based on user's performance history.
     */
    public String getAdaptiveDifficulty(int correctCount, int totalCount) {
        if (totalCount == 0) return "beginner";
        double accuracy = (double) correctCount / totalCount;
        if (accuracy >= 0.85) return "advanced";
        if (accuracy >= 0.60) return "intermediate";
        return "beginner";
    }

    // ===== CORE API CALL =====

    private void callAI(String prompt, AIResponseCallback callback) {
        new Thread(() -> {
            try {
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    callback.onResponse("ERR: API key not configured");
                    return;
                }

                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                JSONObject body = new JSONObject();
                body.put("model", MODEL);

                JSONArray messages = new JSONArray();
                JSONObject sysMsg = new JSONObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", "You are a JSON-only response bot. "
                        + "Always return valid JSON without markdown formatting or extra text. "
                        + "Be accurate and fair in evaluations. Never mark correct answers as wrong.");
                messages.put(sysMsg);

                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", prompt);
                messages.put(userMsg);

                body.put("messages", messages);
                body.put("max_tokens", 1000);
                body.put("temperature", 0.3);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }

                if (conn.getResponseCode() == 200) {
                    InputStream is = conn.getInputStream();
                    InputStreamReader reader = new InputStreamReader(is, "UTF-8");
                    StringBuilder sb = new StringBuilder();
                    char[] buf = new char[1024];
                    int n;
                    while ((n = reader.read(buf)) != -1) sb.append(buf, 0, n);

                    JSONObject resp = new JSONObject(sb.toString());
                    String content = resp.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content").trim();

                    callback.onResponse(content);
                } else {
                    Log.e(TAG, "API error: " + conn.getResponseCode());
                    callback.onResponse("ERR: API error " + conn.getResponseCode());
                }
            } catch (Exception e) {
                Log.e(TAG, "AI call failed", e);
                callback.onResponse("ERR: " + e.getMessage());
            }
        }).start();
    }

    private JSONObject extractJSON(String response) throws Exception {
        // Strip markdown code fences if present
        String clean = response.trim();
        if (clean.startsWith("```json")) clean = clean.substring(7);
        else if (clean.startsWith("```")) clean = clean.substring(3);
        if (clean.endsWith("```")) clean = clean.substring(0, clean.length() - 3);
        clean = clean.trim();

        // Find the first { and last }
        int start = clean.indexOf('{');
        int end = clean.lastIndexOf('}');
        if (start >= 0 && end > start) {
            clean = clean.substring(start, end + 1);
        }

        return new JSONObject(clean);
    }

    /**
     * Improved fallback evaluator when AI API is unavailable.
     * Uses normalized word matching with fuzzy tolerance for speech-to-text noise.
     */
    private EvaluationResult simpleFallbackEval(String user, String expected) {
        EvaluationResult r = new EvaluationResult();
        String u = normalize(user);
        String e = normalize(expected);

        String[] expWords = e.split("\\s+");
        String[] userWords = u.split("\\s+");
        
        // Track which expected words were matched
        boolean[] expMatched = new boolean[expWords.length];
        
        for (String uw : userWords) {
            boolean isMatch = false;
            for (int i = 0; i < expWords.length; i++) {
                if (!expMatched[i] && isFuzzyMatch(uw, expWords[i])) {
                    isMatch = true;
                    expMatched[i] = true;
                    break;
                }
            }
            // Use original word (not normalized) for display
            String displayWord = uw;
            for (String origWord : user.trim().split("\\s+")) {
                if (normalize(origWord).equals(uw)) {
                    displayWord = origWord;
                    break;
                }
            }
            r.highlightedUserAnswer.add(new HighlightedWord(displayWord, isMatch ? "correct" : "wrong"));
        }
        
        int matchCount = 0;
        for (boolean m : expMatched) { if (m) matchCount++; }
        
        // Score based on how many expected words were covered
        int coverage = expWords.length > 0 ? (matchCount * 100 / expWords.length) : 0;
        // Also consider if user said too many extra words
        int precision = userWords.length > 0 ? (matchCount * 100 / userWords.length) : 0;
        r.score = Math.min(100, (coverage * 3 + precision) / 4); // Weight coverage more
        
        r.isCorrect = r.score >= 65;
        r.correction = expected; // Always show the reference answer
        
        if (r.isCorrect) {
            r.summary = r.score >= 90 ? "Excellent! Your answer is correct." : "Good job! Your answer is mostly correct.";
            r.tips.add("Keep practicing to improve fluency!");
        } else {
            r.summary = "Not quite right. Compare your answer with the correct one below.";
            r.tips.add("Look at the correct answer and notice the differences.");
            if (matchCount == 0) {
                r.tips.add("Try to use the key words from the expected answer.");
            }
        }
        return r;
    }
    
    /** Normalize a string for comparison: lowercase, strip punctuation, collapse whitespace. */
    private String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
    
    /** Fuzzy match: exact match OR one character difference (handles speech-to-text typos). */
    private boolean isFuzzyMatch(String a, String b) {
        if (a.equals(b)) return true;
        if (Math.abs(a.length() - b.length()) > 1) return false;
        // Allow one-character edit distance
        int edits = 0;
        int i = 0, j = 0;
        while (i < a.length() && j < b.length()) {
            if (a.charAt(i) != b.charAt(j)) {
                edits++;
                if (edits > 1) return false;
                if (a.length() > b.length()) i++;
                else if (b.length() > a.length()) j++;
                else { i++; j++; }
            } else { i++; j++; }
        }
        edits += (a.length() - i) + (b.length() - j);
        return edits <= 1;
    }

    // ===== DATA CLASSES =====

    public static class HighlightedWord {
        public String word;
        public String status; // "correct", "wrong", or "missing"
        public HighlightedWord(String w, String s) { this.word = w; this.status = s; }
    }

    public static class EvaluationResult {
        public boolean isCorrect;
        public int score;
        public String summary;
        public String correction;
        public List<HighlightedWord> highlightedUserAnswer = new ArrayList<>();
        public List<String> tips = new ArrayList<>();
    }

    // ===== CALLBACKS =====

    private interface AIResponseCallback {
        void onResponse(String response);
    }

    public interface LearnContentCallback {
        void onContent(GrammarDataProvider.LearnContent content);
        void onError(String error);
    }

    public interface PracticeCallback {
        void onSentences(List<GrammarDataProvider.PracticeSentence> sentences);
        void onError(String error);
    }

    public interface TranslateCallback {
        void onSentences(List<GrammarDataProvider.TranslateSentence> sentences);
        void onError(String error);
    }

    public interface SpeakCallback {
        void onQuestions(List<GrammarDataProvider.SpeakQuestion> questions);
        void onError(String error);
    }

    public interface EvaluationCallback {
        void onEvaluated(EvaluationResult result);
    }
}
