package dev.caecorthus.sparkassist.achievement;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Everything the client itself saw during one finished round. Times are world ticks since the round went live.
 * 本地客户端在一局已结束的对局中亲眼看到的一切。时间为对局开始后的世界 tick 数。
 *
 * @param self                 the local player 本地玩家
 * @param roleChain            every role the player held, in order, without repeats in a row
 *                             玩家依次担任过的角色，相邻不重复
 * @param factionId            the faction of the last role, or null 最后一个角色的阵营，没有则为 null
 * @param players              the most players with a role seen at once 同时有角色的玩家数的最大值
 * @param fewestAliveWhileAlive the fewest living players seen while the local player was alive, themselves
 *                             included; 0 when never seen alive
 *                             本地玩家存活期间看到的最少存活人数（含自己）；从未存活时为 0
 * @param traitIds             every owner-visible trait seen while alive 存活期间见过的所有自身可见词条
 * @param durationTicks        ticks from going live to the round being decided 从开始到胜负已定的 tick 数
 * @param deathTick            when the player died, or -1 if they survived 死亡时刻，存活则为 -1
 * @param deathReason          the death reason on the player's own body, or null 自己尸体上的死因，没有则为 null
 * @param tasksCompleted       Wathe task-complete notices received 收到的 Wathe 任务完成通知数
 * @param gunDrops             times the player dropped their gun for shooting an innocent 误杀无辜而丢枪的次数
 * @param poisonOverlays       translation keys of poison overlays shown to the player, in order
 *                             依次显示给玩家的中毒画面翻译键
 */
public record RoundFacts(
        UUID self,
        List<String> roleChain,
        String factionId,
        int players,
        int fewestAliveWhileAlive,
        Set<String> traitIds,
        long durationTicks,
        long deathTick,
        String deathReason,
        int tasksCompleted,
        int gunDrops,
        List<String> poisonOverlays
) {
    public RoundFacts {
        roleChain = List.copyOf(roleChain);
        traitIds = Set.copyOf(traitIds);
        poisonOverlays = List.copyOf(poisonOverlays);
    }

    public String openingRoleId() {
        return roleChain.isEmpty() ? null : roleChain.getFirst();
    }

    public String finalRoleId() {
        return roleChain.isEmpty() ? null : roleChain.getLast();
    }

    public boolean died() {
        return deathTick >= 0;
    }

    /** Ticks the player stayed alive: until death, or the whole round. 存活 tick 数：到死亡为止，或整局。 */
    public long survivedTicks() {
        return died() ? deathTick : durationTicks;
    }
}
