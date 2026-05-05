package com.example.registration_login_module;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirebaseProgressRepository {

    private static final String TAG = "FirebaseRepo";
    private final FirebaseFirestore db;
    private final LearningProgressDBHelper localDbHelper;

    public FirebaseProgressRepository(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.localDbHelper = new LearningProgressDBHelper(context);
    }

    // saveSession: Atomic update of History + Aggregates + Daily Stats
    public void saveSpeechSession(String uid, String originalText, int score, String level,
            List<String> strengths, List<String> weaknesses) {

        if (uid == null || uid.isEmpty())
            return;

        WriteBatch batch = db.batch();
        long now = System.currentTimeMillis();
        String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(now));

        // 1. Save Session Detail
        DocumentReference sessionRef = db.collection("users").document(uid)
                .collection("speech_history").document(String.valueOf(now));

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("timestamp", FieldValue.serverTimestamp());
        sessionData.put("text", originalText);
        sessionData.put("score", score);
        sessionData.put("level", level);
        sessionData.put("strengths", strengths);
        sessionData.put("weaknesses", weaknesses);
        batch.set(sessionRef, sessionData);

        // 2. Update Daily Stats (for Graph)
        DocumentReference dailyRef = db.collection("users").document(uid)
                .collection("daily_stats").document(dateKey);

        // We need to read daily stats first to calculate new average, OR use a simpler
        // "total_score" and "count" structure
        // Firestore doesn't support "update avg" atomically easily without reading.
        // Strategy: Store "daily_total_score" and "daily_count" and compute avg on
        // client, or strictly increment.
        Map<String, Object> dailyUpdate = new HashMap<>();
        dailyUpdate.put("total_score", FieldValue.increment(score));
        dailyUpdate.put("count", FieldValue.increment(1));
        dailyUpdate.put("timestamp", FieldValue.serverTimestamp()); // for sorting
        batch.set(dailyRef, dailyUpdate, SetOptions.merge());

        // 3. Update Aggregates (Speech Stats)
        DocumentReference statsRef = db.collection("users").document(uid)
                .collection("aggregates").document("speech_stats");

        Map<String, Object> statsUpdate = new HashMap<>();
        statsUpdate.put("total_sessions", FieldValue.increment(1));
        statsUpdate.put("total_score_sum", FieldValue.increment(score));

        // Track vocabulary trend (heuristic: +1 for advanced, 0 for others)
        if ("Advanced".equalsIgnoreCase(level)) {
            statsUpdate.put("advanced_count", FieldValue.increment(1));
        }

        // Add mistake/strength counts
        if (weaknesses != null && !weaknesses.isEmpty()) {
            statsUpdate.put("total_mistakes", FieldValue.increment(weaknesses.size()));
        }

        batch.set(statsRef, statsUpdate, SetOptions.merge());

        batch.commit().addOnSuccessListener(aVoid -> Log.d(TAG, "Session saved to Firebase"));

        // Also fire migration if needed (lazy check)
    }

    // Real-time listener for Dashboard Aggregates
    public void attachStatsListener(String uid, StatsListener listener) {
        db.collection("users").document(uid).collection("aggregates").document("speech_stats")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        long totalSessions = snapshot.getLong("total_sessions") != null
                                ? snapshot.getLong("total_sessions")
                                : 0;
                        long totalScoreSum = snapshot.getLong("total_score_sum") != null
                                ? snapshot.getLong("total_score_sum")
                                : 0;
                        long advCount = snapshot.getLong("advanced_count") != null ? snapshot.getLong("advanced_count")
                                : 0;

                        int avgScore = totalSessions > 0 ? (int) (totalScoreSum / totalSessions) : 0;
                        // Approx grammar accuracy: 100 - (mistakes per session * X).
                        // Or just use the avgScore as a proxy for "Fluency" and calculate Accuracy
                        // separately if we stored "total_grammar_errors".
                        // For now, let's map Avg Score -> Fluency.

                        listener.onStatsUpdated(avgScore, totalSessions, advCount);
                    } else {
                        listener.onStatsUpdated(0, 0, 0);
                    }
                });
    }

    // Get Grid/Chart Data
    public void getWeeklyHistory(String uid, WeeklyHistoryCallback callback) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        // Date 7 days ago
        // Firestore collection "daily_stats" keys are YYYY-MM-DD.
        // A range query on document ID is tricky if keys are strings.
        // Better to order by "timestamp" field in daily_stats.

        long sevenDaysAgo = cal.getTimeInMillis();

        db.collection("users").document(uid).collection("daily_stats")
                .whereGreaterThan("timestamp", sevenDaysAgo)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Float> weeklyMap = new HashMap<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String dateKey = doc.getId();
                        long total = doc.getLong("total_score") != null ? doc.getLong("total_score") : 0;
                        long count = doc.getLong("count") != null ? doc.getLong("count") : 0;
                        float avg = count > 0 ? (float) total / count : 0;
                        weeklyMap.put(dateKey, avg);
                    }
                    callback.onHistoryLoaded(weeklyMap);
                })
                .addOnFailureListener(e -> callback.onError(e));
    }

    // Get Recent Feedback
    public void getRecentFeedback(String uid, FeedbackCallback callback) {
        db.collection("users").document(uid).collection("speech_history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<String> strengths = new ArrayList<>();
                    List<String> weaknesses = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots) {
                        List<String> s = (List<String>) doc.get("strengths");
                        List<String> w = (List<String>) doc.get("weaknesses");
                        if (s != null)
                            strengths.addAll(s);
                        if (w != null)
                            weaknesses.addAll(w);
                    }
                    callback.onFeedbackLoaded(strengths, weaknesses);
                })
                .addOnFailureListener(e -> callback.onError(e));
    }

    // Migration Logic (One-time)
    public void checkAndMigrate(String uid, Runnable onComplete) {
        // Check if firebase has data
        db.collection("users").document(uid).collection("aggregates").document("speech_stats")
                .get().addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        // Cloud empty, check local
                        new Thread(() -> {
                            performMigration(uid);
                            if (onComplete != null)
                                onComplete.run();
                        }).start();
                    }
                });
    }

    private void performMigration(String uid) {
        // Read from SQLite
        android.database.sqlite.SQLiteDatabase sqlDb = localDbHelper.getReadableDatabase();
        Cursor c = sqlDb.rawQuery(
                "SELECT * FROM " + LearningProgressDBHelper.TABLE_CONVERSATIONS + " WHERE user_id = ?",
                new String[] { uid });

        if (c.moveToFirst()) {
            Log.d(TAG, "Migrating " + c.getCount() + " records...");
            // Iterate and push to Firebase
            // This might generate many writes. For huge data, use batches.
            // Simplified for now: just push recent or all one by one (Batch limit 500).

            WriteBatch batch = db.batch();
            int count = 0;

            do {
                String text = c.getString(c.getColumnIndexOrThrow(LearningProgressDBHelper.COL_USER_TEXT));
                int score = c.getInt(c.getColumnIndexOrThrow(LearningProgressDBHelper.COL_SCORE));
                String level = c.getString(c.getColumnIndexOrThrow(LearningProgressDBHelper.COL_LEVEL));
                long id = c.getLong(c.getColumnIndexOrThrow(LearningProgressDBHelper.COLUMN_ID));

                // Get mistakes/strengths for this conv
                List<String> mistakes = getLocalList(sqlDb, LearningProgressDBHelper.TABLE_MISTAKES, id);
                List<String> strengths = getLocalList(sqlDb, LearningProgressDBHelper.TABLE_STRENGTHS, id);

                // Write to Firestore logic (Duplicate of saveSession but in batch)
                // ... (Implementation of batch writes for history/daily/stats)
                // For brevity, calling the single save method inside loop (not atomic batch but
                // functionally same for migration)
                saveSpeechSession(uid, text, score, level, strengths, mistakes);
                try {
                    Thread.sleep(50);
                } catch (Exception e) {
                } // Avoid rate limit if simple loop

                count++;
            } while (c.moveToNext());
            Log.d(TAG, "Migration complete.");
        }
        c.close();
    }

    private List<String> getLocalList(android.database.sqlite.SQLiteDatabase db, String table, long convId) {
        List<String> list = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT description FROM " + table + " WHERE conversation_id = ?",
                new String[] { String.valueOf(convId) });
        while (c.moveToNext()) {
            list.add(c.getString(0));
        }
        c.close();
        return list;
    }

    public interface StatsListener {
        void onStatsUpdated(int avgScore, long totalSessions, long advancedCount);
    }

    public interface WeeklyHistoryCallback {
        void onHistoryLoaded(Map<String, Float> weeklyData);

        void onError(Exception e);
    }

    public interface FeedbackCallback {
        void onFeedbackLoaded(List<String> strengths, List<String> weaknesses);

        void onError(Exception e);
    }
}
