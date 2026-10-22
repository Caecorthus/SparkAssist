package dev.caecorthus.sparkassist.client.guidebook;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads only SparkTraits' public, owner-synced active-trait API when that mod is installed.
 * 仅在 SparkTraits 已安装时读取其公开且只同步给本人的生效天赋 API。
 */
public final class SparkTraitsGuideBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/GuidebookTraits");
    private static boolean resolved;
    private static boolean available;
    private static Method registryValues;
    private static Method traitId;
    private static Method hasActiveTrait;
    // Optional: the whole active set in one call. 可选：一次调用取得全部生效天赋。
    private static Method activeTraitIds;
    private static Method registryContains;

    private SparkTraitsGuideBridge() {
    }

    public static Set<String> ownerVisibleActiveTraitIds(PlayerEntity player) {
        resolve();
        if (!available || player == null) {
            return Set.of();
        }

        Set<String> activeIds = new LinkedHashSet<>();
        try {
            if (activeTraitIds != null && registryContains != null) {
                // The guide polls every tick: read the player's few active ids rather than ask about every trait;
                // ids this client does not know are still skipped.
                // 指南每 tick 轮询：直接读取玩家仅有的几个生效天赋，不再逐个询问全部天赋；本客户端不认识的 id 仍然跳过。
                for (Object id : (Collection<?>) activeTraitIds.invoke(null, player)) {
                    if ((boolean) registryContains.invoke(null, id)) {
                        activeIds.add(id.toString());
                    }
                }
            } else {
                Collection<?> traits = (Collection<?>) registryValues.invoke(null);
                for (Object trait : traits) {
                    Identifier id = (Identifier) traitId.invoke(trait);
                    if ((boolean) hasActiveTrait.invoke(null, player, id)) {
                        activeIds.add(id.toString());
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | ClassCastException exception) {
            LOGGER.warn("Could not query owner-visible SparkTraits state", exception);
            available = false;
        }
        return Set.copyOf(activeIds);
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!FabricLoader.getInstance().isModLoaded("sparktraits")) {
            return;
        }
        try {
            Class<?> registryClass = Class.forName("dev.caecorthus.sparktraits.api.TraitRegistry");
            Class<?> traitClass = Class.forName("dev.caecorthus.sparktraits.api.Trait");
            Class<?> apiClass = Class.forName("dev.caecorthus.sparktraits.api.SparkTraitsApi");
            registryValues = registryClass.getMethod("values");
            traitId = traitClass.getMethod("id");
            hasActiveTrait = apiClass.getMethod("hasActiveTrait", PlayerEntity.class, Identifier.class);
            available = true;
            try {
                activeTraitIds = apiClass.getMethod("getActiveTraitIds", PlayerEntity.class);
                registryContains = registryClass.getMethod("contains", Identifier.class);
            } catch (NoSuchMethodException older) {
                activeTraitIds = null;
                registryContains = null;
            }
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            LOGGER.warn("SparkTraits is installed but its public guide API is unavailable", exception);
        }
    }
}
