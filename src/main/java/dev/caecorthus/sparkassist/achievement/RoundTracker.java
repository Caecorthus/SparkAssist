package dev.caecorthus.sparkassist.achievement;

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
    private final Set<String> traitIds = new LinkedHashSet<>();
    private final List<String> poisonOverlays = new ArrayList<>();

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
            if (deathTick < 0 && !roleChain.isEmpty()) {
                deathTick = Math.max(0, now - startWorldTime);
                deathWorldTime = now;
            }
        } else if (roleId != null) {
            int alive = observation.alivePlayers();
            fewestAliveWhileAlive = fewestAliveWhileAlive == 0 ? alive : Math.min(fewestAliveWhileAlive, alive);
            traitIds.addAll(observation.traitIds());
        }
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

    /** Wathe's round-end result reached the client. Wathe 的结算结果到达客户端。 */
    public void onRoundEndSynced() {
        if (active) {
            roundEndSeen = true;
        }
    }

    /**
     * Ends tracking. Returns the facts when the round counts, otherwise empty.
     * 结束跟踪。这一局算数时返回收集到的事实，否则返回空。
     */
    public Optional<RoundFacts> finish() {
        Optional<RoundFacts> facts = active && roundEndSeen && !roleChain.isEmpty()
                ? Optional.of(new RoundFacts(
                        self,
                        roleChain,
                        factionId,
                        players,
                        fewestAliveWhileAlive,
                        traitIds,
                        lastLiveWorldTime - startWorldTime,
                        deathTick,
                        deathReason,
                        tasksCompleted,
                        gunDrops,
                        poisonOverlays))
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
        traitIds.clear();
        poisonOverlays.clear();
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
    }
}
