package dev.caecorthus.sparkassist.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Smooth playback of SparkAssist scenes in Ponder's comfy reading mode, which ticks the scene every third client tick
 * while a caption shows. When the last caption closes the stretch drops to one tick but the countdown keeps running,
 * so for up to two ticks the partial tick goes negative (everything freezes) and then restarts the same tick
 * (everything jumps back). The countdown shrinks with the stretch instead, and partial ticks stay within 0..1.
 * 让 SparkAssist 场景在 Ponder 舒适阅读模式下平滑播放：该模式在有字幕时每三个客户端 tick 才推进一次场景。最后一条字幕
 * 关闭时间隔缩回一 tick，倒计时却照旧，于是最多两 tick 内插值进度为负（画面冻结），随后又重放同一 tick（画面往回跳）。
 * 这里让倒计时随间隔一起缩短，并把插值进度限制在 0..1。
 */
@Pseudo
@Mixin(targets = "net.createmod.ponder.foundation.ui.PonderUI", remap = false)
public abstract class PonderTimingMixin {
    @Shadow(remap = false)
    private int extendedTickLength;
    @Shadow(remap = false)
    private int extendedTickTimer;

    @Inject(method = "tick", remap = true, at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, ordinal = 0, remap = false,
            target = "Lnet/createmod/ponder/foundation/ui/PonderUI;extendedTickTimer:I"))
    private void sparkassist$shrinkCountdown(CallbackInfo ci) {
        if (((PonderUI) (Object) this).getActiveScene().getNamespace().equals("sparkassist")) {
            extendedTickTimer = Math.min(extendedTickTimer, extendedTickLength);
        }
    }

    @ModifyReturnValue(method = "getPartialTicks", at = @At("RETURN"))
    private static float sparkassist$clampPartialTicks(float partialTicks) {
        return MinecraftClient.getInstance().currentScreen instanceof PonderUI ui
                && ui.getActiveScene().getNamespace().equals("sparkassist")
                ? MathHelper.clamp(partialTicks, 0, 1) : partialTicks;
    }
}
