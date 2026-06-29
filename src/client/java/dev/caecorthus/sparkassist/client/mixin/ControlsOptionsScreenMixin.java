package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.screen.SparkAssistOptions;
import java.util.Arrays;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds SparkAssist's instinct mode beside vanilla controls options.
 * 将 SparkAssist 的本能键模式加入原版控制选项列表。
 */
@Mixin(ControlsOptionsScreen.class)
public abstract class ControlsOptionsScreenMixin {
    @Inject(method = "getOptions", at = @At("RETURN"), cancellable = true)
    private static void sparkassist$appendInstinctKeyModeOption(
            GameOptions options,
            CallbackInfoReturnable<SimpleOption<?>[]> cir
    ) {
        SimpleOption<?>[] vanillaOptions = cir.getReturnValue();
        SimpleOption<?>[] optionsWithSparkAssist = Arrays.copyOf(vanillaOptions, vanillaOptions.length + 1);
        optionsWithSparkAssist[vanillaOptions.length] = SparkAssistOptions.instinctKeyModeOption();
        cir.setReturnValue(optionsWithSparkAssist);
    }
}
