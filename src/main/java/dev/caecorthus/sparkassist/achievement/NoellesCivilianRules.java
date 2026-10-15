package dev.caecorthus.sparkassist.achievement;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Noelle's Roles civilians and Wathe's base civilian and police roles, from the match record. "As role X" is my role at
 * that event; every chain of events is read in record order, and a rule finds nothing on a round without a record.
 * Noelle's Roles 的好人身份与 Wathe 的基础平民、警职身份，依据对局记录判断。“以 X 身份”指该事件发生时我的身份；
 * 每条事件链都按记录顺序读取，没有记录的一局里规则什么也找不到。
 */
final class NoellesCivilianRules {
    static final String DEMON_HUNTER = "noellesroles:demon_hunter";
    static final String VOODOO = "noellesroles:voodoo";
    static final String RECALLER = "noellesroles:recaller";
    static final String NOISEMAKER = "noellesroles:noisemaker";
    static final String REPORTER = "noellesroles:reporter";
    static final String CONDUCTOR = "noellesroles:conductor";
    static final String ENGINEER = "noellesroles:engineer";
    static final String TIME_KEEPER = "noellesroles:time_keeper";
    static final String CIVILIAN = "wathe:civilian";
    static final String VIGILANTE = "wathe:vigilante";
    static final String VETERAN = "wathe:veteran";

    static final String DEMON_HUNTER_SHOT = "noellesroles:demon_hunter_shot";
    /** The Voodoo curse's death reason (the same id as the role). 巫毒诅咒的死因（与身份 id 相同）。 */
    static final String VOODOO_CURSE = "noellesroles:voodoo";
    /** Written right after the dead Voodoo's death: actor = the Voodoo, target = the bound player. 巫毒师死亡后紧接着写入。 */
    static final String VOODOO_CHAIN_DEATH = "voodoo_chain_death";
    static final String RECALL_SKILL = "noellesroles:recaller";
    static final String TELEPORT_ACTION = "teleport";
    static final String GLOW_STARTED = "sparkstrength:noisemaker_glow_started";
    static final String GLOW_ENDED = "sparkstrength:noisemaker_glow_ended";
    static final String CONNECTION_FAILED_ONE_DEAD = "sparkstrength:reporter_connection_failed_one_dead";
    static final String CONNECTION_FAILED_BOTH_DEAD = "sparkstrength:reporter_connection_failed_both_dead";
    static final String BROADCAST_FAILED = "sparkstrength:reporter_broadcast_failed";
    static final String BROADCAST_STARTED = "sparkstrength:reporter_broadcast_started";
    static final String BROADCAST_ENDED = "sparkstrength:reporter_broadcast_ended";
    static final String BROADCAST_INTERRUPTED = "sparkstrength:reporter_broadcast_interrupted";
    static final String MASTER_KEY = "noellesroles:master_key";
    static final String CROWBAR = "wathe:crowbar";
    static final String REPAIR_TOOL = "noellesroles:repair_tool";
    static final String REPAIR_ACTION = "repair";
    static final String REDUCE_TIME = "noellesroles:timekeeper_reduce_time";
    static final String POCKET_WATCH = "sparkwitch:time_stealer_pocket_watch";
    static final String TIME_WIN = "TIME";
    static final List<String> CHORES = List.of("sleep", "outside", "eat", "drink");
    static final String MENTAL_BREAKDOWN = "wathe:mental_breakdown";
    static final String REVOLVER = "wathe:revolver";
    static final String GUN_SHOT = "wathe:gun_shot";
    static final String KNIFE_STAB = "wathe:knife_stab";

    /** 10 seconds. 10 秒。 */
    static final int TEN_SECONDS = 200;
    /** The Voodoo curse kills 5 s after the chain record (NR 1.7.6), give or take a tick. 巫毒诅咒在连锁记录后 5 秒致死，误差一 tick。 */
    static final int VOODOO_CURSE_TICKS = 100;
    static final int VOODOO_CURSE_SLACK = 2;
    /** A Noisemaker glow lasts 30 s (SparkStrength). 大嗓门点亮持续 30 秒（SparkStrength）。 */
    static final int GLOW_TICKS = 600;
    /** 15 seconds. 15 秒。 */
    static final int LAST_SCREAM_TICKS = 300;
    /** A Reporter broadcast lasts 30 s (SparkStrength). 记者广播采访持续 30 秒（SparkStrength）。 */
    static final int BROADCAST_TICKS = 600;
    /** 30 seconds. 30 秒。 */
    static final int REPAIR_WINDOW_TICKS = 600;
    /**
     * Wathe writes {@code match_end} exactly one round-end fade ({@code FADE_TIME} 40 + {@code FADE_PAUSE} 20 ticks)
     * after the server tick that decided the round. A shop purchase is handled between server ticks, so a time cut
     * recorded at tick T empties the clock before tick T + 1 decides the round, and {@code match_end} follows at T + 61;
     * a purchase recorded at most this many ticks before {@code match_end} came after the round was decided.
     * Wathe 在判定胜负的那个服务端 tick 之后，恰好经过一次结束淡出（{@code FADE_TIME} 40 + {@code FADE_PAUSE} 20 tick）才写
     * {@code match_end}。商店购买在服务端 tick 之间处理，所以记录在 tick T 的减时会在 tick T + 1 判定胜负之前清空时间，
     * {@code match_end} 随后在 T + 61 写入；记录时间距 {@code match_end} 不超过这么多 tick 的购买发生在胜负已定之后。
     */
    static final int ROUND_END_FADE_TICKS = 60;
    static final int ROUND_END_SLACK = 5;
    /** The Time Stealer's curse kills at its fifth chime, 35 s after the theft (SparkWitch). 窃时诅咒在第 5 声钟响（被窃后 35 秒）致死。 */
    static final int TIME_THEFT_LETHAL_TICKS = 700;
    static final int REVOLVER_SHOTS = 2;
    static final int VETERAN_KNIFE_KILLS = 3;

    private NoellesCivilianRules() {
    }

    // ---- Demon Hunter 猎魔人 ----

    /**
     * 你们这是自寻死路: as the Demon Hunter, a Demon Hunter pistol kill ({@code noellesroles:demon_hunter_shot}; the
     * {@code killer_item} is air after the last bullet) of a player in psycho mode at the kill ({@code victim_psycho},
     * else the psycho record).
     * 以猎魔人身份用猎魔枪击杀（死因 {@code noellesroles:demon_hunter_shot}；最后一发子弹后 {@code killer_item} 是空气）
     * 一名击杀时处于疯魔中的玩家（取 {@code victim_psycho}，没有时取疯魔记录）。
     */
    static boolean demonHunterKillsPsycho(AchievementContext context) {
        return !demonHunterPsychoKills(context).isEmpty();
    }

    /**
     * 你们还没有准备好: such a kill within 10 s of the victim's psycho mode starting ({@code sparkfactionapi:psycho},
     * every psycho kind). A fresh pistol cannot fire for its first 5 s, so the window is really 5–10 s unless the
     * hunter already held one.
     * 上述击杀发生在死者进入疯魔（{@code sparkfactionapi:psycho}，各种疯魔都算）后 10 秒内。新发的猎魔枪前 5 秒不能开火，
     * 所以除非猎魔人手里已有枪，实际窗口是 5–10 秒。
     */
    static boolean demonHunterKillsFreshPsycho(AchievementContext context) {
        return demonHunterPsychoKills(context).stream().anyMatch(kill -> context.timeline().psychoWindows(kill.target())
                .stream().anyMatch(window -> window.contains(kill.tick()) && kill.tick() - window.fromTick() <= TEN_SECONDS));
    }

    private static List<MatchEvent> demonHunterPsychoKills(AchievementContext context) {
        return Rules.myKills(context, kill -> Rules.reason(kill, DEMON_HUNTER_SHOT)
                && Rules.killedAs(context, kill, DEMON_HUNTER)
                && (kill.has("victim_psycho") ? kill.bool("victim_psycho")
                        : context.timeline().inPsycho(kill.target(), kill.tick())));
    }

    // ---- Voodoo 巫毒师 ----

    /**
     * 同归于尽: I died as the Voodoo ({@code victim_role}), my curse record ({@code voodoo_chain_death}, same tick) names
     * the bound player, and that player's next death is the curse ({@code noellesroles:voodoo}) about 5 s later.
     * 我以巫毒师身份死亡（{@code victim_role}），同一 tick 的诅咒记录（{@code voodoo_chain_death}）指向绑定目标，
     * 且该目标的下一次死亡是约 5 秒后的诅咒（{@code noellesroles:voodoo}）。
     */
    static boolean voodooCurseTookTarget(AchievementContext context) {
        return voodooCurseLanded(context, false);
    }

    /**
     * 冤有头债有主: the same, when the bound player was the one who killed me (the actor of my death; a poisoner counts).
     * 同上，且杀死我的正是绑定目标（我的死亡记录的凶手；下毒者也算）。
     */
    static boolean voodooCurseTookMyKiller(AchievementContext context) {
        return voodooCurseLanded(context, true);
    }

    private static boolean voodooCurseLanded(AchievementContext context, boolean boundPlayerKilledMe) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        List<MatchEvent> events = timeline.events();
        for (int i = 0; i < events.size() && self != null; i++) {
            MatchEvent chain = events.get(i);
            if (!chain.is(VOODOO_CHAIN_DEATH) || !self.equals(chain.actor()) || chain.target() == null) {
                continue;
            }
            MatchEvent myDeath = myDeathAtChain(events, i, self);
            if (myDeath == null || !VOODOO.equals(timeline.victimRole(myDeath))
                    || boundPlayerKilledMe && !chain.target().equals(myDeath.actor())) {
                continue;
            }
            MatchEvent next = nextDeathOf(events, i, chain.target());
            if (next != null && Rules.reason(next, VOODOO_CURSE)
                    && next.tick() - chain.tick() <= VOODOO_CURSE_TICKS + VOODOO_CURSE_SLACK) {
                return true;
            }
        }
        return false;
    }

    /** My death recorded just before the chain record, in its tick. 诅咒记录之前、同一 tick 内我的死亡记录。 */
    private static MatchEvent myDeathAtChain(List<MatchEvent> events, int chainPosition, UUID self) {
        for (int i = chainPosition - 1; i >= 0 && events.get(i).tick() == events.get(chainPosition).tick(); i--) {
            if (events.get(i).is(MatchEvent.DEATH) && self.equals(events.get(i).target())) {
                return events.get(i);
            }
        }
        return null;
    }

    // ---- Recaller 回溯者 ----

    /**
     * SL大法: as the Recaller, teleport back to the save point ({@code skill_use} {@code action=teleport}), which always
     * costs exactly 100 coins (NR 1.7.6; placing the point is free).
     * 以回溯者身份回溯到存档点（{@code skill_use}，{@code action=teleport}），每次固定花费 100 金币（NR 1.7.6；设置存档点免费）。
     */
    static boolean recalled(AchievementContext context) {
        return !myRecalls(context).isEmpty();
    }

    /**
     * 存档坏了: a death of mine (a Last Stand false death counts, like every "died") within 10 s after such a recall.
     * 回溯后 10 秒内我的一次死亡（与所有“死亡”一样，背水一战的假死也算）。
     */
    static boolean diedSoonAfterRecall(AchievementContext context) {
        UUID self = context.self();
        if (self == null) {
            return false;
        }
        List<MatchEvent> events = context.timeline().events();
        for (MatchEvent recall : myRecalls(context)) {
            for (int i = indexOf(events, recall) + 1; i < events.size(); i++) {
                MatchEvent event = events.get(i);
                if (event.tick() - recall.tick() > TEN_SECONDS) {
                    break;
                }
                if (event.is(MatchEvent.DEATH) && self.equals(event.target())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<MatchEvent> myRecalls(AchievementContext context) {
        return Rules.mySkillUses(context, RECALL_SKILL).stream()
                .filter(use -> TELEPORT_ACTION.equals(use.value("action")) && Rules.iWas(context, use, RECALLER))
                .toList();
    }

    // ---- Noisemaker 大嗓门 ----

    /**
     * 光芒消逝: as the Noisemaker, light up a player ({@code sparkstrength:noisemaker_glow_started}, target = them) who is
     * then killed (a death with a killer) as a 非好人 while still glowing: before their {@code noisemaker_glow_ended} and
     * within 30 s of the latest glow put on them (a new glow restarts the 30 s).
     * 以大嗓门身份点亮一名玩家（{@code sparkstrength:noisemaker_glow_started}，target 为对方），对方随后在发光期间以非好人身份
     * 被击杀（有凶手的死亡）：在其 {@code noisemaker_glow_ended} 之前，且在最近一次被点亮后 30 秒内（再次点亮会重新计时）。
     */
    static boolean glowedOtherSideKilled(AchievementContext context) {
        UUID self = context.self();
        List<MatchEvent> events = context.timeline().events();
        for (int i = 0; i < events.size() && self != null; i++) {
            MatchEvent start = events.get(i);
            if (!start.isGlobal(GLOW_STARTED) || !self.equals(start.actor()) || start.target() == null
                    || !Rules.iWas(context, start, NOISEMAKER)) {
                continue;
            }
            UUID lit = start.target();
            int litSince = start.tick();
            for (int j = i + 1; j < events.size() && events.get(j).tick() - litSince <= GLOW_TICKS; j++) {
                MatchEvent event = events.get(j);
                if (event.isGlobal(GLOW_STARTED) && lit.equals(event.target())) {
                    litSince = event.tick();
                } else if (event.isGlobal(GLOW_ENDED) && lit.equals(event.actor())) {
                    break;
                } else if (event.is(MatchEvent.DEATH) && lit.equals(event.target())) {
                    if (killedBySomeone(event) && Rules.victimNonCivilian(context, event)) {
                        return true;
                    }
                    break;
                }
            }
        }
        return false;
    }

    /**
     * 最后的呐喊: killed as the Noisemaker ({@code victim_role}; my killer then glows for 15 s, which is not recorded), and
     * my killer (the death's actor) dies within 15 s.
     * 以大嗓门身份被击杀（{@code victim_role}；凶手随即发光 15 秒，这一点不写记录），且凶手（死亡记录的执行者）在 15 秒内死亡。
     */
    static boolean killerDiedSoonAfterMe(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        List<MatchEvent> events = timeline.events();
        for (int i = 0; i < events.size() && self != null; i++) {
            MatchEvent myDeath = events.get(i);
            if (!myDeath.is(MatchEvent.DEATH) || !self.equals(myDeath.target()) || !killedBySomeone(myDeath)
                    || !NOISEMAKER.equals(timeline.victimRole(myDeath))) {
                continue;
            }
            MatchEvent theirs = nextDeathOf(events, i, myDeath.actor());
            if (theirs != null && theirs.tick() - myDeath.tick() <= LAST_SCREAM_TICKS) {
                return true;
            }
        }
        return false;
    }

    // ---- Reporter 记者 ----

    /**
     * 您拨打的电话已关机: as the Reporter, a call or broadcast refused because a chosen player was not live
     * ({@code reporter_connection_failed_one_dead} / {@code _both_dead} / {@code reporter_broadcast_failed}), where
     * that player had already died earlier in the record (so a non-participant or an admin does not count).
     * 以记者身份接线或广播采访因所选玩家不在场而失败（{@code reporter_connection_failed_one_dead} / {@code _both_dead} /
     * {@code reporter_broadcast_failed}），且该玩家在记录中更早时已经死亡（因此非参与者或管理员不算）。
     */
    static boolean calledTheDead(AchievementContext context) {
        UUID self = context.self();
        List<MatchEvent> events = context.timeline().events();
        for (int i = 0; i < events.size() && self != null; i++) {
            MatchEvent failure = events.get(i);
            if (!failure.is(MatchEvent.GLOBAL_EVENT) || !self.equals(failure.actor())
                    || !Rules.iWas(context, failure, REPORTER)) {
                continue;
            }
            List<UUID> chosen = failure.isGlobal(CONNECTION_FAILED_BOTH_DEAD)
                    ? Stream.of(failure.uuid("player_one"), failure.uuid("player_two")).filter(Objects::nonNull).toList()
                    : failure.isGlobal(CONNECTION_FAILED_ONE_DEAD) || failure.isGlobal(BROADCAST_FAILED)
                            ? Optional.ofNullable(failure.uuid("dead_player")).stream().toList()
                            : List.of();
            int position = i;
            if (chosen.stream().anyMatch(player -> diedBefore(events, position, player))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 独家专访: as the Reporter, start a broadcast interview ({@code reporter_broadcast_started}, {@code target_player})
     * of a player who is killed (a death with a killer) as a 非好人 before it ends: within its 30 s, before its
     * {@code reporter_broadcast_ended} / {@code _interrupted}, and while I am still alive (my death ends it silently).
     * 以记者身份开始广播采访（{@code reporter_broadcast_started}，{@code target_player}）一名玩家，对方在采访结束前以非好人身份
     * 被击杀（有凶手的死亡）：在 30 秒内、在 {@code reporter_broadcast_ended} / {@code _interrupted} 之前，且我仍存活
     * （我死亡会让采访无记录地结束）。
     */
    static boolean intervieweeKilledOnAir(AchievementContext context) {
        UUID self = context.self();
        List<MatchEvent> events = context.timeline().events();
        for (int i = 0; i < events.size() && self != null; i++) {
            MatchEvent start = events.get(i);
            UUID guest = start.uuid("target_player");
            if (!start.isGlobal(BROADCAST_STARTED) || !self.equals(start.actor()) || guest == null
                    || !Rules.iWas(context, start, REPORTER)) {
                continue;
            }
            for (int j = i + 1; j < events.size() && events.get(j).tick() - start.tick() <= BROADCAST_TICKS; j++) {
                MatchEvent event = events.get(j);
                boolean over = (event.isGlobal(BROADCAST_ENDED) || event.isGlobal(BROADCAST_INTERRUPTED))
                        && self.equals(event.actor());
                if (over || event.is(MatchEvent.DEATH) && self.equals(event.target())) {
                    break;
                }
                if (event.is(MatchEvent.DEATH) && guest.equals(event.target())) {
                    if (killedBySomeone(event) && Rules.victimNonCivilian(context, event)) {
                        return true;
                    }
                    break;
                }
            }
        }
        return false;
    }

    // ---- Conductor 列车长 ----

    /**
     * 引狼入室: after a death of mine as the Conductor ({@code victim_role}), another player on the killer faction at the
     * pickup (by {@link StrengthRecordRules#factionAround}, which catches an Impostor or Conscience flip from a later
     * death record) picks up a master key. The pickup record has no owner, so the key is assumed to be the one I
     * dropped (a second master key in one round is very rare).
     * 我以列车长身份死亡（{@code victim_role}）后，另一名拾取时属于杀手阵营的玩家（按 {@link StrengthRecordRules#factionAround}
     * 判断，能从之后的死亡记录识别内鬼、良知造成的阵营翻转）捡起了一把万能钥匙。拾取记录不带物主，因此默认那就是我掉落的钥匙
     * （一局里出现第二把万能钥匙非常少见）。
     */
    static boolean killerTookMyMasterKey(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        List<MatchEvent> events = timeline.events();
        boolean dropped = false;
        for (MatchEvent event : events) {
            if (self == null) {
                return false;
            }
            if (event.is(MatchEvent.DEATH) && self.equals(event.target())) {
                dropped |= CONDUCTOR.equals(timeline.victimRole(event));
            } else if (dropped && event.is(MatchEvent.ITEM_PICKUP) && MASTER_KEY.equals(event.value("item"))
                    && event.actor() != null && !self.equals(event.actor())
                    && Factions.KILLER.equals(StrengthRecordRules.factionAround(timeline, event.actor(), event))) {
                return true;
            }
        }
        return false;
    }

    // ---- Engineer 工程师 ----

    /**
     * 随叫随到: as the Engineer, repair a pried door ({@code noellesroles:repair_tool}, {@code action=repair}) within 30 s
     * after the latest crowbar use ({@code wathe:crowbar}) on the same door block (both record the door's lower half in
     * {@code pos}).
     * 以工程师身份修好一扇被撬开的门（{@code noellesroles:repair_tool}，{@code action=repair}），距同一扇门最近一次被撬棍撬开
     * （{@code wathe:crowbar}）不超过 30 秒（两条记录的 {@code pos} 都是门的下半格）。
     */
    static boolean repairedPriedDoorQuickly(AchievementContext context) {
        List<MatchEvent> events = context.timeline().events();
        for (MatchEvent repair : Rules.myItemUses(context, REPAIR_TOOL)) {
            if (!REPAIR_ACTION.equals(repair.value("action")) || !hasPos(repair)
                    || !Rules.iWas(context, repair, ENGINEER)) {
                continue;
            }
            for (int i = indexOf(events, repair) - 1; i >= 0; i--) {
                MatchEvent pry = events.get(i);
                if (pry.is(MatchEvent.ITEM_USE) && CROWBAR.equals(pry.value("item")) && samePos(pry, repair)) {
                    if (repair.tick() - pry.tick() <= REPAIR_WINDOW_TICKS) {
                        return true;
                    }
                    break;
                }
            }
        }
        return false;
    }

    // ---- Timekeeper 计时员 ----

    /**
     * 提前下班: as the Timekeeper, buy 「减少45秒游戏时间」 ({@code noellesroles:timekeeper_reduce_time}) and the round ends
     * on time ({@code TIME}) at once: the purchase lies more than one round-end fade ({@link #ROUND_END_FADE_TICKS})
     * before {@code match_end} (61 ticks when it decided the round in the next tick) and at most 5 ticks more. A
     * purchase 60 ticks or less before {@code match_end} was made during the fade, after the round was already
     * decided (e.g. by the time running out on its own), and does not count. A Last Stand revival pending at that
     * moment delays the end and misses it.
     * 以计时员身份购买「减少45秒游戏时间」（{@code noellesroles:timekeeper_reduce_time}），本局随即以时间耗尽（{@code TIME}）结束：
     * 购买距 {@code match_end} 超过一次结束淡出（{@link #ROUND_END_FADE_TICKS}；下一 tick 就判定胜负时正好 61 tick），且最多再多
     * 5 tick。距 {@code match_end} 不超过 60 tick 的购买发生在淡出期间、胜负已定之后（例如时间自然耗尽之后），不算。那一刻若有
     * 背水一战的复活尚未完成，结束会被推迟，因而判不到。
     */
    static boolean timeCutEndedRound(AchievementContext context) {
        Optional<MatchEvent> end = context.timeline().matchEnd();
        if (end.isEmpty() || !TIME_WIN.equals(end.get().value("win_status", context.outcome().winStatus()))) {
            return false;
        }
        int endTick = end.get().tick();
        return myTimeCuts(context).stream().anyMatch(cut -> endTick - cut.tick() > ROUND_END_FADE_TICKS
                && endTick - cut.tick() <= ROUND_END_FADE_TICKS + ROUND_END_SLACK);
    }

    /**
     * 物归原主: as the Timekeeper, buy 「减少45秒游戏时间」 while a Time Stealer curse is still pending, which lifts it
     * (SparkWitch {@code TimekeeperCounter}; the lift itself is not recorded). A curse is a pocket-watch theft
     * ({@code sparkwitch:time_stealer_pocket_watch}, target = the victim) less than 35 s (its lethal chime) before the
     * purchase, whose victim has not died since, and that no earlier Timekeeper purchase already lifted. The lift only
     * happens when the buyer is not on the killer faction, so a purchase while holding Impostor does not count. It also
     * only happens while the round is live, so a purchase at most {@link #ROUND_END_FADE_TICKS} before
     * {@code match_end} (during the round-end fade) does not count either.
     * 以计时员身份在窃时诅咒尚未生效时购买「减少45秒游戏时间」，从而解除诅咒（SparkWitch {@code TimekeeperCounter}；解除本身
     * 不写记录）。诅咒指购买前不到 35 秒（致死钟响）的一次怀表窃时（{@code sparkwitch:time_stealer_pocket_watch}，target
     * 为受害者），受害者此后没有死亡，也没有被更早的计时员购买解除。只有购买者不属于杀手阵营时才会解除，所以持有内鬼时的购买不算。
     * 解除只在对局进行中发生，所以距 {@code match_end} 不超过 {@link #ROUND_END_FADE_TICKS} tick 的购买（结束淡出期间）也不算。
     */
    static boolean timeCutLiftedCurse(AchievementContext context) {
        MatchTimeline timeline = context.timeline();
        List<MatchEvent> events = timeline.events();
        Optional<MatchEvent> end = timeline.matchEnd();
        for (MatchEvent cut : myTimeCuts(context)) {
            if (Factions.KILLER.equals(timeline.factionAfter(context.self(), cut))
                    || context.hadTraitAround(TraitRules.IMPOSTOR, cut.tick())
                    || end.isPresent() && end.get().tick() - cut.tick() <= ROUND_END_FADE_TICKS) {
                continue;
            }
            int position = indexOf(events, cut);
            for (int i = position - 1; i >= 0 && cut.tick() - events.get(i).tick() < TIME_THEFT_LETHAL_TICKS; i--) {
                MatchEvent event = events.get(i);
                if (isTimeCut(context, event)) {
                    break;
                }
                if (event.is(MatchEvent.ITEM_USE) && POCKET_WATCH.equals(event.value("item")) && event.target() != null
                        && !diedBetween(events, i, position, event.target())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<MatchEvent> myTimeCuts(AchievementContext context) {
        return context.myEvents(MatchEvent.SHOP_PURCHASE).stream().filter(purchase -> isTimeCut(context, purchase)).toList();
    }

    /** A 「减少45秒游戏时间」 bought by whoever was the Timekeeper then. 当时身为计时员的玩家购买的「减少45秒游戏时间」。 */
    private static boolean isTimeCut(AchievementContext context, MatchEvent event) {
        return event.is(MatchEvent.SHOP_PURCHASE) && REDUCE_TIME.equals(event.value("item"))
                && TIME_KEEPER.equals(context.roleAt(event.actor(), event));
    }

    // ---- Wathe 基础身份 ----

    /**
     * 全勤奖: as the Civilian ({@code wathe:civilian}), complete the sleep, outside, eat and drink tasks in one round
     * ({@code task_complete} {@code task}).
     * 以平民（{@code wathe:civilian}）身份在一局内完成睡觉、外出透气、吃东西、喝东西四种任务（{@code task_complete} 的 {@code task}）。
     */
    static boolean civilianDidEveryChore(AchievementContext context) {
        Set<String> done = context.myEvents(MatchEvent.TASK_COMPLETE).stream()
                .filter(task -> Rules.iWas(context, task, CIVILIAN))
                .map(task -> task.value("task"))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        return done.containsAll(CHORES);
    }

    /**
     * 心态崩了: a death of mine by mental breakdown ({@code wathe:mental_breakdown}, the only 精神崩溃 reason; the Bell
     * Ringer's toll is not it), from the record or the reason on my body.
     * 我死于精神崩溃（{@code wathe:mental_breakdown}，唯一的“精神崩溃”死因；敲钟人的丧钟不算），取自记录或我尸体上的死因。
     */
    static boolean diedOfMentalBreakdown(AchievementContext context) {
        return MENTAL_BREAKDOWN.equals(context.facts().deathReason())
                || context.timeline().eventsOn(MatchEvent.DEATH, context.self()).stream()
                        .anyMatch(death -> Rules.reason(death, MENTAL_BREAKDOWN));
    }

    /**
     * 弹无虚发: as the Vigilante, at least 2 revolver shots ({@code item_use} {@code wathe:revolver}; a miss has no target)
     * in one round, every one of them killing the player it hit as a 非好人 in the same tick ({@code wathe:gun_shot}),
     * and every {@code wathe:gun_shot} kill I made as the Vigilante that round was of a 非好人. Needs the whole record.
     * Niko's extra burst shots write no {@code item_use}, so they are not counted as shots, but a 好人 they kill still
     * spoils it.
     * 以义警身份在一局内用左轮手枪开枪（{@code item_use} {@code wathe:revolver}；打空没有 target）至少 2 次，且每一枪都在同一
     * tick 击杀了所命中的非好人（{@code wathe:gun_shot}），并且本局我以义警身份的每一次 {@code wathe:gun_shot} 击杀都是非好人。
     * 需要完整记录。Niko 的追加连射不写 {@code item_use}，所以不算作开枪，但它们打死好人仍会破坏成就。
     */
    static boolean everyRevolverShotKilledOtherSide(AchievementContext context) {
        if (!context.timeline().isComplete()) {
            return false;
        }
        List<MatchEvent> shots = Rules.myItemUses(context, REVOLVER).stream()
                .filter(shot -> Rules.iWas(context, shot, VIGILANTE))
                .toList();
        List<MatchEvent> gunKills = Rules.myKills(context, kill -> Rules.reason(kill, GUN_SHOT)
                && Rules.killedAs(context, kill, VIGILANTE));
        return shots.size() >= REVOLVER_SHOTS && shots.stream().allMatch(shot -> shot.target() != null
                && context.kills().stream().anyMatch(kill -> kill.tick() == shot.tick()
                        && shot.target().equals(kill.target()) && Rules.reason(kill, GUN_SHOT)
                        && Rules.victimNonCivilian(context, kill)))
                && gunKills.stream().allMatch(kill -> Rules.victimNonCivilian(context, kill));
    }

    /**
     * 老兵不死: as the Veteran, knife ({@code wathe:knife_stab}) 3 different 非好人 in one round. {@code killer_item} is
     * not used: it is the main hand, and the knife may be in the off hand.
     * 以老兵身份在一局内用刀（{@code wathe:knife_stab}）击杀 3 名不同的非好人。不看 {@code killer_item}：它取主手物品，而刀可以拿在副手。
     */
    static boolean veteranKnifedThree(AchievementContext context) {
        return Rules.myKills(context, kill -> Rules.reason(kill, KNIFE_STAB) && Rules.killedAs(context, kill, VETERAN)
                        && Rules.victimNonCivilian(context, kill)).stream()
                .map(MatchEvent::target).filter(Objects::nonNull).distinct().count() >= VETERAN_KNIFE_KILLS;
    }

    // ---- Record order 记录顺序 ----

    /** A death with a killer other than the victim. 有凶手、且凶手不是死者本人的死亡。 */
    private static boolean killedBySomeone(MatchEvent death) {
        return death.actor() != null && !death.actor().equals(death.target());
    }

    /** {@code event}'s position in {@code events}, by identity; -1 when absent. {@code event} 在列表中的位置（按同一对象），没有则为 -1。 */
    private static int indexOf(List<MatchEvent> events, MatchEvent event) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i) == event) {
                return i;
            }
        }
        return -1;
    }

    /** {@code player}'s first death after position {@code from}, or null. 位置 {@code from} 之后 {@code player} 的第一次死亡。 */
    private static MatchEvent nextDeathOf(List<MatchEvent> events, int from, UUID player) {
        for (int i = from + 1; i < events.size(); i++) {
            if (events.get(i).is(MatchEvent.DEATH) && player.equals(events.get(i).target())) {
                return events.get(i);
            }
        }
        return null;
    }

    /** Whether {@code player} has a death before position {@code before}. {@code player} 在位置 {@code before} 之前是否有死亡记录。 */
    private static boolean diedBefore(List<MatchEvent> events, int before, UUID player) {
        return diedBetween(events, -1, before, player);
    }

    /** Whether {@code player} died strictly between two positions. {@code player} 是否在两个位置之间死亡（不含两端）。 */
    private static boolean diedBetween(List<MatchEvent> events, int after, int before, UUID player) {
        for (int i = after + 1; i < before; i++) {
            if (events.get(i).is(MatchEvent.DEATH) && player.equals(events.get(i).target())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPos(MatchEvent event) {
        return event.has("pos.x") && event.has("pos.y") && event.has("pos.z");
    }

    private static boolean samePos(MatchEvent one, MatchEvent other) {
        return hasPos(one) && hasPos(other)
                && one.intValue("pos.x", 0) == other.intValue("pos.x", 0)
                && one.intValue("pos.y", 0) == other.intValue("pos.y", 0)
                && one.intValue("pos.z", 0) == other.intValue("pos.z", 0);
    }
}
