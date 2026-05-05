package com.example.registration_login_module;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.content.Intent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.content.SharedPreferences;
import androidx.core.content.ContextCompat;

import com.example.registration_login_module.AudioRecorder;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class SelectRole extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    // UI Components
    private TextView botResponseView;
    private TextView roleTitle;
    private TextView roleSubtitle;
    private TextView streakCount;
    private TextView recordText;
    private TextView recordingTimer;
    private ImageView roleIcon;
    private ImageView micIcon;
    private MaterialCardView recordButtonCard;
    private MaterialCardView headerCard;
    private MaterialCardView conversationCard;
    private LinearLayout recordButton;
    private LinearLayout stopRecordingButton;
    private LinearLayout typingIndicator;
    private LinearLayout audioVisualizer;
    private LinearLayout exitButton;
    private LinearLayout progressButton;
    private FrameLayout loadingOverlay;
    private FrameLayout pulseContainer;
    private View listeningOverlay;
    private View dot1, dot2, dot3;
    private View bar1, bar2, bar3, bar4, bar5;

    private StringBuilder chatHistory;
    private TextToSpeech textToSpeech;
    private String role;
    private AudioRecorder audioRecorder;
    private boolean audioPermissionGranted = false;
    private boolean isExiting = false;
    private boolean useLocalSpeechRecognizer = false;
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private boolean isRecording = false;

    private EnglishAnalyzer englishAnalyzer;
    private LearningProgressDBHelper learningProgressDb;
    private String currentUserId;

    // Animation handlers
    private Handler animationHandler = new Handler();
    private Runnable typingAnimationRunnable;
    private Runnable visualizerAnimationRunnable;
    private long recordingStartTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_role);

        initializeViews();
        initializeData();
        checkAudioPermission();
        setupAnimations();
        setupClickListeners();

        // Get role from intent
        role = getIntent().getStringExtra("ROLE");
        if (role == null || role.isEmpty()) {
            role = "assistant";
        }

        updateRoleUI();
        initializeSpeechRecognition();
        initializeTextToSpeech();

        // Animate entrance
        animateEntrance();
    }

    private void initializeViews() {
        // Header views
        headerCard = findViewById(R.id.header_card);
        roleTitle = findViewById(R.id.role_title);
        roleSubtitle = findViewById(R.id.role_subtitle);
        roleIcon = findViewById(R.id.role_icon);
        streakCount = findViewById(R.id.streak_count);

        // Conversation views
        conversationCard = findViewById(R.id.conversation_card);
        botResponseView = findViewById(R.id.bot_response);
        typingIndicator = findViewById(R.id.typing_indicator);
        listeningOverlay = findViewById(R.id.listening_overlay);
        audioVisualizer = findViewById(R.id.audio_visualizer);

        // Typing dots
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);

        // Visualizer bars
        bar1 = findViewById(R.id.bar1);
        bar2 = findViewById(R.id.bar2);
        bar3 = findViewById(R.id.bar3);
        bar4 = findViewById(R.id.bar4);
        bar5 = findViewById(R.id.bar5);

        // Control buttons
        recordButtonCard = findViewById(R.id.record_button_card);
        recordButton = findViewById(R.id.record_button);
        stopRecordingButton = findViewById(R.id.stop_recording_button);
        micIcon = findViewById(R.id.mic_icon);
        recordText = findViewById(R.id.record_text);
        recordingTimer = findViewById(R.id.recording_timer);
        pulseContainer = findViewById(R.id.pulse_container);
        exitButton = findViewById(R.id.exit_button);
        progressButton = findViewById(R.id.progress_button);
        loadingOverlay = findViewById(R.id.loading_overlay);
    }

    private void initializeData() {
        chatHistory = new StringBuilder();
        englishAnalyzer = new EnglishAnalyzer();
        learningProgressDb = new LearningProgressDBHelper(this);

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("UID", null);
    }

    private void updateRoleUI() {
        switch (role.toLowerCase()) {
            case "teacher":
                roleTitle.setText("English Teacher");
                roleSubtitle.setText("Learning Mode");
                // Set teacher icon if you have one
                break;
            case "friend":
                roleTitle.setText("Friendly Chat");
                roleSubtitle.setText("Casual Conversation");
                break;
            case "interviewer":
                roleTitle.setText("Job Interview");
                roleSubtitle.setText("Professional Mode");
                break;
            case "therapist":
                roleTitle.setText("Therapy Session");
                roleSubtitle.setText("Safe Space");
                break;
            case "coach":
                roleTitle.setText("Life Coach");
                roleSubtitle.setText("Motivational Mode");
                break;
            default:
                roleTitle.setText("AI Assistant");
                roleSubtitle.setText("Ready to help");
        }
    }

    private void setupAnimations() {
        // Setup typing indicator animation
        typingAnimationRunnable = new Runnable() {
            int[] delays = {0, 200, 400};
            int currentIndex = 0;

            @Override
            public void run() {
                if (typingIndicator.getVisibility() == View.VISIBLE) {
                    animateDot(currentIndex);
                    currentIndex = (currentIndex + 1) % 3;
                    animationHandler.postDelayed(this, 200);
                }
            }

            private void animateDot(int index) {
                View dot = index == 0 ? dot1 : (index == 1 ? dot2 : dot3);
                ObjectAnimator moveUp = ObjectAnimator.ofFloat(dot, "translationY", 0f, -20f);
                ObjectAnimator moveDown = ObjectAnimator.ofFloat(dot, "translationY", -20f, 0f);

                AnimatorSet set = new AnimatorSet();
                set.playSequentially(moveUp, moveDown);
                set.setDuration(300);
                set.setInterpolator(new AccelerateDecelerateInterpolator());
                set.start();
            }
        };

        // Setup visualizer animation
        visualizerAnimationRunnable = new Runnable() {
            @Override
            public void run() {
                if (audioVisualizer.getVisibility() == View.VISIBLE) {
                    animateBar(bar1, 20, 60);
                    animateBar(bar2, 40, 100);
                    animateBar(bar3, 60, 120);
                    animateBar(bar4, 50, 90);
                    animateBar(bar5, 30, 70);
                    animationHandler.postDelayed(this, 200);
                }
            }

            private void animateBar(View bar, int minHeight, int maxHeight) {
                int randomHeight = minHeight + (int)(Math.random() * (maxHeight - minHeight));
                ValueAnimator animator = ValueAnimator.ofInt(bar.getLayoutParams().height, randomHeight);
                animator.setDuration(200);
                animator.addUpdateListener(animation -> {
                    bar.getLayoutParams().height = (int) animation.getAnimatedValue();
                    bar.requestLayout();
                });
                animator.start();
            }
        };
    }

    private void setupClickListeners() {
        recordButton.setOnClickListener(v -> {
            animateButtonPress(recordButtonCard);
            recordStreakActivity();
            startSpeechRecognition();
        });

        stopRecordingButton.setOnClickListener(v -> {
            animateButtonPress(recordButtonCard);
            stopSpeechRecognition();
        });

        exitButton.setOnClickListener(v -> {
            animateButtonPress((MaterialCardView) v.getParent());
            exitConversation();
        });

        progressButton.setOnClickListener(v -> {
            animateButtonPress((MaterialCardView) v.getParent());
            Toast.makeText(this, "Progress feature coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void animateEntrance() {
        // Animate header card
        headerCard.setAlpha(0f);
        headerCard.setTranslationY(-100f);
        headerCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator())
                .start();

        // Animate conversation card
        conversationCard.setAlpha(0f);
        conversationCard.setScaleX(0.9f);
        conversationCard.setScaleY(0.9f);
        conversationCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(200)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator())
                .start();

        // Animate controls
        View controlsContainer = findViewById(R.id.controls_container);
        controlsContainer.setAlpha(0f);
        controlsContainer.setTranslationY(100f);
        controlsContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(400)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    private void animateButtonPress(MaterialCardView button) {
        button.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    button.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void animateStreakBadge() {
        View streakBadge = findViewById(R.id.streak_badge);
        streakBadge.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(200)
                .withEndAction(() -> {
                    streakBadge.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start();
                })
                .start();
    }

    private void initializeSpeechRecognition() {
        String assemblyKey = getString(R.string.assembly_ai_api_key);
        boolean localAvailable = SpeechRecognizer.isRecognitionAvailable(this);

        if (localAvailable) {
            useLocalSpeechRecognizer = true;
            setupLocalSpeechRecognizer();
        } else if (assemblyKey != null && !assemblyKey.trim().isEmpty()) {
            useLocalSpeechRecognizer = false;
            audioRecorder = new AudioRecorder(assemblyKey);
        } else {
            useLocalSpeechRecognizer = true;
            showSpeechServiceInstallDialog();
        }
    }

    private void setupLocalSpeechRecognizer() {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    runOnUiThread(() -> {
                        roleSubtitle.setText("Listening...");
                        showListeningAnimation();
                    });
                }

                @Override
                public void onBeginningOfSpeech() {
                    runOnUiThread(() -> roleSubtitle.setText("I hear you..."));
                }

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    runOnUiThread(() -> {
                        roleSubtitle.setText("Processing...");
                        showThinkingAnimation();
                    });
                }

                @Override
                public void onError(int error) {
                    runOnUiThread(() -> {
                        roleSubtitle.setText("Ready to chat");
                        Toast.makeText(SelectRole.this, mapSpeechError(error), Toast.LENGTH_SHORT).show();
                        resetRecordingState();
                    });
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenText = matches.get(0);
                        runOnUiThread(() -> {
                            handleUserInput(spokenText);
                            resetRecordingState();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(SelectRole.this, "No speech recognized", Toast.LENGTH_SHORT).show();
                            resetRecordingState();
                        });
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String partial = matches.get(0);
                        runOnUiThread(() -> botResponseView.setText("👂 " + partial + "..."));
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(SelectRole.this,
                    "Speech recognition initialization failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
        }
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    runOnUiThread(() -> Toast.makeText(SelectRole.this,
                            "TTS language not supported",
                            Toast.LENGTH_SHORT).show());
                } else {
                    String initialMessage = generateDynamicGreeting();
                    chatHistory.append("Assistant: ").append(initialMessage).append("\n");
                    runOnUiThread(() -> {
                        animateTextChange(botResponseView, initialMessage);
                        textToSpeech.speak(initialMessage, TextToSpeech.QUEUE_FLUSH, null, null);
                    });
                }
            } else {
                runOnUiThread(() -> Toast.makeText(SelectRole.this,
                        "TTS initialization failed",
                        Toast.LENGTH_SHORT).show());
            }
        });
    }

    private String generateDynamicGreeting() {
        switch (role.toLowerCase()) {
            case "teacher":
                return "Hello! I'm your English teacher. What would you like to practice today?";
            case "friend":
                return "Hey there! What's on your mind today?";
            case "interviewer":
                return "Good day! I'll be conducting your interview. Are you ready to begin?";
            case "therapist":
                return "Hello. This is a safe space. How are you feeling today?";
            case "coach":
                return "Hello! I'm your coach. What are you working on?";
            default:
                return "Hello! How can I help you today?";
        }
    }

    private void startSpeechRecognition() {
        if (isExiting) {
            Toast.makeText(this, "Conversation has ended", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!audioPermissionGranted) {
            checkAudioPermission();
            Toast.makeText(this, "Please grant microphone permission", Toast.LENGTH_SHORT).show();
            return;
        }

        if (useLocalSpeechRecognizer) {
            if (speechRecognizer != null) {
                try {
                    speechRecognizer.startListening(speechIntent);
                    isRecording = true;
                    recordingStartTime = System.currentTimeMillis();
                    updateRecordingUI(true);
                    startRecordingTimer();
                } catch (Exception e) {
                    Toast.makeText(this, "Speech recognition failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                setupLocalSpeechRecognizer();
            }
        } else {
            if (audioRecorder != null) {
                try {
                    audioRecorder.startRecording(this);
                    isRecording = true;
                    recordingStartTime = System.currentTimeMillis();
                    updateRecordingUI(true);
                    startRecordingTimer();
                } catch (Exception e) {
                    Toast.makeText(this, "Recording failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void stopSpeechRecognition() {
        if (!isRecording) return;

        if (useLocalSpeechRecognizer) {
            if (speechRecognizer != null) {
                speechRecognizer.stopListening();
            }
        } else {
            if (audioRecorder != null) {
                audioRecorder.stopRecording();
                processAudioRecording();
            }
        }

        isRecording = false;
        updateRecordingUI(false);
        stopRecordingTimer();
    }

    private void processAudioRecording() {
        showThinkingAnimation();
        new Thread(() -> {
            try {
                File pcmFile = new File(getExternalCacheDir(), "recorded_audio.pcm");
                File wavFile = audioRecorder.convertPcmToWav(pcmFile);
                if (wavFile == null || !wavFile.exists() || wavFile.length() < 2048) {
                    runOnUiThread(() -> {
                        hideThinkingAnimation();
                        Toast.makeText(this, "No audio captured", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                String transcription = audioRecorder.transcribeAudio(wavFile);
                runOnUiThread(() -> handleUserInput(transcription));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideThinkingAnimation();
                    Toast.makeText(this, "Transcription error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void updateRecordingUI(boolean recording) {
        runOnUiThread(() -> {
            if (recording) {
                // Morph to recording state
                crossfadeViews(recordButton, stopRecordingButton, 200);

                // Change card color
                recordButtonCard.setCardBackgroundColor(getColor(R.color.error));

                // Show visualizer
                audioVisualizer.setVisibility(View.VISIBLE);
                audioVisualizer.setAlpha(0f);
                audioVisualizer.animate().alpha(1f).setDuration(300).start();
                animationHandler.post(visualizerAnimationRunnable);

                // Start pulse animation
                pulseContainer.setVisibility(View.VISIBLE);
                View pulseCircle = findViewById(R.id.pulse_circle);
                Animation pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);
                pulseCircle.startAnimation(pulseAnim);

            } else {
                // Morph back to ready state
                crossfadeViews(stopRecordingButton, recordButton, 200);

                // Reset card color
                recordButtonCard.setCardBackgroundColor(getColor(R.color.colorPrimary));

                // Hide visualizer
                audioVisualizer.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            audioVisualizer.setVisibility(View.GONE);
                            animationHandler.removeCallbacks(visualizerAnimationRunnable);
                        })
                        .start();

                // Stop pulse animation
                pulseContainer.clearAnimation();
                pulseContainer.setVisibility(View.GONE);
            }
        });
    }

    private void crossfadeViews(View fadeOut, View fadeIn, long duration) {
        fadeOut.animate()
                .alpha(0f)
                .setDuration(duration)
                .withEndAction(() -> fadeOut.setVisibility(View.GONE))
                .start();

        fadeIn.setVisibility(View.VISIBLE);
        fadeIn.setAlpha(0f);
        fadeIn.animate()
                .alpha(1f)
                .setDuration(duration)
                .start();
    }

    private void resetRecordingState() {
        isRecording = false;
        updateRecordingUI(false);
        stopRecordingTimer();
        hideListeningAnimation();
    }

    private void startRecordingTimer() {
        animationHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    long elapsed = System.currentTimeMillis() - recordingStartTime;
                    int seconds = (int) (elapsed / 1000) % 60;
                    int minutes = (int) (elapsed / 60000);
                    recordingTimer.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
                    animationHandler.postDelayed(this, 500);
                }
            }
        });
    }

    private void stopRecordingTimer() {
        recordingTimer.setText("00:00");
    }

    private void showListeningAnimation() {
        listeningOverlay.setVisibility(View.VISIBLE);
        listeningOverlay.setAlpha(0f);
        listeningOverlay.animate().alpha(1f).setDuration(300).start();
    }

    private void hideListeningAnimation() {
        listeningOverlay.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> listeningOverlay.setVisibility(View.GONE))
                .start();
    }

    private void showThinkingAnimation() {
        typingIndicator.setVisibility(View.VISIBLE);
        typingIndicator.setAlpha(0f);
        typingIndicator.animate().alpha(1f).setDuration(200).start();
        animationHandler.post(typingAnimationRunnable);
    }

    private void hideThinkingAnimation() {
        animationHandler.removeCallbacks(typingAnimationRunnable);
        typingIndicator.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> typingIndicator.setVisibility(View.GONE))
                .start();
    }

    private void showLoadingOverlay() {
        loadingOverlay.setVisibility(View.VISIBLE);
        loadingOverlay.setAlpha(0f);
        loadingOverlay.animate().alpha(1f).setDuration(200).start();
    }

    private void hideLoadingOverlay() {
        loadingOverlay.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> loadingOverlay.setVisibility(View.GONE))
                .start();
    }

    private void animateTextChange(TextView textView, String newText) {
        textView.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    textView.setText(newText);
                    textView.animate()
                            .alpha(1f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }

    private void handleUserInput(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return;
        }

        hideListeningAnimation();

        // Check for exit commands
        if (isExitCommand(userInput)) {
            exitConversation();
            return;
        }

        // Analyze English with NLP
        EnglishAnalyzer.AnalysisResult analysisResult = englishAnalyzer.analyze(userInput);

        // Add to conversation history
        chatHistory.append("You: ").append(userInput).append("\n");

        // Show thinking animation
        roleSubtitle.setText("Thinking...");
        showThinkingAnimation();

        // Generate AI response
        generateAIResponse(userInput, analysisResult);
    }

    private boolean isExitCommand(String input) {
        String lowerInput = input.toLowerCase().trim();
        return lowerInput.equals("exit") ||
                lowerInput.equals("goodbye") ||
                lowerInput.equals("bye") ||
                lowerInput.equals("that's all") ||
                lowerInput.equals("quit") ||
                lowerInput.contains("end conversation");
    }

    private void generateAIResponse(String userInput, EnglishAnalyzer.AnalysisResult analysisResult) {
        new Thread(() -> {
            try {
                // Call Groq AI
                String response = callGroqAI(userInput, analysisResult);

                // Fallback if Groq fails
                if (response.startsWith("ERR:")) {
                    response = generateSmartFallback(userInput);
                }

                final String finalResponse = response;

                // Save to database
                if (currentUserId != null) {
                    learningProgressDb.saveAnalysis(currentUserId, userInput, finalResponse, analysisResult);
                }

                // Display and speak response
                runOnUiThread(() -> {
                    hideThinkingAnimation();
                    roleSubtitle.setText("Ready to chat");
                    chatHistory.append("Assistant: ").append(finalResponse).append("\n");
                    animateTextChange(botResponseView, finalResponse);
                    textToSpeech.speak(finalResponse, TextToSpeech.QUEUE_FLUSH, null, null);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideThinkingAnimation();
                    roleSubtitle.setText("Ready to chat");
                    String errorMsg = generateSmartFallback(userInput);
                    animateTextChange(botResponseView, errorMsg);
                    textToSpeech.speak(errorMsg, TextToSpeech.QUEUE_FLUSH, null, null);
                });
            }
        }).start();
    }

    private String callGroqAI(String userInput, EnglishAnalyzer.AnalysisResult analysis) {
        try {
            String apiKey = getString(R.string.groq_api_key);
            if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("YOUR_GROQ_API_KEY_HERE")) {
                return "ERR: Groq API key not configured";
            }

            URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "llama-3.1-8b-instant");

            JSONArray messages = new JSONArray();

            // System message with role and NLP analysis
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", buildSystemPrompt(analysis));
            messages.put(systemMessage);

            // User message with conversation history
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", getRecentHistory() + "\nUser: " + userInput);
            messages.put(userMessage);

            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 250);
            requestBody.put("temperature", 0.7);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.toString().getBytes("UTF-8"));
            }

            if (conn.getResponseCode() == 200) {
                InputStream inputStream = conn.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                StringBuilder response = new StringBuilder();
                char[] buffer = new char[1024];
                int bytesRead;
                while ((bytesRead = reader.read(buffer)) != -1) {
                    response.append(buffer, 0, bytesRead);
                }
                JSONObject jsonResponse = new JSONObject(response.toString());
                String aiResponse = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content").trim();

                return cleanAIResponse(aiResponse);
            }
            return "ERR: Groq API error " + conn.getResponseCode();
        } catch (Exception e) {
            return "ERR: " + e.getMessage();
        }
    }

    private String buildSystemPrompt(EnglishAnalyzer.AnalysisResult analysis) {
        StringBuilder prompt = new StringBuilder();

        // Role-specific personality
        switch (role.toLowerCase()) {
            case "teacher":
                prompt.append("You are a patient, encouraging English teacher. ");
                break;
            case "friend":
                prompt.append("You are a warm, supportive friend. ");
                break;
            case "interviewer":
                prompt.append("You are a professional interviewer. ");
                break;
            case "therapist":
                prompt.append("You are an empathetic therapist. ");
                break;
            case "coach":
                prompt.append("You are a motivational coach. ");
                break;
            default:
                prompt.append("You are a helpful assistant. ");
        }

        // Add NLP analysis context
        prompt.append("\n\nUser's English Level:\n");
        prompt.append("- Grammar Score: ").append(analysis.overallScore).append("/100\n");
        prompt.append("- Vocabulary: ").append(analysis.level).append("\n");

        if (!analysis.weaknesses.isEmpty()) {
            prompt.append("- Issues detected: ").append(String.join(", ", analysis.weaknesses)).append("\n");
        }

        // Conversation guidelines
        prompt.append("\nGuidelines:\n");
        prompt.append("1. Respond naturally in 2-4 sentences\n");
        prompt.append("2. If grammar errors detected, gently correct by example\n");
        prompt.append("3. Ask ONE relevant follow-up question\n");
        prompt.append("4. Stay in character as ").append(role).append("\n");
        prompt.append("5. Be encouraging and authentic\n");

        return prompt.toString();
    }

    private String getRecentHistory() {
        String history = chatHistory.toString();
        if (history.length() > 600) {
            return history.substring(history.length() - 600);
        }
        return history.isEmpty() ? "[Conversation start]" : history;
    }

    private String cleanAIResponse(String response) {
        // Remove markdown and formatting
        response = response.replaceAll("\\*\\*", "");
        response = response.replaceAll("\\*", "");
        response = response.replaceAll("```", "");
        response = response.replaceAll("(?i)^(Assistant|AI|Bot|Teacher|Friend|Interviewer|Therapist|Coach):\\s*", "");
        return response.trim();
    }

    private String generateSmartFallback(String userInput) {
        String[] responses;

        switch (role.toLowerCase()) {
            case "teacher":
                responses = new String[]{
                        "That's interesting. Could you tell me more?",
                        "I appreciate you sharing that. What else would you like to discuss?",
                        "Good point! Let me think about how to help you with that."
                };
                break;
            case "friend":
                responses = new String[]{
                        "Oh, that's cool! Tell me more.",
                        "I hear you. What's your take on that?",
                        "Interesting! What made you think of that?"
                };
                break;
            case "interviewer":
                responses = new String[]{
                        "Thank you. Can you elaborate on your experience?",
                        "That's good. What challenges did you face?",
                        "I see. How would you approach that now?"
                };
                break;
            case "therapist":
                responses = new String[]{
                        "I'm here to listen. How does that make you feel?",
                        "Thank you for sharing. What else is on your mind?",
                        "That sounds important. Would you like to explore that?"
                };
                break;
            case "coach":
                responses = new String[]{
                        "Great insight! What's your next step?",
                        "I like your thinking. What's holding you back?",
                        "Excellent! How can you apply that?"
                };
                break;
            default:
                responses = new String[]{
                        "That's interesting. Tell me more.",
                        "I'd love to hear more about that.",
                        "What else would you like to discuss?"
                };
        }

        return responses[(int) (Math.random() * responses.length)];
    }

    private void exitConversation() {
        String exitMessage = generateExitMessage();
        chatHistory.append("Assistant: ").append(exitMessage).append("\n");

        isExiting = true;
        recordButton.setEnabled(false);
        stopRecordingButton.setEnabled(false);
        exitButton.setEnabled(false);
        progressButton.setEnabled(false);

        animateTextChange(botResponseView, exitMessage);
        textToSpeech.speak(exitMessage, TextToSpeech.QUEUE_FLUSH, null, null);

        // Animate exit
        animateExit();

        new android.os.Handler().postDelayed(this::finish, 2500);
    }

    private void animateExit() {
        // Fade out controls
        View controlsContainer = findViewById(R.id.controls_container);
        controlsContainer.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(400)
                .start();

        // Fade out conversation card
        conversationCard.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setStartDelay(200)
                .setDuration(400)
                .start();

        // Fade out header
        headerCard.animate()
                .alpha(0f)
                .translationY(-100f)
                .setStartDelay(400)
                .setDuration(400)
                .start();
    }

    private String generateExitMessage() {
        switch (role.toLowerCase()) {
            case "teacher":
                return "Great job today! Keep practicing. Goodbye!";
            case "friend":
                return "It was awesome chatting! Talk soon!";
            case "interviewer":
                return "Thank you for your time. We'll be in touch!";
            case "therapist":
                return "Thank you for sharing. Take care!";
            case "coach":
                return "Excellent session! Keep going!";
            default:
                return "Thank you. See you later!";
        }
    }

    private void checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            audioPermissionGranted = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            audioPermissionGranted = grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!audioPermissionGranted) {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String mapSpeechError(int code) {
        switch (code) {
            case SpeechRecognizer.ERROR_AUDIO: return "Audio error";
            case SpeechRecognizer.ERROR_NETWORK: return "Network error";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No speech recognized";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech input";
            default: return "Error code: " + code;
        }
    }

    private void showSpeechServiceInstallDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Enable Speech Recognition")
                .setMessage("Install 'Speech Services by Google' for voice input.")
                .setPositiveButton("Install", (d, which) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=com.google.android.tts")));
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.tts")));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        // ===== GAMIFICATION: Award XP for conversation session =====
        awardConversationXP();

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (learningProgressDb != null) {
            learningProgressDb.close();
        }
        animationHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    /**
     * Award XP and coins for completing a conversation session.
     * Also checks if this completes a conversation-type mission.
     */
    private void awardConversationXP() {
        try {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String uid = prefs.getString("UID", null);
            if (uid == null || uid.trim().isEmpty()) return;

            GamificationEngine engine = new GamificationEngine(this);
            MissionManager missions = new MissionManager(this);

            // Award conversation XP (fire-and-forget since we're in onDestroy)
            engine.awardXPQuiet(uid, GamificationEngine.XP_CONVERSATION,
                    GamificationEngine.COINS_CONVERSATION, "ai_conversation");

            // Check if this completes a conversation mission
            missions.checkConversationMissionCompletion(uid, engine, (completed, mission) -> {
                // Mission completion is handled silently here since activity is being destroyed
                android.util.Log.d("SelectRole", "Conversation mission check: " + completed);
            });

            // Record adaptive score for conversation (using last known score if available)
            AdaptiveEngine adaptive = new AdaptiveEngine();
            adaptive.recordScore(uid, "conversation", 70); // Default moderate score
        } catch (Exception e) {
            android.util.Log.e("SelectRole", "Error awarding conversation XP", e);
        }
    }

    private void recordStreakActivity() {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String uid = sharedPreferences.getString("UID", null);

            if (uid != null && !uid.trim().isEmpty()) {
                StreakHelper streakHelper = new StreakHelper(this);
                streakHelper.recordDailyActivity(uid, new StreakHelper.StreakCallback() {
                    @Override
                    public void onStreakUpdated(int currentStreak, int longestStreak, int totalDays) {
                        runOnUiThread(() -> {
                            streakCount.setText(String.valueOf(currentStreak));
                            animateStreakBadge();
                        });
                    }

                    @Override
                    public void onStreakMilestone(int streakCount, String milestone) {
                        runOnUiThread(() -> Toast.makeText(SelectRole.this,
                                milestone, Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onError(String error) {
                        android.util.Log.e("SelectRole", "Streak error: " + error);
                    }
                });
            }
        } catch (Exception e) {
            android.util.Log.e("SelectRole", "Error in streak tracking", e);
        }
    }
}