package dev.caecorthus.sparkassist.achievement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * How a round ended, read when the client sees it become inactive: Wathe's round-end result for every participant,
 * the winning faction, and the server's match record when SparkFactionAPI sent one (otherwise {@code events} is
 * empty).
 * 一局如何结束，在客户端看到对局变为未进行时读取：Wathe 给每名参与者的结算结果、获胜阵营，
 * 以及 SparkFactionAPI 发来的服务端对局记录（没有时 {@code events} 为空）。
 *
 * @param winStatus        Wathe {@code WinStatus} name, e.g. {@code KILLERS} Wathe 的 {@code WinStatus} 名称
 * @param won              whether the local player won 本地玩家是否获胜
 * @param winningFactionId the winning faction id, or null (no winner, or Loose Ends) 获胜阵营 id，没有则为 null
 */
public record RoundOutcome(
        String winStatus,
        boolean won,
        String winningFactionId,
        List<Participant> participants,
        List<MatchEvent> events
) {
    public RoundOutcome {
        participants = List.copyOf(participants);
        events = List.copyOf(events);
    }

    /**
     * The faction a plain Wathe win status stands for; custom (SparkFactionAPI) wins override it.
     * 普通 Wathe 胜利状态对应的阵营；自定义（SparkFactionAPI）胜利会覆盖它。
     */
    public static String defaultWinningFaction(String winStatus) {
        if (winStatus == null) {
            return null;
        }
        return switch (winStatus) {
            case "KILLERS" -> "wathe:killer";
            case "PASSENGERS", "TIME" -> "wathe:civilian";
            case "NEUTRAL" -> "wathe:neutral";
            default -> null;
        };
    }

    public Optional<Participant> participant(UUID uuid) {
        return participants.stream().filter(participant -> participant.uuid().equals(uuid)).findFirst();
    }

    /** Whether the server's match record arrived for this round. 本局是否收到了服务端对局记录。 */
    public boolean hasMatchRecord() {
        return !events.isEmpty();
    }

    public List<MatchEvent> events(String type) {
        return events.stream().filter(event -> event.is(type)).toList();
    }

    /** Deaths {@code killer} caused, in order. {@code killer} 造成的死亡，按时间顺序。 */
    public List<MatchEvent> killsBy(UUID killer) {
        return events.stream().filter(event -> event.is(MatchEvent.DEATH) && killer.equals(event.actor())).toList();
    }

    /** The death of {@code victim}, if recorded. {@code victim} 的死亡记录（若有）。 */
    public Optional<MatchEvent> deathOf(UUID victim) {
        return events.stream().filter(event -> event.is(MatchEvent.DEATH) && victim.equals(event.target())).findFirst();
    }

    /**
     * One participant's result. {@code endStatus} is Wathe's {@code PlayerEndStatus} name
     * ({@code ALIVE}, {@code DEAD}, {@code LEFT}, {@code LEFT_DEAD}).
     * 单名参与者的结算结果。{@code endStatus} 为 Wathe 的 {@code PlayerEndStatus} 名称。
     */
    public record Participant(UUID uuid, String name, String roleId, String endStatus, boolean winner) {
    }
}
