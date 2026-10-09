package dev.caecorthus.sparkassist.achievement;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * One hidden achievement. The id is written to players' save files, so it must never change once released; the
 * level, title, description, icon and condition may. The condition sees one settled round and is tested only while
 * the achievement is still locked.
 * 一个隐藏成就。id 会写进玩家的存档，发布后绝不能修改；等级、标题、描述、图标和条件可以改。
 * 条件看到的是一局已结算的对局，只在成就尚未解锁时才会判定。
 *
 * @param level difficulty from {@link #MIN_LEVEL} to {@link #MAX_LEVEL}; it picks the frame and is shown as
 *              "Level x" on the page and in the toast
 *              难度，从 {@link #MIN_LEVEL} 到 {@link #MAX_LEVEL}；决定边框，并在成就页和弹窗里显示为“Level x”
 * @param icon  an item id such as {@code minecraft:clock}; unknown ids fall back to paper
 *              物品 id，例如 {@code minecraft:clock}；未知 id 会显示为纸
 */
public record Achievement(
        String id,
        int level,
        String icon,
        String title,
        String description,
        Predicate<AchievementContext> condition
) {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 5;
    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");

    public Achievement {
        Objects.requireNonNull(id, "id");
        if (!ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Achievement id must match [a-z0-9_]+: " + id);
        }
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("Achievement level must be 1-5: " + id + " has " + level);
        }
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(condition, "condition");
    }

    /** The frame this level is drawn with. 该等级使用的边框。 */
    public AchievementFrame frame() {
        return AchievementFrame.forLevel(level);
    }
}
