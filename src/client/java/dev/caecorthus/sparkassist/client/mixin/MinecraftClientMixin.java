package dev.caecorthus.sparkassist.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.caecorthus.sparkassist.client.guidebook.GuidebookClientState;
import dev.caecorthus.sparkassist.client.guidebook.GuidebookScreen;
import dev.caecorthus.sparkassist.guidebook.GuidebookEntryPoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Dead spectators open the guide with the inventory key, the same key living players use for the limited inventory.
 * 死亡旁观者按背包键打开指南，与存活玩家打开受限背包的按键一致。
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    // Ordinal 1 is the vanilla inventory screen, the call Wathe wraps for the limited inventory. Either wrapper order
    // works: Wathe drops the screen during its fade and passes any other screen through for non-living players.
    // 序号 1 是原版背包界面，也是 Wathe 替换为受限背包的调用。两个包装的先后顺序不影响结果：Wathe 在淡入淡出期间丢弃
    // 界面，对非存活玩家则原样放行。
    @WrapOperation(method = "handleInputEvents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V",
            ordinal = 1))
    private void sparkassist$openGuidebookForSpectators(MinecraftClient client, Screen screen,
            Operation<Void> original) {
        boolean guide = GuidebookClientState.entryPoint(client) == GuidebookEntryPoint.INVENTORY_KEY;
        original.call(client, guide ? new GuidebookScreen(null) : screen);
    }
}
