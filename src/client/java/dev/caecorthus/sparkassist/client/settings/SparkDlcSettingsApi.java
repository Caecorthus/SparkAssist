package dev.caecorthus.sparkassist.client.settings;

/**
 * Frozen public API that sibling mods read by reflection, so they can stop showing settings this screen now hosts.
 * SparkWitch's {@code SparkDlcSettingsBridge} calls {@link #hostsScopeSettings()} on every Accessibility screen init.
 * Keep the class name, the method name and the primitive {@code boolean} return.
 * 供兄弟模组通过反射读取的冻结公开 API，让它们不再重复显示本界面已收纳的设置。SparkWitch 的
 * {@code SparkDlcSettingsBridge} 在每次初始化辅助功能界面时调用 {@link #hostsScopeSettings()}。类名、方法名与基本类型
 * {@code boolean} 返回值都不得更改。
 */
public final class SparkDlcSettingsApi {
    private SparkDlcSettingsApi() {
    }

    /** True when the SparkDLC settings screen shows SparkWitch's scope page. SparkDLC 设置界面是否显示瞄准镜页。 */
    public static boolean hostsScopeSettings() {
        return SparkWitchScopeBridge.available();
    }
}
