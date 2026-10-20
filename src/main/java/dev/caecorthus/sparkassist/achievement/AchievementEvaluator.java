package dev.caecorthus.sparkassist.achievement;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Finds the achievements one settled round earns in its place. Each place earns an achievement once, so one earned
 * only elsewhere can be earned here, and so can an expired 微小胜利 ({@link AchievementStanding}). A condition that
 * throws is skipped for this round and reported, so one broken rule never blocks the others. A condition may ask
 * whether other achievements are unlocked anywhere; those this round earns count too, so the conditions are tested
 * again until nothing new is earned.
 * 找出一局结算后在该地点新达成的成就。每个地点各能获得一次同一成就，所以只在别处获得过的成就在这里还能再获得，
 * 过期的微小胜利也一样（{@link AchievementStanding}）。抛出异常的条件在本局被跳过并上报，一条规则出错不会影响其他规则。
 * 条件可以查询其他成就是否在任何地点解锁过，本局新达成的也算在内，因此会反复判定，直到不再有新达成的成就。
 */
public final class AchievementEvaluator {
    private AchievementEvaluator() {
    }

    /**
     * Achievements newly earned in {@code place} (null when unknown), in catalog order.
     * 在 {@code place}（未知时为 null）新达成的成就，按目录顺序。
     */
    public static List<Achievement> newlyEarned(
            List<Achievement> catalog,
            AchievementContext context,
            AchievementLedger.Place place,
            BiConsumer<Achievement, RuntimeException> onError
    ) {
        Set<String> earnedHere = AchievementStanding.earnedAt(catalog, context.ledger(), place);
        Set<String> earned = new LinkedHashSet<>();
        Set<String> failed = new HashSet<>();
        boolean earnedMore = true;
        while (earnedMore) {
            earnedMore = false;
            AchievementContext seen = context.withEarnedThisRound(earned);
            for (Achievement achievement : catalog) {
                String id = achievement.id();
                if (earnedHere.contains(id) || earned.contains(id) || failed.contains(id)) {
                    continue;
                }
                try {
                    if (achievement.condition().test(seen)) {
                        earned.add(id);
                        earnedMore = true;
                    }
                } catch (RuntimeException exception) {
                    failed.add(id);
                    onError.accept(achievement, exception);
                }
            }
        }
        return catalog.stream().filter(achievement -> earned.contains(achievement.id())).toList();
    }
}
