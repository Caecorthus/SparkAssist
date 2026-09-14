package dev.caecorthus.sparkassist.guidebook;

/**
 * Keeps the reader centered without using the right-hand HUD space.
 * 正文保持屏幕居中，不占用右侧 HUD 空间。
 */
public record GuidebookLayout(Region directory, Region article, boolean compact) {
    public static GuidebookLayout compute(int width, int height) {
        int margin = 8;
        int maxSidebar = (width - 144) / 2 - margin - 8;
        boolean compact = maxSidebar < 64;
        int sidebarWidth = Math.min(176, Math.max(64, width / 4 - 8));
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
    }
}
