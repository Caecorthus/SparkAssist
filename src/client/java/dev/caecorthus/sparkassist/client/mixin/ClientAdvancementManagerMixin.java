package dev.caecorthus.sparkassist.client.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import dev.caecorthus.sparkassist.client.achievement.AchievementAdvancements;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.AdvancementUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the local achievement page in the client's advancement tree and away from the server.
 * 让本地成就页留在客户端的进度树里，并且不让服务器知道它。
 */
@Mixin(ClientAdvancementManager.class)
public abstract class ClientAdvancementManagerMixin {
    // Each server update may clear the whole tree (join, /reload); put the page back afterwards.
    // 服务器每次更新都可能清空整棵树（加入、/reload）；更新后把成就页补回去。
    @Inject(method = "onAdvancements", at = @At("TAIL"))
    private void sparkassist$restoreAchievements(AdvancementUpdateS2CPacket packet, CallbackInfo ci) {
        AchievementAdvancements.restore((ClientAdvancementManager) (Object) this);
    }

    // Opening a tab tells the server its id; ours is local, so say nothing.
    // 打开某页会把它的 id 告诉服务器；我们这页是本地的，什么都不发。
    @WrapWithCondition(
            method = "selectTab",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"
            )
    )
    private boolean sparkassist$keepAchievementTabLocal(ClientPlayNetworkHandler handler, Packet<?> packet,
                                                        AdvancementEntry tab, boolean local) {
        return tab == null || !AchievementAdvancements.isOurs(tab.id());
    }
}
