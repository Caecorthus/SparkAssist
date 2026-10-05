package dev.caecorthus.sparkassist.client.guidebook;

import dev.caecorthus.sparkassist.client.guidebook.render.GuidebookContentRenderer;
import dev.caecorthus.sparkassist.client.guidebook.ui.DirectoryPainter;
import dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPaint;
import dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette;
import dev.caecorthus.sparkassist.client.guidebook.ui.ReaderPainter;
import dev.caecorthus.sparkassist.guidebook.GuidebookCatalog;
import dev.caecorthus.sparkassist.guidebook.GuidebookEntry;
import dev.caecorthus.sparkassist.client.guidebook.ui.decor.DecorSettings;
import dev.caecorthus.sparkassist.client.guidebook.ui.decor.GuidebookDecorator;
import dev.caecorthus.sparkassist.guidebook.GuidebookLayout;
import dev.caecorthus.sparkassist.guidebook.GuidebookLayout.Region;
import dev.caecorthus.sparkassist.guidebook.GuidebookNavigation;
import dev.caecorthus.sparkassist.guidebook.GuidebookSearch;
import dev.caecorthus.sparkassist.guidebook.GuidebookSessionState;
import dev.caecorthus.sparkassist.guidebook.GuidebookTab;
import dev.caecorthus.sparkassist.guidebook.decor.DecorRandom;
import dev.caecorthus.sparkassist.guidebook.decor.DecorSet;
import dev.caecorthus.sparkassist.guidebook.decor.DecorSetResolver;
import dev.caecorthus.sparkassist.guidebook.decor.DecorZones;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookBlock;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookBlockType;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookPage;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookRun;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookTone;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen.StoreItemWidget;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * Left-hand directory with the reader beside it as a book spread, also usable inside the live inventory.
 * Painting lives in {@code client.guidebook.ui}; this class owns state, geometry, input and the cached text.
 * 左侧目录与紧贴其右的正文（如同翻开的书页），也可嵌入正在运行的背包界面。绘制位于 {@code client.guidebook.ui}；
 * 本类负责状态、几何、输入与缓存的文字。
 */
public final class GuidebookScreen extends Screen {
    /** Row clicks right after a layout change are ignored (still captured). 布局变化后短时间内的行点击会被忽略（仍被拦截）。 */
    private static final long ROW_ACTIVATION_GUARD_MS = 250;
    private static final int WHEEL_STEP = 22;
    private static final Region NOWHERE = new Region(0, 0, 0, 0);

    private final Screen parent;
    private final boolean embedded;
    private final GuidebookSessionState session = GuidebookClientState.session();
    private final Set<String> expandedNodes = new HashSet<>();
    private GuidebookCatalog catalog = GuidebookCatalog.of(List.of());
    private TranslationStorage chineseTranslations;

    // ---- geometry / 几何
    private LayoutKey layoutKey;
    private List<Region> obstacles = List.of();
    // Everything obstacles() reads, flattened (x, y, w, h, price per visible widget); the obstacle list, gap and
    // button slot are rebuilt only when it changes, not every frame or keystroke.
    // obstacles() 读取的全部输入（每个可见控件的 x、y、宽、高、价格）；只有它变化时才重建障碍物、空白区与按钮位置，
    // 而不是每帧或每次按键都重建。
    private int[] obstacleInputs;
    private int[] obstacleScratch = new int[80];
    private Region gap;
    private Region buttonSlot;
    private boolean folded;
    private GuidebookLayout.Frames frames;
    private Region nav = NOWHERE;
    private Region reader = NOWHERE;
    private ReaderPainter.Sheet sheet = ReaderPainter.Sheet.of(NOWHERE);
    private long layoutChangedAt;

    // ---- directory rows / 目录行
    private List<RowSource> rowSources = List.of();
    private List<RowSlot> rows = List.of();
    private List<DirectoryPainter.Row> paintRows = List.of();
    private List<ContainerLabel> containerLabels = List.of();
    private int directoryHeight;
    private DirectoryPainter.Chrome chrome;
    private int titleWidth;
    // ---- decoration / 点缀
    private final GuidebookDecorator decorator = new GuidebookDecorator();
    private DecorSet pageSet = DecorSet.CIVILIAN;
    private long pageSeed;
    private Set<String> markedRoles = Set.of();
    private Set<String> markedTraits = Set.of();
    private int marksRevision;

    // ---- reader / 正文
    private GuidebookEntry selectedEntry;
    private String selectedGroup;
    private GuidebookContentRenderer.Layout content = GuidebookContentRenderer.Layout.EMPTY;
    private ReaderPainter.PageHeader pageHeader;
    private ReaderPainter.Band readerBand;
    private Text sourceTooltip;
    private Text runningSection;
    private OrderedText runningSectionText;

    // ---- chrome text, resolved once per init / 界面文字，每次初始化解析一次
    private String titleFull = "";
    private String titleShort = "";
    private String titleTiny = "";
    private String placeholderFull = "";
    private String placeholderShort = "";
    private String placeholderTiny = "";
    private String searchLabel = "";
    private String markedLabel = "";
    private String sourceFormat = "%s";
    private OrderedText creditsLabel = OrderedText.EMPTY;
    private int creditsWidth;
    private OrderedText noResultsLabel = OrderedText.EMPTY;
    private int noResultsWidth;
    private OrderedText frontTitle = OrderedText.EMPTY;
    private OrderedText frontSelect = OrderedText.EMPTY;
    private OrderedText frontKeys = OrderedText.EMPTY;
    private ReaderPainter.Band emptyBand;
    private Text searchTip = Text.empty();
    private Text clearTip = Text.empty();
    private Text foldTip = Text.empty();
    private Text backTip = Text.empty();
    private Text openTip = Text.empty();
    private Text markedTip = Text.empty();
    private int manaPriceWidth;
    private int countReserveWidth;

    // ---- interaction / 交互
    private TextFieldWidget searchField;
    private String searchQuery = "";
    private boolean searchExpanded;
    private boolean creditsOpen;
    private boolean initialized;
    private boolean treeFocused;
    private int focusedRow = -1;
    private int directoryScroll;
    private int articleScroll;
    private boolean draggingDirectory;
    private boolean draggingArticle;
    private boolean directoryExpanded;

    public GuidebookScreen(Screen parent) {
        this(parent, false);
    }

    private GuidebookScreen(Screen parent, boolean embedded) {
        super(Text.translatable("screen.sparkassist.guidebook"));
        this.parent = parent;
        this.embedded = embedded;
    }

    public static GuidebookScreen embedded(Screen parent) {
        return new GuidebookScreen(parent, true);
    }

    @Override
    protected void init() {
        boolean firstLayout = !initialized;
        catalog = GuidebookRuntimeCatalog.load(client);
        chineseTranslations = TranslationStorage.load(client.getResourceManager(), List.of("zh_cn"), false);
        if (!initialized) {
            expandedNodes.addAll(session.expandedNodeIds());
            directoryScroll = session.leftScroll();
            articleScroll = session.rightScroll();
            session.selectedEntryId().flatMap(catalog::find).ifPresent(entry -> selectedEntry = entry);
            initialized = true;
        }
        // Opening the inventory never selects a role or expands a branch automatically.
        // 打开背包时不自动选中身份，也不自动展开目录。
        cacheChrome();
        marksRevision = session.observationRevision();
        markedRoles = session.observedRoleIds();
        markedTraits = session.observedTraitIds();
        searchField = new TextFieldWidget(textRenderer, 0, 0, 1, 9, searchTip);
        searchField.setMaxLength(64);
        // The well, caret offset and placeholder are drawn by DirectoryPainter; vanilla keeps caret and selection.
        // 凹槽与占位文字由 DirectoryPainter 绘制；光标与选区保留原版行为。
        searchField.setDrawsBackground(false);
        searchField.setEditableColor(ExpressPalette.TEXT_HI);
        searchField.setPlaceholder(null);
        searchField.setText(searchQuery);
        searchField.setChangedListener(value -> {
            searchQuery = value;
            directoryScroll = 0;
            refreshRows();
        });
        addDrawableChild(searchField);
        layoutKey = null;
        obstacleInputs = null;
        updateInventoryLayout();
        // A restored selection (a new inventory, or a GUI-scale change re-running init) can sit outside the directory
        // viewport; scroll just enough to show its row. Nothing is selected or expanded here, so opening the inventory
        // still never selects a role or opens a branch by itself.
        // 恢复的选中项（重新打开背包，或改变 GUI 缩放后重新初始化）可能位于目录视口之外，此时仅滚动到恰好露出该行。这里不会
        // 新选中或展开任何内容，因此打开背包时仍不会自动选中身份或展开目录。
        int selectedRow = selectedRowIndex();
        if (selectedRow >= 0) {
            revealRow(selectedRow);
        }
        if (firstLayout) {
            // A freshly opened guide has no earlier layout that could have moved under the pointer, so its first
            // layout does not arm the row click guard; re-inits (resizes) still do, because rows really move there.
            // 刚打开的指南没有可能在指针下移动的旧布局，因此首次排版不启用行点击保护；重新初始化（缩放窗口）时行确实会移动，仍会启用。
            layoutChangedAt = Long.MIN_VALUE / 2;
        }
    }

    /** Resolve every chrome string once (always Chinese) so no translation lookup runs per frame.
     * 一次性解析所有界面文字（始终为中文），避免逐帧查询翻译。 */
    private void cacheChrome() {
        manaPriceWidth = textRenderer.getWidth("888 \uE782");
        // Root and group counts never exceed two digits. 根与分组的计数不超过两位数。
        countReserveWidth = textRenderer.getWidth("88");
        titleFull = chineseString("screen.sparkassist.guidebook");
        titleShort = chineseString("guidebook.sparkassist.title.short");
        titleTiny = chineseString("guidebook.sparkassist.title.tiny");
        placeholderFull = chineseString("guidebook.sparkassist.search.placeholder");
        placeholderShort = chineseString("guidebook.sparkassist.search.placeholder.short");
        placeholderTiny = chineseString("guidebook.sparkassist.search.placeholder.tiny");
        searchLabel = chineseString("guidebook.sparkassist.search");
        markedLabel = chineseString("guidebook.sparkassist.marked");
        sourceFormat = chineseString("guidebook.sparkassist.source");
        String credits = chineseString("guidebook.sparkassist.credits");
        creditsLabel = plain(credits);
        creditsWidth = textRenderer.getWidth(credits);
        String noResults = chineseString("guidebook.sparkassist.search.no_results");
        noResultsLabel = plain(noResults);
        noResultsWidth = textRenderer.getWidth(noResults);
        frontTitle = plain(titleFull);
        frontSelect = plain(chineseString("guidebook.sparkassist.select_entry"));
        frontKeys = plain(chineseString("guidebook.sparkassist.hint.keys"));
        searchTip = chineseText("guidebook.sparkassist.search");
        clearTip = chineseText("guidebook.sparkassist.search.clear");
        foldTip = chineseText("guidebook.sparkassist.fold");
        backTip = chineseText("gui.back");
        openTip = chineseText("button.sparkassist.guidebook.open");
        markedTip = chineseText("guidebook.sparkassist.marked");
    }

    /**
     * Recompute geometry when the size, the article/expanded state, the fold flag or any obstacle changed (other
     * mods may add widgets after init), without reloading the catalog. Returns true when it recomputed.
     * 在尺寸、正文/展开状态、收起标记或任何障碍物变化时重算几何（其他模组可能在初始化后添加控件），不重新加载指南内容；
     * 重算时返回 true。
     */
    private boolean updateInventoryLayout() {
        boolean foldedNow = embedded && GuidebookClientState.directoryFolded();
        if (embedded && obstacleInputsChanged()) {
            obstacles = obstacles();
            gap = GuidebookLayout.computeEmbedded(width, height, obstacles).directory();
            buttonSlot = GuidebookLayout.collapsed(gap, width) ? GuidebookLayout.buttonSlot(
                    GuidebookLayout.compute(width, height).directory(), obstacles, width, height) : null;
        }
        // Key on the geometry the obstacles produce, not on the obstacles themselves, so widgets moving elsewhere on
        // screen never re-lay the guide (or re-arm the row click guard).
        // 以障碍物推导出的几何作为键，而非障碍物本身；屏幕其他位置的控件移动不会让指南重新排版（也不会重置行点击保护）。
        LayoutKey key = new LayoutKey(width, height, isArticleOpen(), directoryExpanded, foldedNow,
                embedded ? gap : null, embedded ? buttonSlot : null);
        if (key.equals(layoutKey)) {
            return false;
        }
        layoutKey = key;
        // Row clicks within ROW_ACTIVATION_GUARD_MS of this are ignored. 此后短时间内的行点击会被忽略。
        layoutChangedAt = Util.getMeasuringTimeMs();
        folded = foldedNow;
        draggingDirectory = false;
        draggingArticle = false;
        refreshRows();
        refreshArticle();
        return true;
    }

    /**
     * Flatten the obstacle inputs into a reused buffer and compare them with the last rebuild; allocation-free while
     * nothing changes. Screen size is part of the inputs (the price-tag placement depends on it).
     * 把障碍物输入展开到复用的缓冲区并与上次重建时比较；输入不变时不分配内存。屏幕尺寸也属于输入（价签位置依赖它）。
     */
    private boolean obstacleInputsChanged() {
        List<? extends Element> children = parent.children();
        int[] values = obstacleScratch;
        int count = 0;
        values[count++] = width;
        values[count++] = height;
        for (int index = 0; index < children.size(); index++) {
            if (!(children.get(index) instanceof ClickableWidget widget) || !widget.visible) {
                continue;
            }
            if (count + 5 > values.length) {
                values = obstacleScratch = Arrays.copyOf(values, values.length * 2);
            }
            values[count++] = widget.getX();
            values[count++] = widget.getY();
            values[count++] = widget.getWidth();
            values[count++] = widget.getHeight();
            values[count++] = widget instanceof StoreItemWidget item ? item.entry.price() : Integer.MIN_VALUE;
        }
        if (obstacleInputs != null && Arrays.equals(values, 0, count, obstacleInputs, 0, obstacleInputs.length)) {
            return false;
        }
        obstacleInputs = Arrays.copyOf(values, count);
        return true;
    }

    /**
     * Obstacles for the persistent directory: every visible clickable widget of the inventory plus Wathe's HUD band.
     * Shop items reserve the slot frame and the price tag (wide enough for SparkWitch's "N ✦" labels); 16x16 widgets
     * (target heads) reserve their 30x30 frame; everything gets 2 px padding.
     * 常驻目录的障碍物：背包中所有可见的可点击控件与 Wathe 顶部 HUD 带。商品保留槽框与价签（宽度足以容纳 SparkWitch 的
     * “N ✦” 标签），16x16 控件（目标头像）保留 30x30 边框，所有障碍物再外扩 2 像素。
     */
    private List<Region> obstacles() {
        List<Region> occupied = new ArrayList<>();
        occupied.add(GuidebookLayout.hudBand(width));
        for (var child : parent.children()) {
            if (!(child instanceof ClickableWidget widget) || !widget.visible) {
                continue;
            }
            if (widget instanceof StoreItemWidget item) {
                int priceWidth = Math.max(textRenderer.getWidth(item.entry.price() + "\uE781"), manaPriceWidth);
                var price = HoveredTooltipPositioner.INSTANCE.getPosition(width, height,
                        item.getX() - 4 - priceWidth / 2, item.getY() - 9, priceWidth, 8);
                int left = Math.min(item.getX() - 7, price.x() - 4) - 2;
                int top = Math.min(item.getY() - 7, price.y() - 4) - 2;
                int right = Math.max(item.getX() + 23, price.x() + priceWidth + 4) + 2;
                int bottom = Math.max(item.getY() + 23, price.y() + 12) + 2;
                occupied.add(new Region(left, top, right - left, bottom - top));
            } else {
                occupied.add(GuidebookLayout.widgetObstacle(widget.getX(), widget.getY(), widget.getWidth(),
                        widget.getHeight()));
            }
        }
        return occupied;
    }

    private void refreshRows() {
        String needle = searchQuery.strip().toLowerCase(Locale.ROOT);
        boolean searching = !needle.isEmpty();
        Predicate<GuidebookEntry> filter = entry -> true;
        if (searching) {
            Set<String> matched = new HashSet<>();
            for (GuidebookEntry entry : catalog.entries()) {
                if (matches(entry, needle)) {
                    matched.add(entry.id());
                }
            }
            filter = entry -> matched.contains(entry.id());
        }
        List<GuidebookNavigation.Row> nodes = GuidebookNavigation.rows(catalog, expandedNodes, filter, searching);
        // Counts cover the groups a root shows when expanded; while searching they count matches.
        // 计数只覆盖根目录展开后可见的分组；搜索时统计匹配项。
        Map<String, GuidebookNavigation.Summary> summaries = GuidebookNavigation.summaries(catalog, filter, searching,
                this::isMarked);
        // Every root and group label the unfiltered directory can show, for the per-depth count decision.
        // 未过滤目录可能显示的全部根与分组标签，用于按层级决定是否显示计数。
        Map<String, GuidebookNavigation.Summary> all = searching
                ? GuidebookNavigation.summaries(catalog, entry -> true, false, this::isMarked) : summaries;
        List<ContainerLabel> labels = new ArrayList<>(all.size());
        all.forEach((key, summary) -> labels.add(new ContainerLabel(key.startsWith("root:") ? 0 : 1,
                textRenderer.getWidth(chineseString(GuidebookNavigation.labelKey(key.substring(key.indexOf(':') + 1)))),
                summary.containsMarked())));
        containerLabels = List.copyOf(labels);
        List<RowSource> sources = new ArrayList<>();
        int y = 0;
        for (int index = 0; index < nodes.size(); index++) {
            GuidebookNavigation.Row node = nodes.get(index);
            boolean first = index == 0;
            int bodyTop = y + (node.depth() == 0 && !first ? DirectoryPainter.ROOT_GAP : 0);
            GuidebookEntry entry = node.entry();
            sources.add(new RowSource(node, chineseString(node.nameKey()), y, bodyTop,
                    entry == null ? summaries.get(node.key()) : null, entry != null && isMarked(entry)));
            y += DirectoryPainter.rowHeight(node.depth(), first);
        }
        rowSources = List.copyOf(sources);
        directoryHeight = y;
        layoutDirectory();
    }

    /** Frames for the current state, then everything that depends on the directory size (labels, field, chrome).
     * 计算当前状态下的各部件位置，再排版依赖目录尺寸的内容（行标签、输入框与界面文字）。 */
    private void layoutDirectory() {
        frames = GuidebookLayout.frames(embedded, width, height, obstacles, isArticleOpen(), directoryExpanded, folded,
                DirectoryPainter.need(searchExpanded, directoryHeight, rowSources.isEmpty()));
        nav = frames.directory();
        if (emptyBand == null || !frames.reader().equals(reader)) {
            reader = frames.reader();
            sheet = ReaderPainter.Sheet.of(reader);
            emptyBand = ReaderPainter.Band.fit(textRenderer, reader, "", null, "", 0);
        }
        if (!directoryVisible()) {
            searchField.setFocused(false);
            treeFocused = false;
        }
        Region view = directoryViewport();
        boolean scrollable = directoryHeight > view.height();
        int rowLeft = DirectoryPainter.rowLeft(nav);
        int rowRight = DirectoryPainter.rowRight(nav, scrollable);
        boolean[] countsDropped = countsDropped(rowLeft);
        List<RowSlot> slots = new ArrayList<>(rowSources.size());
        List<DirectoryPainter.Row> painted = new ArrayList<>(rowSources.size());
        for (RowSource source : rowSources) {
            GuidebookNavigation.Row node = source.node();
            GuidebookNavigation.Summary summary = source.summary();
            boolean containsMarked = summary != null && summary.containsMarked();
            boolean marked = source.marked();
            boolean star = containsMarked && !node.expanded();
            String countText = summary == null || countsDropped[node.depth()] ? null : String.valueOf(summary.count());
            int countWidth = countText == null ? 0 : textRenderer.getWidth(countText);
            int countReserve = countText == null ? 0 : Math.max(countReserveWidth, countWidth);
            int limit = DirectoryPainter.labelLimit(node.depth(), rowLeft, rowRight, countReserve, star, marked);
            if ((star || marked) && textRenderer.getWidth(source.name()) > limit) {
                // Still too long once the count is gone (72 px column at 320x240): the name outranks the star as well,
                // so "大魔女" reads whole instead of "大…★". The row tooltip and the reader chip still say "本局已获得".
                // 省略计数后仍放不下（320x240 下 72 像素目录）：名称也比星标重要，因此显示完整的“大魔女”而不是“大…★”；
                // 行提示与正文徽记仍会显示“本局已获得”。
                containsMarked = false;
                marked = false;
                limit = DirectoryPainter.labelLimit(node.depth(), rowLeft, rowRight, countReserve, false, false);
            }
            String shown = ExpressPaint.ellipsize(textRenderer, source.name(), limit);
            GuidebookEntry entry = node.entry();
            DirectoryPainter.Row row = new DirectoryPainter.Row(node.depth(), source.top(), source.bodyTop(),
                    plain(shown), textRenderer.getWidth(shown), countText == null ? null : plain(countText), countWidth,
                    countReserve, node.expanded(), containsMarked, marked, entry != null
                    && node.depth() == 2 && !node.nameKey().equals(entry.nameKey()), entry == null ? 0 : entry.color(),
                    entry == null ? null : entry.id());
            slots.add(new RowSlot(node, rowTip(source.name(), !shown.equals(source.name()), source.marked()), row));
            painted.add(row);
        }
        rows = List.copyOf(slots);
        paintRows = List.copyOf(painted);
        boolean closeInBand = closeInBand();
        // Each ladder keeps the longest wording that fits and ellipsizes only its last, shortest step.
        // 每组候选保留放得下的最长措辞，只有最后、最短的一项才会被截断。
        String title = ExpressPaint.firstFitting(textRenderer, DirectoryPainter.titleRoom(nav, closeInBand),
                titleFull, titleShort, titleTiny);
        String placeholder = ExpressPaint.firstFitting(textRenderer, DirectoryPainter.fieldWidth(nav) - 7,
                placeholderFull, placeholderShort, placeholderTiny, searchLabel);
        titleWidth = textRenderer.getWidth(title);
        chrome = new DirectoryPainter.Chrome(plain(title), plain(placeholder), creditsLabel, creditsWidth,
                noResultsLabel, noResultsWidth);
        int fieldWidth = DirectoryPainter.fieldWidth(nav);
        searchField.setX(DirectoryPainter.fieldX(nav));
        searchField.setY(DirectoryPainter.fieldY(nav));
        searchField.setWidth(fieldWidth);
        searchField.visible = searchExpanded && directoryVisible();
        directoryScroll = MathHelper.clamp(directoryScroll, 0, maxDirectoryScroll());
        focusedRow = Math.min(focusedRow, rows.size() - 1);
    }

    /**
     * Per depth (roots 0, groups 1): whether every count at that depth is dropped because some label there would not
     * fit beside it (72 px column at 320x240). Decided for the whole column on every label the directory can show,
     * with the star assumed wherever one can appear and the scrollbar present, and on the fixed "88" reserve, so counts
     * never show on some rows only, nor come and go as branches open, the list starts to scroll, a search narrows it
     * or its counts shrink. Names outrank counts. 按层级（根为 0、分组为 1）决定是否省略该层全部计数：只要该层有标签放不下
     * 计数（320x240 下 72 像素目录）就整层省略。判断覆盖目录可能显示的全部标签，凡可能出现星标处都按有星标、并按有滚动条
     * 计算，且使用固定的“88”预留宽度；因此计数不会只出现在部分行，也不会因展开分支、开始滚动、搜索过滤或计数变短而时有时无。
     * 名称比计数重要。
     */
    private boolean[] countsDropped(int rowLeft) {
        int rowRight = DirectoryPainter.rowRight(nav, true);
        // Indexed by depth; entries (2) never carry counts. 按层级索引，条目（2）没有计数。
        boolean[] dropped = new boolean[3];
        for (ContainerLabel label : containerLabels) {
            if (label.width() > DirectoryPainter.labelLimit(label.depth(), rowLeft, rowRight, countReserveWidth,
                    label.containsMarked(), false)) {
                dropped[label.depth()] = true;
            }
        }
        return dropped;
    }

    /** Row tooltip lines, built once per layout: the full name when shortened, then "本局已获得" when marked.
     * 行提示文字，每次排版构建一次：名称被截断时显示完整名称，已获得时再加“本局已获得”。 */
    private List<Text> rowTip(String name, boolean ellipsized, boolean marked) {
        if (ellipsized) {
            return marked ? List.of(Text.literal(name), markedTip) : List.of(Text.literal(name));
        }
        return marked ? List.of(markedTip) : List.of();
    }

    private boolean matches(GuidebookEntry entry, String needle) {
        if (GuidebookSearch.matches(entry, chineseString(entry.nameKey()), entry.ownerRoleIds().stream()
                .map(id -> chineseString("announcement.role." + id.substring(id.indexOf(':') + 1))).toList(), needle)) {
            return true;
        }
        return GuidebookNavigation.groupsFor(entry).stream().anyMatch(group ->
                chineseString(GuidebookNavigation.labelKey(group)).toLowerCase(Locale.ROOT).contains(needle));
    }

    private void refreshArticle() {
        runningSection = null;
        runningSectionText = null;
        if (!isArticleOpen()) {
            content = GuidebookContentRenderer.Layout.EMPTY;
            pageHeader = null;
            readerBand = null;
            sourceTooltip = null;
            return;
        }
        String title;
        int gem;
        boolean marked;
        String crumb;
        String source;
        List<GuidebookBlock> blocks = new ArrayList<>();
        if (creditsOpen) {
            title = chineseString("guidebook.sparkassist.credits.title");
            gem = ExpressPalette.POLISHED;
            marked = false;
            crumb = titleFull;
            source = null;
            blocks.addAll(GuidebookContentRenderer.creditsPage(
                    chineseString("guidebook.sparkassist.credits.body")).blocks());
        } else {
            title = chineseString(selectedEntry.nameKey());
            gem = selectedEntry.color();
            marked = isMarked(selectedEntry);
            crumb = crumbFor(selectedEntry);
            source = sourceName(selectedEntry.sourceModId());
            if (!selectedEntry.pages().isEmpty()) {
                for (GuidebookPage page : selectedEntry.pages()) {
                    if (!blocks.isEmpty()) {
                        blocks.add(new GuidebookBlock(GuidebookBlockType.SPACER, List.of()));
                    }
                    blocks.addAll(page.blocks());
                }
            } else {
                List<String> keys = selectedEntry.pageKeys().isEmpty()
                        ? List.of(selectedEntry.summaryKey()) : selectedEntry.pageKeys();
                keys.forEach(key -> blocks.add(paragraph(key)));
            }
        }
        content = GuidebookContentRenderer.layout(new GuidebookPage(blocks), textRenderer,
                sheet.measure(), this::chineseString);
        pageHeader = ReaderPainter.PageHeader.fit(textRenderer, title, gem, marked, markedLabel, sheet.measure());
        readerBand = ReaderPainter.Band.fit(textRenderer, reader, crumb, source, title, gem);
        sourceTooltip = source == null ? null : Text.literal(sourceFormat.formatted(source));
        pageSet = creditsOpen ? DecorSetResolver.forCredits() : DecorSetResolver.forEntry(selectedEntry);
        pageSeed = DecorRandom.pageSeed(creditsOpen ? "credits" : selectedEntry.id(), session.roundSeed());
        articleScroll = MathHelper.clamp(articleScroll, 0, maxArticleScroll());
    }

    private static GuidebookBlock paragraph(String key) {
        return new GuidebookBlock(GuidebookBlockType.PARAGRAPH,
                List.of(GuidebookRun.translated(key, false, false, GuidebookTone.DEFAULT)));
    }

    /** "<group> · <tab>", the group taken from the clicked row (not persisted); factions use the overview label.
     * “分组 · 类型”，分组取自点击的行（不持久化）；阵营条目使用“阵营总览”。 */
    private String crumbFor(GuidebookEntry entry) {
        List<String> groups = GuidebookNavigation.groupsFor(entry);
        String group = selectedGroup != null && groups.contains(selectedGroup) ? selectedGroup
                : groups.isEmpty() ? null : groups.get(0);
        String kind = entry.tab() == GuidebookTab.FACTION
                ? chineseString(GuidebookNavigation.labelKey("faction_overview"))
                : chineseString("guidebook.sparkassist.tab." + entry.tab().name().toLowerCase(Locale.ROOT));
        return group == null ? kind : chineseString(GuidebookNavigation.labelKey(group)) + " · " + kind;
    }

    private String sourceName(String modId) {
        String key = "guidebook.sparkassist.source_name." + modId;
        return chineseTranslations != null && chineseTranslations.hasTranslation(key)
                ? chineseTranslations.get(key) : modId;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Deliberately empty: embedded, the inventory stays visible; standalone, render() draws its own background.
        // 刻意留空：嵌入时保留背包画面；独立界面由 render() 自行绘制背景。
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateInventoryLayout();
        refreshMarks();
        if (!embedded) {
            // One quiet ground for every standalone state, the same over the title panorama and over the world (m6):
            // vanilla's in-game dim, then the warm soft tint. Opening an entry does not darken it further; the modal
            // scrim below exists only to hide the live inventory. No blur: vanilla allows only one blur per frame.
            // 独立界面在所有状态下使用同一块安静的底色，标题全景图与游戏世界上相同（m6）：先原版游戏内压暗，再叠暖色柔和遮罩。
            // 打开条目不会再加深；下方的模态遮罩只用于遮住正在运行的背包。不使用模糊：原版每帧只允许一次模糊。
            if (client.world == null) {
                renderPanoramaBackground(context, delta);
            }
            renderInGameBackground(context);
            context.fill(0, 0, width, height, ExpressPalette.SCRIM_SOFT);
        } else if (isModal()) {
            context.fill(0, 0, width, height, ExpressPalette.SCRIM);
        }
        Tip tip = null;
        if (directoryVisible()) {
            tip = renderDirectory(context, mouseX, mouseY, delta);
        } else {
            searchField.visible = false;
            if (showButton()) {
                Region button = frames.button();
                boolean hovered = button.contains(mouseX, mouseY);
                ExpressPaint.iconButton(context, button.x(), button.y(), hovered);
                if (hovered) {
                    tip = new Tip(List.of(openTip), -1);
                }
            }
        }
        if (readerFrameVisible()) {
            Tip readerTip = renderReader(context, mouseX, mouseY);
            if (tip == null) {
                tip = readerTip;
            }
        }
        renderOverlays(context);
        if (tip != null) {
            drawTip(context, tip, mouseX, mouseY);
        }
    }

    /**
     * Ornaments that sit on top of the finished panels (ribbon, seal, steam) and the developer zone overlay.
     * They are drawn after every panel and before tooltips; curls never enter a panel or the HUD band.
     * 画在完成的面板之上的点缀（丝带、火漆、蒸汽）与开发用禁区叠加。在所有面板之后、提示框之前绘制；卷云不会进入
     * 面板或 HUD 带。
     */
    private void renderOverlays(DrawContext context) {
        if (!DecorSettings.enabled()) {
            return;
        }
        List<Region> zones = new ArrayList<>();
        if (embedded) {
            zones.add(DecorZones.hud(width));
            if (!isModal()) {
                zones.addAll(obstacles);
            }
        }
        boolean directoryShown = directoryVisible();
        boolean readerShown = readerFrameVisible();
        if (directoryShown) {
            zones.add(DecorZones.panelInterior(nav));
        }
        if (readerShown) {
            zones.add(DecorZones.panelInterior(reader));
        }
        if (directoryShown) {
            decorator.drawDirectoryOverlay(context, nav, ownerSet(), session.roundSeed(), zones);
        }
        if (readerShown) {
            boolean article = isArticleOpen();
            DecorSet set = article ? pageSet : ownerSet();
            long seed = article ? pageSeed : DecorRandom.ownerSeed("frontispiece", session.roundSeed());
            decorator.drawReaderOverlay(context, reader, sheet, set, article && set.equals(ownerSet()), seed, zones);
        }
        if (DecorSettings.debugZones()) {
            GuidebookDecorator.drawDebugZones(context, zones);
        }
    }

    /**
     * Guide tooltips are always brass (ExpressPaint.tooltip), in the lobby and on the title screen too. Row tooltips
     * hang beside the panel on the row's label line so they never cover the row's star; icon tooltips follow the
     * pointer with vanilla's placement.
     * 指南提示框始终为黄铜样式（ExpressPaint.tooltip），大厅与标题界面也一样。行提示位于面板右侧、与该行标签同一行，不会遮住
     * 该行的星标；图标提示沿用原版的指针定位。
     */
    private void drawTip(DrawContext context, Tip tip, int mouseX, int mouseY) {
        int tipWidth = ExpressPaint.tooltipWidth(textRenderer, tip.lines());
        int tipHeight = ExpressPaint.tooltipHeight(tip.lines().size());
        int x;
        int y;
        if (tip.row() >= 0) {
            DirectoryPainter.Row row = paintRows.get(tip.row());
            // Only the persistent directory sits beside live shop items; under the modal scrim nothing shows through.
            // 只有常驻目录旁边是可见的商品；模态遮罩下没有需要避让的内容。
            Region box = DirectoryPainter.rowTipBox(nav, row.depth(),
                    directoryViewport().y() - directoryScroll + row.bodyTop(),
                    DirectoryPainter.labelX(row.depth(), DirectoryPainter.rowLeft(nav)), tipWidth, tipHeight, width,
                    height, embedded && !isModal() ? obstacles : List.of());
            x = box.x();
            y = box.y();
        } else {
            var position = HoveredTooltipPositioner.INSTANCE.getPosition(width, height, mouseX, mouseY, tipWidth,
                    tipHeight);
            // Vanilla only keeps the text on screen; the brass frame reaches 4 px further, so pull the box in by at
            // most those 4 px, never under the pointer. 原版只保证文字不出屏，黄铜边框还会外延 4 像素，
            // 因此最多再向内收 4 像素，不会移到指针下方。
            x = Math.max(4, Math.min(position.x(), width - 4 - tipWidth));
            y = Math.max(4, Math.min(position.y(), height - 4 - tipHeight));
        }
        ExpressPaint.tooltip(context, textRenderer, tip.lines(), x, y, tipWidth);
    }

    private Tip renderDirectory(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean closeInBand = closeInBand();
        boolean foldable = foldable();
        int hovered = rowAt(mouseX, mouseY);
        boolean closeHover = closeInBand && DirectoryPainter.closeBox(nav).contains(mouseX, mouseY);
        boolean searchHover = DirectoryPainter.searchToggle(nav, closeInBand).contains(mouseX, mouseY);
        boolean clearHover = searchExpanded && !searchQuery.isEmpty()
                && DirectoryPainter.clearBox(nav).contains(mouseX, mouseY);
        boolean foldHover = foldable && DirectoryPainter.foldBox(nav).contains(mouseX, mouseY);
        boolean creditsHover = DirectoryPainter.creditsZone(nav, foldable).contains(mouseX, mouseY);
        boolean thumbHot = draggingDirectory || overDirectoryThumbTrack(mouseX, mouseY);
        DirectoryPainter.Model model = new DirectoryPainter.Model(nav, chrome, paintRows, directoryHeight,
                directoryScroll, searchExpanded, searchField.isFocused(), searchQuery.isEmpty(), closeInBand, foldable,
                creditsOpen, hovered, treeFocused ? focusedRow : -1, selectedId(), closeHover, searchHover,
                creditsHover, foldHover, thumbHot, DecorSettings.enabled(), titleWidth, ownerSet().colour());
        // Chrome and rows are separate fill-and-text batches so the decoration layer (one textured quad) can sit
        // between them: over the panel body, under the rows. 边框与行分成两个批次，点缀层（一个贴图四边形）夹在中间：
        // 在面板底之上、行之下。
        context.draw(() -> DirectoryPainter.drawChrome(context, textRenderer, model));
        decorator.drawDirectoryBackground(context, nav, directoryViewport().y(), ownerSet(), session.roundSeed());
        context.draw(() -> DirectoryPainter.drawRows(context, textRenderer, model));
        searchField.visible = searchExpanded;
        if (searchField.visible) {
            searchField.render(context, mouseX, mouseY, delta);
        }
        if (hovered >= 0) {
            List<Text> lines = rows.get(hovered).tip();
            return lines.isEmpty() ? null : new Tip(lines, hovered);
        }
        if (closeHover) {
            return new Tip(List.of(backTip), -1);
        }
        if (searchHover) {
            return new Tip(List.of(searchTip), -1);
        }
        if (clearHover) {
            return new Tip(List.of(clearTip), -1);
        }
        if (foldHover) {
            return new Tip(List.of(foldTip), -1);
        }
        return null;
    }

    private Tip renderReader(DrawContext context, int mouseX, int mouseY) {
        boolean closeHover = ReaderPainter.closeBox(reader).contains(mouseX, mouseY);
        if (!isArticleOpen()) {
            boolean frontPlate = decorator.frontPlateShown(sheet);
            context.draw(() -> {
                ReaderPainter.frame(context, reader, DecorSettings.enabled());
                ReaderPainter.band(context, textRenderer, reader, emptyBand, false, null, closeHover,
                        DecorSettings.enabled());
            });
            if (frontPlate) {
                decorator.drawFrontispiecePlate(context, sheet, ownerSet(), session.roundSeed());
            }
            // The frontispiece draws the book texture, which is not batched, so it stays outside draw().
            // 扉页含书本贴图（不参与批量提交），因此放在 draw() 之外。
            if (sheet.viewport().height() > 0) {
                ReaderPainter.frontispiece(context, textRenderer, sheet, frontTitle, frontSelect, frontKeys,
                        frontPlate);
            }
            return closeHover ? new Tip(List.of(backTip), -1) : null;
        }
        boolean plate = plateShown();
        boolean running = articleScroll > ReaderPainter.runningHeadScroll(plate);
        Text section = ReaderPainter.currentSection(content, articleScroll, plate);
        if (section != runningSection) {
            runningSection = section;
            runningSectionText = readerBand.section(textRenderer, section);
        }
        boolean thumbHot = draggingArticle || sheet.scrollHit().contains(mouseX, mouseY);
        OrderedText head = runningSectionText;
        // Frame and band, then the paper layers (watermark, scrolled plate), then the content: the layers are
        // textured quads between two fill-and-text batches, under the text by draw order alone.
        // 先外框与标题带，再纸面各层（暗纹、随页滚动的扉画），最后正文：各层是夹在两个批次之间的贴图四边形，仅凭绘制顺序
        // 位于文字之下。
        context.draw(() -> {
            ReaderPainter.frame(context, reader, DecorSettings.enabled());
            ReaderPainter.band(context, textRenderer, reader, readerBand, running, head, closeHover,
                    DecorSettings.enabled());
        });
        boolean viewportOpen = sheet.viewport().height() > 0 && sheet.viewport().width() > 0;
        if (viewportOpen) {
            decorator.drawPaperWatermark(context, sheet, pageSet, pageSeed);
            decorator.drawPagePlate(context, sheet, pageSet, pageSeed, articleScroll);
            context.draw(() -> ReaderPainter.content(context, textRenderer, sheet, pageHeader, content,
                    articleScroll, thumbHot, plate));
        }
        if (closeHover) {
            return new Tip(List.of(backTip), -1);
        }
        if (sourceTooltip != null && readerBand.sourceHit(reader).contains(mouseX, mouseY)) {
            return new Tip(List.of(sourceTooltip), -1);
        }
        return null;
    }

    /** Observations only arrive during a round; re-lay rows and the chip when they change.
     * 观察结果只在对局中到达；发生变化时重新排版目录行与徽记。 */
    private void refreshMarks() {
        // The revision check keeps the per-frame cost at one int compare. 版本号比较使逐帧开销只剩一次整数比较。
        int revision = session.observationRevision();
        if (revision == marksRevision) {
            return;
        }
        marksRevision = revision;
        Set<String> roles = session.observedRoleIds();
        Set<String> traits = session.observedTraitIds();
        if (roles.equals(markedRoles) && traits.equals(markedTraits)) {
            return;
        }
        markedRoles = roles;
        markedTraits = traits;
        refreshRows();
        refreshArticle();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean captured = capturesMouse(mouseX, mouseY);
        if (!captured && embedded) {
            searchField.setFocused(false);
            treeFocused = false;
            return false;
        }
        if (button != 0) {
            return captured;
        }
        if (showButton() && frames.button().contains(mouseX, mouseY)) {
            // Collapsed by rule: expand as a modal (existing behaviour). Folded by the user: unfold in place.
            // 按规则收起：以模态方式展开（保持原有行为）；用户收起：原位展开。
            if (frames.collapsed()) {
                directoryExpanded = true;
            } else {
                GuidebookClientState.setDirectoryFolded(false);
            }
            updateInventoryLayout();
            return true;
        }
        if (readerFrameVisible() && ReaderPainter.closeBox(reader).contains(mouseX, mouseY)) {
            if (isArticleOpen()) {
                dismissArticle();
            } else {
                close();
            }
            return true;
        }
        if (directoryVisible()) {
            if (closeInBand() && DirectoryPainter.closeBox(nav).contains(mouseX, mouseY)) {
                close();
                return true;
            }
            if (DirectoryPainter.searchToggle(nav, closeInBand()).contains(mouseX, mouseY)) {
                searchExpanded = !searchExpanded;
                searchField.visible = searchExpanded;
                searchField.setFocused(searchExpanded);
                treeFocused = false;
                if (!searchExpanded && !searchField.getText().isEmpty()) {
                    searchField.setText("");
                } else {
                    refreshRows();
                }
                return true;
            }
            if (searchExpanded && !searchQuery.isEmpty() && DirectoryPainter.clearBox(nav).contains(mouseX, mouseY)) {
                searchField.setText("");
                searchField.setFocused(true);
                treeFocused = false;
                return true;
            }
            if (searchExpanded && DirectoryPainter.well(nav).contains(mouseX, mouseY)) {
                searchField.setFocused(true);
                searchField.mouseClicked(mouseX, mouseY, button);
                treeFocused = false;
                return true;
            }
            searchField.setFocused(false);
            Region view = directoryViewport();
            if (view.contains(mouseX, mouseY)) {
                treeFocused = true;
                if (overDirectoryThumbTrack(mouseX, mouseY)) {
                    draggingDirectory = true;
                    dragScroll(mouseY);
                } else {
                    int index = rowAt(mouseX, mouseY);
                    if (index >= 0) {
                        // Focus always follows the click, so the ring and Enter never point at a stale row; only the
                        // activation waits out the guard.
                        // 焦点始终跟随点击，焦点框与 Enter 不会指向旧的行；只有激活需要等待保护时间结束。
                        focusedRow = index;
                        if (Util.getMeasuringTimeMs() - layoutChangedAt >= ROW_ACTIVATION_GUARD_MS) {
                            activate(rows.get(index));
                        }
                    }
                }
                return true;
            }
            if (foldable() && DirectoryPainter.foldBox(nav).contains(mouseX, mouseY)) {
                GuidebookClientState.setDirectoryFolded(true);
                treeFocused = false;
                updateInventoryLayout();
                return true;
            }
            if (DirectoryPainter.creditsZone(nav, foldable()).contains(mouseX, mouseY)) {
                creditsOpen = true;
                articleScroll = 0;
                if (!updateInventoryLayout()) {
                    refreshArticle();
                }
                return true;
            }
        }
        treeFocused = false;
        if (isArticleOpen() && maxArticleScroll() > 0 && sheet.scrollHit().contains(mouseX, mouseY)) {
            draggingArticle = true;
            dragScroll(mouseY);
        }
        return captured;
    }

    /** Row under the pointer: body only (the root margin is excluded), inside the scissored viewport.
     * 指针下的行：仅计行体（不含根行间隔），且位于裁剪视口内。 */
    private int rowAt(double mouseX, double mouseY) {
        if (!directoryVisible()) {
            return -1;
        }
        Region view = directoryViewport();
        if (!view.contains(mouseX, mouseY) || overDirectoryThumbTrack(mouseX, mouseY)) {
            return -1;
        }
        int y = (int) Math.floor(mouseY) - view.y() + directoryScroll;
        for (int index = 0; index < paintRows.size(); index++) {
            DirectoryPainter.Row row = paintRows.get(index);
            if (y >= row.bodyTop() && y < row.bodyBottom()) {
                return index;
            }
        }
        return -1;
    }

    private void activate(RowSlot slot) {
        GuidebookNavigation.Row node = slot.node();
        if (node.entry() == null) {
            if (!expandedNodes.remove(node.key())) {
                expandedNodes.add(node.key());
            }
            refreshRows();
        } else {
            selectedEntry = node.entry();
            selectedGroup = groupOf(node.key());
            creditsOpen = false;
            articleScroll = 0;
            session.rememberSelection(selectedEntry.tab(), selectedEntry.id());
            if (!updateInventoryLayout()) {
                refreshArticle();
            }
        }
        rememberView();
    }

    /** "group:<g>/<entry>" -> g. 由行键取出分组。 */
    private static String groupOf(String rowKey) {
        int slash = rowKey.indexOf('/');
        return rowKey.startsWith("group:") && slash > 0 ? rowKey.substring("group:".length(), slash) : null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        updateInventoryLayout();
        int amount = (int) Math.round(-vertical * WHEEL_STEP);
        if (directoryVisible() && nav.contains(mouseX, mouseY)) {
            directoryScroll = MathHelper.clamp(directoryScroll + amount, 0, maxDirectoryScroll());
        } else if (isArticleOpen() && reader.contains(mouseX, mouseY)) {
            articleScroll = MathHelper.clamp(articleScroll + amount, 0, maxArticleScroll());
        }
        rememberView();
        return capturesMouse(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && (draggingDirectory || draggingArticle)) {
            dragScroll(mouseY);
            return true;
        }
        return isModal();
    }

    /** Centre the thumb on the pointer; tracks and thumbs match the painters exactly. 滑块中心跟随指针，轨道与绘制一致。 */
    private void dragScroll(double mouseY) {
        if (draggingDirectory) {
            Region view = directoryViewport();
            int thumb = ExpressPaint.thumbSize(view.height(), view.height(), directoryHeight);
            double fraction = (mouseY - view.y() - thumb / 2.0) / Math.max(1, view.height() - thumb);
            directoryScroll = (int) Math.round(MathHelper.clamp(fraction, 0, 1) * maxDirectoryScroll());
        } else {
            Region view = sheet.viewport();
            int track = view.height() - 4;
            int thumb = ExpressPaint.thumbSize(track, view.height(), ReaderPainter.contentHeight(content, plateShown()));
            double fraction = (mouseY - (view.y() + 2) - thumb / 2.0) / Math.max(1, track - thumb);
            articleScroll = (int) Math.round(MathHelper.clamp(fraction, 0, 1) * maxArticleScroll());
        }
        rememberView();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean dragging = draggingDirectory || draggingArticle;
        draggingDirectory = false;
        draggingArticle = false;
        return dragging || isModal();
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        updateInventoryLayout();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (searchField.isFocused()) {
                searchField.setFocused(false);
                return true;
            }
            if (isModal()) {
                dismissArticle();
                return true;
            }
            if (!embedded) {
                close();
                return true;
            }
            treeFocused = false;
            return false;
        }
        if (searchField.isFocused()) {
            searchField.keyPressed(key, scanCode, modifiers);
            return true;
        }
        if (isModal() && client.options.inventoryKey.matchesKey(key, scanCode)) {
            dismissArticle();
            return true;
        }
        // Standalone, the inventory key closes the guide like Esc, so spectators toggle it as they would an inventory.
        // 独立打开时，背包键与 Esc 一样关闭指南，旁观者可像开关背包一样使用。
        if (!embedded && client.options.inventoryKey.matchesKey(key, scanCode)) {
            close();
            return true;
        }
        boolean vertical = key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN;
        if (!embedded && !treeFocused && vertical && directoryVisible() && !rows.isEmpty()) {
            // Standalone: the first arrow focuses the tree (the selected row, else the first) so the ↑↓ hint holds.
            // 独立界面：第一次方向键先聚焦目录（已选中的行，否则第一行），使“↑↓ 浏览”提示成立。
            treeFocused = true;
            focusedRow = Math.max(0, selectedRowIndex());
            revealRow(focusedRow);
            rememberView();
            return true;
        }
        if (treeFocused && !rows.isEmpty() && directoryVisible()) {
            if (vertical) {
                focusedRow = MathHelper.clamp(focusedRow + (key == GLFW.GLFW_KEY_DOWN ? 1 : -1), 0, rows.size() - 1);
                revealRow(focusedRow);
                rememberView();
                return true;
            }
            if (focusedRow >= 0 && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE)) {
                activate(rows.get(focusedRow));
                return true;
            }
        }
        if (isArticleOpen() && (key == GLFW.GLFW_KEY_PAGE_DOWN || key == GLFW.GLFW_KEY_PAGE_UP)) {
            articleScroll = MathHelper.clamp(articleScroll + (key == GLFW.GLFW_KEY_PAGE_DOWN ? 1 : -1)
                    * sheet.viewport().height(), 0, maxArticleScroll());
            rememberView();
            return true;
        }
        if (isArticleOpen() && (key == GLFW.GLFW_KEY_HOME || key == GLFW.GLFW_KEY_END)) {
            articleScroll = key == GLFW.GLFW_KEY_HOME ? 0 : maxArticleScroll();
            rememberView();
            return true;
        }
        return isModal();
    }

    private void revealRow(int index) {
        DirectoryPainter.Row row = paintRows.get(index);
        int viewport = directoryViewport().height();
        if (row.top() < directoryScroll) {
            directoryScroll = row.top();
        } else if (row.bodyBottom() > directoryScroll + viewport) {
            directoryScroll = Math.min(row.top(), row.bodyBottom() - viewport);
        }
        directoryScroll = MathHelper.clamp(directoryScroll, 0, maxDirectoryScroll());
    }

    private int selectedRowIndex() {
        String id = selectedId();
        for (int index = 0; id != null && index < paintRows.size(); index++) {
            if (id.equals(paintRows.get(index).entryId())) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return searchField.isFocused() && searchField.charTyped(chr, modifiers);
    }

    public boolean isArticleOpen() {
        return selectedEntry != null || creditsOpen;
    }

    public boolean isModal() {
        return isArticleOpen() || directoryExpanded;
    }

    public boolean capturesMouse(double x, double y) {
        updateInventoryLayout();
        return isModal() || (showButton() ? frames.button().contains(x, y)
                : directoryVisible() && nav.contains(x, y));
    }

    public boolean capturesKeyboard() {
        updateInventoryLayout();
        return isModal() || searchField.isFocused();
    }

    // State-to-geometry rules live in GuidebookLayout.frames (pure and unit-tested).
    // 状态到几何的规则位于 GuidebookLayout.frames（纯函数，有单元测试）。
    private boolean directoryVisible() {
        return frames.directoryVisible();
    }

    private boolean showButton() {
        return frames.buttonVisible();
    }

    private boolean readerFrameVisible() {
        return frames.readerVisible();
    }

    private boolean closeInBand() {
        return frames.closeInDirectory();
    }

    private boolean foldable() {
        return frames.foldable();
    }

    /** The rightmost 5 px of a scrollable directory drag the thumb instead of hitting rows.
     * 可滚动目录最右侧 5 像素用于拖动滑块，不命中行。 */
    private boolean overDirectoryThumbTrack(double mouseX, double mouseY) {
        return maxDirectoryScroll() > 0 && DirectoryPainter.scrollHit(nav, searchExpanded).contains(mouseX, mouseY);
    }

    private Region directoryViewport() {
        return DirectoryPainter.viewport(nav, searchExpanded);
    }

    private String selectedId() {
        return creditsOpen || selectedEntry == null ? null : selectedEntry.id();
    }

    private void dismissArticle() {
        selectedEntry = null;
        selectedGroup = null;
        creditsOpen = false;
        directoryExpanded = false;
        articleScroll = 0;
        treeFocused = false;
        searchField.setFocused(false);
        session.dismissEntry();
        updateInventoryLayout();
        rememberView();
    }

    private void rememberView() {
        session.rememberExpandedNodes(expandedNodes);
        session.rememberViewPosition(0, directoryScroll, articleScroll);
    }

    @Override
    public void removed() {
        rememberView();
        // Textures come back lazily on the next frame; an embedded guide is reused across inventory opens.
        // 纹理在下一帧惰性重建；嵌入式指南在多次打开背包之间会被复用。
        decorator.close();
    }

    /** The player's own decoration set (directory, card): by role during a round, the train otherwise.
     * 玩家自身的点缀套别（目录、信息卡）：对局中按身份，其余时候是那列火车。 */
    private DecorSet ownerSet() {
        return DecorSetResolver.forRole(session.currentRoleId().orElse(null));
    }

    @Override
    public void close() {
        if (embedded) {
            dismissArticle();
        } else {
            rememberView();
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int maxDirectoryScroll() {
        return Math.max(0, directoryHeight - directoryViewport().height());
    }

    private int maxArticleScroll() {
        return ReaderPainter.maxScroll(sheet, content, plateShown());
    }

    /** Whether the open page carries a chapter plate above its header. 当前页面页眉上方是否有扉画。 */
    private boolean plateShown() {
        return decorator.plateShown(sheet.measure());
    }

    private boolean isMarked(GuidebookEntry entry) {
        return entry.tab() == GuidebookTab.ROLE && markedRoles.contains(entry.id())
                || entry.tab() == GuidebookTab.TRAIT && markedTraits.contains(entry.id());
    }

    private static OrderedText plain(String text) {
        return OrderedText.styledForwardsVisitedString(text, Style.EMPTY);
    }

    private Text chineseText(String key) {
        return Text.literal(chineseString(key));
    }

    private String chineseString(String key) {
        String fallback = Text.translatable(key).getString();
        return chineseTranslations == null ? fallback : chineseTranslations.get(key, fallback);
    }

    /** Everything that invalidates the geometry. 所有会使几何失效的输入。 */
    private record LayoutKey(int width, int height, boolean articleOpen, boolean expanded, boolean folded,
                             Region gap, Region collapsedButton) {
    }

    /** A root (depth 0) or group (depth 1) label: its width and whether a star can appear on it.
     * 根（层级 0）或分组（层级 1）标签：宽度，以及是否可能带星标。 */
    private record ContainerLabel(int depth, int width, boolean containsMarked) {
    }

    /** A tree row before it is fitted to the directory width. 按目录宽度排版之前的目录行。 */
    private record RowSource(GuidebookNavigation.Row node, String name, int top, int bodyTop,
                             GuidebookNavigation.Summary summary, boolean marked) {
    }

    /** A fitted row: its tree node, tooltip lines (empty when none) and painted form.
     * 排版后的行：树节点、提示文字（无提示时为空）与绘制形式。 */
    private record RowSlot(GuidebookNavigation.Row node, List<Text> tip, DirectoryPainter.Row row) {
    }

    /** A tooltip for this frame; {@code row} anchors it to that directory row, -1 follows the pointer.
     * 本帧的提示框；row 表示锚定到该目录行，-1 表示跟随指针。 */
    private record Tip(List<Text> lines, int row) {
    }
}
