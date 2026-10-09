package dev.caecorthus.sparkassist.achievement;

/**
 * Living, dying and the roles held, as the client saw them (no match record needed).
 * 存活、死亡与担任过的身份，以客户端所见为准（不需要对局记录）。
 */
final class RoundRules {
    static final String WRAITH = "sparkwitch:wraith";
    /** The Wraith's id while SparkTraits hosted it (older servers). SparkTraits 托管冤魂时的 id（旧服务器）。 */
    static final String LEGACY_WRAITH = "sparktraits:wraith";
    /** 60 seconds. 60 秒。 */
    static final long FIRST_MINUTE_TICKS = 1200;

    private RoundRules() {
    }

    static boolean survived(AchievementContext context) {
        return !context.facts().died();
    }

    static boolean diedInFirstMinute(AchievementContext context) {
        return context.facts().died() && context.facts().deathTick() <= FIRST_MINUTE_TICKS;
    }

    /** Whether I died this round, seen by the client or in the record. 本局我是否死亡（客户端所见或记录中）。 */
    static boolean died(AchievementContext context) {
        return context.facts().died() || context.death().isPresent();
    }

    static boolean becameWraith(AchievementContext context) {
        return context.facts().roleChain().stream().anyMatch(RoundRules::isWraith)
                || context.timeline().roleHistory(context.self()).stream().anyMatch(RoundRules::isWraith);
    }

    private static boolean isWraith(String roleId) {
        return WRAITH.equals(roleId) || LEGACY_WRAITH.equals(roleId);
    }
}
