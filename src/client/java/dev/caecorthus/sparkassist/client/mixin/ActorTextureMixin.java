package dev.caecorthus.sparkassist.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.caecorthus.sparkassist.client.ponder.ActorPlayer;
import dev.caecorthus.sparkassist.client.ponder.ActorSkinCompat;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Ponder demo actors keep their own skin. Several role views repaint every other player while the reader is that role
 * mid-round (the Wraith's and Black Raven's Steve, the Pathogen's gray crowd, the Jester Moment's psycho skin); the
 * outermost wrap answers for actors before any of them runs. Entity Texture Features also notes the drawn player's skin
 * features inside the original, so {@link ActorSkinCompat} blanks that note for actors: none of a real player's extras
 * carry over to them.
 * 思索演示中的演员保留自己的皮肤。读者在对局中担任某些身份时，会把其他所有玩家重新上色（冤魂与黑羽鸦的史蒂夫、
 * 病原体的灰色人群、小丑时刻的疯魔皮肤）；最外层的包装在它们之前替演员作答。Entity Texture Features 也在原方法内记下
 * 所绘玩家的皮肤特性，因此由 ActorSkinCompat 替演员清空这条记录：真实玩家的附加件不会带到演员身上。
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class ActorTextureMixin {
    @WrapMethod(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;")
    private Identifier sparkassist$actorSkin(AbstractClientPlayerEntity player, Operation<Identifier> original) {
        if (!(player instanceof ActorPlayer actor)) {
            return original.call(player);
        }
        ActorSkinCompat.forgetSkinFeatures((PlayerEntityRenderer) (Object) this);
        return actor.texture();
    }
}
