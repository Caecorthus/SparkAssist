package dev.caecorthus.sparkassist.client.settings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads and writes SparkWitch's scope settings through its public client API, {@code ScopeSettingsApi}, when SparkWitch
 * is installed. Any lookup or call failure turns the bridge off for the session, and the scope page disappears.
 * 在已安装 SparkWitch 时，经由其公开客户端 API {@code ScopeSettingsApi} 读写瞄准镜设置。任何查找或调用失败都会让桥接在
 * 本次运行中关闭，瞄准镜页随之隐藏。
 */
final class SparkWitchScopeBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Settings");
    private static final String API_CLASS = "dev.caecorthus.sparkwitch.api.client.ScopeSettingsApi";

    static final String PICTURE_IN_PICTURE = "picture_in_picture";

    // Looked up once in resolve(); the settings screen reads these every frame.
    // 在 resolve() 中只查找一次；设置界面每帧都会读取。
    private static final Map<String, Method> METHODS = new HashMap<>();

    private static boolean resolved;
    private static boolean available;

    private SparkWitchScopeBridge() {
    }

    static synchronized boolean available() {
        if (!resolved) {
            resolved = true;
            available = resolve();
        }
        return available;
    }

    private static boolean resolve() {
        if (!FabricLoader.getInstance().isModLoaded("sparkwitch")) {
            return false;
        }
        try {
            Class<?> api = Class.forName(API_CLASS);
            for (String name : List.of("scopeMode", "scopeModes", "defaultScopeMode", "pictureInPictureSuppressed",
                    "lensResolutionPercent", "lensResolutionChoices", "defaultLensResolutionPercent",
                    "scopedSensitivityPercent", "scopedSensitivityMin", "scopedSensitivityMax",
                    "scopedSensitivityStep", "defaultScopedSensitivityPercent", "save")) {
                METHODS.put(name, api.getMethod(name));
            }
            METHODS.put("setScopeMode", api.getMethod("setScopeMode", String.class));
            METHODS.put("setLensResolutionPercent", api.getMethod("setLensResolutionPercent", int.class));
            METHODS.put("setScopedSensitivityPercent", api.getMethod("setScopedSensitivityPercent", int.class));
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            // An older SparkWitch keeps its scope rows in vanilla Accessibility, so nothing is lost.
            // 旧版 SparkWitch 仍把瞄准镜选项放在原版辅助功能里，不会丢失入口。
            LOGGER.info("SparkWitch has no ScopeSettingsApi; the scope page stays hidden ({})", exception.toString());
            return false;
        }
    }

    static String scopeMode() {
        return call("scopeMode", String.class, "zoom_blur");
    }

    static void setScopeMode(String mode) {
        call("setScopeMode", Void.class, null, String.class, mode);
    }

    @SuppressWarnings("unchecked")
    static List<String> scopeModes() {
        List<String> modes = call("scopeModes", List.class, List.of());
        return modes == null ? List.of() : List.copyOf(modes);
    }

    static String defaultScopeMode() {
        return call("defaultScopeMode", String.class, "zoom_blur");
    }

    static boolean pictureInPictureSuppressed() {
        return call("pictureInPictureSuppressed", Boolean.class, false);
    }

    static int lensResolutionPercent() {
        return call("lensResolutionPercent", Integer.class, 100);
    }

    static void setLensResolutionPercent(int percent) {
        call("setLensResolutionPercent", Void.class, null, int.class, percent);
    }

    static List<Integer> lensResolutionChoices() {
        int[] choices = call("lensResolutionChoices", int[].class, new int[0]);
        return Arrays.stream(choices).boxed().toList();
    }

    static int defaultLensResolutionPercent() {
        return call("defaultLensResolutionPercent", Integer.class, 100);
    }

    static int scopedSensitivityPercent() {
        return call("scopedSensitivityPercent", Integer.class, 100);
    }

    static void setScopedSensitivityPercent(int percent) {
        call("setScopedSensitivityPercent", Void.class, null, int.class, percent);
    }

    static int scopedSensitivityMin() {
        return call("scopedSensitivityMin", Integer.class, 10);
    }

    static int scopedSensitivityMax() {
        return call("scopedSensitivityMax", Integer.class, 200);
    }

    static int scopedSensitivityStep() {
        return call("scopedSensitivityStep", Integer.class, 5);
    }

    static int defaultScopedSensitivityPercent() {
        return call("defaultScopedSensitivityPercent", Integer.class, 100);
    }

    static void save() {
        call("save", Void.class, null);
    }

    private static <T> T call(String name, Class<T> type, T fallback) {
        return call(name, type, fallback, null, null);
    }

    private static <T> T call(String name, Class<T> type, T fallback, Class<?> argumentType, Object argument) {
        if (!available()) {
            return fallback;
        }
        try {
            Method method = METHODS.get(name);
            Object result = argumentType == null ? method.invoke(null) : method.invoke(null, argument);
            if (type == Void.class) {
                return fallback;
            }
            return type.isInstance(result) ? type.cast(result) : fallback;
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException e) {
            LOGGER.warn("SparkWitch scope settings call {} failed; hiding the scope page", name, e);
            available = false;
            return fallback;
        }
    }
}
