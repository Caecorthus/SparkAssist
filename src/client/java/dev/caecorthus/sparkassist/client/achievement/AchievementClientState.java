package dev.caecorthus.sparkassist.client.achievement;

import dev.caecorthus.sparkassist.achievement.Achievement;
import dev.caecorthus.sparkassist.achievement.AchievementCatalog;
import dev.caecorthus.sparkassist.achievement.AchievementContext;
import dev.caecorthus.sparkassist.achievement.AchievementEvaluator;
import dev.caecorthus.sparkassist.achievement.AchievementLedger;
import dev.caecorthus.sparkassist.achievement.MatchTimeline;
import dev.caecorthus.sparkassist.achievement.RoundFacts;
import dev.caecorthus.sparkassist.achievement.RoundObservation;
import dev.caecorthus.sparkassist.achievement.RoundOutcome;
import dev.caecorthus.sparkassist.achievement.RoundStatsRecorder;
import dev.caecorthus.sparkassist.achievement.RoundTracker;
import dev.caecorthus.sparkassist.client.guidebook.SparkTraitsGuideBridge;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Follows Wathe rounds on the client and settles each finished one into the local achievement save. Everything it
 * reads is already synced to this client; it never sends anything to the server.
 * 在客户端跟踪 Wathe 对局，并把每局结果结算进本地成就存档。读取的都是已同步到本客户端的数据，从不向服务器发送任何内容。
 */
public final class AchievementClientState {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Achievements");
    // Owner-visible traits are polled once a second; they change rarely. 自身可见词条每秒轮询一次，变化很少。
    private static final int TRAIT_POLL_TICKS = 20;

    private static final RoundTracker TRACKER = new RoundTracker();
    private static Object recordAtStart;
    private static Set<String> traits = Set.of();
    private static int traitPollCountdown;
    // The types behind RoundObservation.EXPLOSIVE_ENTITY_IDS that this client knows, resolved once.
    // RoundObservation.EXPLOSIVE_ENTITY_IDS 中本客户端已注册的实体类型，只解析一次。
    private static Set<EntityType<?>> explosiveTypes;

    private AchievementClientState() {
    }

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (client.world == null || player == null) {
            TRACKER.abort();
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(client.world);
        GameWorldComponent.GameStatus status = game.getGameStatus();
        boolean live = status == GameWorldComponent.GameStatus.ACTIVE || status == GameWorldComponent.GameStatus.STOPPING;
        if (!live) {
            if (TRACKER.active()) {
                TRACKER.finish(LocalDateTime.now()).ifPresent(facts -> settle(client, game, facts));
            }
            return;
        }
        if (!TRACKER.active()) {
            TRACKER.start(player.getUuid());
            recordAtStart = MatchRecordBridge.latest();
            traits = Set.of();
            traitPollCountdown = 0;
        }

        UUID self = player.getUuid();
        Role role = game.getRole(self);
        if (role == WatheRoles.NO_ROLE) {
            role = null;
        }
        boolean dead = game.isPlayerDead(self);
        if (--traitPollCountdown <= 0) {
            traitPollCountdown = TRAIT_POLL_TICKS;
            if (dead) {
                traits = Set.of();
            } else if (!player.isSpectator()) {
                traits = SparkTraitsGuideBridge.ownerVisibleActiveTraitIds(player);
            }
            // A living spectator is in a fake death (e.g. Depression): keep the last traits rather than read what
            // spectators are sent. 存活的旁观者处于假死（例如抑郁）：沿用上次的词条，不读取发给旁观者的数据。
        }
        if (!dead) {
            TRACKER.onDepressionAttacker(SparkTraitsAchievementBridge.depressionPsychoAttacker(player));
        }
        long worldTime = client.world.getTime();
        TRACKER.observe(new RoundObservation(
                worldTime,
                status == GameWorldComponent.GameStatus.STOPPING,
                role == null ? null : role.identifier().toString(),
                SparkFactionAchievementBridge.faction(player, game, role),
                dead,
                game.getAllAlivePlayers().size(),
                game.getAllPlayers().size(),
                traits,
                !dead && explosiveNearby(client.world, player)));
        if (TRACKER.wantsDeathReason(worldTime)) {
            findOwnDeathReason(client, self);
        }
    }

    public static void disconnect() {
        TRACKER.abort();
        recordAtStart = null;
    }

    public static void onRoundEndSynced() {
        TRACKER.onRoundEndSynced();
    }

    public static void onTaskComplete() {
        TRACKER.onTaskComplete();
    }

    public static void onGunDrop() {
        TRACKER.onGunDrop();
    }

    public static void onPoisonOverlay(String translationKey) {
        TRACKER.onPoisonOverlay(translationKey);
    }

    /**
     * Whether a live explosive ({@link RoundObservation#EXPLOSIVE_ENTITY_IDS}) is within
     * {@link RoundObservation#EXPLOSIVE_RANGE_BLOCKS} of the player: a box query around them, then a distance check.
     * 玩家 {@link RoundObservation#EXPLOSIVE_RANGE_BLOCKS} 格内是否有未爆的爆炸物（{@link RoundObservation#EXPLOSIVE_ENTITY_IDS}）：
     * 先按方框查询玩家周围的实体，再检查距离。
     */
    private static boolean explosiveNearby(ClientWorld world, ClientPlayerEntity player) {
        Set<EntityType<?>> types = explosiveTypes();
        double range = RoundObservation.EXPLOSIVE_RANGE_BLOCKS;
        return !world.getOtherEntities(player, player.getBoundingBox().expand(range), entity ->
                types.contains(entity.getType()) && entity.squaredDistanceTo(player) <= range * range).isEmpty();
    }

    /**
     * The nearby check runs every tick for every entity around the player, so it compares these types rather than
     * build an id string per entity.
     * 附近检查每 tick 都会遍历玩家周围的每个实体，因此比较这些类型，而不是为每个实体拼出 id 字符串。
     */
    private static Set<EntityType<?>> explosiveTypes() {
        if (explosiveTypes == null) {
            Set<EntityType<?>> types = new HashSet<>();
            for (String id : RoundObservation.EXPLOSIVE_ENTITY_IDS) {
                Identifier parsed = Identifier.tryParse(id);
                if (parsed != null && Registries.ENTITY_TYPE.containsId(parsed)) {
                    types.add(Registries.ENTITY_TYPE.get(parsed));
                }
            }
            explosiveTypes = Set.copyOf(types);
        }
        return explosiveTypes;
    }

    private static void findOwnDeathReason(MinecraftClient client, UUID self) {
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof PlayerBodyEntity body && self.equals(body.getPlayerUuid())) {
                Identifier reason = body.getDeathReason();
                if (reason != null) {
                    TRACKER.onDeathReason(reason.toString());
                }
                return;
            }
        }
    }

    private static void settle(MinecraftClient client, GameWorldComponent game, RoundFacts facts) {
        RoundOutcome outcome = outcome(client.world.getScoreboard(), game, facts.self());
        if (outcome.participant(facts.self()).isEmpty()) {
            // Wathe's result does not list us (e.g. we joined after roles were dealt): not our round.
            // Wathe 的结算里没有我们（例如分配角色后才加入）：这不是我们的对局。
            return;
        }
        MatchTimeline timeline = MatchTimeline.of(outcome.events(), SparkFactionAchievementBridge::baseFaction);
        AchievementLedger ledger = AchievementStorage.ledger(client);
        AchievementLedger.Place place = place(client);
        try {
            RoundStatsRecorder.record(ledger.stats(), facts, outcome, timeline);
        } catch (RuntimeException exception) {
            // Unexpected record data must never break the game; this round's stats may be partly counted.
            // 意外的记录数据绝不能影响游戏；本局统计可能只计入了一部分。
            LOGGER.warn("Could not count this round's stats", exception);
        }
        List<Achievement> earned = AchievementEvaluator.newlyEarned(
                AchievementCatalog.all(),
                new AchievementContext(facts, outcome, timeline, ledger.stats(), ledger),
                place,
                (achievement, exception) -> LOGGER.warn("Achievement {} failed to evaluate", achievement.id(), exception));
        long now = System.currentTimeMillis();
        earned.forEach(achievement -> ledger.unlock(achievement.id(), now, place));
        AchievementStorage.save();
        AchievementAdvancements.announce(client, earned);
    }

    /**
     * The local world when this client hosts the game (including a world opened to LAN), otherwise the server address
     * as entered in the server list or Direct Connect; null when neither is known.
     * 本客户端自己运行游戏时（包括对局域网开放的世界）为本地世界，否则为服务器列表或直接连接里填写的地址；两者都不知道时为 null。
     */
    static AchievementLedger.Place place(MinecraftClient client) {
        if (client.isIntegratedServerRunning()) {
            return AchievementLedger.Place.LOCAL_WORLD;
        }
        ServerInfo server = client.getCurrentServerEntry();
        return server == null || server.address == null || server.address.isBlank()
                ? null
                : new AchievementLedger.Place(server.address);
    }

    private static RoundOutcome outcome(Scoreboard scoreboard, GameWorldComponent game, UUID self) {
        GameRoundEndComponent roundEnd = GameRoundEndComponent.KEY.get(scoreboard);
        GameFunctions.WinStatus winStatus = roundEnd.getWinStatus();
        List<RoundOutcome.Participant> participants = new ArrayList<>();
        for (GameRoundEndComponent.RoundEndData data : roundEnd.getPlayers()) {
            participants.add(new RoundOutcome.Participant(
                    data.player().getId(),
                    data.player().getName(),
                    data.role().toString(),
                    data.endStatus().name(),
                    data.isWinner()));
        }
        boolean won = winStatus == GameFunctions.WinStatus.LOOSE_END
                ? self.equals(game.getLooseEndWinner())
                : roundEnd.didWin(self);
        String customFaction = SparkFactionAchievementBridge.customWinningFaction(scoreboard);
        Object record = MatchRecordBridge.latest();
        return new RoundOutcome(
                winStatus.name(),
                won,
                customFaction != null ? customFaction : RoundOutcome.defaultWinningFaction(winStatus.name()),
                participants,
                record != recordAtStart ? MatchRecordBridge.events(record) : List.of());
    }
}
