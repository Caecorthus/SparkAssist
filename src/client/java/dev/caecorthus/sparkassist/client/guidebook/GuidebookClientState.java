package dev.caecorthus.sparkassist.client.guidebook;

import dev.caecorthus.sparkassist.guidebook.GuidebookEntryPoint;
import dev.caecorthus.sparkassist.guidebook.GuidebookSessionState;
import dev.caecorthus.sparkassist.guidebook.GuidebookObservationRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import java.util.Set;
import net.minecraft.client.MinecraftClient;

/**
 * Keeps personal guide markers for exactly one Wathe round.
 * 仅在单局 Wathe 游戏期间保留玩家自己的指南标记。
 */
public final class GuidebookClientState {
    private static final GuidebookSessionState SESSION = new GuidebookSessionState();
    private static boolean directoryFolded;

    private GuidebookClientState() {
    }

    public static GuidebookSessionState session() {
        return SESSION;
    }

    /** The player folded the persistent directory to its book button. Client-lifetime only: kept across rounds
     * and disconnects, never written to disk, and not part of the per-round session.
     * 玩家把常驻目录收起为书本按钮。仅在客户端运行期间有效：跨对局与断线保留，不写入磁盘，也不属于单局会话状态。 */
    public static boolean directoryFolded() {
        return directoryFolded;
    }

    public static void setDirectoryFolded(boolean folded) {
        directoryFolded = folded;
    }

    public static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            if (SESSION.roundActive()) {
                SESSION.endRound();
            }
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(client.world);
        boolean roundLive = game.getGameStatus() == GameWorldComponent.GameStatus.ACTIVE
                || game.getGameStatus() == GameWorldComponent.GameStatus.STOPPING;
        if (!roundLive) {
            if (SESSION.roundActive()) {
                SESSION.endRound();
            }
            return;
        }
        if (!SESSION.roundActive()) {
            SESSION.startRound();
        }

        Role role = game.getRole(client.player);
        String roleId = role == null ? null : role.identifier().toString();
        boolean dead = game.isPlayerDead(client.player.getUuid());
        boolean spectator = client.player.isSpectator();
        boolean creative = client.player.isCreative();
        Set<String> visibleTraits = GuidebookObservationRules.shouldPollOwnerTraits(true, dead, spectator, creative)
                ? SparkTraitsGuideBridge.ownerVisibleActiveTraitIds(client.player)
                : Set.of();
        SESSION.observe(roleId, visibleTraits);
    }

    public static GuidebookEntryPoint entryPoint(MinecraftClient client) {
        boolean roundHud = WatheClient.trainComponent != null && WatheClient.trainComponent.hasHud();
        boolean spectator = client.player != null && client.player.isSpectator();
        return GuidebookEntryPoint.resolve(roundHud, WatheClient.isPlayerAliveAndInSurvival(), spectator);
    }

    public static void disconnect() {
        SESSION.disconnect();
    }
}
