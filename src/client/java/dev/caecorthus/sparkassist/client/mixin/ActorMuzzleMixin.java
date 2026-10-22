package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.ponder.ActorPlayer;
import dev.doctor4t.wathe.util.MatrixParticleManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wathe records the muzzle of every other player drawn holding a gun in a static map that only that player's shot
 * clears. Demo actors never shoot and nothing reads their muzzle, so they stay out of it: otherwise the map would keep
 * each actor, and its world, after the demo closes, and an actor sharing a real player's entity id (entities compare
 * by id) would overwrite that player's muzzle.
 * Wathe 把每个持枪的其他玩家的枪口位置记在静态表中，只有该玩家开枪才会移除。演示演员从不开枪，也没有代码读取它们的枪口，
 * 因此不让它们进表：否则演示关闭后该表仍会持有每个演员及其世界，而与真实玩家实体 id 相同的演员（实体按 id 比较）还会覆盖
 * 该玩家的枪口位置。
 */
@Mixin(value = MatrixParticleManager.class, remap = false)
public interface ActorMuzzleMixin {
    @Inject(method = "setMuzzlePosForPlayer", at = @At("HEAD"), cancellable = true)
    private static void sparkassist$skipActors(PlayerEntity player, Vec3d pos, CallbackInfo ci) {
        if (player instanceof ActorPlayer) {
            ci.cancel();
        }
    }
}
