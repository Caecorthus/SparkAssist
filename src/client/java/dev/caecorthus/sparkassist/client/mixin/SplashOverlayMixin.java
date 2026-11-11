package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.config.SparkAssistClientSettings;
import dev.caecorthus.sparkassist.client.menu.ExpressSplashRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Covers the vanilla Mojang splash with SparkDLC's Express boot screen. Vanilla's {@code render} runs to completion —
 * it also advances the fade clock and dismisses the overlay — and the Express frame is painted over the result at TAIL;
 * {@link ExpressSplashRenderer} reads the overlay's own progress and reload flag and paints everything itself.
 * 用 SparkDLC 的特快启动画面盖住原版 Mojang 启动图。原版 {@code render} 完整执行——它同时负责推进淡出计时并移除遮罩——
 * 特快画面在 TAIL 处覆盖其结果；{@link ExpressSplashRenderer} 读取叠加层自身的进度与重载标志并自行绘制全部内容。
 *
 * <p>The splash runs before the window is reliably sized, so a null font or a zero-sized window leaves the vanilla frame
 * showing instead of painting over it — a broken boot screen is worse than the stock one.
 * 启动画面运行时窗口尺寸未必可靠，因此字体为空或窗口尺寸为零时保留原版帧而不去覆盖——坏掉的启动画面比原版更糟。</p>
 */
@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
    /**
     * Smoothed boot progress. Read at the tail of {@code render}, after vanilla has assigned it for this frame, so the
     * bar follows the same lerp curve the vanilla bar uses.
     * 平滑后的启动进度。在 {@code render} 的尾部读取，此时原版已完成本帧赋值，因此进度条与原版红条使用同一条插值曲线。
     */
    @Shadow
    private float progress;
    /** True while a resource reload owns the splash. 资源重载占用启动画面时为真。 */
    @Shadow
    @Final
    private boolean reloading;
    /** The client the overlay was built for. 该叠加层所属的客户端。 */
    @Shadow
    @Final
    private MinecraftClient client;

    /**
     * Paints the Express frame over the finished vanilla one. The injection is deliberately at TAIL and not at HEAD:
     * {@code SplashOverlay.render} does more than draw — it advances the fade-out clock and calls
     * {@code client.setOverlay(null)}, which is what actually dismisses the splash and reveals the title screen.
     * Cancelling at HEAD therefore froze the game on the splash at 100%, and it also swallowed the exception vanilla
     * raises for a failed reload. Letting the original run to completion and overpainting it keeps both behaviours;
     * the backdrop is fully opaque, so no Mojang mark or red bar shows through.
     * 在已完成的原版帧上覆盖特快画面。注入点刻意选在 TAIL 而非 HEAD：{@code SplashOverlay.render} 不只是绘制——
     * 它推进淡出计时并调用 {@code client.setOverlay(null)}，这才是移除启动遮罩、露出主菜单的调用。在 HEAD 取消会让
     * 游戏冻结在 100% 的启动画面，并且会吞掉原版为加载失败抛出的异常。让原方法跑完再覆盖即可同时保住这两种行为；
     * 底幕完全不透明，因此原版标志与红条不会透出来。
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void sparkassist$renderExpressSplash(DrawContext context, int mouseX, int mouseY, float delta,
                                                 CallbackInfo ci) {
        if (!SparkAssistClientSettings.customSplash()) {
            return;
        }
        MinecraftClient minecraft = this.client;
        if (minecraft == null) {
            return;
        }
        TextRenderer font = minecraft.textRenderer;
        if (font == null) {
            return;
        }
        int width = minecraft.getWindow().getScaledWidth();
        int height = minecraft.getWindow().getScaledHeight();
        if (width < 2 || height < 2) {
            return;
        }
        ExpressSplashRenderer.render(context, font, width, height, this.progress, this.reloading);
    }
}
