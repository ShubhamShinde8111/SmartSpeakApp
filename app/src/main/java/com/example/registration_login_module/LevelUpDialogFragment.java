package com.example.registration_login_module;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

/**
 * LevelUpDialogFragment — Full-screen celebration overlay when user levels up.
 *
 * Shows:
 * - Star burst animation
 * - "LEVEL UP!" title with gold text
 * - Large animated level number in circular badge
 * - Congratulations message
 * - Continue button
 *
 * Usage: LevelUpDialogFragment.show(activity, newLevel);
 */
public class LevelUpDialogFragment extends DialogFragment {

    private static final String ARG_LEVEL = "level";

    public static void show(FragmentActivity activity, int newLevel) {
        LevelUpDialogFragment fragment = new LevelUpDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_LEVEL, newLevel);
        fragment.setArguments(args);
        fragment.setCancelable(true);
        fragment.show(activity.getSupportFragmentManager(), "level_up");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_level_up, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int level = getArguments() != null ? getArguments().getInt(ARG_LEVEL, 2) : 2;

        // Set level text
        TextView tvNewLevel = view.findViewById(R.id.tvNewLevel);
        tvNewLevel.setText(String.valueOf(level));

        // Set message
        TextView tvMessage = view.findViewById(R.id.tvLevelUpMessage);
        tvMessage.setText(getString(R.string.level_up_message, level));

        // Continue button
        view.findViewById(R.id.btnContinue).setOnClickListener(v -> dismiss());

        // Tap anywhere to dismiss
        view.findViewById(R.id.levelUpRoot).setOnClickListener(v -> dismiss());

        // Apply animations
        playAnimations(view);
    }

    private void playAnimations(View view) {
        View content = view.findViewById(R.id.levelUpContent);
        TextView stars = view.findViewById(R.id.tvStarBurst);
        TextView title = view.findViewById(R.id.tvLevelUpTitle);
        View levelBadge = view.findViewById(R.id.tvNewLevel);

        // Content fade in
        content.setAlpha(0f);
        content.setScaleX(0.8f);
        content.setScaleY(0.8f);
        content.animate()
                .alpha(1f)
                .scaleX(1f).scaleY(1f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();

        // Stars burst animation
        stars.setAlpha(0f);
        stars.setScaleX(0.3f);
        stars.setScaleY(0.3f);
        stars.animate()
                .alpha(1f)
                .scaleX(1.3f).scaleY(1.3f)
                .setDuration(600)
                .setStartDelay(200)
                .setInterpolator(new OvershootInterpolator(2f))
                .withEndAction(() -> stars.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(300)
                        .start())
                .start();

        // Title slide + scale
        title.setTranslationY(40f);
        title.setAlpha(0f);
        title.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(300)
                .start();

        // Level badge scale bounce
        if (levelBadge != null) {
            levelBadge.setScaleX(0f);
            levelBadge.setScaleY(0f);
            levelBadge.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(600)
                    .setStartDelay(400)
                    .setInterpolator(new OvershootInterpolator(3f))
                    .start();
        }

        // Continue button fade in
        View continueBtn = view.findViewById(R.id.btnContinue);
        continueBtn.setAlpha(0f);
        continueBtn.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(800)
                .start();
    }
}
