package dev.caecorthus.sparkassist.client.settings;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * What one opening of the settings screen changed. Values apply live; each touched store is written once per
 * {@link #flush()}. The values seen when the screen opened stay available for {@link #undoAll()}.
 * 一次打开设置界面期间的改动。值立即生效；每次 flush 时被改动的存储各写盘一次。打开时的值保留，供「撤销」使用。
 */
public final class SettingsSession {
    private final List<SettingsPage> pages;
    /** Keyed by identity (options don't override equals), in page order so Undo saves stores in a stable order.
     * 以对象身份为键（选项不重写 equals），按页面顺序排列，撤销时各存储的保存顺序固定。 */
    private final Map<SettingsOption, Object> opened = new LinkedHashMap<>();
    private final Set<SettingsStore> dirty = new LinkedHashSet<>();

    public SettingsSession(List<SettingsPage> pages) {
        this.pages = List.copyOf(pages);
        for (SettingsPage page : this.pages) {
            page.options().forEach(option -> opened.put(option, option.capture()));
        }
    }

    public List<SettingsPage> pages() {
        return pages;
    }

    /** Records that {@code option} changed; its store is written on the next flush. 记录改动，下次刷新时写盘。 */
    public void changed(SettingsOption option) {
        dirty.add(Objects.requireNonNull(option).store());
    }

    /** Writes every store touched since the last flush. 写入上次刷新以来被改动的存储。 */
    public void flush() {
        if (dirty.isEmpty()) {
            return;
        }
        List<SettingsStore> stores = List.copyOf(dirty);
        dirty.clear();
        stores.forEach(SettingsStore::save);
    }

    public boolean hasUnsavedChanges() {
        return !dirty.isEmpty();
    }

    /** True when any value differs from what it was when the screen opened. 是否有值与打开时不同。 */
    public boolean hasChanges() {
        for (Map.Entry<SettingsOption, Object> entry : opened.entrySet()) {
            if (!Objects.equals(entry.getKey().capture(), entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    /** Puts every value back to what it was when the screen opened, then saves. 全部恢复为打开时的值并保存。 */
    public void undoAll() {
        for (Map.Entry<SettingsOption, Object> entry : opened.entrySet()) {
            SettingsOption option = entry.getKey();
            if (!Objects.equals(option.capture(), entry.getValue())) {
                option.restore(entry.getValue());
                changed(option);
            }
        }
        flush();
    }

    public boolean pageHasNonDefault(SettingsPage page) {
        return page.options().anyMatch(option -> !option.isDefault());
    }

    /** Resets the page's rows to their defaults, then saves. 把本页各行恢复默认并保存。 */
    public void resetPage(SettingsPage page) {
        page.options().forEach(option -> {
            if (!option.isDefault()) {
                option.resetToDefault();
                changed(option);
            }
        });
        flush();
    }
}
