package dev.caecorthus.sparkassist.client.guidebook;

import dev.caecorthus.sparkassist.client.guidebook.render.GuidebookContentRenderer;
import dev.caecorthus.sparkassist.guidebook.GuidebookCatalog;
import dev.caecorthus.sparkassist.guidebook.GuidebookEntry;
import dev.caecorthus.sparkassist.guidebook.GuidebookLayout;
import dev.caecorthus.sparkassist.guidebook.GuidebookLayout.Region;
import dev.caecorthus.sparkassist.guidebook.GuidebookNavigation;
import dev.caecorthus.sparkassist.guidebook.GuidebookSearch;
import dev.caecorthus.sparkassist.guidebook.GuidebookSessionState;
import dev.caecorthus.sparkassist.guidebook.GuidebookTab;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookBlock;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookBlockType;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookPage;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookRun;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookTone;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * Left-hand directory and centered reader, also usable inside the live inventory.
 * 左侧目录与居中正文，也可嵌入正在运行的背包界面。
 */
public final class GuidebookScreen extends Screen {
    private static final int TEXT_COLOR = 0xFF252B30;
    private static final int MUTED_COLOR = 0xFF68737A;
    private static final int NAV_TEXT = 0xFFF0F3F4;
    private static final int NAV_MUTED = 0xFFB0BEC2;
    private static final int ACCENT = 0xFF72C5B7;
    private static final int LINE_HEIGHT = 11;

    private final Screen parent;
    private final boolean embedded;
    private final GuidebookSessionState session = GuidebookClientState.session();
    private final Set<String> expandedNodes = new HashSet<>();
    private GuidebookCatalog catalog = GuidebookCatalog.of(List.of());
    private TranslationStorage chineseTranslations;
    private GuidebookLayout layout;
    private List<RenderedRow> rows = List.of();
    private List<OrderedText> titleLines = List.of();
    private List<OrderedText> articleTitle = List.of();
    private List<OrderedText> articleSource = List.of();
    private GuidebookContentRenderer.Layout content = new GuidebookContentRenderer.Layout(List.of(), 0);
    private GuidebookEntry selectedEntry;
    private TextFieldWidget searchField;
    private String searchQuery = "";
    private boolean searchExpanded;
    private boolean creditsOpen;
    private boolean initialized;
    private boolean treeFocused;
    private int focusedRow = -1;
    private int directoryScroll;
    private int articleScroll;
    private int directoryHeight;
    private int toolbarY;
    private Region directoryViewport;
    private Region articleViewport;
    private boolean draggingDirectory;
    private boolean draggingArticle;

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
        layout = GuidebookLayout.compute(width, height);
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
        Region nav = layout.directory();
        titleLines = wrapped(chineseText("screen.sparkassist.guidebook"), nav.width() - 14);
        toolbarY = nav.y() + 9 + titleLines.size() * LINE_HEIGHT + 5;
        directoryViewport = new Region(nav.x() + 4, toolbarY + 21, nav.width() - 8,
                Math.max(20, nav.bottom() - toolbarY - 44));
        searchField = new TextFieldWidget(textRenderer, nav.x() + 6, toolbarY,
                nav.width() - 29, 16, chineseText("guidebook.sparkassist.search"));
        searchField.setMaxLength(64);
        searchField.setPlaceholder(chineseText("guidebook.sparkassist.search"));
        searchField.setText(searchQuery);
        searchField.setChangedListener(value -> {
            searchQuery = value;
            directoryScroll = 0;
            refreshRows();
        });
        searchField.visible = searchExpanded;
        addDrawableChild(searchField);
        refreshRows();
        refreshArticle();
    }

    private void refreshRows() {
        String needle = searchQuery.strip().toLowerCase(Locale.ROOT);
        List<GuidebookNavigation.Row> nodes = GuidebookNavigation.rows(catalog, expandedNodes,
                entry -> matches(entry, needle), !needle.isEmpty());
        List<RenderedRow> rendered = new ArrayList<>();
        int y = 0;
        for (GuidebookNavigation.Row node : nodes) {
            int nameWidth = directoryViewport.width() - 16 - node.depth() * 8;
            List<OrderedText> lines = wrapped(chineseText(node.nameKey()), nameWidth);
            int rowHeight = Math.max(18, lines.size() * LINE_HEIGHT + 7);
            rendered.add(new RenderedRow(node, y, rowHeight, lines));
            y += rowHeight;
        }
        rows = List.copyOf(rendered);
        directoryHeight = y;
        directoryScroll = MathHelper.clamp(directoryScroll, 0, maxDirectoryScroll());
        focusedRow = Math.min(focusedRow, rows.size() - 1);
    }

    private boolean matches(GuidebookEntry entry, String needle) {
        if (GuidebookSearch.matches(entry, chineseString(entry.nameKey()), entry.ownerRoleIds().stream()
                .map(id -> chineseString("announcement.role." + id.substring(id.indexOf(':') + 1))).toList(), needle)) {
            return true;
        }
        return GuidebookNavigation.groupsFor(entry).stream()
                .anyMatch(group -> chineseString(GuidebookNavigation.labelKey(group)).toLowerCase(Locale.ROOT).contains(needle));
    }

    private void refreshArticle() {
        if (!isArticleOpen()) {
            return;
        }
        Region article = layout.article();
        articleTitle = wrapped(chineseText(creditsOpen ? "guidebook.sparkassist.credits.title" : selectedEntry.nameKey()),
                article.width() - 42);
        articleSource = creditsOpen ? List.of() : wrapped(
                Text.literal(chineseString("guidebook.sparkassist.source").formatted(selectedEntry.sourceModId())),
                article.width() - 24);
        int contentY = article.y() + 20 + (articleTitle.size() + articleSource.size()) * LINE_HEIGHT;
        articleViewport = new Region(article.x() + 12, contentY, article.width() - 24,
                Math.max(20, article.bottom() - contentY - 12));
        List<GuidebookBlock> blocks = new ArrayList<>();
        if (creditsOpen) {
            blocks.add(paragraph("guidebook.sparkassist.credits.body"));
        } else if (!selectedEntry.pages().isEmpty()) {
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
        content = GuidebookContentRenderer.layout(new GuidebookPage(blocks), textRenderer,
                articleViewport.width() - 5, this::chineseString);
        articleScroll = MathHelper.clamp(articleScroll, 0, maxArticleScroll());
    }

    private static GuidebookBlock paragraph(String key) {
        return new GuidebookBlock(GuidebookBlockType.PARAGRAPH,
                List.of(GuidebookRun.translated(key, false, false, GuidebookTone.DEFAULT)));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // The inventory remains visible behind the modal scrim; no extra blur or book texture.
        // 背包保留在遮罩后方，不再叠加模糊或书本材质。
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (isArticleOpen() || !embedded) {
            context.fill(0, 0, width, height, isArticleOpen() ? 0xAE000000 : 0x60000000);
        }
        if (directoryVisible()) {
            renderDirectory(context, mouseX, mouseY);
            searchField.visible = searchExpanded;
            super.render(context, mouseX, mouseY, delta);
        } else {
            searchField.visible = false;
        }
        if (isArticleOpen()) {
            renderArticle(context, mouseX, mouseY);
        } else if (!embedded) {
            renderClose(context, new Region(width - 24, 8, 16, 16), mouseX, mouseY);
        }
    }

    private void renderDirectory(DrawContext context, int mouseX, int mouseY) {
        Region nav = layout.directory();
        context.fill(nav.x(), nav.y(), nav.right(), nav.bottom(), 0xEE1D2326);
        drawLines(context, titleLines, nav.x() + 7, nav.y() + 9, NAV_TEXT);
        renderSearchIcon(context, mouseX, mouseY);
        GuidebookEntry hoveredEntry = null;
        context.enableScissor(directoryViewport.x(), directoryViewport.y(), directoryViewport.right(), directoryViewport.bottom());
        for (int index = 0; index < rows.size(); index++) {
            RenderedRow row = rows.get(index);
            int y = directoryViewport.y() + row.y() - directoryScroll;
            if (y + row.height() <= directoryViewport.y() || y >= directoryViewport.bottom()) {
                continue;
            }
            boolean hovered = directoryViewport.contains(mouseX, mouseY) && mouseY >= y && mouseY < y + row.height();
            boolean selected = !creditsOpen && row.node().entry() != null && row.node().entry().equals(selectedEntry);
            if (selected || hovered || treeFocused && focusedRow == index) {
                context.fill(directoryViewport.x(), y, directoryViewport.right() - 4, y + row.height(),
                        selected ? 0xFF344D4B : 0xFF303B3F);
            }
            int x = directoryViewport.x() + 3 + row.node().depth() * 8;
            if (row.node().entry() == null) {
                context.drawText(textRenderer, row.node().expanded() ? "v" : ">", x, y + 4, ACCENT, false);
            } else {
                if (isMarked(row.node().entry())) {
                    context.drawText(textRenderer, "*", x, y + 4, 0xFFF0C869, false);
                } else {
                    context.fill(x + 1, y + 6, x + 3, y + 12, 0xFF000000 | row.node().entry().color());
                }
                if (hovered) {
                    hoveredEntry = row.node().entry();
                }
            }
            drawLines(context, row.lines(), x + 8, y + 4, NAV_TEXT);
        }
        context.disableScissor();
        renderScrollbar(context, directoryViewport, directoryHeight, directoryScroll, NAV_MUTED);
        if (rows.isEmpty()) {
            drawLines(context, wrapped(chineseText("guidebook.sparkassist.search.no_results"), nav.width() - 16),
                    nav.x() + 8, directoryViewport.y() + 4, NAV_MUTED);
        }
        context.drawText(textRenderer, chineseText("guidebook.sparkassist.credits"), nav.x() + 7,
                nav.bottom() - 15, NAV_MUTED, false);
        if (hoveredEntry != null && isMarked(hoveredEntry)) {
            context.drawTooltip(textRenderer, chineseText("guidebook.sparkassist.marked"), mouseX, mouseY);
        }
    }

    private void renderArticle(DrawContext context, int mouseX, int mouseY) {
        Region article = layout.article();
        context.fill(article.x(), article.y(), article.right(), article.bottom(), 0xFFF0F3F4);
        context.fill(article.x(), article.y(), article.x() + 2, article.bottom(), ACCENT);
        drawLines(context, articleTitle, article.x() + 12, article.y() + 12, TEXT_COLOR);
        drawLines(context, articleSource, article.x() + 12,
                article.y() + 15 + articleTitle.size() * LINE_HEIGHT, MUTED_COLOR);
        context.fill(articleViewport.x(), articleViewport.y() - 5, articleViewport.right(), articleViewport.y() - 4, 0xFFCBD4D7);
        context.enableScissor(articleViewport.x(), articleViewport.y(), articleViewport.right(), articleViewport.bottom());
        for (GuidebookContentRenderer.RenderedLine line : content.lines()) {
            context.drawText(textRenderer, line.text(), articleViewport.x() + line.indent(),
                    articleViewport.y() + line.y() - articleScroll, TEXT_COLOR, false);
        }
        context.disableScissor();
        renderScrollbar(context, articleViewport, content.height(), articleScroll, MUTED_COLOR);
        renderClose(context, articleCloseButton(), mouseX, mouseY);
    }

    private void renderClose(DrawContext context, Region button, int mouseX, int mouseY) {
        if (button.contains(mouseX, mouseY)) {
            context.fill(button.x(), button.y(), button.right(), button.bottom(), 0x30405259);
        }
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("×"), button.x() + 8, button.y() + 4,
                isArticleOpen() ? TEXT_COLOR : NAV_TEXT);
        if (button.contains(mouseX, mouseY)) {
            context.drawTooltip(textRenderer, chineseText("gui.back"), mouseX, mouseY);
        }
    }

    private void renderSearchIcon(DrawContext context, int mouseX, int mouseY) {
        Region button = searchButton();
        int x = button.x() + 3;
        int y = button.y() + 3;
        int color = searchExpanded || button.contains(mouseX, mouseY) ? ACCENT : NAV_MUTED;
        context.fill(x + 1, y, x + 6, y + 1, color);
        context.fill(x, y + 1, x + 1, y + 6, color);
        context.fill(x + 6, y + 1, x + 7, y + 6, color);
        context.fill(x + 1, y + 6, x + 6, y + 7, color);
        context.fill(x + 6, y + 6, x + 9, y + 9, color);
        if (button.contains(mouseX, mouseY)) {
            context.drawTooltip(textRenderer, chineseText("guidebook.sparkassist.search"), mouseX, mouseY);
        }
    }

    private void renderScrollbar(DrawContext context, Region viewport, int totalHeight, int scroll, int color) {
        if (totalHeight <= viewport.height()) {
            return;
        }
        int thumb = Math.max(12, viewport.height() * viewport.height() / totalHeight);
        int y = viewport.y() + scroll * (viewport.height() - thumb) / (totalHeight - viewport.height());
        context.fill(viewport.right() - 3, viewport.y(), viewport.right() - 1, viewport.bottom(), 0x3068737A);
        context.fill(viewport.right() - 3, y, viewport.right() - 1, y + thumb, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean captured = capturesMouse(mouseX, mouseY);
        if (button != 0) {
            return captured;
        }
        if (isArticleOpen() && articleCloseButton().contains(mouseX, mouseY)) {
            dismissArticle();
            return true;
        }
        if (!embedded && !isArticleOpen() && new Region(width - 24, 8, 16, 16).contains(mouseX, mouseY)) {
            close();
            return true;
        }
        if (directoryVisible()) {
            if (searchButton().contains(mouseX, mouseY)) {
                searchExpanded = !searchExpanded;
                searchField.visible = searchExpanded;
                searchField.setFocused(searchExpanded);
                if (!searchExpanded) {
                    searchField.setText("");
                }
                treeFocused = false;
                return true;
            }
            if (searchExpanded && searchField.mouseClicked(mouseX, mouseY, button)) {
                treeFocused = false;
                return true;
            }
            searchField.setFocused(false);
            if (directoryViewport.contains(mouseX, mouseY)) {
                treeFocused = true;
                if (mouseX >= directoryViewport.right() - 5 && maxDirectoryScroll() > 0) {
                    draggingDirectory = true;
                    dragScroll(mouseY);
                } else {
                    int y = (int) mouseY - directoryViewport.y() + directoryScroll;
                    for (int i = 0; i < rows.size(); i++) {
                        RenderedRow row = rows.get(i);
                        if (y >= row.y() && y < row.y() + row.height()) {
                            focusedRow = i;
                            activate(row.node());
                            break;
                        }
                    }
                }
                return true;
            }
            Region nav = layout.directory();
            if (new Region(nav.x(), nav.bottom() - 20, nav.width(), 20).contains(mouseX, mouseY)) {
                creditsOpen = true;
                articleScroll = 0;
                refreshArticle();
                return true;
            }
        }
        treeFocused = false;
        if (isArticleOpen() && articleViewport.contains(mouseX, mouseY)
                && mouseX >= articleViewport.right() - 5 && maxArticleScroll() > 0) {
            draggingArticle = true;
            dragScroll(mouseY);
        }
        return captured;
    }

    private void activate(GuidebookNavigation.Row node) {
        if (node.entry() == null) {
            if (!expandedNodes.remove(node.key())) {
                expandedNodes.add(node.key());
            }
            refreshRows();
        } else {
            selectedEntry = node.entry();
            creditsOpen = false;
            articleScroll = 0;
            session.rememberSelection(selectedEntry.tab(), selectedEntry.id());
            refreshArticle();
        }
        rememberView();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int amount = (int) Math.round(-vertical * 22);
        if (directoryVisible() && layout.directory().contains(mouseX, mouseY)) {
            directoryScroll = MathHelper.clamp(directoryScroll + amount, 0, maxDirectoryScroll());
        } else if (isArticleOpen() && layout.article().contains(mouseX, mouseY)) {
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
        return isArticleOpen();
    }

    private void dragScroll(double mouseY) {
        Region viewport = draggingDirectory ? directoryViewport : articleViewport;
        int total = draggingDirectory ? directoryHeight : content.height();
        int thumb = Math.max(12, viewport.height() * viewport.height() / total);
        double fraction = (mouseY - viewport.y() - thumb / 2.0) / Math.max(1, viewport.height() - thumb);
        int scroll = (int) Math.round(MathHelper.clamp(fraction, 0, 1) * (total - viewport.height()));
        if (draggingDirectory) {
            directoryScroll = scroll;
        } else {
            articleScroll = scroll;
        }
        rememberView();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean dragging = draggingDirectory || draggingArticle;
        draggingDirectory = false;
        draggingArticle = false;
        return dragging || isArticleOpen();
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (searchField.isFocused()) {
                searchField.setFocused(false);
                return true;
            }
            if (isArticleOpen()) {
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
        if (isArticleOpen() && client.options.inventoryKey.matchesKey(key, scanCode)) {
            dismissArticle();
            return true;
        }
        if (treeFocused && !rows.isEmpty() && directoryVisible()) {
            if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
                focusedRow = MathHelper.clamp(focusedRow + (key == GLFW.GLFW_KEY_DOWN ? 1 : -1), 0, rows.size() - 1);
                RenderedRow row = rows.get(focusedRow);
                if (row.y() < directoryScroll) {
                    directoryScroll = row.y();
                } else if (row.y() + row.height() > directoryScroll + directoryViewport.height()) {
                    directoryScroll = Math.min(row.y(), row.y() + row.height() - directoryViewport.height());
                }
                directoryScroll = MathHelper.clamp(directoryScroll, 0, maxDirectoryScroll());
                rememberView();
                return true;
            }
            if (focusedRow >= 0 && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE)) {
                activate(rows.get(focusedRow).node());
                return true;
            }
        }
        if (isArticleOpen() && (key == GLFW.GLFW_KEY_PAGE_DOWN || key == GLFW.GLFW_KEY_PAGE_UP)) {
            articleScroll = MathHelper.clamp(articleScroll + (key == GLFW.GLFW_KEY_PAGE_DOWN ? 1 : -1)
                    * articleViewport.height(), 0, maxArticleScroll());
            rememberView();
            return true;
        }
        return isArticleOpen();
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return searchField.isFocused() && searchField.charTyped(chr, modifiers);
    }

    public boolean isArticleOpen() {
        return selectedEntry != null || creditsOpen;
    }

    public boolean capturesMouse(double x, double y) {
        return isArticleOpen() || directoryVisible() && layout.directory().contains(x, y);
    }

    public boolean capturesKeyboard() {
        return isArticleOpen() || searchField.isFocused();
    }

    private boolean directoryVisible() {
        return !layout.compact() || !isArticleOpen();
    }

    private void dismissArticle() {
        selectedEntry = null;
        creditsOpen = false;
        articleScroll = 0;
        treeFocused = false;
        searchField.setFocused(false);
        session.dismissEntry();
        rememberView();
    }

    private void rememberView() {
        session.rememberExpandedNodes(expandedNodes);
        session.rememberViewPosition(0, directoryScroll, articleScroll);
    }

    @Override
    public void removed() {
        rememberView();
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

    private Region articleCloseButton() {
        return new Region(layout.article().right() - 24, layout.article().y() + 8, 16, 16);
    }

    private Region searchButton() {
        return new Region(layout.directory().right() - 22, toolbarY, 16, 16);
    }

    private int maxDirectoryScroll() {
        return Math.max(0, directoryHeight - directoryViewport.height());
    }

    private int maxArticleScroll() {
        return Math.max(0, content.height() - articleViewport.height());
    }

    private boolean isMarked(GuidebookEntry entry) {
        return entry.tab() == GuidebookTab.ROLE && session.observedRoleIds().contains(entry.id())
                || entry.tab() == GuidebookTab.TRAIT && session.observedTraitIds().contains(entry.id());
    }

    private List<OrderedText> wrapped(Text text, int width) {
        return textRenderer.wrapLines(text, Math.max(12, width));
    }

    private void drawLines(DrawContext context, List<OrderedText> lines, int x, int y, int color) {
        for (OrderedText line : lines) {
            context.drawText(textRenderer, line, x, y, color, false);
            y += LINE_HEIGHT;
        }
    }

    private Text chineseText(String key) {
        return Text.literal(chineseString(key));
    }

    private String chineseString(String key) {
        String fallback = Text.translatable(key).getString();
        return chineseTranslations == null ? fallback : chineseTranslations.get(key, fallback);
    }

    private record RenderedRow(GuidebookNavigation.Row node, int y, int height, List<OrderedText> lines) {
    }
}
