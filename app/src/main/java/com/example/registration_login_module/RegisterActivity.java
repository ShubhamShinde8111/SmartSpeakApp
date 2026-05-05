package com.example.registration_login_module;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

public class RegisterActivity extends AppCompatActivity {

    TextView tvLogin;
    private com.google.android.material.textfield.TextInputEditText inputEmail, inputMobileno,
            inputPlace, inputPassword, inputConfirmPassword;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        tvLogin = findViewById(R.id.alreadyHaveAccount);

        // bind inputs
        inputEmail = findViewById(R.id.inputEmail);
        inputMobileno = findViewById(R.id.inputMobileno);
        inputPlace = findViewById(R.id.inputPlace);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);

        // Animations
        android.view.animation.Animation slideUp = android.view.animation.AnimationUtils.loadAnimation(this,
                R.anim.slide_in_bottom);

        findViewById(R.id.headerContainer).startAnimation(slideUp);
        findViewById(R.id.registerCard).startAnimation(slideUp);

        // Helper to animate inputs
        animateInput(findViewById(R.id.emailLayout), 300);
        animateInput(findViewById(R.id.mobileLayout), 500);
        animateInput(findViewById(R.id.placeLayout), 600);
        animateInput(findViewById(R.id.passwordLayout), 700);
        animateInput(findViewById(R.id.confirmPasswordLayout), 800);

        btnRegister.setAlpha(0f);
        btnRegister.animate().alpha(1f).setDuration(500).setStartDelay(900).start();

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkCredentials();
            }
        });

        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            }
        });
    }

    private void animateInput(View view, long delay) {
        if (view != null) {
            view.setAlpha(0f);
            view.animate().alpha(1f).setDuration(500).setStartDelay(delay).start();
        }
    }

    private void checkCredentials() {
        String email = inputEmail.getText().toString().trim();
        String mobileno = inputMobileno.getText().toString().trim();
        String place = inputPlace.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || !email.contains("@") || !email.endsWith(".com")) {
            showError(inputEmail, "Email is not valid!");
        } else if (mobileno.isEmpty() || mobileno.length() != 10) {
            showError(inputMobileno, "Mobile No. must be 10 digits");
        } else if (password.isEmpty() || password.length() < 7) {
            showError(inputPassword, "Password must be at least 7 characters");
        } else if (confirmPassword.isEmpty() || !confirmPassword.equals(password)) {
            showError(inputConfirmPassword, "Passwords do not match!");
        } else {
            registerUser(mobileno, email, password, place);
        }
    }

    private void registerUser(String mobileno, String email, String password, String place) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore fs = FirebaseFirestore.getInstance();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    java.util.Map<String, Object> profile = new java.util.HashMap<>();
                    profile.put("name", "User");
                    profile.put("phone", mobileno);
                    profile.put("email", email);
                    profile.put("place", place);
                    profile.put("createdAt", com.google.firebase.Timestamp.now());

                    fs.collection("users").document(uid).set(profile, SetOptions.merge())
                            .addOnSuccessListener(v -> {
                                // Initialize user progress in Firebase
                                FirebaseProgressHelper progressHelper = new FirebaseProgressHelper();
                                progressHelper.initializeUserProgress(uid,
                                        new FirebaseProgressHelper.ProgressCallback() {
                                            @Override
                                            public void onSuccess() {
                                                Toast.makeText(RegisterActivity.this, "Registration successful!",
                                                        Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                                finish();
                                            }

                                            @Override
                                            public void onFailure(String error) {
                                                Toast.makeText(RegisterActivity.this,
                                                        "Registration successful, but progress initialization failed",
                                                        Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                                finish();
                                            }
                                        });
                            })
                            .addOnFailureListener(
                                    e -> Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showError(com.google.android.material.textfield.TextInputEditText input, String s) {
        input.setError(s);
        input.requestFocus();
    }
}
