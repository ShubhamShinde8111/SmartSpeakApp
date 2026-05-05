package com.example.registration_login_module;

import android.content.Context;
import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpeechAnalyticsHelper {

    private final FirebaseFirestore firestore;
    private final FirebaseProgressRepository progressRepo;

    public SpeechAnalyticsHelper(Context context) {
        this.firestore = FirebaseFirestore.getInstance();
        this.progressRepo = new FirebaseProgressRepository(context);
    }

    public void recordAnalysis(String userId, String role, String originalText, int score,
            List<?> errors, List<String> suggestions) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();

        // 1. Save to Firebase legacy analytics (for per-role tracking)
        Map<String, Object> session = new HashMap<>();
        session.put("role", role);
        session.put("originalText", originalText);
        session.put("score", score);
        session.put("errorsCount", errors == null ? 0 : errors.size());
        session.put("suggestionsCount", suggestions == null ? 0 : suggestions.size());
        session.put("createdAt", now);

        DocumentReference logRef = firestore.collection("users")
                .document(userId)
                .collection("speech_sessions")
                .document(String.valueOf(now));
        logRef.set(session);

        // 2. Build strengths/weaknesses lists for the progress repo
        String level;
        if (score >= 80)
            level = "Advanced";
        else if (score >= 50)
            level = "Intermediate";
        else
            level = "Basic";

        List<String> strengthList = new ArrayList<>();
        List<String> weaknessList = new ArrayList<>();

        if (suggestions != null) {
            strengthList.addAll(suggestions);
        } else if (score > 70) {
            strengthList.add("Good pronunciation clarity.");
        }

        if (errors != null) {
            for (Object error : errors) {
                weaknessList.add(String.valueOf(error));
            }
        }

        if ((errors == null || errors.isEmpty()) && score < 50) {
            weaknessList.add("Practice more to improve accuracy.");
        }

        // 3. Save to FirebaseProgressRepository (feeds Dashboard graph + stats)
        progressRepo.saveSpeechSession(userId, originalText, score, level, strengthList, weaknessList);
    }
}
