package com.example.registration_login_module;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.EventListener;

import javax.annotation.Nullable;

public class ProfileActivity extends AppCompatActivity {

    private TextView profileName, profileEmail, profileMobileNo, profilePlace, titleUsername;
    private ProgressBar progressBar;
    private Button editButton, backBtnProfile, logoutbtn;
    private String uid;
    private ListenerRegistration profileListener;

    // ActivityResultLauncher to replace startActivityForResult
    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                // No need to manually fetch, Listener will handle updates
                if (result.getResultCode() == RESULT_OK) {
                    // Optional: Show a message or just let the UI update automatically
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize views
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        profileMobileNo = findViewById(R.id.profileMobileNo);
        titleUsername = findViewById(R.id.titleUsername);
        profilePlace = findViewById(R.id.profilePlace);
        progressBar = findViewById(R.id.progressBar);
        editButton = findViewById(R.id.editButton);
        backBtnProfile = findViewById(R.id.backBtnProfile);
        logoutbtn = findViewById(R.id.logoutButton);

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        uid = sharedPreferences.getString("UID", null);

        // Handle Edit Profile button click
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            editProfileLauncher.launch(intent);
        });

        backBtnProfile.setOnClickListener(v -> {
            finish();
        });

        logoutbtn.setOnClickListener(v -> {
            SharedPreferences shpf = getSharedPreferences("Login", MODE_PRIVATE);
            SharedPreferences.Editor editor = shpf.edit();
            editor.putBoolean("flag", false);
            editor.apply();
            Intent iNext = new Intent(ProfileActivity.this, LoginActivity.class);
            iNext.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(iNext);
            finish();
        });

        setupBottomNavigation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (uid != null) {
            attachProfileListener(uid);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
    }

    private void attachProfileListener(String uid) {
        progressBar.setVisibility(View.VISIBLE);
        profileListener = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot,
                            @Nullable FirebaseFirestoreException e) {
                        progressBar.setVisibility(View.GONE);
                        if (e != null) {
                            showToast("Listen failed: " + e.getMessage());
                            return;
                        }

                        if (snapshot != null && snapshot.exists()) {
                            String name = snapshot.getString("name");
                            String email = snapshot.getString("email");
                            String mobile = snapshot.getString("phone");
                            String place = snapshot.getString("place");

                            profileName.setText(name != null ? name : "");
                            profileEmail.setText(email != null ? email : "");
                            profileMobileNo.setText(mobile != null ? mobile : "");
                            profilePlace.setText(place != null ? place : "");
                            titleUsername.setText(name != null ? name : "");
                        } else {
                            // User document doesn't exist?
                        }
                    }
                });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.bringToFront();
            bottomNavigationView.setSelectedItemId(R.id.nav_profile);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_profile) {
                    return true;
                }

                Class<?> destination = null;
                if (id == R.id.nav_learn)
                    destination = HomeActivity.class;
                else if (id == R.id.nav_dictionary)
                    destination = DictionaryActivity.class;
                else if (id == R.id.nav_progress)
                    destination = ProgressDashboardActivity.class;

                if (destination != null) {
                    startActivity(new Intent(ProfileActivity.this, destination));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    private void showToast(String message) {
        Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
