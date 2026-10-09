package dev.caecorthus.sparkassist.achievement;

import dev.caecorthus.sparkassist.achievement.MatchTimeline.TickWindow;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Small queries the achievement rules share. Each one is safe on a round without a match record and on events
 * missing fields an older server never wrote: it simply finds nothing.
 * 成就规则共用的小查询。没有对局记录、或事件缺少旧服务器没写的字段时都安全：只是什么也找不到。
 */
final class Rules {
    static final String DEATH_REASON = "death_reason";

    private Rules() {
    }

    // ---- My events 我的事件 ----

    /** My {@code global_event}s with this id. 我触发的、id 为此值的 {@code global_event}。 */
    static List<MatchEvent> myGlobalEvents(AchievementContext context, String eventId) {
        return withValue(context.myEvents(MatchEvent.GLOBAL_EVENT), "event", eventId);
    }

    /** My {@code item_use}s of this item. 我对这件物品的 {@code item_use}。 */
    static List<MatchEvent> myItemUses(AchievementContext context, String itemId) {
        return withValue(context.myEvents(MatchEvent.ITEM_USE), "item", itemId);
    }

    /** My {@code skill_use}s of this skill. 我对这个技能的 {@code skill_use}。 */
    static List<MatchEvent> mySkillUses(AchievementContext context, String skillId) {
        return withValue(context.myEvents(MatchEvent.SKILL_USE), "skill", skillId);
    }

    static List<MatchEvent> withValue(List<MatchEvent> events, String key, String value) {
        return events.stream().filter(event -> value.equals(event.value(key))).toList();
    }

    /** Whether I held {@code roleId} when {@code event} happened. {@code event} 发生时我是否为该身份。 */
    static boolean iWas(AchievementContext context, MatchEvent event, String roleId) {
        return roleId.equals(context.roleAt(context.self(), event));
    }

    // ---- Kills 击杀 ----

    /** My kills (never by my own hand) that pass {@code test}. 我满足 {@code test} 的击杀（不含死于自己之手）。 */
    static List<MatchEvent> myKills(AchievementContext context, Predicate<MatchEvent> test) {
        return context.kills().stream().filter(test).toList();
    }

    /** Whether this death has {@code reason}. 这次死亡的死因是否为 {@code reason}。 */
    static boolean reason(MatchEvent death, String reason) {
        return reason.equals(death.value(DEATH_REASON));
    }

    /** Whether I killed as {@code roleId}. 我击杀时是否为该身份。 */
    static boolean killedAs(AchievementContext context, MatchEvent kill, String roleId) {
        return roleId.equals(context.timeline().killerRole(kill));
    }

    /** "非好人" victim: their effective faction at death is a real faction other than civilian. 死者在死亡时是非好人。 */
    static boolean victimNonCivilian(AchievementContext context, MatchEvent death) {
        return Factions.isNonCivilian(context.timeline().victimFaction(death));
    }

    /** The most of {@code events} that share one record tick. {@code events} 中同一记录 tick 的最多数量。 */
    static int mostAtOneTick(List<MatchEvent> events) {
        return MatchTimeline.groupByTick(events).stream().mapToInt(List::size).max().orElse(0);
    }

    /** How many of {@code events} fall inside any of {@code windows}. 落在任一时段内的事件数。 */
    static int countInWindows(List<MatchEvent> events, List<TickWindow> windows) {
        return (int) events.stream()
                .filter(event -> windows.stream().anyMatch(window -> window.contains(event.tick())))
                .count();
    }

    /** The most of {@code events} inside one of {@code windows}. 单个时段内的最多事件数。 */
    static int mostInOneWindow(List<MatchEvent> events, List<TickWindow> windows) {
        return windows.stream()
                .mapToInt(window -> (int) events.stream().filter(event -> window.contains(event.tick())).count())
                .max().orElse(0);
    }

    /**
     * When my on/off state from {@code type} events ({@code active} 1/0) was on. A window that never ended closes at
     * my death or at the end of the record.
     * 我由 {@code type} 事件（{@code active} 1/0）切换的状态处于开启的时段。没有结束的时段在我死亡或记录结束时关闭。
     */
    static List<TickWindow> myToggleWindows(AchievementContext context, String type) {
        UUID self = context.self();
        List<TickWindow> windows = new ArrayList<>();
        Integer openSince = null;
        for (MatchEvent event : context.timeline().events()) {
            boolean mine = self != null && self.equals(event.actor());
            if (mine && event.is(type) && event.has("active")) {
                if (event.bool("active")) {
                    openSince = openSince == null ? event.tick() : openSince;
                } else if (openSince != null) {
                    windows.add(new TickWindow(openSince, Math.max(openSince, event.tick())));
                    openSince = null;
                }
            } else if (openSince != null && event.is(MatchEvent.DEATH) && self != null && self.equals(event.target())) {
                windows.add(new TickWindow(openSince, Math.max(openSince, event.tick())));
                openSince = null;
            }
        }
        if (openSince != null) {
            windows.add(new TickWindow(openSince, Math.max(openSince, context.timeline().endTick())));
        }
        return List.copyOf(windows);
    }

    // ---- Traits 词条 ----

    /**
     * Whether I held all of {@code traitIds} at the same moment around record tick {@code recordTick} (within
     * {@link RoundFacts#RECORD_TICK_TOLERANCE}).
     * 在记录 tick {@code recordTick} 前后（{@link RoundFacts#RECORD_TICK_TOLERANCE} 以内）是否在同一时刻同时持有全部词条。
     */
    static boolean heldTogetherAround(AchievementContext context, int recordTick, String... traitIds) {
        TraitTimeline traits = context.facts().traits();
        long from = (long) recordTick - RoundFacts.RECORD_TICK_TOLERANCE;
        long to = (long) recordTick + RoundFacts.RECORD_TICK_TOLERANCE;
        Set<String> wanted = Set.of(traitIds);
        if (traits.at(from).containsAll(wanted)) {
            return true;
        }
        return traits.changes().stream().anyMatch(change -> change.tick() > from && change.tick() <= to
                && change.traitIds().containsAll(wanted));
    }
}
