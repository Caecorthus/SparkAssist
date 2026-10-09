package dev.caecorthus.sparkassist.achievement;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Everything the client itself saw during one finished round. Times are round ticks: world ticks since the client
 * first saw the round live. The match record counts from the server's match start instead; the two agree to within
 * network latency plus the trait poll interval, so compare them with {@link #RECORD_TICK_TOLERANCE}.
 * 本地客户端在一局已结束的对局中亲眼看到的一切。时间为对局 tick：客户端第一次看到对局进行中之后的世界 tick 数。
 * 对局记录则从服务端开始对局时算起；两者相差不超过网络延迟加上词条轮询间隔，比较时使用 {@link #RECORD_TICK_TOLERANCE}。
 *
 * @param self                 the local player 本地玩家
 * @param roleChain            every role the player held, in order, without repeats in a row
 *                             玩家依次担任过的角色，相邻不重复
 * @param factionId            the faction of the last role, or null 最后一个角色的阵营，没有则为 null
 * @param players              the most players with a role seen at once 同时有角色的玩家数的最大值
 * @param fewestAliveWhileAlive the fewest living players seen while the local player was alive, themselves
 *                             included; 0 when never seen alive
 *                             本地玩家存活期间看到的最少存活人数（含自己）；从未存活时为 0
 * @param traits               owner-visible traits over the round, empty after death 本局自身可见词条的变化，死亡后为空
 * @param durationTicks        ticks from going live to the round being decided 从开始到胜负已定的 tick 数
 * @param deathTick            when the player died, or -1 if they survived 死亡时刻，存活则为 -1
 * @param deathReason          the death reason on the player's own body, or null 自己尸体上的死因，没有则为 null
 * @param tasksCompleted       Wathe task-complete notices received 收到的 Wathe 任务完成通知数
 * @param gunDrops             times the player dropped their gun for shooting an innocent 误杀无辜而丢枪的次数
 * @param poisonOverlays       translation keys of poison overlays shown to the player, in order
 *                             依次显示给玩家的中毒画面翻译键
 * @param depressionAttackers  players whose attack sent the local player into Depression psycho (SparkTraits,
 *                             owner-only data) 攻击本地玩家、使其进入抑郁疯魔的玩家（SparkTraits，仅本人可见的数据）
 */
public record RoundFacts(
        UUID self,
        List<String> roleChain,
        String factionId,
        int players,
        int fewestAliveWhileAlive,
        TraitTimeline traits,
        long durationTicks,
        long deathTick,
        String deathReason,
        int tasksCompleted,
        int gunDrops,
        List<String> poisonOverlays,
        Set<UUID> depressionAttackers
) {
    /**
     * How far apart a round tick and a record tick for the same moment may be: network latency plus the one-second
     * trait poll, with margin.
     * 同一时刻的对局 tick 与记录 tick 最多相差多少：网络延迟加上每秒一次的词条轮询，再留出余量。
     */
    public static final int RECORD_TICK_TOLERANCE = 40;

    public RoundFacts {
        roleChain = List.copyOf(roleChain);
        traits = traits == null ? TraitTimeline.empty() : traits;
        poisonOverlays = List.copyOf(poisonOverlays);
        depressionAttackers = depressionAttackers == null ? Set.of() : Set.copyOf(depressionAttackers);
    }

    /** Every owner-visible trait held while alive. 存活期间持有过的所有自身可见词条。 */
    public Set<String> traitIds() {
        return traits.everHeldIds();
    }

    /** Whether the player held this owner-visible trait at some point. 玩家是否在某一时刻持有过这个自身可见词条。 */
    public boolean hadTrait(String traitId) {
        return traits.everHeld(traitId);
    }

    /** The owner-visible traits held at round tick {@code tick}. 对局 tick {@code tick} 时持有的自身可见词条。 */
    public Set<String> traitsAt(long tick) {
        return traits.at(tick);
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
