package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.sound.EventSoundVolumeController;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
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
