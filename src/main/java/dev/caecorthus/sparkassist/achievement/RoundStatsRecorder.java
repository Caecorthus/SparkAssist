package dev.caecorthus.sparkassist.achievement;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Adds one settled round to the account's {@link LifetimeStats}. Runs before achievements are evaluated, so rules
 * see stats with this round already counted.
 * <ul>
 *     <li>the general round counts ({@link LifetimeStats#recordRound});</li>
 *     <li>food and drink the player finished ({@code sparkfactionapi:consume}, by {@code kind});</li>
 *     <li>for every kill (dying by your own hand is not one), the victim's role at death goes into the killed-role
 *     set of each {@link RoleGroups} group the player was in at that kill;</li>
 *     <li>streak sets ({@link RoleGroups#STREAK_GROUPS}) last one life across rounds: if the player died this round
 *     while in that group, the group's streak set is emptied and this round's kills in it are not added.</li>
 * </ul>
 * Without a match record only the general counts change, except that a death the client saw still ends streaks,
 * judged by the role and faction the client saw last.
 * <p>
 * 把一局已结算的对局记入账号的 {@link LifetimeStats}。在判定成就之前运行，规则因此看到已计入本局的统计：
 * 通用对局计数；玩家吃完、喝完的东西（{@code sparkfactionapi:consume}，按 {@code kind}）；每次击杀（死于自己之手不算）
 * 时死者的身份，计入玩家当时所在的每个 {@link RoleGroups} 分组的击杀身份集合；连续击杀集合（{@link RoleGroups#STREAK_GROUPS}）
 * 跨局延续一条命：本局在该分组中死亡时清空该分组的集合，且不计入本局该分组的击杀。
 * 没有对局记录时只更新通用计数，但客户端看到的死亡仍会按客户端最后看到的身份与阵营结束连续击杀。
 */
public final class RoundStatsRecorder {
    private RoundStatsRecorder() {
    }

    public static void record(LifetimeStats stats, RoundFacts facts, RoundOutcome outcome, MatchTimeline timeline) {
        stats.recordRound(facts.roleChain(), facts.factionId(), outcome.won(), facts.died(), facts.tasksCompleted());
        UUID self = facts.self();
        recordConsumed(stats, self, timeline);

        Map<String, Set<String>> killedThisRound = new TreeMap<>();
        for (MatchEvent kill : timeline.killsBy(self)) {
            String victimRole = timeline.victimRole(kill);
            if (victimRole == null) {
                continue;
            }
            for (String group : RoleGroups.groupsOf(timeline.killerRole(kill), timeline.killerFaction(kill))) {
                killedThisRound.computeIfAbsent(group, key -> new LinkedHashSet<>()).add(victimRole);
            }
        }
        killedThisRound.forEach(stats::addKilledRoles);

        Set<String> diedIn = groupsAtDeath(facts, timeline);
        for (String group : RoleGroups.STREAK_GROUPS) {
            if (diedIn.contains(group)) {
                stats.resetStreak(group);
            } else if (killedThisRound.containsKey(group)) {
                stats.addStreakKilledRoles(group, killedThisRound.get(group));
            }
        }
    }

    private static void recordConsumed(LifetimeStats stats, UUID self, MatchTimeline timeline) {
        for (MatchEvent consume : timeline.eventsBy(MatchEvent.CONSUME, self)) {
            stats.recordConsumed(consume.value("kind", null), 1);
        }
    }

    /**
     * The groups the player was in when they died this round, empty if they survived. The death record decides;
     * whatever it cannot tell falls back to the role and faction the client saw last.
     * 玩家本局死亡时所在的分组；存活则为空。以死亡记录为准，记录给不出的部分取客户端最后看到的身份与阵营。
     */
    private static Set<String> groupsAtDeath(RoundFacts facts, MatchTimeline timeline) {
        Optional<MatchEvent> death = timeline.deathOf(facts.self());
        if (death.isEmpty() && !facts.died()) {
            return Set.of();
        }
        String role = death.map(timeline::victimRole).orElse(null);
        String faction = death.map(timeline::victimFaction).orElse(null);
        return RoleGroups.groupsOf(
                role != null ? role : facts.finalRoleId(),
                faction != null ? faction : facts.factionId());
    }
}
