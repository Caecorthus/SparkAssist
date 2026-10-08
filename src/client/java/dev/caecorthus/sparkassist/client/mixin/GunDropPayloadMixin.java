package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.achievement.AchievementClientState;
import dev.doctor4t.wathe.util.GunDropPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Notes Wathe's "you dropped your gun for shooting an innocent" notice. 记录 Wathe 的“误杀无辜而丢枪”通知。 */
@Mixin(value = GunDropPayload.Receiver.class, remap = false)
public abstract class GunDropPayloadMixin {
    @Inject(
            method = "receive(Ldev/doctor4t/wathe/util/GunDropPayload;Lnet/fabricmc/fabric/api/client/networking/v1/ClientPlayNetworking$Context;)V",
            at = @At("HEAD")
    )
    private void sparkassist$gunDrop(CallbackInfo ci) {
        AchievementClientState.onGunDrop();
    }
}
