package com.example.registration_login_module;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.ClientRoleOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

public class AudioCallActivity extends AppCompatActivity {

    private static final int REQ_AUDIO = 201;

    private static final String AGORA_APP_ID = "609fcb521b5d46399cfcaa456f117fad"; // provided

    private RtcEngine rtcEngine;
    private boolean joined = false;
    private boolean muted = false;

    private ProgressBar progress;
    private TextView status;
    private ImageButton muteBtn;
    private ImageButton endBtn;
    private ImageButton speakerBtn;
    private TextView waitingText;
    private View startBtn;
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener focusChangeListener;
    private android.widget.Chronometer callTimer;
    private View callControls;
    private View callHeader;
    private TextView connStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_call);

        progress = findViewById(R.id.progress);
        status = findViewById(R.id.status);
        muteBtn = findViewById(R.id.muteBtn);
        endBtn = findViewById(R.id.endBtn);
        speakerBtn = findViewById(R.id.speakerBtn);
        waitingText = findViewById(R.id.waitingText);
        startBtn = findViewById(R.id.startBtn);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        focusChangeListener = change -> { /* no-op */ };

        callTimer = findViewById(R.id.callTimer);
        callControls = findViewById(R.id.callControls);
        callHeader = findViewById(R.id.callHeader);
        connStatus = findViewById(R.id.connStatus);

        muteBtn.setOnClickListener(v -> toggleMute());
        endBtn.setOnClickListener(v -> finishCall());
        speakerBtn.setOnClickListener(v -> toggleSpeaker());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
        }

        startBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
                return;
            }
            startMatchmaking();
        });
        status.setText("Tap Start Conversation");
    }

    private void startInCallUI() {
        if (callHeader != null && callControls != null && callTimer != null) {
            callHeader.setVisibility(View.VISIBLE);
            callControls.setVisibility(View.VISIBLE);
            startBtn.setVisibility(View.GONE);
            waitingText.setVisibility(View.GONE);
            callTimer.setBase(android.os.SystemClock.elapsedRealtime());
            callTimer.start();
        }
    }

    private FirebaseFirestore db;
    private DocumentReference lobbyDoc;
    private ListenerRegistration lobbyListener;
    private String channel;
    private String token;

    private void startMatchmaking() {
        acquireAudioFocus();
        startBtn.setEnabled(false);
        waitingText.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);
        channel = getIntent().getStringExtra("CHANNEL");
        if (channel == null || channel.isEmpty()) channel = getString(R.string.agora_channel);
        token = getIntent().getStringExtra("TOKEN");
        db = FirebaseFirestore.getInstance();

        // Simple 2-user lobby: collection "voice_lobby", one doc with field waiting=true and timestamp
        db.collection("voice_lobby")
                .whereEqualTo("waiting", true)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) {
                        // Match with the first waiting user; take over doc and set waiting=false
                        DocumentSnapshot doc = snap.getDocuments().get(0);
                        lobbyDoc = doc.getReference();
                        String docChannel = doc.getString("channel");
                        if (docChannel != null && !docChannel.isEmpty()) {
                            this.channel = docChannel;
                        }
                        lobbyDoc.update("waiting", false)
                                .addOnSuccessListener(v -> {
                                    waitingText.setText("Connecting...");
                                    initAndJoin();
                                })
                                .addOnFailureListener(e -> {
                                    status.setText("Failed to connect");
                                    progress.setVisibility(View.GONE);
                                });
                    } else {
                        // Create our waiting doc and listen for someone to take it
                        lobbyDoc = db.collection("voice_lobby").document();
                        // If a token is supplied, use static channel from strings so token matches.
                        if (token != null && !token.isEmpty()) {
                            this.channel = getString(R.string.agora_channel);
                        } else {
                            // Otherwise use a unique channel id via lobby id
                            this.channel = lobbyDoc.getId();
                        }
                        lobbyDoc.set(new java.util.HashMap<String, Object>() {{
                            put("waiting", true);
                            put("createdAt", FieldValue.serverTimestamp());
                            put("channel", AudioCallActivity.this.channel);
                        }}).addOnSuccessListener(v -> listenForMatch())
                          .addOnFailureListener(e -> {
                              status.setText("Failed to wait");
                              progress.setVisibility(View.GONE);
                          });
                    }
                })
                .addOnFailureListener(e -> {
                    status.setText("Matchmaking error");
                    progress.setVisibility(View.GONE);
                });
    }

    private void listenForMatch() {
        waitingText.setText("Waiting for another user to join...");
        lobbyListener = lobbyDoc.addSnapshotListener((value, error) -> {
            if (error != null || value == null) return;
            Boolean waiting = value.getBoolean("waiting");
            if (waiting != null && !waiting) {
                waitingText.setText("Connecting...");
                initAndJoin();
                if (lobbyListener != null) lobbyListener.remove();
            }
        });
    }

    private void initAndJoin() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = AGORA_APP_ID;
            config.mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
            config.mEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    runOnUiThread(() -> {
                        joined = true;
                        progress.setVisibility(View.GONE);
                        status.setText("Connected");
                        startInCallUI();
                    });
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    runOnUiThread(() -> status.setText("User joined"));
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    runOnUiThread(() -> status.setText("User left"));
                }

                @Override
                public void onError(int err) {
                    runOnUiThread(() -> {
                        // Agora common error codes: 110 often indicates token/app setup issues
                        if (err == 110) {
                            status.setText("Error 110: Token/App setup required. Provide a temp token.");
                        } else {
                            status.setText("Error: " + err);
                        }
                    });
                }

                @Override
                public void onFirstLocalAudioFramePublished(int elapsed) {
                    runOnUiThread(() -> status.setText("Local audio publishing"));
                }

                @Override
                public void onFirstRemoteAudioFrame(int uid, int elapsed) {
                    runOnUiThread(() -> {
                        status.setText("Remote audio received");
                        connStatus.setText("Connected");
                    });
                }
            };
            rtcEngine = RtcEngine.create(config);

            rtcEngine.enableAudio();
            rtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_SPEECH_STANDARD);
            rtcEngine.muteLocalVideoStream(true);
            try { rtcEngine.setDefaultAudioRoutetoSpeakerphone(true); } catch (Throwable ignored) {}
            try { rtcEngine.setEnableSpeakerphone(true); } catch (Throwable ignored) {}
            try { rtcEngine.enableAudioVolumeIndication(500, 3, true); } catch (Throwable ignored) {}
            try { rtcEngine.muteLocalAudioStream(false); } catch (Throwable ignored) {}
            try { rtcEngine.muteAllRemoteAudioStreams(false); } catch (Throwable ignored) {}
            try { rtcEngine.adjustPlaybackSignalVolume(100); } catch (Throwable ignored) {}
            try { rtcEngine.adjustRecordingSignalVolume(100); } catch (Throwable ignored) {}

            ChannelMediaOptions options = new ChannelMediaOptions();
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
            // Ensure microphone is published and remote audio subscribed
            options.publishMicrophoneTrack = true;
            options.autoSubscribeAudio = true;
            // Compatibility flags for 4.x
            options.publishCameraTrack = false;
            options.enableAudioRecordingOrPlayout = true;

            progress.setVisibility(View.VISIBLE);
            status.setText("Connecting...");

            String channel = this.channel;
            String token = this.token; // if null and certificate is enabled, join will fail with 110

            rtcEngine.joinChannel(token, channel, 0, options);
        } catch (Exception e) {
            status.setText("Init failed");
        }
    }

    private void toggleMute() {
        muted = !muted;
        if (rtcEngine != null) {
            rtcEngine.muteLocalAudioStream(muted);
        }
        muteBtn.setSelected(muted);
    }

    private boolean speakerOn = true;
    private void toggleSpeaker() {
        speakerOn = !speakerOn;
        if (audioManager != null) {
            try { audioManager.setSpeakerphoneOn(speakerOn); } catch (Throwable ignored) {}
        }
        if (rtcEngine != null) {
            try { rtcEngine.setEnableSpeakerphone(speakerOn); } catch (Throwable ignored) {}
        }
        speakerBtn.setSelected(speakerOn);
    }

    private void finishCall() {
        if (rtcEngine != null) {
            if (joined) rtcEngine.leaveChannel();
            RtcEngine.destroy();
            rtcEngine = null;
        }
        if (lobbyListener != null) lobbyListener.remove();
        if (lobbyDoc != null) {
            lobbyDoc.delete();
        }
        releaseAudioFocus();
        finish();
    }

    private void acquireAudioFocus() {
        if (audioManager != null) {
            try {
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioManager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN);
                audioManager.setSpeakerphoneOn(true);
            } catch (Throwable ignored) {}
        }
    }

    private void releaseAudioFocus() {
        if (audioManager != null) {
            try {
                audioManager.abandonAudioFocus(focusChangeListener);
                audioManager.setMode(AudioManager.MODE_NORMAL);
                audioManager.setSpeakerphoneOn(false);
            } catch (Throwable ignored) {}
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rtcEngine != null) {
            if (joined) rtcEngine.leaveChannel();
            RtcEngine.destroy();
            rtcEngine = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initAndJoin();
        } else {
            status.setText("Mic permission required");
        }
    }
}
