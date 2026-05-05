package com.example.registration_login_module;

import android.util.Log;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class FirebaseProgressHelper {
    
    private static final String TAG = "FirebaseProgressHelper";
    private static final String PROGRESS_COLLECTION = "user_progress";
    
    private FirebaseFirestore db;
    
    public FirebaseProgressHelper() {
        db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Initialize user progress in Firebase (call when user first registers)
     */
    public void initializeUserProgress(String userId, ProgressCallback callback) {
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("currentMilestone", 0);
        progressData.put("basicsCompleted", false);
        progressData.put("intermediateCompleted", false);
        progressData.put("advancedCompleted", false);
        progressData.put("expertCompleted", false);
        progressData.put("totalScore", 0);
        progressData.put("lastUpdated", com.google.firebase.Timestamp.now());
        
        db.collection(PROGRESS_COLLECTION).document(userId)
                .set(progressData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Progress initialized for user: " + userId);
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error initializing progress: " + e.getMessage());
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }
    
    /**
     * Get user progress from Firebase
     */
    public void getUserProgress(String userId, ProgressDataCallback callback) {
        db.collection(PROGRESS_COLLECTION).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserProgress progress = parseProgressData(documentSnapshot);
                        if (callback != null) callback.onSuccess(progress);
                    } else {
                        // User doesn't have progress record, initialize it
                        initializeUserProgress(userId, new ProgressCallback() {
                            @Override
                            public void onSuccess() {
                                UserProgress defaultProgress = new UserProgress();
                                if (callback != null) callback.onSuccess(defaultProgress);
                            }
                            
                            @Override
                            public void onFailure(String error) {
                                if (callback != null) callback.onFailure(error);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user progress: " + e.getMessage());
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }
    
    /**
     * Mark a specific milestone as completed
     */
    public void markMilestoneCompleted(String userId, int milestone, int score, ProgressCallback callback) {
        // First get current progress
        getUserProgress(userId, new ProgressDataCallback() {
            @Override
            public void onSuccess(UserProgress currentProgress) {
                Map<String, Object> updates = new HashMap<>();
                
                // Mark specific milestone as completed
                switch (milestone) {
                    case 0: // Basics
                        updates.put("basicsCompleted", true);
                        break;
                    case 1: // Intermediate
                        updates.put("intermediateCompleted", true);
                        break;
                    case 2: // Advanced
                        updates.put("advancedCompleted", true);
                        break;
                    case 3: // Expert
                        updates.put("expertCompleted", true);
                        break;
                }
                
                // Update current milestone to next one
                updates.put("currentMilestone", milestone + 1);
                
                // Add to total score
                updates.put("totalScore", currentProgress.getTotalScore() + score);
                updates.put("lastUpdated", com.google.firebase.Timestamp.now());
                
                // Update in Firebase
                db.collection(PROGRESS_COLLECTION).document(userId)
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Milestone " + milestone + " completed for user: " + userId);
                            if (callback != null) callback.onSuccess();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error updating milestone: " + e.getMessage());
                            if (callback != null) callback.onFailure(e.getMessage());
                        });
            }
            
            @Override
            public void onFailure(String error) {
                if (callback != null) callback.onFailure(error);
            }
        });
    }
    
    /**
     * Check if a specific milestone is completed
     */
    public void isMilestoneCompleted(String userId, int milestone, MilestoneCallback callback) {
        getUserProgress(userId, new ProgressDataCallback() {
            @Override
            public void onSuccess(UserProgress progress) {
                boolean isCompleted = false;
                switch (milestone) {
                    case 0: isCompleted = progress.isBasicsCompleted(); break;
                    case 1: isCompleted = progress.isIntermediateCompleted(); break;
                    case 2: isCompleted = progress.isAdvancedCompleted(); break;
                    case 3: isCompleted = progress.isExpertCompleted(); break;
                }
                if (callback != null) callback.onResult(isCompleted);
            }
            
            @Override
            public void onFailure(String error) {
                if (callback != null) callback.onResult(false);
            }
        });
    }
    
    /**
     * Parse progress data from Firebase document
     */
    private UserProgress parseProgressData(DocumentSnapshot document) {
        UserProgress progress = new UserProgress();
        progress.setCurrentMilestone(document.getLong("currentMilestone") != null ? 
                document.getLong("currentMilestone").intValue() : 0);
        progress.setBasicsCompleted(Boolean.TRUE.equals(document.getBoolean("basicsCompleted")));
        progress.setIntermediateCompleted(Boolean.TRUE.equals(document.getBoolean("intermediateCompleted")));
        progress.setAdvancedCompleted(Boolean.TRUE.equals(document.getBoolean("advancedCompleted")));
        progress.setExpertCompleted(Boolean.TRUE.equals(document.getBoolean("expertCompleted")));
        progress.setTotalScore(document.getLong("totalScore") != null ? 
                document.getLong("totalScore").intValue() : 0);
        return progress;
    }
    
    /**
     * Reset user progress to beginning
     */
    public void resetUserProgress(String userId, ProgressCallback callback) {
        initializeUserProgress(userId, callback);
    }
    
    // Callback interfaces
    public interface ProgressCallback {
        void onSuccess();
        void onFailure(String error);
    }
    
    public interface ProgressDataCallback {
        void onSuccess(UserProgress progress);
        void onFailure(String error);
    }
    
    public interface MilestoneCallback {
        void onResult(boolean isCompleted);
    }
    
    // UserProgress data class
    public static class UserProgress {
        private int currentMilestone = 0;
        private boolean basicsCompleted = false;
        private boolean intermediateCompleted = false;
        private boolean advancedCompleted = false;
        private boolean expertCompleted = false;
        private int totalScore = 0;
        
        // Getters and setters
        public int getCurrentMilestone() { return currentMilestone; }
        public void setCurrentMilestone(int currentMilestone) { this.currentMilestone = currentMilestone; }
        
        public boolean isBasicsCompleted() { return basicsCompleted; }
        public void setBasicsCompleted(boolean basicsCompleted) { this.basicsCompleted = basicsCompleted; }
        
        public boolean isIntermediateCompleted() { return intermediateCompleted; }
        public void setIntermediateCompleted(boolean intermediateCompleted) { this.intermediateCompleted = intermediateCompleted; }
        
        public boolean isAdvancedCompleted() { return advancedCompleted; }
        public void setAdvancedCompleted(boolean advancedCompleted) { this.advancedCompleted = advancedCompleted; }
        
        public boolean isExpertCompleted() { return expertCompleted; }
        public void setExpertCompleted(boolean expertCompleted) { this.expertCompleted = expertCompleted; }
        
        public int getTotalScore() { return totalScore; }
        public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
    }
}
