package dev.caecorthus.sparkassist.achievement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import dev.caecorthus.sparkassist.DurableFiles;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Reads and writes one {@link AchievementLedger} file. A save goes to a temporary sibling first, is forced to disk and
 * is then moved over the real file, so a crash or power cut mid-write leaves the previous save intact. A file whose
 * content is broken is renamed aside instead of being overwritten by an empty save; one that cannot be read at all
 * (or cannot be renamed aside) is left in place and reported as {@link Loaded#unreadable()}, and must not be saved
 * over.
 * 读写单个 {@link AchievementLedger} 文件。保存时先写到同目录的临时文件并刷到磁盘，再替换正式文件，
 * 写到一半崩溃或断电也不会损坏上一次的存档。内容损坏的文件会被改名留存，而不是被空存档覆盖；完全读不出来
 * （或无法改名留存）的文件留在原处，并以 {@link Loaded#unreadable()} 报告，不得覆盖。
 */
public final class AchievementFiles {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private AchievementFiles() {
    }

    /**
     * Loads {@code file}; a missing file is an empty ledger. One whose content is broken is moved to
     * {@code <name>.broken-<epochMillis>} and reported in the result. One that cannot be read (perhaps locked for a
     * moment by another program) or cannot be moved aside stays where it is and is reported as unreadable.
     * 读取 {@code file}；文件不存在时返回空存档。内容损坏的文件会被改名为 {@code <name>.broken-<epochMillis>}，并在结果里注明。
     * 读不出来（可能被其他程序暂时锁住）或无法改名留存的文件保持原位，并在结果里标为读不出来。
     */
    public static Loaded load(Path file, long epochMillis) {
        // Only a file known to be absent is a new, empty save. 只有确定不存在的文件才算新的空存档。
        if (Files.notExists(file)) {
            return new Loaded(AchievementLedger.empty(), null);
        }
        try {
            return new Loaded(AchievementLedger.fromJson(JsonParser.parseString(Files.readString(file)).getAsJsonObject()), null);
        } catch (CharacterCodingException | RuntimeException exception) {
            Path aside = file.resolveSibling(file.getFileName() + ".broken-" + epochMillis);
            try {
                Files.move(file, aside, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveFailure) {
                return new Loaded(AchievementLedger.empty(), null, true);
            }
            return new Loaded(AchievementLedger.empty(), aside);
        } catch (IOException readFailure) {
            return new Loaded(AchievementLedger.empty(), null, true);
        }
    }

    public static void save(Path file, AchievementLedger ledger) throws IOException {
        DurableFiles.write(file, () -> GSON.toJson(ledger.toJson()));
    }

    /**
     * A loaded ledger; {@code quarantined} is where an unreadable file was moved, or null. {@code unreadable} means
     * the file exists but could not be read and is still in place: the empty ledger returned must never be saved
     * over it, and the file should be loaded again later.
     * 读取结果；{@code quarantined} 是读不出来的文件被移到的位置，没有则为 null。{@code unreadable} 表示文件存在但读不出来、
     * 且仍在原处：返回的空存档绝不能覆盖它，应稍后重新读取。
     */
    public record Loaded(AchievementLedger ledger, Path quarantined, boolean unreadable) {
        public Loaded(AchievementLedger ledger, Path quarantined) {
            this(ledger, quarantined, false);
        }
    }
}
