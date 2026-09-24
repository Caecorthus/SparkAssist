package dev.caecorthus.sparkassist.guidebook;

import java.util.Comparator;
import java.util.List;

/**
 * Keeps the reader centered without using the right-hand HUD space.
 * 正文保持屏幕居中，不占用右侧 HUD 空间。
 */
public record GuidebookLayout(Region directory, Region article, boolean compact) {
    public static GuidebookLayout compute(int width, int height) {
        int margin = Math.min(8, Math.max(0, Math.min(width, height) / 4));
        int maxSidebar = (width - 144) / 2 - margin - 8;
        boolean compact = maxSidebar < 64;
        int sidebarWidth = Math.min(Math.max(0, width - margin * 2),
                Math.min(176, Math.max(64, width / 4 - 8)));
        if (!compact) {
            sidebarWidth = Math.min(sidebarWidth, maxSidebar);
        }
        int articleWidth = compact ? width - margin * 2
                : Math.min(440, width - 2 * (margin + sidebarWidth + 8));
        int articleHeight = Math.min(520, height - margin * 2);
        return new GuidebookLayout(
                new Region(margin, margin, sidebarWidth, height - margin * 2),
                new Region((width - articleWidth) / 2, (height - articleHeight) / 2,
                        articleWidth, articleHeight),
                compact
        );
    }

    /** Keep the persistent directory in the largest clear strip beside the shop.
     * 常驻目录仅使用左侧未被商品及价格占用的区域。 */
    public static GuidebookLayout computeEmbedded(int width, int height, List<Region> occupied) {
        GuidebookLayout base = compute(width, height);
        Region nav = base.directory();
        List<Region> obstacles = occupied.stream()
                .filter(region -> nav.intersects(region))
                .sorted(Comparator.comparingInt(Region::y)).toList();
        int top = nav.y();
        Region largest = new Region(nav.x(), top, nav.width(), 0);
        for (Region obstacle : obstacles) {
            int gap = Math.max(0, obstacle.y() - top);
            if (gap > largest.height()) {
                largest = new Region(nav.x(), top, nav.width(), gap);
            }
            top = Math.min(nav.bottom(), Math.max(top, obstacle.bottom()));
        }
        if (nav.bottom() - top > largest.height()) {
            largest = new Region(nav.x(), top, nav.width(), nav.bottom() - top);
        }
        return new GuidebookLayout(largest, base.article(), base.compact());
    }

    public record Region(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }

        public boolean intersects(Region other) {
            return width > 0 && height > 0 && other.width > 0 && other.height > 0
                    && x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
        }
    }
}
