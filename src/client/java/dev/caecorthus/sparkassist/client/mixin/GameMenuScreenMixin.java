package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.settings.screen.SettingsGearButton;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The settings gear in the pause menu's top-right corner (unless the menu itself is hidden with F3+Esc). The guide
 * book used to sit here too; it now lives behind the inventory key ({@code GuidebookInventoryButton}).
 * 暂停菜单右上角的设置齿轮（F3+Esc 隐藏菜单时除外）。指南书原来也在这里，现在改到背包键之后（{@code GuidebookInventoryButton}）。
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    private static final int EDGE = 8;

    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sparkassist$addSettingsGear(CallbackInfo ci) {
        GameMenuScreen self = (GameMenuScreen) (Object) this;
        if (!self.shouldShowMenu()) {
            return;
        }
        SettingsGearButton gear = new SettingsGearButton(this.width - EDGE - SettingsGearButton.SIZE, EDGE, self);
        this.addDrawableChild(gear);
        // The brass tooltip goes on top of the finished pause menu. Fabric resets screen events on every init and
        // resize, so this never registers twice.
        // 黄铜提示框绘制在已完成的暂停菜单之上。Fabric 每次初始化与缩放都会重置界面事件，因此不会重复注册。
        ScreenEvents.afterRender(this).register((screen, context, mouseX, mouseY, delta) ->
                gear.renderTooltip(context, this.textRenderer));
    }
}
