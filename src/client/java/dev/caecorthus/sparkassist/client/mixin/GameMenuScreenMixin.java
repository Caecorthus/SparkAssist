package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.guidebook.GuidebookClientState;
import dev.caecorthus.sparkassist.client.guidebook.GuidebookOpenButton;
import dev.caecorthus.sparkassist.client.settings.screen.SettingsGearButton;
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
 * Top-right icons of the pause menu. The settings gear is always there (unless the menu itself is hidden with F3+Esc).
 * The book is for lobby and creative players, who have no limited inventory; dead spectators use the inventory key
 * instead. With both, the book keeps its corner and the gear sits to its left.
 * 暂停菜单右上角的图标。设置齿轮始终显示（F3+Esc 隐藏菜单时除外）。书本供没有受限背包的大厅与创造模式玩家使用；
 * 死亡旁观者改用背包键。两者同时出现时，书本保持在角落，齿轮放在它左侧。
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    private static final int EDGE = 8;
    private static final int ICON_GAP = 4;

    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sparkassist$addCornerIcons(CallbackInfo ci) {
        GameMenuScreen self = (GameMenuScreen) (Object) this;
        if (!self.shouldShowMenu()) {
            return;
        }
        int right = this.width - EDGE;

        if (GuidebookClientState.entryPoint(this.client) == GuidebookEntryPoint.PAUSE_MENU) {
            right -= GuidebookOpenButton.SIZE;
            GuidebookOpenButton book = new GuidebookOpenButton(right, EDGE, self);
            this.addDrawableChild(book);
            // The brass tooltip goes on top of the finished pause menu. Fabric resets screen events on every init and
            // resize, so this never registers twice.
            // 黄铜提示框绘制在已完成的暂停菜单之上。Fabric 每次初始化与缩放都会重置界面事件，因此不会重复注册。
            ScreenEvents.afterRender(this).register((screen, context, mouseX, mouseY, delta) ->
                    book.renderTooltip(context, this.textRenderer));
            right -= ICON_GAP;
        }

        SettingsGearButton gear = new SettingsGearButton(right - SettingsGearButton.SIZE, EDGE, self);
        this.addDrawableChild(gear);
        ScreenEvents.afterRender(this).register((screen, context, mouseX, mouseY, delta) ->
                gear.renderTooltip(context, this.textRenderer));
    }
}
