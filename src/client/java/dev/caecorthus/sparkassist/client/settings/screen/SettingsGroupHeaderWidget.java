package dev.caecorthus.sparkassist.client.settings.screen;

import dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPaint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * A section title in the option list that folds and unfolds its rows; a folded one shows how many rows it hides.
 * 选项列表中的分节标题，点击可折叠或展开其下各行；折叠时显示隐藏了几行。
 */
final class SettingsGroupHeaderWidget extends ClickableWidget {
    static final int HEIGHT = 14;

    private final String key;
    private final boolean collapsed;
    private final int rowCount;
    private final Runnable onToggle;
    private final SettingsRowWidget.Host host;

    SettingsGroupHeaderWidget(int x, int width, Text title, String key, boolean collapsed, int rowCount,
                              Runnable onToggle, SettingsRowWidget.Host host) {
        super(x, 0, width, HEIGHT, title);
        this.key = key;
        this.collapsed = collapsed;
        this.rowCount = rowCount;
        this.onToggle = onToggle;
        this.host = host;
    }

    String key() {
        return key;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY) && host.inListViewport(mouseX, mouseY);
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return isMouseOver(mouseX, mouseY);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onToggle.run();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean toggle = keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_SPACE
                || (keyCode == GLFW.GLFW_KEY_LEFT && !collapsed)
                || (keyCode == GLFW.GLFW_KEY_RIGHT && collapsed);
        if (!toggle) {
            return false;
        }
        this.playDownSound(MinecraftClient.getInstance().getSoundManager());
        onToggle.run();
        return true;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        boolean keyboardFocus = isFocused() && MinecraftClient.getInstance().getNavigationType().isKeyboard();
        boolean hot = isHovered() || keyboardFocus;
        int color = hot ? SettingsPalette.TEXT : SettingsPalette.TEXT_MUTED;
        int textY = getY() + 4;
        ExpressPaint.chevron(context, getX() + 2, textY + 1, !collapsed, color);
        int textX = getX() + 11;
        context.drawText(font, getMessage(), textX, textY, color, false);
        if (collapsed) {
            Text count = Text.translatable("screen.sparkassist.settings.group.hidden_rows", rowCount);
            context.drawText(font, count, textX + font.getWidth(getMessage()) + 6, textY, SettingsPalette.TEXT_FAINT,
                    false);
        }
        if (keyboardFocus) {
            context.fill(getX(), getY() + height - 1, getX() + width, getY() + height, SettingsPalette.FOCUS);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, Text.translatable(collapsed
                ? "screen.sparkassist.settings.narration.group_collapsed"
                : "screen.sparkassist.settings.narration.group_expanded", getMessage(), rowCount));
    }
}
