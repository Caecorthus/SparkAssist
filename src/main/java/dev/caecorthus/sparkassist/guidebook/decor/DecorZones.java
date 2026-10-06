package dev.caecorthus.sparkassist.guidebook.decor;

import dev.caecorthus.sparkassist.guidebook.GuidebookLayout.Region;
import java.util.ArrayList;
import java.util.List;

/**
 * The no-go rectangles of the decorated guide, derived from the same geometry the painters use: band text, the
 * close and search boxes, the text column and scrollbar gutter, the directory rows, the HUD band, panel interiors
 * and the small ornaments themselves (seal, clasps, charm, tab). Steam and foliage paint through a
 * {@link ZonedSink} built from these lists, so the guarantee is per pixel.
 * 点缀的禁区矩形，由绘制器使用的同一套几何推出：标题带文字、关闭与搜索热区、文字列与滚动条槽、目录行区、HUD 带、
 * 面板内部，以及火漆印、书口扣、吊坠、折角这些小件本身。蒸汽与枝条经 ZonedSink 落笔，保证逐像素成立。
 */
public final class DecorZones {
    /** Wathe's top HUD row. Wathe 顶部 HUD 带。 */
    public static final int HUD_HEIGHT = 16;

    private DecorZones() {
    }

    public static Region hud(int screenWidth) {
        return new Region(0, 0, screenWidth, HUD_HEIGHT);
    }

    /** Everything inside a panel's outermost pixel. 面板最外一圈像素以内的全部区域。 */
    public static Region panelInterior(Region panel) {
        return new Region(panel.x() + 1, panel.y() + 1, Math.max(0, panel.width() - 2), Math.max(0, panel.height() - 2));
    }

    /** A panel plus {@code margin} on every side. 面板连同四周 margin。 */
    public static Region padded(Region panel, int margin) {
        return new Region(panel.x() - margin, panel.y() - margin, panel.width() + 2 * margin,
                panel.height() + 2 * margin);
    }

    /**
     * Reader no-go: crumb/running-head box, source box, close box, the text column over the viewport, the right
     * paper margin with the scrollbar gutter, the wax seal corner and the two clasps.
     * 正文禁区：路径 / 页眉框、来源框、关闭热区、视口内的文字列、含滚动条槽的纸页右边距、火漆印角与两枚书口扣。
     *
     * @param bandTextWidth width of the crumb or running head drawn at x + 7 / 画在 x + 7 的路径或页眉宽度
     * @param sourceWidth   width of the source label (0 for none) / 来源文字宽度（无则 0）
     */
    public static List<Region> reader(Region reader, int textLeft, int textRight, int sheetTop, int sheetBottom,
                                      int sheetRight, int bandTextWidth, int sourceWidth) {
        List<Region> zones = new ArrayList<>();
        int x = reader.x();
        int y = reader.y();
        zones.add(new Region(x + 4, y + 5, bandTextWidth + 8, 12));
        if (sourceWidth > 0) {
            zones.add(new Region(reader.right() - 22 - sourceWidth - 2, y + 5, sourceWidth + 4, 12));
        }
        zones.add(new Region(reader.right() - 18, y + 4, 14, 14));
        zones.add(new Region(textLeft - 1, sheetTop, textRight - textLeft + 2, sheetBottom - sheetTop));
        zones.add(new Region(textRight, sheetTop, sheetRight - textRight, sheetBottom - sheetTop));
        zones.add(sealBox(reader));
        int h = reader.height();
        zones.add(claspBox(reader, y + Math.round(h / 3f)));
        zones.add(claspBox(reader, y + Math.round(h * 2 / 3f)));
        return zones;
    }

    /** The wax seal's corner at the reader's bottom-left. 正文左下角的火漆印区域。 */
    public static Region sealBox(Region reader) {
        return new Region(reader.x() - 1, reader.bottom() - 19, 20, 20);
    }

    public static Region claspBox(Region reader, int cy) {
        return new Region(reader.x() - 3, cy - 5, 10, 11);
    }

    /**
     * Directory no-go: the title's nameplate box, the search toggle box, and the whole row area below the band.
     * 目录禁区：标题铭牌框、搜索热区，以及标题带以下的整个行区。
     */
    public static List<Region> directory(Region nav, int titleWidth, int searchToggleX) {
        List<Region> zones = new ArrayList<>();
        zones.add(new Region(nav.x() + 4, nav.y() + 5, titleWidth + 8, 12));
        zones.add(new Region(searchToggleX, nav.y() + 4, 14, 14));
        zones.add(new Region(nav.x() + 3, nav.y() + 21, Math.max(0, nav.width() - 6), Math.max(0, nav.height() - 24)));
        return zones;
    }
}
