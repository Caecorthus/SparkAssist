package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.screen.EventSoundOptionsScreen;
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
 * Adds the event-volume entry to the vanilla sound options list.
 * 将事件音量入口加入原版声音选项列表。
 */
@Mixin(SoundOptionsScreen.class)
public abstract class SoundOptionsScreenMixin extends GameOptionsScreen {
    private SoundOptionsScreenMixin(Screen parent, GameOptions gameOptions, Text title) {
        super(parent, gameOptions, title);
    }

    @Inject(method = "addOptions", at = @At("RETURN"))
    private void sparkassist$appendEventSoundOptionsButton(CallbackInfo ci) {
        this.body.addWidgetEntry(ButtonWidget.builder(
                        Text.translatable("button.sparkassist.event_sound_options"),
                        button -> this.client.setScreen(new EventSoundOptionsScreen(this))
                )
                .build(), null);
    }
}
