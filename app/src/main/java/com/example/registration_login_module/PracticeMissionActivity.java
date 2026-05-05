package com.example.registration_login_module;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PracticeMissionActivity extends AppCompatActivity {

    private String chapterId, missionId, userId, chapterTitle;
    private int chapterNumber;
    private List<GrammarDataProvider.PracticeSentence> sentences;
    private int currentIndex = 0;
    private int correctCount = 0;

    private TextView tvProgressText, tvLevelTitle, tvMissionSubtitle, tvQuestionText;
    private ProgressBar progressBar;
    private ImageView btnBack, btnSpeakerQuestion, btnSpeakerFeedback;
    private LinearLayout answerContainer, wordBankContainer, feedbackCard;
    private TextView tvFeedbackTitle, tvFeedbackMessage, btnNext;

    private List<String> availableWords = new ArrayList<>();
    private List<String> selectedWords = new ArrayList<>();

    private TextToSpeech ttsEnglish;
    private TextToSpeech ttsHindi;
    
    private boolean isChecking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.parseColor("#FF6A00"));
        getWindow().setNavigationBarColor(Color.parseColor("#1B2A4A"));

        setContentView(R.layout.activity_practice_mission);

        chapterId = getIntent().getStringExtra("CHAPTER_ID");
        chapterNumber = getIntent().getIntExtra("CHAPTER_NUMBER", 1);
        chapterTitle = getIntent().getStringExtra("CHAPTER_TITLE");
        missionId = getIntent().getStringExtra("MISSION_ID");
        userId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("UID", "");

        sentences = GrammarDataProvider.getPracticeContent(chapterId);

        initUI();
        initTTS();
        showSentence(0);
    }

    private void initUI() {
        btnBack = findViewById(R.id.btnBack);
        tvProgressText = findViewById(R.id.tvProgressText);
        tvLevelTitle = findViewById(R.id.tvLevelTitle);
        tvMissionSubtitle = findViewById(R.id.tvMissionSubtitle);
        progressBar = findViewById(R.id.progressBar);
        btnSpeakerQuestion = findViewById(R.id.btnSpeakerQuestion);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        answerContainer = findViewById(R.id.answerContainer);
        wordBankContainer = findViewById(R.id.wordBankContainer);
        feedbackCard = findViewById(R.id.feedbackCard);
        tvFeedbackTitle = findViewById(R.id.tvFeedbackTitle);
        tvFeedbackMessage = findViewById(R.id.tvFeedbackMessage);
        btnSpeakerFeedback = findViewById(R.id.btnSpeakerFeedback);
        btnNext = findViewById(R.id.btnNext);

        btnBack.setOnClickListener(v -> finish());
        
        btnNext.setOnClickListener(v -> {
            if (!isChecking) {
                checkAnswer();
            } else {
                nextQuestion();
            }
        });

        btnSpeakerQuestion.setOnClickListener(v -> speakHindi(sentences.get(currentIndex).hindiSentence));
        btnSpeakerFeedback.setOnClickListener(v -> speakEnglish(sentences.get(currentIndex).correctEnglish));
    }

    private void initTTS() {
        ttsEnglish = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ttsEnglish.setLanguage(new Locale("en", "IN"));
            }
        });
        ttsHindi = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ttsHindi.setLanguage(new Locale("hi", "IN"));
            }
        });
    }

    private void showSentence(int index) {
        currentIndex = index;
        isChecking = false;
        selectedWords.clear();
        availableWords.clear();
        
        feedbackCard.setVisibility(View.GONE);
        btnNext.setText("Check Answer");
        btnNext.setBackgroundResource(R.drawable.btn_chapter_start);
        btnNext.setEnabled(false);
        btnNext.setAlpha(0.5f);

        GrammarDataProvider.PracticeSentence s = sentences.get(index);
        
        tvLevelTitle.setText(chapterTitle != null ? chapterTitle : "Practice");
        tvMissionSubtitle.setText("🧩 Mission 2: Practice");
        tvQuestionText.setText(s.hindiSentence);
        tvProgressText.setText((index + 1) + "/" + sentences.size());
        
        int progressPercent = (int) (((index) / (float) sentences.size()) * 100);
        ObjectAnimator.ofInt(progressBar, "progress", progressPercent).setDuration(500).start();

        availableWords.addAll(Arrays.asList(s.wordBank));
        Collections.shuffle(availableWords);

        renderAnswerArea();
        renderWordBankArea();
        speakHindi(s.hindiSentence);
    }

    private void renderAnswerArea() {
        answerContainer.removeAllViews();
        
        if (selectedWords.isEmpty()) {
            return;
        }

        LinearLayout currentRow = createWordRow();
        int rowWidth = 0;
        int maxWidth = getResources().getDisplayMetrics().widthPixels - dp(64);

        for (int i = 0; i < selectedWords.size(); i++) {
            String word = selectedWords.get(i);
            final int indexToRemove = i;
            TextView chip = createAnswerChip(word);
            
            chip.setOnClickListener(v -> {
                if(isChecking) return;
                availableWords.add(selectedWords.remove(indexToRemove));
                renderAnswerArea();
                renderWordBankArea();
                updateCheckButtonState();
            });

            int estimatedWidth = (int) (chip.getPaint().measureText(word)) + dp(32);
            if (rowWidth + estimatedWidth > maxWidth) {
                answerContainer.addView(currentRow);
                currentRow = createWordRow();
                rowWidth = 0;
            }

            currentRow.addView(chip);
            rowWidth += estimatedWidth + dp(8);
        }
        answerContainer.addView(currentRow);
    }

    private void renderWordBankArea() {
        wordBankContainer.removeAllViews();

        LinearLayout currentRow = createWordRow();
        int rowWidth = 0;
        int maxWidth = getResources().getDisplayMetrics().widthPixels - dp(40);

        for (int i = 0; i < availableWords.size(); i++) {
            String word = availableWords.get(i);
            final int indexToSelect = i;
            TextView chip = createWordChip(word);

            chip.setOnClickListener(v -> {
                if(isChecking) return;
                selectedWords.add(availableWords.remove(indexToSelect));
                renderAnswerArea();
                renderWordBankArea();
                updateCheckButtonState();
            });

            int estimatedWidth = (int) (chip.getPaint().measureText(word)) + dp(32);
            if (rowWidth + estimatedWidth > maxWidth) {
                wordBankContainer.addView(currentRow);
                currentRow = createWordRow();
                rowWidth = 0;
            }

            currentRow.addView(chip);
            rowWidth += estimatedWidth + dp(8);
        }
        wordBankContainer.addView(currentRow);
    }
    
    private void updateCheckButtonState() {
        if (selectedWords.isEmpty()) {
            btnNext.setEnabled(false);
            btnNext.setAlpha(0.5f);
        } else {
            btnNext.setEnabled(true);
            btnNext.setAlpha(1.0f);
        }
    }

    private LinearLayout createWordRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.bottomMargin = dp(8);
        row.setLayoutParams(rp);
        return row;
    }

    private TextView createWordChip(String word) {
        TextView chip = new TextView(this);
        chip.setText(word);
        chip.setTextColor(Color.WHITE);
        chip.setTextSize(16);
        chip.setPadding(dp(16), dp(10), dp(16), dp(10));
        chip.setBackgroundResource(R.drawable.chip_practice_default);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.setMarginEnd(dp(8));
        chip.setLayoutParams(cp);
        return chip;
    }
    
    private TextView createAnswerChip(String word) {
        TextView chip = createWordChip(word);
        return chip;
    }

    private void checkAnswer() {
        isChecking = true;
        GrammarDataProvider.PracticeSentence s = sentences.get(currentIndex);
        String built = String.join(" ", selectedWords).trim().toLowerCase();
        String correct = s.correctEnglish.trim().toLowerCase();
        // remove punctuation for flexible matching
        built = built.replaceAll("[^a-zA-Z0-9 ]", "");
        correct = correct.replaceAll("[^a-zA-Z0-9 ]", "");

        feedbackCard.setVisibility(View.VISIBLE);
        feedbackCard.setAlpha(0f);
        feedbackCard.setTranslationY(100f);
        feedbackCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        if (built.equals(correct)) {
            correctCount++;
            feedbackCard.setBackgroundResource(R.drawable.card_practice_correct);
            tvFeedbackTitle.setText("Correct!");
            tvFeedbackMessage.setText(s.correctEnglish);
            speakEnglish(s.correctEnglish);
        } else {
            feedbackCard.setBackgroundResource(R.drawable.card_practice_wrong);
            tvFeedbackTitle.setText("Try Again");
            tvFeedbackMessage.setText("Correct Answer: " + s.correctEnglish);
        }

        boolean isLast = currentIndex >= sentences.size() - 1;
        btnNext.setText(isLast ? "Complete" : "Next →");
        btnNext.setBackgroundColor(Color.parseColor("#10B981")); // Standard green action
        btnNext.setEnabled(true);
        btnNext.setAlpha(1.0f);
    }
    
    private void nextQuestion() {
        if (currentIndex < sentences.size() - 1) {
            showSentence(currentIndex + 1);
        } else {
            completeMission();
        }
    }

    private void completeMission() {
        if (userId.isEmpty()) { finish(); return; }

        int progressPercent = 100;
        ObjectAnimator.ofInt(progressBar, "progress", progressPercent).setDuration(500).start();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("completedMissions", 2);
        Map<String, Object> chapterData = new HashMap<>();
        chapterData.put(chapterId, data);
        db.collection("users").document(userId)
                .collection("gamification").document("chapter_progress")
                .set(chapterData, SetOptions.merge());

        GamificationEngine engine = new GamificationEngine(this);
        engine.awardXP(userId, 60, 15, "practice_" + chapterId, new GamificationEngine.XPCallback() {
            @Override
            public void onXPAwarded(int xpGained, int coinsGained, int totalXP, int newLevel, boolean leveledUp) {
                runOnUiThread(() -> {
                    Toast.makeText(PracticeMissionActivity.this,
                            "🧩 Practice Complete! +" + xpGained + " XP (" + correctCount + "/" + sentences.size() + " correct)",
                            Toast.LENGTH_LONG).show();
                    if (leveledUp) LevelUpDialogFragment.show(PracticeMissionActivity.this, newLevel);
                    finish();
                });
            }
            @Override
            public void onFailure(String error) { finish(); }
        });
    }
    
    private void speakHindi(String text) {
        if (ttsHindi != null && !text.isEmpty()) {
            ttsHindi.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hindi");
        }
    }

    private void speakEnglish(String text) {
        if (ttsEnglish != null && !text.isEmpty()) {
            ttsEnglish.speak(text, TextToSpeech.QUEUE_FLUSH, null, "english");
        }
    }

    private int dp(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onDestroy() {
        if (ttsEnglish != null) {
            ttsEnglish.stop();
            ttsEnglish.shutdown();
        }
        if (ttsHindi != null) {
            ttsHindi.stop();
            ttsHindi.shutdown();
        }
        super.onDestroy();
    }
}
