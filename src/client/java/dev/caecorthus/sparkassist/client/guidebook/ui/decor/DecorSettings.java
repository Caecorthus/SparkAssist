package dev.caecorthus.sparkassist.client.guidebook.ui.decor;

import dev.caecorthus.sparkassist.client.config.SparkAssistClientSettings;
import dev.caecorthus.sparkassist.config.SparkAssistConfig.GuidebookDecor;

/**
 * The decoration settings as the painters read them each frame, plus the developer overlay switch
 * ({@code -Dsparkassist.decorDebug=true} paints the no-go rectangles in red).
 * 绘制时读取的点缀设置，以及开发用的禁区叠加开关（-Dsparkassist.decorDebug=true 用红色画出禁区）。
 */
public final class DecorSettings {
    private static final boolean DEBUG_ZONES = Boolean.getBoolean("sparkassist.decorDebug");

    private DecorSettings() {
    }

    /** -1 off, 0 light, 1 medium, 2 full. 关为 -1，轻 0，中 1，满 2。 */
    public static int density() {
        return SparkAssistClientSettings.guidebookDecor().density();
    }

    public static boolean enabled() {
        return SparkAssistClientSettings.guidebookDecor() != GuidebookDecor.OFF;
    }

    public static boolean foliage() {
        return enabled() && SparkAssistClientSettings.guidebookFoliage();
    }

    public static boolean debugZones() {
        return DEBUG_ZONES;
    }

    /** A value that changes whenever any decoration setting changes, for layer cache keys. 设置变化即变的缓存键成分。 */
    public static int revision() {
        return SparkAssistClientSettings.guidebookDecor().ordinal() * 2 + (SparkAssistClientSettings.guidebookFoliage() ? 1 : 0);
    }
}
