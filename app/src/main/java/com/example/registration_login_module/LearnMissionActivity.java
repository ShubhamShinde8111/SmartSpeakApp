package com.example.registration_login_module;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LearnMissionActivity extends AppCompatActivity {

    private String chapterId, missionId, chapterTitle;
    private int chapterNumber;
    private GrammarDataProvider.LearnContent content;
    private String userId;

    private TextView tvLevelTitle, tvMissionSubtitle, tvXpReward;
    private TextView tvCardTitle, tvCardDescription;
    private ImageView btnSpeaker, btnBack;
    private LinearLayout sectionsContainer;
    private Button btnContinue;

    private TextToSpeech tts;
    private boolean isSpeaking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.parseColor("#FF6A00")); // Orange header
        getWindow().setNavigationBarColor(Color.parseColor("#0D1B2A"));

        setContentView(R.layout.activity_learn_mission);

        chapterId = getIntent().getStringExtra("CHAPTER_ID");
        chapterNumber = getIntent().getIntExtra("CHAPTER_NUMBER", 1);
        chapterTitle = getIntent().getStringExtra("CHAPTER_TITLE");
        missionId = getIntent().getStringExtra("MISSION_ID");
        userId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("UID", "");

        // Load static hardcoded content (dynamic data model)
        content = GrammarDataProvider.getLearnContent(chapterId);

        initUI();
        initTTS();
        populateData();
    }

    private void initUI() {
        btnBack = findViewById(R.id.btnBack);
        tvLevelTitle = findViewById(R.id.tvLevelTitle);
        tvMissionSubtitle = findViewById(R.id.tvMissionSubtitle);
        tvXpReward = findViewById(R.id.tvXpReward);
        tvCardTitle = findViewById(R.id.tvCardTitle);
        tvCardDescription = findViewById(R.id.tvCardDescription);
        btnSpeaker = findViewById(R.id.btnSpeaker);
        sectionsContainer = findViewById(R.id.sectionsContainer);
        btnContinue = findViewById(R.id.btnContinue);

        btnBack.setOnClickListener(v -> finish());
        btnContinue.setOnClickListener(v -> completeMission());
        btnSpeaker.setOnClickListener(v -> toggleSpeech());
    }

    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("en", "IN"));
            }
        });
    }

    private void populateData() {
        // Static Title Section
        tvLevelTitle.setText(content.levelTitle);
        tvMissionSubtitle.setText("Mission " + content.missionNumber + ": " + content.missionType);
        tvXpReward.setText("+" + content.xpReward + " XP");

        // Card Header
        tvCardTitle.setText(content.levelTitle);
        tvCardDescription.setText(content.description);

        // Dynamically add sections with fade-in animation
        sectionsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        
        int delay = 0;
        for (GrammarDataProvider.LearnSection section : content.sections) {
            View sectionView = inflater.inflate(R.layout.item_learn_section, sectionsContainer, false);
            
            TextView tvEmoji = sectionView.findViewById(R.id.tvSectionEmoji);
            TextView tvTitle = sectionView.findViewById(R.id.tvSectionTitle);
            TextView tvContent = sectionView.findViewById(R.id.tvSectionContent);
            LinearLayout examplesContainer = sectionView.findViewById(R.id.examplesContainer);

            tvEmoji.setText(section.emoji);
            tvTitle.setText(section.title);
            
            // Handle markdown-like bolding simply by removing asterisks for now
            // or we could use Html.fromHtml if we format it. For this example, we leave as is.
            tvContent.setText(section.content.replace("**", ""));

            if (section.examples != null && !section.examples.isEmpty()) {
                examplesContainer.setVisibility(View.VISIBLE);
                for (String example : section.examples) {
                    TextView exView = new TextView(this);
                    exView.setText("• " + example.replace("**", ""));
                    exView.setTextColor(Color.WHITE);
                    exView.setTextSize(14);
                    exView.setPadding(0, dp(4), 0, dp(4));
                    examplesContainer.addView(exView);
                }
            }

            // Animation
            sectionView.setAlpha(0f);
            sectionsContainer.addView(sectionView);
            sectionView.animate().alpha(1f).setDuration(500).setStartDelay(delay).start();
            delay += 150; // Staggered fade in
        }
    }

    private void toggleSpeech() {
        if (tts == null) return;

        if (isSpeaking) {
            tts.stop();
            isSpeaking = false;
            btnSpeaker.setImageResource(R.drawable.ic_volume);
            btnSpeaker.setColorFilter(Color.parseColor("#64B5F6"));
        } else {
            StringBuilder fullText = new StringBuilder();
            fullText.append(content.levelTitle).append(". ");
            fullText.append(content.description).append(". ");
            for (GrammarDataProvider.LearnSection section : content.sections) {
                fullText.append(section.title).append(". ");
                fullText.append(section.content.replace("**", "")).append(". ");
                if (section.examples != null) {
                    for (String example : section.examples) {
                        fullText.append(example.replace("**", "")).append(". ");
                    }
                }
            }

            tts.speak(fullText.toString(), TextToSpeech.QUEUE_FLUSH, null, "LearnTTS");
            isSpeaking = true;
            btnSpeaker.setImageResource(R.drawable.ic_stop); // Make sure you have an ic_stop drawable, or just color it
            btnSpeaker.setColorFilter(Color.parseColor("#FF5252"));
        }
    }

    private void completeMission() {
        if (tts != null) tts.stop();
        if (userId.isEmpty()) { finish(); return; }

        // Save to Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> missionData = new HashMap<>();
        missionData.put("completedMissions", 1); // Learn is mission 0, so 1 done

        Map<String, Object> chapterData = new HashMap<>();
        chapterData.put(chapterId, missionData);

        db.collection("users").document(userId)
                .collection("gamification").document("chapter_progress")
                .set(chapterData, SetOptions.merge());

        // Award XP dynamically based on data model
        GamificationEngine engine = new GamificationEngine(this);
        engine.awardXP(userId, content.xpReward, 10, "learn_" + chapterId, new GamificationEngine.XPCallback() {
            @Override
            public void onXPAwarded(int xpGained, int coinsGained, int totalXP, int newLevel, boolean leveledUp) {
                runOnUiThread(() -> {
                    Toast.makeText(LearnMissionActivity.this,
                            "📘 Learn Complete! +" + xpGained + " XP", Toast.LENGTH_LONG).show();
                    if (leveledUp) LevelUpDialogFragment.show(LearnMissionActivity.this, newLevel);
                    finish();
                });
            }
            @Override
            public void onFailure(String error) { finish(); }
        });
    }

    private int dp(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
