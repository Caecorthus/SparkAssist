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
 * 与已解锁成就一起保存的账号通用对局统计。只记录玩家怎么玩过，不记录离某个成就还差多少：
 * 没有任何字段以成就命名，所以翻看存档也看不出还剩什么没发现。较新版本 SparkAssist 写入的字段会原样保留并写回，降级不会丢失。
 * 击杀身份集合按击杀分组保存（见 {@link RoleGroups}）：{@link #killedRoles} 只增不减，{@link #streakKilledRoles}
 * 会被 {@link #resetStreak} 清空，例如玩家在该分组中死亡时。
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
    private static final Set<String> KNOWN_FIELDS = Set.of(ROUNDS, WINS, DEATHS, TASKS, ROLE_ROUNDS, ROLE_WINS,
            FACTION_ROUNDS, FACTION_WINS, CONSUMED, KILLED_ROLES, STREAK_KILLED_ROLES);

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
    private final Map<String, Set<String>> killedRoles = new TreeMap<>();
    private final Map<String, Set<String>> streakKilledRoles = new TreeMap<>();
    // Written by a newer SparkAssist; kept as is. 较新版本写入的字段，原样保留。
    private final Map<String, JsonElement> laterFields = new TreeMap<>();

    /**
     * Adds one settled round. Every distinct role the player held counts one round; a win counts for the last role
     * only, since that is the role that won. A null faction is counted in the totals only.
     * 记入一局已结算的对局。本局担任过的每个不同角色各记一局；胜场只记在最后一个角色上，因为获胜的是它。
     * 阵营为 null 时只计入总数。
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

    /** Every role killed while in {@code group}, sorted. 在 {@code group} 分组中击杀过的全部身份，已排序。 */
    public Set<String> killedRoles(String group) {
        return view(killedRoles, group);
    }

    /** Roles killed while in {@code group} since its last reset, sorted. 该分组上次清空以来击杀过的身份，已排序。 */
    public Set<String> streakKilledRoles(String group) {
        return view(streakKilledRoles, group);
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

    private static JsonObject roleSets(Map<String, Set<String>> sets) {
        JsonObject json = new JsonObject();
        sets.forEach((group, roles) -> {
            if (!roles.isEmpty()) {
                JsonArray array = new JsonArray();
                roles.forEach(array::add);
                json.add(group, array);
            }
        });
        return json;
    }

    private static void readRoleSets(JsonElement element, Map<String, Set<String>> into) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            for (JsonElement role : entry.getValue().getAsJsonArray()) {
                if (role.isJsonPrimitive() && role.getAsJsonPrimitive().isString() && !role.getAsString().isBlank()) {
                    into.computeIfAbsent(entry.getKey(), key -> new TreeSet<>()).add(role.getAsString());
                }
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
