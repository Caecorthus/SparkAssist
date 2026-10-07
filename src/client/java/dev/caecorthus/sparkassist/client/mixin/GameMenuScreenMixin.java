package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.guidebook.GuidebookClientState;
import dev.caecorthus.sparkassist.client.guidebook.GuidebookOpenButton;
import dev.caecorthus.sparkassist.guidebook.GuidebookEntryPoint;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Provides the same book icon for lobby and creative players, who have no limited inventory; dead spectators use the
 * inventory key instead.
 * 大厅和创造模式玩家没有受限背包，在暂停菜单提供同款书本入口；死亡旁观者改用背包键打开。
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sparkassist$addGuidebookFallback(CallbackInfo ci) {
        if (GuidebookClientState.entryPoint(this.client) != GuidebookEntryPoint.PAUSE_MENU) {
            return;
        }

        int x = this.width - GuidebookOpenButton.SIZE - 8;
        GuidebookOpenButton button = new GuidebookOpenButton(x, 8, (GameMenuScreen) (Object) this);
        this.addDrawableChild(button);
        // The brass tooltip goes on top of the finished pause menu. Fabric resets screen events on every init and
        // resize, so this never registers twice.
        // 黄铜提示框绘制在已完成的暂停菜单之上。Fabric 每次初始化与缩放都会重置界面事件，因此不会重复注册。
        ScreenEvents.afterRender(this).register((screen, context, mouseX, mouseY, delta) ->
                button.renderTooltip(context, this.textRenderer));
    }
}
