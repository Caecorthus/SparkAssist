package dev.caecorthus.sparkassist.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.caecorthus.sparkassist.SparkAssist;
import dev.caecorthus.sparkassist.config.SparkAssistConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class SparkAssistConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;
    private final SparkAssistConfig config;

    private SparkAssistConfigManager(Path path, SparkAssistConfig config) {
        this.path = path;
        this.config = config;
    }

    public static SparkAssistConfigManager load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(SparkAssist.MOD_ID + ".json");
        SparkAssistConfig config = SparkAssistConfig.defaults();
        if (Files.isRegularFile(path)) {
            try {
                JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                config = SparkAssistConfig.fromJson(json);
            } catch (RuntimeException | IOException ignored) {
                config = SparkAssistConfig.defaults();
            }
        }
        SparkAssistConfigManager manager = new SparkAssistConfigManager(path, config);
        manager.save();
        return manager;
    }

    public SparkAssistConfig config() {
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(config.toJson()));
        } catch (IOException ignored) {
            // Config save failures should not break the client settings screen.
            // 配置保存失败不应导致客户端设置界面崩溃。
        }
    }
}
