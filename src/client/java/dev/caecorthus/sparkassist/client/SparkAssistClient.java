package dev.caecorthus.sparkassist.client;

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
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            InstinctKeyController.reset();
            GuidebookClientState.disconnect();
        });
    }

    public static SparkAssistConfigManager configManager() {
        return configManager;
    }
}
