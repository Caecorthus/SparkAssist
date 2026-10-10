package dev.caecorthus.sparkassist.client.ponder;

import java.util.List;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.createmod.catnip.gui.ScreenOpener;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * Ponder's own player for demos opened from the guide. Unlike {@code PonderUI.of}, it takes scenes compiled from
 * SparkAssist's demo catalog, so role demos need no item; closing returns to the guide instead of the game.
 * 从指南书打开演示时使用的 Ponder 播放界面。与 PonderUI.of 不同，它接收从 SparkAssist 演示目录编译的场景，
 * 所以职业演示不需要物品；关闭时回到指南书，而不是回到游戏。
 */
final class DemoPonderUI extends PonderUI {
    @Nullable
    private final Screen returnTo;

    DemoPonderUI(List<PonderScene> scenes, @Nullable Screen returnTo) {
        super(scenes);
        this.returnTo = returnTo;
    }

    @Override
    public void close() {
        ScreenOpener.clearStack();
        if (client != null) {
            client.setScreen(returnTo);
        }
    }
}
