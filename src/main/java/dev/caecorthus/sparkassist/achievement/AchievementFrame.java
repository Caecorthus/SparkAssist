package dev.caecorthus.sparkassist.achievement;

/**
 * The frame an achievement is drawn with, matching vanilla's three advancement frames; it also picks the toast
 * title ("达成进度！" / "达成目标！" / "完成挑战！") and, for a challenge, the fanfare.
 * 成就使用的边框，对应原版的三种进度边框；同时决定弹窗标题（“达成进度！”/“达成目标！”/“完成挑战！”），挑战还会播放号角声。
 */
public enum AchievementFrame {
    TASK,
    GOAL,
    CHALLENGE
}
