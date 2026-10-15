package dev.caecorthus.sparkassist.achievement;

import dev.caecorthus.sparkassist.achievement.MatchTimeline.TickWindow;
import dev.caecorthus.sparkassist.achievement.RoundFacts.TickSpan;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Achievements open to every role: how a round was won, the sides at a kill or a death, who was still alive at a
 * given moment, and a few things the client saw itself (tasks, explosives nearby, the local clock). "好人" and "杀手"
 * are effective factions ({@link Factions}); who was alive is rebuilt from the match record
 * ({@link MatchTimeline#aliveAfter}: a Last Stand false death keeps a player alive), and every condition about
 * "all", "none" or "the last" needs a {@linkplain MatchTimeline#isComplete complete} record.
 * <p>
 * "I survived" is strict here, as in {@link RoundRules}: any death of mine this round, seen by the client or in the
 * record (a Last Stand false death included), means I did not. Wathe writes {@code match_end} after the fade-out
 * that follows the decision ({@link #STOP_FADE_TICKS}), so the moment a round was decided is read as
 * {@code match_end} minus the fade-out.
 * <p>
 * 不限身份的成就：一局怎么赢的、击杀或死亡时双方的阵营、某一刻还有谁活着，以及客户端自己看到的几件事（任务、附近的爆炸物、
 * 本机时钟）。“好人”与“杀手”指实际阵营（{@link Factions}）；谁还活着由对局记录还原（{@link MatchTimeline#aliveAfter}：
 * 背水一战的假死仍算存活），条件里有“全部 / 没有 / 最后一个”的成就都需要{@linkplain MatchTimeline#isComplete 完整}的记录。
 * 这里的“我存活”与 {@link RoundRules} 一样从严：本局我的任何一次死亡（客户端所见或记录中，包括背水一战的假死）都不算存活。
 * Wathe 在胜负已定后的淡出（{@link #STOP_FADE_TICKS}）结束时才写 {@code match_end}，所以胜负判定的时刻取
 * {@code match_end} 减去淡出时长。
 */
final class GeneralRules {
    static final String FELL_OUT_OF_TRAIN = "wathe:fell_out_of_train";
    static final String GUN_SHOT = "wathe:gun_shot";
    static final String REVOLVER = "wathe:revolver";
    /**
     * Written right after a fall death when a Wind Spirit's wind charge knocked the victim off; the charge credits
     * no killer.
     * 风精灵的风弹把人吹下列车时，紧接在坠车死亡之后写入；风弹不会记下凶手。
     */
    static final String WIND_SPIRIT_FALL = "sparkwitch:wind_spirit_fall";
    static final String KILLERS = "KILLERS";
    static final String PASSENGERS = "PASSENGERS";
    static final String TIME = "TIME";
    static final String LOOSE_END = "LOOSE_END";
    /** Wathe's Loose End role. Wathe 的亡命徒身份。 */
    static final String LOOSE_END_ROLE = "wathe:loose_end";
    static final String ALIVE = "ALIVE";

    /** 5 seconds. 5 秒。 */
    static final int FRESH_KILL_TICKS = 100;
    /** 10 seconds. 10 秒。 */
    static final int CROSSFIRE_TICKS = 200;
    /** 60 seconds. 60 秒。 */
    static final int DEFIANCE_TICKS = 1200;
    /**
     * Wathe's fade-out between deciding a round and writing {@code match_end} ({@code FADE_TIME + FADE_PAUSE}).
     * Wathe 在胜负已定到写下 {@code match_end} 之间的淡出时长（{@code FADE_TIME + FADE_PAUSE}）。
     */
    static final int STOP_FADE_TICKS = 60;
    /**
     * How soon after a kill the round must be decided for the kill to have decided it: Wathe checks the win every
     * server tick, so it is the same or the next tick; one second of margin.
     * 击杀后多久之内胜负已定，才算这次击杀决定了胜负：Wathe 每个服务端 tick 都检查胜负，所以是同一 tick 或下一 tick；留出一秒余量。
     */
    static final int DECIDING_KILL_TICKS = 20;
    static final int KILLER_TASKS = 5;
    static final int ROUND_TASKS = 10;
    static final int BIG_SPEND = 500;
    static final int CROWDED_ROUND = 20;
    static final int DUEL_ROUND = 12;
    static final int LONE_HERO_SIDE = 4;
    static final int MIN_OPENING_KILLERS = 2;
    static final int HOLD_OUT_KILLERS = 2;
    static final int DEFIANT_KILLERS = 3;
    static final int HIGH_NOON_KILLERS = 3;
    static final int TEN_AT_ONCE_KILLERS = 3;
    static final int TEN_AT_ONCE_CIVILIANS = 10;
    static final LocalTime NIGHT_FROM = LocalTime.of(3, 0);
    static final LocalTime NIGHT_TO = LocalTime.of(5, 0);

    private GeneralRules() {
    }

    // ---- Deaths 死亡 ----

    /**
     * 手足相残: killed by another player who was on my side at that death, both 好人 or both on the killer faction,
     * by the effective factions in the death record (a poison death is the poisoner's). Any death of mine counts, a
     * Last Stand false death included; neutrals or witches killing each other do not.
     * 被击杀时与我同为好人或同为杀手阵营的另一名玩家击杀，按死亡记录中的实际阵营判断（毒死算下毒者的）。我的任何一次死亡都算，
     * 包括背水一战的假死；中立之间、魔女之间不算。
     */
    static boolean killedByTeammate(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        return myDeaths(context).stream().anyMatch(death -> death.actor() != null && !death.actor().equals(self)
                && sameSide(timeline.killerFaction(death), timeline.victimFaction(death)));
    }

    private static boolean sameSide(String killerFaction, String victimFaction) {
        return (Factions.isCivilian(killerFaction) || Factions.KILLER.equals(killerFaction))
                && killerFaction.equals(victimFaction);
    }

    /**
     * 星之卡比: fell out of the train with no one to blame: a {@code wathe:fell_out_of_train} death of mine without a
     * killer (Wathe credits the last player who hit me within vanilla's 5 seconds; a Fire Poker or Shriek Gun push
     * credits the pusher) and without a {@link #WIND_SPIRIT_FALL} on me at that tick. A fall leaves no body, so this
     * needs the match record.
     * 从列车上坠落身亡且无人可怪：我的一次 {@code wathe:fell_out_of_train} 死亡没有凶手（Wathe 会记上原版 5 秒内最后一个打中我的玩家；
     * 烧火棍、尖啸枪的推人会记上推人者），且那一刻没有针对我的 {@link #WIND_SPIRIT_FALL}。坠车不留尸体，所以需要对局记录。
     */
    static boolean fellOffUnpushed(AchievementContext context) {
        UUID self = context.self();
        List<MatchEvent> windFalls = context.timeline().events(WIND_SPIRIT_FALL);
        return myDeaths(context).stream().anyMatch(death -> Rules.reason(death, FELL_OUT_OF_TRAIN)
                && death.actor() == null
                && windFalls.stream().noneMatch(fall -> self.equals(fall.target()) && fall.tick() == death.tick()));
    }

    // ---- Winning 获胜 ----

    /**
     * 躺赢: won on the killer faction (my effective faction at the end, as the client saw it) without killing anyone
     * this round. Needs a complete record that has me in it, so that no kill can be missing.
     * 以杀手阵营（客户端所见结束时我的实际阵营）获胜，且本局没有击杀任何人。需要包含我的完整记录，确保不会漏掉击杀。
     */
    static boolean wonAsKillerWithoutKilling(AchievementContext context) {
        MatchTimeline timeline = context.timeline();
        return context.won() && Factions.KILLER.equals(context.faction()) && timeline.isComplete()
                && timeline.startingPlayers().contains(context.self()) && context.kills().isEmpty();
    }

    /**
     * 绝杀: my kill of the other side's last player won it for my side: as a 好人 I killed the last player of the
     * killer faction and the passengers won ({@code PASSENGERS}), or on the killer faction I killed the last 好人 and
     * the killers won ({@code KILLERS}). In a complete record my kill is the last real death on that side, and the
     * round was decided within {@link #DECIDING_KILL_TICKS} of it, so a win held back (e.g. by a living Corrupt Cop)
     * and settled by a later death does not count.
     * 我击杀对方阵营最后一名玩家，使己方获胜：作为好人击杀杀手阵营最后一名玩家且乘客获胜（{@code PASSENGERS}），或作为杀手阵营
     * 击杀最后一名好人且杀手获胜（{@code KILLERS}）。在完整记录中，我的这次击杀是对方阵营的最后一次真实死亡，且胜负在击杀后
     * {@link #DECIDING_KILL_TICKS} 以内判定，所以被拖住（例如有黑警存活）、后来由别的死亡定下的胜利不算。
     */
    static boolean killedTheLastOpponent(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        Optional<MatchEvent> end = timeline.matchEnd();
        String winStatus = context.outcome().winStatus();
        if (self == null || !context.won() || end.isEmpty()
                || !PASSENGERS.equals(winStatus) && !KILLERS.equals(winStatus)) {
            return false;
        }
        String mine = PASSENGERS.equals(winStatus) ? Factions.CIVILIAN : Factions.KILLER;
        String theirs = PASSENGERS.equals(winStatus) ? Factions.KILLER : Factions.CIVILIAN;
        MatchEvent last = null;
        for (MatchEvent death : timeline.realDeaths()) {
            if (theirs.equals(timeline.victimFaction(death))) {
                last = death;
            }
        }
        return last != null && self.equals(last.actor()) && !self.equals(last.target())
                && mine.equals(timeline.killerFaction(last))
                && decidedAt(end.get()) - last.tick() <= DECIDING_KILL_TICKS;
    }

    /**
     * 苟到最后: as a 好人 (my effective faction at the end), survived to the time running out and won, with 2 or more
     * players of the killer faction still alive at {@code match_end} (complete record).
     * 以好人身份（结束时我的实际阵营）存活到时间耗尽并获胜，且 {@code match_end} 时仍有 2 名及以上杀手阵营玩家存活（完整记录）。
     */
    static boolean outlastedTheKillers(AchievementContext context) {
        Optional<MatchEvent> end = context.timeline().matchEnd();
        return context.won() && TIME.equals(context.outcome().winStatus()) && Factions.isCivilian(context.faction())
                && survived(context) && end.isPresent()
                && count(context.timeline().aliveFactionsAfter(end.get()), Factions.KILLER) >= HOLD_OUT_KILLERS;
    }

    /**
     * 春运: survived and won a round of 20 or more players (the most players with a role the client saw at once, or
     * the record's opening snapshot, whichever is larger).
     * 在 20 名及以上玩家的对局中存活并获胜（客户端同时看到的有角色玩家数的最大值，或记录的开局快照人数，取较大者）。
     */
    static boolean wonCrowdedRound(AchievementContext context) {
        return context.won() && survived(context) && roundSize(context) >= CROWDED_ROUND;
    }

    /**
     * 孤胆英雄: won as the only living member of a winning side of 4 or more, by Wathe's round-end result (dead and
     * departed winners are winners there too: every other winner's end status is not {@code ALIVE}); not in Loose
     * Ends, whose single winner is not part of a side.
     * 获胜时我是获胜方唯一存活的成员，且获胜方至少 4 人，按 Wathe 的结算结果（死去或离开的获胜者在结算里也是获胜者：
     * 其他每名获胜者的结束状态都不是 {@code ALIVE}）；不含亡命徒模式，那里唯一的获胜者不属于任何一方。
     */
    static boolean onlySurvivingWinner(AchievementContext context) {
        UUID self = context.self();
        if (self == null || !context.won() || LOOSE_END.equals(context.outcome().winStatus()) || !survived(context)) {
            return false;
        }
        List<RoundOutcome.Participant> winners = context.outcome().participants().stream()
                .filter(RoundOutcome.Participant::winner).toList();
        return winners.size() >= LONE_HERO_SIDE
                && winners.stream().anyMatch(winner -> winner.uuid().equals(self))
                && winners.stream().noneMatch(winner -> !winner.uuid().equals(self) && ALIVE.equals(winner.endStatus()));
    }

    /**
     * 完美犯罪: the round opened with 2 or more killers, the killers won with me among the winners (on the killer
     * faction at the end, as the client saw it), and no player on the killer faction has any death record all round,
     * a Last Stand false death included (complete record). A disconnect ({@link MatchEvent#isDisconnect}) is leaving,
     * not dying, so it is ignored.
     * 开局有 2 名及以上杀手，杀手获胜且我是获胜者之一（客户端所见结束时在杀手阵营），并且整局没有任何杀手阵营玩家的死亡记录，
     * 背水一战的假死也算（完整记录）。断线（{@link MatchEvent#isDisconnect}）是离开而不是死亡，不予理会。
     */
    static boolean killersWonUnscathed(AchievementContext context) {
        MatchTimeline timeline = context.timeline();
        return context.won() && Factions.KILLER.equals(winningFaction(context))
                && Factions.KILLER.equals(context.faction()) && timeline.isComplete()
                && timeline.openingFactionCount(Factions.KILLER) >= MIN_OPENING_KILLERS
                && timeline.deaths().stream().noneMatch(death -> !death.isDisconnect()
                        && Factions.KILLER.equals(timeline.victimFaction(death)));
    }

    /**
     * 平安到站: the round opened with 2 or more killers, the passengers won by wiping out the killers (Wathe's
     * {@code PASSENGERS}, not the time running out) with me among the winners (a 好人 at the end, as the client saw
     * it), and no 好人 has any death record all round, a Last Stand false death included (complete record). A
     * disconnect ({@link MatchEvent#isDisconnect}) is leaving, not dying, so it is ignored.
     * 开局有 2 名及以上杀手，乘客通过消灭所有杀手获胜（Wathe 的 {@code PASSENGERS}，不是时间耗尽）且我是获胜者之一
     * （客户端所见结束时是好人），并且整局没有任何好人的死亡记录，背水一战的假死也算（完整记录）。断线
     * （{@link MatchEvent#isDisconnect}）是离开而不是死亡，不予理会。
     */
    static boolean civiliansWonUnscathed(AchievementContext context) {
        MatchTimeline timeline = context.timeline();
        return context.won() && PASSENGERS.equals(context.outcome().winStatus())
                && Factions.isCivilian(context.faction()) && timeline.isComplete()
                && timeline.openingFactionCount(Factions.KILLER) >= MIN_OPENING_KILLERS
                && timeline.deaths().stream().noneMatch(death -> !death.isDisconnect()
                        && Factions.isCivilian(timeline.victimFaction(death)));
    }

    /**
     * 最后的决斗: in a round of 12 or more players, I killed my last opponent when only the two of us were alive, and
     * won. "Opponent": their effective faction at that death is known and differs from mine. The kill must be a real
     * death (after a Last Stand false death they come back).
     * 在 12 人及以上的对局中，场上只剩我和最后一名对手时将其击杀，并获胜。“对手”：此人死亡时的实际阵营已知且与我不同。
     * 这次击杀必须是真实死亡（背水一战的假死之后对方会复活）。
     */
    static boolean wonFinalDuel(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        if (self == null || !context.won() || roundSize(context) < DUEL_ROUND) {
            return false;
        }
        return context.kills().stream().anyMatch(kill -> kill.target() != null && !timeline.isFakeDeath(kill)
                && timeline.aliveBefore(kill).equals(Set.of(self, kill.target()))
                && rivals(timeline.killerFaction(kill), timeline.victimFaction(kill)));
    }

    // ---- Kills 击杀 ----

    /**
     * 人赃并获: a kill of a 非好人 who had killed someone else within the 5 seconds before.
     * 击杀一名在 5 秒内刚击杀过他人的非好人。
     */
    static boolean killedFreshKiller(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> Rules.victimNonCivilian(context, kill)
                && context.timeline().killsBy(kill.target()).stream().anyMatch(theirs ->
                        theirs.tick() > kill.tick() - FRESH_KILL_TICKS && theirs.tick() <= kill.tick()));
    }

    /**
     * 薪火相传: a police player P is really killed by K; after that, as a non-police 好人, I pick up a revolver; after
     * that, as a non-police 好人 at the kill, I shoot K dead with a revolver ({@code wathe:gun_shot} with a revolver
     * in my hand). The record cannot tell whose revolver it was, and the condition does not ask.
     * 警职 P 被 K 真实杀死；之后我以非警职的好人身份捡起一把左轮手枪；再之后，击杀时仍为非警职的好人，我用左轮手枪击杀 K
     * （{@code wathe:gun_shot}，手持左轮手枪）。记录分不出捡到的是谁的枪，条件也没有这样要求。
     */
    static boolean avengedPolice(AchievementContext context) {
        UUID self = context.self();
        if (self == null) {
            return false;
        }
        MatchTimeline timeline = context.timeline();
        List<MatchEvent> events = timeline.events();
        for (int i = 0; i < events.size(); i++) {
            MatchEvent policeDeath = events.get(i);
            UUID police = policeDeath.target();
            UUID killer = policeDeath.actor();
            if (!policeDeath.is(MatchEvent.DEATH) || police == null || killer == null || killer.equals(police)
                    || killer.equals(self) || police.equals(self) || timeline.isFakeDeath(policeDeath)
                    || !RoleGroups.isPolice(timeline.victimRole(policeDeath))) {
                continue;
            }
            boolean armed = false;
            for (MatchEvent event : events.subList(i + 1, events.size())) {
                if (!armed && event.is(MatchEvent.ITEM_PICKUP) && self.equals(event.actor())
                        && REVOLVER.equals(event.value("item"))
                        && nonPoliceCivilian(context.roleAt(self, event), context.factionAt(self, event))) {
                    armed = true;
                } else if (armed && event.is(MatchEvent.DEATH) && self.equals(event.actor())
                        && killer.equals(event.target()) && Rules.reason(event, GUN_SHOT)
                        && REVOLVER.equals(event.value("killer_item"))
                        && nonPoliceCivilian(timeline.killerRole(event), timeline.killerFaction(event))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean nonPoliceCivilian(String role, String faction) {
        return !RoleGroups.isPolice(role) && Factions.isCivilian(faction);
    }

    /**
     * 午时已到: as a 好人, killed every player of the killer faction this round (at least 3) myself, and the 好人
     * won. Those players are everyone but me who was ever on the killer faction (by a role they held or a death
     * record), except a player whose every real death shows another faction (e.g. a Conscience flip); each needs a
     * real death I caused while I was a 好人 and they were on the killer faction (complete record). A living
     * player's trait flip is invisible, so a surviving Conscience killer still counts as one left alive.
     * 以好人身份亲手击杀本局全部杀手阵营玩家（至少 3 人），且好人获胜。这些玩家是除我之外曾属于杀手阵营（按担任过的身份或死亡记录）
     * 的所有人，但每次真实死亡都显示为其他阵营的玩家除外（例如良知翻转）；其中每人都要有一次由我造成的真实死亡，
     * 当时我是好人、对方在杀手阵营（完整记录）。存活玩家的词条翻转看不到，所以活下来的良知杀手仍算作一名幸存的杀手。
     */
    static boolean killedEveryKiller(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        if (self == null || !context.won() || !Factions.isCivilian(winningFaction(context)) || !timeline.isComplete()) {
            return false;
        }
        List<MatchEvent> realDeaths = timeline.realDeaths();
        Set<UUID> killers = new LinkedHashSet<>();
        for (UUID player : timeline.playersEverIn(Factions.KILLER::equals)) {
            List<MatchEvent> theirDeaths = realDeaths.stream().filter(death -> player.equals(death.target())).toList();
            if (!player.equals(self) && (theirDeaths.isEmpty() || theirDeaths.stream()
                    .anyMatch(death -> Factions.KILLER.equals(timeline.victimFaction(death))))) {
                killers.add(player);
            }
        }
        return killers.size() >= HIGH_NOON_KILLERS && killers.stream().allMatch(killer -> realDeaths.stream()
                .anyMatch(death -> killer.equals(death.target()) && self.equals(death.actor())
                        && Factions.KILLER.equals(timeline.victimFaction(death))
                        && Factions.isCivilian(timeline.killerFaction(death))));
    }

    // ---- Who was left 谁还活着 ----

    /**
     * 我命由我不由天: at the moment I became the last living 好人, 3 or more players of the killer faction were alive;
     * I triggered no Last Stand at any point this round, never held {@link #LOOSE_END_ROLE}, was alive at
     * {@code match_end}, and the 好人 won with me among the winners. If the time ran out, the round was decided at
     * least 60 seconds after that moment. Complete record. A Last Stand before that moment rules it out too: it is
     * what sets up SparkTraits' Final Moment (which turns its holder into a Loose End) once they become the last 好人.
     * Other players' factions are the ones the record
     * shows (a living player's trait flip it never shows is invisible); mine is read by {@link #myFactionAt}.
     * 在我成为最后一名存活好人的那一刻，场上有 3 名及以上杀手阵营玩家存活；本局我从未触发背水一战、从未担任
     * {@link #LOOSE_END_ROLE}，{@code match_end} 时仍存活，并且好人获胜、我是获胜者之一。若是时间耗尽获胜，胜负至少在那一刻之后
     * 60 秒才判定。需要完整记录。那一刻之前的背水一战也不行：正是它让持有者在成为最后一名好人时进入 SparkTraits 的最终时刻
     * （把持有者变成亡命徒）。
     * 其他玩家的阵营按记录给出的为准（记录里从未出现的存活玩家的词条阵营翻转看不到）；我的阵营按 {@link #myFactionAt} 读取。
     */
    static boolean defiedFate(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        Optional<MatchEvent> end = timeline.matchEnd();
        if (self == null || !context.won() || !Factions.isCivilian(winningFaction(context)) || end.isEmpty()
                || !timeline.aliveAfter(end.get()).contains(self)
                || !Rules.myGlobalEvents(context, MatchEvent.LAST_STAND_TRIGGERED).isEmpty()
                || timeline.roleHistory(self).contains(LOOSE_END_ROLE)) {
            return false;
        }
        boolean wasLast = false;
        for (MatchEvent event : timeline.events()) {
            if (event.is(MatchEvent.MATCH_END)) {
                break;
            }
            if (!changesWhoIsAlive(event)) {
                continue;
            }
            Map<UUID, String> alive = timeline.aliveFactionsAfter(event);
            boolean last = alive.containsKey(self) && Factions.isCivilian(myFactionAt(context, event))
                    && othersIn(alive, self, Factions.CIVILIAN) == 0;
            if (last && !wasLast && count(alive, Factions.KILLER) >= DEFIANT_KILLERS
                    && (!TIME.equals(context.outcome().winStatus())
                            || decidedAt(end.get()) - event.tick() >= DEFIANCE_TICKS)) {
                return true;
            }
            wasLast = last;
        }
        return false;
    }

    /**
     * Events after which who is alive, or a known faction, may differ.
     * 之后存活名单或已知阵营可能发生变化的事件。
     */
    private static boolean changesWhoIsAlive(MatchEvent event) {
        return event.is(MatchEvent.ROLE_ASSIGNED) || event.is(MatchEvent.ROLE_CHANGED) || event.is(MatchEvent.DEATH)
                || event.is(MatchEvent.PLAYER_LEAVE) || event.isGlobal(MatchEvent.PATHOGEN_REVIVED);
    }

    /**
     * 我要打十个: the round opened with 3 or more killers; when the last of my killer-faction teammates died (a real
     * death; leaving counts as gone), I was alive on the killer faction with 10 or more 好人 alive; and I won as the
     * only living player of the killer faction at {@code match_end}, the killers winning (complete record). Factions
     * are the ones the record shows.
     * 开局有 3 名及以上杀手；我的最后一名杀手阵营队友死亡时（真实死亡；离开也算离场），我在杀手阵营且存活，场上有 10 名及以上好人存活；
     * 并且杀手获胜、我赢下了对局，{@code match_end} 时我是唯一存活的杀手阵营玩家（完整记录）。阵营按记录给出的为准。
     */
    static boolean wonAloneAgainstTen(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        Optional<MatchEvent> end = timeline.matchEnd();
        if (self == null || !context.won() || !Factions.KILLER.equals(winningFaction(context)) || end.isEmpty()
                || timeline.openingFactionCount(Factions.KILLER) < TEN_AT_ONCE_KILLERS
                || !aloneOnKillerSide(timeline.aliveFactionsAfter(end.get()), self)) {
            return false;
        }
        for (MatchEvent death : timeline.realDeaths()) {
            if (self.equals(death.target()) || !Factions.KILLER.equals(timeline.victimFaction(death))) {
                continue;
            }
            Map<UUID, String> alive = timeline.aliveFactionsAfter(death);
            if (aloneOnKillerSide(alive, self)) {
                return count(alive, Factions.CIVILIAN) >= TEN_AT_ONCE_CIVILIANS;
            }
        }
        return false;
    }

    private static boolean aloneOnKillerSide(Map<UUID, String> alive, UUID self) {
        return Factions.KILLER.equals(alive.get(self)) && count(alive, Factions.KILLER) == 1;
    }

    /**
     * 一路穿越炮火地带: while I was a 好人 and two players other than me were in psycho mode at once (any kind), an
     * explosive (grenade, M67, grenade drone or Potion Gunner shell) was within 15 blocks of me, and I then stayed
     * alive for 10 more seconds with two others in psycho all along. The explosive is on the round clock and psycho
     * mode on the record clock, which may differ by {@link RoundFacts#RECORD_TICK_TOLERANCE}; to stay on the safe
     * side the psycho span must cover the 10 seconds with that margin on both sides, I must be a 好人 at both ends of
     * the margin around the explosive, no death of mine may be recorded before the 10 seconds plus the margin are
     * over, and the round must not have been decided within the 10 seconds.
     * 当我是好人、且除我之外有两名玩家同时处于疯魔（任何一种）时，我 15 格内出现了爆炸物（手雷、M67、榴弹无人机或药炮手炮弹），
     * 此后我又存活了 10 秒，这 10 秒内一直有两名其他玩家处于疯魔。爆炸物按对局时钟记录，疯魔按记录时钟记录，两者可能相差
     * {@link RoundFacts#RECORD_TICK_TOLERANCE}；为了稳妥，疯魔时段必须在两侧各多留这段余量后覆盖这 10 秒，
     * 爆炸物前后余量的两端我都必须是好人，10 秒加余量结束之前不能有我的死亡记录，且这 10 秒内胜负还没有定。
     */
    static boolean survivedCrossfire(AchievementContext context) {
        int tolerance = RoundFacts.RECORD_TICK_TOLERANCE;
        for (TickWindow psychos : context.timeline().concurrentPsychoWindows(2, context.self())) {
            for (TickSpan explosive : context.facts().explosiveNearby()) {
                long from = Math.max(explosive.fromTick(), (long) psychos.fromTick() + tolerance);
                long to = Math.min(explosive.toTick(), (long) psychos.toTick() - CROSSFIRE_TICKS - tolerance);
                if (from <= to && civilianAndAliveThrough(context, from)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether I was a 好人 around round tick {@code tick} and lived through the 10 seconds after it. Only the earliest
     * tick of each overlap is tried: a later one leaves less of the round and of my life to cover, so it could only
     * differ by my faction changing inside the overlap, which this misses.
     * 我在对局 tick {@code tick} 前后是否是好人，并活过了此后的 10 秒。每段重叠只尝试最早的 tick：更晚的 tick 要覆盖的对局和存活时间
     * 只会更靠后，唯一可能不同的是我的阵营在重叠内发生变化，这种情况会漏判。
     */
    private static boolean civilianAndAliveThrough(AchievementContext context, long tick) {
        RoundFacts facts = context.facts();
        int tolerance = RoundFacts.RECORD_TICK_TOLERANCE;
        long until = tick + CROSSFIRE_TICKS;
        if (facts.died() && facts.deathTick() <= until || facts.durationTicks() < until
                || myDeaths(context).stream().anyMatch(death -> death.tick() <= until + tolerance)) {
            return false;
        }
        return Factions.isCivilian(myFactionAt(context, (int) Math.max(0, tick - tolerance)))
                && Factions.isCivilian(myFactionAt(context, (int) (tick + tolerance)));
    }

    // ---- Tasks and the shop 任务与商店 ----

    /**
     * 演员的自我修养: 5 tasks finished in one round while on the killer faction (killers get fake tasks, which Wathe
     * records like real ones). The faction at each task is my effective faction as {@link #myFactionAt} reads it.
     * 一局内在杀手阵营时完成 5 个任务（杀手有假任务，Wathe 会像真任务一样记录）。每次任务时的阵营按 {@link #myFactionAt}
     * 读到的实际阵营判断。
     */
    static boolean killerFinishedTasks(AchievementContext context) {
        return context.myEvents(MatchEvent.TASK_COMPLETE).stream()
                .filter(task -> Factions.KILLER.equals(myFactionAt(context, task)))
                .count() >= KILLER_TASKS;
    }

    /**
     * 劳模: 10 tasks finished in one round, by Wathe's task-complete notices or the record, whichever saw more.
     * 一局内完成 10 个任务，按 Wathe 的任务完成通知或对局记录，取较多的一方。
     */
    static boolean manyTasksInOneRound(AchievementContext context) {
        return Math.max(context.facts().tasksCompleted(), context.myEvents(MatchEvent.TASK_COMPLETE).size())
                >= ROUND_TASKS;
    }

    /**
     * 买买买: 500 coins spent in the shop in one round, adding up each purchase's {@code price_paid}. When the killer
     * team purse covers part of a purchase, Wathe still records the full price as paid by the buyer.
     * 一局内在商店累计花费 500 金币，累加每次购买的 {@code price_paid}。杀手团队经济代付一部分时，Wathe 仍把全价记为买家支付。
     */
    static boolean spentBig(AchievementContext context) {
        return context.myEvents(MatchEvent.SHOP_PURCHASE).stream()
                .mapToLong(purchase -> Math.max(0, purchase.intValue("price_paid", purchase.intValue("price", 0))))
                .sum() >= BIG_SPEND;
    }

    // ---- The local clock 本机时钟 ----

    /** 修仙: the round settled between 3:00 and 5:00 local time. 这一局在本机时间凌晨 3:00 到 5:00 之间结算。 */
    static boolean settledLateAtNight(AchievementContext context) {
        return context.facts().settledBetween(NIGHT_FROM, NIGHT_TO);
    }

    // ---- Helpers 工具 ----

    /** Every recorded death of mine, a false one included. 记录中我的每一次死亡，包括假死。 */
    private static List<MatchEvent> myDeaths(AchievementContext context) {
        UUID self = context.self();
        return self == null ? List.of() : context.timeline().eventsOn(MatchEvent.DEATH, self);
    }

    private static boolean survived(AchievementContext context) {
        return !RoundRules.died(context);
    }

    /**
     * My effective faction when {@code event} happened. Outside a death the record only knows my role's base faction,
     * so while that role is still my role at the end (in the record and as the client saw it) and I never died, the
     * faction the client saw last stands in: it includes my own trait flips (Impostor, Conscience).
     * {@code event} 发生时我的实际阵营。死亡以外的事件里，记录只知道我身份的基础阵营；所以只要那时的身份直到结束都还是我的身份
     * （记录与客户端所见都是），且我从未死亡，就改用客户端最后看到的阵营：它包含了我自己的词条翻转（内鬼、良知）。
     */
    private static String myFactionAt(AchievementContext context, MatchEvent event) {
        return effectiveFaction(context, context.roleAt(context.self(), event), context.factionAt(context.self(), event));
    }

    /**
     * {@link #myFactionAt(AchievementContext, MatchEvent)} right after record tick {@code tick}; null when the record
     * does not have me alive then.
     * 记录 tick {@code tick} 刚过之后的 {@link #myFactionAt(AchievementContext, MatchEvent)}；记录中我那时不在存活名单里时为 null。
     */
    private static String myFactionAt(AchievementContext context, int tick) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        Map<UUID, String> alive = timeline.aliveFactionsAt(tick);
        if (self == null || !alive.containsKey(self)) {
            return null;
        }
        MatchEvent lastEvent = null;
        for (MatchEvent event : timeline.events()) {
            if (event.tick() > tick) {
                break;
            }
            lastEvent = event;
        }
        String role = lastEvent == null ? timeline.openingRole(self) : timeline.roleAfter(self, lastEvent);
        return effectiveFaction(context, role, alive.get(self));
    }

    private static String effectiveFaction(AchievementContext context, String roleThen, String recordFaction) {
        boolean sameRoleToTheEnd = roleThen != null && roleThen.equals(context.timeline().finalRole(context.self()))
                && roleThen.equals(context.role());
        return sameRoleToTheEnd && !RoundRules.died(context) && context.faction() != null
                ? context.faction()
                : recordFaction;
    }

    /**
     * The winning faction: SparkFactionAPI's custom one, else the one Wathe's win status stands for.
     * 获胜阵营：SparkFactionAPI 的自定义阵营，否则取 Wathe 胜利状态对应的阵营。
     */
    private static String winningFaction(AchievementContext context) {
        String custom = context.outcome().winningFactionId();
        return custom != null ? custom : RoundOutcome.defaultWinningFaction(context.outcome().winStatus());
    }

    /**
     * How many players the round had: the most with a role the client saw at once, or the record's opening snapshot.
     * 本局人数：客户端同时看到的有角色玩家数的最大值，或记录的开局快照人数。
     */
    private static int roundSize(AchievementContext context) {
        return Math.max(context.facts().players(), context.timeline().startingPlayers().size());
    }

    /** The record tick the round was decided at. 胜负判定时的记录 tick。 */
    private static int decidedAt(MatchEvent matchEnd) {
        return matchEnd.tick() - STOP_FADE_TICKS;
    }

    /** Two known, different real factions. 两个已知且不同的真实阵营。 */
    private static boolean rivals(String a, String b) {
        return isFaction(a) && isFaction(b) && !a.equals(b);
    }

    private static boolean isFaction(String factionId) {
        return factionId != null && !factionId.isBlank() && !Factions.NONE.equals(factionId);
    }

    private static int othersIn(Map<UUID, String> factions, UUID self, String factionId) {
        return (int) factions.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(self) && factionId.equals(entry.getValue())).count();
    }

    private static int count(Map<UUID, String> factions, String factionId) {
        return (int) factions.values().stream().filter(factionId::equals).count();
    }
}
