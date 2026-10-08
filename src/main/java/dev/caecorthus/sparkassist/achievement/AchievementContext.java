package dev.caecorthus.sparkassist.achievement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What an achievement condition may look at: the round that just settled, how it ended, and the account's stats and
 * unlocks with this round already counted.
 * 成就条件可以查看的内容：刚结算的这一局、它的结局，以及已计入本局的账号统计与解锁记录。
 */
public record AchievementContext(RoundFacts facts, RoundOutcome outcome, LifetimeStats stats, AchievementLedger ledger) {
    public UUID self() {
        return facts.self();
    }

    public boolean won() {
        return outcome.won();
    }

    /** The role the player ended the round with. 玩家结束这一局时的角色。 */
    public String role() {
        return facts.finalRoleId();
    }

    public String faction() {
        return facts.factionId();
    }

    /** Deaths the player caused this round (needs the match record). 本局玩家造成的死亡（需要对局记录）。 */
    public List<MatchEvent> kills() {
        return outcome.killsBy(self());
    }

    /** How the player died this round (needs the match record). 本局玩家的死亡记录（需要对局记录）。 */
    public Optional<MatchEvent> death() {
        return outcome.deathOf(self());
    }

    /** Another participant's final role, from Wathe's round-end result. 其他参与者的最终角色，取自 Wathe 的结算结果。 */
    public Optional<String> roleOf(UUID player) {
        return outcome.participant(player).map(RoundOutcome.Participant::roleId);
    }
}
