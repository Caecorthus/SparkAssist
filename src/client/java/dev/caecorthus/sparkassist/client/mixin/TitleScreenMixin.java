package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.config.SparkAssistClientSettings;
import dev.caecorthus.sparkassist.client.menu.ExpressTitleRenderer;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the title screen's backdrop and lettering with SparkDLC's Express chrome. The frame is taken over at
 * {@code render} HEAD, {@link ExpressTitleRenderer} paints the backdrop and the hung station sign, and the vanilla
 * widget pass is replayed through {@code super.render}, so the button grid, narration, focus handling and every click
 * target stay exactly as vanilla built them.
 * 用 SparkDLC 的特快装饰替换主菜单的底幕与文字。整帧在 {@code render} 的 HEAD 处接管，{@link ExpressTitleRenderer}
 * 绘制底幕与悬挂站牌，随后通过 {@code super.render} 重放原版控件渲染，因此按钮阵列、叙述、焦点处理以及全部点击目标
 * 都与原版构建的完全一致。
 *
 * <p>Taking the frame over also drops the vanilla logo, the splash text and the Realms notification banner. The logo and
 * splash text are intentionally replaced by the sign; leaving a Realms banner unrendered only affects clients signed in
 * to Realms, which never reaches a session game.
 * 接管整帧同时也放弃了原版标志、闪现标语与 Realms 通知横幅。标志与标语本就由站牌取代；不绘制 Realms 横幅只影响已登录
 * Realms 的客户端，而这类客户端不会进入对局。</p>
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    /** The departure horn fires once per process, on the first title screen after boot. 发车号角每个进程只在首次主菜单响一次。 */
    private static boolean sparkassist$hornPlayed;

    /**
     * Blows Wathe's train horn once the splash is over, so the boot sequence ends on the departure cue instead of a
     * silent cut. The sound id comes from Wathe, so it is skipped when that mod is absent rather than played as a
     * no-op event.
     * 启动画面结束后吹响 Wathe 的列车号角，让启动流程以发车提示收尾而不是静默切换。声音 id 来自 Wathe，因此该模组
     * 缺席时直接跳过，而不是播放一个空事件。
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void sparkassist$playDepartureHorn(CallbackInfo ci) {
        if (sparkassist$hornPlayed) {
            return;
        }
        sparkassist$hornPlayed = true;
        if (!SparkAssistClientSettings.customSplash() || !FabricLoader.getInstance().isModLoaded("wathe")) {
            return;
        }
        this.client.getSoundManager().play(PositionedSoundInstance.master(
                SoundEvent.of(Identifier.of("wathe", "ambient.train.horn")),
                1.0F
        ));
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void sparkassist$renderExpressTitle(DrawContext context, int mouseX, int mouseY, float delta,
                                                CallbackInfo ci) {
        if (!SparkAssistClientSettings.customTitle()) {
            return;
        }
        // Wathe ships its own title panorama; draw it through vanilla's renderer so the menu background stays the
        // rotating train scene instead of a flat colour. This screen overrides renderBackground, so the panorama is
        // requested here directly.
        // Wathe 自带主菜单全景图；通过原版渲染器绘制，使菜单背景仍是旋转的列车场景而非平涂色块。本界面覆写了
        // renderBackground，因此在此直接请求全景。
        this.renderPanoramaBackground(context, delta);
        this.renderDarkening(context);
        ExpressTitleRenderer.render(
                context,
                this.textRenderer,
                this.width,
                this.height,
                "Minecraft " + SharedConstants.getGameVersion().getName()
        );
        // Vanilla widget pass: buttons, narration and click targets stay exactly as vanilla built them.
        // 原版控件层：按钮、叙述与点击目标完全保持原版构建的样子。
        super.render(context, mouseX, mouseY, delta);
        // Brass rim over each widget so the grid reads as part of the chrome; geometry comes from the widgets
        // themselves, so focus, hover and clicks are untouched.
        // 给每个控件描一道黄铜边，使按钮阵列与装饰风格一致；几何取自控件本身，焦点、悬停与点击不受影响。
        for (Element element : this.children()) {
            if (element instanceof ClickableWidget widget && widget.visible) {
                ExpressTitleRenderer.outlineButton(
                        context,
                        widget.getX(),
                        widget.getY(),
                        widget.getWidth(),
                        widget.getHeight()
                );
            }
        }
        ci.cancel();
    }
}
