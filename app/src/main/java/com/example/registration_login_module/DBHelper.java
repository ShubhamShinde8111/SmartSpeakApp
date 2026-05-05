package com.example.registration_login_module;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

    // Database and Table information
    private static final String DATABASE_NAME = "userdatabase.db";
    private static final int DATABASE_VERSION = 3; // Incremented for speech analysis table
    private static final String TABLE_NAME = "users";
    private static final String PROGRESS_TABLE_NAME = "user_progress";
    private static final String ANALYSIS_TABLE_NAME = "speech_analysis";

    // User table column names
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PHONE = "phone";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PLACE = "place";
    public static final String COLUMN_PASSWORD = "password";
    
    // Progress table column names
    public static final String PROGRESS_COLUMN_ID = "id";
    public static final String PROGRESS_COLUMN_USER_ID = "user_id";
    public static final String PROGRESS_COLUMN_CURRENT_MILESTONE = "current_milestone";
    public static final String PROGRESS_COLUMN_BASICS_COMPLETED = "basics_completed";
    public static final String PROGRESS_COLUMN_INTERMEDIATE_COMPLETED = "intermediate_completed";
    public static final String PROGRESS_COLUMN_ADVANCED_COMPLETED = "advanced_completed";
    public static final String PROGRESS_COLUMN_EXPERT_COMPLETED = "expert_completed";
    public static final String PROGRESS_COLUMN_TOTAL_SCORE = "total_score";
    public static final String PROGRESS_COLUMN_LAST_UPDATED = "last_updated";

    // Speech analysis table column names
    public static final String ANALYSIS_COLUMN_ID = "id";
    public static final String ANALYSIS_COLUMN_USER_ID = "user_id";
    public static final String ANALYSIS_COLUMN_ORIGINAL_TEXT = "original_text";
    public static final String ANALYSIS_COLUMN_STRENGTHS = "strengths";
    public static final String ANALYSIS_COLUMN_WEAKNESSES = "weaknesses";
    public static final String ANALYSIS_COLUMN_LEVEL = "level";
    public static final String ANALYSIS_COLUMN_SCORE = "score";
    public static final String ANALYSIS_COLUMN_TIMESTAMP = "timestamp";

    // Constructor
    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Create table SQL query
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_PHONE + " TEXT, " +
                COLUMN_EMAIL + " TEXT UNIQUE, " +
                COLUMN_PLACE + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT)";
        db.execSQL(CREATE_USERS_TABLE);
        
        // Create user progress table
        String CREATE_PROGRESS_TABLE = "CREATE TABLE " + PROGRESS_TABLE_NAME + " (" +
                PROGRESS_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                PROGRESS_COLUMN_USER_ID + " INTEGER, " +
                PROGRESS_COLUMN_CURRENT_MILESTONE + " INTEGER DEFAULT 0, " +
                PROGRESS_COLUMN_BASICS_COMPLETED + " INTEGER DEFAULT 0, " +
                PROGRESS_COLUMN_INTERMEDIATE_COMPLETED + " INTEGER DEFAULT 0, " +
                PROGRESS_COLUMN_ADVANCED_COMPLETED + " INTEGER DEFAULT 0, " +
                PROGRESS_COLUMN_EXPERT_COMPLETED + " INTEGER DEFAULT 0, " +
                PROGRESS_COLUMN_TOTAL_SCORE + " INTEGER DEFAULT 0, " +
                PROGRESS_COLUMN_LAST_UPDATED + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(" + PROGRESS_COLUMN_USER_ID + ") REFERENCES " + TABLE_NAME + "(" + COLUMN_ID + "))";
        db.execSQL(CREATE_PROGRESS_TABLE);

        // Create speech analysis table
        String CREATE_ANALYSIS_TABLE = "CREATE TABLE " + ANALYSIS_TABLE_NAME + " (" +
                ANALYSIS_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ANALYSIS_COLUMN_USER_ID + " TEXT, " +
                ANALYSIS_COLUMN_ORIGINAL_TEXT + " TEXT, " +
                ANALYSIS_COLUMN_STRENGTHS + " TEXT, " +
                ANALYSIS_COLUMN_WEAKNESSES + " TEXT, " +
                ANALYSIS_COLUMN_LEVEL + " TEXT, " +
                ANALYSIS_COLUMN_SCORE + " INTEGER, " +
                ANALYSIS_COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
        db.execSQL(CREATE_ANALYSIS_TABLE);
    }

    // Upgrade database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Create user progress table for existing users
            String CREATE_PROGRESS_TABLE = "CREATE TABLE " + PROGRESS_TABLE_NAME + " (" +
                    PROGRESS_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    PROGRESS_COLUMN_USER_ID + " INTEGER, " +
                    PROGRESS_COLUMN_CURRENT_MILESTONE + " INTEGER DEFAULT 0, " +
                    PROGRESS_COLUMN_BASICS_COMPLETED + " INTEGER DEFAULT 0, " +
                    PROGRESS_COLUMN_INTERMEDIATE_COMPLETED + " INTEGER DEFAULT 0, " +
                    PROGRESS_COLUMN_ADVANCED_COMPLETED + " INTEGER DEFAULT 0, " +
                    PROGRESS_COLUMN_EXPERT_COMPLETED + " INTEGER DEFAULT 0, " +
                    PROGRESS_COLUMN_TOTAL_SCORE + " INTEGER DEFAULT 0, " +
                    PROGRESS_COLUMN_LAST_UPDATED + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(" + PROGRESS_COLUMN_USER_ID + ") REFERENCES " + TABLE_NAME + "(" + COLUMN_ID + "))";
            db.execSQL(CREATE_PROGRESS_TABLE);
        }
        if (oldVersion < 3) {
            String CREATE_ANALYSIS_TABLE = "CREATE TABLE " + ANALYSIS_TABLE_NAME + " (" +
                    ANALYSIS_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ANALYSIS_COLUMN_USER_ID + " TEXT, " +
                    ANALYSIS_COLUMN_ORIGINAL_TEXT + " TEXT, " +
                    ANALYSIS_COLUMN_STRENGTHS + " TEXT, " +
                    ANALYSIS_COLUMN_WEAKNESSES + " TEXT, " +
                    ANALYSIS_COLUMN_LEVEL + " TEXT, " +
                    ANALYSIS_COLUMN_SCORE + " INTEGER, " +
                    ANALYSIS_COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
            db.execSQL(CREATE_ANALYSIS_TABLE);
        }
    }

    // Method to add a new record
    public boolean addRecord(String name, String phone, String email, String place, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_PHONE, phone);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PLACE, place);
        values.put(COLUMN_PASSWORD, password);

        long result = -1;
        try {
            result = db.insert(TABLE_NAME, null, values);
            Log.d("DBHelper", "Inserted Record ID: " + result + ", Place: " + place);
        } catch (Exception e) {
            Log.e("DBHelper", "Error inserting data: " + e.getMessage());
        } finally {
            db.close(); // Ensure database is closed
        }
        return result != -1;
    }

    // Method to retrieve a user by email
    public Cursor getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_NAME, null, COLUMN_EMAIL + " = ?", new String[]{email}, null, null, null);
    }

    // Method to retrieve a user by ID
    public Cursor getUserById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_ID, COLUMN_NAME, COLUMN_EMAIL, COLUMN_PHONE, COLUMN_PLACE};
        return db.query(TABLE_NAME, columns, COLUMN_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
    }

    // Method to update a user record
    public int updateRecord(int id, String name, String phone, String email, String place, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_PHONE, phone);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PLACE, place);
        if (password != null) {
            values.put(COLUMN_PASSWORD, password);
        }

        int rowsUpdated = db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close(); // Ensure database is closed
        return rowsUpdated;
    }

    // Method to delete a user record
    public void deleteRecord(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close(); // Ensure database is closed
    }

    // Method to retrieve all records
    public Cursor getRecords() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }

    // Method to retrieve user by email and phone number
    public Cursor getUserByEmailAndPhone(String email, String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_NAME, COLUMN_PASSWORD}; // Select username and password
        String selection = COLUMN_EMAIL + " = ? AND " + COLUMN_PHONE + " = ?"; // WHERE clause
        String[] selectionArgs = {email, phone}; // Arguments for the WHERE clause

        // Execute the query and return the result
        return db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null);
    }

    // ========== PROGRESS TRACKING METHODS ==========

    /**
     * Initialize user progress (call when user first registers)
     */
    public boolean initializeUserProgress(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PROGRESS_COLUMN_USER_ID, userId);
        values.put(PROGRESS_COLUMN_CURRENT_MILESTONE, 0);
        values.put(PROGRESS_COLUMN_BASICS_COMPLETED, 0);
        values.put(PROGRESS_COLUMN_INTERMEDIATE_COMPLETED, 0);
        values.put(PROGRESS_COLUMN_ADVANCED_COMPLETED, 0);
        values.put(PROGRESS_COLUMN_EXPERT_COMPLETED, 0);
        values.put(PROGRESS_COLUMN_TOTAL_SCORE, 0);

        long result = -1;
        try {
            result = db.insert(PROGRESS_TABLE_NAME, null, values);
            Log.d("DBHelper", "Initialized progress for user ID: " + userId);
        } catch (Exception e) {
            Log.e("DBHelper", "Error initializing user progress: " + e.getMessage());
        } finally {
            db.close();
        }
        return result != -1;
    }

    /**
     * Get user progress by user ID
     */
    public Cursor getUserProgress(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(PROGRESS_TABLE_NAME, null, 
                PROGRESS_COLUMN_USER_ID + " = ?", 
                new String[]{String.valueOf(userId)}, 
                null, null, null);
    }

    /**
     * Update user's current milestone
     */
    public boolean updateCurrentMilestone(int userId, int milestone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PROGRESS_COLUMN_CURRENT_MILESTONE, milestone);
        values.put(PROGRESS_COLUMN_LAST_UPDATED, "CURRENT_TIMESTAMP");

        int rowsUpdated = db.update(PROGRESS_TABLE_NAME, values, 
                PROGRESS_COLUMN_USER_ID + " = ?", 
                new String[]{String.valueOf(userId)});
        db.close();
        return rowsUpdated > 0;
    }

    /**
     * Mark a specific milestone as completed
     */
    public boolean markMilestoneCompleted(int userId, int milestone, int score) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        // Mark specific milestone as completed
        switch (milestone) {
            case 0: // Basics
                values.put(PROGRESS_COLUMN_BASICS_COMPLETED, 1);
                break;
            case 1: // Intermediate
                values.put(PROGRESS_COLUMN_INTERMEDIATE_COMPLETED, 1);
                break;
            case 2: // Advanced
                values.put(PROGRESS_COLUMN_ADVANCED_COMPLETED, 1);
                break;
            case 3: // Expert
                values.put(PROGRESS_COLUMN_EXPERT_COMPLETED, 1);
                break;
        }
        
        // Update current milestone to next one
        values.put(PROGRESS_COLUMN_CURRENT_MILESTONE, milestone + 1);
        
        // Add to total score
        values.put(PROGRESS_COLUMN_TOTAL_SCORE, getTotalScore(userId) + score);
        values.put(PROGRESS_COLUMN_LAST_UPDATED, "CURRENT_TIMESTAMP");

        int rowsUpdated = db.update(PROGRESS_TABLE_NAME, values, 
                PROGRESS_COLUMN_USER_ID + " = ?", 
                new String[]{String.valueOf(userId)});
        db.close();
        return rowsUpdated > 0;
    }

    /**
     * Get total score for a user
     */
    public int getTotalScore(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(PROGRESS_TABLE_NAME, 
                new String[]{PROGRESS_COLUMN_TOTAL_SCORE}, 
                PROGRESS_COLUMN_USER_ID + " = ?", 
                new String[]{String.valueOf(userId)}, 
                null, null, null);
        
        int totalScore = 0;
        if (cursor.moveToFirst()) {
            totalScore = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return totalScore;
    }

    /**
     * Check if a specific milestone is completed
     */
    public boolean isMilestoneCompleted(int userId, int milestone) {
        SQLiteDatabase db = this.getReadableDatabase();
        String columnName;
        
        switch (milestone) {
            case 0: columnName = PROGRESS_COLUMN_BASICS_COMPLETED; break;
            case 1: columnName = PROGRESS_COLUMN_INTERMEDIATE_COMPLETED; break;
            case 2: columnName = PROGRESS_COLUMN_ADVANCED_COMPLETED; break;
            case 3: columnName = PROGRESS_COLUMN_EXPERT_COMPLETED; break;
            default: return false;
        }
        
        Cursor cursor = db.query(PROGRESS_TABLE_NAME, 
                new String[]{columnName}, 
                PROGRESS_COLUMN_USER_ID + " = ?", 
                new String[]{String.valueOf(userId)}, 
                null, null, null);
        
        boolean isCompleted = false;
        if (cursor.moveToFirst()) {
            isCompleted = cursor.getInt(0) == 1;
        }
        cursor.close();
        db.close();
        return isCompleted;
    }

    /**
     * Get user's current milestone
     */
    public int getCurrentMilestone(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(PROGRESS_TABLE_NAME, 
                new String[]{PROGRESS_COLUMN_CURRENT_MILESTONE}, 
                PROGRESS_COLUMN_USER_ID + " = ?", 
                new String[]{String.valueOf(userId)}, 
                null, null, null);
        
        int currentMilestone = 0;
        if (cursor.moveToFirst()) {
            currentMilestone = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return currentMilestone;
    }

    // ========== SPEECH ANALYSIS METHODS ==========

    public long saveSpeechAnalysis(String userId, String originalText, String strengths, String weaknesses, String level, int score) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ANALYSIS_COLUMN_USER_ID, userId);
        values.put(ANALYSIS_COLUMN_ORIGINAL_TEXT, originalText);
        values.put(ANALYSIS_COLUMN_STRENGTHS, strengths);
        values.put(ANALYSIS_COLUMN_WEAKNESSES, weaknesses);
        values.put(ANALYSIS_COLUMN_LEVEL, level);
        values.put(ANALYSIS_COLUMN_SCORE, score);
        
        long id = db.insert(ANALYSIS_TABLE_NAME, null, values);
        db.close();
        return id;
    }

    public Cursor getSpeechAnalysisForUser(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(ANALYSIS_TABLE_NAME, null, 
                ANALYSIS_COLUMN_USER_ID + " = ?", 
                new String[]{userId}, 
                null, null, ANALYSIS_COLUMN_TIMESTAMP + " DESC");
    }

}
