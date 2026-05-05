package com.example.registration_login_module;

import java.util.ArrayList;
import java.util.List;

/**
 * GrammarChapter — Represents a single level/chapter in the Mission Hub.
 *
 * Each chapter focuses on one grammar topic and contains exactly 4 missions:
 * Learn, Practice, Translate, Speak.
 */
public class GrammarChapter {

    public String chapterId;
    public int chapterNumber;
    public String title;
    public String subtitle;
    public String emoji;
    public ChapterStatus status;
    public List<GrammarMission> missions;
    public int completedCount;

    public enum ChapterStatus {
        COMPLETED,  // All 4 missions done — green border
        ACTIVE,     // Currently unlocked — orange border
        LOCKED      // Not yet unlocked — dimmed
    }

    public GrammarChapter() {
        this.missions = new ArrayList<>();
        this.completedCount = 0;
        this.status = ChapterStatus.LOCKED;
    }

    public GrammarChapter(String chapterId, int chapterNumber, String title,
                          String subtitle, String emoji) {
        this.chapterId = chapterId;
        this.chapterNumber = chapterNumber;
        this.title = title;
        this.subtitle = subtitle;
        this.emoji = emoji;
        this.missions = new ArrayList<>();
        this.completedCount = 0;
        this.status = ChapterStatus.LOCKED;
    }

    public int getTotalMissions() {
        return 4;
    }

    public boolean isCompleted() {
        return completedCount >= 4;
    }

    public boolean isActive() {
        return status == ChapterStatus.ACTIVE;
    }

    public boolean isLocked() {
        return status == ChapterStatus.LOCKED;
    }

    /**
     * Get the next uncompleted mission in this chapter, or null if all done.
     */
    public GrammarMission getNextMission() {
        for (GrammarMission m : missions) {
            if (m.status != GrammarMission.MissionStatus.COMPLETED) {
                return m;
            }
        }
        return null;
    }

    /**
     * Get the progress as a fraction string, e.g. "2/4".
     */
    public String getProgressText() {
        return completedCount + "/" + getTotalMissions();
    }
}
