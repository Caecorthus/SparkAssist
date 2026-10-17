package dev.caecorthus.sparkassist.client.ponder;

import java.lang.reflect.Field;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps Entity Texture Features' skin extras off demo actors. ETF draws a player's extra skin features (a villager or
 * textured nose, the coat, emissive and enchanted skin parts) from a note it keeps on each player renderer, "the skin
 * features of the player being drawn", which it refreshes from inside the renderer's getTexture. Actors answer
 * getTexture themselves ({@link dev.caecorthus.sparkassist.client.mixin.ActorTextureMixin}), so for an actor the note
 * would still describe the last real player drawn, and the actor would wear that player's nose and coat. An actor has
 * no skin features: the note is blanked whenever an actor answers, and the next real player writes it again. Reached
 * by reflection, so nothing changes without ETF.
 * 让 Entity Texture Features 的皮肤附加件不出现在演示演员身上。ETF 在每个玩家渲染器上记一条"正在绘制的玩家的皮肤特性"
 * （村民鼻子或贴图鼻子、外套、发光与附魔的皮肤部件），在渲染器的 getTexture 内刷新，再据此绘制。演员自己回答
 * getTexture（见 ActorTextureMixin），这条记录对演员而言仍是上一个绘制的真实玩家，演员就会戴上那名玩家的鼻子和外套。
 * 演员没有皮肤特性：每当演员作答就清空这条记录，下一个真实玩家会重新写入。通过反射访问，未安装 ETF 时不产生任何影响。
 */
public final class ActorSkinCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Ponder");
    private static final String ETF_MOD_ID = "entity_texture_features";
    /**
     * ETF's note on the renderer: MixinPlayerEntityRenderer's {@code etf$ETFPlayerTexture}, merged into
     * PlayerEntityRenderer under its own name (ETF 7.0.8 through 7.2.6 on 1.21.1). Its feature layer draws nothing
     * while the note is null.
     * ETF 记在渲染器上的字段：MixinPlayerEntityRenderer 的 etf$ETFPlayerTexture，以原名合并进 PlayerEntityRenderer
     * （1.21.1 上的 ETF 7.0.8 至 7.2.6）。记录为 null 时其特性图层不绘制任何东西。
     */
    private static final String SKIN_FEATURES_FIELD = "etf$ETFPlayerTexture";
    @Nullable
    private static final Field SKIN_FEATURES = locate();

    private ActorSkinCompat() {
    }

    /** Blank ETF's note on {@code renderer}, which is answering for an actor. 清空 renderer 上 ETF 的记录；它正在替演员作答。 */
    public static void forgetSkinFeatures(PlayerEntityRenderer renderer) {
        if (SKIN_FEATURES == null) {
            return;
        }
        try {
            SKIN_FEATURES.set(renderer, null);
        } catch (IllegalAccessException exception) {
            // Made accessible in locate(); not reachable. 已在 locate() 中设为可访问；不会到达这里。
            throw new IllegalStateException(exception);
        }
    }

    @Nullable
    private static Field locate() {
        if (!FabricLoader.getInstance().isModLoaded(ETF_MOD_ID)) {
            return null;
        }
        try {
            Field field = PlayerEntityRenderer.class.getDeclaredField(SKIN_FEATURES_FIELD);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.warn("Could not reach Entity Texture Features' skin-feature note; Ponder actors may inherit a real player's skin features", exception);
            return null;
        }
    }
}
