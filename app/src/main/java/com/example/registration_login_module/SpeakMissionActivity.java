package com.example.registration_login_module;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import android.view.animation.DecelerateInterpolator;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SpeakMissionActivity extends AppCompatActivity {

    private static final String TAG = "SpeakMission";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int SPEECH_REQUEST_CODE = 101;

    // UI
    private TextView tvChapterTitle, tvProgress, tvQuestion;
    private TextView tvFeedbackTitle, tvFeedbackBody, tvScore, tvCorrectAnswer;
    private TextView tvMistakes, tvSuggestions;
    private TextView tvStatus, btnNext, btnTryAgain;
    private View cardFeedback, actionButtonsContainer;
    private ImageView btnMic, btnSpeakerCorrect;
    private ProgressBar progressBar;
    private LinearProgressIndicator recordingProgress;
    private com.google.android.material.progressindicator.CircularProgressIndicator progressScore;

    // Speech & TTS
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private boolean isListening = false;
    private boolean isProcessing = false; // Prevent double-submit
    private boolean isSpeechServiceAvailable = false;
    private boolean activityActive = true;
    private ObjectAnimator pulseAnimator;
    private String lastPartialResult = "";

    // Auto-mode handler
    private Handler autoHandler = new Handler(Looper.getMainLooper());

    // Data
    private String chapterId, userId, chapterTitle;
    private List<GrammarDataProvider.SpeakQuestion> questions;
    private int currentIndex = 0, correctCount = 0;
    private MissionAIEngine aiEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.parseColor("#9C27B0"));
        getWindow().setNavigationBarColor(Color.parseColor("#1B2A4A"));
        setContentView(R.layout.activity_speak_mission);

        // Get intent data
        chapterId = getIntent().getStringExtra("CHAPTER_ID");
        chapterTitle = getIntent().getStringExtra("CHAPTER_TITLE");
        userId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("UID", "");
        aiEngine = new MissionAIEngine(this);

        // Bind views
        tvChapterTitle = findViewById(R.id.tvChapterTitle);
        tvProgress = findViewById(R.id.tvProgress);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvFeedbackTitle = findViewById(R.id.tvFeedbackTitle);
        tvFeedbackBody = findViewById(R.id.tvFeedbackBody);
        tvScore = findViewById(R.id.tvScore);
        tvMistakes = findViewById(R.id.tvMistakes);
        tvCorrectAnswer = findViewById(R.id.tvCorrectAnswer);
        tvSuggestions = findViewById(R.id.tvSuggestions);
        tvStatus = findViewById(R.id.tvStatus);
        btnNext = findViewById(R.id.btnNext);
        btnTryAgain = findViewById(R.id.btnTryAgain);
        cardFeedback = findViewById(R.id.cardFeedback);
        actionButtonsContainer = findViewById(R.id.actionButtonsContainer);
        btnMic = findViewById(R.id.btnMic);
        btnSpeakerCorrect = findViewById(R.id.btnSpeakerCorrect);
        progressBar = findViewById(R.id.progressBar);
        recordingProgress = findViewById(R.id.recordingProgress);
        progressScore = findViewById(R.id.progressScore);

        // Set chapter title
        if (chapterTitle != null) {
            tvChapterTitle.setText(chapterTitle);
        }

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Initialize Text-To-Speech with auto-listen after speech
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                    }

                    @Override
                    public void onError(String utteranceId) {
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        // After TTS finishes reading, prompt the user to tap the mic manually
                        if ("auto_read".equals(utteranceId) && activityActive) {
                            runOnUiThread(() -> {
                                if (activityActive && !isProcessing) {
                                    tvStatus.setText("Tap the mic to speak");
                                    tvStatus.setTextColor(Color.parseColor("#8899AA"));
                                }
                            });
                        }
                    }
                });
                // Start first question after TTS is ready
                if (questions != null && !questions.isEmpty()) {
                    showQuestion(0);
                }
            }
        });

        // Speaker button — replay question
        ImageView btnSpeakerQuestion = findViewById(R.id.btnSpeakerQuestion);
        btnSpeakerQuestion.setOnClickListener(v -> {
            if (questions != null && !questions.isEmpty() && tts != null) {
                stopListeningSilently();
                tts.speak(questions.get(currentIndex).question, TextToSpeech.QUEUE_FLUSH, null, "manual_read");
            }
        });

        // Speech recognizer
        isSpeechServiceAvailable = SpeechRecognizer.isRecognitionAvailable(this);
        if (isSpeechServiceAvailable) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            setupSpeechRecognizer();
        }

        // Mic button — retry recording (re-record answer)
        btnMic.setOnClickListener(v -> {
            if (isListening) {
                // If already listening, ignore tap
                return;
            }
            // Reset and re-record
            isProcessing = false;
            cardFeedback.setVisibility(View.GONE);
            actionButtonsContainer.setVisibility(View.GONE);
            checkPermissionAndStart();
        });

        // Try Again button
        btnTryAgain.setOnClickListener(v -> {
            if (isListening) return;
            isProcessing = false;
            cardFeedback.setVisibility(View.GONE);
            actionButtonsContainer.setVisibility(View.GONE);
            checkPermissionAndStart();
        });

        // Next button
        btnNext.setOnClickListener(v -> {
            if (currentIndex < questions.size() - 1) {
                showQuestion(currentIndex + 1);
            } else {
                completeMission();
            }
        });

        // Load data
        questions = GrammarDataProvider.getSpeakContent(chapterId);
        showQuestion(0);
    }

    // ─── Speech Recognizer Setup (Auto-Stop on Silence) ───────────────────
    private void setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                tvStatus.setText("🎙️ Listening... Speak now");
                tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                recordingProgress.setVisibility(View.VISIBLE);
                recordingProgress.setIndeterminate(true);
            }

            @Override
            public void onBeginningOfSpeech() {
                tvStatus.setText("🎙️ Hearing you...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                // Silence detected — processing will happen in onResults
                tvStatus.setText("⏳ Processing your answer...");
                tvStatus.setTextColor(Color.parseColor("#FFB300"));
                stopMicPulseAnimation();
                recordingProgress.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onError(int error) {
                Log.w(TAG, "Speech error: " + error);
                if (!activityActive)
                    return;

                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    // No speech heard — wait for user to retry manually
                    resetListeningUI();
                    tvStatus.setText("🔇 No speech heard. Tap mic to retry.");
                    tvStatus.setTextColor(Color.parseColor("#FFB300"));
                    return;
                }

                if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    // Recoverable — recreate and retry
                    recreateSpeechRecognizer();
                    autoHandler.postDelayed(() -> {
                        if (activityActive && !isProcessing) {
                            safeStartListening();
                        }
                    }, 1000);
                    return;
                }

                // Non-recoverable errors
                resetListeningUI();
                tvStatus.setText("⚠️ Error. Tap mic to retry.");
                tvStatus.setTextColor(Color.parseColor("#FF6B6B"));
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                String finalSpoken = "";
                if (matches != null && !matches.isEmpty()) {
                    finalSpoken = matches.get(0).trim();
                }
                // Fallback: use partial result if final is empty
                if (finalSpoken.isEmpty() && !lastPartialResult.isEmpty()) {
                    finalSpoken = lastPartialResult;
                }

                if (!finalSpoken.isEmpty() && !isProcessing) {
                    isProcessing = true;
                    resetListeningUI();
                    processAnswer(finalSpoken);
                } else if (!isProcessing) {
                    // Empty result — wait for user to retry manually
                    resetListeningUI();
                    tvStatus.setText("🔇 Didn't catch that. Tap mic to retry.");
                    tvStatus.setTextColor(Color.parseColor("#FFB300"));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    lastPartialResult = matches.get(0).trim();
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });
    }

    // ─── Permission & Start ──────────────────────────────────────────────
    private void checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.RECORD_AUDIO }, PERMISSION_REQUEST_CODE);
        } else {
            safeStartListening();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            safeStartListening();
        } else {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private Intent createRecognizerIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        // Let Android auto-detect silence and stop
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000);
        return intent;
    }

    // ─── Safe Start/Stop Listening ────────────────────────────────────────
    private void safeStartListening() {
        if (!activityActive || isProcessing || isListening)
            return;
        if (tts != null && tts.isSpeaking())
            tts.stop();

        if (isSpeechServiceAvailable && speechRecognizer != null) {
            try {
                isListening = true;
                lastPartialResult = "";
                speechRecognizer.startListening(createRecognizerIntent());
                btnMic.setImageResource(R.drawable.ic_mic_off);
                btnMic.setBackgroundResource(R.drawable.mic_bg_red);
                startMicPulseAnimation();
                tvStatus.setText("🎙️ Initializing...");
                tvStatus.setTextColor(Color.parseColor("#FFB300"));
            } catch (Exception e) {
                Log.e(TAG, "startListening failed", e);
                isListening = false;
                tvStatus.setText("⚠️ Tap mic to retry");
            }
        } else {
            // Fallback to system dialog
            try {
                startActivityForResult(createRecognizerIntent(), SPEECH_REQUEST_CODE);
            } catch (Exception e) {
                Toast.makeText(this, "No speech app found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopListeningSilently() {
        autoHandler.removeCallbacksAndMessages(null);
        isListening = false;
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
            } catch (Exception ignored) {
            }
            try {
                speechRecognizer.cancel();
            } catch (Exception ignored) {
            }
        }
        resetListeningUI();
    }

    private void recreateSpeechRecognizer() {
        isListening = false;
        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel();
            } catch (Exception ignored) {
            }
            try {
                speechRecognizer.destroy();
            } catch (Exception ignored) {
            }
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        setupSpeechRecognizer();
    }

    private void resetListeningUI() {
        isListening = false;
        btnMic.setImageResource(R.drawable.ic_mic);
        btnMic.setBackgroundResource(R.drawable.mic_bg_purple);
        stopMicPulseAnimation();
        recordingProgress.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                isProcessing = true;
                processAnswer(results.get(0));
            }
        }
    }

    // ─── Mic Pulse Animation ──────────────────────────────────────────
    private void startMicPulseAnimation() {
        if (pulseAnimator == null) {
            pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(btnMic,
                    PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.15f),
                    PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.15f));
            pulseAnimator.setDuration(800);
            pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            pulseAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        }
        pulseAnimator.start();
    }

    private void stopMicPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            btnMic.setScaleX(1.0f);
            btnMic.setScaleY(1.0f);
        }
    }

    // ─── Show Question (Auto-Read → Auto-Listen) ─────────────────────────
    private void showQuestion(int idx) {
        currentIndex = idx;
        isProcessing = false;
        GrammarDataProvider.SpeakQuestion q = questions.get(idx);

        tvQuestion.setText(q.question);
        tvProgress.setText((idx + 1) + "/" + questions.size());
        
        int progressPercent = (int) (((idx) / (float) questions.size()) * 100);
        ObjectAnimator.ofInt(progressBar, "progress", progressPercent).setDuration(500).start();

        cardFeedback.setVisibility(View.GONE);
        actionButtonsContainer.setVisibility(View.GONE);
        stopListeningSilently();

        tvStatus.setText("🔊 Reading question...");
        tvStatus.setTextColor(Color.parseColor("#8899AA"));

        // Read the question, but DO NOT auto-listen. Wait for user tap.
        if (tts != null) {
            tts.speak(q.question, TextToSpeech.QUEUE_FLUSH, null, "auto_read");
        } else {
            // TTS not ready
            tvStatus.setText("Tap the mic to speak");
            tvStatus.setTextColor(Color.parseColor("#8899AA"));
        }
    }

    // ─── Process Speech Answer with AI Evaluation ─────────────────────────
    private void processAnswer(String spoken) {
        GrammarDataProvider.SpeakQuestion q = questions.get(currentIndex);

        tvStatus.setText("🤖 AI is evaluating...");
        tvStatus.setTextColor(Color.parseColor("#CE93D8"));

        aiEngine.evaluateAnswer(spoken, q.expectedKeywords,
                chapterTitle != null ? chapterTitle : "grammar",
                result -> runOnUiThread(() -> {
                    // Trigger slide up animation
                    cardFeedback.setVisibility(View.VISIBLE);
                    cardFeedback.setAlpha(0f);
                    cardFeedback.setTranslationY(100f);
                    cardFeedback.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(400)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();

                    // Animate the score ring
                    progressScore.setProgress(0);
                    android.animation.ObjectAnimator.ofInt(progressScore, "progress", result.score)
                            .setDuration(600).start();
                    tvScore.setText(result.score + "%");

                    // Score-based visual theming (5 tiers)
                    if (result.score >= 90) {
                        correctCount++;
                        cardFeedback.setBackgroundResource(R.drawable.card_feedback_green);
                        tvFeedbackTitle.setTextColor(Color.parseColor("#22C55E"));
                        tvFeedbackTitle.setText("🌟 Excellent!");
                    } else if (result.score >= 70) {
                        if (result.isCorrect) correctCount++;
                        cardFeedback.setBackgroundResource(R.drawable.card_feedback_green);
                        tvFeedbackTitle.setTextColor(Color.parseColor("#4ADE80"));
                        tvFeedbackTitle.setText("👍 Good Job!");
                    } else if (result.score >= 50) {
                        cardFeedback.setBackgroundResource(R.drawable.card_feedback_yellow);
                        tvFeedbackTitle.setTextColor(Color.parseColor("#FFB300"));
                        tvFeedbackTitle.setText("⚠️ Almost There");
                    } else if (result.score >= 30) {
                        cardFeedback.setBackgroundResource(R.drawable.card_feedback_red);
                        tvFeedbackTitle.setTextColor(Color.parseColor("#FF7043"));
                        tvFeedbackTitle.setText("📝 Keep Trying");
                    } else {
                        cardFeedback.setBackgroundResource(R.drawable.card_feedback_red);
                        tvFeedbackTitle.setTextColor(Color.parseColor("#EF4444"));
                        tvFeedbackTitle.setText("❌ Needs Practice");
                    }

                    tvFeedbackBody.setText(result.summary != null && !result.summary.isEmpty() ? result.summary : "Keep practicing!");

                    // Build color-coded word highlights with bold for wrong words
                    if (result.highlightedUserAnswer != null && !result.highlightedUserAnswer.isEmpty()) {
                        android.text.SpannableStringBuilder builder = new android.text.SpannableStringBuilder();
                        for (MissionAIEngine.HighlightedWord hw : result.highlightedUserAnswer) {
                            int start = builder.length();
                            builder.append(hw.word).append(" ");
                            int end = builder.length() - 1;
                            boolean isCorrectWord = "correct".equalsIgnoreCase(hw.status);
                            int color = isCorrectWord ? Color.parseColor("#22C55E") : Color.parseColor("#EF4444");
                            builder.setSpan(new android.text.style.ForegroundColorSpan(color), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            if (!isCorrectWord) {
                                builder.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                builder.setSpan(new android.text.style.StrikethroughSpan(), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                        }
                        tvMistakes.setText(builder);
                    } else {
                        tvMistakes.setText(spoken);
                    }

                    if (result.correction != null && !result.correction.isEmpty()) {
                        tvCorrectAnswer.setText(result.correction);
                    } else {
                        tvCorrectAnswer.setText(q.expectedKeywords); // Fallback
                    }

                    btnSpeakerCorrect.setOnClickListener(v -> {
                        if (tts != null) {
                            tts.speak(tvCorrectAnswer.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, "correct_ans");
                        }
                    });

                    // Build tips section
                    StringBuilder suggestions = new StringBuilder();
                    if (result.tips != null && !result.tips.isEmpty()) {
                        suggestions.append("💡 Tips:\n");
                        for (String tip : result.tips) {
                            suggestions.append("  • ").append(tip).append("\n");
                        }
                    }
                    if (suggestions.length() > 0) {
                        tvSuggestions.setText(suggestions.toString().trim());
                        tvSuggestions.setVisibility(View.VISIBLE);
                    } else {
                        tvSuggestions.setVisibility(View.GONE);
                    }

                    tvStatus.setText(currentIndex < questions.size() - 1
                            ? "Tap Next to continue"
                            : "🎉 All done! Tap Next to finish.");
                    tvStatus.setTextColor(Color.parseColor("#8899AA"));
                    
                    actionButtonsContainer.setVisibility(View.VISIBLE);
                    actionButtonsContainer.setAlpha(0f);
                    actionButtonsContainer.animate().alpha(1f).setDuration(300).start();
                    
                    isProcessing = false; // Allow mic retries after evaluation finishes
                }));
    }

    private void completeMission() {
        if (userId.isEmpty()) {
            finish();
            return;
        }

        int progressPercent = 100;
        ObjectAnimator.ofInt(progressBar, "progress", progressPercent).setDuration(500).start();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> d = new HashMap<>();
        d.put("completedMissions", 4);
        d.put("completedAt", FieldValue.serverTimestamp());
        Map<String, Object> cd = new HashMap<>();
        cd.put(chapterId, d);
        db.collection("users").document(userId)
                .collection("gamification").document("chapter_progress")
                .set(cd, SetOptions.merge());

        GamificationEngine engine = new GamificationEngine(this);
        engine.awardXP(userId, 80, 25, "speak_" + chapterId,
                new GamificationEngine.XPCallback() {
                    @Override
                    public void onXPAwarded(int xp, int coins, int total, int level, boolean up) {
                        runOnUiThread(() -> {
                            Toast.makeText(SpeakMissionActivity.this,
                                    "💬 Speaking Complete! +" + xp + " XP\n🎉 Chapter Finished!",
                                    Toast.LENGTH_LONG).show();
                            if (up)
                                LevelUpDialogFragment.show(SpeakMissionActivity.this, level);
                            finish();
                        });
                    }

                    @Override
                    public void onFailure(String e) {
                        finish();
                    }
                });
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────
    @Override
    protected void onPause() {
        super.onPause();
        activityActive = false;
        stopListeningSilently();
        if (tts != null && tts.isSpeaking())
            tts.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityActive = true;
        if (isSpeechServiceAvailable && speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            setupSpeechRecognizer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityActive = false;
        autoHandler.removeCallbacksAndMessages(null);
        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel();
            } catch (Exception ignored) {
            }
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
