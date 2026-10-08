package dev.caecorthus.sparkassist.client.achievement;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.Identifier;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads factions from SparkFactionAPI's public API when it is installed, so witch and other custom factions are
 * named correctly; without it, falls back to Wathe's four base factions ({@code wathe:civilian} and so on).
 * 安装了 SparkFactionAPI 时通过其公开 API 读取阵营，使魔女等自定义阵营得到正确的 id；
 * 未安装时回退到 Wathe 的四个基础阵营（{@code wathe:civilian} 等）。
 */
final class SparkFactionAchievementBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Achievements");
    private static boolean resolved;
    private static boolean available;
    private static Method resolveEffectiveFaction;
    private static ComponentKey<?> roundEndKey;
    private static Method winningFaction;

    private SparkFactionAchievementBridge() {
    }

    /** The player's current faction id, or null without a role. 玩家当前的阵营 id，没有角色时为 null。 */
    static String faction(PlayerEntity player, GameWorldComponent game, Role role) {
        if (role == null) {
            return null;
        }
        resolve();
        if (available) {
            try {
                Object faction = resolveEffectiveFaction.invoke(null, player, game);
                if (faction instanceof Identifier id) {
                    return id.toString();
                }
            } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
                disable("faction lookup", exception);
            }
        }
        return "wathe:" + Faction.fromRole(role).name().toLowerCase(Locale.ROOT);
    }

    /**
     * The custom winning faction SparkFactionAPI recorded for the last round, or null when the round ended with a
     * plain Wathe win (or the mod is absent).
     * SparkFactionAPI 记录的上一局自定义获胜阵营；普通 Wathe 胜利（或未安装该模组）时为 null。
     */
    static String customWinningFaction(Scoreboard scoreboard) {
        resolve();
        if (!available) {
            return null;
        }
        try {
            Object component = roundEndKey.getNullable(scoreboard);
            Object faction = component == null ? null : winningFaction.invoke(component);
            return faction instanceof Identifier id ? id.toString() : null;
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            disable("round-end lookup", exception);
            return null;
        }
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
            Class<?> api = Class.forName("dev.caecorthus.sparkfactionapi.api.SparkFactionApi");
            resolveEffectiveFaction = api.getMethod("resolveEffectiveFaction", PlayerEntity.class, GameWorldComponent.class);
            Class<?> roundEnd = Class.forName("dev.caecorthus.sparkfactionapi.component.SparkFactionRoundEndComponent");
            Field key = roundEnd.getField("KEY");
            roundEndKey = (ComponentKey<?>) key.get(null);
            winningFaction = roundEnd.getMethod("getWinningFaction");
            available = true;
        } catch (ReflectiveOperationException | ClassCastException exception) {
            LOGGER.warn("SparkFactionAPI is installed but its faction API is unavailable", exception);
        }
    }

    private static void disable(String what, Exception exception) {
        LOGGER.warn("SparkFactionAPI {} failed; using Wathe factions from now on", what, exception);
        available = false;
    }
}
