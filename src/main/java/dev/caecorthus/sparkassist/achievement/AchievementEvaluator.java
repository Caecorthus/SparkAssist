package dev.caecorthus.sparkassist.achievement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Finds the achievements one settled round earns. A condition that throws is skipped for this round and reported,
 * so one broken rule never blocks the others.
 * 找出一局结算后新达成的成就。抛出异常的条件在本局被跳过并上报，一条规则出错不会影响其他规则。
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
        List<Achievement> earned = new ArrayList<>();
        for (Achievement achievement : catalog) {
            if (context.ledger().isUnlocked(achievement.id())) {
                continue;
            }
            try {
                if (achievement.condition().test(context)) {
                    earned.add(achievement);
                }
            } catch (RuntimeException exception) {
                onError.accept(achievement, exception);
            }
        }
        return List.copyOf(earned);
    }
}
