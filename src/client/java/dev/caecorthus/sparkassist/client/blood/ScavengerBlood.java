package dev.caecorthus.sparkassist.client.blood;

import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads NoellesRoles' hidden-body state without compiling against it.
 * 在不编译期依赖 NoellesRoles 的前提下读取隐藏尸体状态。
 *
 * <p>A scavenger kill does not delete the corpse — the body entity still exists and still ticks, it is simply not shown.
 * That matters here because Wathe Blood 1.1.4 spawns its spray from the presence of a {@code PlayerBodyEntity}, so it
 * would happily paint blood next to an invisible body.
 * 清道夫击杀并不会删除尸体——尸体实体仍然存在、仍然 tick，只是不显示。这一点在这里很关键，因为 Wathe Blood 1.1.4 正是
 * 依据 {@code PlayerBodyEntity} 的出现来喷血的，它会给一具不可见的尸体旁边照喷不误。</p>
 */
public final class ScavengerBlood {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Blood");
    private static final String COMPONENT = "org.agmas.noellesroles.scavenger.HiddenBodiesWorldComponent";

    private static boolean resolved;
    private static Object componentKey;
    private static Method keyGet;
    private static Method isHidden;

    private ScavengerBlood() {
    }

    /** True when this body is hidden from view, meaning no blood should be sprayed beside it. 该尸体被隐藏、不应在旁边喷血时返回 true。 */
    public static boolean isBodyHidden(World world, UUID owner) {
        if (world == null || owner == null || !resolve()) {
            return false;
        }
        try {
            Object component = keyGet.invoke(componentKey, world);
            return component != null && Boolean.TRUE.equals(isHidden.invoke(component, owner));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    private static boolean resolve() {
        if (resolved) {
            return componentKey != null;
        }
        resolved = true;
        try {
            Class<?> cls = Class.forName(COMPONENT);
            componentKey = cls.getField("KEY").get(null);
            keyGet = componentKey.getClass().getMethod("get", World.class);
            isHidden = cls.getMethod("isHidden", UUID.class);
        } catch (ReflectiveOperationException | LinkageError exception) {
            componentKey = null;
            LOGGER.warn("NoellesRoles hidden-body component unavailable; scavenger blood suppression is inert", exception);
        }
        return componentKey != null;
    }
}
