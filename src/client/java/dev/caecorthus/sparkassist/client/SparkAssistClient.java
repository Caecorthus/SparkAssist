package dev.caecorthus.sparkassist.client;

import dev.caecorthus.sparkassist.client.config.SparkAssistConfigManager;
import dev.caecorthus.sparkassist.client.input.InstinctKeyController;
import dev.caecorthus.sparkassist.client.screen.SparkAssistSettingsIntegration;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class SparkAssistClient implements ClientModInitializer {
    private static SparkAssistConfigManager configManager;

    @Override
    public void onInitializeClient() {
        configManager = SparkAssistConfigManager.load();
        SparkAssistSettingsIntegration.register();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> InstinctKeyController.reset());
    }

    public static SparkAssistConfigManager configManager() {
        return configManager;
    }
}
