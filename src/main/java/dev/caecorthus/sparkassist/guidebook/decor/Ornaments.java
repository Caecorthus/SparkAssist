package dev.caecorthus.sparkassist.guidebook.decor;

import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.BRASS;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.BRASS_HI;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.BRASS_LO;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.COIN;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.EDGE;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.INNER_LINE;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.NAMEPLATE;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.WALLPAPER;
import static dev.caecorthus.sparkassist.guidebook.decor.DecorPalette.WALLPAPER_DOT;

/**
 * The fixed chrome ornaments of the decorated guide: brass corner plates, the second hairline inside the brass
 * ring, the recessed nameplate behind a band title, the reader's book clasps, the directory's luggage tag and
 * its damask wallpaper. Everything is integer geometry on a {@link PixelSink}, so the same code draws straight into
 * a {@code DrawContext} (the cheap pieces) or into a cached pixel layer (the wallpaper).
 * 指南固定的边框点缀：黄铜角板、黄铜环内的第二道细线、标题带文字下的凹铭牌、正文的书口扣、目录的行李牌与菱格墙纸。
 * 全部是 PixelSink 上的整数几何，同一份代码既能直接画进 DrawContext（便宜的几件），也能画进缓存的像素层（墙纸）。
 */
public final class Ornaments {
    /** Size of a corner plate; it rides on the bevel and the ring at inset 1. 角板边长，位于内缩 1 的斜面与环上。 */
    public static final int CORNER_PLATE = 6;
    /** Band nameplate height; it sits at band y + 5. 铭牌高度，位于标题带 y + 5。 */
    public static final int NAMEPLATE_HEIGHT = 12;
    /** Horizontal padding the nameplate adds around its text (3 left, 4 right). 铭牌在文字两侧的内距。 */
    public static final int NAMEPLATE_PADDING = 7;
    public static final int TAG_WIDTH = 13;
    public static final int TAG_HEIGHT = 7;
    /** The tag's string reaches 4 px left of the tag. 行李牌的绳子向左伸出 4 像素。 */
    public static final int TAG_STRING = 4;

    private Ornaments() {
    }

    // ================================================================ frames / 边框

    /** Four 6x6 brass plates with an engraved screw, one on each corner of a panel. 面板四角的黄铜角板。 */
    public static void cornerPlates(PixelSink s, int x, int y, int w, int h) {
        plate(s, x + 1, y + 1);
        plate(s, x + w - 1 - CORNER_PLATE, y + 1);
        plate(s, x + 1, y + h - 1 - CORNER_PLATE);
        plate(s, x + w - 1 - CORNER_PLATE, y + h - 1 - CORNER_PLATE);
    }

    private static void plate(PixelSink s, int px, int py) {
        s.fill(px, py, px + 6, py + 6, BRASS);
        s.fill(px, py, px + 6, py + 1, BRASS_HI);
        s.fill(px, py, px + 1, py + 6, BRASS_HI);
        s.fill(px, py + 5, px + 6, py + 6, BRASS_LO);
        s.fill(px + 5, py, px + 6, py + 6, BRASS_LO);
        s.fill(px + 2, py + 2, px + 4, py + 4, EDGE);
        s.set(px + 2, py + 2, COIN);
    }

    /** A fainter hairline one pixel inside the brass ring, making a double-ruled frame. 黄铜环内的第二道细线。 */
    public static void innerLine(PixelSink s, int x, int y, int w, int h) {
        roundedOutline(s, x + 4, y + 4, w - 8, h - 8, INNER_LINE);
    }

    /** Recessed plate behind a band title: dark fill, brass hairline, four rivets. Draw it before the text.
     * 标题带文字下的凹铭牌：暗底、黄铜细线、四颗铆钉。先于文字绘制。 */
    public static void nameplate(PixelSink s, int x, int y, int w) {
        if (w < 8) {
            return;
        }
        roundedFill(s, x, y, w, NAMEPLATE_HEIGHT, NAMEPLATE);
        roundedOutline(s, x, y, w, NAMEPLATE_HEIGHT, BRASS_LO);
        s.set(x + 1, y + 1, BRASS_HI);
        s.set(x + w - 2, y + 1, BRASS_HI);
        s.set(x + 1, y + NAMEPLATE_HEIGHT - 2, BRASS_HI);
        s.set(x + w - 2, y + NAMEPLATE_HEIGHT - 2, BRASS_HI);
    }

    /** Book clasp straddling a panel's left rim at centre y {@code cy}: 8 wide (x-2..x+6), 9 tall, strap hole in
     * the middle. 跨在面板左沿上的书口扣：宽 8（x-2 到 x+6）、高 9，中间是带孔。 */
    public static void clasp(PixelSink s, int x, int cy) {
        s.fill(x - 2, cy - 4, x + 6, cy + 5, EDGE);
        s.fill(x - 1, cy - 3, x + 5, cy + 4, BRASS);
        s.fill(x - 1, cy - 3, x + 5, cy - 2, BRASS_HI);
        s.fill(x - 1, cy + 3, x + 5, cy + 4, BRASS_LO);
        s.fill(x + 1, cy - 1, x + 3, cy + 2, EDGE);
        s.set(x + 1, cy - 1, BRASS_HI);
    }

    /** Brass luggage tag (13x7) lying on a band at (x, y), with a stripe in the faction colour and a short string
     * to the left. 躺在标题带上的黄铜行李牌（13×7），右侧一道阵营色条，左侧一根短绳。 */
    public static void luggageTag(PixelSink s, int x, int y, int accentRgb) {
        int accent = DecorPalette.opaque(accentRgb);
        roundedFill(s, x, y, TAG_WIDTH, TAG_HEIGHT, BRASS_LO);
        roundedOutline(s, x, y, TAG_WIDTH, TAG_HEIGHT, EDGE);
        s.fill(x + 1, y + 1, x + 12, y + 2, BRASS);
        s.set(x + 2, y + 3, EDGE);
        s.fill(x + 9, y + 1, x + 12, y + 6, DecorPalette.mix(accent, 0xFF000000, 0.2));
        s.set(x + 9, y + 1, DecorPalette.mix(accent, 0xFFFFFFFF, 0.2));
        Dither.line(s, x + 2, y + 3, x - TAG_STRING, y + 1, BRASS_LO);
        s.set(x - TAG_STRING, y + 1, BRASS);
    }

    // ================================================================ directory background / 目录背景

    /** Damask wallpaper over a dark body: an 8 px diagonal lattice with a brighter dot at every crossing. The
     * phase follows the absolute coordinates so adjoining tiles continue each other.
     * 暗底上的菱格墙纸：8 像素斜格，交点亮一级。相位按绝对坐标计算，相邻区域可以接续。 */
    public static void wallpaper(PixelSink s, int x, int y, int w, int h) {
        for (int yy = y; yy < y + h; yy++) {
            for (int xx = x; xx < x + w; xx++) {
                int u = (xx + yy) & 7;
                int v = (xx - yy) & 7;
                if (u == 0 && v == 0) {
                    s.set(xx, yy, WALLPAPER_DOT);
                } else if (u == 0 || v == 0) {
                    s.set(xx, yy, WALLPAPER);
                }
            }
        }
    }

    // ================================================================ helpers / 工具

    /** Rectangle with the four corner pixels left out, like {@code ExpressPaint.roundedFill}. 四角留空的矩形。 */
    public static void roundedFill(PixelSink s, int x, int y, int w, int h, int col) {
        s.fill(x + 1, y, x + w - 1, y + 1, col);
        s.fill(x, y + 1, x + w, y + h - 1, col);
        s.fill(x + 1, y + h - 1, x + w - 1, y + h, col);
    }

    public static void roundedOutline(PixelSink s, int x, int y, int w, int h, int col) {
        s.fill(x + 1, y, x + w - 1, y + 1, col);
        s.fill(x + 1, y + h - 1, x + w - 1, y + h, col);
        s.fill(x, y + 1, x + 1, y + h - 1, col);
        s.fill(x + w - 1, y + 1, x + w, y + h - 1, col);
    }
}
