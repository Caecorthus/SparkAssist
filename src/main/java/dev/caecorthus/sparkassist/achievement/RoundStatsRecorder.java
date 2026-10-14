package dev.caecorthus.sparkassist.achievement;

import java.util.LinkedHashSet;
import java.util.List;
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
 *     <li>players the player's Iron Man vials saved from death ({@link IronManSaves});</li>
 *     <li>for every kill (dying by your own hand is not one), the victim's role at death goes into the killed-role
 *     set of each {@link RoleGroups} group the player was in at that kill;</li>
 *     <li>streak sets ({@link RoleGroups#STREAK_GROUPS}) last one life across rounds: if the player died this round
 *     while in that group, the group's streak set is emptied and this round's kills in it are not added;</li>
 *     <li>the death reasons of the player's deaths (record first, else the reason on their body; a disconnect is
 *     not one);</li>
 *     <li>win streaks ({@link LifetimeStats#recordWinStreaks}): the survived ones per {@link LifetimeStats#ALL_ROUNDS}
 *     and each {@link RoleGroups} group the player was in at the end (at their death if they died);</li>
 *     <li>food and drink since the last death (emptied by a death this round, which also keeps this round's from
 *     counting), every item consumed, and food eaten per owner-visible trait held around the meal;</li>
 *     <li>SparkWitch Fisher catches, kills of players on another faction per role (with the traits held around
 *     them), and roles the player was turned into mid-round with their causes.</li>
 * </ul>
 * "Died this round" means any death: the client saw the player dead at some point, or the record has a death of
 * theirs, including a Last Stand false death and a death undone by the T-Virus. A round left mid-way never settles,
 * so it never comes here and breaks no streak.
 * Without a match record only the general counts change, except that a death the client saw still ends streaks,
 * judged by the role and faction the client saw last.
 * <p>
 * 把一局已结算的对局记入账号的 {@link LifetimeStats}。在判定成就之前运行，规则因此看到已计入本局的统计：
 * 通用对局计数；玩家吃完、喝完的东西（{@code sparkfactionapi:consume}，按 {@code kind}）；玩家的铁人药剂救下的玩家次数（{@link IronManSaves}）；每次击杀（死于自己之手不算）
 * 时死者的身份，计入玩家当时所在的每个 {@link RoleGroups} 分组的击杀身份集合；连续击杀集合（{@link RoleGroups#STREAK_GROUPS}）
 * 跨局延续一条命：本局在该分组中死亡时清空该分组的集合，且不计入本局该分组的击杀；玩家各次死亡的死因（以记录为准，
 * 否则取尸体上的死因；断线不算）；连胜（{@link LifetimeStats#recordWinStreaks}）：存活连胜按 {@link LifetimeStats#ALL_ROUNDS}
 * 和玩家结束时（死亡则为死亡时）所在的每个 {@link RoleGroups} 分组分别计；上次死亡以来的吃喝数（本局死亡则清空，且本局的不计入）、
 * 吃喝过的每种物品，以及进食时持有的每个自身可见词条的进食数；SparkWitch 钓鱼佬的渔获、按身份统计的对其他阵营的击杀（及当时持有的词条），
 * 以及玩家在对局中途变成的身份及其原因。
 * “本局死亡”指任何一次死亡：客户端某一刻看到玩家死亡，或记录里有玩家的死亡，包括背水一战的假死和被 T 病毒复活的死亡。
 * 中途退出的对局不会结算，不会走到这里，也不会打断任何连续记录。
 * 没有对局记录时只更新通用计数，但客户端看到的死亡仍会按客户端最后看到的身份与阵营结束连续击杀。
 */
public final class RoundStatsRecorder {
    static final String FISHING_ROD = "sparkwitch:fishing_rod";
    static final String FISHER = "sparkwitch:fisher";
    static final String CATCH = "catch";

    private RoundStatsRecorder() {
    }

    public static void record(LifetimeStats stats, RoundFacts facts, RoundOutcome outcome, MatchTimeline timeline) {
        stats.recordRound(facts.roleChain(), facts.factionId(), outcome.won(), facts.died(), facts.tasksCompleted());
        UUID self = facts.self();
        recordConsumed(stats, self, timeline);
        stats.recordSaves(IronManSaves.VIAL, IronManSaves.byMe(timeline, self));

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

        boolean died = facts.died() || timeline.deathOf(self).isPresent();
        recordDeathReasons(stats, facts, timeline);
        stats.recordWinStreaks(outcome.won(), !died,
                died ? diedIn : RoleGroups.groupsOf(facts.finalRoleId(), facts.factionId()));
        recordConsumedSinceDeath(stats, self, timeline, died);
        recordFoodWithTraits(stats, facts, timeline);
        recordFishCaught(stats, self, timeline);
        recordRivalKills(stats, facts, timeline);
        recordRolesTaken(stats, self, timeline);
    }

    private static void recordConsumed(LifetimeStats stats, UUID self, MatchTimeline timeline) {
        for (MatchEvent consume : timeline.eventsBy(MatchEvent.CONSUME, self)) {
            stats.recordConsumed(consume.value("kind", null), 1);
            stats.recordConsumedItem(consume.value("item", null), 1);
        }
    }

    /** Every death of mine in the record, else the reason on my body. 记录里我的每次死亡，否则取我尸体上的死因。 */
    private static void recordDeathReasons(LifetimeStats stats, RoundFacts facts, MatchTimeline timeline) {
        List<MatchEvent> deaths = timeline.eventsOn(MatchEvent.DEATH, facts.self());
        if (deaths.isEmpty()) {
            stats.recordDeathReason(facts.deathReason());
        }
        deaths.forEach(death -> stats.recordDeathReason(death.value(Rules.DEATH_REASON, null)));
    }

    private static void recordConsumedSinceDeath(LifetimeStats stats, UUID self, MatchTimeline timeline, boolean died) {
        if (died) {
            stats.resetConsumedSinceDeath();
            return;
        }
        for (MatchEvent consume : timeline.eventsBy(MatchEvent.CONSUME, self)) {
            stats.addConsumedSinceDeath(consume.value("kind", null), 1);
        }
    }

    private static void recordFoodWithTraits(LifetimeStats stats, RoundFacts facts, MatchTimeline timeline) {
        for (MatchEvent consume : timeline.eventsBy(MatchEvent.CONSUME, facts.self())) {
            if (ConsumptionRules.FOOD.equals(consume.value("kind"))) {
                facts.traitsAround(consume.tick()).forEach(traitId -> stats.recordFoodWithTrait(traitId, 1));
            }
        }
    }

    /** My Fishing Rod casts as the Fisher, by catch. 我以钓鱼佬身份的每一竿，按渔获。 */
    private static void recordFishCaught(LifetimeStats stats, UUID self, MatchTimeline timeline) {
        for (MatchEvent cast : timeline.eventsBy(MatchEvent.ITEM_USE, self)) {
            if (FISHING_ROD.equals(cast.value("item")) && FISHER.equals(timeline.roleAt(self, cast))) {
                stats.recordFishCaught(cast.value(CATCH, null), 1);
            }
        }
    }

    /**
     * Kills whose victim's faction is known and differs from mine at the kill, per my role then, with the traits I
     * held around them. {@link Factions#NONE} is not a faction here.
     * 死者阵营已知且与我击杀时的阵营不同的击杀，按我当时的身份计，并记下当时持有的词条。{@link Factions#NONE} 在这里不算阵营。
     */
    private static void recordRivalKills(LifetimeStats stats, RoundFacts facts, MatchTimeline timeline) {
        for (MatchEvent kill : timeline.killsBy(facts.self())) {
            String mine = timeline.killerFaction(kill);
            String theirs = timeline.victimFaction(kill);
            if (isFaction(mine) && isFaction(theirs) && !mine.equals(theirs)) {
                stats.recordRivalKill(timeline.killerRole(kill), facts.traitsAround(kill.tick()));
            }
        }
    }

    private static boolean isFaction(String factionId) {
        return factionId != null && !factionId.isBlank() && !Factions.NONE.equals(factionId);
    }

    private static void recordRolesTaken(LifetimeStats stats, UUID self, MatchTimeline timeline) {
        if (self == null) {
            return;
        }
        for (MatchEvent change : timeline.events(MatchEvent.ROLE_CHANGED)) {
            UUID player = change.uuid("player") != null ? change.uuid("player") : change.actor();
            if (self.equals(player)) {
                stats.recordRoleTaken(change.value("to", null), change.value("cause", null));
            }
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
