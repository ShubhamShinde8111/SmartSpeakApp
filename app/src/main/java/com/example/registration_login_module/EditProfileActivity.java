package com.example.registration_login_module;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editUsername, editEmail, editMobileNo, editPlace, currentPassword, newPassword, confirmNewPassword;
    private Button saveButton, backButton;
    private ProgressBar progressBar;
    private String uid;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Original values to check for changes
    private String originalName, originalEmail, originalPhone, originalPlace;

    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (uid != null) {
                        fetchUserDetails(uid);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        editUsername = findViewById(R.id.editUsername);
        editEmail = findViewById(R.id.editEmail);
        editMobileNo = findViewById(R.id.editMobileNo);
        editPlace = findViewById(R.id.editPlace);
        currentPassword = findViewById(R.id.currentPassword);
        newPassword = findViewById(R.id.newPassword);
        confirmNewPassword = findViewById(R.id.confirmNewPassword);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);
        progressBar = findViewById(R.id.progressBar);

        // Get user UID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        uid = sharedPreferences.getString("UID", null);

        if (uid != null) {
            fetchUserDetails(uid);
        } else {
            showToast("User not logged in");
            finish();
        }

        saveButton.setOnClickListener(v -> updateUserDetails());

        backButton.setOnClickListener(v -> {
            finish();
        });
    }

    private void fetchUserDetails(String uid) {
        setLoading(true);
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    setLoading(false);
                    if (documentSnapshot.exists()) {
                        originalName = documentSnapshot.getString("name");
                        originalEmail = documentSnapshot.getString("email");
                        originalPhone = documentSnapshot.getString("phone");
                        originalPlace = documentSnapshot.getString("place");

                        editUsername.setText(originalName);
                        editEmail.setText(originalEmail);
                        editMobileNo.setText(originalPhone);
                        editPlace.setText(originalPlace);
                    } else {
                        showToast("User data not found");
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showToast("Failed to fetch data: " + e.getMessage());
                });
    }

    private void updateUserDetails() {
        String newUsername = editUsername.getText().toString().trim();
        String newEmail = editEmail.getText().toString().trim();
        String newMobileNo = editMobileNo.getText().toString().trim();
        String newPlace = editPlace.getText().toString().trim();
        String currentPwd = currentPassword.getText().toString().trim();
        String newPwd = newPassword.getText().toString().trim();
        String confirmPwd = confirmNewPassword.getText().toString().trim();

        if (newUsername.isEmpty() || newEmail.isEmpty() || newMobileNo.isEmpty()) {
            showToast("Name, Email, and Mobile fields cannot be empty");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        boolean hasProfileChanges = false;

        if (!newUsername.equals(originalName)) {
            updates.put("name", newUsername);
            hasProfileChanges = true;
        }
        if (!newEmail.equals(originalEmail)) {
            updates.put("email", newEmail);
            hasProfileChanges = true;
        }
        if (!newMobileNo.equals(originalPhone)) {
            updates.put("phone", newMobileNo);
            hasProfileChanges = true;
        }
        if (newPlace != null && !newPlace.equals(originalPlace)) {
            updates.put("place", newPlace);
            hasProfileChanges = true;
        }

        // Handle Password Update first if requested
        if (!newPwd.isEmpty()) {
            if (newPwd.length() < 7) {
                showToast("New password must be at least 7 characters");
                return;
            }
            if (!newPwd.equals(confirmPwd)) {
                showToast("Passwords do not match");
                return;
            }
            if (currentPwd.isEmpty()) {
                showToast("Please enter current password to change password");
                return;
            }

            updatePassword(currentPwd, newPwd, updates, hasProfileChanges);
        } else {
            // Only profile updates
            if (hasProfileChanges) {
                pushUpdatesToFirestore(updates);
            } else {
                showToast("No changes to save");
            }
        }
    }

    private void updatePassword(String currentPwd, String newPwd, Map<String, Object> profileUpdates,
            boolean hasProfileUpdates) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            setLoading(true);
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPwd);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updatePassword(newPwd).addOnCompleteListener(passwordTask -> {
                        if (passwordTask.isSuccessful()) {
                            showToast("Password updated successfully");
                            if (hasProfileUpdates) {
                                pushUpdatesToFirestore(profileUpdates);
                            } else {
                                setLoading(false);
                                setResult(RESULT_OK);
                                finish();
                            }
                        } else {
                            setLoading(false);
                            showToast("Failed to update password: " + passwordTask.getException().getMessage());
                        }
                    });
                } else {
                    setLoading(false);
                    showToast("Incorrect current password");
                }
            });
        }
    }

    private void pushUpdatesToFirestore(Map<String, Object> updates) {
        setLoading(true);
        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    showToast("Profile updated successfully");
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showToast("Failed to update profile: " + e.getMessage());
                });
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        saveButton.setEnabled(!isLoading);
        backButton.setEnabled(!isLoading);
    }

    private void showToast(String message) {
        Toast.makeText(EditProfileActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
