package dev.caecorthus.sparkassist.achievement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Reads and writes one {@link AchievementLedger} file. A save goes to a temporary sibling first and is then moved
 * over the real file, so a crash mid-write leaves the previous save intact. A file that cannot be read is renamed
 * aside instead of being overwritten by an empty save.
 * 读写单个 {@link AchievementLedger} 文件。保存时先写到同目录的临时文件，再替换正式文件，
 * 写到一半崩溃也不会损坏上一次的存档。读不出来的文件会被改名留存，而不是被空存档覆盖。
 */
public final class AchievementFiles {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private AchievementFiles() {
    }

    /**
     * Loads {@code file}; a missing file is an empty ledger. An unreadable one is moved to
     * {@code <name>.broken-<epochMillis>} and reported in the result.
     * 读取 {@code file}；文件不存在时返回空存档。读不出来的文件会被改名为 {@code <name>.broken-<epochMillis>}，并在结果里注明。
     */
    public static Loaded load(Path file, long epochMillis) {
        if (!Files.isRegularFile(file)) {
            return new Loaded(AchievementLedger.empty(), null);
        }
        try {
            return new Loaded(AchievementLedger.fromJson(JsonParser.parseString(Files.readString(file)).getAsJsonObject()), null);
        } catch (IOException | RuntimeException exception) {
            Path aside = file.resolveSibling(file.getFileName() + ".broken-" + epochMillis);
            try {
                Files.move(file, aside, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveFailure) {
                aside = null;
            }
            return new Loaded(AchievementLedger.empty(), aside);
        }
    }

    public static void save(Path file, AchievementLedger ledger) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, GSON.toJson(ledger.toJson()));
        try {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * A loaded ledger; {@code quarantined} is where an unreadable file was moved, or null.
     * 读取结果；{@code quarantined} 是读不出来的文件被移到的位置，没有则为 null。
     */
    public record Loaded(AchievementLedger ledger, Path quarantined) {
    }
}
