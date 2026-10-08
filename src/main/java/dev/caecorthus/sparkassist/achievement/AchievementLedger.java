package dev.caecorthus.sparkassist.achievement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One account's local achievement save: what was unlocked, in unlock order, plus the {@link LifetimeStats}.
 * Ids the current catalog no longer knows are kept, so a downgrade or a removed achievement never loses data.
 * 单个账号的本地成就存档：按解锁顺序记录已解锁的成就，以及 {@link LifetimeStats}。
 * 当前目录里已不存在的 id 也会保留，降级或删掉某个成就都不会丢数据。
 */
public final class AchievementLedger {
    public static final int VERSION = 1;

    private static final String VERSION_KEY = "version";
    private static final String UNLOCKED = "unlocked";
    private static final String ID = "id";
    private static final String AT = "at";
    private static final String STATS = "stats";

    // Insertion order is unlock order. 插入顺序即解锁顺序。
    private final Map<String, Long> unlocked = new LinkedHashMap<>();
    private final LifetimeStats stats;

    private AchievementLedger(LifetimeStats stats) {
        this.stats = stats;
    }

    public static AchievementLedger empty() {
        return new AchievementLedger(new LifetimeStats());
    }

    public boolean isUnlocked(String id) {
        return unlocked.containsKey(id);
    }

    /**
     * Records an unlock at {@code epochMillis}; false when it was already unlocked.
     * 记录在 {@code epochMillis} 时解锁；已解锁过则返回 false。
     */
    public boolean unlock(String id, long epochMillis) {
        if (id == null || id.isBlank() || unlocked.containsKey(id)) {
            return false;
        }
        unlocked.put(id, epochMillis);
        return true;
    }

    /** Unlocks in the order they happened. 按发生顺序排列的解锁记录。 */
    public List<Unlock> unlocks() {
        List<Unlock> list = new ArrayList<>(unlocked.size());
        unlocked.forEach((id, at) -> list.add(new Unlock(id, at)));
        return List.copyOf(list);
    }

    public LifetimeStats stats() {
        return stats;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(VERSION_KEY, VERSION);
        JsonArray list = new JsonArray();
        unlocked.forEach((id, at) -> {
            JsonObject unlock = new JsonObject();
            unlock.addProperty(ID, id);
            unlock.addProperty(AT, at);
            list.add(unlock);
        });
        json.add(UNLOCKED, list);
        json.add(STATS, stats.toJson());
        return json;
    }

    /**
     * Lenient: malformed unlock rows are skipped and missing stats read as zero.
     * 宽松读取：格式错误的解锁行会被跳过，缺失的统计按 0 处理。
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
            ledger.unlock(id.getAsString(), epochMillis);
        }
        return ledger;
    }

    public record Unlock(String id, long epochMillis) {
    }
}
