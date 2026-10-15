package dev.caecorthus.sparkassist.achievement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * General per-account round statistics kept next to the unlocked achievements. They describe how the player has
 * played, never how close they are to any achievement: no field is named after one, so reading the save file
 * reveals nothing about what is left to find. Fields a newer SparkAssist wrote are kept and written back untouched,
 * so a downgrade never drops them.
 * <p>
 * Killed-role sets are kept per kill group (see {@link RoleGroups}): {@link #killedRoles} never shrinks, while
 * {@link #streakKilledRoles} is emptied by {@link #resetStreak}, e.g. when the player dies in that group.
 * <p>
 * Streaks ({@link #winStreak}, {@link #survivedWinStreak}, {@link #consumedSinceDeath}) only move on a settled round:
 * leaving mid-round never settles, so it neither counts nor breaks anything (an intended leniency).
 * <p>
 * 与已解锁成就一起保存的账号通用对局统计。只记录玩家怎么玩过，不记录离某个成就还差多少：
 * 没有任何字段以成就命名，所以翻看存档也看不出还剩什么没发现。较新版本 SparkAssist 写入的字段会原样保留并写回，降级不会丢失。
 * 击杀身份集合按击杀分组保存（见 {@link RoleGroups}）：{@link #killedRoles} 只增不减，{@link #streakKilledRoles}
 * 会被 {@link #resetStreak} 清空，例如玩家在该分组中死亡时。
 * 连续类统计（{@link #winStreak}、{@link #survivedWinStreak}、{@link #consumedSinceDeath}）只在一局结算时变化：
 * 中途退出的对局不结算，既不计入也不打断任何连续记录（有意从宽）。
 */
public final class LifetimeStats {
    private static final String ROUNDS = "rounds";
    private static final String WINS = "wins";
    private static final String DEATHS = "deaths";
    private static final String TASKS = "tasks";
    private static final String ROLE_ROUNDS = "roleRounds";
    private static final String ROLE_WINS = "roleWins";
    private static final String FACTION_ROUNDS = "factionRounds";
    private static final String FACTION_WINS = "factionWins";
    private static final String CONSUMED = "consumed";
    private static final String KILLED_ROLES = "killedRoles";
    private static final String STREAK_KILLED_ROLES = "streakKilledRoles";
    private static final String SAVES = "saves";
    private static final String DEATH_REASONS = "deathReasons";
    private static final String WIN_STREAK = "winStreak";
    private static final String SURVIVED_WIN_STREAKS = "survivedWinStreaks";
    private static final String CONSUMED_SINCE_DEATH = "consumedSinceDeath";
    private static final String CONSUMED_ITEMS = "consumedItems";
    private static final String FOOD_WITH_TRAITS = "foodWithTraits";
    private static final String FISH_CAUGHT = "fishCaught";
    private static final String RIVAL_KILLS = "rivalKills";
    private static final String RIVAL_KILL_TRAITS = "rivalKillTraits";
    private static final String ROLES_TAKEN = "rolesTaken";
    private static final String HELD_ROLE_WINS = "heldRoleWins";
    private static final Set<String> KNOWN_FIELDS = Set.of(ROUNDS, WINS, DEATHS, TASKS, ROLE_ROUNDS, ROLE_WINS,
            FACTION_ROUNDS, FACTION_WINS, CONSUMED, KILLED_ROLES, STREAK_KILLED_ROLES, SAVES, DEATH_REASONS, WIN_STREAK,
            SURVIVED_WIN_STREAKS, CONSUMED_SINCE_DEATH, CONSUMED_ITEMS, FOOD_WITH_TRAITS, FISH_CAUGHT, RIVAL_KILLS,
            RIVAL_KILL_TRAITS, ROLES_TAKEN, HELD_ROLE_WINS);

    /**
     * The {@link #survivedWinStreak} scope every settled round is in, whatever the role.
     * 每一局已结算的对局都属于的 {@link #survivedWinStreak} 范围，与身份无关。
     */
    public static final String ALL_ROUNDS = "all";

    private int rounds;
    private int wins;
    private int deaths;
    private int tasks;
    // Sorted so the save file diffs stay stable. 排序保存，存档内容稳定。
    private final Map<String, Integer> roleRounds = new TreeMap<>();
    private final Map<String, Integer> roleWins = new TreeMap<>();
    private final Map<String, Integer> factionRounds = new TreeMap<>();
    private final Map<String, Integer> factionWins = new TreeMap<>();
    private final Map<String, Integer> consumed = new TreeMap<>();
    private final Map<String, Integer> saves = new TreeMap<>();
    private final Map<String, Set<String>> killedRoles = new TreeMap<>();
    private final Map<String, Set<String>> streakKilledRoles = new TreeMap<>();
    private final Set<String> deathReasons = new TreeSet<>();
    private int winStreak;
    private final Map<String, Integer> survivedWinStreaks = new TreeMap<>();
    private final Map<String, Integer> consumedSinceDeath = new TreeMap<>();
    private final Map<String, Integer> consumedItems = new TreeMap<>();
    private final Map<String, Integer> foodWithTraits = new TreeMap<>();
    private final Map<String, Integer> fishCaught = new TreeMap<>();
    private final Map<String, Integer> rivalKills = new TreeMap<>();
    private final Map<String, Set<String>> rivalKillTraits = new TreeMap<>();
    // Role → causes; a role taken without a recorded cause has an empty set. 身份 → 原因；没有记录原因时为空集合。
    private final Map<String, Set<String>> rolesTaken = new TreeMap<>();
    // Wins counted for every distinct role held in the won round, not just the last. 获胜那局担任过的每个不同身份各记一胜，不只是最后一个。
    private final Map<String, Integer> heldRoleWins = new TreeMap<>();
    // Written by a newer SparkAssist; kept as is. 较新版本写入的字段，原样保留。
    private final Map<String, JsonElement> laterFields = new TreeMap<>();

    /**
     * Adds one settled round. Every distinct role the player held counts one round; a win counts for the last role
     * only, since that is the role that won, and separately for every distinct role held ({@link #winsHolding}). A
     * null faction is counted in the totals only.
     * 记入一局已结算的对局。本局担任过的每个不同角色各记一局；胜场只记在最后一个角色上，因为获胜的是它，
     * 另外也给担任过的每个不同角色各记一胜（{@link #winsHolding}）。阵营为 null 时只计入总数。
     */
    public void recordRound(List<String> roleChain, String factionId, boolean won, boolean died, int tasksCompleted) {
        rounds++;
        if (won) {
            wins++;
        }
        if (died) {
            deaths++;
        }
        tasks += Math.max(0, tasksCompleted);
        new LinkedHashSet<>(roleChain).forEach(roleId -> roleRounds.merge(roleId, 1, Integer::sum));
        if (won && !roleChain.isEmpty()) {
            roleWins.merge(roleChain.getLast(), 1, Integer::sum);
            new LinkedHashSet<>(roleChain).forEach(roleId -> heldRoleWins.merge(roleId, 1, Integer::sum));
        }
        if (factionId != null) {
            factionRounds.merge(factionId, 1, Integer::sum);
            if (won) {
                factionWins.merge(factionId, 1, Integer::sum);
            }
        }
    }

    /**
     * Counts {@code count} things consumed of {@code kind} ({@code food} or {@code drink}).
     * 记入 {@code count} 次 {@code kind}（{@code food} 或 {@code drink}）的食用。
     */
    public void recordConsumed(String kind, int count) {
        if (kind != null && !kind.isBlank() && count > 0) {
            consumed.merge(kind, count, Integer::sum);
        }
    }

    /**
     * Counts {@code count} times another player was saved from death by {@code source} (an item id such as
     * {@code noellesroles:iron_man_vial}) that the player gave them.
     * 记入 {@code count} 次其他玩家靠本玩家给的 {@code source}（物品 id，例如 {@code noellesroles:iron_man_vial}）免于一死。
     */
    public void recordSaves(String source, int count) {
        if (source != null && !source.isBlank() && count > 0) {
            saves.merge(source, count, Integer::sum);
        }
    }

    /** Roles killed while in {@code group}, kept forever. 在 {@code group} 分组中击杀过的身份，永久保留。 */
    public void addKilledRoles(String group, Collection<String> roleIds) {
        addRoles(killedRoles, group, roleIds);
    }

    /** Roles killed while in {@code group} since the last {@link #resetStreak}. 自上次 {@link #resetStreak} 以来在该分组中击杀过的身份。 */
    public void addStreakKilledRoles(String group, Collection<String> roleIds) {
        addRoles(streakKilledRoles, group, roleIds);
    }

    /** Empties {@code group}'s streak set. 清空 {@code group} 的连续击杀身份集合。 */
    public void resetStreak(String group) {
        streakKilledRoles.remove(group);
    }

    /**
     * Adds the death reason (e.g. {@code wathe:knife_stab}) of one of the player's deaths; a disconnect
     * ({@link MatchEvent#ESCAPED}) is not a way to die and is ignored.
     * 记入玩家一次死亡的死因（例如 {@code wathe:knife_stab}）；断线（{@link MatchEvent#ESCAPED}）不算死法，忽略。
     */
    public void recordDeathReason(String reason) {
        if (reason != null && !reason.isBlank() && !MatchEvent.ESCAPED.equals(reason)) {
            deathReasons.add(reason);
        }
    }

    /**
     * Moves the win streaks on by one settled round. {@link #winStreak} grows on a win and drops to 0 otherwise. Each
     * {@link #survivedWinStreak} scope the player was in this round ({@link #ALL_ROUNDS} always, plus {@code groups},
     * e.g. the {@link RoleGroups} groups at the end of the round) grows on a win they survived and drops to 0
     * otherwise; scopes they were not in stay as they are.
     * 按一局已结算的对局推进连胜。{@link #winStreak} 获胜时加一，否则归零。玩家本局所在的每个 {@link #survivedWinStreak}
     * 范围（总是包括 {@link #ALL_ROUNDS}，再加上 {@code groups}，例如本局结束时所在的 {@link RoleGroups} 分组）在存活获胜时加一，
     * 否则归零；本局不在的范围保持不变。
     */
    public void recordWinStreaks(boolean won, boolean survived, Collection<String> groups) {
        winStreak = won ? winStreak + 1 : 0;
        Set<String> scopes = new LinkedHashSet<>();
        scopes.add(ALL_ROUNDS);
        if (groups != null) {
            groups.stream().filter(group -> group != null && !group.isBlank()).forEach(scopes::add);
        }
        for (String scope : scopes) {
            if (won && survived) {
                survivedWinStreaks.merge(scope, 1, Integer::sum);
            } else {
                survivedWinStreaks.remove(scope);
            }
        }
    }

    /** Empties every {@link #consumedSinceDeath} count, e.g. on dying. 清空所有 {@link #consumedSinceDeath} 计数，例如死亡时。 */
    public void resetConsumedSinceDeath() {
        consumedSinceDeath.clear();
    }

    /**
     * Counts {@code count} things of {@code kind} ({@code food}, {@code drink}) consumed since the last death.
     * 记入自上次死亡以来 {@code count} 次 {@code kind}（{@code food}、{@code drink}）的食用。
     */
    public void addConsumedSinceDeath(String kind, int count) {
        addCount(consumedSinceDeath, kind, count);
    }

    /** Counts {@code count} of {@code itemId} eaten or drunk. 记入 {@code count} 次食用 {@code itemId}。 */
    public void recordConsumedItem(String itemId, int count) {
        addCount(consumedItems, itemId, count);
    }

    /**
     * Counts {@code count} food eaten while holding the owner-visible trait {@code traitId}.
     * 记入 {@code count} 次在持有自身可见词条 {@code traitId} 时吃下的食物。
     */
    public void recordFoodWithTrait(String traitId, int count) {
        addCount(foodWithTraits, traitId, count);
    }

    /**
     * Counts {@code count} casts that landed {@code catchName} (SparkWitch's catch name, e.g. {@code SALMON},
     * {@code SKUNK} for nothing).
     * 记入 {@code count} 次钓到 {@code catchName}（SparkWitch 的渔获名，例如 {@code SALMON}，空军为 {@code SKUNK}）。
     */
    public void recordFishCaught(String catchName, int count) {
        addCount(fishCaught, catchName, count);
    }

    /**
     * Counts one kill of a player on another faction made as {@code roleId}, with the owner-visible traits held
     * around it.
     * 记入一次以 {@code roleId} 身份击杀其他阵营玩家，以及当时持有的自身可见词条。
     */
    public void recordRivalKill(String roleId, Collection<String> traitIds) {
        if (roleId == null || roleId.isBlank()) {
            return;
        }
        rivalKills.merge(roleId, 1, Integer::sum);
        addRoles(rivalKillTraits, roleId, traitIds);
    }

    /**
     * Notes that the player was turned into {@code roleId} mid-round, by {@code cause} (a role-change cause id such as
     * {@code sparkwitch:bewitched_promotion}, or null when none was recorded).
     * 记下玩家在对局中途变成了 {@code roleId}，原因是 {@code cause}（身份变化原因 id，例如
     * {@code sparkwitch:bewitched_promotion}；没有记录原因时为 null）。
     */
    public void recordRoleTaken(String roleId, String cause) {
        if (roleId == null || roleId.isBlank()) {
            return;
        }
        Set<String> causes = rolesTaken.computeIfAbsent(roleId, key -> new TreeSet<>());
        if (cause != null && !cause.isBlank()) {
            causes.add(cause);
        }
    }

    public int rounds() {
        return rounds;
    }

    public int wins() {
        return wins;
    }

    public int deaths() {
        return deaths;
    }

    public int tasks() {
        return tasks;
    }

    public int roundsAs(String roleId) {
        return roleRounds.getOrDefault(roleId, 0);
    }

    public int winsAs(String roleId) {
        return roleWins.getOrDefault(roleId, 0);
    }

    /**
     * Rounds won while holding {@code roleId} at some point, whatever role the round ended with (e.g. a Grand Witch
     * who became a Wraith and won as a Curser). Saves from before this count existed start from {@link #winsAs}.
     * 在某一时刻担任过 {@code roleId} 且获胜的局数，不论一局结束时是什么身份（例如大魔女变成冤魂、再以诅咒者获胜）。
     * 这项统计出现之前的存档以 {@link #winsAs} 为起点。
     */
    public int winsHolding(String roleId) {
        return heldRoleWins.getOrDefault(roleId, 0);
    }

    public Map<String, Integer> heldRoleWins() {
        return Collections.unmodifiableMap(heldRoleWins);
    }

    public int roundsFor(String factionId) {
        return factionRounds.getOrDefault(factionId, 0);
    }

    public int winsFor(String factionId) {
        return factionWins.getOrDefault(factionId, 0);
    }

    /** Every role played at least once, with its round count. 每个玩过的角色及其局数。 */
    public Map<String, Integer> roleRounds() {
        return Collections.unmodifiableMap(roleRounds);
    }

    public Map<String, Integer> roleWins() {
        return Collections.unmodifiableMap(roleWins);
    }

    public Map<String, Integer> factionRounds() {
        return Collections.unmodifiableMap(factionRounds);
    }

    public Map<String, Integer> factionWins() {
        return Collections.unmodifiableMap(factionWins);
    }

    /** Things consumed of {@code kind} ({@code food}, {@code drink}). {@code kind}（{@code food}、{@code drink}）的食用次数。 */
    public int consumed(String kind) {
        return consumed.getOrDefault(kind, 0);
    }

    public Map<String, Integer> consumed() {
        return Collections.unmodifiableMap(consumed);
    }

    /** Players saved from death by {@code source}. 靠 {@code source} 免于一死的玩家次数。 */
    public int saves(String source) {
        return saves.getOrDefault(source, 0);
    }

    public Map<String, Integer> saves() {
        return Collections.unmodifiableMap(saves);
    }

    /** Every role killed while in {@code group}, sorted. 在 {@code group} 分组中击杀过的全部身份，已排序。 */
    public Set<String> killedRoles(String group) {
        return view(killedRoles, group);
    }

    /** Roles killed while in {@code group} since its last reset, sorted. 该分组上次清空以来击杀过的身份，已排序。 */
    public Set<String> streakKilledRoles(String group) {
        return view(streakKilledRoles, group);
    }

    /** Every way the player has died (death reason ids), sorted. 玩家死过的所有方式（死因 id），已排序。 */
    public Set<String> deathReasons() {
        return Collections.unmodifiableSet(deathReasons);
    }

    /** Settled rounds won in a row, alive or not. 连续获胜的已结算局数，不论存活。 */
    public int winStreak() {
        return winStreak;
    }

    /**
     * Rounds in a row won and survived within {@code scope}: {@link #ALL_ROUNDS} or a {@link RoleGroups} group.
     * 在 {@code scope}（{@link #ALL_ROUNDS} 或某个 {@link RoleGroups} 分组）内连续存活并获胜的局数。
     */
    public int survivedWinStreak(String scope) {
        return survivedWinStreaks.getOrDefault(scope, 0);
    }

    public Map<String, Integer> survivedWinStreaks() {
        return Collections.unmodifiableMap(survivedWinStreaks);
    }

    /**
     * Things of {@code kind} ({@code food}, {@code drink}) consumed since the player last died, across rounds.
     * 自玩家上次死亡以来（跨局）{@code kind}（{@code food}、{@code drink}）的食用次数。
     */
    public int consumedSinceDeath(String kind) {
        return consumedSinceDeath.getOrDefault(kind, 0);
    }

    public Map<String, Integer> consumedSinceDeath() {
        return Collections.unmodifiableMap(consumedSinceDeath);
    }

    /** Times {@code itemId} was eaten or drunk. 吃喝 {@code itemId} 的次数。 */
    public int consumedItem(String itemId) {
        return consumedItems.getOrDefault(itemId, 0);
    }

    /** Every item ever eaten or drunk, with its count. 吃喝过的所有物品及次数。 */
    public Map<String, Integer> consumedItems() {
        return Collections.unmodifiableMap(consumedItems);
    }

    /** Food eaten while holding {@code traitId}. 持有 {@code traitId} 时吃下的食物数。 */
    public int foodWithTrait(String traitId) {
        return foodWithTraits.getOrDefault(traitId, 0);
    }

    public Map<String, Integer> foodWithTraits() {
        return Collections.unmodifiableMap(foodWithTraits);
    }

    /** Casts that landed {@code catchName}. 钓到 {@code catchName} 的次数。 */
    public int fishCaught(String catchName) {
        return fishCaught.getOrDefault(catchName, 0);
    }

    /** Every catch ever landed, with its count. 钓到过的所有渔获及次数。 */
    public Map<String, Integer> fishCaught() {
        return Collections.unmodifiableMap(fishCaught);
    }

    /** Kills of players on another faction made as {@code roleId}. 以 {@code roleId} 身份击杀其他阵营玩家的次数。 */
    public int rivalKillsAs(String roleId) {
        return rivalKills.getOrDefault(roleId, 0);
    }

    public Map<String, Integer> rivalKills() {
        return Collections.unmodifiableMap(rivalKills);
    }

    /**
     * Every owner-visible trait held around a kill of a player on another faction made as {@code roleId}, sorted.
     * 以 {@code roleId} 身份击杀其他阵营玩家时持有过的所有自身可见词条，已排序。
     */
    public Set<String> rivalKillTraitsAs(String roleId) {
        return view(rivalKillTraits, roleId);
    }

    public Map<String, Set<String>> rivalKillTraits() {
        return views(rivalKillTraits);
    }

    /** Whether the player was ever turned into {@code roleId} mid-round. 玩家是否曾在对局中途变成 {@code roleId}。 */
    public boolean tookRole(String roleId) {
        return rolesTaken.containsKey(roleId);
    }

    /** The recorded causes of becoming {@code roleId} mid-round, sorted. 中途变成 {@code roleId} 的已记录原因，已排序。 */
    public Set<String> roleTakenCauses(String roleId) {
        return view(rolesTaken, roleId);
    }

    /** Every role taken mid-round, with its recorded causes. 中途变成过的所有身份及其已记录原因。 */
    public Map<String, Set<String>> rolesTaken() {
        return views(rolesTaken);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(ROUNDS, rounds);
        json.addProperty(WINS, wins);
        json.addProperty(DEATHS, deaths);
        json.addProperty(TASKS, tasks);
        json.add(ROLE_ROUNDS, counts(roleRounds));
        json.add(ROLE_WINS, counts(roleWins));
        json.add(FACTION_ROUNDS, counts(factionRounds));
        json.add(FACTION_WINS, counts(factionWins));
        json.add(CONSUMED, counts(consumed));
        json.add(KILLED_ROLES, roleSets(killedRoles));
        json.add(STREAK_KILLED_ROLES, roleSets(streakKilledRoles));
        json.add(SAVES, counts(saves));
        json.add(DEATH_REASONS, strings(deathReasons));
        json.addProperty(WIN_STREAK, winStreak);
        json.add(SURVIVED_WIN_STREAKS, counts(survivedWinStreaks));
        json.add(CONSUMED_SINCE_DEATH, counts(consumedSinceDeath));
        json.add(CONSUMED_ITEMS, counts(consumedItems));
        json.add(FOOD_WITH_TRAITS, counts(foodWithTraits));
        json.add(FISH_CAUGHT, counts(fishCaught));
        json.add(RIVAL_KILLS, counts(rivalKills));
        json.add(RIVAL_KILL_TRAITS, roleSets(rivalKillTraits));
        json.add(ROLES_TAKEN, roleSets(rolesTaken, true));
        json.add(HELD_ROLE_WINS, counts(heldRoleWins));
        laterFields.forEach((key, value) -> json.add(key, value.deepCopy()));
        return json;
    }

    /**
     * Lenient: a missing or malformed field reads as zero or empty, so saves from before a field existed load fine.
     * 宽松读取：缺失或格式错误的字段按 0 或空处理，因此加入新字段之前的存档也能正常读取。
     */
    public static LifetimeStats fromJson(JsonObject json) {
        LifetimeStats stats = new LifetimeStats();
        if (json == null) {
            return stats;
        }
        stats.rounds = count(json.get(ROUNDS));
        stats.wins = count(json.get(WINS));
        stats.deaths = count(json.get(DEATHS));
        stats.tasks = count(json.get(TASKS));
        readCounts(json.get(ROLE_ROUNDS), stats.roleRounds);
        readCounts(json.get(ROLE_WINS), stats.roleWins);
        readCounts(json.get(FACTION_ROUNDS), stats.factionRounds);
        readCounts(json.get(FACTION_WINS), stats.factionWins);
        readCounts(json.get(CONSUMED), stats.consumed);
        readRoleSets(json.get(KILLED_ROLES), stats.killedRoles);
        readRoleSets(json.get(STREAK_KILLED_ROLES), stats.streakKilledRoles);
        readCounts(json.get(SAVES), stats.saves);
        readStrings(json.get(DEATH_REASONS), stats.deathReasons);
        stats.winStreak = count(json.get(WIN_STREAK));
        readCounts(json.get(SURVIVED_WIN_STREAKS), stats.survivedWinStreaks);
        readCounts(json.get(CONSUMED_SINCE_DEATH), stats.consumedSinceDeath);
        readCounts(json.get(CONSUMED_ITEMS), stats.consumedItems);
        readCounts(json.get(FOOD_WITH_TRAITS), stats.foodWithTraits);
        readCounts(json.get(FISH_CAUGHT), stats.fishCaught);
        readCounts(json.get(RIVAL_KILLS), stats.rivalKills);
        readRoleSets(json.get(RIVAL_KILL_TRAITS), stats.rivalKillTraits);
        readRoleSets(json.get(ROLES_TAKEN), stats.rolesTaken, true);
        if (json.has(HELD_ROLE_WINS)) {
            readCounts(json.get(HELD_ROLE_WINS), stats.heldRoleWins);
        } else {
            // A save from before this count: start from the wins by final role. 这项统计之前的存档：以按最终身份的胜场为起点。
            stats.heldRoleWins.putAll(stats.roleWins);
        }
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!KNOWN_FIELDS.contains(entry.getKey())) {
                stats.laterFields.put(entry.getKey(), entry.getValue().deepCopy());
            }
        }
        return stats;
    }

    private static void addRoles(Map<String, Set<String>> sets, String group, Collection<String> roleIds) {
        if (group == null || group.isBlank() || roleIds == null) {
            return;
        }
        for (String roleId : roleIds) {
            if (roleId != null && !roleId.isBlank()) {
                sets.computeIfAbsent(group, key -> new TreeSet<>()).add(roleId);
            }
        }
    }

    private static Set<String> view(Map<String, Set<String>> sets, String group) {
        Set<String> set = sets.get(group);
        return set == null ? Set.of() : Collections.unmodifiableSet(set);
    }

    private static Map<String, Set<String>> views(Map<String, Set<String>> sets) {
        Map<String, Set<String>> views = new TreeMap<>();
        sets.forEach((key, set) -> views.put(key, Collections.unmodifiableSet(set)));
        return Collections.unmodifiableMap(views);
    }

    private static void addCount(Map<String, Integer> counts, String key, int count) {
        if (key != null && !key.isBlank() && count > 0) {
            counts.merge(key, count, Integer::sum);
        }
    }

    private static JsonObject roleSets(Map<String, Set<String>> sets) {
        return roleSets(sets, false);
    }

    /** {@code keepEmpty}: write keys whose set is empty too. {@code keepEmpty}：集合为空的键也写入。 */
    private static JsonObject roleSets(Map<String, Set<String>> sets, boolean keepEmpty) {
        JsonObject json = new JsonObject();
        sets.forEach((group, roles) -> {
            if (keepEmpty || !roles.isEmpty()) {
                json.add(group, strings(roles));
            }
        });
        return json;
    }

    private static JsonArray strings(Set<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    private static void readRoleSets(JsonElement element, Map<String, Set<String>> into) {
        readRoleSets(element, into, false);
    }

    /** {@code keepEmpty}: keep keys whose array is empty. {@code keepEmpty}：数组为空的键也保留。 */
    private static void readRoleSets(JsonElement element, Map<String, Set<String>> into, boolean keepEmpty) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            Set<String> roles = new TreeSet<>();
            readStrings(entry.getValue(), roles);
            if (keepEmpty || !roles.isEmpty()) {
                into.computeIfAbsent(entry.getKey(), key -> new TreeSet<>()).addAll(roles);
            }
        }
    }

    private static void readStrings(JsonElement element, Set<String> into) {
        if (element == null || !element.isJsonArray()) {
            return;
        }
        for (JsonElement value : element.getAsJsonArray()) {
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() && !value.getAsString().isBlank()) {
                into.add(value.getAsString());
            }
        }
    }

    private static JsonObject counts(Map<String, Integer> map) {
        JsonObject json = new JsonObject();
        map.forEach(json::addProperty);
        return json;
    }

    private static void readCounts(JsonElement element, Map<String, Integer> into) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            int value = count(entry.getValue());
            if (value > 0) {
                into.put(entry.getKey(), value);
            }
        }
    }

    private static int count(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return 0;
        }
        return Math.max(0, element.getAsInt());
    }
}
