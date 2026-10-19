package dev.caecorthus.sparkassist.client.settings.screen;

import dev.caecorthus.sparkassist.SparkAssist;
import dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPaint;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The pause-menu gear that opens the SparkDLC settings: the guide book's 22 px mahogany button with a brass gear, and
 * the same brass tooltip to its left.
 * 暂停菜单中打开 SparkDLC 设置的齿轮：与指南书本相同的 22 像素桃花心木按钮配黄铜齿轮，左侧显示同款黄铜提示框。
 */
public final class SettingsGearButton extends ButtonWidget {
    public static final int SIZE = ExpressPaint.ICON_BUTTON_SIZE;
    private static final Identifier GEAR_ICON = SparkAssist.id("textures/gui/settings/gear.png");

    public SettingsGearButton(int x, int y, Screen parent) {
        super(x, y, SIZE, SIZE, Text.translatable("screen.sparkassist.settings.title"),
                button -> MinecraftClient.getInstance().setScreen(new SparkDlcSettingsScreen(parent)),
                DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        ExpressPaint.iconButton(context, this.getX(), this.getY(), this.isHovered(), GEAR_ICON);
    }

    /** Brass tooltip left of the button, as the guide book's; call after the screen rendered. 同指南书本的黄铜提示框。 */
    public void renderTooltip(DrawContext context, TextRenderer textRenderer) {
        boolean keyboardFocus = this.isFocused() && MinecraftClient.getInstance().getNavigationType().isKeyboard();
        if (!this.visible || !(this.isHovered() || keyboardFocus)) {
            return;
        }
        List<Text> lines = List.of(this.getMessage());
        int width = ExpressPaint.tooltipWidth(textRenderer, lines);
        int x = Math.max(4, this.getX() - 4 - 4 - width);
        int y = this.getY() + (SIZE - ExpressPaint.tooltipHeight(lines.size())) / 2;
        ExpressPaint.tooltip(context, textRenderer, lines, x, y, width);
    }
}
