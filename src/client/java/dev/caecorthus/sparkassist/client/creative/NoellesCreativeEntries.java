package dev.caecorthus.sparkassist.client.creative;

import java.util.function.Predicate;
import dev.caecorthus.sparkassist.client.config.SparkAssistClientSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Surfaces NoellesRoles' items in the creative inventory without compiling against it. NoellesRoles registers no item
 * group of its own, so its items are unreachable from the creative tabs even though they exist in the registry.
 * 在不编译期依赖 NoellesRoles 的前提下把它的物品放进创造背包。NoellesRoles 自己没有注册物品组，因此它的物品虽然存在于
 * 注册表，却无法从创造模式标签页中取到。
 *
 * <p>Each item is filed by kind rather than dumped into one tab: keys and tools into Tools, weapons into Combat, and
 * everything else — drinks, vials, reagents — into Ingredients. Matching is on the registry id, and every step is
 * guarded on the mod being present, so a pack without NoellesRoles simply sees no change.
 * 每件物品按种类归档，而不是全部堆进一个标签页：钥匙与工具进「工具」，武器进「战斗」，其余——酒、药剂、试剂——进「材料」。
 * 匹配依据是注册表 id，且每一步都以该模组存在为前提，因此没有装 NoellesRoles 的整合包不会看到任何变化。</p>
 */
public final class NoellesCreativeEntries {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Creative");
    private static final String MOD_ID = "noellesroles";

    private NoellesCreativeEntries() {
    }

    public static void register() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID) || !SparkAssistClientSettings.noellesCreativeEntries()) {
            return;
        }
        long total = file(ItemGroups.TOOLS, NoellesCreativeEntries::isTool)
                + file(ItemGroups.COMBAT, NoellesCreativeEntries::isWeapon)
                + file(ItemGroups.INGREDIENTS, NoellesCreativeEntries::isEverythingElse);
        LOGGER.info("Filed {} NoellesRoles item(s) into the creative tabs", total);
    }

    /** Appends every mod item matching {@code kind} to {@code group} and returns how many were added. 把符合条件的物品追加到该组并返回数量。 */
    private static long file(RegistryKey<ItemGroup> group, Predicate<Identifier> kind) {
        // Count inside the callback so the log line reports what was actually filed, not what was scanned.
        // 在回调里计数，使日志反映实际归档的数量而不是扫描的数量。
        long[] filed = {0L};
        ItemGroupEvents.modifyEntriesEvent(group).register(entries -> {
            for (Item item : Registries.ITEM) {
                Identifier id = Registries.ITEM.getId(item);
                if (MOD_ID.equals(id.getNamespace()) && kind.test(id)) {
                    entries.add(new ItemStack(item));
                    filed[0]++;
                }
            }
        });
        return filed[0];
    }

    private static boolean isTool(Identifier id) {
        String path = id.getPath();
        return path.contains("key") || path.contains("lockpick") || path.contains("tool")
                || path.contains("crowbar") || path.contains("needle");
    }

    private static boolean isWeapon(Identifier id) {
        String path = id.getPath();
        return path.contains("pistol") || path.contains("knife") || path.contains("axe")
                || path.contains("bomb") || path.contains("gas") || path.contains("trap");
    }

    /** Everything left over belongs in Ingredients: drinks, vials, reagents, ingredients. 剩下的都进材料：酒、药剂、试剂、食材。 */
    private static boolean isEverythingElse(Identifier id) {
        return !isTool(id) && !isWeapon(id);
    }
}
