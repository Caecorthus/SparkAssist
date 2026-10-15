package dev.caecorthus.sparkassist.achievement;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Neutral wins: the Pathogen (Noelle's Roles, with SparkStrength's T-Virus and Virus) and Wathe's Loose Ends mode,
 * where the last Loose End standing wins alone.
 * 中立胜利：病原体（Noelle's Roles，加上 SparkStrength 的 T 病毒与病毒），以及 Wathe 的亡命徒模式（最后存活的亡命徒独自获胜）。
 */
final class NeutralRules {
    static final String PATHOGEN = "noellesroles:pathogen";
    /** {@code actor} (a Pathogen) made {@code target} a virus carrier. 病原体（{@code actor}）把 {@code target} 变成带毒者。 */
    static final String VIRUS_USED = "sparkstrength:pathogen_virus_used";
    /** The carrier ({@code actor}) infected {@code target}. 带毒者（{@code actor}）感染了 {@code target}。 */
    static final String VIRUS_SPREAD = "sparkstrength:pathogen_virus_spread";
    /**
     * Wathe's win status when one Loose End is left; only the Loose Ends mode sets it.
     * 只剩一名亡命徒时 Wathe 的胜利状态；只有亡命徒模式会设置它。
     */
    static final String LOOSE_END = "LOOSE_END";
    static final int CARRIER_INFECTIONS = 3;
    static final int LOOSE_ENDS_PLAYERS = 6;

    private NeutralRules() {
    }

    // ---- Pathogen 病原体 ----

    /**
     * 生化危机: as the Pathogen, revive a player with the T-Virus, and win the round.
     * 以病原体身份用 T 病毒复活一名玩家，并赢得对局。
     */
    static boolean revivedAndWon(AchievementContext context) {
        return context.won() && Rules.myGlobalEvents(context, MatchEvent.PATHOGEN_REVIVED).stream()
                .anyMatch(revive -> Rules.iWas(context, revive, PATHOGEN));
    }

    /**
     * 一传十，十传百: one carrier made by my Virus as the Pathogen infects 3 different players. A spread belongs to the
     * latest Virus use on its carrier (a carrier cannot be given the Virus again while carrying it).
     * 我以病原体身份用病毒制造的一名带毒者感染了 3 名不同的玩家。每次传染归于其带毒者身上最近一次的病毒使用
     * （带毒期间不能再被使用病毒）。
     */
    static boolean carrierInfectedThree(AchievementContext context) {
        UUID self = context.self();
        Map<UUID, MatchEvent> latestVirus = new HashMap<>();
        Map<UUID, Set<UUID>> infected = new HashMap<>();
        for (MatchEvent event : context.timeline().events()) {
            if (event.isGlobal(VIRUS_USED) && event.target() != null) {
                latestVirus.put(event.target(), event);
            } else if (event.isGlobal(VIRUS_SPREAD) && event.actor() != null && event.target() != null) {
                MatchEvent virus = latestVirus.get(event.actor());
                if (virus != null && self != null && self.equals(virus.actor())
                        && Rules.iWas(context, virus, PATHOGEN)) {
                    infected.computeIfAbsent(event.actor(), carrier -> new LinkedHashSet<>()).add(event.target());
                }
            }
        }
        return infected.values().stream().anyMatch(players -> players.size() >= CARRIER_INFECTIONS);
    }

    // ---- Loose Ends 亡命徒模式 ----

    /**
     * 大吉大利，今晚吃鸡: win a Loose Ends round of 6+ players as the last Loose End, by win status {@code LOOSE_END} (not
     * by role: SparkTraits' Last Stand Final Moment also makes Loose Ends, but ends as a passenger win).
     * 在 6 人及以上的亡命徒模式中，以胜利状态 {@code LOOSE_END} 成为最后的亡命徒（不按身份判断：SparkTraits 背水一战的终局时刻
     * 也会产生亡命徒，但以乘客胜利结束）。
     */
    static boolean lastLooseEnd(AchievementContext context) {
        return LOOSE_END.equals(context.outcome().winStatus()) && context.won()
                && matchPlayers(context) >= LOOSE_ENDS_PLAYERS;
    }

    /**
     * 渔翁得利: {@link #lastLooseEnd} having killed exactly one player; needs the complete record.
     * 在 {@link #lastLooseEnd} 的基础上恰好只击杀了一名玩家；需要完整记录。
     */
    static boolean lastLooseEndWithOneKill(AchievementContext context) {
        return lastLooseEnd(context) && context.timeline().isComplete()
                && context.kills().stream().map(MatchEvent::target).distinct().count() == 1;
    }

    /**
     * How many players the round had: the most with a role the client saw at once, or the record's opening snapshot
     * when that is larger.
     * 本局的人数：客户端同时看到有身份的最多人数；对局记录的开局快照更多时取后者。
     */
    private static int matchPlayers(AchievementContext context) {
        return Math.max(context.facts().players(), context.timeline().startingPlayers().size());
    }
}
