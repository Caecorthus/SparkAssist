package dev.caecorthus.sparkassist.achievement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.AbstractNbtList;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtString;

/**
 * Turns one match-record event's NBT into the flat string map {@link MatchEvent#values()} holds:
 * <ul>
 *     <li>nested compounds become dotted keys ({@code player.uuid}, {@code player.role}, {@code pos.x});</li>
 *     <li>a four-int array is a UUID and becomes UUID text, nested or not;</li>
 *     <li>numbers become plain decimal text ({@code putBool} arrives as {@code "1"}/{@code "0"});</li>
 *     <li>a list (or array) of strings, numbers or UUIDs becomes one comma-joined string; any other list is
 *     flattened entry by entry under {@code key.0}, {@code key.1}, …</li>
 * </ul>
 * 把一条对局记录事件的 NBT 转成 {@link MatchEvent#values()} 使用的扁平字符串表：
 * 嵌套复合标签展开为带点的键（{@code player.uuid}、{@code player.role}、{@code pos.x}）；
 * 四个整数的数组视为 UUID，无论是否嵌套都转成 UUID 文本；数字转成普通十进制文本（{@code putBool} 得到 {@code "1"}/{@code "0"}）；
 * 由字符串、数字或 UUID 组成的列表（或数组）合并为一个逗号分隔的字符串，其他列表按 {@code key.0}、{@code key.1}…逐项展开。
 */
public final class RecordFlattener {
    private RecordFlattener() {
    }

    public static Map<String, String> flatten(NbtCompound data) {
        Map<String, String> values = new LinkedHashMap<>();
        if (data != null) {
            putCompound(values, "", data);
        }
        return values;
    }

    private static void putCompound(Map<String, String> values, String prefix, NbtCompound compound) {
        for (String key : compound.getKeys()) {
            put(values, prefix + key, compound.get(key));
        }
    }

    private static void put(Map<String, String> values, String key, NbtElement element) {
        if (element instanceof NbtCompound compound) {
            putCompound(values, key + ".", compound);
            return;
        }
        String scalar = scalar(element);
        if (scalar != null) {
            values.put(key, scalar);
        } else if (element instanceof AbstractNbtList<?> list) {
            putList(values, key, list);
        }
    }

    private static void putList(Map<String, String> values, String key, AbstractNbtList<?> list) {
        List<String> parts = new ArrayList<>(list.size());
        for (NbtElement entry : list) {
            String scalar = scalar(entry);
            if (scalar == null) {
                for (int i = 0; i < list.size(); i++) {
                    put(values, key + "." + i, list.get(i));
                }
                return;
            }
            parts.add(scalar);
        }
        values.put(key, String.join(",", parts));
    }

    /** Text for a single value, or null for a compound or a general list. 单个值的文本；复合标签或普通列表返回 null。 */
    private static String scalar(NbtElement element) {
        if (element instanceof NbtString string) {
            return string.asString();
        }
        if (element instanceof AbstractNbtNumber number) {
            return number(number.numberValue());
        }
        if (element instanceof NbtIntArray array && array.size() == 4) {
            return NbtHelper.toUuid(array).toString();
        }
        return null;
    }

    private static String number(Number number) {
        if (number instanceof Float value) {
            return Float.toString(value);
        }
        if (number instanceof Double value) {
            return Double.toString(value);
        }
        return Long.toString(number.longValue());
    }
}
