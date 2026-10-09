package dev.caecorthus.sparkassist.achievement;

/**
 * Food and drink: over the account's lifetime ({@link LifetimeStats#consumed}, this round already counted) and within
 * one round ({@code sparkfactionapi:consume}).
 * 食物与饮品：账号累计（{@link LifetimeStats#consumed}，已计入本局）与单局内（{@code sparkfactionapi:consume}）。
 */
final class ConsumptionRules {
    static final String FOOD = "food";
    static final String DRINK = "drink";
    static final int PLENTY = 10;

    private ConsumptionRules() {
    }

    static boolean lifetimeFood(AchievementContext context) {
        return context.stats().consumed(FOOD) >= PLENTY;
    }

    static boolean lifetimeDrinks(AchievementContext context) {
        return context.stats().consumed(DRINK) >= PLENTY;
    }

    static boolean roundFood(AchievementContext context) {
        return thisRound(context, FOOD) >= PLENTY;
    }

    static boolean roundDrinks(AchievementContext context) {
        return thisRound(context, DRINK) >= PLENTY;
    }

    static boolean roundFoodThenDied(AchievementContext context) {
        return roundFood(context) && RoundRules.died(context);
    }

    static boolean roundDrinksThenDied(AchievementContext context) {
        return roundDrinks(context) && RoundRules.died(context);
    }

    private static long thisRound(AchievementContext context, String kind) {
        return context.myEvents(MatchEvent.CONSUME).stream()
                .filter(consume -> kind.equals(consume.value("kind")))
                .count();
    }
}
