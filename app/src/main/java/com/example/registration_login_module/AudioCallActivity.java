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

        private static final String AGORA_APP_ID = null; // moved to strings.xml


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
        private TextView contactName;
        private TextView callType;
        private android.widget.ImageView avatar;
        private android.widget.ImageView avatarPulse;

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
            contactName = findViewById(R.id.contactName);
            callType = findViewById(R.id.callType);
            avatar = findViewById(R.id.avatar);
            avatarPulse = findViewById(R.id.avatarPulse);

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
                // Hide initial UI elements
                startBtn.setVisibility(View.GONE);
                waitingText.setVisibility(View.GONE);
                progress.setVisibility(View.GONE);

                // Show call header with animation
                callHeader.setVisibility(View.VISIBLE);
                callHeader.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.call_fade_in));

                // Show call controls with slide animation
                callControls.setVisibility(View.VISIBLE);
                callControls.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.call_slide_in));

                // Start avatar pulse animation
                if (avatarPulse != null) {
                    avatarPulse.setVisibility(View.VISIBLE);
                    avatarPulse.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.avatar_pulse));
                }

                // Update status to "Calling..."
                if (connStatus != null) {
                    connStatus.setText("Calling...");
                }
            }
        }

        private void transitionToConnected() {
            if (callTimer != null && connStatus != null) {
                // Show timer and update status
                callTimer.setVisibility(View.VISIBLE);
                callTimer.setBase(android.os.SystemClock.elapsedRealtime());
                callTimer.start();

                // Update connection status
                connStatus.setText("Connected");
                connStatus.setTextColor(getResources().getColor(R.color.success));

                // Stop avatar pulse animation when connected
                if (avatarPulse != null) {
                    avatarPulse.clearAnimation();
                    avatarPulse.setVisibility(View.GONE);
                }
            }
        }

        private void simulateSuccessfulConnection() {
            // For development/testing when token fails, simulate a successful connection
            new android.os.Handler().postDelayed(() -> {
                joined = true;
                progress.setVisibility(View.GONE);
                status.setText("Connected (Test Mode)");
                startInCallUI();
                // Transition to connected state after a short delay
                new android.os.Handler().postDelayed(() -> transitionToConnected(), 1000);
            }, 2000); // 2 second delay to show the error message
        }

        private void ensureAudioIsWorking() {
            // Additional audio setup to ensure voice communication works
            if (rtcEngine != null) {
                try {
                    // Double-check audio is enabled and unmuted
                    rtcEngine.muteLocalAudioStream(false);
                    rtcEngine.muteAllRemoteAudioStreams(false);

                    // Ensure proper audio routing
                    rtcEngine.setEnableSpeakerphone(speakerOn);

                    // Set volumes to maximum
                    rtcEngine.adjustPlaybackSignalVolume(100);
                    rtcEngine.adjustRecordingSignalVolume(100);

                    status.setText("Audio configured - Ready for voice");
                } catch (Throwable e) {
                    status.setText("Audio setup error: " + e.getMessage());
                }
            }
        }

        private FirebaseFirestore db;
        private DocumentReference lobbyDoc;
        private ListenerRegistration lobbyListener;
        private String channel;
        private String token;
        private int localUid = 0; // Let Agora assign if 0
        private AgoraTokenService tokenService = new AgoraTokenService();
        private ChannelMediaOptions joinOptions; // keep last options for retries

        private void startMatchmaking() {
            acquireAudioFocus();
            startBtn.setEnabled(false);
            waitingText.setVisibility(View.VISIBLE);
            progress.setVisibility(View.VISIBLE);
            channel = getString(R.string.agora_channel);
            token = getIntent().getStringExtra("TOKEN");
            
            // Skip Firebase lobby — connect directly to Agora channel
            // Both users will join the same fixed channel "smarttalk"
            waitingText.setText("Connecting to voice channel...");
            status.setText("Channel: " + channel);
            ensureTokenThenJoin();
        }

        private void listenForMatch() {
            // Kept for backwards compatibility but no longer used
            waitingText.setText("Waiting for another user to join...");
        }

        private void ensureTokenThenJoin() {
            String tokenServer = getString(R.string.agora_token_server_base);
            try {
                int configuredUid = Integer.parseInt(getString(R.string.agora_token_uid));
                if (configuredUid >= 0) {
                    localUid = configuredUid;
                }
            } catch (Throwable ignored) {}
            
            // If a non-empty token is already provided via Intent, prefer that
            if (this.token != null && !this.token.isEmpty() && !"null".equals(this.token)) {
                initAndJoin();
                return;
            }
            // If token server is configured, fetch runtime token
            if (tokenServer != null && !tokenServer.trim().isEmpty()) {
                waitingText.setText("Fetching token...");
                tokenService.fetchTokenAsync(tokenServer, channel, localUid, new AgoraTokenService.TokenCallback() {
                    @Override
                    public void onSuccess(String token) {
                        AudioCallActivity.this.token = token;
                        initAndJoin();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        String fallback = getString(R.string.agora_temp_token);
                        if (fallback != null && !fallback.isEmpty()) {
                            status.setText("Token fetch failed, using temp token...");
                            AudioCallActivity.this.token = fallback;
                            initAndJoin();
                        } else {
                            status.setText("Token fetch failed: " + errorMessage);
                            progress.setVisibility(View.GONE);
                        }
                    }
                });
            } else {
                // No token server → try temp token first, else join without token
                String temp = getString(R.string.agora_temp_token);
                if (temp != null && !temp.isEmpty()) {
                    this.token = temp;
                } else {
                    this.token = null;
                }
                initAndJoin();
            }
        }

        private int joinRetryCount = 0;
        private static final int MAX_JOIN_RETRIES = 2;
        private int lastErrorCode = -1;

        private void initAndJoin() {
            joinRetryCount = 0;
            lastErrorCode = -1;
            try {
                RtcEngineConfig config = new RtcEngineConfig();
                config.mContext = getApplicationContext();
                // Read App ID from resources so it matches tokens/server
                config.mAppId = getString(R.string.agora_app_id);
                config.mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
                config.mEventHandler = new IRtcEngineEventHandler() {
                    @Override
                    public void onConnectionStateChanged(int state, int reason) {
                        runOnUiThread(() -> {
                            // Surface state transitions for debugging
                            switch (state) {
                                case Constants.CONNECTION_STATE_CONNECTING:
                                    status.setText("Connecting to voice channel...");
                                    break;
                                case Constants.CONNECTION_STATE_CONNECTED:
                                    status.setText("Channel connected, waiting for audio...");
                                    break;
                                case Constants.CONNECTION_STATE_RECONNECTING:
                                    status.setText("Reconnecting...");
                                    break;
                                case Constants.CONNECTION_STATE_FAILED:
                                    status.setText("Connection failed (reason: " + reason + ")");
                                    progress.setVisibility(View.GONE);
                                    startBtn.setEnabled(true);
                                    break;
                            }
                        });
                    }
                    @Override
                    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                        runOnUiThread(() -> {
                            joined = true;
                            progress.setVisibility(View.GONE);
                            status.setText("Connected (UID: " + uid + ")");
                            startInCallUI();

                            // Ensure audio is properly configured
                            ensureAudioIsWorking();

                            // Transition to connected state after a short delay
                            new android.os.Handler().postDelayed(() -> transitionToConnected(), 1000);
                        });
                    }

                    @Override
                    public void onUserJoined(int uid, int elapsed) {
                        runOnUiThread(() -> {
                            status.setText("Partner joined! (UID: " + uid + ")");
                            transitionToConnected();
                        });
                    }

                    @Override
                    public void onUserOffline(int uid, int reason) {
                        runOnUiThread(() -> status.setText("Partner left the call"));
                    }

                    @Override
                    public void onError(int err) {
                        runOnUiThread(() -> {
                            lastErrorCode = err;
                            android.util.Log.e("AudioCall", "Agora error: " + err + ", retries: " + joinRetryCount);
                            // Prevent infinite retry loop
                            if (joinRetryCount >= MAX_JOIN_RETRIES) {
                                status.setText("Connection failed (error " + err + "). Check Agora Console: App ID may be expired or App Certificate is blocking.");
                                progress.setVisibility(View.GONE);
                                startBtn.setEnabled(true);
                                return;
                            }

                            // Agora error codes: 109/110 = token issues, 17 = already joined
                            if (err == 109 || err == 110) {
                                joinRetryCount++;
                                status.setText("Token error " + err + ". Retrying without token (attempt " + joinRetryCount + ")...");
                                try {
                                    if (rtcEngine != null) {
                                        try { rtcEngine.leaveChannel(); } catch (Throwable ignored) {}
                                        // Retry without token — works if App Certificate is disabled
                                        AudioCallActivity.this.token = null;
                                        rtcEngine.joinChannel(null, channel, localUid, (joinOptions != null ? joinOptions : new ChannelMediaOptions()));
                                    }
                                } catch (Throwable t) {
                                    status.setText("Retry failed: " + t.getMessage());
                                    progress.setVisibility(View.GONE);
                                    startBtn.setEnabled(true);
                                }
                            } else if (err == 17) {
                                // Already joined — this is fine, ignore
                                status.setText("Already in channel");
                            } else {
                                status.setText("Agora error: " + err);
                            }
                        });
                    }

                    @Override
                    public void onFirstLocalAudioFramePublished(int elapsed) {
                        runOnUiThread(() -> status.setText("Your mic is live"));
                    }

                    @Override
                    public void onFirstRemoteAudioFrame(int uid, int elapsed) {
                        runOnUiThread(() -> {
                            status.setText("Voice connected!");
                            // This indicates the call is fully connected
                            transitionToConnected();
                        });
                    }

                    @Override
                    public void onTokenPrivilegeWillExpire(String token) {
                        runOnUiThread(() -> {
                            status.setText("Token expiring. Renewing...");
                            renewTokenAndContinue();
                        });
                    }
                };
                rtcEngine = RtcEngine.create(config);

                // Enable audio and configure for voice calls
                rtcEngine.enableAudio();
                rtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_SPEECH_STANDARD);
                rtcEngine.muteLocalVideoStream(true);

                // Configure audio routing - let Agora handle it
                try {
                    rtcEngine.setDefaultAudioRoutetoSpeakerphone(false); // Start with earpiece
                    rtcEngine.setEnableSpeakerphone(false); // Start with earpiece
                } catch (Throwable ignored) {}

                // Ensure local audio is unmuted and remote audio is enabled
                try {
                    rtcEngine.muteLocalAudioStream(false); // Unmute local microphone
                } catch (Throwable ignored) {}

                try {
                    rtcEngine.muteAllRemoteAudioStreams(false); // Enable remote audio
                } catch (Throwable ignored) {}

                // Set proper audio volumes
                try {
                    rtcEngine.adjustPlaybackSignalVolume(100); // Max playback volume
                } catch (Throwable ignored) {}

                try {
                    rtcEngine.adjustRecordingSignalVolume(100); // Max recording volume
                } catch (Throwable ignored) {}

                ChannelMediaOptions options = new ChannelMediaOptions();
                options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
                options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
                // Ensure microphone is published and remote audio subscribed
                options.publishMicrophoneTrack = true;
                options.autoSubscribeAudio = true;
                // Compatibility flags for 4.x
                options.publishCameraTrack = false;
                options.enableAudioRecordingOrPlayout = true;
                // store for retry
                joinOptions = options;

                progress.setVisibility(View.VISIBLE);
                status.setText("Joining channel '" + channel + "'...");

                String joinToken = this.token;

                // Strategy: try with token first; if it fails, onError will retry without token
                android.util.Log.d("AudioCall", "Joining channel=" + channel + " uid=" + localUid + " hasToken=" + (joinToken != null && !joinToken.isEmpty()));
                if (joinToken != null && !joinToken.isEmpty() && !"null".equals(joinToken)) {
                    rtcEngine.joinChannel(joinToken, channel, localUid, options);
                } else {
                    // Join without token (works if App Certificate is disabled in Agora Console)
                    rtcEngine.joinChannel(null, channel, localUid, options);
                }

                // Fail-safe: if not connected within 15s, surface timeout hint with diagnostic info
                new android.os.Handler().postDelayed(() -> {
                    if (!joined) {
                        String errInfo = lastErrorCode > 0 ? " (last error: " + lastErrorCode + ")" : " (no error callback — check network/App ID)";
                        status.setText("Timeout" + errInfo + ". AppID=" + getString(R.string.agora_app_id).substring(0, 8) + "... Ch='" + channel + "'");
                        progress.setVisibility(View.GONE);
                        startBtn.setEnabled(true);
                        waitingText.setText("Tap Start to retry. Check Agora Console for expired project.");
                    }
                }, 15000);
            } catch (Exception e) {
                status.setText("Init failed: " + e.getMessage());
                progress.setVisibility(View.GONE);
                startBtn.setEnabled(true);
            }
        }

        private void renewTokenAndContinue() {
            String tokenServer = getString(R.string.agora_token_server_base);
            if (rtcEngine == null) return;
            if (tokenServer != null && !tokenServer.trim().isEmpty()) {
                tokenService.fetchTokenAsync(tokenServer, channel, localUid, new AgoraTokenService.TokenCallback() {
                    @Override
                    public void onSuccess(String token) {
                        AudioCallActivity.this.token = token;
                        try { rtcEngine.renewToken(token); } catch (Throwable ignored) {}
                        status.setText("Token renewed");
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        status.setText("Token renew failed: " + errorMessage);
                    }
                });
            } else {
                // No server configured → try renewing with temp token (if provided)
                String temp = getString(R.string.agora_temp_token);
                if (temp != null && !temp.isEmpty()) {
                    try { rtcEngine.renewToken(temp); } catch (Throwable ignored) {}
                    status.setText("Token renewed (temp)");
                } else {
                    status.setText("Set token server URL or provide temp token");
                }
            }
        }

        private void toggleMute() {
            muted = !muted;
            if (rtcEngine != null) {
                rtcEngine.muteLocalAudioStream(muted);
            }
            muteBtn.setSelected(muted);

            // Update button appearance based on mute state
            if (muted) {
                muteBtn.setBackgroundResource(R.drawable.call_control_bg_muted);
                muteBtn.setImageResource(R.drawable.ic_mic_off);
            } else {
                muteBtn.setBackgroundResource(R.drawable.call_control_bg);
                muteBtn.setImageResource(R.drawable.ic_mic);
            }
        }

        private boolean speakerOn = false; // Start with earpiece
        private void toggleSpeaker() {
            speakerOn = !speakerOn;

            // Use Agora's speaker control (more reliable for voice calls)
            if (rtcEngine != null) {
                try {
                    rtcEngine.setEnableSpeakerphone(speakerOn);
                    // Also update Android AudioManager for consistency
                    if (audioManager != null) {
                        audioManager.setSpeakerphoneOn(speakerOn);
                    }
                } catch (Throwable e) {
                    // Fallback to AudioManager only
                    if (audioManager != null) {
                        try { audioManager.setSpeakerphoneOn(speakerOn); } catch (Throwable ignored) {}
                    }
                }
            }

            speakerBtn.setSelected(speakerOn);

            // Update button appearance based on speaker state
            if (speakerOn) {
                speakerBtn.setBackgroundResource(R.drawable.call_control_bg);
                speakerBtn.setImageResource(R.drawable.ic_speaker);
            } else {
                speakerBtn.setBackgroundResource(R.drawable.call_control_bg_muted);
                speakerBtn.setImageResource(R.drawable.ic_speaker_off);
            }
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
                    // Set audio mode for communication
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

                    // Request audio focus for voice calls
                    int result = audioManager.requestAudioFocus(
                        focusChangeListener,
                        AudioManager.STREAM_VOICE_CALL,
                        AudioManager.AUDIOFOCUS_GAIN
                    );

                    // Don't force speakerphone - let user control it
                    // audioManager.setSpeakerphoneOn(false); // Start with earpiece

                    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        // Audio focus granted successfully
                    }
                } catch (Throwable e) {
                    // Log error but don't crash
                }
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
                // Resume normal flow by starting matchmaking, which prepares channel/token
                startMatchmaking();
            } else {
                status.setText("Mic permission required");
            }
        }
    }
