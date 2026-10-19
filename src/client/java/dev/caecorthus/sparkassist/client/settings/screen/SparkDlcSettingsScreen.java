package dev.caecorthus.sparkassist.client.settings.screen;

import dev.caecorthus.sparkassist.client.config.SparkAssistClientSettings;
import dev.caecorthus.sparkassist.client.settings.SettingsOption;
import dev.caecorthus.sparkassist.client.settings.SettingsPage;
import dev.caecorthus.sparkassist.client.settings.SettingsSession;
import dev.caecorthus.sparkassist.client.settings.SoundPreview;
import dev.caecorthus.sparkassist.client.settings.SparkDlcSettingsPages;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * The one place for every SparkDLC client setting, laid out like Sodium's options: page tabs on top, grouped rows on
 * the left, the hovered row explained on the right. Changes apply at once; "Undo" puts back everything changed since
 * the screen opened. It stays a {@link GameOptionsScreen} so SparkWitch's sessions that close gameplay screens leave
 * it open, as they do every vanilla options screen.
 * 所有 SparkDLC 客户端设置的统一入口，布局参照 Sodium：顶部页签，左侧分组行，右侧解释鼠标所指的行。改动立即生效；
 * 「撤销」把这次打开后改过的全部退回。它继承 {@link GameOptionsScreen}，因此 SparkWitch 那些会关闭游戏界面的流程会像
 * 对待原版设置界面一样让它保持打开。
 */
public final class SparkDlcSettingsScreen extends GameOptionsScreen implements SettingsRowWidget.Host {
    private static final Text TITLE = Text.translatable("screen.sparkassist.settings.title");
    private static final int GROUP_GAP = 6;
    private static final int ROW_GAP = 1;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int SCROLL_STEP = 16;
    private static final int PANEL_PAD = 6;
    private static final int LINE_HEIGHT = 10;
    private static final int TOOLTIP_WIDTH = 200;

    /** The page last shown, kept for this client run only. 上次显示的页面，仅在本次运行期间保留。 */
    private static String lastPageId = SparkDlcSettingsPages.GENERAL;

    private final SettingsSession session;
    /** Headers and rows of the current page, top to bottom, with their offsets in the list. 当前页的标题与行及其偏移。 */
    private final List<ClickableWidget> listItems = new ArrayList<>();
    private final List<Integer> itemOffsets = new ArrayList<>();
    private final List<SettingsRowWidget> rows = new ArrayList<>();
    /** The header to focus after a fold rebuilt the list. 折叠导致重建后要聚焦的标题。 */
    @Nullable
    private String refocusGroup;
    private int pageIndex;
    private double scroll;
    private int contentHeight;
    private boolean showTitle;
    private SettingsLayout layout = SettingsLayout.of(0, 0);
    @Nullable
    private SettingsFlatButton undoButton;
    @Nullable
    private SettingsFlatButton resetButton;

    public SparkDlcSettingsScreen(Screen parent) {
        this(parent, null);
    }

    /** Opens on {@code pageId}, or on the page shown last time when null. 打开到指定页；为 null 时打开上次的页面。 */
    public SparkDlcSettingsScreen(Screen parent, @Nullable String pageId) {
        super(parent, MinecraftClient.getInstance().options, TITLE);
        this.session = new SettingsSession(SparkDlcSettingsPages.build());
        this.pageIndex = indexOf(pageId == null ? lastPageId : pageId);
    }

    private int indexOf(String pageId) {
        List<SettingsPage> pages = session.pages();
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).id().equals(pageId)) {
                return i;
            }
        }
        return 0;
    }

    @Nullable
    private SettingsPage page() {
        List<SettingsPage> pages = session.pages();
        return pages.isEmpty() ? null : pages.get(Math.min(pageIndex, pages.size() - 1));
    }

    /** The rows are drawn by this screen itself, not by the vanilla option list. 行由本界面自行绘制，不用原版选项列表。 */
    @Override
    protected void addOptions() {
    }

    @Override
    protected void init() {
        layout = SettingsLayout.of(this.width, this.height);
        listItems.clear();
        itemOffsets.clear();
        rows.clear();

        initTabs();
        initRows();
        initFooterButtons();
    }

    private void initTabs() {
        List<SettingsPage> pages = session.pages();
        int tabsWidth = 0;
        for (SettingsPage page : pages) {
            tabsWidth += this.textRenderer.getWidth(page.title()) + 2 * SettingsTabWidget.PAD_X + 2;
        }
        int titleWidth = this.textRenderer.getWidth(TITLE) + 12;
        showTitle = titleWidth + tabsWidth <= layout.contentWidth();
        int x = layout.left() + (showTitle ? titleWidth : 0);
        for (int i = 0; i < pages.size(); i++) {
            int index = i;
            SettingsTabWidget tab = new SettingsTabWidget(x, SettingsLayout.TOP, pages.get(i).title(),
                    i == pageIndex, () -> showPage(index));
            this.addDrawableChild(tab);
            x += tab.getWidth() + 2;
        }
    }

    private void initRows() {
        SettingsPage page = page();
        int offset = 0;
        if (page != null) {
            for (SettingsPage.Group group : page.groups()) {
                if (!listItems.isEmpty()) {
                    offset += GROUP_GAP;
                }
                String key = page.groupKey(group);
                boolean collapsed = SparkAssistClientSettings.settingsGroupCollapsed(key);
                SettingsGroupHeaderWidget header = new SettingsGroupHeaderWidget(layout.listX(), layout.listWidth(),
                        group.title(), key, collapsed, group.options().size(), () -> toggleGroup(key), this);
                addListItem(header, offset);
                offset += SettingsGroupHeaderWidget.HEIGHT;
                if (collapsed) {
                    continue;
                }
                for (SettingsOption option : group.options()) {
                    SettingsRowWidget row = new SettingsRowWidget(layout.listX(), 0, layout.listWidth(), option, this);
                    rows.add(row);
                    addListItem(row, offset);
                    offset += SettingsRowWidget.HEIGHT + ROW_GAP;
                }
            }
        }
        contentHeight = Math.max(0, offset - ROW_GAP);
        int itemWidth = scrollable() ? layout.listWidth() - SCROLLBAR_WIDTH - 2 : layout.listWidth();
        listItems.forEach(item -> item.setWidth(itemWidth));
        scroll = clampScroll(scroll);
        positionRows();
    }

    private void addListItem(ClickableWidget item, int offset) {
        listItems.add(item);
        itemOffsets.add(offset);
        this.addSelectableChild(item);
    }

    /** Folds or unfolds a section; remembered in sparkassist.json, not part of Undo. 折叠或展开分节；记入配置，不受撤销影响。 */
    private void toggleGroup(String key) {
        SparkAssistClientSettings.setSettingsGroupCollapsed(key, !SparkAssistClientSettings.settingsGroupCollapsed(key));
        SparkAssistClientSettings.save();
        refocusGroup = key;
        this.clearAndInit();
    }

    @Override
    protected void setInitialFocus() {
        String key = refocusGroup;
        refocusGroup = null;
        if (key != null) {
            for (ClickableWidget item : listItems) {
                if (item instanceof SettingsGroupHeaderWidget header && header.key().equals(key)) {
                    this.setFocused(header);
                    return;
                }
            }
        }
        super.setInitialFocus();
    }

    private void initFooterButtons() {
        SettingsFlatButton done = new SettingsFlatButton(ScreenTexts.DONE, button -> this.close());
        resetButton = new SettingsFlatButton(Text.translatable("screen.sparkassist.settings.reset_page"), button -> {
            SettingsPage page = page();
            if (page != null) {
                session.resetPage(page);
            }
        });
        undoButton = new SettingsFlatButton(Text.translatable("screen.sparkassist.settings.undo"),
                button -> session.undoAll());
        int x = layout.right();
        for (SettingsFlatButton button : List.of(done, resetButton, undoButton)) {
            x -= button.getWidth();
            button.setPosition(x, layout.footerY());
            x -= 4;
        }
        this.addDrawableChild(undoButton);
        this.addDrawableChild(resetButton);
        this.addDrawableChild(done);
        refreshFooter();
    }

    private void refreshFooter() {
        SettingsPage page = page();
        if (undoButton != null) {
            undoButton.active = session.hasChanges();
        }
        if (resetButton != null) {
            resetButton.active = page != null && session.pageHasNonDefault(page);
        }
    }

    /** Resizing rebuilds the widgets; page and scroll position are kept. 缩放窗口时重建控件，保留当前页与滚动位置。 */
    @Override
    protected void initTabNavigation() {
        this.clearAndInit();
    }

    private void showPage(int index) {
        session.flush();
        SoundPreview.stopAll();
        pageIndex = index;
        SettingsPage page = page();
        if (page != null) {
            lastPageId = page.id();
        }
        scroll = 0;
        this.clearAndInit();
    }

    private boolean scrollable() {
        return contentHeight > layout.listViewportHeight();
    }

    private double clampScroll(double value) {
        return Math.max(0, Math.min(value, Math.max(0, contentHeight - layout.listViewportHeight())));
    }

    private void positionRows() {
        for (int i = 0; i < listItems.size(); i++) {
            listItems.get(i).setY(layout.listTop() + itemOffsets.get(i) - (int) scroll);
        }
    }

    private void ensureVisible(ClickableWidget item) {
        int index = listItems.indexOf(item);
        if (index < 0) {
            return;
        }
        int top = itemOffsets.get(index);
        int bottom = top + item.getHeight();
        int viewport = layout.listViewportHeight();
        double target = scroll;
        if (top < scroll) {
            target = top;
        } else if (bottom > scroll + viewport) {
            target = bottom - viewport;
        }
        if (target != scroll) {
            scroll = clampScroll(target);
            positionRows();
        }
    }

    @Override
    public boolean inListViewport(double x, double y) {
        return x >= layout.listX() && x < layout.listX() + layout.listWidth()
                && y >= layout.listTop() && y < layout.listBottom();
    }

    @Override
    public void changed(SettingsOption option, boolean finished) {
        session.changed(option);
        if (finished) {
            session.flush();
        }
    }

    @Override
    public void setFocused(@Nullable Element focused) {
        super.setFocused(focused);
        if (focused instanceof ClickableWidget item && listItems.contains(item)) {
            ensureVisible(item);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (inListViewport(mouseX, mouseY) && scrollable()) {
            scroll = clampScroll(scroll - verticalAmount * SCROLL_STEP);
            positionRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // A slider drag ends wherever the mouse is released, even outside its row.
        // 无论在哪里松开鼠标，滑块拖动都会结束，即使松开时不在该行上。
        boolean finished = false;
        for (SettingsRowWidget row : rows) {
            finished |= row.finishDrag();
        }
        return super.mouseReleased(mouseX, mouseY, button) || finished;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        refreshFooter();
        super.render(context, mouseX, mouseY, delta);

        if (showTitle) {
            context.drawText(this.textRenderer, TITLE, layout.left(), SettingsLayout.TOP + 4, SettingsPalette.TEXT,
                    false);
        }
        context.fill(layout.left(), SettingsLayout.SEPARATOR_Y, layout.right(), SettingsLayout.SEPARATOR_Y + 1,
                SettingsPalette.SEPARATOR);

        renderList(context, mouseX, mouseY, delta);

        SettingsRowWidget hovered = hoveredRow(mouseX, mouseY);
        SettingsOption described = hovered != null ? hovered.option() : focusedOption();
        if (!layout.narrow()) {
            renderPanel(context, described);
        } else if (hovered != null) {
            context.drawOrderedTooltip(this.textRenderer, describe(hovered.option(), TOOLTIP_WIDTH).stream()
                    .map(line -> line.text() == null ? OrderedText.EMPTY : line.text()).toList(), mouseX, mouseY);
        }
    }

    private void renderList(DrawContext context, int mouseX, int mouseY, float delta) {
        context.enableScissor(layout.listX(), layout.listTop(), layout.listX() + layout.listWidth(),
                layout.listBottom());
        for (ClickableWidget item : listItems) {
            if (item.getY() + item.getHeight() > layout.listTop() && item.getY() < layout.listBottom()) {
                item.render(context, mouseX, mouseY, delta);
            }
        }
        context.disableScissor();

        if (scrollable()) {
            int trackX = layout.listX() + layout.listWidth() - SCROLLBAR_WIDTH;
            int viewport = layout.listViewportHeight();
            context.fill(trackX, layout.listTop(), trackX + SCROLLBAR_WIDTH, layout.listBottom(),
                    SettingsPalette.SCROLL_TRACK);
            int thumb = Math.max(12, viewport * viewport / Math.max(1, contentHeight));
            int maxScroll = Math.max(1, contentHeight - viewport);
            int thumbY = layout.listTop() + (int) Math.round((viewport - thumb) * (scroll / maxScroll));
            context.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumb, SettingsPalette.SCROLL_THUMB);
        }
    }

    @Nullable
    private SettingsRowWidget hoveredRow(double mouseX, double mouseY) {
        for (SettingsRowWidget row : rows) {
            if (row.isMouseOver(mouseX, mouseY)) {
                return row;
            }
        }
        return null;
    }

    @Nullable
    private SettingsOption focusedOption() {
        return this.getFocused() instanceof SettingsRowWidget row ? row.option() : null;
    }

    private void renderPanel(DrawContext context, @Nullable SettingsOption option) {
        int innerWidth = layout.panelWidth() - 2 * PANEL_PAD;
        List<Line> lines = option == null
                ? wrap(Text.translatable("screen.sparkassist.settings.hint"), innerWidth, SettingsPalette.TEXT_MUTED)
                : describe(option, innerWidth);
        int maxLines = Math.max(1, (layout.listViewportHeight() - 2 * PANEL_PAD) / LINE_HEIGHT);
        if (lines.size() > maxLines) {
            lines = lines.subList(0, maxLines);
        }
        int height = 2 * PANEL_PAD + lines.size() * LINE_HEIGHT - 1;
        int x = layout.panelX();
        int y = layout.listTop();
        context.fill(x, y, x + layout.panelWidth(), y + height, SettingsPalette.PANEL);
        int lineY = y + PANEL_PAD;
        for (Line line : lines) {
            if (line.text() != null) {
                context.drawText(this.textRenderer, line.text(), x + PANEL_PAD, lineY, line.color(), false);
            }
            lineY += LINE_HEIGHT;
        }
    }

    /** One drawn line of the description; a null text is a spacer. 说明的一行；text 为 null 时是空行。 */
    private record Line(@Nullable OrderedText text, int color) {
    }

    private List<Line> describe(SettingsOption option, int width) {
        List<Line> lines = new ArrayList<>(wrap(option.title(), width, SettingsPalette.TEXT));
        lines.add(new Line(null, 0));
        lines.addAll(wrap(option.description(), width, SettingsPalette.TEXT_BODY));
        Text valueDescription = option.valueDescription();
        if (valueDescription != null) {
            lines.add(new Line(null, 0));
            lines.addAll(wrap(valueDescription, width, SettingsPalette.TEXT_BODY));
        }
        if (option.preview() != null) {
            lines.add(new Line(null, 0));
            lines.addAll(wrap(Text.translatable("screen.sparkassist.settings.preview_hint"), width,
                    SettingsPalette.TEXT_MUTED));
        }
        Text reason = option.disabledReason();
        if (reason != null) {
            lines.add(new Line(null, 0));
            lines.addAll(wrap(reason, width, SettingsPalette.TEXT_WARNING));
        }
        lines.add(new Line(null, 0));
        lines.addAll(wrap(Text.translatable("screen.sparkassist.settings.default", option.defaultValueText()), width,
                SettingsPalette.TEXT_FAINT));
        lines.addAll(wrap(Text.translatable("screen.sparkassist.settings.source", option.source()), width,
                SettingsPalette.TEXT_FAINT));
        return lines;
    }

    private List<Line> wrap(Text text, int width, int color) {
        return SettingsText.wrap(this.textRenderer, text, width).stream().map(line -> new Line(line, color)).toList();
    }

    @Override
    public void tick() {
        super.tick();
        SoundPreview.tick();
    }

    @Override
    public void close() {
        session.flush();
        SoundPreview.stopAll();
        super.close();
    }

    @Override
    public void removed() {
        session.flush();
        SoundPreview.stopAll();
        super.removed();
    }
}
