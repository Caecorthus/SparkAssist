package dev.caecorthus.sparkassist.achievement;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
 * @param explosiveNearby      round-tick spans, in order, in which a live explosive was within
 *                             {@link RoundObservation#EXPLOSIVE_RANGE_BLOCKS} of the player while they were alive
 *                             玩家存活期间、{@link RoundObservation#EXPLOSIVE_RANGE_BLOCKS} 格内有未爆爆炸物的对局 tick 时段，按顺序
 * @param settledAt            the local wall-clock time when the round settled, or null when unknown
 *                             这一局结算时的本机时间，未知时为 null
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
        Set<UUID> depressionAttackers,
        List<TickSpan> explosiveNearby,
        LocalDateTime settledAt
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
        explosiveNearby = explosiveNearby == null ? List.of() : List.copyOf(explosiveNearby);
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

    /**
     * Every owner-visible trait held at some moment within {@link #RECORD_TICK_TOLERANCE} of record tick
     * {@code recordTick}, e.g. at a kill or a meal from the match record.
     * 在记录 tick {@code recordTick} 前后 {@link #RECORD_TICK_TOLERANCE} 以内的某一刻持有过的所有自身可见词条，
     * 例如对局记录中某次击杀或进食的那一刻。
     */
    public Set<String> traitsAround(int recordTick) {
        long from = (long) recordTick - RECORD_TICK_TOLERANCE;
        long to = (long) recordTick + RECORD_TICK_TOLERANCE;
        Set<String> held = new TreeSet<>();
        for (String traitId : traits.everHeldIds()) {
            if (traits.heldBetween(traitId, from, to)) {
                held.add(traitId);
            }
        }
        return Set.copyOf(held);
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

    /**
     * Whether an explosive was near the player at some round tick from {@code fromTick} to {@code toTick}, both
     * included.
     * 从对局 tick {@code fromTick} 到 {@code toTick}（含两端）之间是否有某一刻爆炸物在玩家附近。
     */
    public boolean explosiveNearbyBetween(long fromTick, long toTick) {
        return explosiveNearby.stream().anyMatch(span -> span.fromTick() <= toTick && span.toTick() >= fromTick);
    }

    /**
     * Whether the round settled at a local time from {@code from} (included) to {@code to} (excluded); a span that
     * passes midnight, such as 23:00 to 01:00, works too. False when the time is unknown.
     * 这一局是否在本机时间 {@code from}（含）到 {@code to}（不含）之间结算；跨过午夜的时段（如 23:00 到 01:00）同样可用。
     * 时间未知时为 false。
     */
    public boolean settledBetween(LocalTime from, LocalTime to) {
        if (settledAt == null) {
            return false;
        }
        LocalTime time = settledAt.toLocalTime();
        return from.isAfter(to)
                ? !time.isBefore(from) || time.isBefore(to)
                : !time.isBefore(from) && time.isBefore(to);
    }

    /** An inclusive span of round ticks. 一段对局 tick（含两端）。 */
    public record TickSpan(long fromTick, long toTick) {
        public boolean contains(long tick) {
            return tick >= fromTick && tick <= toTick;
        }
    }
}
