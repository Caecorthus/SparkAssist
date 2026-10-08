package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.achievement.AchievementClientState;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the achievement tracker that Wathe's round-end result reached this client. Wathe only writes that result
 * when a round is won, so {@code /stop} never triggers it.
 * 通知成就跟踪器：Wathe 的结算结果已到达本客户端。Wathe 只在有人获胜时写入结算，{@code /stop} 不会触发。
 */
@Mixin(value = GameRoundEndComponent.class, remap = false)
public abstract class GameRoundEndComponentMixin {
    @Inject(method = "readFromNbt", at = @At("TAIL"))
    private void sparkassist$roundEndSynced(CallbackInfo ci) {
        // The render thread is the client's sync; the integrated server loads its own copy on the server thread.
        // 渲染线程上的才是客户端同步；单人游戏的内置服务器在服务端线程读取它自己的副本。
        if (MinecraftClient.getInstance().isOnThread()) {
            AchievementClientState.onRoundEndSynced();
        }
    }
}
