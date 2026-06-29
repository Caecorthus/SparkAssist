package dev.caecorthus.sparkassist.client.screen;

import dev.caecorthus.sparkassist.client.SparkAssistClient;
import dev.caecorthus.sparkassist.config.SparkAssistConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public final class EventSoundOptionsScreen extends Screen {
    private static final Text TITLE = Text.translatable("screen.sparkassist.event_sound_options");
    private static final Text DONE = Text.translatable("gui.done");
    private final Screen parent;

    public EventSoundOptionsScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(310, this.width - 40);
        int x = (this.width - contentWidth) / 2;
        int y = this.height / 2 - 22;
        this.addDrawableChild(new EventVolumeSlider(x, y, contentWidth, 20));
        this.addDrawableChild(ButtonWidget.builder(DONE, button -> this.close())
                .dimensions(this.width / 2 - 100, this.height - 28, 200, 20)
                .build());
    }

    @Override
    public void close() {
        SparkAssistClient.configManager().save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    private static final class EventVolumeSlider extends SliderWidget {
        private EventVolumeSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Text.empty(),
                    SparkAssistClient.configManager().config().eventSoundVolume());
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            int percent = (int) Math.round(this.value * 100.0D);
            this.setMessage(Text.translatable("option.sparkassist.event_sound_volume.value", percent));
        }

        @Override
        protected void applyValue() {
            SparkAssistConfig config = SparkAssistClient.configManager().config();
            config.setEventSoundVolume(this.value);
            SparkAssistClient.configManager().save();
        }
    }
}
