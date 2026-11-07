package dev.caecorthus.sparkassist.client.menu;

import static dev.caecorthus.sparkassist.client.guidebook.ui.ExpressPalette.*;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/**
 * Draws SparkDLC's boot splash: the Express backdrop, a hanging station sign, the brass progress bar and the line that
 * names what the client is currently loading. It replaces the vanilla Mojang mark and red bar but keeps the same
 * contract — one frame, no widget tree, no input — so it can run before the window is fully sized and during a resource
 * reload alike.
 * 绘制 SparkDLC 的启动画面：特快底幕、悬挂站牌、黄铜进度条，以及一行说明当前装载阶段的文字。它替换原版的 Mojang
 * 标与红条，但保持同样的约定——单帧、无控件树、无输入——因此窗口尚未定尺时与资源重载期间都能运行。
 *
 * <p>The renderer is stateless: progress and the reload flag arrive from {@code SplashOverlay} every frame, and the clock
 * is read here. A frame smaller than {@link #MIN_SIZE} is skipped so the very first layout pass cannot divide by zero.
 * 渲染器无状态：进度与重载标志每帧由 {@code SplashOverlay} 传入，时钟在此读取。小于 {@link #MIN_SIZE} 的帧会被跳过，
 * 以免首次布局时除以零。</p>
 */
public final class ExpressSplashRenderer {
    /** Below this either dimension the window is not laid out yet and the splash is skipped. 小于此值时窗口尚未定尺。 */
    private static final int MIN_SIZE = 64;
    /** Heading scale on the station sign. 站牌标题的放大倍数。 */
    private static final float HEADING_SCALE = 2.0F;
    /** Where the sign sits vertically, as a fraction of the screen height. 站牌在竖直方向的位置（屏高比例）。 */
    private static final float SIGN_ANCHOR = 0.40F;

    private ExpressSplashRenderer() {
    }

    /**
     * Renders one splash frame.
     * 渲染一帧启动画面。
     *
     * @param progress 0..1 boot progress, already smoothed by the overlay 0..1 的启动进度，叠加层已完成平滑
     * @param reloading whether a resource reload is running instead of a cold boot 是资源重载而非冷启动
     */
    public static void render(DrawContext context, TextRenderer font, int width, int height, float progress,
                              boolean reloading) {
        if (width < MIN_SIZE || height < MIN_SIZE) {
            return;
        }
        double seconds = Util.getMeasuringTimeMs() / 1000.0D;
        ExpressMenuPaint.backdrop(context, width, height, seconds, true);

        int cx = width / 2;
        int signCy = Math.round(height * SIGN_ANCHOR);
        ExpressMenuPaint.stationSign(
                context,
                font,
                cx,
                signCy,
                MenuText.localized("screen.sparkassist.splash.title", "SPARK EXPRESS"),
                MenuText.localized("screen.sparkassist.splash.subtitle", "快 运 特 急"),
                HEADING_SCALE
        );

        int barY = signCy + ExpressMenuPaint.SIGN_HEIGHT / 2 + 20;
        ExpressMenuPaint.progressBar(context, cx, barY, progress, seconds);
        ExpressMenuPaint.scaledCentered(
                context,
                font,
                MenuText.literal(Math.round(Math.max(0.0F, Math.min(1.0F, progress)) * 100.0F) + "%"),
                cx,
                barY + 16,
                1.5F,
                POLISHED
        );
        ExpressMenuPaint.centered(context, font, stage(progress, reloading), cx, barY + 32, MUTED);

        ExpressMenuPaint.centered(
                context,
                font,
                MenuText.localized("screen.sparkassist.splash.footer", "SparkDLC · 哈比特快"),
                cx,
                height - ExpressMenuPaint.TRACK_HEIGHT - ExpressMenuPaint.FOOTER_HEIGHT - 14,
                FAINT
        );
    }

    /**
     * Maps boot progress onto the loading stage the sign claims. The thresholds are deliberately coarse: the bar is the
     * source of truth and this line only has to feel like it belongs to it.
     * 把启动进度映射为站牌所声称的装载阶段。阈值刻意粗糙：进度条才是准绳，这行字只需与之相称。
     */
    static Text stage(float progress, boolean reloading) {
        if (reloading) {
            return MenuText.localized("screen.sparkassist.splash.stage.reload", "正在重新装载资源…");
        }
        if (progress < 0.15F) {
            return MenuText.localized("screen.sparkassist.splash.stage.boiler", "正在点燃锅炉…");
        }
        if (progress < 0.45F) {
            return MenuText.localized("screen.sparkassist.splash.stage.coupling", "正在挂接车厢…");
        }
        if (progress < 0.75F) {
            return MenuText.localized("screen.sparkassist.splash.stage.signal", "正在调校信号…");
        }
        if (progress < 0.98F) {
            return MenuText.localized("screen.sparkassist.splash.stage.crew", "正在集合乘务…");
        }
        return MenuText.localized("screen.sparkassist.splash.stage.depart", "即将发车…");
    }
}
