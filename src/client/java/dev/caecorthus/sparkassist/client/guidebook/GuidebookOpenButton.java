package dev.caecorthus.sparkassist.client.guidebook;

import dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPaint;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Compact book entry used by both the limited inventory and pause menu.
 * 受限背包和暂停菜单共用的小型书本入口。
 */
public final class GuidebookOpenButton extends ButtonWidget {
    public static final int SIZE = 22;

    public GuidebookOpenButton(int x, int y, Screen parent) {
        super(
                x,
                y,
                SIZE,
                SIZE,
                Text.translatable("button.sparkassist.guidebook.open"),
                button -> MinecraftClient.getInstance().setScreen(new GuidebookScreen(parent)),
                DEFAULT_NARRATION_SUPPLIER
        );
        // No vanilla Tooltip: it would be purple outside a round. The brass one is drawn by renderTooltip; the message
        // is the same text, so narration still announces it.
        // 不使用原版 Tooltip（对局外会是紫色）；黄铜提示框由 renderTooltip 绘制。按钮文字与提示相同，旁白仍会朗读。
    }

    /** The 22 px mahogany icon button (panel recipe, coin ring on hover, no raise); the hit area equals the visual.
     * 22 像素桃花心木图标按钮（面板配方，悬停时金币色环，不抬升）；热区与外观一致。 */
    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        ExpressPaint.iconButton(context, this.getX(), this.getY(), this.isHovered());
    }

    /**
     * The brass tag (ExpressPaint.tooltip, as every guide tooltip) left of the button: its frame ends 4 px before the
     * button and it is centred on it. Shown on hover or keyboard focus, like vanilla; call it after the screen has
     * rendered so nothing draws over it.
     * 按钮左侧的黄铜提示框（与指南其他提示框相同的 ExpressPaint.tooltip）：边框止于按钮左侧 4 像素，并与按钮垂直居中。
     * 与原版一样在悬停或键盘聚焦时显示；应在界面绘制完成后调用，避免被其他内容覆盖。
     */
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
