package dev.caecorthus.sparkassist.client.achievement;

import dev.caecorthus.sparkassist.achievement.Achievement;
import dev.caecorthus.sparkassist.achievement.AchievementGalaxy;
import dev.caecorthus.sparkassist.achievement.AchievementLedger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.advancement.AdvancementObtainedStatus;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

/**
 * The achievement page, full screen: the {@link AchievementGalaxy} on a starry sky, dragged to move and scrolled to
 * zoom. The advancement tabs run along the top, so another tab is one click away; picking one goes back to the
 * vanilla screen on that tab. Hovering a star shows its title, level and condition, and where it was earned; a star
 * not earned yet shows "???" for its title.
 * 全屏的成就页：星空上的 {@link AchievementGalaxy}，拖动平移、滚轮缩放。进度标签页排在顶部，点其他标签页会回到原版界面的那一页。
 * 鼠标悬停在成就上会显示标题、等级、条件以及获得地点；尚未达成的成就标题显示为“???”。
 */
public final class AchievementGalaxyScreen extends Screen {
    public static final int SPACE = 0xFF07060D;
    // Vanilla advancement sizes. 原版成就界面的尺寸。
    private static final int STAR_SIZE = 26;
    private static final int ICON_SIZE = 16;
    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 32;
    private static final int TAB_GAP = 4;
    private static final Identifier TAB = Identifier.ofVanilla("advancements/tab_above_middle");
    private static final Identifier TAB_SELECTED = Identifier.ofVanilla("advancements/tab_above_middle_selected");
    // Room for the tabs above the galaxy and the Done button below it. 银河上方标签页与下方“完成”按钮占的高度。
    private static final int TOP = TAB_HEIGHT + 4;
    private static final int BOTTOM = 32;
    private static final int TOOLTIP_WIDTH = 200;
    private static final int HINT_X = 6;
    // Low enough for the whole galaxy to fit a 1280×720 window at its automatic GUI scale. 足以在自动界面缩放的 1280×720 窗口中装下整个银河。
    private static final double MIN_ZOOM = 0.2;
    private static final double MAX_ZOOM = 2.5;
    private static final double ZOOM_STEP = 1.15;
    // Drawn far back so the tabs, button and tooltips always cover it. 画在很靠后的深度，标签页、按钮和提示框总能盖住它。
    private static final float GALAXY_Z = -1000;
    private static final int DIM_Z = 200;
    private static final int LIT_LINK = 0xA0F2C14E;
    private static final int DIM_LINK = 0x80A8A8B8;
    private static final int HALO = 0x40F2C14E;
    private static final int NOT_EARNED_ICON = 0x99000000;
    private static final double LIT_WIDTH = 1;
    private static final double DIM_WIDTH = 1;
    private static final double DASH = 4;
    private static final double DASH_GAP = 3;
    private static final String LEVEL_KEY = "advancements.sparkassist.achievement.level";
    private static final String PLACE_KEY = "advancements.sparkassist.achievement.place";
    private static final String LOCAL_WORLD_KEY = "advancements.sparkassist.achievement.place.local_world";
    private static final String HIDDEN_TITLE = "???";
    private static final String NOT_YET_KEY = "advancements.sparkassist.achievement.not_yet";
    private static final String HINT_KEY = "gui.sparkassist.achievements.hint";
    private static final Sky SKY = new Sky(480, 220, 0.15, 20260101L);

    // The camera survives closing the screen for the rest of the session. 关闭界面后镜头位置保留到本次游戏结束。
    private static boolean cameraPlaced;
    private static double cameraX;
    private static double cameraY;
    private static double zoom = 1;

    private final ClientAdvancementManager advancements;
    private final Screen parent;
    private AchievementGalaxy.View view;

    public AchievementGalaxyScreen(ClientAdvancementManager advancements, Screen parent) {
        super(Text.translatable("gui.advancements"));
        this.advancements = advancements;
        this.parent = parent;
    }

    @Override
    protected void init() {
        refresh();
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(width / 2 - 100, height - BOTTOM + 6, 200, 20)
                .build());
        if (!cameraPlaced) {
            cameraPlaced = true;
            double reach = AchievementGalaxy.radius() + STAR_SIZE;
            zoom = MathHelper.clamp(Math.min(width - 16, height - TOP - BOTTOM) / (2 * reach), MIN_ZOOM, 1);
        }
        clampCamera();
    }

    @Override
    public void tick() {
        // A round can settle while the page is open. 成就页打开时也可能有对局结算。
        refresh();
    }

    private void refresh() {
        view = AchievementGalaxy.view(AchievementStorage.ledger(client));
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    // ---- Drawing 绘制 ----

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, GALAXY_Z);
        context.fill(0, 0, width, height, SPACE);
        SKY.draw(context, width, height, cameraX * zoom, cameraY * zoom);
        drawGalaxy(context);
        context.getMatrices().pop();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        Text hint = Text.translatable(HINT_KEY).formatted(Formatting.DARK_GRAY);
        // Only where it clears the Done button. 只在不挡住“完成”按钮时显示。
        if (HINT_X + textRenderer.getWidth(hint) + 4 <= width / 2 - 100) {
            context.drawTextWithShadow(textRenderer, hint, HINT_X, height - BOTTOM + 12, -1);
        }
        PlacedAdvancement hoveredTab = drawTabs(context, mouseX, mouseY);
        if (hoveredTab != null) {
            hoveredTab.getAdvancement().display()
                    .ifPresent(display -> context.drawTooltip(textRenderer, display.getTitle(), mouseX, mouseY));
        } else if (mouseY > TOP && mouseY < height - BOTTOM) {
            starAt(mouseX, mouseY).ifPresent(star -> context.drawOrderedTooltip(textRenderer, tooltip(star), mouseX, mouseY));
        }
    }

    private void drawGalaxy(DrawContext context) {
        context.getMatrices().push();
        context.getMatrices().translate(centerX(), centerY(), 0);
        context.getMatrices().scale((float) zoom, (float) zoom, 1);
        context.getMatrices().translate((float) -cameraX, (float) -cameraY, 0);

        for (AchievementGalaxy.Link link : view.links()) {
            AchievementGalaxy.Star from = link.from();
            AchievementGalaxy.Star to = link.to();
            if (link.lit()) {
                line(context, from.x(), from.y(), to.x(), to.y(), LIT_WIDTH / zoom, LIT_LINK);
            } else {
                dashedLine(context, from.x(), from.y(), to.x(), to.y(), DIM_WIDTH / zoom, DIM_LINK);
            }
        }
        for (AchievementGalaxy.Star star : view.earned()) {
            if (star.achievement().crown()) {
                circle(context, star.x(), star.y(), STAR_SIZE * 0.8, 2 / zoom, HALO);
                circle(context, star.x(), star.y(), STAR_SIZE * 1.0, 1 / zoom, HALO);
            }
        }
        context.draw();
        view.shown().forEach(star -> drawStar(context, star, false));
        view.earned().forEach(star -> drawStar(context, star, true));
        context.getMatrices().pop();
    }

    private void drawStar(DrawContext context, AchievementGalaxy.Star star, boolean earned) {
        int x = (int) Math.round(star.x());
        int y = (int) Math.round(star.y());
        AdvancementObtainedStatus status = earned ? AdvancementObtainedStatus.OBTAINED : AdvancementObtainedStatus.UNOBTAINED;
        context.drawGuiTexture(status.getFrameTexture(frame(star.achievement())),
                x - STAR_SIZE / 2, y - STAR_SIZE / 2, STAR_SIZE, STAR_SIZE);
        int iconX = x - ICON_SIZE / 2;
        int iconY = y - ICON_SIZE / 2;
        context.drawItemWithoutEntity(AchievementAdvancements.icon(star.achievement().icon()), iconX, iconY);
        if (!earned) {
            context.fill(RenderLayer.getGui(), iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, DIM_Z, NOT_EARNED_ICON);
        }
    }

    private static AdvancementFrame frame(Achievement achievement) {
        return switch (achievement.frame()) {
            case TASK -> AdvancementFrame.TASK;
            case GOAL -> AdvancementFrame.GOAL;
            case CHALLENGE -> AdvancementFrame.CHALLENGE;
        };
    }

    /**
     * Draws the tabs and returns the hovered one, if any. With no other tab to switch to, there is no strip.
     * 绘制标签页并返回鼠标悬停的那一个（如有）。没有其他标签页可切换时不画标签栏。
     */
    private PlacedAdvancement drawTabs(DrawContext context, int mouseX, int mouseY) {
        List<PlacedAdvancement> tabs = tabs();
        PlacedAdvancement hovered = null;
        if (tabs.size() < 2) {
            return null;
        }
        int x = tabsLeft(tabs.size());
        for (PlacedAdvancement tab : tabs) {
            boolean ours = AchievementAdvancements.isOurs(tab.getAdvancementEntry().id());
            context.drawGuiTexture(ours ? TAB_SELECTED : TAB, x, 0, TAB_WIDTH, TAB_HEIGHT);
            int iconX = x + 6;
            tab.getAdvancement().display().map(AdvancementDisplay::getIcon)
                    .ifPresent(icon -> context.drawItemWithoutEntity(icon, iconX, 9));
            if (mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= 0 && mouseY < TAB_HEIGHT) {
                hovered = tab;
            }
            x += TAB_WIDTH + TAB_GAP;
        }
        return hovered;
    }

    private List<OrderedText> tooltip(AchievementGalaxy.Star star) {
        Achievement achievement = star.achievement();
        AchievementLedger ledger = AchievementStorage.ledger(client);
        boolean earned = ledger.isUnlocked(achievement.id());
        List<OrderedText> lines = new ArrayList<>();
        // Not earned yet: the condition shows, the title stays a secret. 尚未达成：显示条件，标题保密。
        lines.add((earned ? AchievementAdvancements.title(achievement)
                : Text.literal(HIDDEN_TITLE).formatted(AchievementAdvancements.levelColor(achievement.level())))
                .asOrderedText());
        lines.add(Text.translatable(LEVEL_KEY, achievement.level())
                .formatted(AchievementAdvancements.levelColor(achievement.level())).asOrderedText());
        lines.addAll(textRenderer.wrapLines(Text.literal(achievement.description()), TOOLTIP_WIDTH));
        if (!earned) {
            lines.add(Text.translatable(NOT_YET_KEY).formatted(Formatting.DARK_GRAY).asOrderedText());
        } else {
            ledger.unlocks().stream()
                    .filter(unlock -> unlock.id().equals(achievement.id()) && unlock.place() != null)
                    .findFirst()
                    .ifPresent(unlock -> lines.add(Text.translatable(PLACE_KEY, place(unlock.place()))
                            .formatted(Formatting.DARK_GRAY).asOrderedText()));
        }
        return lines;
    }

    private static Text place(AchievementLedger.Place place) {
        return place.isLocalWorld() ? Text.translatable(LOCAL_WORLD_KEY) : Text.literal(place.server());
    }

    // ---- Input 输入 ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseY >= 0 && mouseY < TAB_HEIGHT) {
            List<PlacedAdvancement> tabs = tabs();
            int x = tabsLeft(tabs.size());
            for (PlacedAdvancement tab : tabs) {
                if (mouseX >= x && mouseX < x + TAB_WIDTH && !AchievementAdvancements.isOurs(tab.getAdvancementEntry().id())) {
                    advancements.selectTab(tab.getAdvancementEntry(), true);
                    client.setScreen(new AdvancementsScreen(advancements, parent));
                    return true;
                }
                x += TAB_WIDTH + TAB_GAP;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button != 0) {
            return false;
        }
        cameraX -= deltaX / zoom;
        cameraY -= deltaY / zoom;
        clampCamera();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Keep the point under the cursor in place. 保持光标下的点不动。
        double galaxyX = cameraX + (mouseX - centerX()) / zoom;
        double galaxyY = cameraY + (mouseY - centerY()) / zoom;
        zoom = MathHelper.clamp(zoom * Math.pow(ZOOM_STEP, verticalAmount), MIN_ZOOM, MAX_ZOOM);
        cameraX = galaxyX - (mouseX - centerX()) / zoom;
        cameraY = galaxyY - (mouseY - centerY()) / zoom;
        clampCamera();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (client.options.advancementsKey.matchesKey(keyCode, scanCode)) {
            client.setScreen(null);
            client.mouse.lockCursor();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ---- Helpers 辅助 ----

    private Optional<AchievementGalaxy.Star> starAt(double mouseX, double mouseY) {
        double galaxyX = cameraX + (mouseX - centerX()) / zoom;
        double galaxyY = cameraY + (mouseY - centerY()) / zoom;
        double half = STAR_SIZE / 2.0;
        List<AchievementGalaxy.Star> stars = new ArrayList<>(view.earned());
        stars.addAll(view.shown());
        return stars.stream()
                .filter(star -> Math.abs(star.x() - galaxyX) <= half && Math.abs(star.y() - galaxyY) <= half)
                .findFirst();
    }

    private List<PlacedAdvancement> tabs() {
        List<PlacedAdvancement> tabs = new ArrayList<>();
        for (PlacedAdvancement root : advancements.getManager().getRoots()) {
            if (root.getAdvancement().display().isPresent()) {
                tabs.add(root);
            }
        }
        return tabs;
    }

    private int tabsLeft(int count) {
        return (width - count * (TAB_WIDTH + TAB_GAP) + TAB_GAP) / 2;
    }

    private float centerX() {
        return width / 2f;
    }

    private float centerY() {
        return TOP + (height - TOP - BOTTOM) / 2f;
    }

    private static void clampCamera() {
        double limit = AchievementGalaxy.radius();
        cameraX = MathHelper.clamp(cameraX, -limit, limit);
        cameraY = MathHelper.clamp(cameraY, -limit, limit);
    }

    private static void circle(DrawContext context, double x, double y, double radius, double width, int color) {
        int segments = 48;
        for (int i = 0; i < segments; i++) {
            double a = 2 * Math.PI * i / segments;
            double b = 2 * Math.PI * (i + 1) / segments;
            line(context, x + radius * Math.cos(a), y + radius * Math.sin(a),
                    x + radius * Math.cos(b), y + radius * Math.sin(b), width, color);
        }
    }

    private static void dashedLine(DrawContext context, double x1, double y1, double x2, double y2, double width,
                                   int color) {
        double length = Math.hypot(x2 - x1, y2 - y1);
        if (length == 0) {
            return;
        }
        double dx = (x2 - x1) / length;
        double dy = (y2 - y1) / length;
        // Skip the parts hidden under the two frames. 跳过被两端边框盖住的部分。
        for (double at = STAR_SIZE / 2.0; at < length - STAR_SIZE / 2.0; at += DASH + DASH_GAP) {
            double end = Math.min(at + DASH, length - STAR_SIZE / 2.0);
            line(context, x1 + dx * at, y1 + dy * at, x1 + dx * end, y1 + dy * end, width, color);
        }
    }

    private static void line(DrawContext context, double x1, double y1, double x2, double y2, double width, int color) {
        quad(context, x1, y1, x2, y2, width, color);
    }

    /**
     * A {@code width}-thick band from one point to another, wound like {@link DrawContext#fill}.
     * 从一点到另一点、粗 {@code width} 的带状四边形，顶点顺序与 {@link DrawContext#fill} 相同。
     */
    private static void quad(DrawContext context, double x1, double y1, double x2, double y2, double width, int color) {
        double length = Math.hypot(x2 - x1, y2 - y1);
        if (length == 0) {
            return;
        }
        float nx = (float) (-(y2 - y1) / length * width / 2);
        float ny = (float) ((x2 - x1) / length * width / 2);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        VertexConsumer buffer = context.getVertexConsumers().getBuffer(RenderLayer.getGui());
        buffer.vertex(matrix, (float) x1 - nx, (float) y1 - ny, 0).color(color);
        buffer.vertex(matrix, (float) x1 + nx, (float) y1 + ny, 0).color(color);
        buffer.vertex(matrix, (float) x2 + nx, (float) y2 + ny, 0).color(color);
        buffer.vertex(matrix, (float) x2 - nx, (float) y2 - ny, 0).color(color);
    }

    /**
     * A fixed field of faint stars tiled across the screen, drifting slowly against the galaxy as it moves.
     * 平铺在屏幕上的固定暗星，随银河移动缓慢地相对漂移。
     */
    private record Sky(int tile, int[] stars, double drift) {
        Sky(int tile, int count, double drift, long seed) {
            this(tile, scatter(tile, count, seed), drift);
        }

        private static int[] scatter(int tile, int count, long seed) {
            Random random = new Random(seed);
            int[] stars = new int[count * 3];
            for (int i = 0; i < count; i++) {
                stars[i * 3] = random.nextInt(tile);
                stars[i * 3 + 1] = random.nextInt(tile);
                // Mostly dim, a few bright and two pixels wide. 多数暗淡，少数明亮且两像素宽。
                int alpha = random.nextInt(10) == 0 ? 0xC0 : 0x30 + random.nextInt(0x50);
                stars[i * 3 + 2] = alpha << 24 | (random.nextBoolean() ? 0xFFFFFF : 0xCFD8FF);
            }
            return stars;
        }

        void draw(DrawContext context, int width, int height, double offsetX, double offsetY) {
            int shiftX = (int) Math.floor(-offsetX * drift);
            int shiftY = (int) Math.floor(-offsetY * drift);
            for (int i = 0; i < stars.length; i += 3) {
                int color = stars[i + 2];
                int size = color >>> 24 == 0xC0 ? 2 : 1;
                for (int x = Math.floorMod(stars[i] + shiftX, tile); x < width; x += tile) {
                    for (int y = Math.floorMod(stars[i + 1] + shiftY, tile); y < height; y += tile) {
                        context.fill(x, y, x + size, y + size, color);
                    }
                }
            }
        }
    }
}
