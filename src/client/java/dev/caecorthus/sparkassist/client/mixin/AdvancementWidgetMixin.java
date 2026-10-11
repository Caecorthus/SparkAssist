package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.achievement.AchievementAdvancements;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leaves the local achievement page without connector lines, so each achievement stands on its own.
 * 本地成就页不画连线，每个成就各自独立。
 */
@Mixin(AdvancementWidget.class)
public abstract class AdvancementWidgetMixin {
    @Shadow
    @Final
    private PlacedAdvancement advancement;

    // The tab draws lines by recursing from its root widget, so stopping at our root covers the whole page.
    // 每页从根节点递归画线，在我们的根节点处停下即覆盖整页。
    @Inject(method = "renderLines", at = @At("HEAD"), cancellable = true)
    private void sparkassist$skipAchievementLines(DrawContext context, int x, int y, boolean border, CallbackInfo ci) {
        if (AchievementAdvancements.isOurs(advancement.getAdvancementEntry().id())) {
            ci.cancel();
        }
    }
}
