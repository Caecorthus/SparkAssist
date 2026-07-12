package dev.caecorthus.sparkassist.client.guidebook;

import dev.caecorthus.sparkassist.SparkAssist;
import dev.caecorthus.sparkassist.guidebook.GuidebookCatalog;
import dev.caecorthus.sparkassist.guidebook.GuidebookEntry;
import dev.caecorthus.sparkassist.guidebook.GuidebookSearch;
import dev.caecorthus.sparkassist.guidebook.GuidebookSessionState;
import dev.caecorthus.sparkassist.guidebook.GuidebookTab;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Responsive, data-backed guidebook screen with mirrored tabs and personal round markers.
 * 支持响应式布局、镜像书签与本局个人标记的数据化指南书界面。
 */
public final class GuidebookScreen extends Screen {
    private static final Identifier BOOK_FRAME = SparkAssist.id("textures/gui/guidebook/book_frame.png");
    private static final Identifier BOOK_PAGES = SparkAssist.id("textures/gui/guidebook/book_pages.png");
    private static final Identifier TAB_SELECTED = SparkAssist.id("textures/gui/guidebook/tab_selected.png");
    private static final Identifier TAB_UNSELECTED = SparkAssist.id("textures/gui/guidebook/tab_unselected.png");
    private static final Identifier CLOSE = SparkAssist.id("textures/gui/guidebook/navigation/close.png");
    private static final Identifier CLOSE_HOVERED = SparkAssist.id("textures/gui/guidebook/navigation/close_hovered.png");
    private static final Identifier PREVIOUS = SparkAssist.id("textures/gui/guidebook/navigation/previous.png");
    private static final Identifier PREVIOUS_HOVERED = SparkAssist.id("textures/gui/guidebook/navigation/previous_hovered.png");
    private static final Identifier PREVIOUS_DISABLED = SparkAssist.id("textures/gui/guidebook/navigation/previous_disabled.png");
    private static final Identifier NEXT = SparkAssist.id("textures/gui/guidebook/navigation/next.png");
    private static final Identifier NEXT_HOVERED = SparkAssist.id("textures/gui/guidebook/navigation/next_hovered.png");
    private static final Identifier NEXT_DISABLED = SparkAssist.id("textures/gui/guidebook/navigation/next_disabled.png");

    private static final int BOOK_TEXTURE_WIDTH = 640;
    private static final int BOOK_TEXTURE_HEIGHT = 414;
    private static final int MAX_BOOK_WIDTH = 420;
    private static final int TAB_WIDTH = 48;
    private static final int TAB_HEIGHT = 19;
    private static final int TAB_GAP = 2;
    private static final int ROW_HEIGHT = 13;
    private static final int NAV_WIDTH = 23;
    private static final int NAV_HEIGHT = 13;
    private static final int CLOSE_SIZE = 12;
    private static final int TEXT_COLOR = 0xFF3B2A1A;
    private static final int MUTED_COLOR = 0xFF8A765D;
    private static final int HOVER_COLOR = 0x334C2E25;
    private static final int SELECTED_COLOR = 0x553C211C;
    private static final int STAR_COLOR = 0xFFF2B84B;

    private final Screen parent;
    private final GuidebookSessionState session = GuidebookClientState.session();

    private GuidebookCatalog catalog = GuidebookCatalog.of(List.of());
    private TranslationStorage chineseTranslations;
    private List<GuidebookEntry> visibleEntries = List.of();
    private GuidebookTab activeTab = GuidebookTab.ROLE;
    private GuidebookEntry selectedEntry;
    private int selectedPage;
    private int leftScroll;
    private int leftContentHeight;
    private int rightScroll;
    private int rightContentHeight;
    private boolean searchExpanded;
    private boolean creditsOpen;
    private String searchQuery = "";
    private TextFieldWidget searchField;

    private int bookX;
    private int bookY;
    private int bookWidth;
    private int bookHeight;
    private int leftPageX;
    private int leftPageY;
    private int leftPageWidth;
    private int leftPageHeight;
    private int rightPageX;
    private int rightPageY;
    private int rightPageWidth;
    private int rightPageHeight;

    public GuidebookScreen(Screen parent) {
        super(Text.translatable("screen.sparkassist.guidebook"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        computeLayout();
        catalog = GuidebookRuntimeCatalog.load(this.client);
        // Guidebook copy stays Chinese even when the rest of the client uses another locale.
        // 指南书文本固定使用中文，不跟随客户端其余界面的语言。
        chineseTranslations = TranslationStorage.load(this.client.getResourceManager(), List.of("zh_cn"), false);

        activeTab = session.selectedTab().orElse(activeTab);
        session.selectedEntryId().flatMap(catalog::find).ifPresent(entry -> selectedEntry = entry);
        selectedPage = session.selectedPage();
        leftScroll = session.leftScroll();
        rightScroll = session.rightScroll();
        session.consumeRoleAutoSelection().flatMap(catalog::find).ifPresent(entry -> {
            activeTab = GuidebookTab.ROLE;
            selectEntry(entry);
        });

        searchField = new TextFieldWidget(
                this.textRenderer,
                leftPageX + 3,
                leftPageY + 15,
                Math.max(40, leftPageWidth - 20),
                15,
                Text.translatable("guidebook.sparkassist.search")
        );
        searchField.setMaxLength(64);
        searchField.setPlaceholder(Text.translatable("guidebook.sparkassist.search.placeholder"));
        searchField.setText(searchQuery);
        searchField.setChangedListener(value -> {
            searchQuery = value;
            leftScroll = 0;
            refreshEntries();
        });
        searchField.visible = searchExpanded;
        this.addDrawableChild(searchField);

        refreshEntries();
        if (selectedEntry == null && !visibleEntries.isEmpty()) {
            selectEntry(visibleEntries.getFirst());
        }
    }

    private void computeLayout() {
        int availableWidth = Math.max(220, this.width - 96);
        int availableHeight = Math.max(142, this.height - 18);
        int heightLimitedWidth = availableHeight * BOOK_TEXTURE_WIDTH / BOOK_TEXTURE_HEIGHT;
        bookWidth = Math.min(MAX_BOOK_WIDTH, Math.min(availableWidth, heightLimitedWidth));
        bookHeight = bookWidth * BOOK_TEXTURE_HEIGHT / BOOK_TEXTURE_WIDTH;
        bookX = (this.width - bookWidth) / 2;
        bookY = (this.height - bookHeight) / 2;

        int half = bookWidth / 2;
        int horizontalMargin = Math.max(11, bookWidth / 28);
        int verticalMargin = Math.max(10, bookHeight / 18);
        int spineGap = Math.max(7, bookWidth / 48);
        leftPageX = bookX + horizontalMargin;
        leftPageY = bookY + verticalMargin;
        leftPageWidth = half - horizontalMargin - spineGap;
        leftPageHeight = bookHeight - verticalMargin * 2;
        rightPageX = bookX + half + spineGap;
        rightPageY = leftPageY;
        rightPageWidth = half - horizontalMargin - spineGap;
        rightPageHeight = leftPageHeight;
    }

    private void refreshEntries() {
        visibleEntries = catalog.entries().stream()
                .filter(entry -> entry.tab() == activeTab)
                .filter(entry -> GuidebookSearch.matches(
                        entry,
                        chineseString(entry.nameKey()),
                        localizedOwnerRoleNames(entry),
                        searchQuery
                ))
                .toList();
        leftContentHeight = visibleEntries.size() * ROW_HEIGHT;
        leftScroll = MathHelper.clamp(leftScroll, 0, maxLeftScroll());

        if (selectedEntry != null && !visibleEntries.contains(selectedEntry)) {
            selectedEntry = null;
        }
        if (selectedEntry == null && !visibleEntries.isEmpty() && !creditsOpen) {
            selectEntry(visibleEntries.getFirst());
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // The book supplies its own backdrop; avoid vanilla blur over its page texture.
        // 书本已有完整背景，不叠加原版模糊层以免纸张发灰。
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xB0000000);
        context.drawTexture(
                BOOK_FRAME,
                bookX,
                bookY,
                bookWidth,
                bookHeight,
                0,
                0,
                BOOK_TEXTURE_WIDTH,
                BOOK_TEXTURE_HEIGHT,
                BOOK_TEXTURE_WIDTH,
                BOOK_TEXTURE_HEIGHT
        );
        renderTabs(context, mouseX, mouseY);
        context.drawTexture(
                BOOK_PAGES,
                bookX,
                bookY,
                bookWidth,
                bookHeight,
                0,
                0,
                BOOK_TEXTURE_WIDTH,
                BOOK_TEXTURE_HEIGHT,
                BOOK_TEXTURE_WIDTH,
                BOOK_TEXTURE_HEIGHT
        );
        renderLeftPage(context, mouseX, mouseY);
        renderRightPage(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
        renderCloseButton(context, mouseX, mouseY);
    }

    private void renderTabs(DrawContext context, int mouseX, int mouseY) {
        renderTab(context, GuidebookTab.ROLE, leftTabX(), tabY(0), mouseX, mouseY);
        renderTab(context, GuidebookTab.FACTION, leftTabX(), tabY(1), mouseX, mouseY);
        renderTab(context, GuidebookTab.SKILL, rightTabX(), tabY(0), mouseX, mouseY);
        renderTab(context, GuidebookTab.TRAIT, rightTabX(), tabY(1), mouseX, mouseY);
    }

    private void renderTab(
            DrawContext context,
            GuidebookTab tab,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        boolean selected = activeTab == tab;
        boolean hovered = inside(mouseX, mouseY, x, y, TAB_WIDTH, TAB_HEIGHT);
        Identifier texture = selected || hovered ? TAB_SELECTED : TAB_UNSELECTED;
        if (tab == GuidebookTab.SKILL || tab == GuidebookTab.TRAIT) {
            context.getMatrices().push();
            context.getMatrices().translate(x + TAB_WIDTH, 0, 0);
            context.getMatrices().scale(-1.0F, 1.0F, 1.0F);
            context.drawTexture(texture, 0, y, 0, 0, TAB_WIDTH, TAB_HEIGHT, TAB_WIDTH, TAB_HEIGHT);
            context.getMatrices().pop();
        } else {
            context.drawTexture(texture, x, y, 0, 0, TAB_WIDTH, TAB_HEIGHT, TAB_WIDTH, TAB_HEIGHT);
        }
        Text label = Text.translatable(tabTranslationKey(tab));
        String labelText = this.textRenderer.trimToWidth(label.getString(), TAB_WIDTH - 6);
        int textX = x + (TAB_WIDTH - this.textRenderer.getWidth(labelText)) / 2;
        context.drawText(this.textRenderer, labelText, textX, y + 6, 0xFFF1D9B0, true);
    }

    private void renderLeftPage(DrawContext context, int mouseX, int mouseY) {
        Text tabTitle = Text.translatable(tabTranslationKey(activeTab));
        context.drawText(this.textRenderer, tabTitle, leftPageX + 3, leftPageY + 3, TEXT_COLOR, false);
        renderSearchIcon(context, mouseX, mouseY);

        int listY = listTop();
        int listHeight = listHeight();
        context.enableScissor(leftPageX, listY, leftPageX + leftPageWidth, listY + listHeight);
        GuidebookEntry hoveredEntry = null;
        int y = listY - leftScroll;
        for (GuidebookEntry entry : visibleEntries) {
            if (y + ROW_HEIGHT >= listY && y <= listY + listHeight) {
                boolean hovered = inside(mouseX, mouseY, leftPageX + 2, y, leftPageWidth - 4, ROW_HEIGHT);
                if (entry.equals(selectedEntry) && !creditsOpen) {
                    context.fill(leftPageX + 2, y, leftPageX + leftPageWidth - 2, y + ROW_HEIGHT, SELECTED_COLOR);
                } else if (hovered) {
                    context.fill(leftPageX + 2, y, leftPageX + leftPageWidth - 2, y + ROW_HEIGHT, HOVER_COLOR);
                }

                boolean marked = isMarked(entry);
                int availableNameWidth = leftPageWidth - 10 - (marked ? 10 : 0);
                String fullName = chineseString(entry.nameKey());
                String name = this.textRenderer.trimToWidth(fullName, Math.max(12, availableNameWidth));
                context.drawText(this.textRenderer, name, leftPageX + 5, y + 3, TEXT_COLOR, false);
                if (marked) {
                    context.drawText(
                            this.textRenderer,
                            Text.literal("\u2605"),
                            leftPageX + leftPageWidth - 11,
                            y + 3,
                            STAR_COLOR,
                            true
                    );
                }
                if (hovered) {
                    hoveredEntry = entry;
                }
            }
            y += ROW_HEIGHT;
        }
        context.disableScissor();

        if (visibleEntries.isEmpty()) {
            Text empty = Text.translatable(searchQuery.isBlank()
                    ? "guidebook.sparkassist.empty"
                    : "guidebook.sparkassist.search.no_results");
            drawWrapped(context, empty, leftPageX + 4, listY + 5, leftPageWidth - 8, MUTED_COLOR, 0);
        }

        String credits = this.textRenderer.trimToWidth(
                Text.translatable("guidebook.sparkassist.credits").getString(),
                leftPageWidth - 6
        );
        int creditsY = leftPageY + leftPageHeight - this.textRenderer.fontHeight;
        int creditsColor = creditsOpen || inside(mouseX, mouseY, leftPageX + 3, creditsY, this.textRenderer.getWidth(credits), 10)
                ? TEXT_COLOR
                : MUTED_COLOR;
        context.drawText(this.textRenderer, credits, leftPageX + 3, creditsY, creditsColor, false);

        if (hoveredEntry != null) {
            List<Text> tooltip = new ArrayList<>();
            tooltip.add(chineseText(hoveredEntry.nameKey()));
            if (isMarked(hoveredEntry)) {
                tooltip.add(Text.translatable("guidebook.sparkassist.marked").withColor(STAR_COLOR));
            }
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }
    }

    private void renderSearchIcon(DrawContext context, int mouseX, int mouseY) {
        int x = searchIconX();
        int y = searchIconY();
        boolean hovered = inside(mouseX, mouseY, x - 2, y - 2, 13, 13);
        int color = hovered || searchExpanded ? TEXT_COLOR : MUTED_COLOR;
        context.fill(x + 1, y, x + 6, y + 1, color);
        context.fill(x, y + 1, x + 1, y + 6, color);
        context.fill(x + 6, y + 1, x + 7, y + 6, color);
        context.fill(x + 1, y + 6, x + 6, y + 7, color);
        context.fill(x + 6, y + 6, x + 8, y + 8, color);
        context.fill(x + 8, y + 8, x + 10, y + 10, color);
    }

    private void renderRightPage(DrawContext context, int mouseX, int mouseY) {
        if (creditsOpen) {
            renderCredits(context);
            return;
        }
        if (selectedEntry == null) {
            drawWrapped(
                    context,
                    Text.translatable("guidebook.sparkassist.select_entry"),
                    rightPageX + 4,
                    rightPageY + 18,
                    rightPageWidth - 8,
                    MUTED_COLOR,
                    0
            );
            return;
        }

        String title = chineseString(selectedEntry.nameKey());
        title = this.textRenderer.trimToWidth(title, Math.max(20, rightPageWidth - 23));
        context.drawText(this.textRenderer, title, rightPageX + 3, rightPageY + 3, TEXT_COLOR, false);
        String source = Text.translatable("guidebook.sparkassist.source", selectedEntry.sourceModId()).getString();
        source = this.textRenderer.trimToWidth(source, Math.max(20, rightPageWidth - 6));
        context.drawText(this.textRenderer, source, rightPageX + 3, rightPageY + 15, MUTED_COLOR, false);

        List<String> pages = selectedEntry.pageKeys().isEmpty()
                ? List.of(selectedEntry.summaryKey())
                : selectedEntry.pageKeys();
        selectedPage = MathHelper.clamp(selectedPage, 0, pages.size() - 1);
        int contentY = rightPageY + 31;
        int contentHeight = Math.max(20, rightPageHeight - 49);
        List<OrderedText> lines = wrappedLines(chineseText(pages.get(selectedPage)), rightPageWidth - 8);
        rightContentHeight = lines.size() * (this.textRenderer.fontHeight + 2);
        rightScroll = MathHelper.clamp(rightScroll, 0, maxRightScroll(contentHeight));

        context.enableScissor(rightPageX, contentY, rightPageX + rightPageWidth, contentY + contentHeight);
        int y = contentY - rightScroll;
        for (OrderedText line : lines) {
            context.drawText(this.textRenderer, line, rightPageX + 4, y, TEXT_COLOR, false);
            y += this.textRenderer.fontHeight + 2;
        }
        context.disableScissor();
        renderPageNavigation(context, mouseX, mouseY, pages.size());
    }

    private void renderCredits(DrawContext context) {
        String title = Text.translatable("guidebook.sparkassist.credits.title").getString();
        title = this.textRenderer.trimToWidth(title, Math.max(20, rightPageWidth - CLOSE_SIZE - 5));
        context.drawText(this.textRenderer, title, rightPageX + 3, rightPageY + 3, TEXT_COLOR, false);
        int contentY = rightPageY + 19;
        int contentHeight = Math.max(20, rightPageHeight - 23);
        List<OrderedText> lines = wrappedLines(
                Text.translatable("guidebook.sparkassist.credits.body"),
                rightPageWidth - 8
        );
        rightContentHeight = lines.size() * (this.textRenderer.fontHeight + 2);
        rightScroll = MathHelper.clamp(rightScroll, 0, maxRightScroll(contentHeight));
        context.enableScissor(rightPageX, contentY, rightPageX + rightPageWidth, contentY + contentHeight);
        int y = contentY - rightScroll;
        for (OrderedText line : lines) {
            context.drawText(this.textRenderer, line, rightPageX + 4, y, TEXT_COLOR, false);
            y += this.textRenderer.fontHeight + 2;
        }
        context.disableScissor();
    }

    private void renderPageNavigation(DrawContext context, int mouseX, int mouseY, int pageCount) {
        int y = navY();
        int previousX = previousX();
        int nextX = nextX();
        boolean hasPrevious = selectedPage > 0;
        boolean hasNext = selectedPage < pageCount - 1;
        boolean previousHovered = hasPrevious && inside(mouseX, mouseY, previousX, y, NAV_WIDTH, NAV_HEIGHT);
        boolean nextHovered = hasNext && inside(mouseX, mouseY, nextX, y, NAV_WIDTH, NAV_HEIGHT);
        Identifier previousTexture = hasPrevious ? (previousHovered ? PREVIOUS_HOVERED : PREVIOUS) : PREVIOUS_DISABLED;
        Identifier nextTexture = hasNext ? (nextHovered ? NEXT_HOVERED : NEXT) : NEXT_DISABLED;
        context.drawTexture(previousTexture, previousX, y, 0, 0, NAV_WIDTH, NAV_HEIGHT, NAV_WIDTH, NAV_HEIGHT);
        context.drawTexture(nextTexture, nextX, y, 0, 0, NAV_WIDTH, NAV_HEIGHT, NAV_WIDTH, NAV_HEIGHT);

        Text indicator = Text.translatable("guidebook.sparkassist.page", selectedPage + 1, pageCount);
        int indicatorX = rightPageX + (rightPageWidth - this.textRenderer.getWidth(indicator)) / 2;
        context.drawText(this.textRenderer, indicator, indicatorX, y + 3, MUTED_COLOR, false);
    }

    private void renderCloseButton(DrawContext context, int mouseX, int mouseY) {
        Identifier texture = inside(mouseX, mouseY, closeX(), closeY(), CLOSE_SIZE, CLOSE_SIZE)
                ? CLOSE_HOVERED
                : CLOSE;
        context.drawTexture(texture, closeX(), closeY(), 0, 0, CLOSE_SIZE, CLOSE_SIZE, CLOSE_SIZE, CLOSE_SIZE);
    }

    private void drawWrapped(
            DrawContext context,
            Text text,
            int x,
            int y,
            int width,
            int color,
            int scroll
    ) {
        int lineY = y - scroll;
        for (OrderedText line : wrappedLines(text, width)) {
            context.drawText(this.textRenderer, line, x, lineY, color, false);
            lineY += this.textRenderer.fontHeight + 2;
        }
    }

    private List<OrderedText> wrappedLines(Text text, int width) {
        List<OrderedText> lines = new ArrayList<>();
        String[] paragraphs = text.getString().split("\\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add(Text.empty().asOrderedText());
            } else {
                lines.addAll(this.textRenderer.wrapLines(Text.literal(paragraph), Math.max(20, width)));
            }
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        int x = (int) mouseX;
        int y = (int) mouseY;

        if (inside(x, y, closeX(), closeY(), CLOSE_SIZE, CLOSE_SIZE)) {
            close();
            return true;
        }
        GuidebookTab clickedTab = tabAt(x, y);
        if (clickedTab != null) {
            activeTab = clickedTab;
            selectedEntry = null;
            selectedPage = 0;
            leftScroll = 0;
            rightScroll = 0;
            creditsOpen = false;
            refreshEntries();
            return true;
        }
        if (inside(x, y, searchIconX() - 2, searchIconY() - 2, 13, 13)) {
            searchExpanded = !searchExpanded;
            searchField.visible = searchExpanded;
            if (searchExpanded) {
                this.setFocused(searchField);
                searchField.setFocused(true);
            } else {
                searchField.setText("");
                searchField.setFocused(false);
                this.setFocused(null);
            }
            return true;
        }

        String credits = this.textRenderer.trimToWidth(
                Text.translatable("guidebook.sparkassist.credits").getString(),
                leftPageWidth - 6
        );
        int creditsY = leftPageY + leftPageHeight - this.textRenderer.fontHeight;
        if (inside(x, y, leftPageX + 3, creditsY, this.textRenderer.getWidth(credits), 10)) {
            creditsOpen = true;
            rightScroll = 0;
            return true;
        }

        if (inside(x, y, leftPageX, listTop(), leftPageWidth, listHeight())) {
            int index = (y - listTop() + leftScroll) / ROW_HEIGHT;
            if (index >= 0 && index < visibleEntries.size()) {
                selectEntry(visibleEntries.get(index));
                return true;
            }
        }

        if (!creditsOpen && selectedEntry != null) {
            int pageCount = Math.max(1, selectedEntry.pageKeys().size());
            if (inside(x, y, previousX(), navY(), NAV_WIDTH, NAV_HEIGHT) && selectedPage > 0) {
                selectedPage--;
                rightScroll = 0;
                rememberViewPosition();
                return true;
            }
            if (inside(x, y, nextX(), navY(), NAV_WIDTH, NAV_HEIGHT) && selectedPage < pageCount - 1) {
                selectedPage++;
                rightScroll = 0;
                rememberViewPosition();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int amount = (int) Math.round(-verticalAmount * ROW_HEIGHT);
        if (inside((int) mouseX, (int) mouseY, leftPageX, listTop(), leftPageWidth, listHeight())) {
            leftScroll = MathHelper.clamp(leftScroll + amount, 0, maxLeftScroll());
            rememberViewPosition();
            return true;
        }
        if (inside((int) mouseX, (int) mouseY, rightPageX, rightPageY, rightPageWidth, rightPageHeight)) {
            int contentHeight = Math.max(20, rightPageHeight - (creditsOpen ? 23 : 49));
            rightScroll = MathHelper.clamp(rightScroll + amount, 0, maxRightScroll(contentHeight));
            rememberViewPosition();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void selectEntry(GuidebookEntry entry) {
        selectedEntry = entry;
        selectedPage = 0;
        rightScroll = 0;
        creditsOpen = false;
        session.rememberSelection(entry.tab(), entry.id());
        rememberViewPosition();
    }

    private void rememberViewPosition() {
        session.rememberViewPosition(selectedPage, leftScroll, rightScroll);
    }

    private List<String> localizedOwnerRoleNames(GuidebookEntry entry) {
        return entry.ownerRoleIds().stream().map(roleId -> {
            int separator = roleId.indexOf(':');
            String path = separator >= 0 ? roleId.substring(separator + 1) : roleId;
            return chineseString("announcement.role." + path);
        }).toList();
    }

    private Text chineseText(String translationKey) {
        return Text.literal(chineseString(translationKey));
    }

    private String chineseString(String translationKey) {
        String fallback = Text.translatable(translationKey).getString();
        return chineseTranslations == null ? fallback : chineseTranslations.get(translationKey, fallback);
    }

    private boolean isMarked(GuidebookEntry entry) {
        return entry.tab() == GuidebookTab.ROLE && session.observedRoleIds().contains(entry.id())
                || entry.tab() == GuidebookTab.TRAIT && session.observedTraitIds().contains(entry.id());
    }

    private int listTop() {
        return leftPageY + (searchExpanded ? 34 : 18);
    }

    private int listHeight() {
        return Math.max(20, leftPageY + leftPageHeight - this.textRenderer.fontHeight - 4 - listTop());
    }

    private int maxLeftScroll() {
        return Math.max(0, leftContentHeight - listHeight());
    }

    private int maxRightScroll(int contentHeight) {
        return Math.max(0, rightContentHeight - contentHeight);
    }

    private int leftTabX() {
        return bookX - TAB_WIDTH + 9;
    }

    private int rightTabX() {
        return bookX + bookWidth - 9;
    }

    private int tabY(int index) {
        return bookY + 17 + index * (TAB_HEIGHT + TAB_GAP);
    }

    private GuidebookTab tabAt(int mouseX, int mouseY) {
        if (inside(mouseX, mouseY, leftTabX(), tabY(0), TAB_WIDTH, TAB_HEIGHT)) {
            return GuidebookTab.ROLE;
        }
        if (inside(mouseX, mouseY, leftTabX(), tabY(1), TAB_WIDTH, TAB_HEIGHT)) {
            return GuidebookTab.FACTION;
        }
        if (inside(mouseX, mouseY, rightTabX(), tabY(0), TAB_WIDTH, TAB_HEIGHT)) {
            return GuidebookTab.SKILL;
        }
        if (inside(mouseX, mouseY, rightTabX(), tabY(1), TAB_WIDTH, TAB_HEIGHT)) {
            return GuidebookTab.TRAIT;
        }
        return null;
    }

    private int searchIconX() {
        return leftPageX + leftPageWidth - 13;
    }

    private int searchIconY() {
        return leftPageY + 3;
    }

    private int closeX() {
        return rightPageX + rightPageWidth - CLOSE_SIZE - 1;
    }

    private int closeY() {
        return rightPageY + 1;
    }

    private int navY() {
        return rightPageY + rightPageHeight - NAV_HEIGHT - 1;
    }

    private int previousX() {
        return rightPageX + 4;
    }

    private int nextX() {
        return rightPageX + rightPageWidth - NAV_WIDTH - 4;
    }

    private static String tabTranslationKey(GuidebookTab tab) {
        return "guidebook.sparkassist.tab." + tab.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (this.client != null) {
            rememberViewPosition();
            this.client.setScreen(parent);
        }
    }
}
