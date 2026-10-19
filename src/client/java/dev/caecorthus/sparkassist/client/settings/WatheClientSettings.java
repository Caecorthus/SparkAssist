package dev.caecorthus.sparkassist.client.settings;

import dev.doctor4t.wathe.WatheConfig;
import dev.doctor4t.wathe.WatheConfig.SnowModeConfig;
import eu.midnightdust.lib.config.MidnightConfig;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wathe's own snow choices, which live in its MidnightConfig file {@code config/wathe.json} and have no in-game screen
 * unless Mod Menu is installed. Values are Wathe's static fields, read live by its snowflake spawner. Defaults are the
 * field initialisers of the pinned Wathe 1.5.6-spark. Its {@code disableScreenShake} entry is left out on purpose:
 * nothing in that version reads it.
 * Wathe 自带的雪花选项，保存在其 MidnightConfig 文件 {@code config/wathe.json} 中；没有 Mod Menu 时游戏内无处修改。
 * 值即 Wathe 的静态字段，由其雪花生成逻辑实时读取。默认值取自锁定版本 Wathe 1.5.6-spark 的字段初始值。
 * 刻意不包含 {@code disableScreenShake}：该版本中没有任何代码读取它。
 */
public final class WatheClientSettings {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Settings");
    private static final String WATHE_CONFIG_ID = "wathe";
    private static boolean saveFailureLogged;

    public static final SettingsStore STORE = new SettingsStore(WATHE_CONFIG_ID, WatheClientSettings::save);

    public static final SnowModeConfig DEFAULT_SNOW_MODE = SnowModeConfig.NO_OPTIMIZATION;
    public static final int DEFAULT_SNOWFLAKE_CHANCE = 100;
    public static final int SNOWFLAKE_CHANCE_MIN = 0;
    public static final int SNOWFLAKE_CHANCE_MAX = 100;

    private WatheClientSettings() {
    }

    public static List<SnowModeConfig> snowModes() {
        return List.of(SnowModeConfig.values());
    }

    public static SnowModeConfig snowMode() {
        return WatheConfig.snowOptLevel == null ? DEFAULT_SNOW_MODE : WatheConfig.snowOptLevel;
    }

    public static void setSnowMode(SnowModeConfig mode) {
        WatheConfig.snowOptLevel = mode == null ? DEFAULT_SNOW_MODE : mode;
    }

    public static int snowflakeChance() {
        return WatheConfig.snowflakeChance;
    }

    public static void setSnowflakeChance(int chance) {
        WatheConfig.snowflakeChance = Math.max(SNOWFLAKE_CHANCE_MIN, Math.min(SNOWFLAKE_CHANCE_MAX, chance));
    }

    /** Writes {@code config/wathe.json} through MidnightLib, as Wathe's own screen would. 经 MidnightLib 写盘。 */
    public static void save() {
        try {
            MidnightConfig.write(WATHE_CONFIG_ID);
        } catch (RuntimeException | LinkageError exception) {
            // The values still apply for this session; only persistence failed.
            // 本次运行中的值仍然生效，只是没能写盘。
            if (!saveFailureLogged) {
                saveFailureLogged = true;
                LOGGER.warn("Could not save Wathe's client config", exception);
            }
        }
    }
}
