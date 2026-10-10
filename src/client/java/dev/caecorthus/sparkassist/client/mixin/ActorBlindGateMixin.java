package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.ponder.ActorPlayer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A Blind reader (SparkWitch) sees other players without held items, armour or arm poses; demo actors are not other
 * players, so their knives, vests and gun stances stay. Skipped when SparkWitch is absent.
 * 盲人读者（SparkWitch）看其他玩家时不显示手持物、护甲与手臂姿势；演示演员不是其他玩家，刀、背心与持枪姿势照常显示。
 * 未安装 SparkWitch 时跳过。
 */
@Pseudo
@Mixin(targets = "dev.caecorthus.sparkwitch.client.blind.gate.BlindClientGates", remap = false)
public abstract class ActorBlindGateMixin {
    @Inject(method = "suppressesFeatures", at = @At("HEAD"), cancellable = true, require = 0)
    private static void sparkassist$keepActorFeatures(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ActorPlayer) {
            cir.setReturnValue(false);
        }
    }
}
