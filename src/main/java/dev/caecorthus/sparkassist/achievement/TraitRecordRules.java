package dev.caecorthus.sparkassist.achievement;

import java.util.UUID;

/**
 * Achievements read from the record events SparkTraits writes for them: Last Escape cancelling a death, and the
 * coins the Showman and the Plunderer earn on a kill. Each event is written only while the trait is really active, so
 * holding the trait needs no separate check. Servers without that SparkTraits never write them, so every rule here
 * simply finds nothing.
 * 依据 SparkTraits 专为成就写下的记录事件判断的成就：绝处逢生取消一次死亡，以及作秀与掠夺者在击杀时获得的金币。每条事件只在词条
 * 确实生效时写入，因此不必另外检查是否持有词条。没有该版本 SparkTraits 的服务器不会写这些事件，因此这里的规则只是什么也找不到。
 */
final class TraitRecordRules {
    /**
     * actor = the survivor, target = the player whose attack would have killed them (absent when none or self-inflicted;
     * the poisoner for poison, the thrower for a grenade); {@code death_reason} = the cancelled reason.
     * actor 为幸存者，target 为本会杀死他的玩家（没有或自己造成时不写；毒杀为下毒者，手雷为投掷者）；{@code death_reason} 为被取消的死因。
     */
    static final String LAST_ESCAPE_TRIGGERED = "sparktraits:last_escape_triggered";
    /**
     * actor = the killer, target = the victim; {@code witnesses} (other living players within 8 blocks of the victim,
     * uncapped) and {@code reward} = min(witnesses, 10) × 8 coins, written only when the reward is above 0.
     * actor 为凶手，target 为死者；{@code witnesses}（死者 8 格内的其他存活玩家数，不封顶）与 {@code reward} = min(witnesses, 10) × 8
     * 金币，仅在奖励大于 0 时写入。
     */
    static final String SHOWMAN_REWARD = "sparktraits:showman_reward";
    /** actor = the killer, target = the victim; {@code stolen} coins (a third of the victim's). actor 为凶手；{@code stolen} 为夺取的金币。 */
    static final String PLUNDERER_STEAL = "sparktraits:plunderer_steal";

    static final String POISON = "wathe:poison";

    /** The Showman's most for one kill: 10 witnesses × 8 coins. 作秀单次击杀的上限：10 名目击者 × 8 金币。 */
    static final int FULL_SHOWMAN_REWARD = 80;
    static final int BIG_PLUNDER = 100;

    private TraitRecordRules() {
    }

    /**
     * 我一定会回来的: after my Last Escape cancelled a death ({@code sparktraits:last_escape_triggered}), I kill with my
     * own hand, later in the round, the player whose attack it was (its target). "With my own hand" leaves out a
     * poison death ({@link #POISON}), which is credited to the poisoner, and a Last Stand false death, after which they
     * come back.
     * 我的绝处逢生取消了一次死亡（{@code sparktraits:last_escape_triggered}）之后，我在本局稍后亲手击杀当时出手的那名玩家（其 target）。
     * “亲手”不含毒死（{@link #POISON}，记在下毒者名下）和背水一战的假死（之后对方会复活）。
     */
    static boolean killedWhoNearlyKilledMe(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        return context.myEvents(LAST_ESCAPE_TRIGGERED).stream().anyMatch(escape -> escape.target() != null
                && !escape.target().equals(self)
                && context.kills().stream().anyMatch(kill -> escape.target().equals(kill.target())
                        && !timeline.isFakeDeath(kill) && !Rules.reason(kill, POISON)
                        && StrengthRecordRules.comesAfter(timeline, kill, escape)));
    }

    /**
     * 公开处刑: with the Showman, one kill pays the full 80 coins ({@code sparktraits:showman_reward} {@code reward}
     * 80, i.e. 10 or more other living players within 8 blocks of the victim).
     * 持有作秀时，一次击杀拿满 80 金币（{@code sparktraits:showman_reward} 的 {@code reward} 为 80，即死者 8 格内有 10 名及以上其他存活玩家）。
     */
    static boolean fullShowmanReward(AchievementContext context) {
        return context.myEvents(SHOWMAN_REWARD).stream()
                .anyMatch(reward -> reward.intValue("reward", 0) >= FULL_SHOWMAN_REWARD);
    }

    /**
     * 留下买路财: with the Plunderer, one kill takes 100 coins or more ({@code sparktraits:plunderer_steal}
     * {@code stolen}; a third of the victim's coins, so they had 300 or more).
     * 持有掠夺者时，一次击杀夺取 100 金币及以上（{@code sparktraits:plunderer_steal} 的 {@code stolen}；为死者金币的三分之一，
     * 所以对方要有 300 及以上）。
     */
    static boolean bigPlunder(AchievementContext context) {
        return context.myEvents(PLUNDERER_STEAL).stream()
                .anyMatch(steal -> steal.intValue("stolen", 0) >= BIG_PLUNDER);
    }
}
