package dev.caecorthus.sparkassist.client;

import dev.caecorthus.sparkassist.client.achievement.AchievementAdvancements;
import dev.caecorthus.sparkassist.client.achievement.AchievementClientState;
import dev.caecorthus.sparkassist.client.config.SparkAssistConfigManager;
import dev.caecorthus.sparkassist.client.guidebook.GuidebookClientState;
import dev.caecorthus.sparkassist.client.input.InstinctKeyController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class SparkAssistClient implements ClientModInitializer {
    private static SparkAssistConfigManager configManager;

    @Override
    public void onInitializeClient() {
        configManager = SparkAssistConfigManager.load();
        ClientTickEvents.END_CLIENT_TICK.register(GuidebookClientState::tick);
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
    }

    public static SparkAssistConfigManager configManager() {
        return configManager;
    }
}
