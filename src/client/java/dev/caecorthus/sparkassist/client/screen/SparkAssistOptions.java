package dev.caecorthus.sparkassist.client.screen;

import com.mojang.serialization.Codec;
import dev.caecorthus.sparkassist.client.SparkAssistClient;
import dev.caecorthus.sparkassist.client.input.InstinctKeyController;
import dev.caecorthus.sparkassist.config.SparkAssistConfig.InstinctKeyMode;
import java.util.List;
import net.minecraft.client.option.SimpleOption;

/**
 * Factory for client options that are rendered by vanilla option screens.
 * 由原版设置界面渲染的 SparkAssist 客户端选项工厂。
 */
public final class SparkAssistOptions {
    private static final Codec<InstinctKeyMode> INSTINCT_KEY_MODE_CODEC = Codec.STRING.xmap(
            InstinctKeyMode::fromSerialized,
            InstinctKeyMode::serializedName
    );

    private SparkAssistOptions() {
    }

    public static SimpleOption<InstinctKeyMode> instinctKeyModeOption() {
        return new SimpleOption<>(
                "option.sparkassist.instinct_key_mode",
                SimpleOption.emptyTooltip(),
                SimpleOption.enumValueText(),
                new SimpleOption.PotentialValuesBasedCallbacks<>(
                        List.of(InstinctKeyMode.values()),
                        INSTINCT_KEY_MODE_CODEC
                ),
                SparkAssistClient.configManager().config().instinctKeyMode(),
                mode -> {
                    SparkAssistClient.configManager().config().setInstinctKeyMode(mode);
                    SparkAssistClient.configManager().save();
                    InstinctKeyController.reset();
                }
        );
    }
}
