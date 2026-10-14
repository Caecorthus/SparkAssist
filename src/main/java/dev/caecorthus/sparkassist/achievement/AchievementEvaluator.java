package dev.caecorthus.sparkassist.achievement;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Finds the achievements one settled round earns. A condition that throws is skipped for this round and reported,
 * so one broken rule never blocks the others. A condition may ask whether other achievements are unlocked; those
 * this round earns count too, so the conditions are tested again until nothing new is earned.
 * 找出一局结算后新达成的成就。抛出异常的条件在本局被跳过并上报，一条规则出错不会影响其他规则。
 * 条件可以查询其他成就是否已解锁，本局新达成的也算在内，因此会反复判定，直到不再有新达成的成就。
 */
public final class AchievementEvaluator {
    private AchievementEvaluator() {
    }

    /** Newly earned achievements, in catalog order. 新达成的成就，按目录顺序。 */
    public static List<Achievement> newlyEarned(
            List<Achievement> catalog,
            AchievementContext context,
            BiConsumer<Achievement, RuntimeException> onError
    ) {
        Set<String> earned = new LinkedHashSet<>();
        Set<String> failed = new HashSet<>();
        boolean earnedMore = true;
        while (earnedMore) {
            earnedMore = false;
            AchievementContext seen = context.withEarnedThisRound(earned);
            for (Achievement achievement : catalog) {
                if (seen.isUnlocked(achievement.id()) || failed.contains(achievement.id())) {
                    continue;
                }
                try {
                    if (achievement.condition().test(seen)) {
                        earned.add(achievement.id());
                        earnedMore = true;
                    }
                } catch (RuntimeException exception) {
                    failed.add(achievement.id());
                    onError.accept(achievement, exception);
                }
            }
        }
        return catalog.stream().filter(achievement -> earned.contains(achievement.id())).toList();
    }
}
