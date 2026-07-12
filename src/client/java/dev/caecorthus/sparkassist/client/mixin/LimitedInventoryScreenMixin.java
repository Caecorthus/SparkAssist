package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.guidebook.GuidebookOpenButton;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the primary guidebook entry to Wathe's limited inventory.
 * 在 Wathe 受限背包右上角加入指南书主入口。
 */
@Mixin(LimitedInventoryScreen.class)
public abstract class LimitedInventoryScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    protected LimitedInventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void sparkassist$addGuidebookButton(CallbackInfo ci) {
        int x = this.width - GuidebookOpenButton.SIZE - 8;
        this.addDrawableChild(new GuidebookOpenButton(x, 22, (LimitedInventoryScreen) (Object) this));
    }
}
