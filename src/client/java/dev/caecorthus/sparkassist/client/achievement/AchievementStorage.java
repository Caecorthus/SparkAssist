package dev.caecorthus.sparkassist.client.achievement;

import dev.caecorthus.sparkassist.SparkAssist;
import dev.caecorthus.sparkassist.achievement.AchievementFiles;
import dev.caecorthus.sparkassist.achievement.AchievementLedger;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The signed-in account's achievement save at {@code config/sparkassist/achievements/<account uuid>.json}. Nothing
 * here is ever sent to a server; offline and singleplayer rounds use the same file as online ones.
 * 当前登录账号的成就存档，位于 {@code config/sparkassist/achievements/<账号 uuid>.json}。
 * 这里的任何数据都不会发给服务器；离线与单人对局和在线对局共用同一个文件。
 */
public final class AchievementStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Achievements");

    private static String loadedAccount;
    private static AchievementLedger ledger;

    private AchievementStorage() {
    }

    /** The current account's ledger, loaded on first use. 当前账号的存档，首次使用时读取。 */
    public static AchievementLedger ledger(MinecraftClient client) {
        String account = account(client);
        if (ledger == null || !Objects.equals(account, loadedAccount)) {
            AchievementFiles.Loaded loaded = AchievementFiles.load(file(account), System.currentTimeMillis());
            if (loaded.quarantined() != null) {
                LOGGER.warn("Achievement save for {} was unreadable and was moved to {}", account, loaded.quarantined());
            }
            ledger = loaded.ledger();
            loadedAccount = account;
        }
        return ledger;
    }

    public static void save() {
        if (ledger == null) {
            return;
        }
        try {
            AchievementFiles.save(file(loadedAccount), ledger);
        } catch (IOException exception) {
            // Keep playing; the next settled round tries again with the same in-memory ledger.
            // 不影响游戏；下一局结算时会用同一份内存存档再试一次。
            LOGGER.warn("Could not save achievements for {}", loadedAccount, exception);
        }
    }

    private static Path file(String account) {
        return FabricLoader.getInstance().getConfigDir()
                .resolve(SparkAssist.MOD_ID)
                .resolve("achievements")
                .resolve(account + ".json");
    }

    private static String account(MinecraftClient client) {
        UUID uuid = client.getSession().getUuidOrNull();
        if (uuid != null) {
            return uuid.toString();
        }
        return "offline-" + client.getSession().getUsername().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    }
}
