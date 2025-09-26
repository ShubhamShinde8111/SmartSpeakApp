package com.example.registration_login_module;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

public class HomeActivity extends AppCompatActivity {

    private GridLayout gridLayout;
    private ProgressBar progressBar;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    ImageButton buttonDrawer;
    private DBHelper dbHelper;
    TextView navUserName,navUserEmail;    
    private TextView headerUserName;
    private View progressLineActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        buttonDrawer = findViewById(R.id.buttonDrawer);

        View headerView = navigationView.getHeaderView(0);
        progressBar = findViewById(R.id.progressBar);
        progressLineActive = findViewById(R.id.progressLineActive);
        headerUserName = findViewById(R.id.welcomeTitle);

        // Find the TextViews in the header
        navUserName = headerView.findViewById(R.id.userName);
        navUserEmail = headerView.findViewById(R.id.userEmail);

        // Load profile from Firebase using stored UID
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String uid = sharedPreferences.getString("UID", null);
        if (uid != null) {
            loadProfileFromFirestore(uid);
        }

        // Set up drawer toggle
        buttonDrawer.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Handle navigation item clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navProfile) {
                Toast.makeText(this, "Profile Selected", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                intent.putExtra("USER_ID", 1); // Example user ID
                startActivity(intent);
            }
            else if(itemId == R.id.navProfile) {
                Intent iNext = new Intent(HomeActivity.this, ProfileActivity.class);
                startActivity(iNext);
            }
            else if(itemId == R.id.navChngPassword) {
                Intent iNext = new Intent(HomeActivity.this, EditProfileActivity.class);
                startActivity(iNext);
            }
            else if(itemId == R.id.navFeedback) {
                Intent iNext = new Intent(HomeActivity.this, FeedbackAcitivity.class);
                startActivity(iNext);
            }
            else if(itemId == R.id.navLogout) {
                SharedPreferences shpf=getSharedPreferences("Login",MODE_PRIVATE);
                SharedPreferences.Editor editor=shpf.edit();
                editor.putBoolean("flag",false);
                editor.apply();
                Intent iNext = new Intent(HomeActivity.this, LoginActivity.class);
                iNext.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(iNext);
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Initialize the GridLayout
        gridLayout = findViewById(R.id.gridLayout);

        // Setting up click listener for Profile CardView
        // Assuming Profile is the first card
        CardView profileVisitCard = findViewById(R.id.profileCard);
        profileVisitCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                intent.putExtra("USER_ID", 1); // Example user ID
                startActivity(intent);
            }
        });

        // Access the "About" card view (second card in the GridLayout)

        CardView aboutVisitCard = findViewById(R.id.aboutCard);// About is the second card
        aboutVisitCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent to navigate to the AboutActivity when the card is clicked
                Intent intent = new Intent(HomeActivity.this, AboutPageAcitivity.class);
                startActivity(intent); // Start the AboutActivity
            }
        });

//        CardView speakLearnCard = findViewById(R.id.speakLearnCard);
//        speakLearnCard.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Intent to navigate to Speak-Learn activity
//                Intent intent = new Intent(HomeActivity.this, SpeakLearnActivity.class);
//                startActivity(intent);
//            }
//        });

        CardView dictionaryCard = findViewById(R.id.dictionaryCard);
        dictionaryCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent to navigate to Dictionary activity
                Intent intent = new Intent(HomeActivity.this, DictionaryActivity.class);
                startActivity(intent);
            }
        });



        CardView learningPathCard = findViewById(R.id.learningCard);
        learningPathCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent to navigate to Learning Path activity
                Intent intent = new Intent(HomeActivity.this, LearningActivity.class);
                startActivity(intent);
            }
        });


        CardView feedbackCard = findViewById(R.id.feedbackCard);
        feedbackCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent to navigate to Feedback activity
                Intent intent = new Intent(HomeActivity.this, FeedbackAcitivity.class);
                startActivity(intent);
            }
        });



        CardView aispeakCard=findViewById(R.id.speakLearnCard);
        aispeakCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(HomeActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        CardView callCard=findViewById(R.id.callCard);
        callCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(HomeActivity.this, AudioCallActivity.class);
                intent.putExtra("CHANNEL", getString(R.string.agora_channel));
                intent.putExtra("TOKEN", getString(R.string.agora_temp_token));
                startActivity(intent);
            }
        });



        // Add other listeners as needed
        updateProgressUI(1);
    }

    private void updateProgressUI(int milestoneIndex) {
        if (progressLineActive == null) return;
        progressLineActive.post(() -> {
            ViewGroup parent = (ViewGroup) progressLineActive.getParent();
            int width = parent.getWidth();
            float fraction = Math.max(0f, Math.min(1f, (milestoneIndex - 1) / 3f));
            ViewGroup.LayoutParams lp = progressLineActive.getLayoutParams();
            lp.width = (int)(width * fraction);
            progressLineActive.setLayoutParams(lp);
        });
    }

    private void loadProfileFromFirestore(String uid) {
        progressBar.setVisibility(View.VISIBLE);
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        navUserName.setText(name != null ? name : "");
                        navUserEmail.setText(email != null ? email : "");
                        if (headerUserName != null && name != null && !name.isEmpty()) {
                            headerUserName.setText(name);
                        }
                    }
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void showToast(String message) {
        Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
