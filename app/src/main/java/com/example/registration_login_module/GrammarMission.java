package com.example.registration_login_module;

/**
 * GrammarMission — Represents a single mission within a chapter.
 *
 * Each chapter contains exactly 4 missions in fixed order:
 * 1. LEARN    — Read grammar rules and examples
 * 2. PRACTICE — Hindi→English word bank sentence building
 * 3. TRANSLATE — Hindi→English speech translation
 * 4. SPEAK    — English Q&A speaking exercise
 */
public class GrammarMission {

    public String missionId;
    public String title;
    public MissionType type;
    public MissionStatus status;
    public String chapterId;  // parent chapter reference
    public int orderIndex;    // 0-3 within the chapter

    public enum MissionType {
        LEARN,      // 📘 Read rules and examples
        PRACTICE,   // 🧩 Word bank sentence building
        TRANSLATE,  // 🎤 Speech translation
        SPEAK       // 💬 Speaking Q&A
    }

    public enum MissionStatus {
        COMPLETED,  // Done — green dot
        ACTIVE,     // Currently available — colored dot
        LOCKED      // Not yet unlocked — grey dot
    }

    public GrammarMission() {
        this.status = MissionStatus.LOCKED;
    }

    public GrammarMission(String missionId, String title, MissionType type,
                          String chapterId, int orderIndex) {
        this.missionId = missionId;
        this.title = title;
        this.type = type;
        this.chapterId = chapterId;
        this.orderIndex = orderIndex;
        this.status = MissionStatus.LOCKED;
    }

    public boolean isCompleted() {
        return status == MissionStatus.COMPLETED;
    }

    public boolean isActive() {
        return status == MissionStatus.ACTIVE;
    }

    public boolean isLocked() {
        return status == MissionStatus.LOCKED;
    }

    /**
     * Get emoji icon for this mission type.
     */
    public String getTypeEmoji() {
        switch (type) {
            case LEARN:     return "📘";
            case PRACTICE:  return "🧩";
            case TRANSLATE: return "🎤";
            case SPEAK:     return "💬";
            default:        return "📋";
        }
    }

    /**
     * Get display label for this mission type.
     */
    public String getTypeLabel() {
        switch (type) {
            case LEARN:     return "Learn";
            case PRACTICE:  return "Practice";
            case TRANSLATE: return "Translate";
            case SPEAK:     return "Speak";
            default:        return "Mission";
        }
    }
}
