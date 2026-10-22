package dev.caecorthus.sparkassist.client;

import dev.caecorthus.sparkassist.achievement.AchievementGalaxy;
import dev.caecorthus.sparkassist.client.achievement.AchievementAdvancements;
import dev.caecorthus.sparkassist.client.achievement.AchievementClientState;
import dev.caecorthus.sparkassist.client.config.SparkAssistConfigManager;
import dev.caecorthus.sparkassist.client.guidebook.GuidebookClientState;
import dev.caecorthus.sparkassist.client.guidebook.GuidebookInventoryButton;
import dev.caecorthus.sparkassist.client.guidebook.GuidebookResources;
import dev.caecorthus.sparkassist.client.ponder.PonderSupport;
import dev.caecorthus.sparkassist.client.input.InstinctKeyController;
import dev.doctor4t.wathe.client.WatheClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SparkAssistClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Achievements");

    private static SparkAssistConfigManager configManager;
    /** The world Wathe's muzzle map was last cleared for. 上次为之清空 Wathe 枪口表的世界。 */
    private static ClientWorld muzzleWorld;

    @Override
    public void onInitializeClient() {
        configManager = SparkAssistConfigManager.load();
        // Lay the achievement galaxy out now, off the render thread. 现在就在渲染线程之外排布成就银河。
        Thread galaxy = new Thread(SparkAssistClient::layOutGalaxy, "SparkAssist galaxy layout");
        galaxy.setDaemon(true);
        galaxy.start();
        PonderSupport.init();
        GuidebookResources.register();
        ClientTickEvents.END_CLIENT_TICK.register(GuidebookClientState::tick);
        GuidebookInventoryButton.register();
        ClientTickEvents.END_CLIENT_TICK.register(AchievementClientState::tick);
        // A server with no advancements of its own never sends the update that would restore the page.
        // 自身没有任何进度的服务器不会发送更新包，因此加入时主动补上成就页。
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                AchievementAdvancements.restore(handler.getAdvancementHandler()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            InstinctKeyController.reset();
            GuidebookClientState.disconnect();
            AchievementClientState.disconnect();
        });
        ClientTickEvents.END_CLIENT_TICK.register(SparkAssistClient::forgetMuzzles);
    }

    public static SparkAssistConfigManager configManager() {
        return configManager;
    }

    private static void layOutGalaxy() {
        try {
            AchievementGalaxy.warmUp();
        } catch (LinkageError exception) {
            // Opening the page then fails too, as it would have without this thread. 之后打开成就页同样会失败，与没有这个线程时一样。
            LOGGER.error("Could not lay out the achievement galaxy", exception);
        }
    }

    /**
     * Wathe keeps the muzzle of every gun-holding player it draws in a static map and forgets one only when that player
     * fires, so a world left behind would stay referenced through its players. The map is cleared on the client thread
     * whenever the client's world changes: a disconnect, a server switch (a proxy's reconfiguration fires no disconnect)
     * or a new dimension, whose players are all new entities.
     * Wathe 把所绘制的每个持枪玩家的枪口位置记在静态表中，只有该玩家开枪时才移除，因此离开的世界会通过其玩家一直被引用。
     * 客户端世界一旦改变就在客户端线程上清空该表：断开连接、切换服务器（代理的重新配置不会触发断开事件）或进入新维度时，
     * 其中的玩家都会是新实体。
     */
    private static void forgetMuzzles(MinecraftClient client) {
        if (client.world != muzzleWorld) {
            muzzleWorld = client.world;
            if (WatheClient.particleMap != null) {
                WatheClient.particleMap.clear();
            }
        }
    }
}
