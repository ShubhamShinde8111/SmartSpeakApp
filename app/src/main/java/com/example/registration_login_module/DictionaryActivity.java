package com.example.registration_login_module;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.registration_login_module.databinding.ActivityDictionaryBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DictionaryActivity extends AppCompatActivity {

    private ActivityDictionaryBinding binding;
    private MeaningAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDictionaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup RecyclerView
        binding.meaningRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MeaningAdapter(new ArrayList<>());
        binding.meaningRecyclerView.setAdapter(adapter);

        // Search button logic
        binding.searchBtn.setOnClickListener(v -> {
            String word = binding.searchInput.getText().toString().trim();
            if (!word.isEmpty()) {
                getMeaning(word);
            } else {
                Toast.makeText(DictionaryActivity.this, "Please enter a word", Toast.LENGTH_SHORT).show();
            }
        });

        // Bottom navigation transitions
        BottomNavigationView bottomNavigationView = binding.bottomNavigation.getRoot();

        if (bottomNavigationView != null) {
            bottomNavigationView.bringToFront();
            bottomNavigationView.setSelectedItemId(R.id.nav_dictionary);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_dictionary) {
                    return true;
                }

                Class<?> destination = null;
                if (id == R.id.nav_learn)
                    destination = HomeActivity.class;
                else if (id == R.id.nav_progress)
                    destination = ProgressDashboardActivity.class;
                else if (id == R.id.nav_profile)
                    destination = ProfileActivity.class;

                if (destination != null) {
                    startActivity(new Intent(DictionaryActivity.this, destination));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                    return true;
                }
                return false;
            });

            bottomNavigationView.setOnItemReselectedListener(item -> {
                // Already on Dictionary
            });
        }
    }

    private void getMeaning(String word) {
        setInProgress(true);
        RetrofitInstance.dictionaryApi.getMeaning(word).enqueue(new Callback<List<WordResult>>() {
            @Override
            public void onResponse(@NonNull Call<List<WordResult>> call, @NonNull Response<List<WordResult>> response) {
                setInProgress(false);
                if (response.body() != null && !response.body().isEmpty()) {
                    WordResult wordResult = response.body().get(0);
                    displayResult(wordResult);
                } else {
                    showError("Word not found");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<WordResult>> call, @NonNull Throwable t) {
                setInProgress(false);
                showError("Something went wrong");
            }
        });
    }

    private void displayResult(WordResult result) {
        binding.emptyState.setVisibility(View.GONE);
        binding.contentLayout.setVisibility(View.VISIBLE);

        binding.wordTextview.setText(result.getWord());
        if (result.getPhonetic() != null) {
            binding.phoneticTextview.setText(result.getPhonetic());
        } else {
            binding.phoneticTextview.setText("Phonetic unavailable");
        }

        adapter.updateNewData(result.getMeanings());
        binding.meaningRecyclerView.scheduleLayoutAnimation();

        // ===== GAMIFICATION: Award XP for lookup =====
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String uid = prefs.getString("UID", null);
        if (uid != null && !uid.trim().isEmpty()) {
            GamificationEngine engine = new GamificationEngine(this);
            engine.awardDictionaryXP(uid);
        }
    }

    private void showError(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void setInProgress(boolean inProgress) {
        if (inProgress) {
            binding.searchBtn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.searchBtn.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
}
