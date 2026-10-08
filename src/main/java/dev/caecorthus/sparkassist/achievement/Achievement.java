package dev.caecorthus.sparkassist.achievement;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * One hidden achievement. The id is written to players' save files, so it must never change once released; the
 * title, description, icon and condition may. The condition sees one settled round and is tested only while the
 * achievement is still locked.
 * 一个隐藏成就。id 会写进玩家的存档，发布后绝不能修改；标题、描述、图标和条件可以改。
 * 条件看到的是一局已结算的对局，只在成就尚未解锁时才会判定。
 *
 * @param icon an item id such as {@code minecraft:clock}; unknown ids fall back to paper
 *             物品 id，例如 {@code minecraft:clock}；未知 id 会显示为纸
 */
public record Achievement(
        String id,
        AchievementFrame frame,
        String icon,
        String title,
        String description,
        Predicate<AchievementContext> condition
) {
    private static final Pattern ID = Pattern.compile("[a-z0-9_]+");

    public Achievement {
        Objects.requireNonNull(id, "id");
        if (!ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Achievement id must match [a-z0-9_]+: " + id);
        }
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(condition, "condition");
    }
}
