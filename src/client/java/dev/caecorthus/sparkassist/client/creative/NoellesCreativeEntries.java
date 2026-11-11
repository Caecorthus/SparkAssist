package dev.caecorthus.sparkassist.client.creative;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
 * everything else — drinks, vials, reagents — into Ingredients. The kind is read from the whole words of the registry
 * id, never from a substring: {@code whiskey} contains "key" and is still a drink. Shop icons and leftovers that were
 * never meant to be held stay out altogether.
 * 每件物品按种类归档，而不是全部堆进一个标签页：钥匙与工具进「工具」，武器进「战斗」，其余——酒、药剂、试剂——进「材料」。
 * 种类取自注册表 id 中的完整单词，而不是子串：{@code whiskey} 里含有 "key"，但它仍然是酒。商店图标与从未打算让人拿在手里的
 * 遗留物品则完全不收录。</p>
 *
 * <p>The setting is read each time a tab is built, and Minecraft rebuilds the tabs when a world is joined, so a change
 * applies from the next world rather than needing a restart.
 * 设置在每次构建标签页时读取，而 Minecraft 在进入世界时重建标签页，因此改动从下一次进入世界起生效，无需重启。</p>
 */
public final class NoellesCreativeEntries {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Creative");
    private static final String MOD_ID = "noellesroles";
    private static final Set<String> TOOL_WORDS = Set.of("key", "lockpick", "tool", "crowbar");
    private static final Set<String> WEAPON_WORDS = Set.of("pistol", "knife", "axe", "bomb", "needle", "trap");
    /**
     * Registered only so a shop row has an icon ({@code scavenger_reset_knife_cd}, {@code timekeeper_reduce_time}), or
     * left behind with no name at all ({@code role_mine}).
     * 仅为给商店条目提供图标而注册（{@code scavenger_reset_knife_cd}、{@code timekeeper_reduce_time}），或是连名字都没有的
     * 遗留物品（{@code role_mine}）。
     */
    private static final Set<String> NOT_FOR_PLAYERS =
            Set.of("scavenger_reset_knife_cd", "timekeeper_reduce_time", "role_mine");

    private NoellesCreativeEntries() {
    }

    public static void register() {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }
        file(ItemGroups.TOOLS, Kind.TOOL);
        file(ItemGroups.COMBAT, Kind.WEAPON);
        file(ItemGroups.INGREDIENTS, Kind.OTHER);
        // NoellesRoles registers its items in its main initializer, which runs before any client initializer, so the
        // registry is complete here. The tabs themselves are filled later, each time Minecraft builds them.
        // NoellesRoles 在主初始化器中注册物品，早于任何客户端初始化器，因此此处注册表已完整。标签页本身要等 Minecraft
        // 每次构建时才填充。
        long available = Registries.ITEM.getIds().stream().filter(id -> kindOf(id) != null).count();
        LOGGER.info("{} NoellesRoles item(s) available to the creative tabs", available);
    }

    /** Appends every mod item of {@code kind} to {@code group} whenever that tab is built. 每次构建该标签页时追加对应种类的物品。 */
    private static void file(RegistryKey<ItemGroup> group, Kind kind) {
        ItemGroupEvents.modifyEntriesEvent(group).register(entries -> {
            if (!SparkAssistClientSettings.noellesCreativeEntries()) {
                return;
            }
            for (Item item : Registries.ITEM) {
                if (kindOf(Registries.ITEM.getId(item)) == kind) {
                    entries.add(new ItemStack(item));
                }
            }
        });
    }

    /** The one tab this id belongs to, or {@code null} when it is not a NoellesRoles item a player should take. 该 id 所属的唯一标签页；不是玩家该拿的 NoellesRoles 物品时返回 {@code null}。 */
    static Kind kindOf(Identifier id) {
        if (!MOD_ID.equals(id.getNamespace()) || NOT_FOR_PLAYERS.contains(id.getPath())) {
            return null;
        }
        List<String> words = Arrays.asList(id.getPath().split("_"));
        // Weapons first: a poison needle is a weapon, whatever else its name suggests.
        // 先判武器：毒针是武器，不管名字还让人联想到什么。
        if (words.stream().anyMatch(WEAPON_WORDS::contains)) {
            return Kind.WEAPON;
        }
        if (words.stream().anyMatch(TOOL_WORDS::contains)) {
            return Kind.TOOL;
        }
        return Kind.OTHER;
    }

    enum Kind {
        TOOL,
        WEAPON,
        /** Drinks, vials, reagents, ingredients. 酒、药剂、试剂、食材。 */
        OTHER
    }
}
