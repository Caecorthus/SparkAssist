package dev.caecorthus.sparkassist.achievement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * General per-account round statistics kept next to the unlocked achievements. They describe how the player has
 * played, never how close they are to any achievement: no field is named after one, so reading the save file
 * reveals nothing about what is left to find.
 * 与已解锁成就一起保存的账号通用对局统计。只记录玩家怎么玩过，不记录离某个成就还差多少：
 * 没有任何字段以成就命名，所以翻看存档也看不出还剩什么没发现。
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

    private int rounds;
    private int wins;
    private int deaths;
    private int tasks;
    // Sorted so the save file diffs stay stable. 排序保存，存档内容稳定。
    private final Map<String, Integer> roleRounds = new TreeMap<>();
    private final Map<String, Integer> roleWins = new TreeMap<>();
    private final Map<String, Integer> factionRounds = new TreeMap<>();
    private final Map<String, Integer> factionWins = new TreeMap<>();

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
        return json;
    }

    /** Lenient: a missing or malformed field reads as zero. 宽松读取：缺失或格式错误的字段按 0 处理。 */
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
        return stats;
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
