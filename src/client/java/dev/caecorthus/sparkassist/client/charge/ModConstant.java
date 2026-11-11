package dev.caecorthus.sparkassist.client.charge;

/**
 * A {@code static int} that another mod owns, read at run time so SparkAssist neither compiles against that mod nor
 * keeps a copy that can go stale. The fallback is used when the mod is absent or the constant has moved.
 * 另一个模组拥有的 {@code static int}，在运行时读取，使 SparkAssist 既不编译期依赖该模组，也不保留一份会过时的副本。模组缺席
 * 或常量已挪走时使用兜底值。
 */
final class ModConstant {
    private final String className;
    private final String fieldName;
    private final int fallback;
    private boolean resolved;
    private int value;

    ModConstant(String className, String fieldName, int fallback) {
        this.className = className;
        this.fieldName = fieldName;
        this.fallback = fallback;
    }

    int value() {
        if (!resolved) {
            resolved = true;
            value = read();
        }
        return value;
    }

    private int read() {
        try {
            int read = Class.forName(className).getField(fieldName).getInt(null);
            return read > 0 ? read : fallback;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return fallback;
        }
    }
}
