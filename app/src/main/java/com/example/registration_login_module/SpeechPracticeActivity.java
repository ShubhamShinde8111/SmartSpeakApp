package com.example.registration_login_module;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Locale;

public class SpeechPracticeActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int SPEECH_REQUEST_CODE = 101;

    private TextView tvTargetSentence, tvStatus, tvResultScore, tvFeedback;
    private FloatingActionButton fabRecord;
    private LinearProgressIndicator recordingProgress;
    private View resultCard;

    private SpeechRecognizer speechRecognizer;
    private boolean isRecording = false;
    private SpeechAnalyticsHelper analyticsHelper;
    private boolean isSpeechServiceAvailable = false;

    private String[] sentences = {
            "The quick brown fox jumps over the lazy dog.",
            "Practice makes a man perfect.",
            "English is a global language of communication.",
            "Technology is changing the way we live and work.",
            "Consistency is the key to mastering any skill."
    };
    private int currentSentenceIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_practice);

        tvTargetSentence = findViewById(R.id.tvTargetSentence);
        tvStatus = findViewById(R.id.tvStatus);
        tvResultScore = findViewById(R.id.tvResultScore);
        tvFeedback = findViewById(R.id.tvFeedback);
        fabRecord = findViewById(R.id.fabRecord);
        recordingProgress = findViewById(R.id.recordingProgress);
        resultCard = findViewById(R.id.resultCard);

        analyticsHelper = new SpeechAnalyticsHelper(this);

        setupToolbar();
        updateSentence();

        findViewById(R.id.btnNextSentence).setOnClickListener(v -> {
            currentSentenceIndex = (currentSentenceIndex + 1) % sentences.length;
            updateSentence();
            resultCard.setVisibility(View.GONE);
        });

        // Check for Speech Service availability
        isSpeechServiceAvailable = SpeechRecognizer.isRecognitionAvailable(this);
        if (isSpeechServiceAvailable) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            setupSpeechRecognizer();
        } else {
            // We don't disable the button anymore; we'll use the Intent fallback
            tvStatus.setText("Tap to start (System Dialog)");
        }

        fabRecord.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                checkPermissionAndStart();
            }
        });
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void updateSentence() {
        tvTargetSentence.setText(sentences[currentSentenceIndex]);
    }

    private void setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                tvStatus.setText("Listening...");
                recordingProgress.setVisibility(View.VISIBLE);
                recordingProgress.setIndeterminate(true);
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                tvStatus.setText("Processing...");
                recordingProgress.setIndeterminate(false);
            }

            @Override
            public void onError(int error) {
                String message;
                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        message = "Audio recording error";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        message = "Client side error";
                        break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        message = "Insufficient permissions";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        message = "Network error";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        message = "Network timeout";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        message = "No match found";
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        message = "Recognition Service busy";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        message = "Server error";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        message = "No speech input";
                        break;
                    default:
                        message = "Error: " + error;
                        break;
                }
                tvStatus.setText("Tap to start recording");
                recordingProgress.setVisibility(View.INVISIBLE);
                isRecording = false;
                fabRecord.setImageResource(R.drawable.ic_mic);
                Toast.makeText(SpeechPracticeActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    processResults(matches.get(0));
                }
                resetUI();
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    tvStatus.setText(matches.get(0));
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });
    }

    private void resetUI() {
        isRecording = false;
        fabRecord.setImageResource(R.drawable.ic_mic);
        recordingProgress.setVisibility(View.INVISIBLE);
        tvStatus.setText("Tap to start recording");
    }

    private void checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.RECORD_AUDIO },
                    PERMISSION_REQUEST_CODE);
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak the sentence clearly");

        if (isSpeechServiceAvailable && speechRecognizer != null) {
            // Use background service
            try {
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
                speechRecognizer.startListening(intent);
                isRecording = true;
                fabRecord.setImageResource(R.drawable.ic_mic_off);
                resultCard.setVisibility(View.GONE);
                tvStatus.setText("Initializing...");
            } catch (Exception e) {
                // Fallback to intent if background service fails
                startActivityForResult(intent, SPEECH_REQUEST_CODE);
            }
        } else {
            // Fallback: Launch the system speech dialog
            try {
                startActivityForResult(intent, SPEECH_REQUEST_CODE);
            } catch (Exception e) {
                Toast.makeText(this, "No speech app found on this device", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                processResults(results.get(0));
            }
        }
    }

    private void stopRecording() {
        if (isSpeechServiceAvailable && speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
        isRecording = false;
        fabRecord.setImageResource(R.drawable.ic_mic);
        tvStatus.setText("Processing...");
        recordingProgress.setVisibility(View.INVISIBLE);
    }

    private void processResults(String spokenText) {
        String targetText = tvTargetSentence.getText().toString();
        int score = calculateSimilarity(targetText.toLowerCase(), spokenText.toLowerCase());

        tvResultScore.setText("Accuracy: " + score + "%");
        tvFeedback.setText("You said: \"" + spokenText + "\"");
        resultCard.setVisibility(View.VISIBLE);
        tvStatus.setText("Tap to start recording");

        String userId = FirebaseAuth.getInstance().getUid();
        if (userId != null) {
            analyticsHelper.recordAnalysis(userId, "Speech Practice", targetText, score, null, null);

            // ===== GAMIFICATION: Award XP & check mission =====
            GamificationEngine engine = new GamificationEngine(this);
            MissionManager missions = new MissionManager(this);
            AdaptiveEngine adaptive = new AdaptiveEngine();

            // 1. Award XP
            engine.awardXPQuiet(userId, GamificationEngine.XP_SPEECH_PRACTICE,
                    GamificationEngine.COINS_SPEECH, "speech_practice");
            Toast.makeText(this, "+" + GamificationEngine.XP_SPEECH_PRACTICE + " XP", Toast.LENGTH_SHORT).show();

            // 2. Record adaptive score
            adaptive.recordScore(userId, "speech", score);

            // 3. Check mission completion
            missions.checkSpeechMissionCompletion(userId, engine, (completed, mission) -> {
                if (completed && mission != null) {
                    runOnUiThread(() -> Toast.makeText(SpeechPracticeActivity.this,
                            "🎯 Mission Complete: " + mission.title, Toast.LENGTH_LONG).show());
                }
            });
        }
    }

    private int calculateSimilarity(String s1, String s2) {
        String[] words1 = s1.split("\\s+");
        String[] words2 = s2.split("\\s+");
        int matches = 0;
        for (String w1 : words1) {
            for (String w2 : words2) {
                if (w1.replaceAll("[^a-zA-Z]", "").equals(w2.replaceAll("[^a-zA-Z]", ""))) {
                    matches++;
                    break;
                }
            }
        }
        return (int) ((double) matches / words1.length * 100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}
