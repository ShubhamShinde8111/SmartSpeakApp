package com.example.registration_login_module;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.List;

public class LearningProgressDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "learning_progress.db";
    private static final int DATABASE_VERSION = 2; // Incremented version to apply schema fix

    // Table Names
    public static final String TABLE_CONVERSATIONS = "conversations";
    public static final String TABLE_MISTAKES = "mistakes";
    public static final String TABLE_STRENGTHS = "strengths";
    public static final String TABLE_DAILY_SCORES = "daily_scores";

    // Common Columns
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    // Conversations Columns
    public static final String COL_USER_TEXT = "user_text";
    public static final String COL_AI_RESPONSE = "ai_response";
    public static final String COL_SCORE = "score";
    public static final String COL_LEVEL = "proficiency_level";

    // Mistakes/Strengths Columns
    public static final String COL_CONV_ID = "conversation_id";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_CATEGORY = "category"; // e.g., "Grammar", "Vocabulary"

    // Daily Scores Columns
    public static final String COL_DATE = "date"; // YYYY-MM-DD
    public static final String COL_AVG_SCORE = "avg_score";
    public static final String COL_SENTENCE_COUNT = "sentence_count";

    public LearningProgressDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Conversations Table
        db.execSQL("CREATE TABLE " + TABLE_CONVERSATIONS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USER_ID + " TEXT, " +
                COL_USER_TEXT + " TEXT, " +
                COL_AI_RESPONSE + " TEXT, " +
                COL_SCORE + " INTEGER, " +
                COL_LEVEL + " TEXT, " +
                COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP)");

        // 2. Mistakes Table
        db.execSQL("CREATE TABLE " + TABLE_MISTAKES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CONV_ID + " INTEGER, " +
                COL_DESCRIPTION + " TEXT, " +
                COL_CATEGORY + " TEXT, " +
                "FOREIGN KEY(" + COL_CONV_ID + ") REFERENCES " + TABLE_CONVERSATIONS + "(" + COLUMN_ID + "))");

        // 3. Strengths Table
        db.execSQL("CREATE TABLE " + TABLE_STRENGTHS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CONV_ID + " INTEGER, " +
                COL_DESCRIPTION + " TEXT, " +
                "FOREIGN KEY(" + COL_CONV_ID + ") REFERENCES " + TABLE_CONVERSATIONS + "(" + COLUMN_ID + "))");

        // 4. Daily Scores Table (for Analytics) - Fixed UNIQUE constraint to be per
        // user
        db.execSQL("CREATE TABLE " + TABLE_DAILY_SCORES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USER_ID + " TEXT, " +
                COL_DATE + " TEXT, " +
                COL_AVG_SCORE + " REAL, " +
                COL_SENTENCE_COUNT + " INTEGER, " +
                "UNIQUE(" + COLUMN_USER_ID + ", " + COL_DATE + "))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONVERSATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MISTAKES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STRENGTHS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DAILY_SCORES);
        onCreate(db);
    }

    /**
     * Comprehensive method to save analysis results into multiple tables.
     */
    public void saveAnalysis(String userId, String userText, String aiResponse, EnglishAnalyzer.AnalysisResult result) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Insert into Conversations
            ContentValues convValues = new ContentValues();
            convValues.put(COLUMN_USER_ID, userId);
            convValues.put(COL_USER_TEXT, userText);
            convValues.put(COL_AI_RESPONSE, aiResponse);
            convValues.put(COL_SCORE, result.overallScore);
            convValues.put(COL_LEVEL, result.level);
            long convId = db.insert(TABLE_CONVERSATIONS, null, convValues);

            // Insert Mistakes
            for (String m : result.weaknesses) {
                ContentValues v = new ContentValues();
                v.put(COL_CONV_ID, convId);
                v.put(COL_DESCRIPTION, m);
                v.put(COL_CATEGORY, "Grammar/Clarity");
                db.insert(TABLE_MISTAKES, null, v);
            }

            // Insert Strengths
            for (String s : result.strengths) {
                ContentValues v = new ContentValues();
                v.put(COL_CONV_ID, convId);
                v.put(COL_DESCRIPTION, s);
                db.insert(TABLE_STRENGTHS, null, v);
            }

            // Update Daily Aggregates
            updateDailyStats(db, userId, result.overallScore);

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("LearningProgressDB", "Error saving analysis", e);
        } finally {
            db.endTransaction();
        }
    }

    private void updateDailyStats(SQLiteDatabase db, String userId, int newScore) {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());

        // Check if entry for today exists for this user
        Cursor cursor = db.query(TABLE_DAILY_SCORES, null, COL_DATE + "=? AND " + COLUMN_USER_ID + "=?",
                new String[] { today, userId }, null, null, null);

        if (cursor.moveToFirst()) {
            int count = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SENTENCE_COUNT));
            float currentAvg = cursor.getFloat(cursor.getColumnIndexOrThrow(COL_AVG_SCORE));

            float newAvg = ((currentAvg * count) + newScore) / (count + 1);

            ContentValues values = new ContentValues();
            values.put(COL_AVG_SCORE, newAvg);
            values.put(COL_SENTENCE_COUNT, count + 1);
            db.update(TABLE_DAILY_SCORES, values, COL_DATE + "=? AND " + COLUMN_USER_ID + "=?",
                    new String[] { today, userId });
        } else {
            ContentValues values = new ContentValues();
            values.put(COLUMN_USER_ID, userId);
            values.put(COL_DATE, today);
            values.put(COL_AVG_SCORE, (float) newScore);
            values.put(COL_SENTENCE_COUNT, 1);
            db.insert(TABLE_DAILY_SCORES, null, values);
        }
        cursor.close();
    }

    // ========== ADVANCED ANALYTICS LOGIC ==========

    /**
     * Calculates the percentage of sentences that had ZERO mistakes.
     * Formula: (Sentences with 0 mistakes / Total Sentences) * 100
     */
    public double getGrammarAccuracy(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Count total sentences
        Cursor totalCursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_CONVERSATIONS + " WHERE " + COLUMN_USER_ID + "=?",
                new String[] { userId });
        totalCursor.moveToFirst();
        int total = totalCursor.getInt(0);
        totalCursor.close();

        if (total == 0)
            return 0.0;

        // Count sentences that appear in the mistakes table
        Cursor mistakeSentencesCursor = db.rawQuery(
                "SELECT COUNT(DISTINCT " + COL_CONV_ID + ") FROM " + TABLE_MISTAKES +
                        " WHERE " + COL_CONV_ID + " IN (SELECT " + COLUMN_ID + " FROM " + TABLE_CONVERSATIONS
                        + " WHERE " + COLUMN_USER_ID + "=?)",
                new String[] { userId });
        mistakeSentencesCursor.moveToFirst();
        int sentencesWithMistakes = mistakeSentencesCursor.getInt(0);
        mistakeSentencesCursor.close();

        int correctSentences = total - sentencesWithMistakes;
        return (double) correctSentences / total * 100.0;
    }

    /**
     * Identifies if vocabulary level is moving from Basic -> Intermediate ->
     * Advanced.
     * Formula: Weighted average of levels (Basic=1, Inter=2, Adv=3) over time.
     * Returns: Positive value for improvement, negative for decline.
     */
    public double getVocabularyImprovementTrend(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Helper to get weighted score for a set of rows
        // We compare the first half of history with the second half
        String query = "SELECT " + COL_LEVEL + " FROM " + TABLE_CONVERSATIONS +
                " WHERE " + COLUMN_USER_ID + "=? ORDER BY " + COLUMN_TIMESTAMP + " ASC";
        Cursor cursor = db.rawQuery(query, new String[] { userId });

        int total = cursor.getCount();
        if (total < 10) {
            cursor.close();
            return 0.0;
        } // Need enough data for a trend

        int mid = total / 2;
        double firstHalfScore = 0;
        double secondHalfScore = 0;

        int i = 0;
        while (cursor.moveToNext()) {
            String level = cursor.getString(0);
            int weight = level.equalsIgnoreCase("Advanced") ? 3 : (level.equalsIgnoreCase("Intermediate") ? 2 : 1);
            if (i < mid)
                firstHalfScore += weight;
            else
                secondHalfScore += weight;
            i++;
        }
        cursor.close();

        double avg1 = firstHalfScore / mid;
        double avg2 = secondHalfScore / (total - mid);

        return avg2 - avg1; // Positive means user is using higher-level words now
    }

    /**
     * A holistic score (0-100) reflecting accuracy, complexity, and volume.
     * Formula: (Accuracy * 0.4) + (Complexity_Weight * 0.4) + (Volume_Weight * 0.2)
     */
    public int getFluencyScore(String userId) {
        double accuracy = getGrammarAccuracy(userId);

        SQLiteDatabase db = this.getReadableDatabase();
        // Volume: Sentence count compared to a milestone (e.g., 50 sentences = max
        // volume weight)
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CONVERSATIONS + " WHERE " + COLUMN_USER_ID + "=?",
                new String[] { userId });
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();

        double volumeWeight = Math.min(100.0, (count / 50.0) * 100.0);

        // Complexity: Current level weighted avg
        Cursor levelCursor = db.rawQuery("SELECT " + COL_LEVEL + " FROM " + TABLE_CONVERSATIONS +
                " WHERE " + COLUMN_USER_ID + "=? ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT 20",
                new String[] { userId });
        double complexitySum = 0;
        int levelCount = levelCursor.getCount();
        while (levelCursor.moveToNext()) {
            String level = levelCursor.getString(0);
            complexitySum += level.equalsIgnoreCase("Advanced") ? 100
                    : (level.equalsIgnoreCase("Intermediate") ? 70 : 40);
        }
        levelCursor.close();
        double complexityWeight = levelCount > 0 ? (complexitySum / levelCount) : 40;

        double finalScore = (accuracy * 0.4) + (complexityWeight * 0.4) + (volumeWeight * 0.2);
        return (int) Math.round(finalScore);
    }

    // Query: Get last 7 days of scores for a chart
    public Cursor getWeeklyProgress(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT " + COL_DATE + ", " + COL_AVG_SCORE + " FROM " + TABLE_DAILY_SCORES +
                " WHERE " + COLUMN_USER_ID + " = ? ORDER BY " + COL_DATE + " DESC LIMIT 7", new String[] { userId });
    }
}
