package dev.caecorthus.sparkassist.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.caecorthus.sparkassist.client.ponder.ActorPlayer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A Blind reader (SparkWitch) sees other players without held items, armour or arm poses; demo actors are not other
 * players, so their knives, vests and gun stances stay. Skipped when SparkWitch is absent. The gate is asked for every
 * feature layer of every living entity, and only reads state, so the answer is changed on return rather than through
 * a cancellable callback allocated per call.
 * 盲人读者（SparkWitch）看其他玩家时不显示手持物、护甲与手臂姿势；演示演员不是其他玩家，刀、背心与持枪姿势照常显示。
 * 未安装 SparkWitch 时跳过。每个生物的每个附加层都会询问该闸门，且它只读取状态，因此在返回时修改结果，而不是通过
 * 每次调用都要分配的可取消回调。
 */
@Pseudo
@Mixin(targets = "dev.caecorthus.sparkwitch.client.blind.gate.BlindClientGates", remap = false)
public abstract class ActorBlindGateMixin {
    @ModifyReturnValue(method = "suppressesFeatures", at = @At("RETURN"), require = 0)
    private static boolean sparkassist$keepActorFeatures(boolean suppresses, @Local(argsOnly = true) Entity entity) {
        return suppresses && !(entity instanceof ActorPlayer);
    }
}
