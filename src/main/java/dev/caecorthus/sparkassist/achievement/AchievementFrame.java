package dev.caecorthus.sparkassist.achievement;

/**
 * The frame an achievement is drawn with, matching vanilla's three advancement frames and derived from its level:
 * levels 1–2 are tasks, 3–4 goals and 5 a challenge, which also gets the challenge colour and fanfare in the toast.
 * 成就使用的边框，对应原版的三种进度边框，由等级决定：1–2 级为任务，3–4 级为目标，5 级为挑战，挑战的弹窗还会使用挑战颜色并播放号角声。
 */
public enum AchievementFrame {
    TASK,
    GOAL,
    CHALLENGE;

    /** The frame for a level from 1 to 5. 1 到 5 级对应的边框。 */
    public static AchievementFrame forLevel(int level) {
        if (level < Achievement.MIN_LEVEL || level > Achievement.MAX_LEVEL) {
            throw new IllegalArgumentException("level " + level);
        }
        return level <= 2 ? TASK : level <= 4 ? GOAL : CHALLENGE;
    }
}
