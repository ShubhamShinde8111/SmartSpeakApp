package com.example.registration_login_module;

import android.content.DialogInterface;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.Button;
import androidx.core.view.WindowCompat;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;

public class LearningActivity extends AppCompatActivity {

    private MaterialCardView vocabularyCard, tenseCard, prepositionCard, articleCard, speechPracticeCard, introCard;
    private MaterialButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_learning);

        initializeViews();
        setupClickListeners();
        applyEntranceAnimations();
    }

    private void initializeViews() {
        vocabularyCard = findViewById(R.id.vocabularyCard);
        tenseCard = findViewById(R.id.tenseCard);
        prepositionCard = findViewById(R.id.prepositionCard);
        articleCard = findViewById(R.id.articleCard);
        speechPracticeCard = findViewById(R.id.speechPracticeCard);
        introCard = findViewById(R.id.introCard);
        backButton = findViewById(R.id.BackBtn);
    }

    private void setupClickListeners() {
        vocabularyCard.setOnClickListener(v -> navigateTo(BasicTest.class));
        tenseCard.setOnClickListener(v -> navigateTo(TenseTest.class));
        prepositionCard.setOnClickListener(v -> navigateTo(PrepositionTest.class));
        articleCard.setOnClickListener(v -> navigateTo(ArticleTest.class));
        speechPracticeCard.setOnClickListener(v -> navigateTo(SpeechPracticeActivity.class));

        backButton.setOnClickListener(v -> finish());
    }

    private void navigateTo(Class<?> destination) {
        startActivity(new Intent(LearningActivity.this, destination));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void applyEntranceAnimations() {
        View[] cards = { introCard, vocabularyCard, tenseCard, prepositionCard, articleCard, speechPracticeCard };
        for (int i = 0; i < cards.length; i++) {
            View card = cards[i];
            if (card != null) {
                card.setAlpha(0f);
                card.setTranslationY(100f);
                card.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(800)
                        .setStartDelay(100 * i)
                        .setInterpolator(new AnticipateOvershootInterpolator(1.0f))
                        .start();
            }
        }
    }

    @Override
    @SuppressLint("MissingSuperCall")
    public void onBackPressed() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Close Journey?")
                .setMessage("Are you sure you want to stop your current learning session?")
                .setNegativeButton("Keep Learning", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Exit", (dialog, which) -> finish())
                .show();
    }
}