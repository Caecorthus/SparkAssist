package dev.caecorthus.sparkassist.client.menu;

import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.*;

import dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPaint;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Shared drawing recipes for the SparkDLC boot chrome: the mahogany-and-brass backdrop, the station sign and the brass
 * progress bar. Every pixel is a {@code DrawContext} fill, so the menu needs no texture pack of its own and inherits the
 * Harpy Express tokens the guidebook and the two sibling mods already use.
 * SparkDLC 启动界面的共享绘制配方：桃花心木与黄铜的底幕、站牌与黄铜进度条。全部由 {@code DrawContext} 填充完成，
 * 因此这套界面不需要自带贴图，并直接沿用指南书与两个兄弟模组已在用的哈比特快设计令牌。
 *
 * <p>Geometry is integer and centred on a passed-in {@code cx}; {@code DrawContext.fill} takes exclusive x2/y2. Time is
 * passed in seconds so callers own the clock and a paused or headless frame still draws a stable picture.
 * 几何为整数并以传入的 {@code cx} 居中；{@code DrawContext.fill} 的 x2/y2 为独占；时间以秒传入，由调用方掌握时钟，
 * 因此暂停帧或无头帧同样能画出稳定画面。</p>
 */
public final class ExpressMenuPaint {
    /** Height of the track band itself. 铁轨带自身的高度。 */
    public static final int TRACK_HEIGHT = 34;
    /** Strip kept below the track for the version line. 铁轨下方留给版本行的条带高度。 */
    public static final int FOOTER_HEIGHT = 16;
    /** Station sign geometry. 站牌尺寸。 */
    public static final int SIGN_WIDTH = 260;
    public static final int SIGN_HEIGHT = 64;
    /** Progress bar geometry. 进度条尺寸。 */
    public static final int BAR_WIDTH = 268;
    public static final int BAR_HEIGHT = 10;
    /** Sleeping spacing and length, in scaled pixels. 枕木间距与长度。 */
    private static final int SLEEPER_STEP = 28;
    private static final int SLEEPER_LENGTH = 16;
    /** Sleepers scroll at this many scaled pixels per second. 枕木每秒滚动的像素数。 */
    private static final double SLEEPER_SPEED = 22.0D;

    private ExpressMenuPaint() {
    }

    // ================================================================ backdrop / 底幕

    /**
     * Draws the full-screen backdrop: a vertical mahogany gradient, a soft vignette and the track along the bottom edge.
     * When {@code moving} is true the sleepers scroll, which reads as a train already under way; the title screen passes
     * false so the menu does not fidget behind the buttons.
     * 绘制全屏底幕：桃花心木垂直渐变、柔和暗角，以及底边铁轨。{@code moving} 为真时枕木滚动，读起来像列车已启动；
     * 主菜单传 false，避免按钮背后不断晃动。
     */
    public static void backdrop(DrawContext c, int w, int h, double seconds, boolean moving) {
        // Fold the epoch millisecond clock into one hour before it drives any animation: the raw value overflows when
        // narrowed to an int and loses phase precision long before that.
        // 先把纪元毫秒时钟折进一小时再驱动动画：原始值收窄为 int 会溢出，且远在此之前就损失相位精度。
        double t = seconds % 3600.0D;
        c.fillGradient(0, 0, w, h, BODY, WELL_SHADE);
        // Vignette: the top and bottom thirds fade towards the frame edges. 暗角：上下各三分之一向画面边缘压暗。
        c.fillGradient(0, 0, w, Math.max(1, h / 3), 0xB3050200, 0x00050200);
        c.fillGradient(0, h - Math.max(1, h / 3), w, h, 0x00050200, 0xC7050200);
        track(c, w, h, t, moving);
    }

    /**
     * Draws one length of track flush with the bottom edge: ballast, sleepers and two brass rails.
     * 绘制贴底的一截轨道：道砟、枕木与两条黄铜钢轨。
     */
    public static void track(DrawContext c, int w, int h, double seconds, boolean moving) {
        int railTop = h - TRACK_HEIGHT - FOOTER_HEIGHT;
        // Ballast bed, continuing to the very bottom so the footer strip shares the track's backing.
        // 道砟延伸到最底部，使页脚条与铁轨共用同一底色。
        c.fill(0, railTop + 12, w, h, 0xFF0B0603);
        c.fill(0, railTop + 11, w, railTop + 12, 0xFF1A0E07);
        // Sleepers, scrolled. 枕木，随行进滚动。
        int shift = moving ? (int) ((seconds * SLEEPER_SPEED) % SLEEPER_STEP) : 0;
        for (int x = -SLEEPER_STEP + shift; x < w; x += SLEEPER_STEP) {
            c.fill(x, railTop + 5, x + SLEEPER_LENGTH, railTop + 11, 0xFF2A1408);
            c.fill(x, railTop + 5, x + SLEEPER_LENGTH, railTop + 6, RIM_HI);
        }
        // Rails: a lit crown, a brass body and a shaded foot. 钢轨：高光轨顶、黄铜轨身、阴影轨底。
        c.fill(0, railTop, w, railTop + 1, 0xFFE8C97A);
        c.fill(0, railTop + 1, w, railTop + 4, BRASS_LO);
        c.fill(0, railTop + 4, w, railTop + 5, EDGE);
    }

    /**
     * A light scrim for a screen that keeps its own background (the title screen's panorama): it only darkens the top
     * band behind the sign and the bottom band behind the footer, leaving the middle of the panorama readable.
     * 给保留自有背景（主菜单全景图）的界面用的轻量压暗层：只压暗站牌背后的顶部与页脚背后的底部，
     * 让全景图中段保持可读。
     */
    public static void scrim(DrawContext c, int w, int h, int band) {
        int top = Math.max(1, band);
        int bottom = Math.max(1, band);
        c.fillGradient(0, 0, w, top, 0xD8050200, 0x00050200);
        c.fillGradient(0, h - bottom, w, h, 0x00050200, 0xE0050200);
    }

    /**
     * A translucent plate behind a block of footer text. The title screen keeps Wathe's panorama visible, and light text
     * over a bright scene is unreadable without it; the plate is darker than the scrim so it still separates from the
     * surrounding band.
     * 页脚文字块后面的半透明底板。主菜单保留 Wathe 全景，浅色文字压在明亮场景上会看不清；底板比压暗层更深，因此仍能
     * 与周围的暗带区分开。
     */
    public static void plate(DrawContext c, int x, int y, int w, int h) {
        c.fill(x, y, x + w, y + h, 0xB0080402);
        c.fill(x, y, x + w, y + 1, 0x66C5A244);
        c.fill(x, y + h - 1, x + w, y + h, 0x66000000);
    }

    // ================================================================ station sign / 站牌

    /**
     * Draws the hanging station sign: a mahogany panel with brass rules, four rivets, a scaled line of heading text and a
     * smaller subtitle underneath. The two brass rules are placed first and the lettering is fitted between them, so a
     * later change to {@link #SIGN_HEIGHT} cannot let the text collide with the frame.
     * 绘制悬挂站牌：桃花心木面板配黄铜刻线、四颗铆钉、一行放大的标题与下方较小的副标题。两条黄铜刻线先定位，文字再
     * 填入其间，因此日后改动 {@link #SIGN_HEIGHT} 也不会让文字撞上边框。
     */
    public static void stationSign(DrawContext c, TextRenderer font, int cx, int cy, Text heading, Text subtitle,
                                   float headingScale) {
        int x = cx - SIGN_WIDTH / 2;
        int y = cy - SIGN_HEIGHT / 2;
        ExpressPaint.panel(c, x, y, SIGN_WIDTH, SIGN_HEIGHT);
        // Inner well so the lettering sits in a recess rather than on the panel face. 内凹井，让文字落在凹槽里。
        c.fill(x + 6, y + 6, x + SIGN_WIDTH - 6, y + SIGN_HEIGHT - 6, 0xFF120802);
        c.fill(x + 6, y + 6, x + SIGN_WIDTH - 6, y + 7, 0xFF050200);
        // Brass rules above and below the lettering. 文字上下的黄铜刻线。
        int ruleTop = y + 12;
        int ruleBottom = y + SIGN_HEIGHT - 14;
        for (int ruleY : new int[] {ruleTop, ruleBottom}) {
            c.fill(x + 14, ruleY, x + SIGN_WIDTH - 14, ruleY + 1, BRASS_LO);
            c.fill(x + 14, ruleY + 1, x + SIGN_WIDTH - 14, ruleY + 2, EDGE);
        }
        for (int i = 0; i < 4; i++) {
            int rx = (i % 2 == 0) ? x + 4 : x + SIGN_WIDTH - 8;
            int ry = (i < 2) ? y + 4 : y + SIGN_HEIGHT - 8;
            rivet(c, rx, ry);
        }
        // Heading sits directly under the top rule; the subtitle hugs the bottom rule. 标题贴上方刻线，副标题贴下方刻线。
        int headingTop = ruleTop + 4;
        scaledCentered(c, font, heading, cx, headingTop, headingScale, POLISHED);
        if (subtitle != null) {
            int subtitleTop = ruleBottom - 12;
            centered(c, font, subtitle, cx, subtitleTop, BRASS_HI);
        }
    }

    /** A single brass rivet: a dark seat, a lit crown. 单颗黄铜铆钉：暗底座配亮帽。 */
    private static void rivet(DrawContext c, int x, int y) {
        c.fill(x, y, x + 4, y + 4, EDGE);
        c.fill(x + 1, y + 1, x + 3, y + 3, BRASS);
        c.fill(x + 1, y + 1, x + 2, y + 2, POLISHED);
    }

    // ================================================================ progress / 进度条

    /**
     * Draws the brass progress bar with its recessed well, fill and lit head. {@code progress} is clamped to 0..1.
     * 绘制黄铜进度条：内凹槽、填充与亮起的头部。{@code progress} 会被夹到 0..1。
     */
    public static void progressBar(DrawContext c, int cx, int y, float progress, double seconds) {
        int x = cx - BAR_WIDTH / 2;
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        // Well with a shaded lip. 带阴影边沿的凹槽。
        c.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, EDGE);
        c.fill(x + 1, y + 1, x + BAR_WIDTH - 1, y + BAR_HEIGHT - 1, WELL);
        c.fill(x + 1, y + 1, x + BAR_WIDTH - 1, y + 2, WELL_SHADE);
        // Fill: a brass gradient, never shorter than one pixel once progress is non-zero. 填充：黄铜渐变。
        int filled = Math.round(clamped * (BAR_WIDTH - 2));
        if (filled > 0) {
            int fillX = x + 1;
            c.fillGradient(fillX, y + 2, fillX + filled, y + BAR_HEIGHT - 2, BRASS_HI, BRASS_LO);
            c.fill(fillX, y + 2, fillX + filled, y + 3, POLISHED);
            // Lit head, pulsing so an idle frame still reads as alive. 亮起的头部，做呼吸以免静止帧显得卡死。
            int pulse = (int) (40 + 55 * (0.5D + 0.5D * Math.sin(seconds * 3.1D)));
            int headX = Math.max(fillX, fillX + filled - 2);
            c.fill(headX, y + 2, fillX + filled, y + BAR_HEIGHT - 2, ((pulse & 0xFF) << 24) | 0xFFFFFF);
        }
        // Frame. 外框。
        c.fill(x, y, x + BAR_WIDTH, y + 1, BRASS_LO);
        c.fill(x, y + BAR_HEIGHT - 1, x + BAR_WIDTH, y + BAR_HEIGHT, EDGE);
        c.fill(x, y, x + 1, y + BAR_HEIGHT, BRASS_LO);
        c.fill(x + BAR_WIDTH - 1, y, x + BAR_WIDTH, y + BAR_HEIGHT, EDGE);
    }

    // ================================================================ text / 文字

    /** Centred line at 1x with a drop shadow. 1 倍居中带阴影文本。 */
    public static void centered(DrawContext c, TextRenderer font, Text text, int cx, int y, int color) {
        c.drawCenteredTextWithShadow(font, text, cx, y, color);
    }

    /**
     * Centred line drawn at {@code scale} through the matrix stack, so headings can be larger than the 9 px font without
     * a resource pack. {@code cx} and {@code y} stay in scaled-screen pixels; the translation happens before the scale.
     * 通过矩阵栈按 {@code scale} 绘制的居中文本，使标题能大于 9 像素字体而无需资源包。{@code cx} 与 {@code y} 仍是
     * 缩放屏幕坐标，平移发生在缩放之前。
     */
    public static void scaledCentered(DrawContext c, TextRenderer font, Text text, int cx, int y, float scale, int color) {
        if (scale == 1.0F) {
            centered(c, font, text, cx, y, color);
            return;
        }
        c.getMatrices().push();
        c.getMatrices().translate((float) cx, (float) y, 0.0F);
        c.getMatrices().scale(scale, scale, 1.0F);
        c.drawCenteredTextWithShadow(font, text, 0, 0, color);
        c.getMatrices().pop();
    }

    /**
     * Left-aligned small caption with a shadow, used for the build line and the copyright row.
     * 左对齐的小字说明（带阴影），用于构建信息与版权行。
     */
    public static void caption(DrawContext c, TextRenderer font, Text text, int x, int y, int color) {
        c.drawTextWithShadow(font, text, x, y, color);
    }

    /**
     * Right-aligned small caption with a shadow. The width is measured by the font so the caller does not have to.
     * 右对齐的小字说明（带阴影）；宽度由字体测量，调用方无需自行计算。
     */
    public static void rightCaption(DrawContext c, TextRenderer font, Text text, int right, int y, int color) {
        c.drawTextWithShadow(font, text, right - font.getWidth(text), y, color);
    }
}
