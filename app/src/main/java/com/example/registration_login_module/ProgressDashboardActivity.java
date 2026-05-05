package com.example.registration_login_module;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProgressDashboardActivity extends AppCompatActivity {

    private FirebaseProgressRepository firebaseRepo;
    private LearningProgressDBHelper dbHelper;
    private CircularProgressIndicator fluencyProgress;
    private TextView tvFluencyScore, tvGrammarAccuracy, tvStrengthsList, tvMistakesList, tvGreeting, tvWeeklyInsight,
            tvAvgScore;
    private MaterialCardView mainStatsCard, chartCard, strengthCard, mistakeCard;
    private LinearLayout chartLayout;
    private LinearLayout dayLabelsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_dashboard);

        initializeViews();
        setupBottomNavigation();
        loadStatistics();
        applyEntranceAnimations();
    }

    private void initializeViews() {
        firebaseRepo = new FirebaseProgressRepository(this);
        dbHelper = new LearningProgressDBHelper(this);
        fluencyProgress = findViewById(R.id.fluencyProgress);
        tvFluencyScore = findViewById(R.id.tvFluencyScore);
        tvGrammarAccuracy = findViewById(R.id.tvGrammarAccuracy);
        tvStrengthsList = findViewById(R.id.tvStrengthsList);
        tvMistakesList = findViewById(R.id.tvMistakesList);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvWeeklyInsight = findViewById(R.id.tvWeeklyInsight);
        tvAvgScore = findViewById(R.id.tvAvgScore);

        mainStatsCard = findViewById(R.id.mainStatsCard);
        chartCard = findViewById(R.id.chartCard);
        strengthCard = findViewById(R.id.strengthCard);
        mistakeCard = findViewById(R.id.mistakeCard);
        chartLayout = findViewById(R.id.chartLayout);
        dayLabelsLayout = findViewById(R.id.dayLabelsLayout);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_progress);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_progress)
                    return true;

                Class<?> destination = null;
                if (itemId == R.id.nav_learn)
                    destination = HomeActivity.class;
                else if (itemId == R.id.nav_dictionary)
                    destination = DictionaryActivity.class;
                else if (itemId == R.id.nav_profile)
                    destination = ProfileActivity.class;

                if (destination != null) {
                    startActivity(new Intent(this, destination));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    private void applyEntranceAnimations() {
        if (mainStatsCard != null)
            mainStatsCard.setAlpha(0f);
        if (chartCard != null)
            chartCard.setAlpha(0f);
        if (strengthCard != null)
            strengthCard.setAlpha(0f);
        if (mistakeCard != null)
            mistakeCard.setAlpha(0f);

        new Handler().postDelayed(() -> {
            if (mainStatsCard != null)
                mainStatsCard.animate().alpha(1f).setDuration(500).start();
            if (chartCard != null)
                chartCard.animate().alpha(1f).setDuration(500).setStartDelay(100).start();
            if (strengthCard != null)
                strengthCard.animate().alpha(1f).setDuration(500).setStartDelay(200).start();
            if (mistakeCard != null)
                mistakeCard.animate().alpha(1f).setDuration(500).setStartDelay(300).start();
        }, 100);
    }

    private void loadStatistics() {
        String uid = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("UID", "");
        if (uid.isEmpty())
            return;

        String name = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("NAME", "Learner");
        if (tvGreeting != null)
            tvGreeting.setText("Welcome back, " + name + "!");

        // 1. Real-time Aggregates (Fluency, Sessions)
        firebaseRepo.attachStatsListener(uid, (avgScore, totalSessions, advCount) -> {
            runOnUiThread(() -> {
                // Determine Empty State
                if (totalSessions == 0) {
                    if (tvFluencyScore != null)
                        tvFluencyScore.setText("--");
                    if (fluencyProgress != null)
                        fluencyProgress.setProgress(0);
                    if (tvGrammarAccuracy != null)
                        tvGrammarAccuracy.setText("Grammar: --");
                    updateInsight(0, 0, 0); // show start msg
                } else {
                    animateProgressRing(fluencyProgress, (int) avgScore);
                    animateTextCount(tvFluencyScore, (int) avgScore);

                    if (tvGrammarAccuracy != null)
                        tvGrammarAccuracy.setText("Grammar: " + (int) avgScore + "%");

                    // Insight logic
                    double trend = (double) advCount / (double) totalSessions;
                    updateInsight(avgScore, avgScore, trend);
                }
            });
        });

        // 2. Weekly History (Graph)
        firebaseRepo.getWeeklyHistory(uid, new FirebaseProgressRepository.WeeklyHistoryCallback() {
            @Override
            public void onHistoryLoaded(Map<String, Float> weeklyData) {
                runOnUiThread(() -> renderChartFromMap(weeklyData));
            }

            @Override
            public void onError(Exception e) {
                Log.e("Dashboard", "Chart Error", e);
            }
        });

        // 3. Recent Feedback
        firebaseRepo.getRecentFeedback(uid, new FirebaseProgressRepository.FeedbackCallback() {
            @Override
            public void onFeedbackLoaded(List<String> strengths, List<String> weaknesses) {
                runOnUiThread(() -> {
                    if (tvStrengthsList != null) {
                        String sText = formatFeedbackList(strengths, "Keep practicing conversations!");
                        tvStrengthsList.setText(sText);
                    }
                    if (tvMistakesList != null) {
                        String mText = formatFeedbackList(weaknesses, "Great job! No recent mistakes.");
                        tvMistakesList.setText(mText);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e("Dashboard", "Feedback Error", e);
            }
        });

        // 4. Trigger Migration (Just in case)
        firebaseRepo.checkAndMigrate(uid, () -> {
            Log.d("Dashboard", "Migration check complete");
        });
    }

    private void updateInsight(double score, double grammar, double trend) {
        if (tvWeeklyInsight == null)
            return;
        if (score == 0) {
            tvWeeklyInsight.setText("Start your first conversation to see insights!");
        } else if (trend > 0.5) {
            tvWeeklyInsight.setText("You're using advanced vocabulary frequently. Impressive!");
        } else {
            tvWeeklyInsight.setText("Consistency is key. Keep practicing every day!");
        }
    }

    private void animateProgressRing(CircularProgressIndicator progress, int target) {
        if (progress == null)
            return;
        ObjectAnimator animator = ObjectAnimator.ofInt(progress, "progress", 0, target);
        animator.setDuration(1000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    private void animateTextCount(TextView view, int target) {
        if (view == null)
            return;
        ValueAnimator animator = ValueAnimator.ofInt(0, target);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> view.setText(animation.getAnimatedValue().toString()));
        animator.start();
    }

    private void renderChartFromMap(Map<String, Float> dateToScoreMap) {
        if (chartLayout == null)
            return;

        // Prepare 7-day data ending today
        float[] weeklyData = new float[7];
        String[] dayLabels = new String[7];

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayFormat = new SimpleDateFormat("EEE", Locale.getDefault());

        for (int i = 6; i >= 0; i--) {
            Date currentDate = cal.getTime();
            String dbDateStr = dbFormat.format(currentDate);
            dayLabels[i] = displayFormat.format(currentDate);

            if (dateToScoreMap.containsKey(dbDateStr)) {
                Float val = dateToScoreMap.get(dbDateStr);
                weeklyData[i] = val != null ? val : 0f;
            } else {
                weeklyData[i] = 0f;
            }
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        renderChart(weeklyData, dayLabels);
        updateWeeklyAverage(weeklyData);
    }

    private void renderChart(float[] weeklyData, String[] dayLabels) {
        if (chartLayout == null)
            return;
        chartLayout.removeAllViews();
        float max = 0;
        for (float v : weeklyData)
            if (v > max)
                max = v;
        if (max == 0)
            max = 100;

        for (int i = 0; i < weeklyData.length; i++) {
            addBarToChart(weeklyData[i], (int) max, i == 6, dayLabels[i], i);
        }

        // Dynamically populate day labels to match bars
        if (dayLabelsLayout != null) {
            dayLabelsLayout.removeAllViews();
            for (String label : dayLabels) {
                TextView tv = new TextView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                tv.setLayoutParams(lp);
                tv.setText(label.substring(0, 1)); // Single letter: M, T, W, etc.
                tv.setGravity(Gravity.CENTER);
                tv.setTextSize(10);
                tv.setTextColor(0xFFA0AEC0);
                dayLabelsLayout.addView(tv);
            }
        }
    }

    private void updateWeeklyAverage(float[] weeklyData) {
        if (tvAvgScore == null)
            return;
        float sum = 0;
        int count = 0;
        for (float v : weeklyData) {
            if (v > 0) {
                sum += v;
                count++;
            }
        }
        int avg = count > 0 ? (int) (sum / count) : 0;
        tvAvgScore.setText("Avg: " + avg + "%");
    }

    private String formatFeedbackList(List<String> items, String fallback) {
        if (items == null || items.isEmpty())
            return fallback;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(3, items.size()); i++) {
            sb.append("• ").append(items.get(i)).append("\n");
        }
        return sb.toString().trim();
    }

    private void addBarToChart(float score, int max, boolean isToday, String dayName, int index) {
        LinearLayout container = new LinearLayout(this);
        container.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        container.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        container.setPadding(4, 20, 4, 0);

        container.setOnClickListener(v -> {
            View barView = container.getChildAt(0);
            barView.animate().scaleX(1.2f).scaleY(1.1f).setDuration(100)
                    .withEndAction(() -> barView.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();

            String scoreText = (score > 0) ? (int) score + "%" : "No activity";
            Toast.makeText(this, dayName + ": " + scoreText, Toast.LENGTH_SHORT).show();
        });

        View bar = new View(this);
        float density = getResources().getDisplayMetrics().density;
        int emptyHeight = (int) (4 * density);
        int maxBarHeight = (int) (150 * density);

        int barHeightPx = (score == 0) ? emptyHeight : Math.max(12, (int) ((score / max) * maxBarHeight));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) (14 * density), barHeightPx);

        bar.setLayoutParams(params);

        int startColor, endColor;
        if (score == 0) {
            startColor = 0xFFE0E0E0;
            endColor = 0xFFF5F5F5;
        } else if (score >= 80) {
            startColor = 0xFF4CAF50;
            endColor = 0xFF81C784;
        } else if (score >= 50) {
            startColor = 0xFFFF9800;
            endColor = 0xFFFFB74D;
        } else {
            startColor = 0xFFFF5252;
            endColor = 0xFFFF8A80;
        }

        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[] { startColor, endColor });
        gd.setCornerRadius(20f);

        if (isToday) {
            gd.setStroke((int) (2 * density), 0xFF1A237E);
        }
        bar.setBackground(gd);

        bar.setScaleY(0f);
        bar.setPivotY(barHeightPx);
        container.addView(bar);

        chartLayout.addView(container);

        bar.animate()
                .scaleY(1f)
                .setDuration(600)
                .setStartDelay(50 * index)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private String getRecentFeedback(String userId, String table) {
        StringBuilder sb = new StringBuilder();
        try {
            android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();

            String query = "SELECT m." + LearningProgressDBHelper.COL_DESCRIPTION +
                    " FROM " + table + " m " +
                    " JOIN " + LearningProgressDBHelper.TABLE_CONVERSATIONS + " c " +
                    " ON m." + LearningProgressDBHelper.COL_CONV_ID + " = c." + LearningProgressDBHelper.COLUMN_ID +
                    " WHERE c." + LearningProgressDBHelper.COLUMN_USER_ID + " = ? " +
                    " ORDER BY c." + LearningProgressDBHelper.COLUMN_TIMESTAMP + " DESC LIMIT 5";

            Cursor cursor = db.rawQuery(query, new String[] { userId });
            while (cursor.moveToNext()) {
                sb.append("• ").append(cursor.getString(0)).append("\n");
            }
            cursor.close();
        } catch (Exception e) {
            Log.e("DB", "Error fetching feedback", e);
        }
        return sb.toString().trim();
    }
}
