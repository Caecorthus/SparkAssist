package dev.caecorthus.sparkassist.client.settings;

import java.util.Objects;

/**
 * A config file that settings write into. Options share one instance per file, so a flush saves each file once.
 * 设置写入的配置文件。同一文件的选项共用一个实例，刷新时每个文件只保存一次。
 */
public final class SettingsStore {
    private final String id;
    private final Runnable save;

    public SettingsStore(String id, Runnable save) {
        this.id = Objects.requireNonNull(id);
        this.save = Objects.requireNonNull(save);
    }

    public String id() {
        return id;
    }

    public void save() {
        save.run();
    }

    @Override
    public String toString() {
        return "SettingsStore[" + id + "]";
    }
}
