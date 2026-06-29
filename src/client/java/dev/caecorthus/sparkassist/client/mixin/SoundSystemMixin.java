package dev.caecorthus.sparkassist.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.caecorthus.sparkassist.client.sound.EventSoundVolumeController;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
    /**
     * Scales the initial volume after vanilla category volume is applied.
     * 在原版分类音量生效后缩放初次播放音量。
     */
    @ModifyExpressionValue(
            method = "play(Lnet/minecraft/client/sound/SoundInstance;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sound/SoundSystem;getAdjustedVolume(FLnet/minecraft/sound/SoundCategory;)F"
            )
    )
    private float sparkassist$scaleInitialEventSoundVolume(
            float originalVolume,
            @Local(argsOnly = true) SoundInstance soundInstance,
            @Local(ordinal = 0) Sound selectedSound
    ) {
        return EventSoundVolumeController.adjustedVolume(soundInstance, selectedSound, originalVolume);
    }

    /**
     * Scales tick/update recalculations for already playing sounds.
     * 缩放已播放声音在 tick 或音量刷新时重新计算出的音量。
     */
    @Inject(
            method = "getAdjustedVolume(Lnet/minecraft/client/sound/SoundInstance;)F",
            at = @At("RETURN"),
            cancellable = true
    )
    private void sparkassist$scaleEventSoundVolume(
            SoundInstance soundInstance,
            CallbackInfoReturnable<Float> cir
    ) {
        cir.setReturnValue(EventSoundVolumeController.adjustedVolume(soundInstance, cir.getReturnValueF()));
    }
}
