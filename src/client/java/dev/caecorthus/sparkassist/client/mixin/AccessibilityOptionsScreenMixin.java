package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.screen.SparkAssistOptions;
import java.util.Arrays;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the guidebook decoration density and foliage switches beside vanilla accessibility options, where the
 * other "how much is on screen" choices live.
 * 把指南点缀的密度与枝叶开关加入原版辅助功能选项列表，与其它“画面上放多少东西”的选项放在一起。
 */
@Mixin(AccessibilityOptionsScreen.class)
public abstract class AccessibilityOptionsScreenMixin {
    @Inject(method = "getOptions", at = @At("RETURN"), cancellable = true)
    private static void sparkassist$appendGuidebookDecorOptions(
            GameOptions options,
            CallbackInfoReturnable<SimpleOption<?>[]> cir
    ) {
        SimpleOption<?>[] vanillaOptions = cir.getReturnValue();
        SimpleOption<?>[] optionsWithSparkAssist = Arrays.copyOf(vanillaOptions, vanillaOptions.length + 2);
        optionsWithSparkAssist[vanillaOptions.length] = SparkAssistOptions.guidebookDecorOption();
        optionsWithSparkAssist[vanillaOptions.length + 1] = SparkAssistOptions.guidebookFoliageOption();
        cir.setReturnValue(optionsWithSparkAssist);
    }
}
