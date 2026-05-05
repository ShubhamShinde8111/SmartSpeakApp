package com.example.registration_login_module;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class MissionHubActivity extends AppCompatActivity {

    private LinearLayout chapterListContainer;
    private TextView tvRankName, tvTotalXP, tvChaptersCount, tvMissionsCount, tvBadgesCount;
    private ProgressBar pbRankXP;
    private String userId;
    private List<GrammarChapter> chapters;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.parseColor("#0D1B2A"));
        setContentView(R.layout.activity_mission_hub);

        userId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("UID", "");
        db = FirebaseFirestore.getInstance();

        initViews();
        loadChapters();
    }

    private void initViews() {
        chapterListContainer = findViewById(R.id.chapterListContainer);
        tvRankName = findViewById(R.id.tvRankName);
        tvTotalXP = findViewById(R.id.tvTotalXP);
        tvChaptersCount = findViewById(R.id.tvChaptersCount);
        tvMissionsCount = findViewById(R.id.tvMissionsCount);
        tvBadgesCount = findViewById(R.id.tvBadgesCount);
        pbRankXP = findViewById(R.id.pbRankXP);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null)
            btnBack.setOnClickListener(v -> finish());
    }

    private void loadChapters() {
        chapters = GrammarDataProvider.getAllChapters();

        if (userId.isEmpty()) {
            setChapterStatuses(1, 0);
            return;
        }

        // Load progress from Firestore
        db.collection("users").document(userId)
                .collection("gamification").document("chapter_progress")
                .get()
                .addOnSuccessListener(doc -> {
                    int completedChapters = 0;
                    int totalMissions = 0;

                    if (doc.exists()) {
                        for (GrammarChapter ch : chapters) {
                            Map<String, Object> chData = (Map<String, Object>) doc.get(ch.chapterId);
                            if (chData != null) {
                                Long count = (Long) chData.get("completedMissions");
                                ch.completedCount = count != null ? count.intValue() : 0;
                                totalMissions += ch.completedCount;
                                if (ch.completedCount >= 4)
                                    completedChapters++;
                            }
                        }
                    }

                    setChapterStatuses(completedChapters + 1, totalMissions);
                    loadProfileStats();
                })
                .addOnFailureListener(e -> {
                    setChapterStatuses(1, 0);
                    loadProfileStats();
                });
    }

    private void setChapterStatuses(int activeChapterNumber, int totalMissions) {
        for (GrammarChapter ch : chapters) {
            if (ch.chapterNumber < activeChapterNumber) {
                ch.status = GrammarChapter.ChapterStatus.COMPLETED;
                ch.completedCount = 4;
            } else if (ch.chapterNumber == activeChapterNumber) {
                ch.status = GrammarChapter.ChapterStatus.ACTIVE;
                // Set mission statuses within active chapter
                for (int i = 0; i < ch.missions.size(); i++) {
                    if (i < ch.completedCount) {
                        ch.missions.get(i).status = GrammarMission.MissionStatus.COMPLETED;
                    } else if (i == ch.completedCount) {
                        ch.missions.get(i).status = GrammarMission.MissionStatus.ACTIVE;
                    } else {
                        ch.missions.get(i).status = GrammarMission.MissionStatus.LOCKED;
                    }
                }
            } else {
                ch.status = GrammarChapter.ChapterStatus.LOCKED;
            }
        }

        // Update stats
        int completedChapters = activeChapterNumber - 1;
        tvChaptersCount.setText(String.valueOf(completedChapters));
        tvMissionsCount.setText(String.valueOf(totalMissions));

        renderChapterCards();
    }

    private void loadProfileStats() {
        if (userId.isEmpty())
            return;

        GamificationEngine engine = new GamificationEngine(this);
        engine.getProfile(userId, new GamificationEngine.ProfileCallback() {
            @Override
            public void onLoaded(GamificationEngine.GamificationProfile profile) {
                runOnUiThread(() -> {
                    tvTotalXP.setText(profile.totalXP + " XP");
                    tvBadgesCount.setText(String.valueOf(profile.unlockedBadges.size()));

                    // Set rank
                    String[] ranks = { "Meteor", "Comet", "Star", "Supernova" };
                    String[] rankEmojis = { "☄️", "💫", "⭐", "🌟" };
                    int rankIndex = Math.min(profile.level / 5, ranks.length - 1);
                    tvRankName.setText(ranks[rankIndex]);
                    TextView tvEmoji = findViewById(R.id.tvRankEmoji);
                    if (tvEmoji != null)
                        tvEmoji.setText(rankEmojis[rankIndex]);

                    int[] progress = GamificationEngine.getLevelProgress(profile.totalXP);
                    pbRankXP.setMax(progress[1]);
                    pbRankXP.setProgress(progress[0]);
                });
            }

            @Override
            public void onFailure(String error) {
            }
        });
    }

    static class SectionGroup {
        String title;
        int startChapter;
        int endChapter;
        String emoji;
        int bgDrawableResId;
        
        SectionGroup(String title, int start, int end, String emoji, int bgDrawableResId) {
            this.title = title;
            this.startChapter = start;
            this.endChapter = end;
            this.emoji = emoji;
            this.bgDrawableResId = bgDrawableResId;
        }
    }

    private void renderChapterCards() {
        chapterListContainer.removeAllViews();

        java.util.List<SectionGroup> sectionGroups = java.util.Arrays.asList(
            new SectionGroup("Section 1: Foundations", 1, 3, "🌱", R.drawable.bg_section_header_purple),
            new SectionGroup("Section 2: Progressing", 4, 6, "🚀", R.drawable.bg_section_header_blue),
            new SectionGroup("Section 3: Advanced", 7, 10, "👑", R.drawable.bg_section_header_pink)
        );

        int globalDelayIndex = 0;

        for (int sectionIdx = 0; sectionIdx < sectionGroups.size(); sectionIdx++) {
            SectionGroup group = sectionGroups.get(sectionIdx);
            
            // Collect chapters for this section
            java.util.List<GrammarChapter> sectionChapters = new java.util.ArrayList<>();
            int totalMissionsInSection = 0;
            int completedMissionsInSection = 0;
            boolean sectionIsLocked = true;
            
            for (GrammarChapter ch : chapters) {
                if (ch.chapterNumber >= group.startChapter && ch.chapterNumber <= group.endChapter) {
                    sectionChapters.add(ch);
                    totalMissionsInSection += ch.getTotalMissions();
                    completedMissionsInSection += ch.completedCount;
                    if (!ch.isLocked()) {
                        sectionIsLocked = false;
                    }
                }
            }

            if (sectionChapters.isEmpty()) continue;

            // --- SECTION HEADER ---
            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setBackgroundResource(group.bgDrawableResId);
            header.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            headerParams.bottomMargin = dpToPx(16);
            headerParams.topMargin = sectionIdx > 0 ? dpToPx(16) : 0;
            header.setLayoutParams(headerParams);

            // Header Emoji
            TextView emoji = new TextView(this);
            emoji.setText(group.emoji);
            emoji.setTextSize(32);
            emoji.setPadding(0, 0, dpToPx(12), 0);
            if (sectionIsLocked) emoji.setAlpha(0.4f);
            header.addView(emoji);

            // Header Title Block
            LinearLayout titleBlock = new LinearLayout(this);
            titleBlock.setOrientation(LinearLayout.VERTICAL);
            titleBlock.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView sectionLabel = new TextView(this);
            sectionLabel.setText(completedMissionsInSection + " / " + totalMissionsInSection + " Missions");
            sectionLabel.setTextColor(Color.parseColor("#8899AA"));
            sectionLabel.setTextSize(12);
            sectionLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            titleBlock.addView(sectionLabel);

            TextView title = new TextView(this);
            title.setText(group.title);
            title.setTextColor(sectionIsLocked ? Color.parseColor("#667788") : Color.WHITE);
            title.setTextSize(20);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            titleBlock.addView(title);
            header.addView(titleBlock);

            // Status Badge
            TextView statusIcon = new TextView(this);
            statusIcon.setTextSize(28);
            if (completedMissionsInSection == totalMissionsInSection) {
                statusIcon.setText("👑");
            } else if (sectionIsLocked) {
                statusIcon.setText("🔒");
            }
            header.addView(statusIcon);
            chapterListContainer.addView(header);

            // Add chapters for this section
            for (GrammarChapter ch : sectionChapters) {
                View card = buildChapterCard(ch);
                
                // Entrance animation
                card.setAlpha(0f);
                card.setTranslationY(50f);
                card.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(500)
                        .setStartDelay(100 * globalDelayIndex++)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();

                chapterListContainer.addView(card);
            }
        }
    }

    private View buildChapterCard(GrammarChapter ch) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dpToPx(16);
        card.setLayoutParams(cardParams);
        card.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

        // Set modern premium background
        switch (ch.status) {
            case COMPLETED:
                card.setBackgroundResource(R.drawable.bg_card_completed);
                break;
            case ACTIVE:
                card.setBackgroundResource(R.drawable.bg_card_active);
                break;
            default:
                card.setBackgroundResource(R.drawable.bg_card_locked);
                break;
        }

        // === Top Row: Emoji + Title + Status Icon ===
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        // Emoji
        TextView emoji = new TextView(this);
        emoji.setText(ch.emoji);
        emoji.setTextSize(36);
        emoji.setPadding(0, 0, dpToPx(16), 0);
        if (ch.isLocked()) emoji.setAlpha(0.4f);
        topRow.addView(emoji);

        // Title block
        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView chapterLabel = new TextView(this);
        chapterLabel.setText("Week " + ch.chapterNumber);
        chapterLabel.setTextColor(Color.parseColor(ch.isActive() ? "#FFB300" : "#8899AA"));
        chapterLabel.setTextSize(13);
        chapterLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        titleBlock.addView(chapterLabel);

        TextView title = new TextView(this);
        title.setText(ch.title);
        title.setTextColor(ch.isLocked() ? Color.parseColor("#667788") : Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titleBlock.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(ch.subtitle);
        subtitle.setTextColor(ch.isLocked() ? Color.parseColor("#556677") : Color.parseColor("#AABBCC"));
        subtitle.setTextSize(14);
        titleBlock.addView(subtitle);

        topRow.addView(titleBlock);

        // Status icon
        TextView statusIcon = new TextView(this);
        statusIcon.setTextSize(28);
        if (ch.isCompleted()) {
            statusIcon.setText("✅");
        } else if (ch.isLocked()) {
            statusIcon.setText("🔒");
        } else {
            statusIcon.setText("");
        }
        topRow.addView(statusIcon);

        card.addView(topRow);

        // === Progress Bar (Animated) ===
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(8));
        pbParams.topMargin = dpToPx(16);
        progressBar.setLayoutParams(pbParams);
        progressBar.setMax(400); // Higher max for smooth animation
        progressBar.setProgressDrawable(getDrawable(R.drawable.chapter_progress_bar));
        card.addView(progressBar);

        // Animate Progress Bar
        int targetProgress = ch.completedCount * 100;
        android.animation.ObjectAnimator progressAnimator = android.animation.ObjectAnimator.ofInt(progressBar, "progress", 0, targetProgress);
        progressAnimator.setDuration(1000);
        progressAnimator.setInterpolator(new DecelerateInterpolator());
        progressAnimator.start();

        // Progress text
        TextView progressText = new TextView(this);
        progressText.setText(ch.getProgressText());
        progressText.setTextColor(Color.parseColor("#8899AA"));
        progressText.setTextSize(13);
        progressText.setGravity(Gravity.END);
        LinearLayout.LayoutParams ptParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ptParams.topMargin = dpToPx(6);
        progressText.setLayoutParams(ptParams);
        card.addView(progressText);

        // === Mission Pills Row ===
        LinearLayout pillsRow = new LinearLayout(this);
        pillsRow.setOrientation(LinearLayout.HORIZONTAL);
        pillsRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams pillsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pillsParams.topMargin = dpToPx(12);
        pillsRow.setLayoutParams(pillsParams);

        String[] labels = { "Learn", "Practice", "Translate", "Speak" };
        String[] emojis = { "📘", "🧩", "🎤", "💬" };

        for (int i = 0; i < 4; i++) {
            LinearLayout pillItem = new LinearLayout(this);
            pillItem.setOrientation(LinearLayout.HORIZONTAL);
            pillItem.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams pillItemParams = new LinearLayout.LayoutParams(0, dpToPx(32), 1f);
            if (i < 3) pillItemParams.setMarginEnd(dpToPx(6));
            pillItem.setLayoutParams(pillItemParams);

            boolean missionDone = i < ch.completedCount;
            boolean missionActive = ch.isActive() && i == ch.completedCount;

            if (missionDone) {
                pillItem.setBackgroundResource(R.drawable.bg_pill_completed);
            } else if (missionActive) {
                pillItem.setBackgroundResource(R.drawable.bg_pill_active);
            } else {
                pillItem.setBackgroundResource(R.drawable.bg_pill_locked);
            }

            // Pill Icon
            TextView pillIcon = new TextView(this);
            pillIcon.setText(emojis[i]);
            pillIcon.setTextSize(12);
            pillIcon.setPadding(0, 0, dpToPx(4), 0);
            if (!missionDone && !missionActive) pillIcon.setAlpha(0.3f);
            pillItem.addView(pillIcon);

            // Pill Label
            TextView label = new TextView(this);
            label.setText(labels[i]);
            label.setTextSize(11);
            label.setTypeface(null, android.graphics.Typeface.BOLD);
            label.setTextColor(missionDone ? Color.parseColor("#4CAF50")
                    : missionActive ? Color.parseColor("#FFB300") : Color.parseColor("#556677"));
            pillItem.addView(label);

            if (missionActive) {
                // Subtle pulse for active pill
                android.animation.ObjectAnimator pillAnim = android.animation.ObjectAnimator.ofFloat(pillItem, "alpha", 0.6f, 1f);
                pillAnim.setDuration(800);
                pillAnim.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
                pillAnim.setRepeatMode(android.animation.ObjectAnimator.REVERSE);
                pillAnim.start();
            }

            pillsRow.addView(pillItem);
        }
        card.addView(pillsRow);

        // === Start Button (only for active chapter) ===
        if (ch.isActive()) {
            TextView startBtn = new TextView(this);
            GrammarMission nextMission = ch.getNextMission();
            String btnText = "📖 Start: " + (nextMission != null ? nextMission.getTypeLabel() : "Learn");
            startBtn.setText(btnText);
            startBtn.setTextColor(Color.WHITE);
            startBtn.setTextSize(16);
            startBtn.setTypeface(null, android.graphics.Typeface.BOLD);
            startBtn.setGravity(Gravity.CENTER);
            startBtn.setBackgroundResource(R.drawable.bg_start_btn_glow);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(52));
            btnParams.topMargin = dpToPx(16);
            startBtn.setLayoutParams(btnParams);

            // Breathing animation for start button
            android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(startBtn, "scaleX", 1f, 1.02f, 1f);
            android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(startBtn, "scaleY", 1f, 1.02f, 1f);
            scaleX.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
            scaleY.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
            scaleX.setDuration(1500);
            scaleY.setDuration(1500);
            scaleX.start();
            scaleY.start();

            startBtn.setOnClickListener(v -> launchMission(ch, nextMission));
            card.addView(startBtn);
        }

        // Card Touch Interaction
        if (!ch.isLocked()) {
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start();
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                }
                return false;
            });
            
            if (ch.isActive()) {
                card.setOnClickListener(v -> launchMission(ch, ch.getNextMission()));
            }
        }

        return card;
    }

    private void launchMission(GrammarChapter chapter, GrammarMission mission) {
        if (mission == null)
            return;

        Intent intent;
        switch (mission.type) {
            case LEARN:
                intent = new Intent(this, LearnMissionActivity.class);
                break;
            case PRACTICE:
                intent = new Intent(this, PracticeMissionActivity.class);
                break;
            case TRANSLATE:
                intent = new Intent(this, TranslateMissionActivity.class);
                break;
            case SPEAK:
                intent = new Intent(this, SpeakMissionActivity.class);
                break;
            default:
                return;
        }

        intent.putExtra("CHAPTER_ID", chapter.chapterId);
        intent.putExtra("CHAPTER_NUMBER", chapter.chapterNumber);
        intent.putExtra("CHAPTER_TITLE", chapter.title);
        intent.putExtra("MISSION_ID", mission.missionId);
        intent.putExtra("MISSION_TYPE", mission.type.name());
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChapters(); // Reload on return from mission
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }
}
