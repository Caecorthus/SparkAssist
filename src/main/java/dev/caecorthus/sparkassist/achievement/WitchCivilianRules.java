package dev.caecorthus.sparkassist.achievement;

import java.util.List;
import java.util.UUID;

/**
 * SparkWitch civilian and police roles: the Blind, the Fisher, the Perfumer, the Orthopedist, the Saint, the Tarot
 * Reader, Emma, the Control Expert and the Seeker, plus the police-role kill collection. "As role X" is my real role
 * at that event from the match record, so a Black Raven disguised as X never counts.
 * SparkWitch 好人与警职身份：盲人、钓鱼佬、调香师、骨科大夫、圣徒、塔罗牌师、樱羽艾玛、控场专家与搜寻者，以及警职击杀收集。
 * “以 X 身份”指对局记录中该事件发生时我的真实身份，因此伪装成 X 的黑羽鸦永远不算。
 */
final class WitchCivilianRules {
    static final String BLIND = "sparkwitch:blind";
    static final String FISHER = "sparkwitch:fisher";
    static final String PERFUMER = "sparkwitch:perfumer";
    static final String ORTHOPEDIST = "sparkwitch:orthopedist";
    static final String HUNTER = "sparkwitch:hunter";
    static final String SAINT = "sparkwitch:saint";
    static final String TAROT_READER = "sparkwitch:tarot_reader";
    static final String EMMA = "sparkwitch:emma";
    static final String CONTROL_EXPERT = "sparkwitch:control_expert";
    static final String SEEKER = "sparkwitch:seeker";

    static final String SWORDFISH_STAB = "sparkwitch:swordfish_stab";
    static final String FACTOR_BACKLASH = "sparkwitch:factor_backlash";
    static final String FISHING_ROD = "sparkwitch:fishing_rod";
    static final String TASER = "sparkwitch:taser";
    /** The Orthopedist's bone setting and fracture healing share this skill id. 骨科大夫的正骨与接骨共用这个技能 id。 */
    static final String ORTHOPEDIST_SKILL = "sparkwitch:orthopedist";
    static final String HOLY_FLASH_BURST = "sparkwitch:holy_flash_burst";
    static final String COOLING_OIL_HIT = "sparkstrength:perfumer_cooling_oil_hit";
    static final String CAR_SWALLOWED = "sparkwitch:seeker_car_swallowed";
    /** The three Tarot readings, one shop entry each. 三种塔罗占卜，各对应一个商店条目。 */
    static final List<String> TAROT_READINGS = List.of(
            "sparkwitch_tarot_regular", "sparkwitch_tarot_identity", "sparkwitch_tarot_survival");
    static final String SKUNK = "SKUNK";
    /** Every catch that is a fish, i.e. SparkWitch's catch names without SKUNK. 所有鱼类渔获，即去掉 SKUNK 的 SparkWitch 渔获名。 */
    static final List<String> FISH = List.of(
            "SALMON", "COD", "CLOWNFISH", "GOLDFISH", "KEY_FISH", "PUFFERFISH", "SWORDFISH", "GLIMMERFISH");

    /** 5 seconds. 5 秒。 */
    static final int FIVE_SECONDS_TICKS = 100;
    static final int SKUNKS_IN_A_ROUND = 3;

    private WitchCivilianRules() {
    }

    // ---- The Blind 盲人 ----

    /** 盲狙: as the Blind, kill a 非好人 (the Blind has no weapon of their own). 以盲人身份击杀一名非好人。 */
    static boolean blindKill(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> Rules.killedAs(context, kill, BLIND)
                && Rules.victimNonCivilian(context, kill));
    }

    // ---- The Fisher 钓鱼佬 ----

    /**
     * 愿者上钩: as the Fisher, a swordfish stab ({@code sparkwitch:swordfish_stab}) kills a 非好人. The swordfish is
     * used up before the kill, so {@code killer_item} is never looked at.
     * 以钓鱼佬身份用剑鱼刺杀（{@code sparkwitch:swordfish_stab}）一名非好人。剑鱼在击杀前就已用掉，所以不看 {@code killer_item}。
     */
    static boolean swordfishKill(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> swordfishKillAsFisher(context, kill)
                && Rules.victimNonCivilian(context, kill));
    }

    /**
     * 钓鱼佬永不空军: 3 casts this round that landed nothing ({@code catch} SKUNK), as the Fisher.
     * 以钓鱼佬身份在一局内有 3 竿什么也没钓到（{@code catch} 为 SKUNK）。
     */
    static boolean threeSkunks(AchievementContext context) {
        return Rules.myItemUses(context, FISHING_ROD).stream()
                .filter(cast -> SKUNK.equals(cast.value(RoundStatsRecorder.CATCH)) && Rules.iWas(context, cast, FISHER))
                .count() >= SKUNKS_IN_A_ROUND;
    }

    /**
     * 一竿清台: as the Fisher, a swordfish stab kills a killer-faction player after which no killer-faction player is
     * left alive, and the round ends as a civilian win. Needs the complete record. A living player turned to the killer
     * faction by a trait is invisible to the record, but they would have kept the civilians from winning.
     * 以钓鱼佬身份用剑鱼刺杀一名杀手阵营玩家，此后场上不再有存活的杀手阵营玩家，且本局以好人获胜结束。需要完整记录。
     * 被词条翻到杀手阵营的存活玩家在记录里看不到，但有他在好人就不会获胜。
     */
    static boolean swordfishClearsTheLastKiller(AchievementContext context) {
        MatchTimeline timeline = context.timeline();
        if (!timeline.isComplete() || !Factions.isCivilian(winningFaction(context))) {
            return false;
        }
        return context.kills().stream().anyMatch(kill -> swordfishKillAsFisher(context, kill)
                && Factions.KILLER.equals(timeline.victimFaction(kill))
                && !timeline.aliveFactionsAfter(kill).containsValue(Factions.KILLER));
    }

    /**
     * 海王: as the Fisher, ever landed each of the 8 fish (every catch but SKUNK), across rounds.
     * 以钓鱼佬身份累计钓到过全部 8 种鱼（SKUNK 以外的所有渔获），可跨局。
     */
    static boolean caughtEveryFish(AchievementContext context) {
        return FISH.stream().allMatch(fish -> context.stats().fishCaught(fish) >= 1);
    }

    private static boolean swordfishKillAsFisher(AchievementContext context, MatchEvent kill) {
        return Rules.reason(kill, SWORDFISH_STAB) && Rules.killedAs(context, kill, FISHER);
    }

    // ---- The Perfumer 调香师 ----

    /**
     * 提神醒脑: as the Perfumer, my Cooling Oil splashes a player who dies within the next 5 s and was a 非好人 at that
     * death. Any death counts, by anyone's hand or none; a disconnect does not.
     * 以调香师身份用风油精溅到一名玩家，对方在之后 5 秒内死亡，且死亡时是非好人。任何死亡都算，不论是否有人动手；断线不算。
     */
    static boolean coolingOilAssist(AchievementContext context) {
        MatchTimeline timeline = context.timeline();
        return Rules.myGlobalEvents(context, COOLING_OIL_HIT).stream().anyMatch(hit -> hit.target() != null
                && Rules.iWas(context, hit, PERFUMER)
                && timeline.eventsOn(MatchEvent.DEATH, hit.target()).stream().anyMatch(death -> !death.isDisconnect()
                        && death.tick() >= hit.tick() && death.tick() <= hit.tick() + FIVE_SECONDS_TICKS
                        && Rules.victimNonCivilian(context, death)));
    }

    // ---- The Orthopedist 骨科大夫 ----

    /**
     * 农夫与蛇: as the Orthopedist, set or mend the bones of a player whose real role then was the Hunter (a Black
     * Raven disguised as the Hunter is not), and later die by that player's hand.
     * 以骨科大夫身份为一名当时真实身份为猎人的玩家正骨或接骨（伪装成猎人的黑羽鸦不算），之后死于此人之手。
     */
    static boolean killedByTheHunterIHealed(AchievementContext context) {
        UUID self = context.self();
        List<MatchEvent> myDeaths = context.timeline().eventsOn(MatchEvent.DEATH, self);
        return Rules.mySkillUses(context, ORTHOPEDIST_SKILL).stream().anyMatch(care -> care.target() != null
                && Rules.iWas(context, care, ORTHOPEDIST)
                && HUNTER.equals(context.roleAt(care.target(), care))
                && myDeaths.stream().anyMatch(death -> care.target().equals(death.actor())
                        && comesAfter(context, death, care)));
    }

    // ---- The Saint 圣徒 ----

    /**
     * 我闪我自己: as the Saint, a Holy Flash of mine went off and flashed only me ({@code affected} counts everyone
     * flashed, {@code affected_others} everyone but the thrower).
     * 以圣徒身份投出的圣光弹爆开后只闪到了我自己（{@code affected} 是被闪到的总人数，{@code affected_others} 是投掷者以外的人数）。
     */
    static boolean flashedOnlyMyself(AchievementContext context) {
        return context.myEvents(HOLY_FLASH_BURST).stream().anyMatch(burst -> burst.has("affected")
                && burst.has("affected_others")
                && burst.intValue("affected", 0) >= 1 && burst.intValue("affected_others", -1) == 0
                && Rules.iWas(context, burst, SAINT));
    }

    // ---- The Tarot Reader 塔罗牌师 ----

    /**
     * 命运之轮: as the Tarot Reader, buy all three readings in one round (each reading is a shop purchase; the identity
     * and survival readings are paid for when their picker opens).
     * 以塔罗牌师身份在一局内买下全部三种占卜（每种占卜都是一次商店购买；身份与存活占卜在打开选择界面时付费）。
     */
    static boolean allThreeReadings(AchievementContext context) {
        return context.myEvents(MatchEvent.SHOP_PURCHASE).stream()
                .filter(purchase -> TAROT_READINGS.contains(purchase.value("entry_id", ""))
                        && Rules.iWas(context, purchase, TAROT_READER))
                .map(purchase -> purchase.value("entry_id"))
                .distinct().count() == TAROT_READINGS.size();
    }

    // ---- Emma 樱羽艾玛 ----

    /** 引火烧身: as Emma, die of factor backlash ({@code sparkwitch:factor_backlash}). 以樱羽艾玛身份死于因子反噬。 */
    static boolean diedOfBacklash(AchievementContext context) {
        return context.timeline().eventsOn(MatchEvent.DEATH, context.self()).stream()
                .anyMatch(death -> Rules.reason(death, FACTOR_BACKLASH)
                        && EMMA.equals(context.timeline().victimRole(death)));
    }

    // ---- The Control Expert 控场专家 ----

    /**
     * 麻了: as the Control Expert, my taser hits a player and within the next 5 s I kill them myself, they being a
     * 非好人 at that death.
     * 以控场专家身份用电击枪电中一名玩家，并在之后 5 秒内亲手将其击杀，对方死亡时为非好人。
     */
    static boolean killSoonAfterTasing(AchievementContext context) {
        List<MatchEvent> kills = Rules.myKills(context, kill -> Rules.killedAs(context, kill, CONTROL_EXPERT)
                && Rules.victimNonCivilian(context, kill));
        return Rules.myItemUses(context, TASER).stream().anyMatch(shot -> shot.target() != null
                && shot.bool("hit", true) && Rules.iWas(context, shot, CONTROL_EXPERT)
                && kills.stream().anyMatch(kill -> shot.target().equals(kill.target())
                        && kill.tick() >= shot.tick() && kill.tick() <= shot.tick() + FIVE_SECONDS_TICKS));
    }

    // ---- The Seeker 搜寻者 ----

    /**
     * 还我车来: as the Seeker, kill with my own hand the Taotie who swallowed my car, after the swallow
     * ({@code sparkwitch:seeker_car_swallowed}: actor = the Taotie, {@code owner} = me).
     * 以搜寻者身份，在小车被吞之后亲手击杀吞掉它的饕餮（{@code sparkwitch:seeker_car_swallowed}：actor 为饕餮，{@code owner} 为我）。
     */
    static boolean killedTheCarEater(AchievementContext context) {
        UUID self = context.self();
        if (self == null) {
            return false;
        }
        return context.timeline().events(MatchEvent.GLOBAL_EVENT).stream()
                .filter(swallow -> swallow.isGlobal(CAR_SWALLOWED) && self.equals(swallow.uuid("owner")))
                .anyMatch(swallow -> {
                    UUID taotie = swallow.actor() != null ? swallow.actor() : swallow.uuid("taotie");
                    return taotie != null && context.kills().stream().anyMatch(kill -> taotie.equals(kill.target())
                            && Rules.killedAs(context, kill, SEEKER) && comesAfter(context, kill, swallow));
                });
    }

    // ---- Police collection 警职收集 ----

    /**
     * 七种武器: ever killed a 非好人 as each of the 7 police roles, across rounds. A kill counts when the victim's
     * faction was known and differed from mine ({@link LifetimeStats#rivalKillsAs}); police are always on the civilian
     * side (SparkTraits never gives them Impostor), so that is a kill of a 非好人.
     * 累计分别以 7 个警职身份各击杀过一名非好人，可跨局。死者阵营已知且与我不同的击杀才计入（{@link LifetimeStats#rivalKillsAs}）；
     * 警职永远站在好人一方（SparkTraits 从不给警职内鬼），所以这就是击杀非好人。
     */
    static boolean policeKillWithEveryRole(AchievementContext context) {
        return RoleGroups.POLICE_ROLES.stream().allMatch(role -> context.stats().rivalKillsAs(role) >= 1);
    }

    // ---- Helpers 辅助 ----

    /**
     * The winning faction: SparkFactionAPI's when it named one, otherwise the one Wathe's win status stands for.
     * 获胜阵营：SparkFactionAPI 给出时取它，否则取 Wathe 胜利状态对应的阵营。
     */
    static String winningFaction(AchievementContext context) {
        String winner = context.outcome().winningFactionId();
        return winner != null ? winner : RoundOutcome.defaultWinningFaction(context.outcome().winStatus());
    }

    /** Whether {@code later} comes after {@code earlier} in record order. 按记录顺序 {@code later} 是否在 {@code earlier} 之后。 */
    private static boolean comesAfter(AchievementContext context, MatchEvent later, MatchEvent earlier) {
        int earlierAt = positionOf(context.timeline().events(), earlier);
        return earlierAt >= 0 && positionOf(context.timeline().events(), later) > earlierAt;
    }

    /** The record position of this very event, -1 if absent. 这条事件本身在记录中的位置，没有则为 -1。 */
    private static int positionOf(List<MatchEvent> events, MatchEvent event) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i) == event) {
                return i;
            }
        }
        return -1;
    }
}
