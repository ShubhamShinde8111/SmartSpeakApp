package com.example.registration_login_module;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private LinearLayout gridLayout;
    private ProgressBar progressBar;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    ImageButton buttonDrawer;
    private DBHelper dbHelper;
    private FirebaseProgressHelper firebaseProgressHelper;
    TextView navUserName, navUserEmail;
    private TextView headerUserName;
    private View progressLineActive;
    private TextView progressPercentage;
    private TextView progressCounter;

    // Progress tracking variables
    private int currentMilestone = 0; // 0-based index
    private int totalMilestones = 4;
    private String currentUserId = null; // Firebase UID
    private String[] milestoneNames = { "Basics", "Intermediate", "Advanced", "Expert" };
    private String[] milestoneStatuses = { "Completed", "Current", "Locked", "Locked" };
    private FirebaseProgressHelper.UserProgress userProgress;

    // Streak tracking variables
    private StreakHelper streakHelper;
    private TextView streakCount, longestStreak, totalDays, streakDescription, nextMilestone;
    private ImageView fireIcon;
    private TextView streakStatus;
    private MaterialCardView streakCard;

    // Gamification variables
    private GamificationEngine gamificationEngine;
    private MissionManager missionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        buttonDrawer = findViewById(R.id.buttonDrawer);
        if (buttonDrawer != null) {
            buttonDrawer.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        View headerView = null;
        if (navigationView != null) {
            headerView = navigationView.getHeaderView(0);
        }

        progressBar = findViewById(R.id.progressBar);
        // progressLineActive = findViewById(R.id.progressLineActive);
        // progressPercentage = findViewById(R.id.progressPercentage);
        // progressCounter = findViewById(R.id.progressCounter);
        headerUserName = findViewById(R.id.welcomeTitle);

        // Find the TextViews in the header
        if (headerView != null) {
            navUserName = headerView.findViewById(R.id.userName);
            navUserEmail = headerView.findViewById(R.id.userEmail);
        }

        // Load profile from Firebase using stored UID
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String uid = sharedPreferences.getString("UID", null);
        if (uid != null) {
            loadProfileFromFirestore(uid);
        }

        // Initialize streak tracking
        // initializeStreakTracking(uid); // Streak feature removed as per request

        // Set up drawer toggle
        // buttonDrawer.setOnClickListener(v ->
        // drawerLayout.openDrawer(GravityCompat.START)); // Moved to top

        // Handle navigation item clicks
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.navProfile) {
                    Toast.makeText(this, "Profile Selected", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                    intent.putExtra("USER_ID", 1); // Example user ID
                    startActivity(intent);
                } else if (itemId == R.id.navProfile) {
                    Intent iNext = new Intent(HomeActivity.this, ProfileActivity.class);
                    startActivity(iNext);
                } else if (itemId == R.id.navChngPassword) {
                    Intent iNext = new Intent(HomeActivity.this, EditProfileActivity.class);
                    startActivity(iNext);
                } else if (itemId == R.id.navFeedback) {
                    Intent iNext = new Intent(HomeActivity.this, FeedbackAcitivity.class);
                    startActivity(iNext);
                } else if (itemId == R.id.navLogout) {
                    SharedPreferences shpf = getSharedPreferences("Login", MODE_PRIVATE);
                    SharedPreferences.Editor editor = shpf.edit();
                    editor.putBoolean("flag", false);
                    editor.apply();
                    Intent iNext = new Intent(HomeActivity.this, LoginActivity.class);
                    iNext.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(iNext);
                    finish();
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });
        }

        // Bottom navigation initialization
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.bringToFront();
            bottomNav.setSelectedItemId(R.id.nav_learn);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_learn) {
                    return true;
                }

                Class<?> destination = null;
                if (itemId == R.id.nav_dictionary)
                    destination = DictionaryActivity.class;
                else if (itemId == R.id.nav_progress)
                    destination = ProgressDashboardActivity.class;
                else if (itemId == R.id.nav_profile)
                    destination = ProfileActivity.class;

                if (destination != null) {
                    startActivity(new Intent(HomeActivity.this, destination));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                    return true;
                }
                return false;
            });

            bottomNav.setOnItemReselectedListener(item -> {
                // Already on Home, no action needed or scroll to top
            });
        }

        // Initialize the GridLayout
        gridLayout = findViewById(R.id.gridLayout);

        // Add entrance animations to cards
        animateCardsOnLoad();

        // Add interactive animations to cards
        addCardClickAnimations();

        // Update stats with animations
        updateStatsWithAnimation();

        // Setting up click listener for Profile CardView
        // Assuming Profile is the first card
        CardView profileVisitCard = findViewById(R.id.profileCard);
        if (profileVisitCard != null) {
            profileVisitCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                    intent.putExtra("USER_ID", 1); // Example user ID
                    startActivity(intent);
                }
            });
        }

        // Access the "About" card view (second card in the GridLayout)

        CardView aboutVisitCard = findViewById(R.id.aboutCard);// About is the second card
        if (aboutVisitCard != null) {
            aboutVisitCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Intent to navigate to the AboutActivity when the card is clicked
                    Intent intent = new Intent(HomeActivity.this, AboutPageAcitivity.class);
                    startActivity(intent); // Start the AboutActivity
                }
            });
        }

        // CardView speakLearnCard = findViewById(R.id.speakLearnCard);
        // speakLearnCard.setOnClickListener(new View.OnClickListener() {
        // @Override
        // public void onClick(View v) {
        // // Intent to navigate to Speak-Learn activity
        // Intent intent = new Intent(HomeActivity.this, SpeakLearnActivity.class);
        // startActivity(intent);
        // }
        // });

        CardView dictionaryCard = findViewById(R.id.dictionaryCard);
        if (dictionaryCard != null) {
            dictionaryCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Intent to navigate to Dictionary activity
                    Intent intent = new Intent(HomeActivity.this, DictionaryActivity.class);
                    startActivity(intent);
                }
            });
        }

        CardView learningPathCard = findViewById(R.id.learningCard);
        if (learningPathCard != null) {
            learningPathCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Intent to navigate to Learning Path activity
                    Intent intent = new Intent(HomeActivity.this, LearningActivity.class);
                    startActivity(intent);
                }
            });
        }

        CardView feedbackCard = findViewById(R.id.feedbackCard);
        if (feedbackCard != null) {
            feedbackCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Intent to navigate to Feedback activity
                    Intent intent = new Intent(HomeActivity.this, FeedbackAcitivity.class);
                    startActivity(intent);
                }
            });
        }

        CardView aispeakCard = findViewById(R.id.speakLearnCard);
        if (aispeakCard != null) {
            aispeakCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            });
        }

        CardView callCard = findViewById(R.id.callCard);
        if (callCard != null) {
            callCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, AudioCallActivity.class);
                    intent.putExtra("CHANNEL", getString(R.string.agora_channel));
                    intent.putExtra("TOKEN", getString(R.string.agora_temp_token));
                    startActivity(intent);
                }
            });
        }

        LinearLayout missionHubCard = findViewById(R.id.missionHubCard);
        if (missionHubCard != null) {
            missionHubCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, MissionHubActivity.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            });
        }

        // Initialize database helper
        dbHelper = new DBHelper(this);

        // Initialize Firebase progress helper
        firebaseProgressHelper = new FirebaseProgressHelper();

        // Get current user ID from SharedPreferences (set during login)
        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = userPrefs.getString("UID", null);

        // Initialize dynamic progress tracker
        // initializeProgressTracker(); // Progress tracker removed

        // Add other listeners as needed
        // updateProgressUI(currentMilestone); // Progress tracker removed

        // Add test functionality for progress tracker (remove in production)
        // addProgressTrackerTestFunctionality(); // Progress tracker removed

        // ===== GAMIFICATION INITIALIZATION =====
        initializeGamification(uid);
    }

    /**
     * Initialize the Supernova Gamification Layer.
     * - Awards daily login XP (once per day)
     * - Loads XP bar with current progress
     * - Loads today's mission card
     * - Wires mission card click to Mission Hub
     */
    private void initializeGamification(String uid) {
        if (uid == null || uid.isEmpty())
            return;

        gamificationEngine = new GamificationEngine(this);
        missionManager = new MissionManager(this);

        // Award daily login XP
        gamificationEngine.awardDailyLogin(uid, new GamificationEngine.XPCallback() {
            @Override
            public void onXPAwarded(int xpGained, int coinsGained, int totalXP, int newLevel, boolean leveledUp) {
                runOnUiThread(() -> {
                    if (xpGained > 0) {
                        Toast.makeText(HomeActivity.this, getString(R.string.daily_login_xp), Toast.LENGTH_SHORT)
                                .show();
                    }
                    updateXPBarUI(totalXP, newLevel);
                    if (leveledUp) {
                        LevelUpDialogFragment.show(HomeActivity.this, newLevel);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e("HomeActivity", "Daily login XP failed: " + error);
            }
        });

        // Load XP bar data
        gamificationEngine.getProfile(uid, new GamificationEngine.ProfileCallback() {
            @Override
            public void onLoaded(GamificationEngine.GamificationProfile profile) {
                runOnUiThread(() -> updateXPBarUI(profile.totalXP, profile.level));
            }

            @Override
            public void onFailure(String error) {
            }
        });

        // Load today's mission card
        loadHomeMissionCard(uid);

        // Wire mission card click
        MaterialCardView missionCard = findViewById(R.id.missionCard);
        if (missionCard != null) {
            missionCard.setOnClickListener(v -> {
                startActivity(new Intent(HomeActivity.this, MissionHubActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void updateXPBarUI(int totalXP, int level) {
        TextView tvLevelBadge = findViewById(R.id.tvLevelBadge);
        TextView tvLevelLabel = findViewById(R.id.tvLevelLabel);
        TextView tvXPProgress = findViewById(R.id.tvXPProgress);
        TextView tvCoinCount = findViewById(R.id.tvCoinCount);
        ProgressBar xpBar = findViewById(R.id.xpProgressBar);

        if (tvLevelBadge == null)
            return;

        int[] progress = GamificationEngine.getLevelProgress(totalXP);
        int xpProgress = progress[0];
        int xpNeeded = progress[1];

        tvLevelBadge.setText(String.valueOf(level));
        tvLevelLabel.setText(getString(R.string.level_label, level));
        tvXPProgress.setText(getString(R.string.xp_progress_format, xpProgress, xpNeeded));

        if (xpBar != null) {
            xpBar.setMax(xpNeeded);
            xpBar.setProgress(xpProgress);
        }
    }

    private void loadHomeMissionCard(String uid) {
        TextView tvTitle = findViewById(R.id.tvHomeMissionTitle);
        TextView tvObjective = findViewById(R.id.tvHomeMissionObjective);
        TextView tvDayBadge = findViewById(R.id.tvHomeDayBadge);
        TextView tvStatus = findViewById(R.id.tvHomeMissionStatus);

        if (tvTitle == null)
            return;

        missionManager.getTodaysMission(uid, new MissionManager.MissionCallback() {
            @Override
            public void onMissionLoaded(MissionManager.Mission mission) {
                runOnUiThread(() -> {
                    tvTitle.setText(mission.title);
                    tvObjective.setText(mission.objective);
                    tvDayBadge.setText(getString(R.string.mission_day_label, mission.dayIndex));
                    if (mission.completed) {
                        tvStatus.setText(R.string.mission_completed_label);
                        tvStatus.setTextColor(getResources().getColor(R.color.mission_completed));
                    } else {
                        tvStatus.setText(R.string.home_mission_card_title);
                        tvStatus.setTextColor(getResources().getColor(R.color.mission_accent));
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("HomeActivity", "Mission load failed: " + error);
            }
        });
    }

    /**
     * Initialize the dynamic progress tracker with current user progress
     */
    private void initializeProgressTracker() {
        if (currentUserId == null) {
            // No user logged in, show default state
            currentMilestone = 0;
            updateProgressTracker();
            setupMilestoneClickListeners();
            return;
        }

        // Load user progress from Firebase
        loadUserProgressFromFirebase();
    }

    /**
     * Load user progress from Firebase
     */
    private void loadUserProgressFromFirebase() {
        if (currentUserId == null)
            return;

        firebaseProgressHelper.getUserProgress(currentUserId, new FirebaseProgressHelper.ProgressDataCallback() {
            @Override
            public void onSuccess(FirebaseProgressHelper.UserProgress progress) {
                userProgress = progress;
                currentMilestone = progress.getCurrentMilestone();

                // Update UI on main thread
                runOnUiThread(() -> {
                    updateProgressTracker();
                    setupMilestoneClickListeners();
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e("HomeActivity", "Error loading user progress: " + error);
                currentMilestone = 0;

                // Update UI on main thread
                runOnUiThread(() -> {
                    updateProgressTracker();
                    setupMilestoneClickListeners();
                });
            }
        });
    }

    /**
     * Update the progress tracker UI dynamically
     */
    private void updateProgressTracker() {
        updateProgressLine();
        updateMilestoneStates();
        updateProgressIndicator();
        animateProgressChanges();
    }

    /**
     * Update the progress line based on current milestone
     */
    private void updateProgressLine() {
        if (progressLineActive == null)
            return;

        progressLineActive.post(() -> {
            ViewGroup parent = (ViewGroup) progressLineActive.getParent();
            int width = parent.getWidth();
            float fraction = Math.max(0f, Math.min(1f, (float) currentMilestone / (totalMilestones - 1)));

            ViewGroup.LayoutParams lp = progressLineActive.getLayoutParams();
            lp.width = (int) (width * fraction);
            progressLineActive.setLayoutParams(lp);
        });
    }

    /**
     * Update milestone states (completed, current, locked)
     */
    private void updateMilestoneStates() {
        for (int i = 0; i < totalMilestones; i++) {
            LinearLayout container = findViewById(getMilestoneContainerId(i));
            ImageButton dot = findViewById(getMilestoneDotId(i));

            if (container != null && dot != null) {
                // Get TextViews from container (first and second TextView)
                TextView nameText = null;
                TextView statusText = null;

                for (int j = 0; j < container.getChildCount(); j++) {
                    View child = container.getChildAt(j);
                    if (child instanceof TextView) {
                        if (nameText == null) {
                            nameText = (TextView) child;
                        } else if (statusText == null) {
                            statusText = (TextView) child;
                            break;
                        }
                    }
                }

                if (nameText != null && statusText != null) {
                    boolean isCompleted = false;

                    // Check if milestone is completed in Firebase data
                    if (userProgress != null) {
                        switch (i) {
                            case 0:
                                isCompleted = userProgress.isBasicsCompleted();
                                break;
                            case 1:
                                isCompleted = userProgress.isIntermediateCompleted();
                                break;
                            case 2:
                                isCompleted = userProgress.isAdvancedCompleted();
                                break;
                            case 3:
                                isCompleted = userProgress.isExpertCompleted();
                                break;
                        }
                    }

                    if (isCompleted) {
                        // Completed milestone
                        dot.setBackgroundResource(R.drawable.milestone_completed_new);
                        dot.setImageResource(R.drawable.ic_checkmark);
                        nameText.setTextColor(getResources().getColor(R.color.completed_text));
                        statusText.setText("Completed");
                        statusText.setTextColor(getResources().getColor(R.color.text_tertiary));
                    } else if (i == currentMilestone) {
                        // Current milestone
                        dot.setBackgroundResource(R.drawable.milestone_current_new);
                        dot.setImageResource(0); // No icon for current
                        nameText.setTextColor(getResources().getColor(R.color.current_text));
                        statusText.setText("Current");
                        statusText.setTextColor(getResources().getColor(R.color.text_tertiary));
                    } else {
                        // Locked milestone
                        dot.setBackgroundResource(R.drawable.milestone_locked_new);
                        dot.setImageResource(R.drawable.ic_lock);
                        nameText.setTextColor(getResources().getColor(R.color.locked_text));
                        statusText.setText("Locked");
                        statusText.setTextColor(getResources().getColor(R.color.text_tertiary));
                    }
                }
            }
        }
    }

    /**
     * Update progress indicator (percentage and counter)
     */
    private void updateProgressIndicator() {
        if (progressPercentage != null) {
            int percentage = (int) ((float) currentMilestone / totalMilestones * 100);
            progressPercentage.setText(percentage + "%");
        }

        if (progressCounter != null) {
            progressCounter.setText(currentMilestone + "/" + totalMilestones + " completed");
        }
    }

    /**
     * Animate progress changes
     */
    private void animateProgressChanges() {
        // Animate progress line fill
        if (progressLineActive != null) {
            progressLineActive.startAnimation(AnimationUtils.loadAnimation(this, R.anim.progress_line_fill));
        }

        // Animate current milestone with pulse effect
        ImageButton currentDot = findViewById(getMilestoneDotId(currentMilestone));
        if (currentDot != null) {
            currentDot.startAnimation(AnimationUtils.loadAnimation(this, R.anim.milestone_glow));
        }

        // Animate completed milestones with subtle scale
        for (int i = 0; i < currentMilestone; i++) {
            ImageButton completedDot = findViewById(getMilestoneDotId(i));
            if (completedDot != null) {
                completedDot.startAnimation(AnimationUtils.loadAnimation(this, R.anim.milestone_scale));
            }
        }
    }

    /**
     * Setup click listeners for milestone interactions
     */
    private void setupMilestoneClickListeners() {
        for (int i = 0; i < totalMilestones; i++) {
            LinearLayout container = findViewById(getMilestoneContainerId(i));
            if (container != null) {
                final int milestoneIndex = i;
                container.setOnClickListener(v -> onMilestoneClicked(milestoneIndex));
            }
        }
    }

    /**
     * Handle milestone click events
     */
    private void onMilestoneClicked(int milestoneIndex) {
        // Add ripple effect animation
        LinearLayout container = findViewById(getMilestoneContainerId(milestoneIndex));
        if (container != null) {
            container.startAnimation(AnimationUtils.loadAnimation(this, R.anim.milestone_ripple_effect));
        }

        if (milestoneIndex <= currentMilestone) {
            // Show milestone details or navigate to milestone content
            showMilestoneDetails(milestoneIndex);
        } else {
            // Show locked milestone message
            showLockedMilestoneMessage(milestoneIndex);
        }
    }

    /**
     * Show milestone details
     */
    private void showMilestoneDetails(int milestoneIndex) {
        String milestoneName = milestoneNames[milestoneIndex];
        String message = milestoneIndex < currentMilestone ? milestoneName + " completed! Great job!"
                : "You're currently working on " + milestoneName;

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show locked milestone message
     */
    private void showLockedMilestoneMessage(int milestoneIndex) {
        String milestoneName = milestoneNames[milestoneIndex];
        Toast.makeText(this, milestoneName + " will unlock after completing previous milestones", Toast.LENGTH_LONG)
                .show();
    }

    /**
     * Advance to next milestone (called when user completes a milestone)
     */
    public void advanceToNextMilestone() {
        if (currentMilestone < totalMilestones - 1 && currentUserId != null) {
            int completedMilestone = currentMilestone;

            // Save progress to Firebase
            firebaseProgressHelper.markMilestoneCompleted(currentUserId, completedMilestone, 10,
                    new FirebaseProgressHelper.ProgressCallback() {
                        @Override
                        public void onSuccess() {
                            // Reload progress from Firebase
                            loadUserProgressFromFirebase();

                            // Show celebration animation
                            showMilestoneCompletionAnimation();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(HomeActivity.this, "Failed to save progress: " + error, Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
        }
    }

    /**
     * Mark a specific test as completed and update progress
     */
    public void markTestCompleted(String testType, int score) {
        if (currentUserId == null)
            return;

        int milestoneIndex = getMilestoneForTestType(testType);
        if (milestoneIndex != -1) {
            // Mark milestone as completed in Firebase
            firebaseProgressHelper.markMilestoneCompleted(currentUserId, milestoneIndex, score,
                    new FirebaseProgressHelper.ProgressCallback() {
                        @Override
                        public void onSuccess() {
                            // Reload progress from Firebase
                            loadUserProgressFromFirebase();

                            // Show celebration animation
                            showMilestoneCompletionAnimation();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(HomeActivity.this, "Failed to save progress: " + error, Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
        }
    }

    /**
     * Get milestone index for test type
     */
    private int getMilestoneForTestType(String testType) {
        switch (testType.toLowerCase()) {
            case "basic":
            case "basics":
                return 0;
            case "intermediate":
            case "tense":
                return 1;
            case "advanced":
            case "preposition":
                return 2;
            case "expert":
            case "article":
                return 3;
            default:
                return -1;
        }
    }

    /**
     * Show celebration animation when milestone is completed
     */
    private void showMilestoneCompletionAnimation() {
        // Animate the completed milestone with celebration effect
        ImageButton completedDot = findViewById(getMilestoneDotId(currentMilestone - 1));
        if (completedDot != null) {
            completedDot.startAnimation(AnimationUtils.loadAnimation(this, R.anim.progress_celebrate));
        }

        // Animate the new current milestone
        ImageButton newCurrentDot = findViewById(getMilestoneDotId(currentMilestone));
        if (newCurrentDot != null) {
            newCurrentDot.startAnimation(AnimationUtils.loadAnimation(this, R.anim.milestone_glow));
        }

        // Show completion message
        Toast.makeText(this, "Milestone completed! 🎉", Toast.LENGTH_SHORT).show();
    }

    /**
     * Helper methods to get resource IDs
     */
    private int getMilestoneContainerId(int index) {
        return 0; // Removed milestone containers
    }

    private int getMilestoneDotId(int index) {
        return 0; // Removed milestone dots
    }

    private int getMilestoneNameId(int index) {
        // The name TextView is the first TextView in each container
        return 0; // We'll access it directly from container
    }

    private int getMilestoneStatusId(int index) {
        // The status TextView is the second TextView in each container
        return 0; // We'll access it directly from container
    }

    /**
     * Legacy method for backward compatibility
     */
    private void updateProgressUI(int milestoneIndex) {
        currentMilestone = milestoneIndex;
        updateProgressTracker();
    }

    /**
     * Add test functionality for progress tracker (for demonstration purposes)
     * This should be removed in production
     */
    // private void addProgressTrackerTestFunctionality() {
    // // Add long press on progress tracker card to advance milestone
    // MaterialCardView progressCard = findViewById(R.id.progressTrackerCard);
    // if (progressCard != null) {
    // progressCard.setOnLongClickListener(v -> {
    // advanceToNextMilestone();
    // return true;
    // });
    // }
    //
    // // Add double tap to reset progress
    // progressCard.setOnClickListener(v -> {
    // // Simple click counter for double tap detection
    // if (v.getTag() == null) {
    // v.setTag(0);
    // }
    // int clickCount = (Integer) v.getTag();
    // clickCount++;
    // v.setTag(clickCount);
    //
    // if (clickCount == 2) {
    // // Reset progress in Firebase
    // if (currentUserId != null) {
    // firebaseProgressHelper.resetUserProgress(currentUserId,
    // new FirebaseProgressHelper.ProgressCallback() {
    // @Override
    // public void onSuccess() {
    // currentMilestone = 0;
    // updateProgressTracker();
    // Toast.makeText(HomeActivity.this, "Progress reset to beginning",
    // Toast.LENGTH_SHORT)
    // .show();
    // }
    //
    // @Override
    // public void onFailure(String error) {
    // Toast.makeText(HomeActivity.this, "Failed to reset progress: " + error,
    // Toast.LENGTH_SHORT).show();
    // }
    // });
    // } else {
    // Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
    // }
    // v.setTag(0);
    // } else {
    // // Reset counter after 1 second
    // new Handler().postDelayed(() -> v.setTag(0), 1000);
    // }
    // });
    // }

    private void loadProfileFromFirestore(String uid) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        if (navUserName != null) {
                            navUserName.setText(name != null ? name : "");
                        }
                        if (navUserEmail != null) {
                            navUserEmail.setText(email != null ? email : "");
                        }
                        if (headerUserName != null && name != null && !name.isEmpty()) {
                            headerUserName.setText(name);
                        }
                    }
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Animate cards entrance on screen load
     */
    private void animateCardsOnLoad() {
        // Hero section animation
        // MaterialCardView heroCard = findViewById(R.id.progressTrackerCard);
        // if (heroCard != null) {
        // heroCard.startAnimation(AnimationUtils.loadAnimation(this,
        // R.anim.fade_in_animation));
        // }

        // Quick stats animation REMOVED
        // LinearLayout statsLayout = findViewById(R.id.quickStatsLayout);
        // if (statsLayout != null) {
        // Handler handler = new Handler();
        // handler.postDelayed(() -> {
        // for (int i = 0; i < statsLayout.getChildCount(); i++) {
        // View child = statsLayout.getChildAt(i);
        // child.startAnimation(AnimationUtils.loadAnimation(this,
        // R.anim.card_enter_animation));
        // }
        // }, 100);
        // }

        // Module cards animation
        animateModuleCards();
    }

    /**
     * Animate learning module cards with staggered entrance
     */
    private void animateModuleCards() {
        MaterialCardView[] cards = {
                findViewById(R.id.speakLearnCard),
                findViewById(R.id.dictionaryCard),
                findViewById(R.id.learningCard),
                findViewById(R.id.callCard)
        };

        Handler handler = new Handler();
        for (int i = 0; i < cards.length; i++) {
            final MaterialCardView card = cards[i];
            if (card != null) {
                handler.postDelayed(() -> {
                    card.startAnimation(AnimationUtils.loadAnimation(this, R.anim.card_enter_animation));
                }, 200 * i + 500); // Stagger animation with 200ms delay between cards
            }
        }
    }

    /**
     * Add click animations to cards
     */
    private void addCardClickAnimations() {
        View[] cards = {
                findViewById(R.id.speakLearnCard),
                findViewById(R.id.dictionaryCard),
                findViewById(R.id.learningCard),
                findViewById(R.id.callCard)
                // findViewById(R.id.quickStatsLayout) // Removed
        };

        for (View card : cards) {
            if (card != null) {
                card.setOnClickListener(view -> {
                    // Scale animation on click
                    view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_button));
                });
            }
        }
    }

    /**
     * Update stats dynamically with animations
     */
    private void updateStatsWithAnimation() {
        // Stats removed as per request
        // TextView streakText = findViewById(R.id.streakValue);
        // TextView wordsText = findViewById(R.id.wordsValue);
        // TextView sessionsText = findViewById(R.id.sessionsValue);

        // Animate counter values (example implementation)
        // animateCounterValue(streakText, 7);
        // animateCounterValue(wordsText, 156);
        // animateCounterValue(sessionsText, 24);
    }

    /**
     * Animate counter value changes
     */
    private void animateCounterValue(TextView textView, int targetValue) {
        if (textView == null)
            return;

        int startValue = 0;
        int duration = 1000; // 1 second
        int frames = 30;
        int increment = targetValue / frames;

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            int currentValue = startValue;

            @Override
            public void run() {
                currentValue += increment;
                if (currentValue >= targetValue) {
                    currentValue = targetValue;
                }

                textView.setText(String.valueOf(currentValue));

                if (currentValue < targetValue) {
                    handler.postDelayed(this, duration / frames);
                }
            }
        };
        handler.post(runnable);
    }

    /**
     * Initialize streak tracking
     */
    private void initializeStreakTracking(String userId) {
        // Initialize streak helper
        streakHelper = new StreakHelper(this);

        // Initialize streak UI elements with null checks
        try {
            // streakCard = findViewById(R.id.streakCard);
            // streakCount = findViewById(R.id.streakCount);
            // longestStreak = findViewById(R.id.longestStreak);
            // totalDays = findViewById(R.id.totalDays);
            // streakDescription = findViewById(R.id.streakDescription);
            // nextMilestone = findViewById(R.id.nextMilestone);
            // fireIcon = findViewById(R.id.fireIcon);
            // streakStatus = findViewById(R.id.streakStatus);

            // Check if streak card exists, if not, skip streak initialization
            if (streakCard == null) {
                Log.w("HomeActivity", "Streak card not found in layout, skipping streak initialization");
                return;
            }
        } catch (Exception e) {
            Log.e("HomeActivity", "Error initializing streak UI elements", e);
            return;
        }

        if (userId != null && !userId.trim().isEmpty()) {
            // Load existing streak data
            streakHelper.loadStreakFromFirebase(userId, new StreakHelper.StreakCallback() {
                @Override
                public void onStreakUpdated(int currentStreak, int longestStreak, int totalDays) {
                    updateStreakUI(currentStreak, longestStreak, totalDays);
                }

                @Override
                public void onStreakMilestone(int streakCount, String milestone) {
                    showStreakMilestone(milestone);
                }

                @Override
                public void onError(String error) {
                    Log.e("StreakHelper", "Error loading streak: " + error);
                }
            });

            // Record today's activity
            streakHelper.recordDailyActivity(userId, new StreakHelper.StreakCallback() {
                @Override
                public void onStreakUpdated(int currentStreak, int longestStreak, int totalDays) {
                    updateStreakUI(currentStreak, longestStreak, totalDays);
                }

                @Override
                public void onStreakMilestone(int streakCount, String milestone) {
                    showStreakMilestone(milestone);
                }

                @Override
                public void onError(String error) {
                    Log.e("StreakHelper", "Error recording activity: " + error);
                }
            });
        } else {
            // No user ID, show default state
            updateStreakUI(0, 0, 0);
        }
    }

    /**
     * Update streak UI with current data
     */
    private void updateStreakUI(int currentStreak, int longestStreak, int totalDays) {
        // Run on UI thread to prevent crashes
        runOnUiThread(() -> {
            try {
                if (streakCount != null) {
                    streakCount.setText(String.valueOf(currentStreak));
                }

                if (this.longestStreak != null) {
                    this.longestStreak.setText(String.valueOf(longestStreak));
                }

                if (this.totalDays != null) {
                    this.totalDays.setText(String.valueOf(totalDays));
                }

                // Update description based on streak
                if (streakDescription != null) {
                    if (currentStreak == 0) {
                        streakDescription.setText("Start your streak today!");
                    } else if (currentStreak == 1) {
                        streakDescription.setText("Great start! Keep it going!");
                    } else if (currentStreak < 7) {
                        streakDescription.setText(currentStreak + " days in a row! 🔥");
                    } else if (currentStreak < 30) {
                        streakDescription.setText(currentStreak + " days streak! Amazing! 🏆");
                    } else {
                        streakDescription.setText(currentStreak + " days! You're a legend! 👑");
                    }
                }

                // Update next milestone
                if (nextMilestone != null) {
                    int nextMilestoneValue = getNextMilestone(currentStreak);
                    if (nextMilestoneValue > 0) {
                        nextMilestone.setText("Next: " + nextMilestoneValue + " days");
                    } else {
                        nextMilestone.setText("Max milestone reached!");
                    }
                }

                // Show/hide fire status based on streak
                if (streakStatus != null) {
                    if (currentStreak >= 3) {
                        streakStatus.setVisibility(View.VISIBLE);
                        // For ImageView, we'll just show it when streak is active
                        // The fire emoji count is handled by the fire icon animation
                    } else {
                        streakStatus.setVisibility(View.GONE);
                    }
                }

                // Animate fire icon for active streaks
                if (fireIcon != null && currentStreak > 0) {
                    fireIcon.animate()
                            .scaleX(1.2f)
                            .scaleY(1.2f)
                            .setDuration(200)
                            .withEndAction(() -> {
                                fireIcon.animate()
                                        .scaleX(1.0f)
                                        .scaleY(1.0f)
                                        .setDuration(200)
                                        .start();
                            })
                            .start();
                }
            } catch (Exception e) {
                Log.e("HomeActivity", "Error updating streak UI", e);
            }
        });
    }

    /**
     * Get next milestone value
     */
    private int getNextMilestone(int currentStreak) {
        int[] milestones = { 1, 3, 7, 14, 30, 50, 100 };
        for (int milestone : milestones) {
            if (currentStreak < milestone) {
                return milestone;
            }
        }
        return 0; // No more milestones
    }

    /**
     * Show streak milestone achievement
     */
    private void showStreakMilestone(String milestone) {
        runOnUiThread(() -> {
            try {
                Toast.makeText(this, milestone, Toast.LENGTH_LONG).show();

                // Animate streak card for milestone
                if (streakCard != null) {
                    streakCard.animate()
                            .scaleX(1.05f)
                            .scaleY(1.05f)
                            .setDuration(300)
                            .withEndAction(() -> {
                                streakCard.animate()
                                        .scaleX(1.0f)
                                        .scaleY(1.0f)
                                        .setDuration(300)
                                        .start();
                            })
                            .start();
                }
            } catch (Exception e) {
                Log.e("HomeActivity", "Error showing streak milestone", e);
            }
        });
    }

    /**
     * Record activity when user interacts with the app
     */
    public void recordActivity() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String uid = sharedPreferences.getString("UID", null);
        if (uid != null && streakHelper != null) {
            streakHelper.recordDailyActivity(uid, new StreakHelper.StreakCallback() {
                @Override
                public void onStreakUpdated(int currentStreak, int longestStreak, int totalDays) {
                    updateStreakUI(currentStreak, longestStreak, totalDays);
                }

                @Override
                public void onStreakMilestone(int streakCount, String milestone) {
                    showStreakMilestone(milestone);
                }

                @Override
                public void onError(String error) {
                    Log.e("StreakHelper", "Error recording activity: " + error);
                }
            });
        }
    }
}
