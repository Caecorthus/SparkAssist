package dev.caecorthus.sparkassist.achievement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every achievement SparkAssist knows. Players never see this list: an achievement appears only once earned.
 * Empty until the real achievements are added; nothing can be earned meanwhile.
 * SparkAssist 已知的全部成就。玩家看不到这份列表：成就只有在达成后才会出现。
 * 在正式成就加入之前为空，期间不会达成任何成就。
 */
public final class AchievementCatalog {
    private static final List<Achievement> ALL = List.of();
    private static final Map<String, Achievement> BY_ID = index(ALL);

    private AchievementCatalog() {
    }

    public static List<Achievement> all() {
        return ALL;
    }

    public static Optional<Achievement> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    static Map<String, Achievement> index(List<Achievement> achievements) {
        Map<String, Achievement> map = new LinkedHashMap<>();
        for (Achievement achievement : achievements) {
            if (map.put(achievement.id(), achievement) != null) {
                throw new IllegalStateException("Duplicate achievement id " + achievement.id());
            }
        }
        return Map.copyOf(map);
    }
}
