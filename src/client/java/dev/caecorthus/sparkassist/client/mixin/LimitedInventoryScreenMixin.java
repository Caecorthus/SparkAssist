package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.guidebook.GuidebookScreen;
import dev.caecorthus.sparkassist.guidebook.GuidebookInputState;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the guide without replacing the live inventory screen or its handler.
 * 在保留背包界面和容器生命周期的前提下加入指南。
 */
@Mixin(LimitedInventoryScreen.class)
public abstract class LimitedInventoryScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    @Unique
    private GuidebookScreen sparkassist$guide;
    @Unique
    private GuidebookInputState sparkassist$guideInput;

    protected LimitedInventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void sparkassist$addGuidebook(CallbackInfo ci) {
        LimitedInventoryScreen screen = (LimitedInventoryScreen) (Object) this;
        if (sparkassist$guide == null) {
            sparkassist$guide = GuidebookScreen.embedded(screen);
        }
        sparkassist$guide.init(this.client, this.width, this.height);
        // Resize reinitializes this screen while a guide-owned press may still be held.
        // 窗口缩放会重新初始化同一界面，保留尚未释放的指南输入。
        if (sparkassist$guideInput == null) {
            sparkassist$guideInput = new GuidebookInputState();
        }

        ScreenEvents.afterRender(screen).register((current, context, mouseX, mouseY, delta) -> {
            context.getMatrices().push();
            // Persistent navigation stays below shop tooltips; readers remain modal.
            // 常驻目录位于商品说明下方，主动阅读时保留模态层。
            context.getMatrices().translate(0, 0, sparkassist$guide.isModal() ? 400 : 100);
            sparkassist$guide.render(context, mouseX, mouseY, delta);
            context.getMatrices().pop();
        });
        ScreenEvents.remove(screen).register(current -> sparkassist$guide.removed());
        ScreenMouseEvents.allowMouseClick(screen).register((current, x, y, button) -> {
            boolean captured = sparkassist$guideInput.pressMouse(button, sparkassist$guide.capturesMouse(x, y));
            sparkassist$guide.mouseClicked(x, y, button);
            return !captured;
        });
        ScreenMouseEvents.allowMouseRelease(screen).register((current, x, y, button) -> {
            boolean captured = sparkassist$guideInput.releaseMouse(button, sparkassist$guide.isModal());
            if (captured) {
                sparkassist$guide.mouseReleased(x, y, button);
            }
            return !captured;
        });
        ScreenMouseEvents.allowMouseScroll(screen).register((current, x, y, horizontal, vertical) -> {
            boolean captured = sparkassist$guide.capturesMouse(x, y);
            if (captured) {
                sparkassist$guide.mouseScrolled(x, y, horizontal, vertical);
            }
            return !captured;
        });
        ScreenKeyboardEvents.allowKeyPress(screen).register((current, key, scanCode, modifiers) -> {
            boolean captured = sparkassist$guide.capturesKeyboard();
            boolean handled = sparkassist$guide.keyPressed(key, scanCode, modifiers);
            return !sparkassist$guideInput.pressKey(key, captured || handled);
        });
        ScreenKeyboardEvents.allowKeyRelease(screen).register((current, key, scanCode, modifiers) ->
                !sparkassist$guideInput.releaseKey(key, sparkassist$guide.capturesKeyboard()));
    }

    // Fabric has no per-screen drag/character events in this Minecraft version.
    // 当前版本的 Fabric 没有界面级拖动和字符事件，因此仅补充这两个入口。
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (sparkassist$guide != null && sparkassist$guideInput.dragMouse(button, sparkassist$guide.isModal())) {
            sparkassist$guide.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (sparkassist$guide != null && sparkassist$guide.capturesKeyboard()) {
            sparkassist$guide.charTyped(chr, modifiers);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }
}
