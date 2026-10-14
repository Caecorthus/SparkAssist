package dev.caecorthus.sparkassist.achievement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The FOOD arm's level 5 achievements: lifetime eating and drinking ({@link LifetimeStats}, this round already
 * counted) and single rounds decided by poisoned food and drink. Wathe poison is one timer per player: each
 * {@code player_poisoned} (target = victim, {@code poisoner_uuid}, {@code source}, {@code ticks} left) replaces the
 * poisoner, and the poison death's actor is that poisoner while they are in the world, dead or alive.
 * FOOD 旋臂的 5 级成就：累计吃喝（{@link LifetimeStats}，已计入本局）以及由下了毒的食物和饮品决定的单局。Wathe 的毒在每名玩家
 * 身上只有一个计时：每条 {@code player_poisoned}（target 为中毒者，带 {@code poisoner_uuid}、{@code source} 与剩余
 * {@code ticks}）都会替换下毒者，毒死时的凶手就是这名下毒者，只要他还在世界里，无论生死。
 */
final class FeastRules {
    static final int DRINKS_IN_ONE_LIFE = 50;
    static final int DISTINCT_ITEMS = 12;

    static final String POISON = "wathe:poison";
    /** The poison source of everything eaten or drunk (platters, trays, the Waiter, capsules). 所有吃喝下的毒的来源。 */
    static final String FOOD_SOURCE = "wathe:food";
    static final String TOXICOLOGIST = "noellesroles:toxicologist";
    static final String BLUE_VITRIOL = "sparkstrength:blue_vitriol";
    static final String PLATE_ACTION = "plate";
    static final String POISON_VIAL = "wathe:poison_vial";
    static final String ANTIDOTE = "noellesroles:antidote";
    static final String CURE_ACTION = "cure";
    static final String CATALYST = "noellesroles:catalyst";
    /** Noelle's Roles' record of an Iron Man or Whiskey shield stopping a death. Noelle's Roles 记录铁人药剂或威士忌护盾挡下死亡。 */
    static final String DEATH_BLOCKED = "death_blocked";
    /**
     * {@link MatchEvent#GLOBAL_EVENT} id: a Guardian Angel's shield (actor = its owner) stopped a death of
     * {@code target} with this {@code death_reason}.
     * {@link MatchEvent#GLOBAL_EVENT} 的 id：守护天使的护盾（actor 为其主人）挡下了 {@code target} 这次死因为
     * {@code death_reason} 的死亡。
     */
    static final String GUARDIAN_SHIELD_ACTIVATED = "sparkwitch:guardian_shield_activated";
    /**
     * {@link MatchEvent#GLOBAL_EVENT} id: a Jester in its fake death (actor) shrugged off a death with this
     * {@code death_reason}.
     * {@link MatchEvent#GLOBAL_EVENT} 的 id：处于假死的小丑（actor）免疫了一次死因为 {@code death_reason} 的死亡。
     */
    static final String JESTER_FAKE_DEATH_KILL_BLOCKED = "sparkstrength:jester_fake_death_kill_blocked";
    /** The longest a poison (food or blue) runs: Wathe's 800–1400 ticks. 毒（食物毒或蓝毒）最长的时长：Wathe 的 800–1400 tick。 */
    static final int LONGEST_POISON_TICKS = 1400;
    static final String ALIVE = "ALIVE";

    private FeastRules() {
    }

    /**
     * 千杯不醉: 50 drinks since I last died, across rounds ({@link LifetimeStats#consumedSinceDeath}); a round in which
     * I died resets the count, that round's drinks included.
     * 自上次死亡以来（跨局）喝下 50 杯饮品（{@link LifetimeStats#consumedSinceDeath}）；死亡的那一局会清零，该局的饮品也不算。
     */
    static boolean fiftyDrinksInOneLife(AchievementContext context) {
        return context.stats().consumedSinceDeath(ConsumptionRules.DRINK) >= DRINKS_IN_ONE_LIFE;
    }

    /**
     * 满汉全席: 12 different items eaten or drunk over the account's lifetime ({@link LifetimeStats#consumedItems}).
     * 账号累计吃喝过 12 种不同的物品（{@link LifetimeStats#consumedItems}）。
     */
    static boolean twelveDifferentDishes(AchievementContext context) {
        return context.stats().consumedItems().size() >= DISTINCT_ITEMS;
    }

    /**
     * 最后的晚餐: after a death of mine on the killer faction ({@code victim_faction}), a 好人 dies of poison credited to me
     * (actor = me, so no Catalyst took it over) whose latest poisoning came from food or drink I poisoned
     * ({@code source} {@code wathe:food}, {@code poisoner_uuid} me).
     * 我以杀手阵营身份（{@code victim_faction}）死亡后，一名好人毒发身亡且记在我名下（凶手为我，因此没有被催化剂接手），其最近一次
     * 中毒来自我下了毒的食物或饮品（{@code source} 为 {@code wathe:food}，{@code poisoner_uuid} 为我）。
     */
    static boolean poisonKilledCivilianAfterIDied(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        Map<UUID, MatchEvent> latestPoisoning = new HashMap<>();
        boolean diedAsKiller = false;
        for (MatchEvent event : timeline.events()) {
            if (self == null) {
                return false;
            }
            if (event.is(MatchEvent.PLAYER_POISONED) && event.target() != null) {
                latestPoisoning.put(event.target(), event);
            } else if (event.is(MatchEvent.DEATH) && self.equals(event.target())) {
                diedAsKiller |= Factions.KILLER.equals(timeline.victimFaction(event));
            } else if (diedAsKiller && event.is(MatchEvent.DEATH) && self.equals(event.actor()) && event.target() != null
                    && Rules.reason(event, POISON) && Factions.isCivilian(timeline.victimFaction(event))) {
                MatchEvent poisoning = latestPoisoning.get(event.target());
                if (poisoning != null && FOOD_SOURCE.equals(poisoning.value("source"))
                        && self.equals(poisoning.uuid("poisoner_uuid"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 自食其果: as the Toxicologist, turn a plate's poison blue with Blue Vitriol ({@code item_use}
     * {@code sparkstrength:blue_vitriol}, {@code action=plate}, {@code pos}); the plate's poisoner (the latest
     * {@code wathe:poison_vial} use on that {@code pos}; a vial only takes on an unpoisoned plate) is on the killer
     * faction; the next take from that plate ({@code platter_take}, same {@code pos}) is theirs; they eat what they took
     * ({@code sparkfactionapi:consume}) and die of poison credited to me (blue poison is mine) within 1400 ticks. Blue
     * poison writes no {@code player_poisoned}, so this chain is the proof.
     * 以毒理学家身份用蓝矾把餐盘上的毒改成蓝毒（{@code item_use} {@code sparkstrength:blue_vitriol}，{@code action=plate}，带
     * {@code pos}）；该餐盘的下毒者（同一 {@code pos} 上最近一次 {@code wathe:poison_vial}；毒药瓶只能下在没毒的盘子上）属于杀手阵营；
     * 此后从这个盘子拿走的第一份餐点（{@code platter_take}，同一 {@code pos}）正是他拿的；他吃下拿走的东西
     * （{@code sparkfactionapi:consume}），并在 1400 tick 内毒发身亡且记在我名下（蓝毒的主人是我）。蓝毒不写
     * {@code player_poisoned}，所以靠这条事件链证明。
     */
    static boolean killerAteHisPoisonTurnedBlue(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        List<MatchEvent> events = timeline.events();
        for (int i = 0; i < events.size() && self != null; i++) {
            MatchEvent vitriol = events.get(i);
            if (!vitriol.is(MatchEvent.ITEM_USE) || !self.equals(vitriol.actor())
                    || !BLUE_VITRIOL.equals(vitriol.value("item")) || !PLATE_ACTION.equals(vitriol.value("action"))
                    || !hasPos(vitriol) || !Rules.iWas(context, vitriol, TOXICOLOGIST)) {
                continue;
            }
            MatchEvent vial = latestBefore(events, i, event -> event.is(MatchEvent.ITEM_USE)
                    && POISON_VIAL.equals(event.value("item")) && samePos(event, vitriol));
            if (vial == null || vial.actor() == null || vial.actor().equals(self)
                    || !Factions.KILLER.equals(timeline.factionAfter(vial.actor(), vial))) {
                continue;
            }
            int take = firstAfter(events, i, event -> event.is(MatchEvent.PLATTER_TAKE) && samePos(event, vitriol));
            if (take >= 0 && vial.actor().equals(events.get(take).actor())
                    && ateThenDiedOfMyPoison(events, take, self)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the taker at {@code take} later eats what they took and then first dies of poison credited to {@code self}
     * within {@link #LONGEST_POISON_TICKS} of eating.
     * 位置 {@code take} 的拿取者之后是否吃下拿走的东西，并在吃下后 {@link #LONGEST_POISON_TICKS} 内第一次死亡即为记在
     * {@code self} 名下的毒死。
     */
    private static boolean ateThenDiedOfMyPoison(List<MatchEvent> events, int take, UUID self) {
        MatchEvent taken = events.get(take);
        UUID taker = taken.actor();
        int meal = firstAfter(events, take, event -> event.is(MatchEvent.CONSUME) && taker.equals(event.actor())
                && taken.value("item", "").equals(event.value("item")));
        if (meal < 0) {
            return false;
        }
        int death = firstAfter(events, meal, event -> event.is(MatchEvent.DEATH) && taker.equals(event.target()));
        return death >= 0 && Rules.reason(events.get(death), POISON) && self.equals(events.get(death).actor())
                && events.get(death).tick() - events.get(meal).tick() <= LONGEST_POISON_TICKS;
    }

    /**
     * 回光返照: I won and was alive when the win was decided (Wathe's round-end status; dying of the poison during the
     * round-end fade still counts), and my current poisoning — the latest {@code player_poisoned} on me, or a Catalyst
     * on me — was never cured (Antidote {@code action=cure}) or stopped after it, while I killed its poisoner, a 非好人,
     * by my own hand (not by poison) after being poisoned. Wathe clears the poison before the poison death it then
     * tries, so a blocked or undone death leaves me poison-free: any {@code death} record of mine before the win was
     * decided ({@code match_end} minus {@link GeneralRules#STOP_FADE_TICKS}) ends the poisoning, and so does a Last
     * Stand, or a poison death of mine stopped by {@code shield_blocked} / {@code death_blocked},
     * {@link #GUARDIAN_SHIELD_ACTIVATED}, {@link #JESTER_FAKE_DEATH_KILL_BLOCKED} or Last Escape
     * ({@link TraitRecordRules#LAST_ESCAPE_TRIGGERED}). Needs the whole record. Cures or blocks that write no record
     * cannot be seen.
     * 我获胜，且判定胜负时仍存活（取 Wathe 的结算状态；在结束淡出期间毒发身亡也算），而我身上当前的毒（我最近一次
     * {@code player_poisoned}，或对我使用的催化剂）此后从未被解毒（解毒剂 {@code action=cure}）或挡下，同时我在中毒后亲手
     * （不是用毒）击杀了下毒的非好人。Wathe 在尝试毒死之前就清掉了毒，所以被挡下或被撤销的死亡会让我不再中毒：胜负判定
     * （{@code match_end} 减去 {@link GeneralRules#STOP_FADE_TICKS}）之前我的任何 {@code death} 记录都会结束这次中毒，
     * 背水一战也是；我的毒死被 {@code shield_blocked} / {@code death_blocked}、{@link #GUARDIAN_SHIELD_ACTIVATED}、
     * {@link #JESTER_FAKE_DEATH_KILL_BLOCKED} 或绝处逢生（{@link TraitRecordRules#LAST_ESCAPE_TRIGGERED}）挡下时同样如此。
     * 需要完整记录。不写记录的解毒或格挡无法察觉。
     */
    static boolean killedMyPoisonerBeforeItKilledMe(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        if (self == null || !timeline.isComplete() || !context.won() || !context.outcome().participant(self)
                .map(participant -> ALIVE.equals(participant.endStatus())).orElse(false)) {
            return false;
        }
        int decidedAt = timeline.matchEnd().map(end -> end.tick() - GeneralRules.STOP_FADE_TICKS).orElse(0);
        boolean poisoned = false;
        UUID poisoner = null;
        boolean killedPoisoner = false;
        for (MatchEvent event : timeline.events()) {
            UUID newPoisoner = poisonerIfPoisoning(event, self);
            if (newPoisoner != null || poisonedWithoutPoisoner(event, self)) {
                killedPoisoner = killedPoisoner && poisoned && newPoisoner != null && newPoisoner.equals(poisoner);
                poisoned = true;
                poisoner = newPoisoner;
            } else if (poisoned && savedFromPoison(event, self, decidedAt)) {
                poisoned = false;
                poisoner = null;
                killedPoisoner = false;
            } else if (poisoned && poisoner != null && event.is(MatchEvent.DEATH) && self.equals(event.actor())
                    && poisoner.equals(event.target()) && !Rules.reason(event, POISON)
                    && Rules.victimNonCivilian(context, event)) {
                killedPoisoner = true;
            }
        }
        return poisoned && killedPoisoner;
    }

    /** The poisoner of a poisoning of me ({@code player_poisoned} or a Catalyst), else null. 对我下毒的事件中的下毒者。 */
    private static UUID poisonerIfPoisoning(MatchEvent event, UUID self) {
        if (event.is(MatchEvent.PLAYER_POISONED) && self.equals(event.target())) {
            return event.uuid("poisoner_uuid");
        }
        if (event.is(MatchEvent.ITEM_USE) && CATALYST.equals(event.value("item")) && self.equals(event.target())) {
            return event.actor();
        }
        return null;
    }

    private static boolean poisonedWithoutPoisoner(MatchEvent event, UUID self) {
        return event.is(MatchEvent.PLAYER_POISONED) && self.equals(event.target()) && event.uuid("poisoner_uuid") == null;
    }

    /**
     * A cure of my poison, a poison death of mine stopped, or any death of mine before record tick {@code decidedAt}
     * (a later one is the poison catching up during the round-end fade).
     * 我的毒被解、我的毒死被挡下，或记录 tick {@code decidedAt} 之前我的任何一次死亡（之后的死亡是在结束淡出期间毒发）。
     */
    private static boolean savedFromPoison(MatchEvent event, UUID self, int decidedAt) {
        if (event.is(MatchEvent.ITEM_USE) && ANTIDOTE.equals(event.value("item")) && self.equals(event.target())) {
            return CURE_ACTION.equals(event.value("action", CURE_ACTION));
        }
        if ((event.is(MatchEvent.SHIELD_BLOCKED) || event.is(DEATH_BLOCKED)
                || event.isGlobal(JESTER_FAKE_DEATH_KILL_BLOCKED) || event.is(TraitRecordRules.LAST_ESCAPE_TRIGGERED))
                && self.equals(event.actor())
                || event.isGlobal(GUARDIAN_SHIELD_ACTIVATED) && self.equals(event.target())) {
            return Rules.reason(event, POISON);
        }
        return event.is(MatchEvent.DEATH) && self.equals(event.target()) && event.tick() < decidedAt
                || event.isGlobal(MatchEvent.LAST_STAND_TRIGGERED) && self.equals(event.actor());
    }

    // ---- Record order 记录顺序 ----

    private static MatchEvent latestBefore(List<MatchEvent> events, int before, Predicate<MatchEvent> test) {
        for (int i = before - 1; i >= 0; i--) {
            if (test.test(events.get(i))) {
                return events.get(i);
            }
        }
        return null;
    }

    private static int firstAfter(List<MatchEvent> events, int after, Predicate<MatchEvent> test) {
        for (int i = after + 1; i < events.size(); i++) {
            if (test.test(events.get(i))) {
                return i;
            }
        }
        return -1;
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
