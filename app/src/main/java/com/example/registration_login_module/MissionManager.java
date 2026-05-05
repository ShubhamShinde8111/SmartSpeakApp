package com.example.registration_login_module;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
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
 * MissionManager — Automated day-wise learning path.
 *
 * Generates daily missions that guide users through structured learning:
 * Day 1: Vocabulary Basics
 * Day 2: Tense Mastery
 * Day 3: Preposition Challenge
 * Day 4: Article Precision
 * Day 5: Conversation Practice
 * Day 6: Speech Training
 * Day 7: Review & Perfect
 * Day 8+: Adaptive missions based on weak areas
 *
 * Each mission has objectives, rewards, and completion tracking.
 */
public class MissionManager {

    private static final String TAG = "MissionManager";
    private final FirebaseFirestore db;
    private final Context context;

    public MissionManager(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    // ===== MISSION MODEL =====

    public static class Mission {
        public String id;
        public String title;
        public String description;
        public String objective;
        public int xpReward;
        public int coinReward;
        public String badgeReward; // nullable
        public MissionType type;
        public String targetTestType; // "basic", "tense", "preposition", "article"
        public int requiredScore; // minimum correct answers to pass (out of 10)
        public boolean completed;
        public long completedAt;
        public int dayIndex;

        public Mission() {}

        public Mission(String id, String title, String description, String objective,
                       int xpReward, int coinReward, String badgeReward,
                       MissionType type, String targetTestType, int requiredScore, int dayIndex) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.objective = objective;
            this.xpReward = xpReward;
            this.coinReward = coinReward;
            this.badgeReward = badgeReward;
            this.type = type;
            this.targetTestType = targetTestType;
            this.requiredScore = requiredScore;
            this.dayIndex = dayIndex;
            this.completed = false;
            this.completedAt = 0;
        }
    }

    public enum MissionType {
        QUIZ,           // Complete a quiz test
        CONVERSATION,   // Have AI conversations
        PRACTICE,       // Speech practice rounds
        REVIEW,         // Perfect score on any quiz
        ADAPTIVE        // Repeat weak area
    }

    // ===== MISSION GENERATION =====

    /**
     * Get today's mission for the user based on their current day index.
     */
    public void getTodaysMission(String userId, MissionCallback callback) {
        // Get user's current day from gamification profile
        db.collection("users").document(userId)
                .collection("gamification").document("profile")
                .get()
                .addOnSuccessListener(doc -> {
                    int currentDay = 1;
                    if (doc.exists() && doc.getLong("currentDayIndex") != null) {
                        currentDay = doc.getLong("currentDayIndex").intValue();
                    }

                    // Check if today's mission is already completed
                    int finalDay = currentDay;
                    getMissionStatus(userId, currentDay, new MissionStatusCallback() {
                        @Override
                        public void onStatus(boolean isCompleted) {
                            Mission mission;
                            if (finalDay <= 7) {
                                mission = generateFixedMission(finalDay);
                            } else {
                                mission = generateAdaptiveMission(finalDay);
                            }
                            mission.completed = isCompleted;
                            callback.onMissionLoaded(mission);
                        }

                        @Override
                        public void onError(String error) {
                            Mission mission = generateFixedMission(finalDay);
                            callback.onMissionLoaded(mission);
                        }
                    });
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Generate a fixed mission for days 1-7 (the structured curriculum).
     */
    private Mission generateFixedMission(int dayIndex) {
        switch (dayIndex) {
            case 1:
                return new Mission(
                        "day1_vocab",
                        "🚀 Operation: Word Arsenal",
                        "Begin your journey by building a strong vocabulary foundation.",
                        "Complete the Vocabulary Quiz with 6+ correct answers",
                        GamificationEngine.XP_DAILY_MISSION,
                        GamificationEngine.COINS_MISSION,
                        "first_steps",
                        MissionType.QUIZ,
                        "basic",
                        6,
                        1
                );

            case 2:
                return new Mission(
                        "day2_tense",
                        "⏰ Mission: Time Traveler",
                        "Master the tenses — past, present, and future await.",
                        "Complete the Tense Quiz with 6+ correct answers",
                        GamificationEngine.XP_DAILY_MISSION,
                        GamificationEngine.COINS_MISSION,
                        null,
                        MissionType.QUIZ,
                        "tense",
                        6,
                        2
                );

            case 3:
                return new Mission(
                        "day3_preposition",
                        "🗺️ Mission: Navigator",
                        "Prepositions connect ideas — master their placement.",
                        "Complete the Preposition Quiz with 6+ correct answers",
                        GamificationEngine.XP_DAILY_MISSION,
                        GamificationEngine.COINS_MISSION,
                        null,
                        MissionType.QUIZ,
                        "preposition",
                        6,
                        3
                );

            case 4:
                return new Mission(
                        "day4_article",
                        "📰 Mission: Article Commander",
                        "A, An, The — small words with big impact.",
                        "Complete the Article Quiz with 6+ correct answers",
                        GamificationEngine.XP_DAILY_MISSION,
                        GamificationEngine.COINS_MISSION,
                        null,
                        MissionType.QUIZ,
                        "article",
                        6,
                        4
                );

            case 5:
                return new Mission(
                        "day5_conversation",
                        "🤖 Mission: First Contact",
                        "Put your skills to the test in a real AI conversation.",
                        "Complete 1 AI conversation session",
                        GamificationEngine.XP_DAILY_MISSION,
                        GamificationEngine.COINS_MISSION,
                        null,
                        MissionType.CONVERSATION,
                        null,
                        0,
                        5
                );

            case 6:
                return new Mission(
                        "day6_speech",
                        "🎤 Mission: Voice Calibration",
                        "Train your pronunciation and speaking clarity.",
                        "Complete 2 speech practice rounds",
                        GamificationEngine.XP_DAILY_MISSION,
                        GamificationEngine.COINS_MISSION,
                        null,
                        MissionType.PRACTICE,
                        null,
                        2,
                        6
                );

            case 7:
                return new Mission(
                        "day7_review",
                        "🏆 Mission: Mastery Review",
                        "Prove your skills — get a perfect score on any quiz.",
                        "Score 9+ correct answers on any quiz",
                        GamificationEngine.XP_DAILY_MISSION + 25,
                        GamificationEngine.COINS_MISSION + 10,
                        "champion",
                        MissionType.REVIEW,
                        null,
                        9,
                        7
                );

            default:
                return generateAdaptiveMission(dayIndex);
        }
    }

    /**
     * Generate an adaptive mission for day 8+ based on performance.
     * Cycles through categories with emphasis on weak areas.
     */
    private Mission generateAdaptiveMission(int dayIndex) {
        // Cycle through categories: vocab → tense → preposition → article → conversation → speech
        int cycleIndex = (dayIndex - 8) % 6;
        String[] categories = {"basic", "tense", "preposition", "article", "conversation", "speech"};
        String[] titles = {
                "🔄 Reinforce: Word Power",
                "🔄 Reinforce: Time Mastery",
                "🔄 Reinforce: Navigation",
                "🔄 Reinforce: Precision",
                "🤖 Advanced: Deep Conversation",
                "🎤 Advanced: Voice Training"
        };
        String[] descriptions = {
                "Strengthen your vocabulary with advanced challenges.",
                "Push your tense mastery to the next level.",
                "Navigate complex prepositional phrases.",
                "Perfect your article usage in tricky contexts.",
                "Engage in deeper, more complex conversations.",
                "Refine your pronunciation with harder sentences."
        };

        String category = categories[cycleIndex];
        MissionType type;
        if (category.equals("conversation")) {
            type = MissionType.CONVERSATION;
        } else if (category.equals("speech")) {
            type = MissionType.PRACTICE;
        } else {
            type = MissionType.QUIZ;
        }

        // Higher day = higher required score
        int requiredScore = Math.min(9, 6 + (dayIndex - 8) / 6);

        return new Mission(
                "day" + dayIndex + "_adaptive",
                titles[cycleIndex],
                descriptions[cycleIndex],
                type == MissionType.QUIZ ?
                        "Score " + requiredScore + "+ on the " + category + " quiz" :
                        "Complete 1 " + category + " session",
                GamificationEngine.XP_DAILY_MISSION,
                GamificationEngine.COINS_MISSION,
                null,
                type,
                type == MissionType.QUIZ ? category : null,
                requiredScore,
                dayIndex
        );
    }

    // ===== MISSION COMPLETION =====

    /**
     * Check if a quiz result completes the current mission.
     * Called from ResultActivity after a quiz.
     *
     * @param userId   Firebase UID
     * @param testType Quiz type ("basic", "tense", "preposition", "article")
     * @param correct  Number of correct answers
     */
    public void checkQuizMissionCompletion(String userId, String testType, int correct,
                                            GamificationEngine engine, MissionCompleteCallback callback) {
        getTodaysMission(userId, new MissionCallback() {
            @Override
            public void onMissionLoaded(Mission mission) {
                if (mission.completed) {
                    if (callback != null) callback.onResult(false, mission);
                    return;
                }

                boolean completed = false;

                if (mission.type == MissionType.QUIZ && testType.equals(mission.targetTestType)) {
                    completed = correct >= mission.requiredScore;
                } else if (mission.type == MissionType.REVIEW) {
                    // Review mission: perfect score on ANY quiz
                    completed = correct >= mission.requiredScore;
                }

                if (completed) {
                    completeMission(userId, mission, engine);
                }

                if (callback != null) callback.onResult(completed, mission);
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onResult(false, null);
            }
        });
    }

    /**
     * Check if a conversation session completes the current mission.
     */
    public void checkConversationMissionCompletion(String userId, GamificationEngine engine,
                                                    MissionCompleteCallback callback) {
        getTodaysMission(userId, new MissionCallback() {
            @Override
            public void onMissionLoaded(Mission mission) {
                if (mission.completed || mission.type != MissionType.CONVERSATION) {
                    if (callback != null) callback.onResult(false, mission);
                    return;
                }

                completeMission(userId, mission, engine);
                if (callback != null) callback.onResult(true, mission);
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onResult(false, null);
            }
        });
    }

    /**
     * Check if speech practice completes the current mission.
     */
    public void checkSpeechMissionCompletion(String userId, GamificationEngine engine,
                                              MissionCompleteCallback callback) {
        // Track speech rounds today in SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("MissionPrefs", Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastDate = prefs.getString("speech_mission_date", "");
        int rounds = prefs.getInt("speech_rounds_today", 0);

        if (!today.equals(lastDate)) {
            rounds = 0;
            prefs.edit().putString("speech_mission_date", today).apply();
        }
        rounds++;
        prefs.edit().putInt("speech_rounds_today", rounds).apply();

        int finalRounds = rounds;
        getTodaysMission(userId, new MissionCallback() {
            @Override
            public void onMissionLoaded(Mission mission) {
                if (mission.completed || mission.type != MissionType.PRACTICE) {
                    if (callback != null) callback.onResult(false, mission);
                    return;
                }

                if (finalRounds >= mission.requiredScore) {
                    completeMission(userId, mission, engine);
                    if (callback != null) callback.onResult(true, mission);
                } else {
                    if (callback != null) callback.onResult(false, mission);
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onResult(false, null);
            }
        });
    }

    /**
     * Mark mission as completed, award rewards, and advance to next day.
     */
    private void completeMission(String userId, Mission mission, GamificationEngine engine) {
        long now = System.currentTimeMillis();

        // Save completion to Firebase
        Map<String, Object> completionData = new HashMap<>();
        completionData.put("missionId", mission.id);
        completionData.put("dayIndex", mission.dayIndex);
        completionData.put("completedAt", now);
        completionData.put("title", mission.title);

        db.collection("users").document(userId)
                .collection("gamification").document("missions")
                .collection("completed").document(mission.id)
                .set(completionData)
                .addOnSuccessListener(v -> Log.d(TAG, "Mission completed: " + mission.id));

        // Award mission XP and coins
        engine.awardXPQuiet(userId, mission.xpReward, mission.coinReward, "mission_" + mission.id);

        // Award badge if applicable
        if (mission.badgeReward != null) {
            engine.awardBadge(userId, mission.badgeReward, null);
        }

        // Advance day index
        advanceDayIndex(userId, mission.dayIndex);
    }

    /**
     * Advance to the next day (called after mission completion).
     */
    private void advanceDayIndex(String userId, int currentDay) {
        Map<String, Object> update = new HashMap<>();
        update.put("currentDayIndex", currentDay + 1);

        db.collection("users").document(userId)
                .collection("gamification").document("profile")
                .set(update, SetOptions.merge())
                .addOnSuccessListener(v -> Log.d(TAG, "Advanced to day " + (currentDay + 1)));
    }

    // ===== MISSION HISTORY =====

    /**
     * Get all completed missions.
     */
    public void getCompletedMissions(String userId, MissionHistoryCallback callback) {
        db.collection("users").document(userId)
                .collection("gamification").document("missions")
                .collection("completed")
                .orderBy("dayIndex")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Map<String, Object>> history = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        history.add(doc.getData());
                    }
                    callback.onHistoryLoaded(history);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Check if a specific day's mission is completed.
     */
    private void getMissionStatus(String userId, int dayIndex, MissionStatusCallback callback) {
        String missionId = getMissionIdForDay(dayIndex);
        db.collection("users").document(userId)
                .collection("gamification").document("missions")
                .collection("completed").document(missionId)
                .get()
                .addOnSuccessListener(doc -> callback.onStatus(doc.exists()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private String getMissionIdForDay(int dayIndex) {
        switch (dayIndex) {
            case 1: return "day1_vocab";
            case 2: return "day2_tense";
            case 3: return "day3_preposition";
            case 4: return "day4_article";
            case 5: return "day5_conversation";
            case 6: return "day6_speech";
            case 7: return "day7_review";
            default: return "day" + dayIndex + "_adaptive";
        }
    }

    // ===== CALLBACKS =====

    public interface MissionCallback {
        void onMissionLoaded(Mission mission);
        void onError(String error);
    }

    public interface MissionCompleteCallback {
        void onResult(boolean completed, Mission mission);
    }

    public interface MissionStatusCallback {
        void onStatus(boolean isCompleted);
        void onError(String error);
    }

    public interface MissionHistoryCallback {
        void onHistoryLoaded(List<Map<String, Object>> history);
        void onError(String error);
    }
}
