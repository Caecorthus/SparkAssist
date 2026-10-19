package dev.caecorthus.sparkassist.client.settings.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** A flat footer button matching the rows instead of the stone vanilla button. 与设置行一致的扁平底部按钮。 */
final class SettingsFlatButton extends ButtonWidget {
    private static final int MIN_WIDTH = 56;
    private static final int PAD_X = 10;

    SettingsFlatButton(Text message, PressAction onPress) {
        super(0, 0, Math.max(MIN_WIDTH, MinecraftClient.getInstance().textRenderer.getWidth(message) + 2 * PAD_X),
                SettingsLayout.FOOTER_HEIGHT, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        boolean hot = this.active && (isHovered()
                || (isFocused() && MinecraftClient.getInstance().getNavigationType().isKeyboard()));
        int x = getX();
        int y = getY();
        context.fill(x, y, x + width, y + height, SettingsPalette.BUTTON_EDGE);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, !this.active ? SettingsPalette.BUTTON_INACTIVE
                : hot ? SettingsPalette.BUTTON_HOVER : SettingsPalette.BUTTON);
        if (hot && isFocused() && MinecraftClient.getInstance().getNavigationType().isKeyboard()) {
            context.drawBorder(x, y, width, height, SettingsPalette.FOCUS);
        }
        int textColor = this.active ? SettingsPalette.TEXT : SettingsPalette.TEXT_DISABLED;
        context.drawCenteredTextWithShadow(font, getMessage(), x + width / 2, y + (height - font.fontHeight) / 2 + 1,
                textColor);
    }
}
