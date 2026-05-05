package com.example.registration_login_module;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * GamificationEngine — Central brain of the Supernova Gamification Layer.
 *
 * Responsibilities:
 * - Award XP from any activity (quiz, conversation, practice, login)
 * - Calculate user level from total XP (exponential curve)
 * - Detect level-ups and trigger celebrations
 * - Manage coin economy (earn + spend)
 * - Track XP multipliers (power-ups, Supernova mode)
 * - Persist all data to Firestore
 */
public class GamificationEngine {

    private static final String TAG = "GamificationEngine";
    private static final String PREF_NAME = "GamificationPrefs";
    private static final String KEY_LAST_LOGIN_DATE = "last_login_date";
    private static final String KEY_DICT_LOOKUPS_TODAY = "dict_lookups_today";
    private static final String KEY_DICT_LOOKUP_DATE = "dict_lookup_date";

    // XP award values for each activity type
    public static final int XP_QUIZ_COMPLETE = 50;
    public static final int XP_QUIZ_PERFECT = 100;  // bonus on top
    public static final int XP_CONVERSATION = 30;
    public static final int XP_SPEECH_PRACTICE = 25;
    public static final int XP_DAILY_MISSION = 75;
    public static final int XP_VOICE_CALL = 40;
    public static final int XP_DICTIONARY = 5;
    public static final int XP_DAILY_LOGIN = 15;

    // Coin award values
    public static final int COINS_QUIZ = 10;
    public static final int COINS_QUIZ_PERFECT = 25;
    public static final int COINS_CONVERSATION = 5;
    public static final int COINS_SPEECH = 5;
    public static final int COINS_MISSION = 20;
    public static final int COINS_VOICE_CALL = 10;
    public static final int COINS_DICTIONARY = 1;
    public static final int COINS_DAILY_LOGIN = 3;

    private final FirebaseFirestore db;
    private final SharedPreferences prefs;
    private final Context context;

    // Cached profile (loaded on first access)
    private GamificationProfile cachedProfile;

    public GamificationEngine(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ===== XP & LEVEL SYSTEM =====

    /**
     * Award XP and coins for an activity. Detects level-ups.
     *
     * @param userId   Firebase UID
     * @param xp       XP amount to award
     * @param coins    Coins to award
     * @param source   Activity source (for logging)
     * @param callback Result callback with level-up info
     */
    public void awardXP(String userId, int xp, int coins, String source, XPCallback callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) callback.onFailure("User ID is null");
            return;
        }

        getProfile(userId, new ProfileCallback() {
            @Override
            public void onLoaded(GamificationProfile profile) {
                int oldLevel = profile.level;
                int effectiveXP = (int) (xp * profile.xpMultiplier);

                profile.totalXP += effectiveXP;
                profile.coins += coins;
                profile.level = calculateLevel(profile.totalXP);

                boolean leveledUp = profile.level > oldLevel;

                // Save to Firebase
                saveProfile(userId, profile);

                // Log the XP event
                logXPEvent(userId, effectiveXP, coins, source);

                if (callback != null) {
                    callback.onXPAwarded(effectiveXP, coins, profile.totalXP, profile.level, leveledUp);
                }
            }

            @Override
            public void onFailure(String error) {
                if (callback != null) callback.onFailure(error);
            }
        });
    }

    /**
     * Quick XP award without callback (fire-and-forget).
     */
    public void awardXPQuiet(String userId, int xp, int coins, String source) {
        awardXP(userId, xp, coins, source, null);
    }

    /**
     * Calculate level from total XP.
     * Formula: Level = floor(sqrt(totalXP / 100)) + 1
     * This creates an exponential curve where higher levels need progressively more XP.
     *
     * Level 1:    0 XP
     * Level 2:  100 XP
     * Level 3:  400 XP
     * Level 4:  900 XP
     * Level 5: 1600 XP
     * Level 10: 8100 XP
     * Level 20: 36100 XP
     * Level 50: 240100 XP
     */
    public static int calculateLevel(int totalXP) {
        return (int) Math.floor(Math.sqrt(totalXP / 100.0)) + 1;
    }

    /**
     * Get XP required to reach the next level.
     */
    public static int getXPForLevel(int level) {
        return (level - 1) * (level - 1) * 100;
    }

    /**
     * Get XP progress within current level (0 to xpNeeded).
     */
    public static int[] getLevelProgress(int totalXP) {
        int level = calculateLevel(totalXP);
        int currentLevelXP = getXPForLevel(level);
        int nextLevelXP = getXPForLevel(level + 1);
        int progress = totalXP - currentLevelXP;
        int needed = nextLevelXP - currentLevelXP;
        return new int[]{progress, needed, level};
    }

    // ===== COIN SYSTEM =====

    /**
     * Spend coins (for shop purchases). Returns false if insufficient balance.
     */
    public void spendCoins(String userId, int amount, SpendCallback callback) {
        getProfile(userId, new ProfileCallback() {
            @Override
            public void onLoaded(GamificationProfile profile) {
                if (profile.coins >= amount) {
                    profile.coins -= amount;
                    saveProfile(userId, profile);
                    if (callback != null) callback.onSuccess(profile.coins);
                } else {
                    if (callback != null) callback.onInsufficientFunds(profile.coins, amount);
                }
            }

            @Override
            public void onFailure(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    // ===== DAILY LOGIN =====

    /**
     * Award daily login XP (only once per day).
     */
    public void awardDailyLogin(String userId, XPCallback callback) {
        String today = getCurrentDateString();
        String lastLogin = prefs.getString(KEY_LAST_LOGIN_DATE, "");

        if (!today.equals(lastLogin)) {
            prefs.edit().putString(KEY_LAST_LOGIN_DATE, today).apply();
            awardXP(userId, XP_DAILY_LOGIN, COINS_DAILY_LOGIN, "daily_login", callback);
        }
        // Already logged in today — no double award
    }

    /**
     * Award dictionary XP (max 10 per day).
     */
    public void awardDictionaryXP(String userId) {
        String today = getCurrentDateString();
        String lookupDate = prefs.getString(KEY_DICT_LOOKUP_DATE, "");
        int lookups = prefs.getInt(KEY_DICT_LOOKUPS_TODAY, 0);

        if (!today.equals(lookupDate)) {
            lookups = 0;
            prefs.edit().putString(KEY_DICT_LOOKUP_DATE, today).apply();
        }

        if (lookups < 10) {
            prefs.edit().putInt(KEY_DICT_LOOKUPS_TODAY, lookups + 1).apply();
            awardXPQuiet(userId, XP_DICTIONARY, COINS_DICTIONARY, "dictionary_lookup");
        }
    }

    // ===== XP MULTIPLIER =====

    /**
     * Set XP multiplier (for power-ups, Supernova mode).
     */
    public void setXPMultiplier(String userId, double multiplier) {
        getProfile(userId, new ProfileCallback() {
            @Override
            public void onLoaded(GamificationProfile profile) {
                profile.xpMultiplier = multiplier;
                saveProfile(userId, profile);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to set multiplier: " + error);
            }
        });
    }

    // ===== PROFILE MANAGEMENT =====

    /**
     * Get the user's gamification profile (with caching).
     */
    public void getProfile(String userId, ProfileCallback callback) {
        if (cachedProfile != null) {
            callback.onLoaded(cachedProfile);
            return;
        }

        DocumentReference docRef = db.collection("users").document(userId)
                .collection("gamification").document("profile");

        docRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                cachedProfile = parseProfile(doc);
                callback.onLoaded(cachedProfile);
            } else {
                // First time — initialize profile
                cachedProfile = new GamificationProfile();
                saveProfile(userId, cachedProfile);
                callback.onLoaded(cachedProfile);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading profile", e);
            callback.onFailure(e.getMessage());
        });
    }

    /**
     * Force refresh profile from Firebase (clear cache).
     */
    public void refreshProfile(String userId, ProfileCallback callback) {
        cachedProfile = null;
        getProfile(userId, callback);
    }

    /**
     * Initialize gamification profile for a new user.
     */
    public void initializeNewUser(String userId) {
        GamificationProfile profile = new GamificationProfile();
        profile.currentDayIndex = 1;
        saveProfile(userId, profile);
    }

    private void saveProfile(String userId, GamificationProfile profile) {
        cachedProfile = profile;

        Map<String, Object> data = new HashMap<>();
        data.put("totalXP", profile.totalXP);
        data.put("level", profile.level);
        data.put("coins", profile.coins);
        data.put("currentDayIndex", profile.currentDayIndex);
        data.put("xpMultiplier", profile.xpMultiplier);
        data.put("supernovaActive", profile.supernovaActive);
        data.put("unlockedBadges", profile.unlockedBadges);
        data.put("lastUpdated", FieldValue.serverTimestamp());

        db.collection("users").document(userId)
                .collection("gamification").document("profile")
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> Log.d(TAG, "Profile saved"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving profile", e));
    }

    private GamificationProfile parseProfile(DocumentSnapshot doc) {
        GamificationProfile p = new GamificationProfile();
        p.totalXP = doc.getLong("totalXP") != null ? doc.getLong("totalXP").intValue() : 0;
        p.level = doc.getLong("level") != null ? doc.getLong("level").intValue() : 1;
        p.coins = doc.getLong("coins") != null ? doc.getLong("coins").intValue() : 0;
        p.currentDayIndex = doc.getLong("currentDayIndex") != null ? doc.getLong("currentDayIndex").intValue() : 1;
        p.xpMultiplier = doc.getDouble("xpMultiplier") != null ? doc.getDouble("xpMultiplier") : 1.0;
        p.supernovaActive = Boolean.TRUE.equals(doc.getBoolean("supernovaActive"));

        List<String> badges = (List<String>) doc.get("unlockedBadges");
        if (badges != null) p.unlockedBadges = new ArrayList<>(badges);

        return p;
    }

    private void logXPEvent(String userId, int xp, int coins, String source) {
        Map<String, Object> event = new HashMap<>();
        event.put("xp", xp);
        event.put("coins", coins);
        event.put("source", source);
        event.put("timestamp", FieldValue.serverTimestamp());

        db.collection("users").document(userId)
                .collection("gamification").document("xp_log")
                .collection("events").add(event);
    }

    // ===== BADGE SYSTEM =====

    /**
     * Award a badge if not already earned.
     */
    public void awardBadge(String userId, String badgeId, BadgeCallback callback) {
        getProfile(userId, new ProfileCallback() {
            @Override
            public void onLoaded(GamificationProfile profile) {
                if (!profile.unlockedBadges.contains(badgeId)) {
                    profile.unlockedBadges.add(badgeId);
                    saveProfile(userId, profile);
                    if (callback != null) callback.onBadgeAwarded(badgeId);
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to award badge: " + error);
            }
        });
    }

    /**
     * Check and award automatic badges based on current state.
     */
    public void checkAutoBadges(String userId, int streakCount) {
        getProfile(userId, new ProfileCallback() {
            @Override
            public void onLoaded(GamificationProfile profile) {
                // First Steps — complete Day 1
                if (profile.currentDayIndex > 1) {
                    awardBadge(userId, "first_steps", null);
                }
                // On Fire — 3-day streak
                if (streakCount >= 3) {
                    awardBadge(userId, "on_fire", null);
                }
                // Rocket — Level 5
                if (profile.level >= 5) {
                    awardBadge(userId, "rocket", null);
                }
                // Diamond — Level 10
                if (profile.level >= 10) {
                    awardBadge(userId, "diamond", null);
                }
                // Supernova — 14-day streak
                if (streakCount >= 14) {
                    awardBadge(userId, "supernova", null);
                }
            }

            @Override
            public void onFailure(String error) {}
        });
    }

    // ===== SUPERNOVA MODE =====

    /**
     * Activate Supernova mode (2x XP, elite missions).
     */
    public void activateSupernova(String userId) {
        getProfile(userId, new ProfileCallback() {
            @Override
            public void onLoaded(GamificationProfile profile) {
                profile.supernovaActive = true;
                profile.xpMultiplier = 2.0;
                saveProfile(userId, profile);
                awardBadge(userId, "supernova", null);
                Log.d(TAG, "SUPERNOVA MODE ACTIVATED for " + userId);
            }

            @Override
            public void onFailure(String error) {}
        });
    }

    /**
     * Deactivate Supernova mode (streak broken).
     */
    public void deactivateSupernova(String userId) {
        getProfile(userId, new ProfileCallback() {
            @Override
            public void onLoaded(GamificationProfile profile) {
                profile.supernovaActive = false;
                profile.xpMultiplier = 1.0;
                saveProfile(userId, profile);
                Log.d(TAG, "Supernova mode deactivated for " + userId);
            }

            @Override
            public void onFailure(String error) {}
        });
    }

    // ===== HELPERS =====

    private String getCurrentDateString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    // ===== DATA CLASSES =====

    public static class GamificationProfile {
        public int totalXP = 0;
        public int level = 1;
        public int coins = 0;
        public int currentDayIndex = 1;
        public double xpMultiplier = 1.0;
        public boolean supernovaActive = false;
        public List<String> unlockedBadges = new ArrayList<>();
    }

    // ===== CALLBACKS =====

    public interface XPCallback {
        void onXPAwarded(int xpGained, int coinsGained, int totalXP, int newLevel, boolean leveledUp);
        void onFailure(String error);
    }

    public interface ProfileCallback {
        void onLoaded(GamificationProfile profile);
        void onFailure(String error);
    }

    public interface SpendCallback {
        void onSuccess(int remainingCoins);
        void onInsufficientFunds(int currentCoins, int cost);
        void onError(String error);
    }

    public interface BadgeCallback {
        void onBadgeAwarded(String badgeId);
    }
}
