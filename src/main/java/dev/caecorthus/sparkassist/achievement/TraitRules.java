package dev.caecorthus.sparkassist.achievement;

/**
 * SparkTraits traits. A trait "held at a kill" is checked against my owner-visible trait timeline within
 * {@link RoundFacts#RECORD_TICK_TOLERANCE} of the kill's record tick.
 * SparkTraits 词条。“击杀时持有的词条”按击杀的记录 tick 前后 {@link RoundFacts#RECORD_TICK_TOLERANCE} 以内，对照我自身可见的词条时间线判断。
 */
final class TraitRules {
    static final String HEAVY_ARTILLERY = "sparktraits:heavy_artillery";
    static final String FAST_HANDS = "sparktraits:fast_hands";
    static final String FAST_RELOAD = "sparktraits:fast_reload";
    static final String NIKO = "sparktraits:niko";
    static final String LAST_STAND_TRIGGERED = "sparktraits:last_stand_triggered";
    static final String CLOSE_QUARTERS_PARRY = "sparktraits:close_quarters_parry";
    static final String IMPOSTOR = "sparktraits:impostor";
    static final String MONEY_TREE = "sparktraits:money_tree";
    static final String TASK_MASTER = "sparktraits:task_master";
    static final String REVOLVER = "wathe:revolver";
    static final String VETERAN = "wathe:veteran";
    /** 60 seconds. 60 秒。 */
    static final int RECENT_KILL_TICKS = 1200;
    static final double LONG_SHOT_BLOCKS = 35;
    static final int MANY_OPPONENTS = 4;
    static final int TURNCOAT_KILLS = 4;
    static final int FORTUNE_COINS = 200;

    private TraitRules() {
    }

    /** 七步之内: a kill of a player in psycho mode while holding Heavy Artillery. 持有重炮手时击杀疯魔中的玩家。 */
    static boolean killPsychoWithHeavyArtillery(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> kill.bool("victim_psycho")
                && context.hadTraitAround(HEAVY_ARTILLERY, kill.tick()));
    }

    /**
     * 反欧美: with Fast Hands and Fast Reload at once, a kill of a 非好人 who had killed someone within the last 60 s.
     * 同时持有快手与快速装填时，击杀一名 60 秒内有过击杀的非好人。
     */
    static boolean killRecentKillerWithFastHandsAndReload(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> Rules.victimNonCivilian(context, kill)
                && Rules.heldTogetherAround(context, kill.tick(), FAST_HANDS, FAST_RELOAD)
                && context.timeline().killsBy(kill.target()).stream()
                        .anyMatch(theirs -> theirs.tick() > kill.tick() - RECENT_KILL_TICKS && theirs.tick() <= kill.tick()));
    }

    /** 超远打击: a revolver kill of a 非好人 35+ blocks away while holding Niko. 持有 Niko 时用左轮击杀 35 格及以外的非好人。 */
    static boolean longRevolverKillWithNiko(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> REVOLVER.equals(kill.value("killer_item"))
                && kill.doubleValue("distance", 0) >= LONG_SHOT_BLOCKS
                && Rules.victimNonCivilian(context, kill)
                && context.hadTraitAround(NIKO, kill.tick()));
    }

    /** 抑郁: a kill, while in psycho, of a player whose attack sent me into Depression. 疯魔中击杀使我陷入抑郁的人。 */
    static boolean killDepressionAttacker(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> kill.target() != null && kill.bool("killer_psycho")
                && context.facts().depressionAttackers().contains(kill.target()));
    }

    static boolean triggeredLastStand(AchievementContext context) {
        return !Rules.myGlobalEvents(context, LAST_STAND_TRIGGERED).isEmpty();
    }

    /** Last Stand with 4+ 非好人 still alive, and the round won. 背水一战触发时仍有四名及以上非好人存活，且最终获胜。 */
    static boolean wonLastStandAgainstMany(AchievementContext context) {
        return context.won() && Rules.myGlobalEvents(context, LAST_STAND_TRIGGERED).stream()
                .anyMatch(trigger -> trigger.intValue("alive_opponents", 0) >= MANY_OPPONENTS);
    }

    /**
     * 盲目自信: Close Quarters parried a Veteran. An older record without {@code attacker_role} falls back to the
     * attacker's role in the timeline.
     * 狭路相逢挡下老兵的攻击。没有 {@code attacker_role} 的旧记录改用时间线里攻击者的身份。
     */
    static boolean parriedVeteran(AchievementContext context) {
        return context.myEvents(CLOSE_QUARTERS_PARRY).stream().anyMatch(parry -> VETERAN.equals(
                parry.value("attacker_role", parry.target() == null ? null : context.roleAt(parry.target(), parry))));
    }

    /**
     * 叛无所依: 4 kills, each while holding Impostor, of players whose faction at death is known and is not the killer
     * faction (好人, neutrals and witches all count; {@link Factions#NONE} is no faction).
     * 每次都在持有内鬼时，击杀 4 名死亡时阵营已知且不是杀手阵营的玩家（好人、中立、魔女都算；{@link Factions#NONE} 不算阵营）。
     */
    static boolean impostorKilledFourNonKillers(AchievementContext context) {
        return Rules.myKills(context, kill -> victimNonKiller(context, kill)
                && context.hadTraitAround(IMPOSTOR, kill.tick())).size() >= TURNCOAT_KILLS;
    }

    private static boolean victimNonKiller(AchievementContext context, MatchEvent kill) {
        String faction = context.timeline().victimFaction(kill);
        return (Factions.isCivilian(faction) || Factions.isNonCivilian(faction)) && !Factions.KILLER.equals(faction);
    }

    /**
     * 来财: a 好人 who held Money Tree and Task Master at the same moment and gained 200 coins this round.
     * 本局作为好人，曾同时持有摇钱树与任务大师，并获得 200 金币。
     */
    static boolean civilianFortune(AchievementContext context) {
        return Factions.isCivilian(context.faction())
                && Rules.heldTogetherSometime(context, MONEY_TREE, TASK_MASTER)
                && context.myEvents(MatchEvent.INCOME).stream()
                        .anyMatch(income -> income.intValue("amount", 0) >= FORTUNE_COINS);
    }
}
