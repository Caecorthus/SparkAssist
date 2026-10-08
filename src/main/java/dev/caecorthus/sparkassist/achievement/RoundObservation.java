package dev.caecorthus.sparkassist.achievement;

import java.util.Set;

/**
 * What the client sees of a live round on one tick.
 * 客户端在某一 tick 看到的进行中对局状态。
 *
 * @param worldTime    the world's time, which stops while a singleplayer game is paused
 *                     世界时间；单人游戏暂停时不会前进
 * @param stopping     the round is decided and fading out (Wathe {@code STOPPING})
 *                     胜负已定、正在淡出（Wathe 的 {@code STOPPING}）
 * @param roleId       the local player's role, or null when they have none
 *                     本地玩家的角色，没有则为 null
 * @param factionId    the faction of that role, or null
 *                     该角色的阵营，没有则为 null
 * @param selfDead     the local player is on Wathe's dead list
 *                     本地玩家在 Wathe 的死亡名单上
 * @param alivePlayers players with a role who are not dead
 *                     有角色且未死亡的玩家数
 * @param totalPlayers players with a role
 *                     有角色的玩家数
 * @param traitIds     the local player's owner-visible traits
 *                     本地玩家自己可见的词条
 */
public record RoundObservation(
        long worldTime,
        boolean stopping,
        String roleId,
        String factionId,
        boolean selfDead,
        int alivePlayers,
        int totalPlayers,
        Set<String> traitIds
) {
    public RoundObservation {
        traitIds = traitIds == null ? Set.of() : Set.copyOf(traitIds);
    }
}
