package dev.caecorthus.sparkassist.client.settings.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** A page tab: plain text, underlined in brass when selected. 页签：纯文字，选中时下方有黄铜色下划线。 */
final class SettingsTabWidget extends ClickableWidget {
    static final int PAD_X = 6;

    private final boolean selected;
    private final Runnable onSelect;

    SettingsTabWidget(int x, int y, Text title, boolean selected, Runnable onSelect) {
        super(x, y, MinecraftClient.getInstance().textRenderer.getWidth(title) + 2 * PAD_X,
                SettingsLayout.TAB_HEIGHT, title);
        this.selected = selected;
        this.onSelect = onSelect;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!selected) {
            onSelect.run();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            this.playDownSound(MinecraftClient.getInstance().getSoundManager());
            onClick(0, 0);
            return true;
        }
        return false;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        boolean hot = isHovered() || (isFocused() && MinecraftClient.getInstance().getNavigationType().isKeyboard());
        int color = selected || hot ? SettingsPalette.TEXT : SettingsPalette.TEXT_MUTED;
        context.drawText(font, getMessage(), getX() + PAD_X, getY() + 4, color, false);
        int bottom = getY() + height;
        if (selected) {
            context.fill(getX() + 2, bottom - 2, getX() + width - 2, bottom, SettingsPalette.ACCENT);
        } else if (hot) {
            context.fill(getX() + 2, bottom - 1, getX() + width - 2, bottom, SettingsPalette.TEXT_FAINT);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, Text.translatable(selected
                ? "screen.sparkassist.settings.narration.tab_selected"
                : "screen.sparkassist.settings.narration.tab", getMessage()));
    }
}
