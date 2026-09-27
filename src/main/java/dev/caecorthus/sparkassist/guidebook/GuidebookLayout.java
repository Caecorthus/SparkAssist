package dev.caecorthus.sparkassist.guidebook;

import java.util.Comparator;
import java.util.List;

/**
 * Directory column at the left with the reader beside it as a book spread; the right-hand HUD column stays free.
 * 目录位于左侧，正文紧贴目录右侧如同翻开的书页；右侧 HUD 区域保持空闲。
 */
public record GuidebookLayout(Region directory, Region article, boolean compact) {
    /** Gap between a panel and any obstacle (shop, widget, HUD band, strip). 面板与任何障碍物之间的间距。 */
    public static final int CLEARANCE = 4;
    /** Shared top line of the directory and the owner card: HUD band (16) + clearance. 目录与信息卡共用的顶线。 */
    public static final int TOP_LINE = 20;
    /** Smallest persistent directory that still shows the band, one root row and the footer. 常驻目录的最小高度。 */
    public static final int MIN_DIRECTORY_HEIGHT = 81;
    /** The collapsed/folded directory button and the pause-menu button are 22 px squares. 收起后的书本按钮边长。 */
    public static final int BUTTON_SIZE = 22;
    private static final int READER_WIDTH = 284;
    private static final int SPREAD_GAP = 8;
    private static final int HUD_BAND_HEIGHT = 16;
    // Wathe 1.5.6 LimitedHandledScreen draws its 176x32 hotbar strip centred on the screen.
    // Wathe 1.5.6 的受限背包把 176x32 的快捷栏条居中绘制。
    private static final int STRIP_WIDTH = 176;
    private static final int STRIP_HEIGHT = 32;

    public static GuidebookLayout compute(int width, int height) {
        int margin = Math.min(8, Math.max(0, Math.min(width, height) / 4));
        int maxSidebar = (width - 144) / 2 - margin - 8;
        boolean compact = maxSidebar < 64;
        int sidebarWidth = Math.min(Math.max(0, width - margin * 2),
                Math.min(176, Math.max(64, width / 4 - 8)));
        if (!compact) {
            sidebarWidth = Math.min(sidebarWidth, maxSidebar);
        }
        int articleX = compact ? margin : margin + sidebarWidth + SPREAD_GAP;
        int articleWidth = compact ? width - margin * 2 : Math.min(READER_WIDTH, width - articleX - margin);
        return new GuidebookLayout(
                new Region(margin, margin, sidebarWidth, height - margin * 2),
                new Region(articleX, margin, articleWidth, height - margin * 2),
                compact
        );
    }

    /** Keep the persistent directory in the largest clear strip, 4 px away from every obstacle.
     * 常驻目录仅使用与所有障碍物保持 4 像素间距的最大空白区域。 */
    public static GuidebookLayout computeEmbedded(int width, int height, List<Region> occupied) {
        GuidebookLayout base = compute(width, height);
        Region nav = base.directory();
        Region probe = new Region(nav.x(), nav.y() - CLEARANCE, nav.width(), nav.height() + CLEARANCE * 2);
        List<Region> obstacles = occupied.stream()
                .filter(probe::intersects)
                .sorted(Comparator.comparingInt(Region::y)).toList();
        int top = nav.y();
        Region largest = new Region(nav.x(), top, nav.width(), 0);
        for (Region obstacle : obstacles) {
            int gap = Math.max(0, obstacle.y() - CLEARANCE - top);
            if (gap > largest.height()) {
                largest = new Region(nav.x(), top, nav.width(), gap);
            }
            top = Math.min(nav.bottom(), Math.max(top, obstacle.bottom() + CLEARANCE));
        }
        if (nav.bottom() - top > largest.height()) {
            largest = new Region(nav.x(), top, nav.width(), nav.bottom() - top);
        }
        return new GuidebookLayout(largest, base.article(), base.compact());
    }

    /** Footprint of a clickable widget: 16x16 target heads draw a 30x30 frame (+7), then every widget gets +2.
     * 可点击控件的占位：16x16 头像控件外绘 30x30 边框（+7），所有控件再外扩 2 像素。 */
    public static Region widgetObstacle(int x, int y, int width, int height) {
        if (width == 16 && height == 16) {
            return new Region(x - 9, y - 9, 34, 34);
        }
        return new Region(x - 2, y - 2, width + 4, height + 4);
    }

    /** Wathe's top HUD row (money, timer, first task line) ends at y = 15. Wathe 顶部 HUD 行止于 y = 15。 */
    public static Region hudBand(int width) {
        return new Region(0, 0, width, HUD_BAND_HEIGHT);
    }

    /** Content-fit height inside a gap: the top stays fixed, only the bottom follows the content.
     * 在空白区内按内容取高：顶边固定，只有底边随内容变化。 */
    public static Region contentFit(Region gap, int need, int min) {
        return new Region(gap.x(), gap.y(), gap.width(), Math.min(gap.height(), Math.max(min, need)));
    }

    /** Modal directory keeps x, top and width; only the bottom extends to height - 8, so rows never move.
     * 模态目录保持横坐标、顶边与宽度，只把底边延伸到 height - 8，行不会在光标下移动。 */
    public static Region modalDirectory(Region embedded, int height) {
        return new Region(embedded.x(), embedded.y(), embedded.width(), Math.max(0, height - 8 - embedded.y()));
    }

    /** Embedded modal reader hangs from the shared HUD top line. 嵌入式模态正文从共享的 HUD 顶线垂下。 */
    public static Region embeddedReader(Region article, int height) {
        return new Region(article.x(), TOP_LINE, article.width(), Math.max(0, height - 8 - TOP_LINE));
    }

    /** True when the persistent directory must shrink to its book button (too short, too narrow, or it would
     * cross the hotbar strip). 常驻目录过矮、过窄或会越过快捷栏条时，需收起为书本按钮。 */
    public static boolean collapsed(Region gap, int width) {
        return gap.height() < MIN_DIRECTORY_HEIGHT || gap.width() < 48
                || gap.right() + CLEARANCE > (width - STRIP_WIDTH) / 2;
    }

    /** First 22 px slot in the directory column, scanning down from the top line, that keeps the same
     * {@link #CLEARANCE} from every obstacle and the hotbar strip as the directory and the owner card, so the button
     * shares their top line; falls back to the top line so the button always exists.
     * 在目录列中自顶线向下寻找第一个与所有障碍物及快捷栏条保持 CLEARANCE 间距（与目录、信息卡相同，因此三者共用同一顶线）的
     * 22 像素位置；找不到时回落到顶线，保证按钮始终存在。 */
    public static Region buttonSlot(Region column, List<Region> obstacles, int width, int height) {
        Region strip = new Region((width - STRIP_WIDTH) / 2, (height - STRIP_HEIGHT) / 2, STRIP_WIDTH, STRIP_HEIGHT);
        int size = BUTTON_SIZE + 2 * CLEARANCE;
        for (int y = TOP_LINE; y + BUTTON_SIZE <= height - 8; y++) {
            Region padded = new Region(column.x() - CLEARANCE, y - CLEARANCE, size, size);
            if (!padded.intersects(strip) && noneIntersect(obstacles, padded)) {
                return new Region(column.x(), y, BUTTON_SIZE, BUTTON_SIZE);
            }
        }
        return new Region(column.x(), Math.max(0, Math.min(TOP_LINE, height - BUTTON_SIZE)), BUTTON_SIZE, BUTTON_SIZE);
    }

    private static boolean noneIntersect(List<Region> obstacles, Region region) {
        for (Region obstacle : obstacles) {
            if (obstacle.intersects(region)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Where every guide surface goes for one screen state (spec-final §4.1, §5.2, §5.7). Pure, so the screen's
     * state-to-geometry rules are testable without Minecraft.
     * <ul>
     * <li>Standalone: the full column (y = margin) with the reader beside it, the spread centred horizontally; the
     * reader frame also shows the frontispiece when nothing is open, except in compact layouts.</li>
     * <li>Embedded, persistent: content-fit directory in the largest clear gap (top fixed), or the 22 px book button
     * when the gap is too small (collapsed by rule) or the player folded it.</li>
     * <li>Embedded, modal: a docked directory keeps x, top and width and extends only its bottom to height - 8; a
     * collapsed or folded one uses the full column from the HUD top line. The reader hangs from the top line.</li>
     * </ul>
     * The close × lives in the band of the rightmost visible frame; compact layouts hide the directory while reading.
     * 某一界面状态下各指南部件的位置（spec-final §4.1、§5.2、§5.7）。纯函数，界面“状态到几何”的规则可脱离 Minecraft 测试。
     * 独立界面：整列目录（y = 边距）与其右侧的阅读器，整组水平居中；未打开条目时阅读器外框显示扉页（紧凑布局除外）。嵌入常驻：目录在最大
     * 空白区内按内容取高（顶边固定），空白区不足（按规则收起）或玩家主动收起时改为 22 像素书本按钮。嵌入模态：停靠的目录保持
     * 横坐标、顶边与宽度，只把底边延伸到 height - 8；收起状态下使用自 HUD 顶线起的整列；阅读器从顶线垂下。关闭按钮位于最右侧
     * 可见外框的标题带；紧凑布局阅读时隐藏目录。
     *
     * @param directory        drawn directory rect / 目录矩形
     * @param button           22 px book button (persistent collapsed or folded state) / 书本按钮
     * @param collapsed        the gap is too small, so the button expands a modal instead of unfolding / 按规则收起
     * @param closeInDirectory the × sits in the directory band / 关闭按钮位于目录标题带
     * @param foldable         the footer fold control exists (persistent embedded only) / 页脚收起按钮可用
     */
    public record Frames(Region directory, boolean directoryVisible, Region button, boolean buttonVisible,
                         boolean collapsed, Region reader, boolean readerVisible, boolean closeInDirectory,
                         boolean foldable, boolean compact) {
    }

    /**
     * @param obstacles       embedded only: widget footprints plus {@link #hudBand} / 仅嵌入时使用：控件占位与 HUD 带
     * @param directoryNeed   content-fit height of the persistent directory / 常驻目录按内容所需的高度
     */
    public static Frames frames(boolean embedded, int width, int height, List<Region> obstacles, boolean articleOpen,
                                boolean expanded, boolean folded, int directoryNeed) {
        GuidebookLayout full = compute(width, height);
        boolean directoryVisible = !full.compact() || !articleOpen;
        Region none = new Region(0, 0, 0, 0);
        if (!embedded) {
            boolean readerVisible = articleOpen || !full.compact();
            Region directory = full.directory();
            Region article = full.article();
            if (!full.compact()) {
                // Standalone has no inventory to dock against, so the spread is centred (equal margins left and right;
                // 640 px: directory at 98, reader 258..542) instead of hugging the left edge. It never moves between
                // states. 独立界面没有需要停靠的背包，因此整组书页水平居中（左右边距相等；640 像素时目录位于 98、阅读器位于
                // 258..542），而非贴靠左缘；各状态下位置不变。
                int shift = Math.max(0, (width - article.right() - directory.x()) / 2);
                directory = new Region(directory.x() + shift, directory.y(), directory.width(), directory.height());
                article = new Region(article.x() + shift, article.y(), article.width(), article.height());
            }
            return new Frames(directory, directoryVisible, none, false, false, article, readerVisible,
                    directoryVisible && !readerVisible, false, full.compact());
        }
        Region gap = computeEmbedded(width, height, obstacles).directory();
        boolean collapsed = collapsed(gap, width);
        boolean docked = !collapsed && !folded;
        Region button = collapsed ? buttonSlot(full.directory(), obstacles, width, height)
                : new Region(gap.x(), gap.y(), BUTTON_SIZE, BUTTON_SIZE);
        Region reader = embeddedReader(full.article(), height);
        if (!articleOpen && !expanded) {
            return new Frames(contentFit(gap, directoryNeed, MIN_DIRECTORY_HEIGHT), docked, button, !docked, collapsed,
                    reader, false, false, docked, full.compact());
        }
        Region directory;
        if (docked) {
            directory = modalDirectory(gap, height);
        } else {
            Region column = full.directory();
            int top = Math.max(column.y(), TOP_LINE);
            directory = new Region(column.x(), top, column.width(), Math.max(0, column.bottom() - top));
        }
        return new Frames(directory, directoryVisible, button, false, collapsed, reader, articleOpen,
                directoryVisible && !articleOpen, false, full.compact());
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
