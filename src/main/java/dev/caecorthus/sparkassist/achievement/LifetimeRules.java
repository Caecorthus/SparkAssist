package dev.caecorthus.sparkassist.achievement;

import java.util.Set;

/**
 * Counts kept across rounds in {@link LifetimeStats}, with this round already counted: wins per faction and per
 * role, ways of dying, roles played, tasks and win streaks. Streaks only move on a settled round, so a round left
 * mid-way neither counts nor breaks one (an intended leniency).
 * 跨局保存在 {@link LifetimeStats} 中的累计数据（已计入本局）：按阵营和身份的胜场、死法、玩过的身份、任务数与连胜。
 * 连胜只在一局结算时变化，所以中途退出的对局既不计入也不打断连胜（有意从宽）。
 */
final class LifetimeRules {
    static final int DEATH_WAYS = 10;
    static final int ROLES_PLAYED = 40;
    static final int TASKS = 996;
    static final int WIN_STREAK = 5;
    static final int SURVIVED_WIN_STREAK = 4;
    static final int KILLER_SURVIVED_WIN_STREAK = 3;
    static final int NEUTRAL_ROLES_WON = 5;

    /**
     * The neutral roles that can win a round of their own: Noelle's Roles' Jester, Shadow Jester, Vulture, Corrupt
     * Cop and Pathogen, SparkStrength's Taotie, and SparkWitch's Fiend, Insider and Murderous Witch.
     * 能自己赢下对局的中立身份：Noelle's Roles 的小丑、影子小丑、秃鹫、黑警、病原体，SparkStrength 的饕餮，
     * 以及 SparkWitch 的魔人、内应、杀意魔女。
     */
    static final Set<String> WINNING_NEUTRAL_ROLES = Set.of(
            "noellesroles:jester",
            "noellesroles:shadow_jester",
            "noellesroles:vulture",
            "noellesroles:corrupt_cop",
            "noellesroles:pathogen",
            "noellesroles:taotie",
            "sparkwitch:fiend",
            "sparkwitch:insider",
            "sparkwitch:murderous_witch");

    private LifetimeRules() {
    }

    /**
     * 墙头草: won at least once on each of the civilian, killer, neutral and witch factions, by my effective faction
     * at the end of the round. The Murderous Witch is a neutral role on her own faction
     * ({@link Factions#MURDEROUS_WITCH}), so her wins count as neutral.
     * 以好人、杀手、中立、魔女阵营各获胜至少一次，按一局结束时我的实际阵营计。杀意魔女是自成一个阵营
     * （{@link Factions#MURDEROUS_WITCH}）的中立身份，她的胜场算作中立。
     */
    static boolean wonForEveryFaction(AchievementContext context) {
        LifetimeStats stats = context.stats();
        return stats.winsFor(Factions.CIVILIAN) >= 1
                && stats.winsFor(Factions.KILLER) >= 1
                && stats.winsFor(Factions.NEUTRAL) + stats.winsFor(Factions.MURDEROUS_WITCH) >= 1
                && stats.winsFor(Factions.WITCH) >= 1;
    }

    /**
     * 蠢蠢的死法: died in 10 different ways (death reasons), counted across rounds; a disconnect is not a way to die.
     * 累计以 10 种不同的死因死亡；断线不算死法。
     */
    static boolean diedManyWays(AchievementContext context) {
        return context.stats().deathReasons().size() >= DEATH_WAYS;
    }

    /**
     * 百变星君: played rounds as 40 different roles, every role held in a round counting (a role taken mid-round too).
     * 累计以 40 个不同身份参加对局，一局中担任过的每个身份都算（包括中途变成的身份）。
     */
    static boolean playedManyRoles(AchievementContext context) {
        return context.stats().roleRounds().size() >= ROLES_PLAYED;
    }

    /**
     * 福报: 996 tasks finished, counted across rounds from Wathe's task-complete notices.
     * 累计完成 996 个任务，按 Wathe 的任务完成通知跨局累计。
     */
    static boolean manyTasksOverall(AchievementContext context) {
        return context.stats().tasks() >= TASKS;
    }

    /** 赢麻了: 5 settled rounds won in a row, alive or not. 连续获胜 5 局，不论存活。 */
    static boolean wonFiveInARow(AchievementContext context) {
        return context.stats().winStreak() >= WIN_STREAK;
    }

    /**
     * 无伤通关: 4 settled rounds in a row survived and won. Any death breaks it, a Last Stand false death and a death
     * undone by the T-Virus included.
     * 连续 4 局存活并获胜。任何一次死亡都会打断，包括背水一战的假死和被 T 病毒复活的死亡。
     */
    static boolean survivedFourWinsInARow(AchievementContext context) {
        return context.stats().survivedWinStreak(LifetimeStats.ALL_ROUNDS) >= SURVIVED_WIN_STREAK;
    }

    /**
     * 逍遥法外: 3 rounds in a row survived and won on the killer faction. Only rounds that ended (or in which I died)
     * on the killer faction move it: rounds on another side leave it as it is, dying or losing as a killer resets it.
     * 连续 3 次以杀手阵营存活并获胜。只有结束时（或死亡时）在杀手阵营的对局才会推进或打断它：其他阵营的对局不影响，
     * 当杀手时死亡或输掉则重来。
     */
    static boolean survivedThreeKillerWinsInARow(AchievementContext context) {
        return context.stats().survivedWinStreak(RoleGroups.KILLER) >= KILLER_SURVIVED_WIN_STREAK;
    }

    /**
     * 永久中立国: won as 5 different {@link #WINNING_NEUTRAL_ROLES}; a win counts for the role I ended the round as.
     * 以 5 个不同的 {@link #WINNING_NEUTRAL_ROLES} 身份各获胜一次；胜场记在结束这一局时的身份上。
     */
    static boolean wonAsManyNeutralRoles(AchievementContext context) {
        return WINNING_NEUTRAL_ROLES.stream().filter(role -> context.stats().winsAs(role) >= 1).count()
                >= NEUTRAL_ROLES_WON;
    }
}
