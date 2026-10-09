package dev.caecorthus.sparkassist.achievement;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One event of Wathe's server-side match record, as SparkFactionAPI hands it to clients when a round ends. Values
 * are the event's NBT fields flattened to strings by {@link RecordFlattener}: nested fields use dotted keys
 * ({@code player.role}), UUIDs are in their usual text form, booleans are {@code "1"}/{@code "0"} and lists are
 * comma-joined. Servers without a given mod never send its fields, so every typed accessor takes a fallback.
 * Wathe 服务端对局记录中的一条事件，由 SparkFactionAPI 在回合结束时交给客户端。{@code values} 是该事件的 NBT 字段，
 * 由 {@link RecordFlattener} 统一转成字符串：嵌套字段用带点的键（{@code player.role}），UUID 为常见的文本形式，
 * 布尔值为 {@code "1"}/{@code "0"}，列表以逗号连接。没有对应模组的服务器不会发送其字段，所以每个类型化读取方法都带默认值。
 *
 * @param type   Wathe record type, e.g. {@code death}, {@code shop_purchase} Wathe 记录类型
 * @param tick   world ticks since the match started 对局开始后的世界 tick 数
 * @param actor  who did it (for a death: the killer), or null 执行者（死亡事件里是击杀者），没有则为 null
 * @param target who it was done to (for a death: the victim), or null 承受者（死亡事件里是死者），没有则为 null
 */
public record MatchEvent(String type, int tick, UUID actor, UUID target, Map<String, String> values) {
    public static final String DEATH = "death";
    public static final String SHOP_PURCHASE = "shop_purchase";
    public static final String TASK_COMPLETE = "task_complete";
    public static final String PLAYER_POISONED = "player_poisoned";
    public static final String ITEM_USE = "item_use";
    public static final String SKILL_USE = "skill_use";
    /** Wathe's opening role snapshot: {@code player.uuid}, {@code player.role}, … Wathe 开局身份快照。 */
    public static final String ROLE_ASSIGNED = "role_assigned";
    /** A role change: {@code player}, {@code from}, {@code to}, {@code cause}, {@code source}. 身份变化。 */
    public static final String ROLE_CHANGED = "sparkfactionapi:role_changed";
    /** Psycho mode started ({@code active} 1) or ended (0) for {@code actor}. 疯魔模式开始或结束。 */
    public static final String PSYCHO = "sparkfactionapi:psycho";
    /** {@code actor} finished eating or drinking {@code item}; {@code kind} is food or drink. 吃完或喝完一样东西。 */
    public static final String CONSUME = "sparkfactionapi:consume";

    public MatchEvent {
        values = values == null ? Map.of() : Map.copyOf(values);
    }

    public boolean is(String eventType) {
        return eventType.equals(type);
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    /** The raw text of a field, or null. 字段的原始文本，没有则为 null。 */
    public String value(String key) {
        return values.get(key);
    }

    /** The raw text of a field, or {@code fallback} when missing or blank. 字段的原始文本；缺失或为空时返回 {@code fallback}。 */
    public String value(String key, String fallback) {
        String value = values.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    /** A boolean field; false when missing. 布尔字段；缺失时为 false。 */
    public boolean bool(String key) {
        return bool(key, false);
    }

    /**
     * A boolean field: {@code "1"}/{@code "true"} and other non-zero numbers are true, {@code "0"}/{@code "false"}
     * false; anything else is {@code fallback}.
     * 布尔字段：{@code "1"}、{@code "true"} 及其他非零数字为真，{@code "0"}、{@code "false"} 为假；其他情况返回 {@code fallback}。
     */
    public boolean bool(String key, boolean fallback) {
        String value = values.get(key);
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.equalsIgnoreCase("true")) {
            return true;
        }
        if (trimmed.equalsIgnoreCase("false")) {
            return false;
        }
        try {
            return Double.parseDouble(trimmed) != 0;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    /** A whole-number field (a decimal is truncated), or {@code fallback}. 整数字段（小数会截断），否则返回 {@code fallback}。 */
    public int intValue(String key, int fallback) {
        String value = values.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException notAnInt) {
            try {
                double parsed = Double.parseDouble(value.trim());
                return Double.isFinite(parsed) ? (int) parsed : fallback;
            } catch (NumberFormatException notANumber) {
                return fallback;
            }
        }
    }

    /** A numeric field, or {@code fallback}. 数值字段，否则返回 {@code fallback}。 */
    public double doubleValue(String key, double fallback) {
        String value = values.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            double parsed = Double.parseDouble(value.trim());
            return Double.isNaN(parsed) ? fallback : parsed;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    /** A UUID field, or null when missing or malformed. UUID 字段；缺失或格式错误时为 null。 */
    public UUID uuid(String key) {
        String value = values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * A list field split back into its entries; empty when missing. Entries must not contain commas (ids, numbers and
     * UUIDs never do).
     * 拆回各项的列表字段；缺失时为空。各项本身不能含逗号（id、数字和 UUID 都不会有）。
     */
    public List<String> list(String key) {
        String value = values.get(key);
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(value.split(",", -1)).map(String::trim).toList();
    }
}
