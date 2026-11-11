package dev.caecorthus.sparkassist.client.blood;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import java.lang.reflect.Method;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.world.World;
import org.ladysnake.cca.api.v3.component.ComponentKey;
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
    private static final String MOD_ID = "noellesroles";
    private static final String COMPONENT = "org.agmas.noellesroles.scavenger.HiddenBodiesWorldComponent";

    private static boolean resolved;
    private static ComponentKey<?> componentKey;
    private static Method isHidden;

    private ScavengerBlood() {
    }

    /**
     * True when this body is hidden from the local player, meaning no blood should be sprayed beside it. NoellesRoles
     * still draws a hidden body for spectators and for killer and neutral roles, so for them the blood stays too —
     * a visible corpse with a clean floor would stand out from every other one.
     * 该尸体对本地玩家隐藏、不应在旁边喷血时返回 true。NoellesRoles 仍会为旁观者以及杀手、中立身份绘制被隐藏的尸体，因此
     * 对他们血迹同样保留——看得见的尸体配上干净的地板，反而会在所有尸体中显得突兀。
     */
    public static boolean isBodyHidden(World world, UUID owner) {
        if (world == null || owner == null || !resolve()) {
            return false;
        }
        try {
            Object component = componentKey.getNullable(world);
            return component != null && Boolean.TRUE.equals(isHidden.invoke(component, owner))
                    && !localPlayerSeesHiddenBodies(world);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    /** The same exemptions NoellesRoles' {@code ScavengerBodyHideMixin} applies before hiding a body. 与 NoellesRoles 的 {@code ScavengerBodyHideMixin} 隐藏尸体前所用的豁免一致。 */
    private static boolean localPlayerSeesHiddenBodies(World world) {
        if (WatheClient.canSeeSpectatorInformation()) {
            return true;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return false;
        }
        Role role = GameWorldComponent.KEY.maybeGet(world).map(game -> game.getRole(player)).orElse(null);
        return role != null && (role.getFaction() == Faction.KILLER || role.getFaction() == Faction.NEUTRAL);
    }

    private static boolean resolve() {
        if (resolved) {
            return componentKey != null;
        }
        resolved = true;
        // Absence is the normal case for a pack without NoellesRoles, not something to warn about.
        // 没装 NoellesRoles 的整合包里缺席是常态，不值得警告。
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return false;
        }
        try {
            Class<?> cls = Class.forName(COMPONENT);
            // ComponentKey's lookups take Object, so the key is used through its type rather than a reflective
            // get(World), which does not exist.
            // ComponentKey 的查询方法参数是 Object，因此直接按其类型使用 key，而不是反射一个并不存在的 get(World)。
            Method hidden = cls.getMethod("isHidden", UUID.class);
            componentKey = (ComponentKey<?>) cls.getField("KEY").get(null);
            isHidden = hidden;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException exception) {
            componentKey = null;
            LOGGER.warn("NoellesRoles hidden-body component unavailable; scavenger blood suppression is inert", exception);
        }
        return componentKey != null;
    }
}
