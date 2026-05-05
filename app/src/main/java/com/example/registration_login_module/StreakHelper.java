package com.example.registration_login_module;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StreakHelper {
    
    private static final String TAG = "StreakHelper";
    private static final String PREF_NAME = "StreakPrefs";
    private static final String KEY_LAST_ACTIVITY_DATE = "last_activity_date";
    private static final String KEY_CURRENT_STREAK = "current_streak";
    private static final String KEY_LONGEST_STREAK = "longest_streak";
    private static final String KEY_TOTAL_DAYS = "total_days";
    private static final String KEY_STREAK_START_DATE = "streak_start_date";
    
    private Context context;
    private SharedPreferences preferences;
    private FirebaseFirestore firestore;
    
    public interface StreakCallback {
        void onStreakUpdated(int currentStreak, int longestStreak, int totalDays);
        void onStreakMilestone(int streakCount, String milestone);
        void onError(String error);
    }
    
    public StreakHelper(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.firestore = FirebaseFirestore.getInstance();
    }
    
    /**
     * Record daily activity and update streak
     */
    public void recordDailyActivity(String userId, StreakCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            if (callback != null) {
                callback.onError("User ID is null or empty");
            }
            return;
        }
        
        try {
            String today = getCurrentDateString();
            String lastActivityDate = preferences.getString(KEY_LAST_ACTIVITY_DATE, "");
            int currentStreak = preferences.getInt(KEY_CURRENT_STREAK, 0);
            int longestStreak = preferences.getInt(KEY_LONGEST_STREAK, 0);
            int totalDays = preferences.getInt(KEY_TOTAL_DAYS, 0);
            
            // Check if this is a new day
            if (!today.equals(lastActivityDate)) {
                // Check if it's consecutive (yesterday)
                boolean isConsecutive = isConsecutiveDay(lastActivityDate, today);
                
                if (isConsecutive) {
                    // Continue streak
                    currentStreak++;
                } else if (isToday(lastActivityDate)) {
                    // Same day, don't update streak
                    return;
                } else {
                    // Break streak, start new one
                    currentStreak = 1;
                    preferences.edit().putString(KEY_STREAK_START_DATE, today).apply();
                }
                
                // Update longest streak
                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak;
                }
                
                // Update total days
                totalDays++;
                
                // Save to preferences
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(KEY_LAST_ACTIVITY_DATE, today);
                editor.putInt(KEY_CURRENT_STREAK, currentStreak);
                editor.putInt(KEY_LONGEST_STREAK, longestStreak);
                editor.putInt(KEY_TOTAL_DAYS, totalDays);
                editor.apply();
                
                // Save to Firebase
                saveStreakToFirebase(userId, currentStreak, longestStreak, totalDays, today);
                
                // Check for milestones
                String milestone = checkStreakMilestone(currentStreak);
                if (milestone != null && callback != null) {
                    callback.onStreakMilestone(currentStreak, milestone);
                }

                // ===== GAMIFICATION: Supernova Mode =====
                // Every 14 days of consecutive streak, activate Supernova Mode for 3 days
                if (currentStreak > 0 && currentStreak % 14 == 0) {
                    GamificationEngine engine = new GamificationEngine(context);
                    engine.activateSupernova(userId);
                    Log.d(TAG, "Supernova activated for 14-day streak!");
                }

                if (callback != null) {
                    callback.onStreakUpdated(currentStreak, longestStreak, totalDays);
                }
                
                Log.d(TAG, "Streak updated - Current: " + currentStreak + ", Longest: " + longestStreak + ", Total: " + totalDays);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error recording daily activity", e);
            if (callback != null) {
                callback.onError("Failed to update streak: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get current streak information
     */
    public StreakData getCurrentStreak() {
        int currentStreak = preferences.getInt(KEY_CURRENT_STREAK, 0);
        int longestStreak = preferences.getInt(KEY_LONGEST_STREAK, 0);
        int totalDays = preferences.getInt(KEY_TOTAL_DAYS, 0);
        String lastActivityDate = preferences.getString(KEY_LAST_ACTIVITY_DATE, "");
        String streakStartDate = preferences.getString(KEY_STREAK_START_DATE, "");
        
        return new StreakData(currentStreak, longestStreak, totalDays, lastActivityDate, streakStartDate);
    }
    
    /**
     * Load streak data from Firebase
     */
    public void loadStreakFromFirebase(String userId, StreakCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("User ID not available");
            return;
        }
        
        firestore.collection("users")
                .document(userId)
                .collection("streak")
                .document("data")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            int currentStreak = ((Long) data.getOrDefault("currentStreak", 0L)).intValue();
                            int longestStreak = ((Long) data.getOrDefault("longestStreak", 0L)).intValue();
                            int totalDays = ((Long) data.getOrDefault("totalDays", 0L)).intValue();
                            String lastActivityDate = (String) data.getOrDefault("lastActivityDate", "");
                            String streakStartDate = (String) data.getOrDefault("streakStartDate", "");
                            
                            // Update local preferences
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt(KEY_CURRENT_STREAK, currentStreak);
                            editor.putInt(KEY_LONGEST_STREAK, longestStreak);
                            editor.putInt(KEY_TOTAL_DAYS, totalDays);
                            editor.putString(KEY_LAST_ACTIVITY_DATE, lastActivityDate);
                            editor.putString(KEY_STREAK_START_DATE, streakStartDate);
                            editor.apply();
                            
                            if (callback != null) {
                                callback.onStreakUpdated(currentStreak, longestStreak, totalDays);
                            }
                        }
                    } else {
                        // No streak data found, initialize
                        if (callback != null) {
                            callback.onStreakUpdated(0, 0, 0);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading streak from Firebase", e);
                    if (callback != null) {
                        callback.onError("Failed to load streak data");
                    }
                });
    }
    
    /**
     * Save streak data to Firebase
     */
    private void saveStreakToFirebase(String userId, int currentStreak, int longestStreak, int totalDays, String lastActivityDate) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        
        Map<String, Object> streakData = new HashMap<>();
        streakData.put("currentStreak", currentStreak);
        streakData.put("longestStreak", longestStreak);
        streakData.put("totalDays", totalDays);
        streakData.put("lastActivityDate", lastActivityDate);
        streakData.put("streakStartDate", preferences.getString(KEY_STREAK_START_DATE, lastActivityDate));
        streakData.put("lastUpdated", System.currentTimeMillis());
        
        firestore.collection("users")
                .document(userId)
                .collection("streak")
                .document("data")
                .set(streakData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Streak saved to Firebase"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving streak to Firebase", e));
    }
    
    /**
     * Check if the streak should continue
     */
    private boolean isConsecutiveDay(String lastDate, String currentDate) {
        if (lastDate.isEmpty()) {
            return false;
        }
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date last = sdf.parse(lastDate);
            Date current = sdf.parse(currentDate);
            
            if (last != null && current != null) {
                long diffInMillies = current.getTime() - last.getTime();
                long diffInDays = diffInMillies / (1000 * 60 * 60 * 24);
                return diffInDays == 1; // Exactly 1 day difference
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing dates", e);
        }
        
        return false;
    }
    
    /**
     * Check if the last activity was today
     */
    private boolean isToday(String lastDate) {
        return lastDate.equals(getCurrentDateString());
    }
    
    /**
     * Get current date as string
     */
    private String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * Check for streak milestones
     */
    private String checkStreakMilestone(int streakCount) {
        if (streakCount == 1) {
            return "First day! Keep it up! 🔥";
        } else if (streakCount == 3) {
            return "3 days in a row! You're on fire! 🔥🔥🔥";
        } else if (streakCount == 7) {
            return "One week streak! Amazing dedication! 🏆";
        } else if (streakCount == 14) {
            return "Two weeks! You're unstoppable! 💪";
        } else if (streakCount == 30) {
            return "30 days! You're a streak master! 👑";
        } else if (streakCount == 50) {
            return "50 days! Incredible commitment! 🌟";
        } else if (streakCount == 100) {
            return "100 days! You're a legend! 🏅";
        } else if (streakCount % 10 == 0 && streakCount > 10) {
            return streakCount + " days streak! Outstanding! 🎉";
        }
        
        return null;
    }
    
    /**
     * Reset streak (for testing purposes)
     */
    public void resetStreak() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "Streak reset");
    }
    
    /**
     * Data class for streak information
     */
    public static class StreakData {
        public final int currentStreak;
        public final int longestStreak;
        public final int totalDays;
        public final String lastActivityDate;
        public final String streakStartDate;
        
        public StreakData(int currentStreak, int longestStreak, int totalDays, String lastActivityDate, String streakStartDate) {
            this.currentStreak = currentStreak;
            this.longestStreak = longestStreak;
            this.totalDays = totalDays;
            this.lastActivityDate = lastActivityDate;
            this.streakStartDate = streakStartDate;
        }
    }
}
