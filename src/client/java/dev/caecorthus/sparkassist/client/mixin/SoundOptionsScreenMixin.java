package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.settings.SparkDlcSettingsPages;
import dev.caecorthus.sparkassist.client.settings.screen.SparkDlcSettingsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.screen.option.SoundOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps a shortcut in vanilla Music &amp; Sounds, where players look for volume: it opens the SparkDLC settings on the
 * audio page.
 * 在原版「音乐和声音」里保留一个快捷入口（玩家习惯在这里找音量）：直接打开 SparkDLC 设置的音频页。
 */
@Mixin(SoundOptionsScreen.class)
public abstract class SoundOptionsScreenMixin extends GameOptionsScreen {
    private SoundOptionsScreenMixin(Screen parent, GameOptions gameOptions, Text title) {
        super(parent, gameOptions, title);
    }

    @Inject(method = "addOptions", at = @At("RETURN"))
    private void sparkassist$appendSettingsShortcut(CallbackInfo ci) {
        this.body.addWidgetEntry(ButtonWidget.builder(
                        Text.translatable("button.sparkassist.event_sound_options"),
                        button -> this.client.setScreen(new SparkDlcSettingsScreen(this, SparkDlcSettingsPages.AUDIO))
                )
                .build(), null);
    }
}
