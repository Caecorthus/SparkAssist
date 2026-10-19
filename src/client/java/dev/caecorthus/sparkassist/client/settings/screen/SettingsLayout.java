package dev.caecorthus.sparkassist.client.settings.screen;

/**
 * Where the parts of the settings screen go for a given scaled window size. Wide windows get the option list on the
 * left and the description panel beside it; narrow ones (large GUI scale) get the list alone, and the description
 * follows the mouse as a tooltip. The block is centred, like a page, instead of stretching across wide windows.
 * 给定缩放后窗口尺寸时设置界面各部分的位置。宽窗口左侧是选项列表、右侧是说明面板；窄窗口（GUI 缩放较大）只放列表，
 * 说明改为跟随鼠标的提示框。整块内容居中排布，而不是在宽窗口上横向拉满。
 *
 * @param left        x of the content block / 内容块左边
 * @param contentWidth width of the content block / 内容块宽度
 * @param listX       x of the option list / 选项列表左边
 * @param listWidth   width of the option list / 选项列表宽度
 * @param panelX      x of the description panel / 说明面板左边
 * @param panelWidth  width of the description panel, 0 in the narrow layout / 说明面板宽度，窄布局为 0
 * @param listTop     y where the list viewport starts / 列表可视区顶部
 * @param listBottom  y where the list viewport ends / 列表可视区底部
 * @param footerY     y of the footer buttons / 底部按钮的 y
 */
public record SettingsLayout(int left, int contentWidth, int listX, int listWidth, int panelX, int panelWidth,
                             int listTop, int listBottom, int footerY) {
    public static final int MARGIN = 8;
    public static final int TOP = 6;
    public static final int TAB_HEIGHT = 16;
    public static final int GAP = 8;
    public static final int FOOTER_HEIGHT = 20;
    public static final int LIST_MAX_WIDTH = 330;
    public static final int LIST_MIN_WIDTH = 200;
    public static final int PANEL_MAX_WIDTH = 230;
    public static final int PANEL_MIN_WIDTH = 120;

    /** Separator line under the tab row. 页签行下方分隔线的 y。 */
    public static final int SEPARATOR_Y = TOP + TAB_HEIGHT + 2;

    public static SettingsLayout of(int width, int height) {
        int available = Math.max(0, width - 2 * MARGIN);
        int listTop = SEPARATOR_Y + 6;
        int footerY = height - MARGIN - FOOTER_HEIGHT;
        int listBottom = Math.max(listTop, footerY - 6);

        int listWidth = Math.min(LIST_MAX_WIDTH, Math.max(LIST_MIN_WIDTH, (int) Math.round((available - GAP) * 0.6D)));
        int panelWidth = Math.min(PANEL_MAX_WIDTH, available - GAP - listWidth);
        if (panelWidth < PANEL_MIN_WIDTH) {
            int narrowWidth = Math.min(LIST_MAX_WIDTH, available);
            int left = (width - narrowWidth) / 2;
            return new SettingsLayout(left, narrowWidth, left, narrowWidth, left + narrowWidth, 0, listTop, listBottom,
                    footerY);
        }
        int contentWidth = listWidth + GAP + panelWidth;
        int left = (width - contentWidth) / 2;
        return new SettingsLayout(left, contentWidth, left, listWidth, left + listWidth + GAP, panelWidth, listTop,
                listBottom, footerY);
    }

    public boolean narrow() {
        return panelWidth == 0;
    }

    public int right() {
        return left + contentWidth;
    }

    public int listViewportHeight() {
        return listBottom - listTop;
    }
}
