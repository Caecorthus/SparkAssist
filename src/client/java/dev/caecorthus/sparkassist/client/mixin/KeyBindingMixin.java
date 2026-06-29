package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.input.InstinctKeyController;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {
    @Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)
    private void sparkassist$toggleWatheInstinctKey(CallbackInfoReturnable<Boolean> cir) {
        KeyBinding keyBinding = (KeyBinding) (Object) this;
        if (!InstinctKeyController.shouldHandle(keyBinding.getTranslationKey())) {
            return;
        }
        cir.setReturnValue(InstinctKeyController.effectivePressed(cir.getReturnValue()));
    }
}
