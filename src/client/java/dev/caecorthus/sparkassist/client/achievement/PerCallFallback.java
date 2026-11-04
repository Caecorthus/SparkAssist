package dev.caecorthus.sparkassist.client.achievement;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

/**
 * Runs one reflective call into an optional sibling mod and degrades per call: a failure answers null for that call
 * only and is logged once per kind of lookup, so one bad role or round never switches the lookup off for the rest of
 * the session.
 * 对可选兄弟模组执行一次反射调用，并按次降级：失败只让这一次返回 null，每类查询只记录一次日志；某个出错的身份或对局
 * 不会让该查询在本次会话余下时间里被关闭。
 */
final class PerCallFallback {
    private final Logger logger;
    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    PerCallFallback(Logger logger) {
        this.logger = logger;
    }

    /** A reflective call that may throw. 可能抛出异常的反射调用。 */
    @FunctionalInterface
    interface Call<T> {
        T get() throws ReflectiveOperationException;
    }

    /**
     * The call's answer, or null when it threw; the caller then uses its fallback for this call.
     * 调用的结果；调用抛出异常时为 null，调用方随后对这一次使用自己的回退值。
     */
    <T> T call(String what, Call<T> call) {
        try {
            return call.get();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (warned.add(what)) {
                logger.warn("SparkFactionAPI {} failed; using Wathe factions for this lookup", what, exception);
            }
            return null;
        }
    }
}
