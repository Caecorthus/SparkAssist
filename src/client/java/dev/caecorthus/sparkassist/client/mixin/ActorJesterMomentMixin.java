package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.ponder.ActorPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * During a Jester Moment (NoellesRoles) every other player is drawn with the Jester's bat, raised arms and cape; not
 * while a demo actor is being drawn. Skipped when NoellesRoles is absent.
 * 小丑时刻（NoellesRoles）期间，其他所有玩家都画成拿着小丑的球棒、举起双臂并披着小丑的披风；绘制演示演员时不这样画。
 * 未安装 NoellesRoles 时跳过。
 */
@Pseudo
@Mixin(targets = "org.agmas.noellesroles.client.jester.JesterMomentClient", remap = false)
public abstract class ActorJesterMomentMixin {
    @Inject(method = "isActiveForLocalViewer", at = @At("HEAD"), cancellable = true, require = 0)
    private static void sparkassist$notForActors(CallbackInfoReturnable<Boolean> cir) {
        if (ActorPlayer.isDrawing()) {
            cir.setReturnValue(false);
        }
    }
}
