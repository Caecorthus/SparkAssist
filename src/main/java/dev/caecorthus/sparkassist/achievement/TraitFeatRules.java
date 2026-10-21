package dev.caecorthus.sparkassist.achievement;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Feats with one SparkTraits trait, and the trait collections kept in {@link LifetimeStats}. A trait "held" at an
 * event is checked against my owner-visible trait timeline within {@link RoundFacts#RECORD_TICK_TOLERANCE} of the
 * event's record tick. A shield, as SparkTraits' Second Strike counts them, is psycho armour or any other
 * {@code shield_blocked} protection, an Iron Man Vial buff or a whiskey shield layer; Noelle's Roles writes the last two
 * as {@code death_blocked}.
 * 与单个 SparkTraits 词条相关的成就，以及 {@link LifetimeStats} 中的词条收集。某事件发生时“持有”的词条，按该事件记录 tick 前后
 * {@link RoundFacts#RECORD_TICK_TOLERANCE} 以内对照我自身可见的词条时间线判断。护盾按 SparkTraits 二次打击的口径：疯魔护甲或其他
 * 写成 {@code shield_blocked} 的保护、铁人药剂增益或一层威士忌护盾；后两者由 Noelle's Roles 写成 {@code death_blocked}。
 */
final class TraitFeatRules {
    static final String CONSCIENCE = "sparktraits:conscience";
    static final String EXHILARATED = "sparktraits:exhilarated";
    static final String SECOND_STRIKE = "sparktraits:second_strike";
    static final String CORNERED = "sparktraits:cornered";
    static final String PARANOID = "sparktraits:paranoid";
    static final String PIG = "sparktraits:pig";
    static final String THRUST = "sparktraits:thrust";

    /** The 17 killer traits plus Conscience (善良), the 18 a killer can hold. 17 个杀手词条加上善良，共 18 个杀手可持有的词条。 */
    static final List<String> KILLER_TRAITS = List.of(
            "sparktraits:bloodthirsty",
            "sparktraits:the_showman",
            "sparktraits:plunderer",
            "sparktraits:charisma",
            PARANOID,
            THRUST,
            SECOND_STRIKE,
            "sparktraits:oppressive",
            CORNERED,
            "sparktraits:team_first",
            EXHILARATED,
            "sparktraits:close_quarters",
            "sparktraits:last_escape",
            "sparktraits:herculean_strength",
            "sparktraits:master_saboteur",
            "sparktraits:seasoned",
            "sparktraits:manic",
            CONSCIENCE);
    /**
     * Marksman, Fast Reload, Heavy Artillery, Niko, Well Trained and Going Dark (the last two are the Veteran's).
     * 精确枪手、快速装填、重炮手、Niko、训练有素与隐蔽行动（后两个是老兵专属）。
     */
    static final List<String> POLICE_TRAITS = List.of(
            "sparktraits:marksman",
            "sparktraits:fast_reload",
            TraitRules.HEAVY_ARTILLERY,
            TraitRules.NIKO,
            "sparktraits:well_trained",
            "sparktraits:going_dark");

    static final String FELL_OUT_OF_TRAIN = "wathe:fell_out_of_train";
    static final String DEATH_BLOCKED = "death_blocked";
    static final Set<String> SHIELD_BLOCK_REASONS = Set.of("iron_man_buff", "whiskey_shield");

    /** 5 seconds. 5 秒。 */
    static final int RAMPAGE_GAP_TICKS = 100;
    static final int RAMPAGE_KILLS = 3;
    static final int SHIELDED_ATTACKS = 2;
    static final int PIG_MEALS = 30;

    private TraitFeatRules() {
    }

    /** 放下屠刀: with Conscience, kill a killer-faction player. 持有善良时击杀一名杀手阵营玩家。 */
    static boolean conscienceKillsKiller(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> Factions.KILLER.equals(context.timeline().victimFaction(kill))
                && context.hadTraitAround(CONSCIENCE, kill.tick()));
    }

    /**
     * 杀疯了: 3 kills of different players in a row, each within 5 s of the one before and each while holding
     * Exhilarated. A Last Stand false death is no kill here (Exhilarated itself ignores it).
     * 连续击杀 3 名不同的玩家，每次都在上一次击杀后 5 秒内，且每次都持有亢奋。背水一战的假死在这里不算击杀（亢奋本身也不计它）。
     */
    static boolean exhilaratedRampage(AchievementContext context) {
        Set<UUID> chain = new HashSet<>();
        int lastTick = Integer.MIN_VALUE;
        for (MatchEvent kill : context.kills()) {
            if (context.timeline().isFakeDeath(kill)) {
                continue;
            }
            if (!context.hadTraitAround(EXHILARATED, kill.tick())) {
                chain.clear();
                lastTick = Integer.MIN_VALUE;
                continue;
            }
            if (lastTick == Integer.MIN_VALUE || kill.tick() - lastTick > RAMPAGE_GAP_TICKS) {
                chain.clear();
            }
            chain.add(kill.target());
            lastTick = kill.tick();
            if (chain.size() >= RAMPAGE_KILLS) {
                return true;
            }
        }
        return false;
    }

    /**
     * 破防了: with Second Strike, my attack on a player is stopped by their shield and that same attack still kills
     * them: their shield block against me, then my kill of them, in the same tick. A Last Stand false death does not
     * kill them: they come back.
     * 持有二次打击时，我对一名玩家的攻击被其护盾挡下，而这一击仍然杀死了对方：同一 tick 内先有其对我的护盾抵挡，再有我对其的击杀。
     * 背水一战的假死不算杀死：对方会复活。
     */
    static boolean killThroughShield(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        List<MatchEvent> events = timeline.events();
        for (int i = 0; i < events.size(); i++) {
            MatchEvent kill = events.get(i);
            if (!kill.is(MatchEvent.DEATH) || self == null || !self.equals(kill.actor()) || kill.target() == null
                    || self.equals(kill.target()) || timeline.isFakeDeath(kill)
                    || !context.hadTraitAround(SECOND_STRIKE, kill.tick())) {
                continue;
            }
            for (int j = i - 1; j >= 0 && events.get(j).tick() == kill.tick(); j--) {
                MatchEvent block = events.get(j);
                if (isShieldBlock(block) && kill.target().equals(block.actor()) && self.equals(block.target())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 孤勇者: with Cornered, outlive every other killer-faction teammate and win as the only living killer. Needs the
     * complete record: there was at least one other player ever on the killer faction, all of them are dead by the
     * end, I was alive and held Cornered when the last of them died, I won on the killer faction alive, and no other
     * participant both survived and won (that also catches a living Impostor, whom the record cannot see).
     * 持有走投无路时，熬过所有其他杀手阵营队友，并作为唯一存活的杀手获胜。需要完整记录：至少有一名其他玩家曾属于杀手阵营，
     * 他们到结束时全部死亡，最后一人死亡时我存活且持有走投无路，我以杀手阵营存活获胜，且没有其他参与者既存活又获胜
     * （这也能排除记录看不到的存活内鬼）。
     */
    static boolean lastKillerStanding(AchievementContext context) {
        MatchTimeline timeline = context.timeline();
        UUID self = context.self();
        if (!timeline.isComplete() || self == null || !context.won() || !Factions.KILLER.equals(context.faction())) {
            return false;
        }
        MatchEvent end = timeline.matchEnd().orElseThrow();
        Set<UUID> aliveAtEnd = timeline.aliveAfter(end);
        Set<UUID> teammates = new HashSet<>(timeline.playersEverIn(Factions.KILLER::equals));
        teammates.remove(self);
        if (teammates.isEmpty() || !aliveAtEnd.contains(self) || teammates.stream().anyMatch(aliveAtEnd::contains)
                || otherLivingWinner(context)) {
            return false;
        }
        MatchEvent lastTeammateDeath = null;
        for (MatchEvent death : timeline.realDeaths()) {
            if (teammates.contains(death.target())) {
                lastTeammateDeath = death;
            }
        }
        return lastTeammateDeath != null && timeline.aliveAfter(lastTeammateDeath).contains(self)
                && context.hadTraitAround(CORNERED, lastTeammateDeath.tick());
    }

    /**
     * 总有刁民想害朕: with Paranoid, shields stop 2 attacks on me within one psycho mode. Blocks in one tick from one
     * attacker (a piercing shot or a Second Strike replay) are one attack.
     * 持有偏执狂时，同一次疯魔中护盾为我挡下 2 次攻击。同一 tick 内来自同一攻击者的多次抵挡（穿透射击或二次打击的重算）算一次攻击。
     */
    static boolean paranoidShieldsTwice(AchievementContext context) {
        UUID self = context.self();
        if (self == null) {
            return false;
        }
        List<MatchEvent> blocks = context.timeline().events().stream()
                .filter(event -> isShieldBlock(event) && self.equals(event.actor())
                        && context.hadTraitAround(PARANOID, event.tick()))
                .toList();
        return context.timeline().psychoWindows(self).stream().anyMatch(window -> blocks.stream()
                .filter(block -> window.contains(block.tick()))
                .map(block -> block.tick() + "/" + block.target())
                .distinct().count() >= SHIELDED_ATTACKS);
    }

    /** 猪猪侠: with Pig, kill a 非好人 while on the civilian side. 持有猪时以好人身份击杀一名非好人。 */
    static boolean pigKillsOtherSide(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> Factions.isCivilian(context.timeline().killerFaction(kill))
                && Rules.victimNonCivilian(context, kill) && context.hadTraitAround(PIG, kill.tick()));
    }

    /**
     * 你可以下车了: with Thrust, a player whose faction is known and is not the killer faction falls off the train
     * with the kill credited to me. Wathe credits a fall to the victim's last attacker within 5 s, so a shove counts;
     * the record cannot tell a shove with Thrust's extra knockback from any other.
     * 持有突刺时，一名阵营已知且不是杀手阵营的玩家坠车身亡且击杀记在我名下。Wathe 把坠车记给 5 秒内最后攻击死者的人，
     * 因此推人算数；记录分不出这一推是否带有突刺的额外击退。
     */
    static boolean thrustPushOff(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> Rules.reason(kill, FELL_OUT_OF_TRAIN)
                && victimNonKiller(context, kill) && context.hadTraitAround(THRUST, kill.tick()));
    }

    /**
     * 十八般武艺: across rounds, as one of the 21 killer roles, killed a player of another faction while holding each of
     * the 18 killer traits ({@link LifetimeStats#rivalKillTraitsAs}).
     * 累计以 21 个杀手身份之一，分别在持有 18 个杀手词条中的每一个时击杀过其他阵营的玩家（{@link LifetimeStats#rivalKillTraitsAs}）。
     */
    static boolean everyKillerTrait(AchievementContext context) {
        return heldAtRivalKills(context, RoleGroups.KILLER_ROLES).containsAll(KILLER_TRAITS);
    }

    /**
     * 六边形战士: across rounds, as a police role, killed a 非好人 while holding each of the 6 police traits. Police
     * are always on the civilian side, so a kill of another faction is a kill of a 非好人.
     * 累计以警职身份，分别在持有 6 个警职词条中的每一个时击杀过非好人。警职永远站在好人一方，所以击杀其他阵营即击杀非好人。
     */
    static boolean everyPoliceTrait(AchievementContext context) {
        return heldAtRivalKills(context, RoleGroups.POLICE_ROLES).containsAll(POLICE_TRAITS);
    }

    /**
     * 二师兄: across rounds, 30 meals eaten while holding Pig (the Pig God's forced Pig is the same trait).
     * 累计在持有猪时吃下 30 份食物（皮革噶开局强制带的猪是同一个词条）。
     */
    static boolean pigMeals(AchievementContext context) {
        return context.stats().foodWithTrait(PIG) >= PIG_MEALS;
    }

    // ---- Helpers 辅助 ----

    /**
     * A shield stopping a lethal attack on {@code actor} (the attacker is {@code target} when known).
     * 一次护盾挡下了对 {@code actor} 的致命攻击（已知攻击者时为 {@code target}）。
     */
    static boolean isShieldBlock(MatchEvent event) {
        return event.is(MatchEvent.SHIELD_BLOCKED)
                || event.is(DEATH_BLOCKED) && SHIELD_BLOCK_REASONS.contains(event.value("block_reason", ""));
    }

    private static boolean victimNonKiller(AchievementContext context, MatchEvent kill) {
        String faction = context.timeline().victimFaction(kill);
        return (Factions.isCivilian(faction) || Factions.isNonCivilian(faction)) && !Factions.KILLER.equals(faction);
    }

    /**
     * Whether another participant than me ended the round alive and among the winners.
     * 是否有我以外的参与者存活到结束且属于获胜方。
     */
    private static boolean otherLivingWinner(AchievementContext context) {
        return context.outcome().participants().stream().anyMatch(participant -> participant.winner()
                && "ALIVE".equals(participant.endStatus()) && !participant.uuid().equals(context.self()));
    }

    private static Set<String> heldAtRivalKills(AchievementContext context, Set<String> roles) {
        Set<String> held = new HashSet<>();
        Map<String, Set<String>> byRole = context.stats().rivalKillTraits();
        roles.forEach(role -> held.addAll(byRole.getOrDefault(role, Set.of())));
        return held;
    }
}
