package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.achievement.AchievementAdvancements;
import dev.caecorthus.sparkassist.client.achievement.AchievementGalaxyScreen;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Opens the full-screen achievement page whenever the vanilla advancements screen lands on the local achievement tab,
 * whether picked by click or remembered from last time.
 * 原版进度界面一旦停在本地成就页（点选的或上次记住的），就打开全屏成就页。
 */
@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin extends Screen {
    @Shadow
    @Final
    private ClientAdvancementManager advancementHandler;

    @Shadow
    @Final
    private Screen parent;

    @Shadow
    private AdvancementTab selectedTab;

    protected AdvancementsScreenMixin(Text title) {
        super(title);
    }

    // Also runs from init(), so switch screens on the next task rather than in the middle of opening this one.
    // init() 中也会调用，因此在下一个任务里切换界面，而不是在打开这个界面的过程中切换。
    @Inject(method = "selectTab", at = @At("TAIL"))
    private void sparkassist$openAchievementPage(AdvancementEntry advancement, CallbackInfo ci) {
        if (!sparkassist$onAchievementTab()) {
            return;
        }
        Screen self = this;
        MinecraftClient client = MinecraftClient.getInstance();
        client.send(() -> {
            if (client.currentScreen == self && sparkassist$onAchievementTab()) {
                client.setScreen(new AchievementGalaxyScreen(advancementHandler, parent));
            }
        });
    }

    // Until the switch, show the page's empty sky instead of the vanilla window. 切换之前显示成就页的空星空，而不是原版窗口。
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void sparkassist$hideAchievementTab(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (sparkassist$onAchievementTab()) {
            context.fill(0, 0, width, height, AchievementGalaxyScreen.SPACE);
            ci.cancel();
        }
    }

    @Unique
    private boolean sparkassist$onAchievementTab() {
        return selectedTab != null && AchievementAdvancements.isOurs(selectedTab.getRoot().getAdvancementEntry().id());
    }
}
