package dev.caecorthus.sparkassist.client.charge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The pixels of a charge ring, in the order they light up. The ring is drawn pixel by pixel rather than as a smooth
 * arc so it sits in the same pixel grid as Wathe's crosshair and glyphs, and filling it is then just "the first N
 * cells".
 * 蓄力环的像素，按点亮顺序排列。环是逐像素绘制的而不是平滑圆弧，这样它与 Wathe 的准星和图标处在同一像素网格里，而填充就只是
 * 「前 N 个像素」。
 */
public final class ChargeRingShape {
    /** A pixel of the ring, relative to the centre pixel. 环上的一个像素，相对于中心像素。 */
    public record Cell(int x, int y) {
    }

    private ChargeRingShape() {
    }

    /**
     * Cells of a ring centred on the pixel (0, 0), clockwise.
     * 以像素 (0, 0) 为中心的环的像素，顺时针排列。
     *
     * @param diameter   odd outer diameter in pixels / 外径，奇数像素
     * @param thickness  ring thickness in pixels / 环的粗细
     * @param gapDegrees arc left open, centred straight down; 0 for a full ring. An open ring starts at the left edge
     *                   of the gap and runs over the top; a full ring starts at the top. / 留空的弧度，以正下方为中心；
     *                   0 为整环。开口环从缺口左缘起、经顶部绕行；整环从顶部起。
     */
    public static List<Cell> cells(int diameter, int thickness, int gapDegrees) {
        if (diameter < 3 || diameter % 2 == 0 || thickness < 1 || gapDegrees < 0 || gapDegrees >= 360) {
            throw new IllegalArgumentException("ring " + diameter + "/" + thickness + "/" + gapDegrees);
        }
        double outer = diameter / 2.0D - 0.25D;
        double inner = diameter / 2.0D - thickness - 0.25D;
        double start = gapDegrees == 0 ? 0.0D : 180.0D + gapDegrees / 2.0D;
        int reach = diameter / 2;
        List<Cell> cells = new ArrayList<>();
        for (int y = -reach; y <= reach; y++) {
            for (int x = -reach; x <= reach; x++) {
                double radius = Math.hypot(x, y);
                if (radius > outer || radius <= inner) {
                    continue;
                }
                if (gapDegrees > 0 && Math.abs(clockwiseFromTop(x, y) - 180.0D) < gapDegrees / 2.0D) {
                    continue;
                }
                cells.add(new Cell(x, y));
            }
        }
        // Ties (two cells on one ray) go inner first, so the ring grows as a clean front.
        // 同一射线上的两个像素先内后外，使环以整齐的前沿推进。
        cells.sort(Comparator
                .comparingDouble((Cell cell) -> Math.floorMod(Math.round(
                        (clockwiseFromTop(cell.x(), cell.y()) - start) * 1000.0D), 360_000L))
                .thenComparingDouble(cell -> Math.hypot(cell.x(), cell.y())));
        return List.copyOf(cells);
    }

    /** Angle of a cell in degrees, 0 at the top and growing clockwise (screen y points down). 像素的角度，顶部为 0，顺时针增大（屏幕 y 轴向下）。 */
    private static double clockwiseFromTop(int x, int y) {
        double degrees = Math.toDegrees(Math.atan2(x, -y));
        return degrees < 0.0D ? degrees + 360.0D : degrees;
    }
}
