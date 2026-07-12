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
final class SparkTraitsGuideBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/GuidebookTraits");
    private static boolean resolved;
    private static boolean available;
    private static Method registryValues;
    private static Method traitId;
    private static Method hasActiveTrait;

    private SparkTraitsGuideBridge() {
    }

    static Set<String> ownerVisibleActiveTraitIds(PlayerEntity player) {
        resolve();
        if (!available || player == null) {
            return Set.of();
        }

        Set<String> activeIds = new LinkedHashSet<>();
        try {
            Collection<?> traits = (Collection<?>) registryValues.invoke(null);
            for (Object trait : traits) {
                Identifier id = (Identifier) traitId.invoke(trait);
                if ((boolean) hasActiveTrait.invoke(null, player, id)) {
                    activeIds.add(id.toString());
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
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            LOGGER.warn("SparkTraits is installed but its public guide API is unavailable", exception);
        }
    }
}
