package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.guidebook.GuidebookOpenButton;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Provides the same book icon when lobby or spectator players have no limited inventory.
 * 大厅和旁观玩家无法打开受限背包时，在暂停菜单提供同款书本入口。
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sparkassist$addGuidebookFallback(CallbackInfo ci) {
        boolean limitedInventoryAvailable = WatheClient.trainComponent != null
                && WatheClient.trainComponent.hasHud()
                && WatheClient.isPlayerAliveAndInSurvival();
        if (limitedInventoryAvailable) {
            return;
        }

        int x = this.width - GuidebookOpenButton.SIZE - 8;
        this.addDrawableChild(new GuidebookOpenButton(x, 8, (GameMenuScreen) (Object) this));
    }
}
