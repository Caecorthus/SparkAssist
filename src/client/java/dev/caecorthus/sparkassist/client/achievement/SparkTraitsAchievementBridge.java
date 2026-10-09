package dev.caecorthus.sparkassist.client.achievement;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads SparkTraits state that its public API does not expose but that the server syncs to the player it belongs
 * to: who sent the local player into Depression psycho ({@code TraitPlayerComponent#getDepressionPsychoAttacker},
 * synced to its owner only). Without SparkTraits, or if that component changes, it simply reports nothing.
 * 读取 SparkTraits 公开 API 未提供、但服务端只同步给本人的状态：是谁让本地玩家进入了抑郁疯魔
 * （{@code TraitPlayerComponent#getDepressionPsychoAttacker}，仅同步给本人）。没有 SparkTraits 或该组件有变时，不报告任何内容。
 */
final class SparkTraitsAchievementBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Achievements");
    private static boolean resolved;
    private static boolean available;
    private static ComponentKey<?> traitsKey;
    private static Method depressionPsychoAttacker;

    private SparkTraitsAchievementBridge() {
    }

    /**
     * The player who sent {@code player} into Depression psycho, or null when they are not in it. Only meaningful for
     * the local player: the server never sends this to anyone else.
     * 让 {@code player} 进入抑郁疯魔的玩家；不在抑郁疯魔中时为 null。只对本地玩家有意义：服务端不会把它发给其他人。
     */
    static UUID depressionPsychoAttacker(PlayerEntity player) {
        resolve();
        if (!available || player == null) {
            return null;
        }
        try {
            Object component = traitsKey.getNullable(player);
            Object attacker = component == null ? null : depressionPsychoAttacker.invoke(component);
            return attacker instanceof UUID uuid ? uuid : null;
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            LOGGER.warn("Reading SparkTraits Depression state failed; ignoring it from now on", exception);
            available = false;
            return null;
        }
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
            Class<?> component = Class.forName("dev.caecorthus.sparktraits.component.TraitPlayerComponent");
            Field key = component.getField("KEY");
            traitsKey = (ComponentKey<?>) key.get(null);
            depressionPsychoAttacker = component.getMethod("getDepressionPsychoAttacker");
            available = true;
        } catch (ReflectiveOperationException | ClassCastException exception) {
            LOGGER.info("SparkTraits has no readable Depression state; Depression achievements are unavailable");
        }
    }
}
