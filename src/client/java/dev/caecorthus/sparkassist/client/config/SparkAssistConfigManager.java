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
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SparkAssistConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;
    private final SparkAssistConfig config;
    // False when the file exists but could not be read (or its broken content could not be moved aside): this run
    // then starts from the defaults, keeps every change in memory only and never writes over the file.
    // 文件存在却读不出来（或其损坏的内容无法改名留存）时为 false：本次运行从默认值开始，所有改动只保存在内存中，
    // 绝不覆盖该文件。
    private final boolean writable;

    private SparkAssistConfigManager(Path path, SparkAssistConfig config, boolean writable) {
        this.path = path;
        this.config = config;
        this.writable = writable;
    }

    public static SparkAssistConfigManager load() {
        return load(FabricLoader.getInstance().getConfigDir().resolve(SparkAssist.MOD_ID + ".json"),
                System.currentTimeMillis());
    }

    /**
     * Reads {@code path} and writes it straight back (adding keys a newer version knows). A missing file starts from
     * the defaults. A file whose content is broken is moved to {@code <name>.broken-<epochMillis>} first. One that
     * cannot be read (perhaps locked for a moment by another program) or moved aside is never written over for the
     * rest of this run; the next launch reads it again.
     * 读取 {@code path} 并立即写回（补上新版本认识的键）。文件不存在时从默认值开始。内容损坏的文件会先被改名为
     * {@code <name>.broken-<epochMillis>}。读不出来（可能被其他程序暂时锁住）或无法改名留存的文件，本次运行中绝不覆盖，
     * 下次启动时再读取。
     */
    static SparkAssistConfigManager load(Path path, long epochMillis) {
        SparkAssistConfig config = SparkAssistConfig.defaults();
        boolean writable = true;
        if (!Files.notExists(path)) {
            try {
                JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                config = SparkAssistConfig.fromJson(json);
            } catch (CharacterCodingException | RuntimeException broken) {
                Path aside = path.resolveSibling(path.getFileName() + ".broken-" + epochMillis);
                try {
                    Files.move(path, aside, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.warn("SparkAssist settings in {} could not be parsed and were moved to {}", path, aside);
                } catch (IOException moveFailure) {
                    writable = false;
                }
            } catch (IOException readFailure) {
                writable = false;
            }
            if (!writable) {
                LOGGER.error("SparkAssist settings in {} could not be read; defaults are used and the file is not"
                        + " written until the next launch", path);
            }
        }
        SparkAssistConfigManager manager = new SparkAssistConfigManager(path, config, writable);
        manager.save();
        return manager;
    }

    public SparkAssistConfig config() {
        return config;
    }

    public void save() {
        if (!writable) {
            return;
        }
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
