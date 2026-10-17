package dev.caecorthus.sparkassist.client.ponder;

import java.lang.reflect.Method;
import java.util.function.Function;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps model-animation resource packs (Detailed Animations Reworked, Fresh Animations…) off demo actors. Entity Model
 * Features plays such a pack's player animations on every player it renders, and an actor is a player that never joins
 * the world: the pack reads state the actor does not have and pulls its limbs apart mid-pose. EMF lets a mod lock
 * entities to the vanilla model, so actors are locked and draw with exactly the poses the demo sets; real players keep
 * the pack. Reached by reflection, so nothing changes without EMF.
 * 让模型动画资源包（Detailed Animations Reworked、Fresh Animations 等）不作用于演示演员。Entity Model Features 会把
 * 这类资源包的玩家动画套在它渲染的每个玩家身上，而演员是从不加入世界的玩家：资源包读取演员并不具备的状态，会在动作中途
 * 把它的四肢拆散。EMF 允许模组把实体锁定为原版模型，因此演员被锁定，按演示设定的姿势绘制；真实玩家照常使用资源包。
 * 通过反射访问，未安装 EMF 时不产生任何影响。
 */
final class ActorModelCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Ponder");
    private static final String EMF_MOD_ID = "entity_model_features";

    private ActorModelCompat() {
    }

    static void register() {
        if (!FabricLoader.getInstance().isModLoaded(EMF_MOD_ID)) {
            return;
        }
        try {
            Class<?> api = Class.forName("traben.entity_model_features.EMFAnimationApi");
            Method vanillaModelCondition = api.getMethod("registerVanillaModelCondition", Function.class);
            // EMF hands the condition the entity being rendered (its EMFEntity is the entity itself).
            // EMF 传给条件的就是正在渲染的实体（其 EMFEntity 即实体本身）。
            Function<Object, Boolean> isActor = entity -> entity instanceof ActorPlayer;
            if (!Boolean.TRUE.equals(vanillaModelCondition.invoke(null, isActor))) {
                LOGGER.warn("Entity Model Features refused the vanilla-model condition; its animation packs may distort Ponder actors");
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.warn("Could not keep Entity Model Features animations off Ponder actors", exception);
        }
    }
}
