package dev.caecorthus.sparkassist.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.caecorthus.sparkassist.client.ponder.PonderCaptionText;
import dev.caecorthus.sparkassist.ponder.PonderCaptionBounds;
import java.util.List;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.util.Language;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * SparkAssist captions share their measured lines and stay above the playback controls. Ponder still draws its
 * original window, palette, fade and animated pointer; the pointer remains anchored to the scene after a move.
 * SparkAssist 字幕共用测量后的行，保持在播放控件上方；保留 Ponder 原生窗口、配色、淡入和指向动画，移动后仍指向场景。
 */
@Pseudo
@Mixin(targets = "net.createmod.ponder.foundation.element.TextWindowElement", remap = false)
public abstract class PonderCaptionMixin {
    @Shadow(remap = false)
    private Vec3d vec;
    @Shadow(remap = false)
    private int y;

    // Ponder renders every frame and keeps the baked caption text between frames, so the wrapped and reordered lines
    // are kept until something that shapes them changes. Every resource reload installs a new Language, so a font
    // change misses too; Force Unicode Font and Japanese Glyph Variants switch glyphs in place, so they are keyed.
    // Ponder 每帧都会渲染，且在帧之间保留烘焙好的字幕文本，因此换行与重排后的行会一直保留，直到影响它们的条件改变。
    // 每次资源重载都会换上新的 Language，因此字体变化同样会使缓存失效；强制使用 Unicode 字体与日文字形变体会原地切换
    // 字形，因此也计入键中。
    @Unique
    private String sparkassist$wrappedText;
    @Unique
    private TextRenderer sparkassist$wrappedFont;
    @Unique
    private Language sparkassist$wrappedLanguage;
    @Unique
    private int sparkassist$wrappedWidth;
    @Unique
    private int sparkassist$wrappedScreenWidth;
    @Unique
    private int sparkassist$wrappedScreenHeight;
    @Unique
    private int sparkassist$wrappedGlyphs;
    @Unique
    private List<StringVisitable> sparkassist$wrappedLines;
    /** The wrapped lines reordered for drawing, rebuilt with them. 为绘制重排后的行，随换行结果一起重建。 */
    @Unique
    private List<OrderedText> sparkassist$orderedLines;

    @WrapOperation(method = "render", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/client/font/TextHandler;wrapLines(Ljava/lang/String;ILnet/minecraft/text/Style;)Ljava/util/List;"))
    private List<StringVisitable> sparkassist$captionLines(TextHandler handler, String text, int width, Style style,
            Operation<List<StringVisitable>> original, @Local(argsOnly = true) PonderScene scene,
            @Local(argsOnly = true) PonderUI screen, @Share("captionLines") LocalRef<List<StringVisitable>> lines) {
        if (!scene.getNamespace().equals("sparkassist")) {
            return original.call(handler, text, width, style);
        }
        TextRenderer font = screen.getFontRenderer();
        Language language = Language.getInstance();
        int glyphs = glyphOptions();
        if (sparkassist$wrappedLines == null || font != sparkassist$wrappedFont
                || language != sparkassist$wrappedLanguage || glyphs != sparkassist$wrappedGlyphs
                || width != sparkassist$wrappedWidth
                || screen.width != sparkassist$wrappedScreenWidth || screen.height != sparkassist$wrappedScreenHeight
                || (text != sparkassist$wrappedText && !text.equals(sparkassist$wrappedText))) {
            sparkassist$wrappedLines = PonderCaptionText.wrap(font, text, screen.width, screen.height, width);
            sparkassist$orderedLines = null;
            sparkassist$wrappedText = text;
            sparkassist$wrappedFont = font;
            sparkassist$wrappedLanguage = language;
            sparkassist$wrappedGlyphs = glyphs;
            sparkassist$wrappedWidth = width;
            sparkassist$wrappedScreenWidth = screen.width;
            sparkassist$wrappedScreenHeight = screen.height;
        }
        lines.set(sparkassist$wrappedLines);
        return sparkassist$wrappedLines;
    }

    @Unique
    private static int glyphOptions() {
        GameOptions options = MinecraftClient.getInstance().options;
        return (options.getForceUnicodeFont().getValue() ? 1 : 0)
                | (options.getJapaneseGlyphVariants().getValue() ? 2 : 0);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/client/font/TextRenderer;getWrappedLinesHeight(Ljava/lang/String;I)I"))
    private int sparkassist$captionHeight(TextRenderer font, String text, int width, Operation<Integer> original,
            @Share("captionLines") LocalRef<List<StringVisitable>> lines) {
        return lines.get() == null ? original.call(font, text, width)
                : PonderCaptionBounds.textHeight(lines.get().size());
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", ordinal = 0, remap = true,
            target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    private void sparkassist$fitCaption(MatrixStack matrices, float x, float y, float z, Operation<Void> original,
            @Local(argsOnly = true) PonderUI screen, @Local(name = "targetX") LocalFloatRef targetX,
            @Local(name = "boxWidth") int boxWidth, @Local(name = "boxHeight") int boxHeight,
            @Share("captionLines") LocalRef<List<StringVisitable>> lines,
            @Share("captionShiftY") LocalFloatRef shiftY) {
        if (lines.get() != null) {
            PonderCaptionBounds.Position position = vec == null
                    ? PonderCaptionText.placeIndependent(screen.width, screen.height,
                            targetX.get() - 10, this.y, boxWidth, boxHeight)
                    : PonderCaptionBounds.fit(screen.width, screen.height,
                            targetX.get() - 10, y + 3, boxWidth, boxHeight);
            targetX.set(position.x() + 10);
            shiftY.set(position.y() - 3 - y);
            y += shiftY.get();
        }
        original.call(matrices, x, y, z);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", ordinal = 1, remap = true,
            target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    private void sparkassist$keepPointerAnchor(MatrixStack matrices, float x, float y, float z,
            Operation<Void> original, @Share("captionShiftY") LocalFloatRef shiftY) {
        original.call(matrices, x, y - shiftY.get(), z);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"))
    private void sparkassist$pointAtMovedCaption(MatrixStack matrices, float x, float y, float z,
            Operation<Void> original, @Local(argsOnly = true, ordinal = 1) float fade,
            @Local(name = "targetX") float targetX, @Local(name = "sceneToScreen") Vec2f anchor,
            @Local(name = "boxWidth") int boxWidth, @Local(name = "boxHeight") int boxHeight,
            @Share("captionShiftY") LocalFloatRef shiftY) {
        float fraction = shiftY.get() == 0 ? 1 : PonderCaptionBounds.pointerVisibleFraction(
                targetX - anchor.x, shiftY.get(), boxWidth, boxHeight);
        x *= fraction;
        float vertical = shiftY.get() * fade * fraction;
        if (vertical != 0) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.atan2(vertical, x)));
            x = (float) Math.hypot(x, vertical);
        }
        original.call(matrices, x, y, z);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I"))
    private int sparkassist$drawCaptionLine(DrawContext graphics, TextRenderer font, String text, int x, int y,
            int color, boolean shadow, Operation<Integer> original, @Local(name = "i") int lineIndex,
            @Share("captionLines") LocalRef<List<StringVisitable>> lines) {
        if (lines.get() == null) {
            return original.call(graphics, font, text, x, y, color, shadow);
        }
        // Rebuilt once after each wrap, under the Language it was keyed on. 每次换行后重建一次，所用 Language 与换行时一致。
        if (sparkassist$orderedLines == null) {
            sparkassist$orderedLines = Language.getInstance().reorder(lines.get());
        }
        return graphics.drawText(font, sparkassist$orderedLines.get(lineIndex), x, y, color, shadow);
    }
}
