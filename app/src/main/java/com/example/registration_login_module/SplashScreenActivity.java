package com.example.registration_login_module;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.view.WindowCompat;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class SplashScreenActivity extends AppCompatActivity {

    private View logoContainer, loadingContainer;
    private ImageView imgLogo;
    private TextView appNameText, taglineText, versionText, loadingText;
    private ProgressBar loadingProgress;
    private View circle1, circle2, circle3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_splash_screen);

        // Immersive transparent status and navigation bar
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        initializeViews();
        startAnimations();
        navigateToNextScreen();
    }

    private void initializeViews() {
        logoContainer = findViewById(R.id.logoContainer);
        imgLogo = findViewById(R.id.imgLogo);
        appNameText = findViewById(R.id.appNameText);
        taglineText = findViewById(R.id.taglineText);
        versionText = findViewById(R.id.versionText);
        loadingProgress = findViewById(R.id.loadingProgress);
        loadingContainer = findViewById(R.id.loadingContainer);
        loadingText = findViewById(R.id.loadingText);
        circle1 = findViewById(R.id.circle1);
        circle2 = findViewById(R.id.circle2);
        circle3 = findViewById(R.id.circle3);
    }

    private void startAnimations() {
        // Set initial states for clean entry
        logoContainer.setAlpha(0f);
        logoContainer.setScaleX(0.6f);
        logoContainer.setScaleY(0.6f);

        appNameText.setAlpha(0f);
        appNameText.setTranslationY(50f);

        taglineText.setAlpha(0f);
        taglineText.setTranslationY(25f);

        loadingContainer.setAlpha(0f);
        loadingContainer.setTranslationY(20f);

        versionText.setAlpha(0f);
        versionText.setTranslationY(15f);

        // 1. Background circles - Fade in with floating animation
        startCircleAnimations();

        // 2. Logo Entry - Premium overshoot animation
        logoContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(900)
                .setInterpolator(new OvershootInterpolator(1.1f))
                .setStartDelay(300)
                .start();

        // 3. App Name - Smooth slide up
        appNameText.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .setStartDelay(700)
                .start();

        // 4. Tagline - Gentle fade and slide
        taglineText.animate()
                .alpha(0.85f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(new DecelerateInterpolator())
                .setStartDelay(950)
                .start();

        // 5. Loading indicator - Subtle appearance
        loadingContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .setStartDelay(1150)
                .start();

        // 6. Footer - Final subtle reveal
        versionText.animate()
                .alpha(0.45f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(1350)
                .start();
    }

    private void startCircleAnimations() {
        // Circle 1 - Large, top-left, slow float
        circle1.animate()
                .alpha(0.15f)
                .setDuration(1000)
                .setStartDelay(100)
                .start();
        startFloatingAnimation(circle1, 15f, 4500);

        // Circle 2 - Medium, bottom-right, medium float
        circle2.animate()
                .alpha(0.12f)
                .setDuration(1000)
                .setStartDelay(250)
                .start();
        startFloatingAnimation(circle2, 12f, 3800);

        // Circle 3 - Small, center-right, fast float
        circle3.animate()
                .alpha(0.1f)
                .setDuration(1000)
                .setStartDelay(400)
                .start();
        startFloatingAnimation(circle3, 10f, 3200);
    }

    private void startFloatingAnimation(View view, float amplitude, long duration) {
        // Vertical floating
        ObjectAnimator floatY = ObjectAnimator.ofFloat(view, "translationY", 0f, amplitude, 0f, -amplitude, 0f);
        floatY.setDuration(duration);
        floatY.setRepeatCount(ValueAnimator.INFINITE);
        floatY.setInterpolator(new AccelerateDecelerateInterpolator());

        // Horizontal subtle drift
        ObjectAnimator floatX = ObjectAnimator.ofFloat(view, "translationX", 0f, amplitude * 0.5f, 0f,
                -amplitude * 0.5f, 0f);
        floatX.setDuration(duration + 500);
        floatX.setRepeatCount(ValueAnimator.INFINITE);
        floatX.setInterpolator(new AccelerateDecelerateInterpolator());

        // Start with slight delay for natural feel
        new Handler().postDelayed(() -> {
            floatY.start();
            floatX.start();
        }, 500);
    }

    private void navigateToNextScreen() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Fade out animation before navigation
                fadeOutAndNavigate();
            }
        }, 3000);
    }

    private void fadeOutAndNavigate() {
        // Create elegant fade out animation
        View mainLayout = findViewById(R.id.main);

        // Scale down slightly while fading for premium feel
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(mainLayout, "alpha", 1f, 0f);
        fadeOut.setDuration(400);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mainLayout, "scaleX", 1f, 0.95f);
        scaleX.setDuration(400);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mainLayout, "scaleY", 1f, 0.95f);
        scaleY.setDuration(400);

        AnimatorSet exitAnimation = new AnimatorSet();
        exitAnimation.playTogether(fadeOut, scaleX, scaleY);
        exitAnimation.setInterpolator(new AccelerateDecelerateInterpolator());

        exitAnimation.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // Navigate to next screen
                SharedPreferences shpf = getSharedPreferences("Login", MODE_PRIVATE);
                Boolean check = shpf.getBoolean("flag", false);
                Intent iHome;
                if (check) {
                    iHome = new Intent(SplashScreenActivity.this, HomeActivity.class);
                } else {
                    iHome = new Intent(SplashScreenActivity.this, LoginActivity.class);
                }
                startActivity(iHome);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        exitAnimation.start();
    }
}