package dev.caecorthus.sparkassist.client.guidebook;

import dev.caecorthus.sparkassist.guidebook.GuidebookEntryPoint;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

/**
 * Puts the guide book in the top-right corner of the inventory the inventory key opens, for everyone who does not get
 * Wathe's limited inventory with the guide built in: lobby players, players outside a round, dead spectators and
 * creative players. One key, one place, whatever the player's state.
 * 在背包键打开的背包界面右上角放置指南书，供拿不到内嵌指南的 Wathe 受限背包的玩家使用：大厅玩家、对局外的玩家、
 * 死亡旁观者和创造模式玩家。无论处于什么状态，都是同一个按键、同一个位置。
 */
public final class GuidebookInventoryButton {
    private static final int EDGE = 8;

    private GuidebookInventoryButton() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register(GuidebookInventoryButton::afterInit);
    }

    private static void afterInit(MinecraftClient client, Screen screen, int width, int height) {
        if (!(screen instanceof InventoryScreen || screen instanceof CreativeInventoryScreen)) {
            return;
        }
        if (GuidebookClientState.entryPoint(client) != GuidebookEntryPoint.INVENTORY_BUTTON) {
            return;
        }
        GuidebookOpenButton book = new GuidebookOpenButton(width - EDGE - GuidebookOpenButton.SIZE, EDGE, screen);
        Screens.getButtons(screen).add(book);
        // Drawn after the inventory so it sits above slots and item tooltips; Fabric re-registers on every init.
        // 在背包绘制完成后再画，位于格子与物品提示之上；Fabric 每次初始化都会重新注册。
        ScreenEvents.afterRender(screen).register((current, context, mouseX, mouseY, delta) ->
                book.renderTooltip(context, client.textRenderer));
    }
}
