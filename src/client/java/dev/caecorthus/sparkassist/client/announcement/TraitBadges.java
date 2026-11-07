package dev.caecorthus.sparkassist.client.announcement;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.ladysnake.cca.api.v3.component.ComponentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the player's this-round traits from SparkTraits without compiling against it. SparkTraits is optional, so the
 * lookup is done reflectively and every failure degrades to "no badges" instead of breaking the announcement.
 * 在不编译期依赖 SparkTraits 的前提下读取玩家本局词条。SparkTraits 是可选模组，因此用反射查找，任何失败都退化为
 * 「没有词条」，不影响报幕本身。
 *
 * <p>The {@code ComponentKey} and the two accessors are resolved once and cached: the announcement draws every frame, so
 * a per-frame {@code Class.forName} would be wasteful.
 * {@code ComponentKey} 与两个访问器只解析一次并缓存：报幕每帧绘制，逐帧 {@code Class.forName} 太浪费。</p>
 */
public final class TraitBadges {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/RoleAnnouncement");
    private static final String COMPONENT_CLASS = "dev.caecorthus.sparktraits.component.TraitPlayerComponent";

    private static boolean resolved;
    /** Logged once so a single run tells us whether the bridge works. 只记录一次，一次运行即可判断桥接是否可用。 */
    private static boolean reported;
    private static Object componentKey;
    private static Method maybeGet;
    private static Method getActiveTraitIds;
    /** {@code TraitRegistry.get(Identifier)} and {@code TraitDefinition.color()}. 词条表查询与取色器。 */
    private static Method traitGet;
    private static Method traitColor;
    /** Fallback when a trait has no resolvable colour. 词条取不到颜色时的兜底。 */
    private static final int DEFAULT_COLOR = 0xFFB4A080;

    private TraitBadges() {
    }

    /**
     * The trait names the player currently holds, in assignment order. Empty when SparkTraits is absent, the component
     * is not synced yet, or the player simply has no traits.
     * 玩家当前持有的词条名，按分配顺序。SparkTraits 缺席、组件尚未同步或玩家没有词条时返回空表。
     */
    public static List<Text> active(PlayerEntity player) {
        if (player == null) {
            return List.of();
        }
        if (!FabricLoader.getInstance().isModLoaded("sparktraits")) {
            report("mod-missing", 0);
            return List.of();
        }
        if (!resolve()) {
            report("resolve-failed", 0);
            return List.of();
        }
        try {
            Object optional = maybeGet.invoke(componentKey, (ComponentProvider) player);
            if (!(optional instanceof Optional<?> present) || present.isEmpty()) {
                report("no-component", 0);
                return List.of();
            }
            Object component = present.get();
            Object ids = getActiveTraitIds.invoke(component);
            if (!(ids instanceof List<?> list) || list.isEmpty()) {
                report("no-traits", 0);
                return List.of();
            }
            List<Text> names = new ArrayList<>(list.size());
            for (Object id : list) {
                if (id instanceof net.minecraft.util.Identifier identifier) {
                    names.add(Text.translatable("trait." + identifier.getNamespace() + "." + identifier.getPath()
                            + ".name").withColor(colorOf(identifier)));
                }
            }
            report("ok", names.size());
            return names;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException exception) {
            // One warning is enough; a broken bridge would otherwise spam every frame.
            // 警告一次即可；桥接失效时否则会每帧刷屏。
            report("threw", 0);
            if (resolved) {
                resolved = false;
                LOGGER.warn("Could not read SparkTraits traits for the announcement", exception);
            }
            return List.of();
        }
    }

    /** Resolves the reflective handles once. 一次性解析反射句柄。 */
    private static boolean resolve() {
        if (resolved) {
            return componentKey != null;
        }
        resolved = true;
        try {
            Class<?> component = Class.forName(COMPONENT_CLASS);
            Field key = component.getField("KEY");
            componentKey = key.get(null);
            maybeGet = componentKey.getClass().getMethod("maybeGet", ComponentProvider.class);
            getActiveTraitIds = component.getMethod("getActiveTraitIds");
            Class<?> registry = Class.forName("dev.caecorthus.sparktraits.api.TraitRegistry");
            traitGet = registry.getMethod("get", net.minecraft.util.Identifier.class);
            Class<?> definition = Class.forName("dev.caecorthus.sparktraits.api.TraitDefinition");
            traitColor = definition.getMethod("color");
        } catch (ReflectiveOperationException | LinkageError exception) {
            componentKey = null;
            LOGGER.warn("SparkTraits is installed but its trait component could not be reached", exception);
        }
        return componentKey != null;
    }

    /** The joined badge line, or {@code null} when there is nothing to show. 拼成一行，没东西可显示时返回 {@code null}。 */
    public static Text line(PlayerEntity player) {
        List<Text> names = active(player);
        if (names.isEmpty()) {
            return null;
        }
        MutableText joined = Text.translatable("hud.sparkassist.announcement.traits_prefix");
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                joined.append(Text.literal("  ·  "));
            }
            joined.append(names.get(i));
        }
        return joined;
    }

    /**
     * The trait's own colour, opaque. SparkTraits stores it on the trait definition, so each badge renders in the colour
     * the rest of the mod uses for that trait; anything unresolvable falls back to the muted brass.
     * 词条自身的颜色（不透明）。SparkTraits 把颜色存在词条定义上，因此每个词条都用模组里该词条的配色渲染；
     * 取不到时回落到淡黄铜色。
     */
    private static int colorOf(net.minecraft.util.Identifier id) {
        if (traitGet == null || traitColor == null) {
            return DEFAULT_COLOR;
        }
        try {
            Object trait = traitGet.invoke(null, id);
            if (trait == null) {
                return DEFAULT_COLOR;
            }
            Object value = traitColor.invoke(trait);
            if (value instanceof Integer rgb) {
                // Trait colours are authored as RGB; force them opaque so the badge alpha stays in our hands.
                // 词条颜色按 RGB 编写；强制不透明，把透明度留给报幕本身控制。
                return rgb | 0xFF000000;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // A trait implementation without the record accessor is not worth a log line per frame.
            // 没有该 record 访问器的词条实现不值得每帧打一行日志。
        }
        return DEFAULT_COLOR;
    }

    /** One-time diagnostic line: how many traits the announcement actually sees. 一次性诊断：报幕实际看到几个词条。 */
    private static void report(String stage, int count) {
        if (reported) {
            return;
        }
        reported = true;
        LOGGER.info("Role announcement trait probe: stage={} count={}", stage, count);
    }
}
