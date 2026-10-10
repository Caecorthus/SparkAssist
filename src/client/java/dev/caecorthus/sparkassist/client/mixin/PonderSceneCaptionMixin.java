package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.ponder.PonderCaptionText;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Each overlay pass starts a fresh list, including when paused or rendering two scenes during a transition. */
@Pseudo
@Mixin(targets = "net.createmod.ponder.foundation.PonderScene", remap = false)
public abstract class PonderSceneCaptionMixin {
    @Inject(method = "renderOverlay", at = @At("HEAD"))
    private void sparkassist$beginCaptionLayout(PonderUI screen, DrawContext graphics, float partialTicks,
                                                CallbackInfo ci) {
        if (((PonderScene) (Object) this).getNamespace().equals("sparkassist")) {
            PonderCaptionText.beginOverlay();
        }
    }
}
