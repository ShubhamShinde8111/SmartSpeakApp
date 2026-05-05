package com.example.registration_login_module;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnSuccessListener;

public class LoginActivity extends AppCompatActivity {

    TextView Regbtn, forgotPassword;
    private com.google.android.material.textfield.TextInputEditText inputEmail, inputPassword;
    // private CheckBox showPassword; // Removed
    Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login2);

        Regbtn = findViewById(R.id.textViewSignUp);
        //forgotPassword = findViewById(R.id.forgotPassword);

        // Update IDs to match new layout (TextInputEditText is inside TextInputLayout)
        com.google.android.material.textfield.TextInputLayout emailLayout = findViewById(R.id.emailLayout);
        com.google.android.material.textfield.TextInputLayout passwordLayout = findViewById(R.id.passwordLayout);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);

        // Remove CheckBox logic as we now use TextInputLayout's password toggle
        // showPassword = findViewById(R.id.showPassword);

        btnLogin = findViewById(R.id.btnlogin);

        // Add entrance animations
        android.view.animation.Animation slideUp = android.view.animation.AnimationUtils.loadAnimation(this,
                R.anim.slide_in_bottom);
        android.view.animation.Animation fadeIn = android.view.animation.AnimationUtils.loadAnimation(this,
                R.anim.fade_in_animation);

        findViewById(R.id.headerContainer).startAnimation(slideUp);
        findViewById(R.id.loginCard).startAnimation(slideUp);

        // Staggered animation for inputs
        emailLayout.setAlpha(0f);
        emailLayout.animate().alpha(1f).setDuration(500).setStartDelay(300).start();

        passwordLayout.setAlpha(0f);
        passwordLayout.animate().alpha(1f).setDuration(500).setStartDelay(400).start();

        btnLogin.setAlpha(0f);
        btnLogin.animate().alpha(1f).setDuration(500).setStartDelay(500).start();

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                checkCredentials();
                SharedPreferences shpf = getSharedPreferences("Login", MODE_PRIVATE);
                SharedPreferences.Editor editor = shpf.edit();
                editor.putBoolean("flag", true);
                editor.apply();

            }
        });

        Regbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

//        forgotPassword.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
//            }
//        });
    }

    private void checkCredentials() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (email.isEmpty() || !email.contains("@") || !email.endsWith(".com")) {
            showError(inputEmail, "Email is not valid!");
        } else if (password.isEmpty() || password.length() < 7) {
            showError(inputPassword, "Password must be at least 7 characters");
        } else {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        String uid = result.getUser().getUid();
                        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("UID", uid);
                        editor.apply();

                        // Initialize user progress in Firebase
                        FirebaseProgressHelper progressHelper = new FirebaseProgressHelper();
                        progressHelper.initializeUserProgress(uid, new FirebaseProgressHelper.ProgressCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d("LoginActivity", "User progress initialized successfully");
                            }

                            @Override
                            public void onFailure(String error) {
                                Log.e("LoginActivity", "Failed to initialize user progress: " + error);
                            }
                        });

                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void showError(EditText input, String s) {
        input.setError(s);
        input.requestFocus();
    }
}
