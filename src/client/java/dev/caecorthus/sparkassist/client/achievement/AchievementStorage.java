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
    // How often a save file that could not be read is tried again. The page asks for the ledger every tick, so
    // this keeps a locked file from being read 20 times a second.
    // 读不出来的存档文件每隔多久重读一次。成就页每 tick 都会取存档，这样被锁住的文件不会每秒被读 20 次。
    private static final long UNREADABLE_RETRY_MILLIS = 10_000;

    private static String loadedAccount;
    private static AchievementLedger ledger;
    // The account's save file exists but could not be read and is still in place. Until a later load succeeds,
    // the in-memory ledger is never saved over it.
    // 账号的存档文件存在但读不出来、且仍在原处。在之后某次读取成功之前，内存中的存档绝不会覆盖它。
    private static boolean unreadable;
    private static long retryAtMillis;

    private AchievementStorage() {
    }

    /**
     * The current account's ledger, loaded on first use, and loaded again now and then while its file cannot be read.
     * 当前账号的存档，首次使用时读取；存档文件读不出来期间会不时重新读取。
     */
    public static AchievementLedger ledger(MinecraftClient client) {
        String account = account(client);
        long now = System.currentTimeMillis();
        boolean switched = ledger == null || !Objects.equals(account, loadedAccount);
        if (switched || (unreadable && now >= retryAtMillis)) {
            AchievementFiles.Loaded loaded = AchievementFiles.load(file(account), now);
            if (loaded.quarantined() != null) {
                LOGGER.error("Achievement save for {} was unreadable and was moved to {}", account, loaded.quarantined());
            }
            if (loaded.unreadable()) {
                retryAtMillis = now + UNREADABLE_RETRY_MILLIS;
                if (switched) {
                    LOGGER.error("Achievement save for {} exists but could not be read; it is left in place and nothing"
                            + " is saved over it until it can be read", account);
                }
            } else if (!switched) {
                LOGGER.warn("Achievement save for {} can be used again; rounds settled while it could not be read"
                        + " were not saved", account);
            }
            // A retry that fails again keeps this session's ledger. 重读再次失败时保留本次会话的存档。
            if (switched || !loaded.unreadable()) {
                ledger = loaded.ledger();
                loadedAccount = account;
            }
            unreadable = loaded.unreadable();
        }
        return ledger;
    }

    public static void save() {
        if (ledger == null) {
            return;
        }
        if (unreadable) {
            LOGGER.warn("Did not save achievements for {}: the save file could not be read and is not overwritten",
                    loadedAccount);
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
