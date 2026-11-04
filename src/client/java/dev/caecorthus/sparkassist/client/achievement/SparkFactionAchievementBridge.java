package dev.caecorthus.sparkassist.client.achievement;

import dev.caecorthus.sparkassist.achievement.Factions;
import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.Identifier;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads factions from SparkFactionAPI's public API when it is installed, so witch and other custom factions are
 * named correctly; without it, falls back to Wathe's own faction of the role, named by {@link Factions#fromWatheName}
 * ({@code wathe:civilian}, and {@code sparkwitch:witch} / {@code sparkwitch:jiahao} for the native custom constants
 * SparkFactionAPI appends to Wathe's enum). A failing call falls back for that call only and never switches the API
 * off for the session.
 * 安装了 SparkFactionAPI 时通过其公开 API 读取阵营，使魔女等自定义阵营得到正确的 id；未安装时回退到 Wathe 给该身份的
 * 阵营，由 {@link Factions#fromWatheName} 命名（{@code wathe:civilian} 等；SparkFactionAPI 追加到 Wathe 枚举的原生自定义
 * 常量为 {@code sparkwitch:witch} / {@code sparkwitch:jiahao}）。单次调用失败只让这一次回退，不会在本次会话中关闭该 API。
 */
final class SparkFactionAchievementBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Achievements");
    private static final PerCallFallback CALLS = new PerCallFallback(LOGGER);
    private static boolean resolved;
    private static boolean available;
    private static Method resolveEffectiveFaction;
    // Optional: older SparkFactionAPI builds may lack it. 可选：旧版 SparkFactionAPI 可能没有。
    private static Method resolveBaseFaction;
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
            String id = CALLS.call("faction lookup",
                    () -> idOrNull(resolveEffectiveFaction.invoke(null, player, game)));
            if (id != null) {
                return id;
            }
        }
        return watheFaction(role);
    }

    /**
     * The base faction of a role id (no player context, so no trait flips), or null when this client does not know
     * the role. SparkFactionAPI names custom factions such as {@code sparkwitch:witch}; without it, Wathe's base
     * factions are used.
     * 身份 id 的基础阵营（没有玩家上下文，因此不含词条翻转）；本客户端不认识该身份时为 null。
     * SparkFactionAPI 能给出 {@code sparkwitch:witch} 等自定义阵营；没有它时使用 Wathe 的基础阵营。
     */
    static String baseFaction(String roleId) {
        Identifier id = roleId == null ? null : Identifier.tryParse(roleId);
        Role role = id == null ? null : WatheRoles.getRole(id);
        if (role == null) {
            return null;
        }
        resolve();
        if (available && resolveBaseFaction != null) {
            String factionId = CALLS.call("base faction lookup", () -> idOrNull(resolveBaseFaction.invoke(null, role)));
            if (factionId != null) {
                return factionId;
            }
        }
        return watheFaction(role);
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
        return CALLS.call("round-end lookup", () -> {
            Object component = roundEndKey.getNullable(scoreboard);
            return component == null ? null : idOrNull(winningFaction.invoke(component));
        });
    }

    /**
     * Wathe's own faction of {@code role}: built-ins as {@code wathe:*}, SparkFactionAPI's appended native constants
     * by their declared ids (see {@link Factions#fromWatheName}).
     * Wathe 给 {@code role} 的阵营：内置阵营为 {@code wathe:*}，SparkFactionAPI 追加的原生常量取其声明的 id。
     */
    private static String watheFaction(Role role) {
        return Factions.fromWatheName(Faction.fromRole(role).name());
    }

    private static String idOrNull(Object faction) {
        return faction instanceof Identifier id ? id.toString() : null;
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
            try {
                resolveBaseFaction = api.getMethod("resolveBaseFaction", Role.class);
            } catch (NoSuchMethodException missing) {
                resolveBaseFaction = null;
            }
            Class<?> roundEnd = Class.forName("dev.caecorthus.sparkfactionapi.component.SparkFactionRoundEndComponent");
            Field key = roundEnd.getField("KEY");
            roundEndKey = (ComponentKey<?>) key.get(null);
            winningFaction = roundEnd.getMethod("getWinningFaction");
            available = true;
        } catch (ReflectiveOperationException | ClassCastException exception) {
            LOGGER.warn("SparkFactionAPI is installed but its faction API is unavailable", exception);
        }
    }
}
