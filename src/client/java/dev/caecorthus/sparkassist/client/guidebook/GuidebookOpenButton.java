package dev.caecorthus.sparkassist.client.guidebook;

import dev.caecorthus.sparkassist.SparkAssist;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Compact book entry used by both the limited inventory and pause menu.
 * 受限背包和暂停菜单共用的小型书本入口。
 */
public final class GuidebookOpenButton extends ButtonWidget {
    public static final int SIZE = 22;
    private static final Identifier ICON = SparkAssist.id("textures/gui/guidebook/open_button.png");

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
        this.setTooltip(Tooltip.of(Text.translatable("button.sparkassist.guidebook.open")));
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int background = this.isHovered() ? 0xE05A342D : 0xC035211E;
        int border = this.isHovered() ? 0xFFF0C56A : 0xFF9A7046;
        context.fill(this.getX(), this.getY(), this.getX() + SIZE, this.getY() + SIZE, background);
        context.fill(this.getX(), this.getY(), this.getX() + SIZE, this.getY() + 1, border);
        context.fill(this.getX(), this.getY() + SIZE - 1, this.getX() + SIZE, this.getY() + SIZE, border);
        context.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + SIZE, border);
        context.fill(this.getX() + SIZE - 1, this.getY(), this.getX() + SIZE, this.getY() + SIZE, border);
        context.drawTexture(ICON, this.getX() + 3, this.getY() + 3, 0, 0, 16, 16, 16, 16);
    }
}
