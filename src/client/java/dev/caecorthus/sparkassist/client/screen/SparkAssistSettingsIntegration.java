package dev.caecorthus.sparkassist.client.screen;

import dev.caecorthus.sparkassist.client.SparkAssistClient;
import dev.caecorthus.sparkassist.client.input.InstinctKeyController;
import dev.caecorthus.sparkassist.config.SparkAssistConfig.InstinctKeyMode;
import java.util.List;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.SoundOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

public final class SparkAssistSettingsIntegration {
    private static final int BUTTON_WIDTH = 170;
    private static final int BUTTON_HEIGHT = 20;

    private SparkAssistSettingsIntegration() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AccessibilityOptionsScreen) {
                addInstinctModeButton(screen, scaledWidth);
            } else if (screen instanceof SoundOptionsScreen) {
                addEventVolumeButton(screen, scaledWidth);
            }
        });
    }

    private static void addInstinctModeButton(Screen screen, int scaledWidth) {
        List<ClickableWidget> buttons = Screens.getButtons(screen);
        int x = Math.max(6, scaledWidth - BUTTON_WIDTH - 6);
        int y = 6;
        CyclingButtonWidget<InstinctKeyMode> button = CyclingButtonWidget
                .<InstinctKeyMode>builder(mode -> Text.translatable(mode.translationKey()))
                .values(List.of(InstinctKeyMode.values()))
                .initially(SparkAssistClient.configManager().config().instinctKeyMode())
                .build(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Text.translatable("option.sparkassist.instinct_key_mode"),
                        (widget, mode) -> {
                            SparkAssistClient.configManager().config().setInstinctKeyMode(mode);
                            SparkAssistClient.configManager().save();
                            InstinctKeyController.reset();
                        });
        buttons.add(button);
    }

    private static void addEventVolumeButton(Screen screen, int scaledWidth) {
        List<ClickableWidget> buttons = Screens.getButtons(screen);
        int x = Math.max(6, scaledWidth - BUTTON_WIDTH - 6);
        int y = 6;
        buttons.add(ButtonWidget.builder(
                        Text.translatable("button.sparkassist.event_sound_options"),
                        button -> Screens.getClient(screen).setScreen(new EventSoundOptionsScreen(screen))
                )
                .dimensions(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }
}
