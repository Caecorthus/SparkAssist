package dev.caecorthus.sparkassist;

import net.minecraft.util.Identifier;

public final class SparkAssist {
    public static final String MOD_ID = "sparkassist";

    private SparkAssist() {
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
