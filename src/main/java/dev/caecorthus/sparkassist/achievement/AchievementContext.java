package dev.caecorthus.sparkassist.achievement;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * What an achievement condition may look at: the round that just settled as the client saw it ({@link #facts}),
 * how it ended ({@link #outcome}), the server's match record ({@link #timeline}, empty when the server sent none),
 * and the account's stats and unlocks with this round already counted.
 * <p>
 * Two clocks: {@link RoundFacts} counts round ticks from the first live tick the client saw; the match record counts
 * record ticks from the server's match start. They agree to within network latency; compare them with
 * {@link RoundFacts#RECORD_TICK_TOLERANCE}.
 * <p>
 * 成就条件可以查看的内容：客户端看到的刚结算的这一局（{@link #facts}）、它的结局（{@link #outcome}）、服务端对局记录
 * （{@link #timeline}，服务器没发送时为空），以及已计入本局的账号统计与解锁记录。
 * 两种时钟：{@link RoundFacts} 的对局 tick 从客户端看到的第一个进行中 tick 算起；对局记录的记录 tick 从服务端开始对局算起。
 * 两者相差不超过网络延迟，比较时使用 {@link RoundFacts#RECORD_TICK_TOLERANCE}。
 */
public record AchievementContext(
        RoundFacts facts,
        RoundOutcome outcome,
        MatchTimeline timeline,
        LifetimeStats stats,
        AchievementLedger ledger
) {
    public AchievementContext {
        Objects.requireNonNull(facts, "facts");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(timeline, "timeline");
        Objects.requireNonNull(stats, "stats");
        Objects.requireNonNull(ledger, "ledger");
    }

    /**
     * A context whose timeline knows no base factions beyond the Wathe names in the record (for tests and tools).
     * 时间线只认识记录中 Wathe 阵营名的上下文（用于测试和工具）。
     */
    public AchievementContext(RoundFacts facts, RoundOutcome outcome, LifetimeStats stats, AchievementLedger ledger) {
        this(facts, outcome, MatchTimeline.of(outcome.events(), role -> null), stats, ledger);
    }

    public UUID self() {
        return facts.self();
    }

    public boolean won() {
        return outcome.won();
    }

    /** The role the player ended the round with, as the client saw it. 客户端看到的玩家结束这一局时的角色。 */
    public String role() {
        return facts.finalRoleId();
    }

    /** The player's effective faction at the end of the round, or null. 玩家这一局结束时的实际阵营，没有则为 null。 */
    public String faction() {
        return facts.factionId();
    }

    /** Whether the server's match record arrived for this round. 本局是否收到了服务端对局记录。 */
    public boolean hasMatchRecord() {
        return !timeline.isEmpty();
    }

    /**
     * Deaths the player caused this round, in order, without dying by their own hand (needs the match record).
     * 本局玩家造成的死亡，按时间顺序，不含死于自己之手（需要对局记录）。
     */
    public List<MatchEvent> kills() {
        return timeline.killsBy(self());
    }

    /** How the player died this round (needs the match record). 本局玩家的死亡记录（需要对局记录）。 */
    public Optional<MatchEvent> death() {
        return timeline.deathOf(self());
    }

    /**
     * A participant's <em>final</em> role, from Wathe's round-end result; for the role at the time of an event use
     * {@link #roleAt}.
     * 参与者的<em>最终</em>角色，取自 Wathe 的结算结果；要看某个事件发生时的身份请用 {@link #roleAt}。
     */
    public Optional<String> roleOf(UUID player) {
        return outcome.participant(player).map(RoundOutcome.Participant::roleId);
    }

    /** {@code player}'s role when {@code event} happened; see {@link MatchTimeline#roleAt}. 事件发生时 {@code player} 的身份。 */
    public String roleAt(UUID player, MatchEvent event) {
        return timeline.roleAt(player, event);
    }

    /** {@code player}'s faction when {@code event} happened; see {@link MatchTimeline#factionAt}. 事件发生时 {@code player} 的阵营。 */
    public String factionAt(UUID player, MatchEvent event) {
        return timeline.factionAt(player, event);
    }

    /** Record events of {@code type} the player did. 玩家执行的 {@code type} 记录事件。 */
    public List<MatchEvent> myEvents(String type) {
        return timeline.eventsBy(type, self());
    }

    /** Whether the player was in psycho mode at record tick {@code tick}. 玩家在记录 tick {@code tick} 时是否处于疯魔模式。 */
    public boolean inPsycho(int tick) {
        return timeline.inPsycho(self(), tick);
    }
}
