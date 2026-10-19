package dev.caecorthus.sparkassist.client.settings;

import dev.caecorthus.sparkassist.client.config.SparkAssistClientSettings;
import dev.caecorthus.sparkassist.config.SparkAssistConfig.GuidebookDecor;
import dev.caecorthus.sparkassist.config.SparkAssistConfig.InstinctKeyMode;
import dev.caecorthus.sparkassist.sound.EventSoundGroup;
import dev.doctor4t.wathe.WatheConfig.SnowModeConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the pages of the SparkDLC settings screen from the settings each installed mod exposes. Pages and groups
 * with nothing in them are left out, so a missing sibling mod simply hides its rows.
 * 根据各已安装模组提供的设置构建 SparkDLC 设置界面的页面。没有内容的页面与分组会被省略，缺少某个模组时其行自然隐藏。
 */
public final class SparkDlcSettingsPages {
    public static final String GENERAL = "general";
    public static final String AUDIO = "audio";
    public static final String SCOPE = "scope";
    public static final String DISPLAY = "display";

    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Settings");
    private static final Text SPARKASSIST = Text.literal("SparkAssist");
    private static final Text WATHE = Text.literal("Wathe");
    private static final Text SPARKWITCH = Text.literal("SparkWitch");
    private static final SettingsStore SCOPE_STORE = new SettingsStore("sparkwitch-client", SparkWitchScopeBridge::save);

    private SparkDlcSettingsPages() {
    }

    public static List<SettingsPage> build() {
        List<SettingsPage> pages = new ArrayList<>();
        pages.add(general());
        pages.add(audio());
        if (SparkWitchScopeBridge.available()) {
            try {
                pages.add(scope());
            } catch (IllegalArgumentException exception) {
                // SparkWitch answered with values its own defaults are not among; leave the page out.
                // SparkWitch 返回的取值不包含其默认值时，不显示此页。
                LOGGER.warn("SparkWitch scope settings are inconsistent; hiding the scope page", exception);
            }
        }
        pages.add(display());
        return pages.stream().filter(page -> !page.isEmpty()).toList();
    }

    private static SettingsPage general() {
        ChoiceOption<InstinctKeyMode> instinct = new ChoiceOption<>(
                "instinct_key_mode",
                Text.translatable("option.sparkassist.instinct_key_mode"),
                Text.translatable("option.sparkassist.instinct_key_mode.description"),
                SPARKASSIST,
                SparkAssistClientSettings.STORE,
                List.of(InstinctKeyMode.values()),
                SparkAssistClientSettings::instinctKeyMode,
                SparkAssistClientSettings::setInstinctKeyMode,
                InstinctKeyMode.HOLD,
                mode -> Text.translatable(mode.getTranslationKey()));
        ChoiceOption<GuidebookDecor> decor = new ChoiceOption<>(
                "guidebook_decor",
                Text.translatable("option.sparkassist.guidebook_decor"),
                Text.translatable("option.sparkassist.guidebook_decor.description"),
                SPARKASSIST,
                SparkAssistClientSettings.STORE,
                List.of(GuidebookDecor.values()),
                SparkAssistClientSettings::guidebookDecor,
                SparkAssistClientSettings::setGuidebookDecor,
                GuidebookDecor.MEDIUM,
                decorLevel -> Text.translatable(decorLevel.getTranslationKey()));
        ToggleOption foliage = new ToggleOption(
                "guidebook_foliage",
                Text.translatable("option.sparkassist.guidebook_foliage"),
                Text.translatable("option.sparkassist.guidebook_foliage.description"),
                SPARKASSIST,
                SparkAssistClientSettings.STORE,
                SparkAssistClientSettings::guidebookFoliage,
                SparkAssistClientSettings::setGuidebookFoliage,
                true);
        foliage.enabledWhen(() -> SparkAssistClientSettings.guidebookDecor() != GuidebookDecor.OFF,
                () -> Text.translatable("option.sparkassist.guidebook_foliage.needs_decor"));
        return new SettingsPage(GENERAL, Text.translatable("screen.sparkassist.settings.page.general"), List.of(
                new SettingsPage.Group("controls", Text.translatable("screen.sparkassist.settings.group.controls"),
                        List.of(instinct)),
                new SettingsPage.Group("guidebook", Text.translatable("screen.sparkassist.settings.group.guidebook"),
                        List.of(decor, foliage))));
    }

    private static SettingsPage audio() {
        List<SettingsPage.Group> groups = new ArrayList<>();
        SliderOption master = new SliderOption(
                "event_volume.master",
                Text.translatable("option.sparkassist.event_sound_volume.master"),
                Text.translatable("option.sparkassist.event_sound_volume.master.description"),
                SPARKASSIST,
                SparkAssistClientSettings.STORE,
                0, 100, 1,
                () -> (int) Math.round(SparkAssistClientSettings.eventSoundMasterVolume() * 100.0D),
                percent -> SparkAssistClientSettings.setEventSoundMasterVolume(percent / 100.0D),
                100,
                SparkDlcSettingsPages::percent);
        groups.add(new SettingsPage.Group("master",
                Text.translatable("screen.sparkassist.settings.group.event_master"), List.of(master)));

        List<List<EventVolumeRows.Row>> sections = EventVolumeRows.visibleBySection(row ->
                FabricLoader.getInstance().isModLoaded(row.mod()) && preview(row).available());
        EventVolumeRows.Section[] order = EventVolumeRows.Section.values();
        for (int i = 0; i < order.length; i++) {
            List<SettingsOption> volumes = new ArrayList<>();
            for (EventVolumeRows.Row row : sections.get(i)) {
                volumes.add(eventVolume(row));
            }
            groups.add(new SettingsPage.Group(order[i].id(), Text.translatable(order[i].titleKey()), volumes));
        }
        return new SettingsPage(AUDIO, Text.translatable("screen.sparkassist.settings.page.audio"), groups);
    }

    static SliderOption eventVolume(EventVolumeRows.Row row) {
        EventSoundGroup group = row.group();
        Text sound = Text.translatable(group.translationKey());
        Text label = row.whoKey() == null ? sound : Text.translatableWithFallback(row.whoKey(), row.whoFallback());
        SliderOption option = new SliderOption(
                "event_volume." + group.serializedName(),
                label,
                Text.translatable(group.translationKey() + ".description"),
                modName(row.mod()),
                SparkAssistClientSettings.STORE,
                0, 100, 1,
                () -> (int) Math.round(SparkAssistClientSettings.eventSoundVolume(group) * 100.0D),
                percent -> SparkAssistClientSettings.setEventSoundVolume(group, percent / 100.0D),
                100,
                SparkDlcSettingsPages::percent);
        if (row.whoKey() != null) {
            option.withDetail(sound, row.colour());
        }
        option.withPreview(preview(row));
        return option;
    }

    private static SoundPreview preview(EventVolumeRows.Row row) {
        return new SoundPreview(row.preview().id(), row.preview().category());
    }

    private static SettingsPage scope() {
        ChoiceOption<String> mode = new ChoiceOption<>(
                "sparkwitch.scope_mode",
                Text.translatable("option.sparkwitch.scope_mode"),
                Text.translatable("option.sparkassist.scope_mode.description"),
                SPARKWITCH,
                SCOPE_STORE,
                SparkWitchScopeBridge.scopeModes(),
                SparkWitchScopeBridge::scopeMode,
                SparkWitchScopeBridge::setScopeMode,
                SparkWitchScopeBridge.defaultScopeMode(),
                name -> Text.translatable("option.sparkwitch.scope_mode." + name));
        mode.describeValues(name -> {
            Text tooltip = Text.translatable("option.sparkwitch.scope_mode." + name + ".tooltip");
            if (SparkWitchScopeBridge.PICTURE_IN_PICTURE.equals(name)
                    && SparkWitchScopeBridge.pictureInPictureSuppressed()) {
                return Text.empty().append(tooltip).append("\n\n")
                        .append(Text.translatable("option.sparkassist.scope_mode.suppressed"));
            }
            return tooltip;
        });
        ChoiceOption<Integer> lens = new ChoiceOption<>(
                "sparkwitch.scope_lens_resolution",
                Text.translatable("option.sparkwitch.scope_lens_resolution"),
                Text.translatable("option.sparkwitch.scope_lens_resolution.tooltip"),
                SPARKWITCH,
                SCOPE_STORE,
                SparkWitchScopeBridge.lensResolutionChoices(),
                SparkWitchScopeBridge::lensResolutionPercent,
                SparkWitchScopeBridge::setLensResolutionPercent,
                SparkWitchScopeBridge.defaultLensResolutionPercent(),
                SparkDlcSettingsPages::percent);
        lens.enabledWhen(() -> SparkWitchScopeBridge.PICTURE_IN_PICTURE.equals(SparkWitchScopeBridge.scopeMode()),
                () -> Text.translatable("option.sparkassist.scope_lens_resolution.needs_pip"));
        SliderOption sensitivity = new SliderOption(
                "sparkwitch.scope_sensitivity",
                Text.translatable("option.sparkwitch.scope_sensitivity"),
                Text.translatable("option.sparkwitch.scope_sensitivity.tooltip"),
                SPARKWITCH,
                SCOPE_STORE,
                SparkWitchScopeBridge.scopedSensitivityMin(),
                SparkWitchScopeBridge.scopedSensitivityMax(),
                SparkWitchScopeBridge.scopedSensitivityStep(),
                SparkWitchScopeBridge::scopedSensitivityPercent,
                SparkWitchScopeBridge::setScopedSensitivityPercent,
                SparkWitchScopeBridge.defaultScopedSensitivityPercent(),
                SparkDlcSettingsPages::percent);
        return new SettingsPage(SCOPE, Text.translatable("screen.sparkassist.settings.page.scope"), List.of(
                new SettingsPage.Group("scope", Text.translatable("screen.sparkassist.settings.group.scope"),
                        List.of(mode, lens, sensitivity))));
    }

    private static SettingsPage display() {
        ChoiceOption<SnowModeConfig> snowMode = new ChoiceOption<>(
                "wathe.snow_mode",
                Text.translatable("option.sparkassist.wathe.snow_mode"),
                Text.translatable("wathe.midnightconfig.snowOptLevel.tooltip"),
                WATHE,
                WatheClientSettings.STORE,
                WatheClientSettings.snowModes(),
                WatheClientSettings::snowMode,
                WatheClientSettings::setSnowMode,
                WatheClientSettings.DEFAULT_SNOW_MODE,
                mode -> Text.translatable("wathe.midnightconfig.enum.SnowModeConfig." + mode.name()));
        SliderOption snowflakes = new SliderOption(
                "wathe.snowflake_chance",
                Text.translatable("option.sparkassist.wathe.snowflake_amount"),
                Text.translatable("wathe.midnightconfig.snowflakeChance.tooltip"),
                WATHE,
                WatheClientSettings.STORE,
                WatheClientSettings.SNOWFLAKE_CHANCE_MIN, WatheClientSettings.SNOWFLAKE_CHANCE_MAX, 1,
                WatheClientSettings::snowflakeChance,
                WatheClientSettings::setSnowflakeChance,
                WatheClientSettings.DEFAULT_SNOWFLAKE_CHANCE,
                SparkDlcSettingsPages::percent);
        snowflakes.enabledWhen(() -> WatheClientSettings.snowMode() != SnowModeConfig.TURN_OFF,
                () -> Text.translatable("option.sparkassist.wathe.snowflake_amount.needs_particles"));
        return new SettingsPage(DISPLAY, Text.translatable("screen.sparkassist.settings.page.display"), List.of(
                new SettingsPage.Group("snow", Text.translatable("screen.sparkassist.settings.group.train_effects"),
                        List.of(snowMode, snowflakes))));
    }

    /** The installed mod's display name, so a volume row says whose sounds it scales. 已安装模组的显示名。 */
    private static Text modName(String modId) {
        return Text.literal(FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getName())
                .orElse(modId));
    }

    static Text percent(int value) {
        return Text.translatable("screen.sparkassist.settings.percent", String.format(Locale.ROOT, "%d", value));
    }
}
