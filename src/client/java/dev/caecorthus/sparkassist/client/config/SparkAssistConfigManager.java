package dev.caecorthus.sparkassist.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.caecorthus.sparkassist.SparkAssist;
import dev.caecorthus.sparkassist.config.SparkAssistConfig;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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
            // Written to a sibling, forced to disk and then moved over the file (as the achievement save does), so a
            // crash or power cut mid-write keeps the previous settings instead of a cut-off file.
            // 先写到同目录的临时文件并刷到磁盘，再替换正式文件（与成就存档相同），写到一半崩溃或断电时保留上一次的设置，
            // 而不是留下被截断的文件。
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            try (FileChannel channel = FileChannel.open(temporary,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                ByteBuffer bytes = ByteBuffer.wrap(GSON.toJson(config.toJson()).getBytes(StandardCharsets.UTF_8));
                while (bytes.hasRemaining()) {
                    channel.write(bytes);
                }
                channel.force(true);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // Config save failures should not break the client settings screen.
            // 配置保存失败不应导致客户端设置界面崩溃。
        }
    }
}
