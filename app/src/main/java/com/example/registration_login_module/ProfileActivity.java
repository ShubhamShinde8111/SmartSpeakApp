package com.example.registration_login_module;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private TextView profileName, profileEmail, profileMobileNo, profilePlace, titleUsername;
    private ProgressBar progressBar;
    private Button editButton, backBtnProfile,logoutbtn;
    private DBHelper dbHelper;
    private int userId;
    private String uid;

    // ActivityResultLauncher to replace startActivityForResult
    private final ActivityResultLauncher<Intent> editProfileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Refresh user details after editing
                    if (uid != null) {
                        fetchUserDetailsFromFirestore(uid);
                    }
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
        logoutbtn=findViewById(R.id.logoutButton);

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        uid = sharedPreferences.getString("UID", null);
        if (uid != null) {
            fetchUserDetailsFromFirestore(uid);
        }

        // Handle Edit Profile button click
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            // Pass gender to EditProfileActivity
            //intent.putExtra("USER_GENDER", profileGender.getText().toString());
            editProfileLauncher.launch(intent);
        });

        backBtnProfile.setOnClickListener(v -> {
            new android.os.Handler().postDelayed(() -> {
                finish();
            },0);
        });

        logoutbtn.setOnClickListener(v ->{
            SharedPreferences shpf=getSharedPreferences("Login",MODE_PRIVATE);
            SharedPreferences.Editor editor=shpf.edit();
            editor.putBoolean("flag",false);
            editor.apply();
            Intent iNext = new Intent(ProfileActivity.this, LoginActivity.class);
            iNext.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(iNext);
            finish();
        });
    }

    private void fetchUserDetailsFromFirestore(String uid) {
        progressBar.setVisibility(View.VISIBLE);
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String mobile = doc.getString("phone");
                        String place = doc.getString("place");
                        profileName.setText(name != null ? name : "");
                        profileEmail.setText(email != null ? email : "");
                        profileMobileNo.setText(mobile != null ? mobile : "");
                        profilePlace.setText(place != null ? place : "");
                        titleUsername.setText(name != null ? name : "");
                    }
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> progressBar.setVisibility(View.GONE));
    }

    private void showToast(String message) {
        Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
