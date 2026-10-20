package dev.caecorthus.sparkassist.achievement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * One account's local achievement save: what was unlocked, in unlock order and where, plus the {@link LifetimeStats}.
 * Each place (server or local world) earns an achievement once, so one achievement can have an unlock per place.
 * Ids the current catalog no longer knows are kept, so a downgrade or a removed achievement never loses data.
 * 单个账号的本地成就存档：按解锁顺序记录已解锁的成就及获得地点，以及 {@link LifetimeStats}。
 * 每个地点（服务器或本地世界）各能获得一次同一个成就，所以一个成就可以在每个地点各有一条解锁记录。
 * 当前目录里已不存在的 id 也会保留，降级或删掉某个成就都不会丢数据。
 */
public final class AchievementLedger {
    public static final int VERSION = 1;

    private static final String VERSION_KEY = "version";
    private static final String UNLOCKED = "unlocked";
    private static final String ID = "id";
    private static final String AT = "at";
    private static final String SERVER = "server";
    private static final String LOCAL = "local";
    private static final String STATS = "stats";

    // Insertion order is unlock order. 插入顺序即解锁顺序。
    private final Map<Key, Unlock> unlocked = new LinkedHashMap<>();
    private final Set<String> ids = new HashSet<>();
    private final LifetimeStats stats;

    private AchievementLedger(LifetimeStats stats) {
        this.stats = stats;
    }

    public static AchievementLedger empty() {
        return new AchievementLedger(new LifetimeStats());
    }

    /** Whether it was unlocked anywhere. 是否在任何地点解锁过。 */
    public boolean isUnlocked(String id) {
        return ids.contains(id);
    }

    /**
     * Records an unlock at {@code epochMillis} in {@code place} (null when unknown). A place keeps one unlock per
     * achievement: earning it there again (微小胜利 after it expired) moves that unlock to the new time, as the latest.
     * False when the id is blank or this place already has an unlock at least that recent.
     * 记录在 {@code epochMillis} 时于 {@code place} 解锁（未知时为 null）。每个地点同一成就只记一条：在那里再次获得
     * （过期后的微小胜利）会把那条记录移到新的时间，排在最后。id 为空，或该地点已有不早于此时间的记录时返回 false。
     */
    public boolean unlock(String id, long epochMillis, Place place) {
        if (id == null || id.isBlank()) {
            return false;
        }
        Key key = new Key(id, place);
        Unlock earlier = unlocked.get(key);
        if (earlier != null && earlier.epochMillis() >= epochMillis) {
            return false;
        }
        unlocked.remove(key);
        unlocked.put(key, new Unlock(id, epochMillis, place));
        ids.add(id);
        return true;
    }

    /** Unlocks in the order they happened. 按发生顺序排列的解锁记录。 */
    public List<Unlock> unlocks() {
        return List.copyOf(unlocked.values());
    }

    public LifetimeStats stats() {
        return stats;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(VERSION_KEY, VERSION);
        JsonArray list = new JsonArray();
        for (Unlock unlock : unlocked.values()) {
            JsonObject row = new JsonObject();
            row.addProperty(ID, unlock.id());
            row.addProperty(AT, unlock.epochMillis());
            Place place = unlock.place();
            if (place != null && place.isLocalWorld()) {
                row.addProperty(LOCAL, true);
            } else if (place != null) {
                row.addProperty(SERVER, place.server());
            }
            list.add(row);
        }
        json.add(UNLOCKED, list);
        json.add(STATS, stats.toJson());
        return json;
    }

    /**
     * Lenient: malformed unlock rows are skipped, a row without a place (saved before places were recorded) reads as
     * unknown, and missing stats read as zero.
     * 宽松读取：格式错误的解锁行会被跳过，没有地点的行（记录地点之前保存的）视为未知，缺失的统计按 0 处理。
     */
    public static AchievementLedger fromJson(JsonObject json) {
        JsonElement statsJson = json == null ? null : json.get(STATS);
        AchievementLedger ledger = new AchievementLedger(LifetimeStats.fromJson(
                statsJson != null && statsJson.isJsonObject() ? statsJson.getAsJsonObject() : null));
        if (json == null || !json.has(UNLOCKED) || !json.get(UNLOCKED).isJsonArray()) {
            return ledger;
        }
        for (JsonElement element : json.getAsJsonArray(UNLOCKED)) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject unlock = element.getAsJsonObject();
            JsonElement id = unlock.get(ID);
            JsonElement at = unlock.get(AT);
            if (id == null || !id.isJsonPrimitive() || !id.getAsJsonPrimitive().isString()) {
                continue;
            }
            long epochMillis = at != null && at.isJsonPrimitive() && at.getAsJsonPrimitive().isNumber()
                    ? at.getAsLong()
                    : 0L;
            ledger.unlock(id.getAsString(), epochMillis, placeOf(unlock));
        }
        return ledger;
    }

    private static Place placeOf(JsonObject row) {
        JsonElement local = row.get(LOCAL);
        if (local != null && local.isJsonPrimitive() && local.getAsJsonPrimitive().isBoolean() && local.getAsBoolean()) {
            return Place.LOCAL_WORLD;
        }
        JsonElement server = row.get(SERVER);
        if (server != null && server.isJsonPrimitive() && server.getAsJsonPrimitive().isString()
                && !server.getAsString().isBlank()) {
            return new Place(server.getAsString());
        }
        return null;
    }

    /** {@code place} is null when unknown. {@code place} 未知时为 null。 */
    public record Unlock(String id, long epochMillis, Place place) {
    }

    private record Key(String id, Place place) {
    }

    /**
     * Where an unlock happened: {@code server} is the server address as this client joined it, or null for
     * {@link #LOCAL_WORLD}, a world this client hosts itself. Addresses are kept in lower case without the default
     * port, so the same server typed two ways is one place.
     * 解锁发生的地点：{@code server} 是本客户端加入时使用的服务器地址；为 null 表示 {@link #LOCAL_WORLD}，即本客户端自己运行的世界。
     * 地址统一为小写并去掉默认端口，同一台服务器换种写法仍是同一个地点。
     */
    public record Place(String server) {
        public static final Place LOCAL_WORLD = new Place(null);
        private static final String DEFAULT_PORT = ":25565";

        public Place {
            if (server != null) {
                server = server.trim().toLowerCase(Locale.ROOT);
                if (server.endsWith(DEFAULT_PORT) && server.length() > DEFAULT_PORT.length()) {
                    server = server.substring(0, server.length() - DEFAULT_PORT.length());
                }
                if (server.isEmpty()) {
                    throw new IllegalArgumentException("blank server address");
                }
            }
        }

        public boolean isLocalWorld() {
            return server == null;
        }
    }
}
