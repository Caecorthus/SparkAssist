package dev.caecorthus.sparkassist.achievement;

import dev.caecorthus.sparkassist.achievement.MatchTimeline.TickWindow;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Achievements read from the record events SparkStrength writes for them (contract S8-S14): blackout edges, a
 * flashlight beating the blackout, the capture device's owner, tablet meetings and Bandit throwing axes; plus the
 * Undercover's tomahawk kills, which are plain deaths with a SparkStrength death reason. Servers without that
 * SparkStrength never write these events, so every rule here simply finds nothing. "As role X" is my real role at that
 * event from the match record.
 * 依据 SparkStrength 专为成就写下的记录事件（契约 S8-S14）判断的成就：停电边沿、手电筒挡下停电、捕捉装置的主人、平板会议与强盗飞斧；
 * 另有卧底的战术斧击杀，它们是带 SparkStrength 死因的普通死亡记录。
 * 没有该版本 SparkStrength 的服务器不会写这些事件，因此这里的规则只是什么也找不到。“以 X 身份”指对局记录中该事件发生时我的真实身份。
 */
final class StrengthRecordRules {
    static final String ATTENDANT = "noellesroles:attendant";
    static final String MASTER_SABOTEUR = "sparktraits:master_saboteur";

    /**
     * S8, a world event at each real lights-out ({@code active} 1) and lights-back (0) edge; {@code cause} is
     * {@code purchase} (actor = the buyer), {@code power_restoration}, {@code expired} or {@code unknown}. The round
     * end records no edge.
     * S8：每个真实熄灯（{@code active} 1）与来电（0）边沿的世界事件；{@code cause} 为 {@code purchase}（actor 为购买者）、
     * {@code power_restoration}、{@code expired} 或 {@code unknown}。回合结束不记录边沿。
     */
    static final String BLACKOUT_STATE = "sparkstrength:blackout_state";
    static final String CAUSE_PURCHASE = "purchase";
    /**
     * S9: a lit flashlight held by {@code actor} (any role) cancelled their blackout blindness, once per blackout; it
     * may come just before the S8 start in the same tick.
     * S9：{@code actor}（任何身份）手持开启的手电筒挡下了停电失明，每次停电一次；可能在同一 tick 内略早于 S8 的开始边沿。
     */
    static final String FLASHLIGHT_BLACKOUT = "sparkstrength:flashlight_blackout";
    /** S10, a global event: actor = the captured player, {@code owner} = the device's owner. S10 全局事件。 */
    static final String CAPTURE_DEVICE_TRIGGERED = "sparkstrength:capture_device_triggered";
    /** S11: actor = the player who called a tablet emergency meeting. S11：actor 为召开平板紧急会议的玩家。 */
    static final String TABLET_MEETING = "sparkstrength:tablet_meeting";
    /** S12: a meeting made target a suspect; {@code caller} = that meeting's caller. S12：会议把 target 投成嫌疑人。 */
    static final String TABLET_SUSPECT = "sparkstrength:tablet_suspect";
    /** S13: actor threw a throwing axe; {@code throw_id} = the axe entity. S13：actor 扔出一把飞斧。 */
    static final String THROWING_AXE_THROW = "sparkstrength:throwing_axe_throw";
    /** S14: the axe {@code throw_id} hit target; {@code killed} whether that hit killed. S14：飞斧命中 target。 */
    static final String THROWING_AXE_HIT = "sparkstrength:throwing_axe_hit";
    /**
     * The death reason of a tactical tomahawk kill. SparkStrength hands the tomahawk only to the real Undercover, the
     * first time its CQCM mask goes on (stage 2), and nobody else can hold it, so a kill with this reason is always made
     * as the Undercover after putting the mask on (taking the mask off later does not end stage 2).
     * 战术斧击杀的死因。SparkStrength 只在真卧底第一次戴上 CQCM 面具（进入第二阶段）时发给它战术斧，别人拿不到，
     * 所以带这个死因的击杀一定是以卧底身份、戴上面具之后做出的（之后摘下面具也不会退出第二阶段）。
     */
    static final String TOMAHAWK = "sparkstrength:tomahawk";

    static final int AXE_KILLS = 2;
    static final int TOMAHAWK_KILLS = 10;
    static final int TOMAHAWK_KILLS_IN_ONE_DARK_ROUND = 5;
    /** 45 seconds. 45 秒。 */
    static final int SABOTEUR_WINDOW_TICKS = 900;
    static final int SABOTEUR_KILLS = 3;

    private StrengthRecordRules() {
    }

    // ---- Bandit 强盗 ----

    /**
     * 一斧双雕: as the Bandit, one throwing axe kills 2 players: an axe I threw as the Bandit
     * ({@code sparkstrength:throwing_axe_throw}) has hits on two different players with {@code killed} set (its hits
     * repeat the throw's {@code throw_id}). A hit a shield, vest or fake death stopped is not {@code killed}.
     * 以强盗身份用一把飞斧杀死 2 名玩家：一把我以强盗身份扔出的飞斧（{@code sparkstrength:throwing_axe_throw}）对两名不同玩家的命中都带
     * {@code killed}（命中记录沿用投掷的 {@code throw_id}）。被护盾、防弹衣或假死挡下的命中不算 {@code killed}。
     */
    static boolean oneAxeTwoKills(AchievementContext context) {
        UUID self = context.self();
        Set<UUID> myAxes = new HashSet<>();
        for (MatchEvent thrown : context.myEvents(THROWING_AXE_THROW)) {
            UUID axe = thrown.uuid("throw_id");
            if (axe != null && Rules.iWas(context, thrown, NoellesKillerRules.BANDIT)) {
                myAxes.add(axe);
            }
        }
        List<MatchEvent> kills = context.timeline().events(THROWING_AXE_HIT).stream()
                .filter(hit -> hit.bool("killed") && hit.target() != null && !hit.target().equals(self))
                .toList();
        return myAxes.stream().anyMatch(axe -> kills.stream()
                .filter(hit -> axe.equals(hit.uuid("throw_id")))
                .map(MatchEvent::target).distinct().count() >= AXE_KILLS);
    }

    // ---- Attendant 乘务员 ----

    /**
     * 黑夜给了我黑色的眼睛: as the Attendant, my lit flashlight beat a blackout's blindness
     * ({@code sparkstrength:flashlight_blackout}), and I lived through that blackout: no death of mine (a Last Stand
     * false death included) from its start to the lights coming back. A blackout the round end cut short has no end
     * edge; then the record must be complete and I alive at its end.
     * 以乘务员身份用开启的手电筒挡下一次停电的失明（{@code sparkstrength:flashlight_blackout}），并活过这次停电：从停电开始到来电，
     * 我没有任何死亡（背水一战的假死也算）。被回合结束打断的停电没有结束边沿，此时要求记录完整且我活到最后。
     */
    static boolean flashlightOutlastsBlackout(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        if (self == null) {
            return false;
        }
        List<Blackout> blackouts = blackouts(timeline);
        List<MatchEvent> myDeaths = timeline.eventsOn(MatchEvent.DEATH, self);
        return context.myEvents(FLASHLIGHT_BLACKOUT).stream()
                .filter(lit -> Rules.iWas(context, lit, ATTENDANT))
                .anyMatch(lit -> blackouts.stream().anyMatch(blackout -> blackout.window().contains(lit.tick())
                        && (blackout.end() != null || timeline.isComplete())
                        && myDeaths.stream().noneMatch(death -> blackout.window().contains(death.tick()))));
    }

    // ---- Engineer 工程师 ----

    /**
     * 请君入瓮: as the Engineer, my capture device stuns a player on the killer faction then
     * ({@code sparkstrength:capture_device_triggered} with {@code owner} me; the faction by {@link #factionAround}).
     * 以工程师身份用我的捕捉装置定住一名当时属于杀手阵营的玩家（{@code sparkstrength:capture_device_triggered} 的 {@code owner}
     * 为我；阵营按 {@link #factionAround} 判断）。
     */
    static boolean capturedAKiller(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        if (self == null) {
            return false;
        }
        return timeline.events(MatchEvent.GLOBAL_EVENT).stream().anyMatch(capture ->
                capture.isGlobal(CAPTURE_DEVICE_TRIGGERED) && self.equals(capture.uuid("owner"))
                        && capture.actor() != null && !self.equals(capture.actor())
                        && Rules.iWas(context, capture, GadgetRules.ENGINEER)
                        && Factions.KILLER.equals(factionAround(timeline, capture.actor(), capture)));
    }

    // ---- Vigilante 义警 ----

    /**
     * 群众的眼睛是雪亮的: as the Vigilante, call a tablet emergency meeting ({@code sparkstrength:tablet_meeting}) that
     * ends by making a 非好人 a suspect ({@code sparkstrength:tablet_suspect} after it, its {@code caller} me when
     * written; the suspect's faction by {@link #factionAround}). Who voted for whom is not recorded, so my own vote is
     * not checked.
     * 以义警身份召开一次平板紧急会议（{@code sparkstrength:tablet_meeting}），会议结果把一名非好人投成嫌疑人（其后的
     * {@code sparkstrength:tablet_suspect}，写有 {@code caller} 时须为我；嫌疑人阵营按 {@link #factionAround} 判断）。
     * 记录里没有谁投了谁，因此不检查我自己的投票。
     */
    static boolean meetingNamedNonCivilian(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        if (self == null) {
            return false;
        }
        MatchEvent meeting = null;
        for (MatchEvent event : timeline.events()) {
            if (event.is(TABLET_MEETING)) {
                meeting = event;
            } else if (event.is(TABLET_SUSPECT) && meeting != null && event.target() != null
                    && self.equals(meeting.actor())
                    && (!event.has("caller") || self.equals(event.uuid("caller")))
                    && Rules.iWas(context, meeting, NoellesCivilianRules.VIGILANTE)
                    && Factions.isNonCivilian(factionAround(timeline, event.target(), event))) {
                return true;
            }
        }
        return false;
    }

    // ---- Veteran 老兵 ----

    /**
     * 天黑请闭眼: as the Veteran, a knife kill ({@code wathe:knife_stab}) of a 非好人 during a blackout: from its start
     * edge through its end edge, both ticks included, or to the end of the record when the round cut it short.
     * 以老兵身份在停电期间用刀（{@code wathe:knife_stab}）击杀一名非好人：从开始边沿到结束边沿（两端 tick 都算），回合结束打断的停电
     * 算到记录末尾。
     */
    static boolean veteranKnifeInTheDark(AchievementContext context) {
        List<Blackout> blackouts = blackouts(context.timeline());
        return context.kills().stream().anyMatch(kill -> Rules.reason(kill, NoellesCivilianRules.KNIFE_STAB)
                && Rules.killedAs(context, kill, NoellesCivilianRules.VETERAN)
                && Rules.victimNonCivilian(context, kill)
                && inBlackout(blackouts, kill));
    }

    // ---- Undercover 卧底 ----

    /**
     * 花盆剑圣: tomahawk ({@link #TOMAHAWK}) kills of 10 非好人, counted across rounds
     * ({@link LifetimeStats#nonCivilianKills}; a Last Stand false death is not a kill).
     * 累计用战术斧（{@link #TOMAHAWK}）击杀 10 名非好人，可跨局（{@link LifetimeStats#nonCivilianKills}；背水一战的假死不算击杀）。
     */
    static boolean tomahawkKillsOverall(AchievementContext context) {
        return context.stats().nonCivilianKills(TOMAHAWK) >= TOMAHAWK_KILLS;
    }

    /**
     * 夜魔: as 花盆剑圣, counting only the kills made during a blackout
     * ({@link LifetimeStats#blackoutNonCivilianKills}, by {@link #inBlackout}).
     * 同花盆剑圣，但只算停电期间的击杀（{@link LifetimeStats#blackoutNonCivilianKills}，按 {@link #inBlackout} 判断）。
     */
    static boolean tomahawkKillsInTheDarkOverall(AchievementContext context) {
        return context.stats().blackoutNonCivilianKills(TOMAHAWK) >= TOMAHAWK_KILLS;
    }

    /**
     * 梦魇: as the Undercover, tomahawk 5 different 非好人 during blackouts in one round (any number of blackouts; a
     * Last Stand false death is not a kill).
     * 以卧底身份在一局内的停电期间用战术斧击杀 5 名不同的非好人（可分布在多次停电中；背水一战的假死不算击杀）。
     */
    static boolean tomahawkNightmare(AchievementContext context) {
        MatchTimeline timeline = context.timeline();
        List<Blackout> blackouts = blackouts(timeline);
        return Rules.myKills(context, kill -> Rules.reason(kill, TOMAHAWK)
                        && Rules.killedAs(context, kill, KillRules.UNDERCOVER)
                        && Rules.victimNonCivilian(context, kill)
                        && !timeline.isFakeDeath(kill)
                        && inBlackout(blackouts, kill)).stream()
                .map(MatchEvent::target).filter(Objects::nonNull).distinct().count()
                >= TOMAHAWK_KILLS_IN_ONE_DARK_ROUND;
    }

    // ---- Master Saboteur 破坏大师 ----

    /**
     * 月黑风高: holding Master Saboteur when I bought a blackout (its start edge has {@code cause} purchase and me as
     * actor), kill 3 different players after that start and within the next 45 s, however long the blackout lasts.
     * 买下停电时持有破坏大师（开始边沿的 {@code cause} 为 purchase、actor 为我），并在停电开始之后的 45 秒内击杀 3 名不同的玩家，
     * 与停电持续多久无关。
     */
    static boolean saboteurBlackoutTriple(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        if (self == null) {
            return false;
        }
        List<MatchEvent> kills = context.kills();
        return blackouts(timeline).stream().map(Blackout::start).anyMatch(start -> self.equals(start.actor())
                && CAUSE_PURCHASE.equals(start.value("cause"))
                && context.hadTraitAround(MASTER_SABOTEUR, start.tick())
                && kills.stream()
                        .filter(kill -> comesAfter(timeline, kill, start)
                                && kill.tick() - start.tick() <= SABOTEUR_WINDOW_TICKS)
                        .map(MatchEvent::target).filter(Objects::nonNull).distinct().count() >= SABOTEUR_KILLS);
    }

    // ---- Shared helpers 共用辅助 ----

    /**
     * One blackout from the {@link #BLACKOUT_STATE} edges: its start edge and its end edge, or a null end when the
     * round ended first (the round end records no edge) or the record was cut. {@code toTick} is the end edge's tick, or
     * the last record tick without one.
     * 由 {@link #BLACKOUT_STATE} 边沿得出的一次停电：开始边沿与结束边沿；回合先结束（回合结束不记录边沿）或记录被截断时结束为 null。
     * {@code toTick} 为结束边沿的 tick，没有结束边沿时为记录的最后一个 tick。
     */
    record Blackout(MatchEvent start, MatchEvent end, int toTick) {
        /** From the start tick through {@code toTick}, both included. 从开始 tick 到 {@code toTick}（含两端）。 */
        TickWindow window() {
            return new TickWindow(start.tick(), toTick);
        }
    }

    /**
     * Every blackout of the round in order, paired from the {@link #BLACKOUT_STATE} edges (a repeated start or an end
     * without a start is ignored). Empty on servers without these events. A flashlight event can come just before its
     * blackout's start edge in the same tick, so compare ticks (the window includes the start tick), not record order.
     * 本局按顺序的每一次停电，由 {@link #BLACKOUT_STATE} 边沿配对得出（重复的开始或没有开始的结束会被忽略）。没有这些事件的服务器上为空。
     * 手电筒事件可能在同一 tick 内略早于其停电的开始边沿，所以比较 tick（时段包含开始 tick），不要比较记录顺序。
     */
    static List<Blackout> blackouts(MatchTimeline timeline) {
        List<Blackout> blackouts = new ArrayList<>();
        MatchEvent start = null;
        for (MatchEvent edge : timeline.events(BLACKOUT_STATE)) {
            if (!edge.has("active")) {
                continue;
            }
            if (edge.bool("active")) {
                start = start == null ? edge : start;
            } else if (start != null) {
                blackouts.add(new Blackout(start, edge, Math.max(start.tick(), edge.tick())));
                start = null;
            }
        }
        if (start != null) {
            blackouts.add(new Blackout(start, null, Math.max(start.tick(), timeline.endTick())));
        }
        return List.copyOf(blackouts);
    }

    /** Whether {@code event} falls inside one of {@code blackouts}. {@code event} 是否落在某次停电之内。 */
    static boolean inBlackout(List<Blackout> blackouts, MatchEvent event) {
        return blackouts.stream().anyMatch(blackout -> blackout.window().contains(event.tick()));
    }

    /**
     * {@code player}'s best-known effective faction at {@code event} (not their own death): the faction their next
     * death record gives them ({@code victim_faction}, or {@code killer_faction} for a kill of theirs) when they change
     * no role before it, otherwise {@link MatchTimeline#factionAfter}. A trait flip (Impostor, Conscience) only shows in
     * death records, so looking ahead catches it for a player who appears in one later.
     * {@code player} 在 {@code event}（不是其本人的死亡）时已知最准确的实际阵营：若此人在下一条涉及自己的死亡记录之前没有身份变化，
     * 取该记录给出的阵营（作为死者取 {@code victim_faction}，作为击杀者取 {@code killer_faction}），否则取
     * {@link MatchTimeline#factionAfter}。词条造成的阵营翻转（内鬼、良知）只出现在死亡记录里，向后查找能识别之后出现在死亡记录中的人。
     */
    static String factionAround(MatchTimeline timeline, UUID player, MatchEvent event) {
        List<MatchEvent> events = timeline.events();
        for (int i = positionOf(events, event) + 1; i > 0 && i < events.size(); i++) {
            MatchEvent later = events.get(i);
            if (later.is(MatchEvent.ROLE_CHANGED) && player.equals(later.uuid("player"))) {
                break;
            }
            if (later.is(MatchEvent.DEATH)) {
                String recorded = player.equals(later.target()) ? later.value("victim_faction", null)
                        : player.equals(later.actor()) ? later.value("killer_faction", null)
                        : null;
                if (recorded != null) {
                    return recorded;
                }
            }
        }
        return timeline.factionAfter(player, event);
    }

    /** Whether {@code later} comes after {@code earlier} in record order. 按记录顺序 {@code later} 是否在 {@code earlier} 之后。 */
    static boolean comesAfter(MatchTimeline timeline, MatchEvent later, MatchEvent earlier) {
        int earlierAt = positionOf(timeline.events(), earlier);
        return earlierAt >= 0 && positionOf(timeline.events(), later) > earlierAt;
    }

    /** The record position of this very event, -1 if absent. 这条事件本身在记录中的位置，没有则为 -1。 */
    static int positionOf(List<MatchEvent> events, MatchEvent event) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i) == event) {
                return i;
            }
        }
        return -1;
    }
}
