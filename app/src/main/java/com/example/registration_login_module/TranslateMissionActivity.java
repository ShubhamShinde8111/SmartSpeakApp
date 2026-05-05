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
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TranslateMissionActivity extends AppCompatActivity {

    private static final String TAG = "TranslateMission";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int SPEECH_REQUEST_CODE = 101;

    // UI
    private TextView tvChapterTitle, tvMissionTitle, tvProgress, tvHindiSentence;
    private TextView tvFeedbackTitle, tvFeedbackBody, tvCorrectAnswer, tvScore;
    private TextView tvMistakes, tvSuggestions;
    private TextView tvStatus, btnNext, btnTryAgain;
    private View cardFeedback, actionButtonsContainer;
    private ImageView btnMic, btnSpeakerHindi, btnSpeakerCorrect, btnBack;
    private ProgressBar progressBar;
    private LinearProgressIndicator recordingProgress;
    private com.google.android.material.progressindicator.CircularProgressIndicator progressScore;

    // Speech & TTS
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech ttsHindi;
    private TextToSpeech ttsEnglish;
    private boolean isListening = false;
    private boolean isProcessing = false;
    private boolean isSpeechServiceAvailable = false;
    private boolean activityActive = true;
    private ObjectAnimator pulseAnimator;
    private String lastPartialResult = "";

    private Handler autoHandler = new Handler(Looper.getMainLooper());

    // Data
    private String chapterId, userId, chapterTitle;
    private List<GrammarDataProvider.TranslateSentence> sentences;
    private int currentIndex = 0, correctCount = 0;
    private MissionAIEngine aiEngine;
    private String currentCorrectEnglish = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.parseColor("#FF6A00"));
        getWindow().setNavigationBarColor(Color.parseColor("#1B2A4A"));

        setContentView(R.layout.activity_translate_mission);

        chapterId = getIntent().getStringExtra("CHAPTER_ID");
        chapterTitle = getIntent().getStringExtra("CHAPTER_TITLE");
        userId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("UID", "");
        aiEngine = new MissionAIEngine(this);

        initUI();
        initTTS();

        isSpeechServiceAvailable = SpeechRecognizer.isRecognitionAvailable(this);
        if (isSpeechServiceAvailable) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            setupSpeechRecognizer();
        }

        sentences = GrammarDataProvider.getTranslateContent(chapterId);
        // Start first sentence is handled inside TTS init once ready
    }

    private void initUI() {
        tvChapterTitle = findViewById(R.id.tvChapterTitle);
        tvMissionTitle = findViewById(R.id.tvMissionTitle);
        tvProgress = findViewById(R.id.tvProgress);
        tvHindiSentence = findViewById(R.id.tvHindiSentence);
        tvFeedbackTitle = findViewById(R.id.tvFeedbackTitle);
        tvFeedbackBody = findViewById(R.id.tvFeedbackBody);
        tvCorrectAnswer = findViewById(R.id.tvCorrectAnswer);
        tvScore = findViewById(R.id.tvScore);
        tvMistakes = findViewById(R.id.tvMistakes);
        tvSuggestions = findViewById(R.id.tvSuggestions);
        tvStatus = findViewById(R.id.tvStatus);
        btnNext = findViewById(R.id.btnNext);
        btnTryAgain = findViewById(R.id.btnTryAgain);
        cardFeedback = findViewById(R.id.cardFeedback);
        actionButtonsContainer = findViewById(R.id.actionButtonsContainer);
        btnMic = findViewById(R.id.btnMic);
        btnSpeakerHindi = findViewById(R.id.btnSpeakerHindi);
        btnSpeakerCorrect = findViewById(R.id.btnSpeakerCorrect);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        recordingProgress = findViewById(R.id.recordingProgress);
        progressScore = findViewById(R.id.progressScore);

        if (chapterTitle != null) {
            tvChapterTitle.setText(chapterTitle);
        }

        btnBack.setOnClickListener(v -> finish());

        btnSpeakerHindi.setOnClickListener(v -> {
            if (sentences != null && !sentences.isEmpty() && ttsHindi != null) {
                stopListeningSilently();
                ttsHindi.speak(sentences.get(currentIndex).hindiSentence, TextToSpeech.QUEUE_FLUSH, null, "manual_read");
            }
        });

        btnSpeakerCorrect.setOnClickListener(v -> {
            if (ttsEnglish != null && !currentCorrectEnglish.isEmpty()) {
                ttsEnglish.speak(currentCorrectEnglish, TextToSpeech.QUEUE_FLUSH, null, "manual_read_en");
            }
        });

        btnMic.setOnClickListener(v -> {
            if (isListening) return;
            isProcessing = false;
            cardFeedback.setVisibility(View.GONE);
            actionButtonsContainer.setVisibility(View.GONE);
            checkPermissionAndStart();
        });

        btnTryAgain.setOnClickListener(v -> {
            if (isListening) return;
            isProcessing = false;
            cardFeedback.setVisibility(View.GONE);
            actionButtonsContainer.setVisibility(View.GONE);
            checkPermissionAndStart();
        });

        btnNext.setOnClickListener(v -> {
            if (currentIndex < sentences.size() - 1) {
                showSentence(currentIndex + 1);
            } else {
                completeMission();
            }
        });
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
                ttsHindi.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) {}
                    @Override public void onError(String utteranceId) {}
                    @Override
                    public void onDone(String utteranceId) {
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
                if (sentences != null && !sentences.isEmpty()) {
                    runOnUiThread(() -> showSentence(0));
                }
            }
        });
    }

    private void setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                tvStatus.setText("🎙️ Listening... Speak now");
                tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                recordingProgress.setVisibility(View.VISIBLE);
                recordingProgress.setIndeterminate(true);
            }

            @Override public void onBeginningOfSpeech() {
                tvStatus.setText("🎙️ Hearing you...");
            }
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                tvStatus.setText("⏳ Processing your answer...");
                tvStatus.setTextColor(Color.parseColor("#FFB300"));
                stopMicPulseAnimation();
                recordingProgress.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onError(int error) {
                Log.w(TAG, "Speech error: " + error);
                if (!activityActive) return;

                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    resetListeningUI();
                    tvStatus.setText("🔇 No speech heard. Tap mic to retry.");
                    tvStatus.setTextColor(Color.parseColor("#FFB300"));
                    return;
                }

                if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    recreateSpeechRecognizer();
                    autoHandler.postDelayed(() -> {
                        if (activityActive && !isProcessing) {
                            safeStartListening();
                        }
                    }, 1000);
                    return;
                }

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
                    processResult(finalSpoken);
                } else if (!isProcessing) {
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

            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        } else {
            safeStartListening();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000);
        return intent;
    }

    private void safeStartListening() {
        if (!activityActive || isProcessing || isListening) return;
        if (ttsHindi != null && ttsHindi.isSpeaking()) ttsHindi.stop();
        if (ttsEnglish != null && ttsEnglish.isSpeaking()) ttsEnglish.stop();

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
            try { speechRecognizer.stopListening(); } catch (Exception ignored) {}
            try { speechRecognizer.cancel(); } catch (Exception ignored) {}
        }
        resetListeningUI();
    }

    private void recreateSpeechRecognizer() {
        isListening = false;
        if (speechRecognizer != null) {
            try { speechRecognizer.cancel(); } catch (Exception ignored) {}
            try { speechRecognizer.destroy(); } catch (Exception ignored) {}
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        setupSpeechRecognizer();
    }

    private void resetListeningUI() {
        isListening = false;
        btnMic.setImageResource(R.drawable.ic_mic);
        btnMic.setBackgroundResource(R.drawable.mic_bg_yellow);
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
                processResult(results.get(0));
            }
        }
    }

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

    private void showSentence(int idx) {
        currentIndex = idx;
        isProcessing = false;
        GrammarDataProvider.TranslateSentence s = sentences.get(idx);

        tvHindiSentence.setText(s.hindiSentence);
        tvProgress.setText((idx + 1) + "/" + sentences.size());
        
        int progressPercent = (int) (((idx) / (float) sentences.size()) * 100);
        ObjectAnimator.ofInt(progressBar, "progress", progressPercent).setDuration(500).start();

        cardFeedback.setVisibility(View.GONE);
        actionButtonsContainer.setVisibility(View.GONE);
        stopListeningSilently();

        tvStatus.setText("🔊 Reading sentence...");
        tvStatus.setTextColor(Color.parseColor("#8899AA"));

        if (ttsHindi != null) {
            ttsHindi.speak(s.hindiSentence, TextToSpeech.QUEUE_FLUSH, null, "auto_read");
        } else {
            tvStatus.setText("Tap the mic to speak");
            tvStatus.setTextColor(Color.parseColor("#8899AA"));
        }
    }

    private void processResult(String spoken) {
        GrammarDataProvider.TranslateSentence s = sentences.get(currentIndex);
        currentCorrectEnglish = s.expectedEnglish;

        tvStatus.setText("🤖 AI is evaluating...");
        tvStatus.setTextColor(Color.parseColor("#CE93D8"));

        aiEngine.evaluateAnswer(spoken, s.expectedEnglish,
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

                    String correction = (result.correction != null && !result.correction.isEmpty()) ? result.correction : s.expectedEnglish;
                    tvCorrectAnswer.setText(correction);
                    currentCorrectEnglish = correction; // Update for TTS

                    // Build color-coded word highlights with bold + strikethrough for wrong words
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

                    tvStatus.setText(currentIndex < sentences.size() - 1
                            ? "Great effort! Tap Next to continue." : "🎉 All done! Tap Next to finish.");
                    tvStatus.setTextColor(Color.parseColor("#8899AA"));
                    
                    actionButtonsContainer.setVisibility(View.VISIBLE);
                    actionButtonsContainer.setAlpha(0f);
                    actionButtonsContainer.animate().alpha(1f).setDuration(300).start();
                    
                    isProcessing = false;
                }));
    }

    private void completeMission() {
        if (userId.isEmpty()) { finish(); return; }
        
        int progressPercent = 100;
        ObjectAnimator.ofInt(progressBar, "progress", progressPercent).setDuration(500).start();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> d = new HashMap<>();
        d.put("completedMissions", 3);
        d.put("completedAt", FieldValue.serverTimestamp());
        Map<String, Object> cd = new HashMap<>(); cd.put(chapterId, d);
        db.collection("users").document(userId).collection("gamification")
                .document("chapter_progress").set(cd, SetOptions.merge());

        GamificationEngine engine = new GamificationEngine(this);
        engine.awardXP(userId, 70, 20, "translate_" + chapterId, new GamificationEngine.XPCallback() {
            @Override
            public void onXPAwarded(int xp, int coins, int total, int level, boolean up) {
                runOnUiThread(() -> {
                    Toast.makeText(TranslateMissionActivity.this,
                            "🎤 Translate Complete! +" + xp + " XP", Toast.LENGTH_LONG).show();
                    if (up) LevelUpDialogFragment.show(TranslateMissionActivity.this, level);
                    finish();
                });
            }
            @Override public void onFailure(String e) { finish(); }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityActive = false;
        stopListeningSilently();
        if (ttsHindi != null && ttsHindi.isSpeaking()) ttsHindi.stop();
        if (ttsEnglish != null && ttsEnglish.isSpeaking()) ttsEnglish.stop();
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
            try { speechRecognizer.cancel(); } catch (Exception ignored) {}
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (ttsHindi != null) {
            ttsHindi.stop();
            ttsHindi.shutdown();
        }
        if (ttsEnglish != null) {
            ttsEnglish.stop();
            ttsEnglish.shutdown();
        }
    }
}
