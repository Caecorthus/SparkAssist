package dev.caecorthus.sparkassist.client.charge;

import dev.caecorthus.sparkassist.client.charge.ChargeMeter.Reading;
import dev.caecorthus.sparkassist.client.charge.ChargeRingShape.Cell;
import dev.caecorthus.sparkassist.config.ChargeIndicatorStyle;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

/**
 * Draws one reading in one style. Everything is a plain fill: no texture, no text, nothing that needs a resource pack.
 * 以某一样式绘制一个读数。全部是纯色填充：没有贴图，没有文字，不依赖任何资源包。
 *
 * <p>The positions are fitted around what Wathe and the sibling mods already draw:
 * 各位置都是绕开 Wathe 与兄弟模组已有的绘制内容定下的：</p>
 * <ul>
 * <li>Wathe's knife, bat and swordfish glyph is 10x7 at (centre-5, centre+5). The open ring stops one row above it, the
 * small ring ends at centre+4, and the bar sits in the rows below it.
 * Wathe 的刀、球棒与剑鱼图标为 10x7，位于（中心-5，中心+5）。开口环止于它上方一行，小环止于中心+4，短条位于它下面的几行。</li>
 * <li>The nametag of a looked-at player starts near centre+15, so the bar keeps to rows 13 and 14.
 * 所看玩家的名牌从中心+15 附近开始，因此短条只占第 13、14 行。</li>
 * <li>Wathe's stamina icons and cooldown number occupy height-39 to height-27 above the hotbar, so the line sits at
 * height-26, two pixels above the selection frame.
 * Wathe 的体力图标与冷却数字占据快捷栏上方 height-39 到 height-27，因此细线位于 height-26，距选中框两像素。</li>
 * </ul>
 */
public final class ChargeIndicatorRenderer {
    /** Guidebook brass, the colour the gauge fills with. 图鉴黄铜色，仪表填充所用的颜色。 */
    private static final int BRASS = 0xFFC5A244;
    /** The gauge once a release counts. 松手有效后仪表的颜色。 */
    private static final int READY = 0xFFFFF3D0;
    private static final int TRACK = 0x8C000000;

    private static final List<Cell> OPEN_RING = ChargeRingShape.cells(15, 2, 100);
    private static final List<Cell> SMALL_RING = ChargeRingShape.cells(9, 2, 0);
    private static final int BAR_WIDTH = 24;
    private static final int BAR_OFFSET_Y = 13;
    private static final int LINE_WIDTH = 60;
    private static final int LINE_ABOVE_BOTTOM = 26;
    private static final int THICKNESS = 2;

    private ChargeIndicatorRenderer() {
    }

    public static void draw(DrawContext context, ChargeIndicatorStyle style, Reading reading) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        switch (style) {
            case RING_OPEN -> ring(context, OPEN_RING, width, height, reading);
            case RING -> ring(context, SMALL_RING, width, height, reading);
            case CROSSHAIR_BAR -> bar(context, width / 2 - BAR_WIDTH / 2, height / 2 + BAR_OFFSET_Y, BAR_WIDTH, reading);
            case HOTBAR_LINE -> bar(context, width / 2 - LINE_WIDTH / 2, height - LINE_ABOVE_BOTTOM, LINE_WIDTH, reading);
            case OFF -> {
            }
        }
    }

    private static void ring(DrawContext context, List<Cell> cells, int width, int height, Reading reading) {
        int lit = Math.round(reading.fill() * cells.size());
        int color = reading.ready() ? READY : BRASS;
        // Wathe centres its 3x3 crosshair on the exact middle of the window, which falls between two pixels when the
        // scaled size is even. The ring uses the same origin so the two stay concentric at every size.
        // Wathe 把 3x3 准星对准窗口的正中，缩放后尺寸为偶数时该点落在两个像素之间。环使用同一原点，使两者在任何尺寸下都同心。
        context.getMatrices().push();
        context.getMatrices().translate(width / 2.0F - 0.5F, height / 2.0F - 0.5F, 0.0F);
        for (int i = 0; i < cells.size(); i++) {
            Cell cell = cells.get(i);
            context.fill(cell.x(), cell.y(), cell.x() + 1, cell.y() + 1, i < lit ? color : TRACK);
        }
        context.getMatrices().pop();
    }

    private static void bar(DrawContext context, int left, int top, int length, Reading reading) {
        int lit = Math.round(reading.fill() * length);
        context.fill(left, top, left + length, top + THICKNESS, TRACK);
        if (lit > 0) {
            context.fill(left, top, left + lit, top + THICKNESS, reading.ready() ? READY : BRASS);
        }
    }
}
