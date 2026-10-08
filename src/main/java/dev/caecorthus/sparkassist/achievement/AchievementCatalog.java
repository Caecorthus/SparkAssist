package dev.caecorthus.sparkassist.achievement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every achievement SparkAssist knows. Players never see this list: an achievement appears only once earned.
 * The two entries below are placeholders for checking the display in game; the real set is still being designed.
 * SparkAssist 已知的全部成就。玩家看不到这份列表：成就只有在达成后才会出现。
 * 下面两条只是用来在游戏里检查显示效果的占位成就，正式内容仍在设计中。
 */
public final class AchievementCatalog {
    private static final List<Achievement> ALL = List.of(
            new Achievement(
                    "first_round",
                    AchievementFrame.TASK,
                    "minecraft:minecart",
                    "初次发车",
                    "完整地坐完一趟车。",
                    context -> true),
            new Achievement(
                    "first_win",
                    AchievementFrame.GOAL,
                    "minecraft:clock",
                    "抵达终点",
                    "赢下一局。",
                    AchievementContext::won)
    );
    private static final Map<String, Achievement> BY_ID = index(ALL);

    private AchievementCatalog() {
    }

    public static List<Achievement> all() {
        return ALL;
    }

    public static Optional<Achievement> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    private static Map<String, Achievement> index(List<Achievement> achievements) {
        Map<String, Achievement> map = new LinkedHashMap<>();
        for (Achievement achievement : achievements) {
            if (map.put(achievement.id(), achievement) != null) {
                throw new IllegalStateException("Duplicate achievement id " + achievement.id());
            }
        }
        return Map.copyOf(map);
    }
}
