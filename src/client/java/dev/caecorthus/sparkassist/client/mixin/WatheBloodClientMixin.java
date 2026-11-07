package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.blood.ScavengerBlood;
import dev.caecorthus.sparkassist.client.config.SparkAssistClientSettings;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips the spray when the corpse is a hidden one. Wathe Blood 1.1.4 decides to bleed from the mere presence of a
 * {@code PlayerBodyEntity}, and a scavenger's victim keeps its body entity — it is only hidden from rendering — so
 * without this the floor would be painted next to an invisible corpse.
 * 尸体被隐藏时跳过喷血。Wathe Blood 1.1.4 仅凭 {@code PlayerBodyEntity} 是否存在来决定喷血，而清道夫的受害者尸体实体仍在
 * ——只是不渲染——所以没有这段就会在一具看不见的尸体旁画出一滩血。
 *
 * <p>The descriptor is the 1.1.4 one: {@code (ClientWorld, PlayerBodyEntity, PlayerEntity, Item)}. {@code require = 0}
 * keeps a future version with a different signature from failing the whole client.
 * 描述符用的是 1.1.4 的：{@code (ClientWorld, PlayerBodyEntity, PlayerEntity, Item)}。{@code require = 0} 让签名不同的
 * 未来版本不会拖垮整个客户端。</p>
 */
@Pseudo
@Mixin(targets = "wathe_blood.WatheBloodClient")
public abstract class WatheBloodClientMixin {
    @Inject(method = "howDoISpawnBlood", at = @At("HEAD"), cancellable = true, require = 0)
    private void sparkassist$skipHiddenBodyBlood(ClientWorld world, PlayerBodyEntity body, PlayerEntity killer,
                                                 Item weapon, CallbackInfo ci) {
        if (!SparkAssistClientSettings.suppressScavengerBlood() || body == null) {
            return;
        }
        if (ScavengerBlood.isBodyHidden(world, body.getPlayerUuid())) {
            ci.cancel();
        }
    }
}
