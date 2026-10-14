package dev.caecorthus.sparkassist.achievement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Collects {@link RoundFacts} for one round from per-tick observations and one-off client notices. A round only
 * settles when Wathe's round-end result arrived while it was being tracked and the local player held a role:
 * {@code /stop}, a disconnect or joining as a spectator never count.
 * 根据每 tick 的观察和零散的客户端通知，为一局收集 {@link RoundFacts}。只有在跟踪期间收到了 Wathe 的结算结果、
 * 且本地玩家担任过角色时，这一局才会结算：{@code /stop}、断线或以旁观者身份加入都不算。
 */
public final class RoundTracker {
    // How long after dying the client keeps looking for the player's own body. 死后继续寻找自己尸体的时长。
    static final long DEATH_REASON_WINDOW_TICKS = 100;

    private final List<String> roleChain = new ArrayList<>();
    private final List<String> poisonOverlays = new ArrayList<>();
    private final Set<UUID> depressionAttackers = new LinkedHashSet<>();
    private final List<RoundFacts.TickSpan> explosiveNearby = new ArrayList<>();
    private TraitTimeline.Builder traits = new TraitTimeline.Builder();

    private boolean active;
    private boolean roundEndSeen;
    private UUID self;
    private long startWorldTime;
    private long lastLiveWorldTime;
    private boolean started;
    private String factionId;
    private int players;
    private int fewestAliveWhileAlive;
    private long deathTick;
    private long deathWorldTime;
    private String deathReason;
    private int tasksCompleted;
    private int gunDrops;
    // Whether the last observation while alive saw an explosive nearby. 存活时上一次观察是否看到附近有爆炸物。
    private boolean explosiveSeen;

    public RoundTracker() {
        reset();
    }

    public void start(UUID self) {
        reset();
        this.active = true;
        this.self = self;
    }

    public boolean active() {
        return active;
    }

    public void observe(RoundObservation observation) {
        if (!active) {
            return;
        }
        long now = observation.worldTime();
        if (!started) {
            started = true;
            startWorldTime = now;
            lastLiveWorldTime = now;
        }
        if (!observation.stopping()) {
            lastLiveWorldTime = Math.max(lastLiveWorldTime, now);
        }

        String roleId = observation.roleId();
        if (roleId != null && (roleChain.isEmpty() || !roleChain.getLast().equals(roleId))) {
            roleChain.add(roleId);
        }
        if (observation.factionId() != null) {
            factionId = observation.factionId();
        }
        players = Math.max(players, observation.totalPlayers());

        if (observation.selfDead()) {
            explosiveSeen = false;
            if (deathTick < 0 && !roleChain.isEmpty()) {
                deathTick = Math.max(0, now - startWorldTime);
                deathWorldTime = now;
                // SparkTraits clears traits on a real death. SparkTraits 在真正死亡时清空词条。
                traits.observe(deathTick, Set.of());
            }
        } else if (roleId != null) {
            int alive = observation.alivePlayers();
            fewestAliveWhileAlive = fewestAliveWhileAlive == 0 ? alive : Math.min(fewestAliveWhileAlive, alive);
            long tick = Math.max(0, now - startWorldTime);
            traits.observe(tick, observation.traitIds());
            observeExplosive(tick, observation.explosiveNearby());
        } else {
            explosiveSeen = false;
        }
    }

    /**
     * Grows the current explosive span while observations in a row see one, or starts a new span.
     * 连续的观察都看到爆炸物时延长当前时段，否则开始新的时段。
     */
    private void observeExplosive(long tick, boolean nearby) {
        if (nearby) {
            if (explosiveSeen && !explosiveNearby.isEmpty()) {
                RoundFacts.TickSpan last = explosiveNearby.removeLast();
                explosiveNearby.add(new RoundFacts.TickSpan(last.fromTick(), Math.max(last.toTick(), tick)));
            } else {
                explosiveNearby.add(new RoundFacts.TickSpan(tick, tick));
            }
        }
        explosiveSeen = nearby;
    }

    /**
     * Whether the client should still look for the player's own body to read the death reason.
     * 客户端是否还应寻找自己的尸体来读取死因。
     */
    public boolean wantsDeathReason(long worldTime) {
        return active && deathTick >= 0 && deathReason == null
                && worldTime - deathWorldTime <= DEATH_REASON_WINDOW_TICKS;
    }

    public void onDeathReason(String reason) {
        if (active && deathTick >= 0 && deathReason == null) {
            deathReason = reason;
        }
    }

    public void onTaskComplete() {
        if (active) {
            tasksCompleted++;
        }
    }

    public void onGunDrop() {
        if (active) {
            gunDrops++;
        }
    }

    public void onPoisonOverlay(String translationKey) {
        if (active && translationKey != null) {
            poisonOverlays.add(translationKey);
        }
    }

    /**
     * The local player is in Depression psycho because {@code attacker} attacked them (SparkTraits' owner-only state).
     * 本地玩家因 {@code attacker} 的攻击进入抑郁疯魔（SparkTraits 仅本人可见的状态）。
     */
    public void onDepressionAttacker(UUID attacker) {
        if (active && attacker != null) {
            depressionAttackers.add(attacker);
        }
    }

    /** Wathe's round-end result reached the client. Wathe 的结算结果到达客户端。 */
    public void onRoundEndSynced() {
        if (active) {
            roundEndSeen = true;
        }
    }

    /**
     * Ends tracking at the local wall-clock time {@code settledAt} (null when unknown). Returns the facts when the
     * round counts, otherwise empty.
     * 在本机时间 {@code settledAt}（未知时为 null）结束跟踪。这一局算数时返回收集到的事实，否则返回空。
     */
    public Optional<RoundFacts> finish(LocalDateTime settledAt) {
        Optional<RoundFacts> facts = active && roundEndSeen && !roleChain.isEmpty()
                ? Optional.of(new RoundFacts(
                        self,
                        roleChain,
                        factionId,
                        players,
                        fewestAliveWhileAlive,
                        traits.build(),
                        lastLiveWorldTime - startWorldTime,
                        deathTick,
                        deathReason,
                        tasksCompleted,
                        gunDrops,
                        poisonOverlays,
                        depressionAttackers,
                        explosiveNearby,
                        settledAt))
                : Optional.empty();
        reset();
        return facts;
    }

    /** Drops the round without settling it. 放弃这一局，不结算。 */
    public void abort() {
        reset();
    }

    private void reset() {
        roleChain.clear();
        poisonOverlays.clear();
        depressionAttackers.clear();
        explosiveNearby.clear();
        traits = new TraitTimeline.Builder();
        active = false;
        roundEndSeen = false;
        self = null;
        started = false;
        startWorldTime = 0;
        lastLiveWorldTime = 0;
        factionId = null;
        players = 0;
        fewestAliveWhileAlive = 0;
        deathTick = -1;
        deathWorldTime = 0;
        deathReason = null;
        tasksCompleted = 0;
        gunDrops = 0;
        explosiveSeen = false;
    }
}
