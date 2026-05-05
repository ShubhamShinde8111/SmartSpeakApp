package com.example.registration_login_module;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ResultActivity extends AppCompatActivity {

    CardView home;
    TextView correctt, wrongt, resultinfo, resultscore;
    ImageView resultImage;
    private FirebaseProgressHelper firebaseProgressHelper;
    private String currentUserId = null;
    private String testType = "";

    // Gamification
    private GamificationEngine gamificationEngine;
    private MissionManager missionManager;
    private AdaptiveEngine adaptiveEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_result);

        // Initialize Firebase progress helper
        firebaseProgressHelper = new FirebaseProgressHelper();
        
        // Get current user ID
        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = userPrefs.getString("UID", null);
        
        // Get test type from intent
        testType = getIntent().getStringExtra("testType");
        if (testType == null) {
            testType = "basic"; // Default fallback
        }

        home = findViewById(R.id.returnHome);
        correctt = findViewById(R.id.correctScore);
        wrongt = findViewById(R.id.wrongScore);
        resultinfo = findViewById(R.id.resultInfo);
        resultscore = findViewById(R.id.resultScore);
        resultImage = findViewById(R.id.resultImage);

        int correct = getIntent().getIntExtra("correct", 0);
        int wrong = getIntent().getIntExtra("wrong", 0);
        int score = correct * 1;

        correctt.setText("" + correct);
        wrongt.setText("" + wrong);
        resultscore.setText("Your Score: " + score);

        if (correct >= 0 && correct <= 2) {
            resultinfo.setText("You have to take the test again. . ");
            resultImage.setImageResource(R.drawable.ic_sad);
        } else if (correct >= 3 && correct <= 5) {
            resultinfo.setText("You have to try a little more . .");
            resultImage.setImageResource(R.drawable.ic_neutral);
        } else if (correct >= 6 && correct <= 8) {
            resultinfo.setText("You are pretty good.");
            resultImage.setImageResource(R.drawable.ic_smile);
            // Save progress for good performance
            saveTestProgress(score);
        } else {
            resultinfo.setText("Congratulations! You are very good.");
            resultImage.setImageResource(R.drawable.ic_smile);
            // Save progress for excellent performance
            saveTestProgress(score);
        }

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ResultActivity.this, HomeActivity.class));
                finish();
            }
        });

        // ===== GAMIFICATION: Award XP & check mission =====
        awardGamificationRewards(correct);
    }

    /**
     * Award XP, coins, record adaptive score, and check mission completion.
     */
    private void awardGamificationRewards(int correct) {
        if (currentUserId == null) return;

        gamificationEngine = new GamificationEngine(this);
        missionManager = new MissionManager(this);
        adaptiveEngine = new AdaptiveEngine();

        int totalQuestions = correct + getIntent().getIntExtra("wrong", 0);
        boolean isPerfect = (correct == totalQuestions && totalQuestions > 0);

        // 1. Award base XP + bonus for perfect score
        int xpAmount = GamificationEngine.XP_QUIZ_COMPLETE;
        int coinAmount = GamificationEngine.COINS_QUIZ;
        if (isPerfect) {
            xpAmount += GamificationEngine.XP_QUIZ_PERFECT;
            coinAmount += GamificationEngine.COINS_QUIZ_PERFECT;
        }

        gamificationEngine.awardXP(currentUserId, xpAmount, coinAmount, "quiz_" + testType,
                new GamificationEngine.XPCallback() {
                    @Override
                    public void onXPAwarded(int xpGained, int coinsGained, int totalXP, int newLevel, boolean leveledUp) {
                        runOnUiThread(() -> {
                            Toast.makeText(ResultActivity.this,
                                    "+" + xpGained + " XP  |  +" + coinsGained + " 🪙",
                                    Toast.LENGTH_LONG).show();
                            if (leveledUp) {
                                LevelUpDialogFragment.show(ResultActivity.this, newLevel);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e("ResultActivity", "XP award failed: " + error);
                    }
                });

        // 2. Record adaptive score (converts correct/total to percentage)
        int percentage = AdaptiveEngine.toPercentage(correct, totalQuestions > 0 ? totalQuestions : 10);
        adaptiveEngine.recordScore(currentUserId, testType, percentage);

        // 3. Check if this quiz completes today's mission
        missionManager.checkQuizMissionCompletion(currentUserId, testType, correct,
                gamificationEngine, (completed, mission) -> {
                    if (completed && mission != null) {
                        runOnUiThread(() -> Toast.makeText(ResultActivity.this,
                                "🎯 Mission Complete: " + mission.title, Toast.LENGTH_LONG).show());
                    }
                });

        // 4. Award badge for perfect score
        if (isPerfect) {
            gamificationEngine.awardBadge(currentUserId, "champion", null);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(ResultActivity.this, HomeActivity.class));
        finish();
    }
    
    /**
     * Save test progress to Firebase
     */
    private void saveTestProgress(int score) {
        if (currentUserId == null) {
            Log.d("ResultActivity", "No user logged in, cannot save progress");
            return;
        }
        
        // Get milestone index for test type
        int milestoneIndex = getMilestoneForTestType(testType);
        
        if (milestoneIndex != -1) {
            // Mark milestone as completed in Firebase
            firebaseProgressHelper.markMilestoneCompleted(currentUserId, milestoneIndex, score, new FirebaseProgressHelper.ProgressCallback() {
                @Override
                public void onSuccess() {
                    Log.d("ResultActivity", "Progress saved successfully for test: " + testType);
                }
                
                @Override
                public void onFailure(String error) {
                    Log.e("ResultActivity", "Failed to save progress for test: " + testType + " - " + error);
                }
            });
        }
    }
    
    /**
     * Get milestone index for test type
     */
    private int getMilestoneForTestType(String testType) {
        switch (testType.toLowerCase()) {
            case "basic":
            case "basics":
                return 0;
            case "intermediate":
            case "tense":
                return 1;
            case "advanced":
            case "preposition":
                return 2;
            case "expert":
            case "article":
                return 3;
            default:
                return -1;
        }
    }
}