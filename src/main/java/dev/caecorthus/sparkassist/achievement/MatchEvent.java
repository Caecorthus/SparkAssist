package dev.caecorthus.sparkassist.achievement;

import java.util.Map;
import java.util.UUID;

/**
 * One event of Wathe's server-side match record, as SparkFactionAPI hands it to clients when a round ends. Values
 * are the event's NBT fields flattened to strings (UUIDs in their usual text form).
 * Wathe 服务端对局记录中的一条事件，由 SparkFactionAPI 在回合结束时交给客户端。
 * {@code values} 是该事件的 NBT 字段，统一转成字符串（UUID 为常见的文本形式）。
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

    public MatchEvent {
        values = values == null ? Map.of() : Map.copyOf(values);
    }

    public String value(String key) {
        return values.get(key);
    }

    public boolean is(String eventType) {
        return eventType.equals(type);
    }
}
