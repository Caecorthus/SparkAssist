package dev.caecorthus.sparkassist.client.charge;

import java.lang.reflect.Method;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asks SparkTraits how the local player's traits change a charge, without compiling against it. Each call reads only
 * state SparkTraits syncs to the owning client, and is the same call the mod's own release checks go through, so the
 * indicator stays in step with what the server will accept.
 * 在不编译期依赖 SparkTraits 的前提下，询问本地玩家的词条如何改变蓄力。每个调用只读取 SparkTraits 同步给所属客户端的状态，
 * 且与该模组自身松手判定走的是同一个调用，因此指示与服务端将会接受的结果保持一致。
 *
 * <p>SparkTraits is optional and its API grows over time: a method that is missing — the mod absent, or a version from
 * before the method existed — simply leaves the untalented value in place.
 * SparkTraits 是可选模组，其 API 也在逐步增加：方法缺失时（模组不在，或版本早于该方法出现）直接沿用无词条的数值。</p>
 */
public final class SparkTraitsCharge {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Charge");
    private static final String API = "dev.caecorthus.sparktraits.api.SparkTraitsApi";

    private static final Call THROW_CHARGE = new Call("getThrowChargeTicks", PlayerEntity.class, int.class);
    private static final Call KNIFE_WINDUP =
            new Call("getKnifeWindupTicks", PlayerEntity.class, ItemStack.class, int.class);
    private static final Call FORCED_MELEE =
            new Call("getForcedMeleeCooldownTicks", PlayerEntity.class, ItemStack.class);

    private SparkTraitsCharge() {
    }

    /** Charge of a throwable after Herculean Strength, or {@code baseTicks}. 计入「力大无穷」后的投掷物蓄力；否则为 {@code baseTicks}。 */
    public static int throwCharge(PlayerEntity player, int baseTicks) {
        return positive(THROW_CHARGE.invoke(player, baseTicks), baseTicks);
    }

    /** Windup of Wathe's knife after Seasoned or Close Quarters, or {@code baseTicks}. 计入「老练」或「狭路相逢」后的小刀前摇；否则为 {@code baseTicks}。 */
    public static int knifeWindup(PlayerEntity player, ItemStack knife, int baseTicks) {
        return positive(KNIFE_WINDUP.invoke(player, knife, baseTicks), baseTicks);
    }

    /** Whether a forced-melee lock is discarding this weapon's releases right now. 强制近战锁定此刻是否正在作废该武器的松手。 */
    public static boolean meleeLocked(PlayerEntity player, ItemStack weapon) {
        return FORCED_MELEE.invoke(player, weapon) instanceof Integer ticks && ticks > 0;
    }

    private static int positive(Object value, int fallback) {
        return value instanceof Integer ticks && ticks > 0 ? ticks : fallback;
    }

    /** One static SparkTraitsApi method, looked up on first use and dropped for good if it ever fails. 一个 SparkTraitsApi 静态方法，首次使用时查找，一旦失败即永久停用。 */
    private static final class Call {
        private final String name;
        private final Class<?>[] parameters;
        private boolean resolved;
        private Method method;

        Call(String name, Class<?>... parameters) {
            this.name = name;
            this.parameters = parameters;
        }

        Object invoke(Object... arguments) {
            if (!resolved) {
                resolved = true;
                try {
                    method = Class.forName(API).getMethod(name, parameters);
                } catch (ReflectiveOperationException | LinkageError ignored) {
                    // Absent mod or older API: not an error. 模组缺席或 API 较旧：不算错误。
                }
            }
            if (method == null) {
                return null;
            }
            try {
                return method.invoke(null, arguments);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                // Once is enough; a broken bridge must not log every frame. 一次就够；桥接失效不该每帧刷日志。
                method = null;
                LOGGER.warn("SparkTraits {} failed; the charge indicator falls back to untalented timing", name,
                        exception);
                return null;
            }
        }
    }
}
