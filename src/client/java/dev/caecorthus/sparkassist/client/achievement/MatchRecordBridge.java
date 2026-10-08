package dev.caecorthus.sparkassist.client.achievement;

import dev.caecorthus.sparkassist.achievement.MatchEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the server's match record that SparkFactionAPI hands to clients when a round ends
 * ({@code SparkMatchRecordClient.latest()}). Older SparkFactionAPI versions and servers without it simply give no
 * record, and achievements that need one cannot be earned that round.
 * 读取 SparkFactionAPI 在回合结束时交给客户端的服务端对局记录（{@code SparkMatchRecordClient.latest()}）。
 * 旧版 SparkFactionAPI 或没有它的服务器不会提供记录，需要记录的成就在那一局里无法达成。
 */
final class MatchRecordBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Achievements");
    private static boolean resolved;
    private static boolean available;
    private static Method latest;
    private static Method events;
    private static Method eventType;
    private static Method eventTick;
    private static Method eventData;

    private MatchRecordBridge() {
    }

    /**
     * The latest record the client holds, as an opaque token; compare identities to tell whether a new one arrived.
     * 客户端持有的最新记录，作为不透明的标记使用；比较引用即可判断是否收到了新记录。
     */
    static Object latest() {
        resolve();
        if (!available) {
            return null;
        }
        try {
            return latest.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            disable(exception);
            return null;
        }
    }

    static List<MatchEvent> events(Object record) {
        if (record == null || !available) {
            return List.of();
        }
        try {
            List<MatchEvent> list = new ArrayList<>();
            for (Object event : (List<?>) events.invoke(record)) {
                NbtCompound data = (NbtCompound) eventData.invoke(event);
                list.add(new MatchEvent(
                        (String) eventType.invoke(event),
                        (int) eventTick.invoke(event),
                        uuid(data, "actor"),
                        uuid(data, "target"),
                        values(data)));
            }
            return List.copyOf(list);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            disable(exception);
            return List.of();
        }
    }

    private static UUID uuid(NbtCompound data, String key) {
        return data.containsUuid(key) ? data.getUuid(key) : null;
    }

    /** Flattens primitive fields to strings; UUID int arrays become UUID text. 把基础字段转成字符串；UUID 整数数组转成 UUID 文本。 */
    private static Map<String, String> values(NbtCompound data) {
        Map<String, String> values = new HashMap<>();
        for (String key : data.getKeys()) {
            NbtElement element = data.get(key);
            if (element instanceof NbtString || element instanceof AbstractNbtNumber) {
                values.put(key, element.asString());
            } else if (element instanceof NbtIntArray array && array.size() == 4) {
                values.put(key, NbtHelper.toUuid(array).toString());
            }
        }
        return values;
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!FabricLoader.getInstance().isModLoaded("sparkfactionapi")) {
            return;
        }
        try {
            Class<?> client = Class.forName("dev.caecorthus.sparkfactionapi.client.api.SparkMatchRecordClient");
            latest = client.getMethod("latest");
            Class<?> snapshot = latest.getReturnType();
            events = snapshot.getMethod("events");
            Class<?> event = Class.forName(snapshot.getName() + "$Event");
            eventType = event.getMethod("type");
            eventTick = event.getMethod("tick");
            eventData = event.getMethod("data");
            available = true;
        } catch (ReflectiveOperationException exception) {
            // An older SparkFactionAPI without the match record: no record, no warning spam.
            // 不带对局记录的旧版 SparkFactionAPI：没有记录，也不刷警告。
            LOGGER.info("SparkFactionAPI has no match record API; record-based achievements are unavailable");
        }
    }

    private static void disable(Exception exception) {
        LOGGER.warn("Reading the SparkFactionAPI match record failed; ignoring it from now on", exception);
        available = false;
    }
}
