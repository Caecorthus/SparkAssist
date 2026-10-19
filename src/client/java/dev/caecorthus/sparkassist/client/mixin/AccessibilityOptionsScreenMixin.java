package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.settings.screen.SparkDlcSettingsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts the "SparkDLC Settings..." button on the first row of vanilla Accessibility, where the guide decoration and
 * scope options used to sit. First, because the list is over twenty rows long.
 * 在原版辅助功能列表的第一行放「SparkDLC 设置…」按钮（指南点缀与瞄准镜选项原来就在这里）。放第一行是因为这个列表有二十多行。
 */
@Mixin(AccessibilityOptionsScreen.class)
public abstract class AccessibilityOptionsScreenMixin extends GameOptionsScreen {
    private static final int FULL_ROW_WIDTH = 310;

    private AccessibilityOptionsScreenMixin(Screen parent, GameOptions gameOptions, Text title) {
        super(parent, gameOptions, title);
    }

    @Inject(method = "addOptions", at = @At("HEAD"))
    private void sparkassist$prependSettingsButton(CallbackInfo ci) {
        if (this.body == null) {
            return;
        }
        this.body.addWidgetEntry(ButtonWidget.builder(
                        Text.translatable("button.sparkassist.settings"),
                        button -> this.client.setScreen(new SparkDlcSettingsScreen(this)))
                .width(FULL_ROW_WIDTH)
                .build(), null);
    }
}
