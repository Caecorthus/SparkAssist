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
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.util.Language;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
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

    @WrapOperation(method = "render", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/client/font/TextHandler;wrapLines(Ljava/lang/String;ILnet/minecraft/text/Style;)Ljava/util/List;"))
    private List<StringVisitable> sparkassist$captionLines(TextHandler handler, String text, int width, Style style,
            Operation<List<StringVisitable>> original, @Local(argsOnly = true) PonderScene scene,
            @Local(argsOnly = true) PonderUI screen, @Share("captionLines") LocalRef<List<StringVisitable>> lines) {
        if (!scene.getNamespace().equals("sparkassist")) {
            return original.call(handler, text, width, style);
        }
        List<StringVisitable> wrapped = PonderCaptionText.wrap(screen.getFontRenderer(), text,
                screen.width, screen.height, width);
        lines.set(wrapped);
        return wrapped;
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
        return lines.get() == null ? original.call(graphics, font, text, x, y, color, shadow)
                : graphics.drawText(font, Language.getInstance().reorder(lines.get().get(lineIndex)),
                        x, y, color, shadow);
    }
}
