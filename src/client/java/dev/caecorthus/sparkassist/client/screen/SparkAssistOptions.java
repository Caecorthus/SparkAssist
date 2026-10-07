package dev.caecorthus.sparkassist.client.screen;

import com.mojang.serialization.Codec;
import dev.caecorthus.sparkassist.client.config.SparkAssistClientSettings;
import dev.caecorthus.sparkassist.config.SparkAssistConfig.GuidebookDecor;
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

    private static final Codec<GuidebookDecor> GUIDEBOOK_DECOR_CODEC = Codec.STRING.xmap(
            GuidebookDecor::fromSerialized,
            GuidebookDecor::serializedName
    );

    private SparkAssistOptions() {
    }

    /** 关 / 轻 / 中 / 满 for the guidebook decorations. 指南点缀的四档。 */
    public static SimpleOption<GuidebookDecor> guidebookDecorOption() {
        return new SimpleOption<>(
                "option.sparkassist.guidebook_decor",
                SimpleOption.emptyTooltip(),
                SimpleOption.enumValueText(),
                new SimpleOption.PotentialValuesBasedCallbacks<>(
                        List.of(GuidebookDecor.values()),
                        GUIDEBOOK_DECOR_CODEC
                ),
                SparkAssistClientSettings.guidebookDecor(),
                SparkAssistClientSettings::setGuidebookDecor
        );
    }

    /** Whether the faction foliage grows along the frames. 枝叶是否沿边框生长。 */
    public static SimpleOption<Boolean> guidebookFoliageOption() {
        return SimpleOption.ofBoolean(
                "option.sparkassist.guidebook_foliage",
                SparkAssistClientSettings.guidebookFoliage(),
                SparkAssistClientSettings::setGuidebookFoliage
        );
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
                SparkAssistClientSettings.instinctKeyMode(),
                SparkAssistClientSettings::setInstinctKeyMode
        );
    }
}
