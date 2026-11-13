package dev.caecorthus.sparkassist;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;

/**
 * Crash-safe file replacement shared by the settings file and the achievement save: the text goes to a
 * {@code <name>.tmp} sibling, is forced to disk and is then moved over the file (atomically where the file system
 * allows), so a crash or power cut mid-write keeps the previous file instead of a cut-off one. Every failure is
 * thrown; each caller keeps its own policy for it.
 * 设置文件与成就存档共用的防崩溃写入：先写到同目录的 {@code <name>.tmp}，刷到磁盘，再（文件系统支持时以原子方式）
 * 替换正式文件，写到一半崩溃或断电时保留上一次的文件，而不是被截断的文件。所有失败都会抛出，由各调用方按自己的策略处理。
 */
public final class DurableFiles {
    private DurableFiles() {
    }

    /**
     * Replaces {@code file} with {@code text} in UTF-8, creating its directory first. {@code text} is produced only
     * once the temporary file is open, the order both callers have always used.
     * 以 UTF-8 用 {@code text} 替换 {@code file}，必要时先创建目录。{@code text} 在临时文件打开之后才生成，与两个调用方
     * 一直以来的顺序相同。
     */
    public static void write(Path file, Supplier<String> text) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        // Forced to disk before the move: after a power cut some file systems keep the rename but not the data.
        // 移动前先刷到磁盘：断电后有些文件系统会保留改名，却丢失内容。
        try (FileChannel channel = FileChannel.open(temporary,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer bytes = ByteBuffer.wrap(text.get().getBytes(StandardCharsets.UTF_8));
            while (bytes.hasRemaining()) {
                channel.write(bytes);
            }
            channel.force(true);
        }
        try {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
