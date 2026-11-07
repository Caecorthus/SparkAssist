package dev.caecorthus.sparkassist.client.knife;

import java.lang.reflect.Method;
import net.minecraft.entity.player.PlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asks SparkTraits how long a throwable actually needs to charge. SparkStrength already does this server-side through
 * {@code SparkTraitsCompat.getThrowChargeTicks}, which reads only auto-synced components, so the same call is valid on
 * the client and keeps the bar in step with the shortened window.
 * 向 SparkTraits 询问投掷物实际需要蓄力多久。SparkStrength 在服务端已经通过
 * {@code SparkTraitsCompat.getThrowChargeTicks} 这么做了，而该调用只读取自动同步的组件，因此客户端同样可以调用，
 * 能让进度条与缩短后的窗口保持一致。
 *
 * <p>The multiplier is {@code HerculeanStrengthService.THROW_CHARGE_PERCENT = 70}, i.e. Herculean Strength turns a
 * 20-tick charge into 14.
 * 倍率是 {@code HerculeanStrengthService.THROW_CHARGE_PERCENT = 70}，也就是力气天赋把 20 tick 蓄力变成 14。</p>
 */
public final class SparkTraitsCharge {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Charge");
    private static final String API = "dev.caecorthus.sparktraits.api.SparkTraitsApi";

    private static boolean resolved;
    private static Method getThrowChargeTicks;

    private SparkTraitsCharge() {
    }

    /**
     * The trait-adjusted charge window, or {@code baseTicks} unchanged when SparkTraits is absent or silent.
     * 天赋调整后的蓄力窗口；SparkTraits 缺席或未响应时原样返回 {@code baseTicks}。
     */
    public static int adjusted(PlayerEntity player, int baseTicks) {
        if (player == null || !resolve()) {
            return baseTicks;
        }
        try {
            Object value = getThrowChargeTicks.invoke(null, player, baseTicks);
            if (value instanceof Integer ticks && ticks > 0) {
                return ticks;
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            // Once is enough; a broken bridge should not spam the log every tick.
            // 一次就够；桥接失效不该每 tick 刷日志。
            getThrowChargeTicks = null;
            LOGGER.warn("SparkTraits charge lookup failed; falling back to the untalented window", exception);
        }
        return baseTicks;
    }

    private static boolean resolve() {
        if (resolved) {
            return getThrowChargeTicks != null;
        }
        resolved = true;
        try {
            Class<?> api = Class.forName(API);
            getThrowChargeTicks = api.getMethod("getThrowChargeTicks", PlayerEntity.class, int.class);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // SparkTraits is optional; absence is not an error.
            // SparkTraits 是可选模组，缺席不算错误。
        }
        return getThrowChargeTicks != null;
    }
}
