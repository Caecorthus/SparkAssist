package dev.caecorthus.sparkassist.achievement;

import dev.caecorthus.sparkassist.achievement.MatchTimeline.TickWindow;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The Jester and the Shadow Jester (Noelle's Roles). Both only ever win a {@code NEUTRAL} win with an explicit winner
 * list: the Jester alone as the last one standing in its Jester Moment, the allied Shadow Jesters together in their
 * 双影谢幕 showdown. The Jester Moment is a psycho mode bracketed by {@code noellesroles:jester_moment_start} (actor =
 * the Jester, {@code trigger} = the innocent whose gunshot started it) and {@code jester_moment_end} or the Jester's
 * death.
 * 小丑与影子小丑（Noelle's Roles）。两者都只会以带明确赢家名单的 {@code NEUTRAL} 胜利获胜：小丑在小丑时刻中成为唯一存活者，
 * 结盟的影子小丑在“双影谢幕”终局对决中共同获胜。小丑时刻是一段疯魔，以 {@code noellesroles:jester_moment_start}
 * （actor 为小丑，{@code trigger} 为开枪触发它的好人）开始，以 {@code jester_moment_end} 或小丑死亡结束。
 */
final class JesterRules {
    static final String JESTER = "noellesroles:jester";
    static final String SHADOW_JESTER = "noellesroles:shadow_jester";
    /** Wathe's win status for a neutral win. 中立获胜时 Wathe 的胜利状态。 */
    static final String NEUTRAL_WIN = "NEUTRAL";
    static final String MOMENT_START = "noellesroles:jester_moment_start";
    static final String MOMENT_END = "noellesroles:jester_moment_end";
    static final String SHADOW_BOND = "noellesroles:shadow_bond";
    static final String POISON = "wathe:poison";
    static final int MOMENT_KILLS = 5;

    private JesterRules() {
    }

    /**
     * 小丑盛宴: I won a {@code NEUTRAL} win and ended the round as the Jester (Wathe's round-end role, else the client's).
     * A Shadow Jester who became the Jester counts too.
     * 我以 {@code NEUTRAL} 胜利获胜，且以小丑身份结束本局（取 Wathe 结算中的身份，没有时取客户端所见）。化身小丑的影子小丑同样算。
     */
    static boolean wonAsJester(AchievementContext context) {
        return wonNeutralAs(context, JESTER);
    }

    /**
     * 五十度灰: as the Jester, kill 5 different players within one Jester Moment (from my {@code jester_moment_start}
     * to my {@code jester_moment_end} or my death). SparkStrength greys the screen 10% per such kill, capped at 50%.
     * 以小丑身份在一次小丑时刻内（从我的 {@code jester_moment_start} 到我的 {@code jester_moment_end} 或我死亡）击杀 5 名不同的玩家。
     * SparkStrength 每杀一人让画面灰 10%，最多 50%。
     */
    static boolean fiveKillsInOneMoment(AchievementContext context) {
        List<MatchEvent> kills = Rules.myKills(context, kill -> Rules.killedAs(context, kill, JESTER));
        return myMoments(context).stream().anyMatch(moment -> kills.stream()
                .filter(kill -> moment.contains(kill.tick()))
                .map(MatchEvent::target).filter(Objects::nonNull).distinct().count() >= MOMENT_KILLS);
    }

    /**
     * 解铃还须系铃人: a Jester's Moment names me as its {@code trigger} (my gunshot as an innocent started it), and later I
     * kill that Jester by my own hand (a death I caused, not by poison).
     * 某位小丑的小丑时刻以我为 {@code trigger}（我以好人身份开枪触发了它），之后我亲手击杀了这名小丑（由我造成的死亡，毒杀不算）。
     */
    static boolean killedTheJesterIShot(AchievementContext context) {
        UUID self = context.self();
        List<MatchEvent> events = context.timeline().events();
        for (int i = 0; i < events.size() && self != null; i++) {
            MatchEvent start = events.get(i);
            UUID jester = start.actor();
            if (!start.isGlobal(MOMENT_START) || !self.equals(start.uuid("trigger")) || jester == null
                    || jester.equals(self)) {
                continue;
            }
            for (int j = i + 1; j < events.size(); j++) {
                MatchEvent death = events.get(j);
                if (death.is(MatchEvent.DEATH) && self.equals(death.actor()) && jester.equals(death.target())
                        && !Rules.reason(death, POISON)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 双影谢幕: I won a {@code NEUTRAL} win and ended the round as a Shadow Jester. The only way a Shadow Jester is a
     * neutral winner is the allied pair's showdown (Noelle's Roles' or SparkStrength's), whose two winners are both
     * listed.
     * 我以 {@code NEUTRAL} 胜利获胜，且以影子小丑身份结束本局。影子小丑成为中立赢家的唯一途径就是结盟二人的终局对决
     * （Noelle's Roles 或 SparkStrength 的版本），两人都列为赢家。
     */
    static boolean wonShadowShowdown(AchievementContext context) {
        return wonNeutralAs(context, SHADOW_JESTER);
    }

    /**
     * 同年同月同日死: a death of mine by the shadow oath ({@code noellesroles:shadow_bond}), from the record or the reason
     * on my body. Noelle's Roles only gives this reason to the allied Shadow Jester whose partner just died.
     * 我因影誓而死（{@code noellesroles:shadow_bond}），取自记录或我尸体上的死因。Noelle's Roles 只会把这个死因给搭档刚刚死去的
     * 结盟影子小丑。
     */
    static boolean diedByShadowBond(AchievementContext context) {
        return SHADOW_BOND.equals(context.facts().deathReason())
                || context.timeline().eventsOn(MatchEvent.DEATH, context.self()).stream()
                        .anyMatch(death -> Rules.reason(death, SHADOW_BOND));
    }

    /**
     * 影帝: a Shadow Jester who became the Jester (the only such change: the unallied partner died) and then won as the
     * Jester ({@link #wonAsJester}). The change is read from the roles the client saw, else from the record.
     * 影子小丑化身为小丑（唯一的途径：未结盟的搭档死亡），之后以小丑身份获胜（{@link #wonAsJester}）。身份变化取自客户端所见，
     * 没有时取自记录。
     */
    static boolean wonAsJesterAfterShadowTransform(AchievementContext context) {
        return wonAsJester(context) && (shadowThenJester(context.facts().roleChain())
                || shadowThenJester(context.timeline().roleHistory(context.self())));
    }

    private static boolean wonNeutralAs(AchievementContext context, String roleId) {
        return context.won() && NEUTRAL_WIN.equals(context.outcome().winStatus())
                && roleId.equals(context.roleOf(context.self()).orElse(context.role()));
    }

    private static boolean shadowThenJester(List<String> roles) {
        int shadow = roles.indexOf(SHADOW_JESTER);
        return shadow >= 0 && roles.subList(shadow + 1, roles.size()).contains(JESTER);
    }

    /**
     * My Jester Moments: from each {@code jester_moment_start} of mine to my next {@code jester_moment_end} or death; a
     * moment that never ended closes at the end of the record.
     * 我的小丑时刻：从我的每个 {@code jester_moment_start} 到我下一个 {@code jester_moment_end} 或死亡；没有结束的时刻在记录末尾关闭。
     */
    private static List<TickWindow> myMoments(AchievementContext context) {
        UUID self = context.self();
        List<TickWindow> moments = new ArrayList<>();
        Integer since = null;
        for (MatchEvent event : context.timeline().events()) {
            if (self == null) {
                return List.of();
            }
            if (event.isGlobal(MOMENT_START) && self.equals(event.actor())) {
                since = since == null ? event.tick() : since;
            } else if (since != null && (event.isGlobal(MOMENT_END) && self.equals(event.actor())
                    || event.is(MatchEvent.DEATH) && self.equals(event.target()))) {
                moments.add(new TickWindow(since, Math.max(since, event.tick())));
                since = null;
            }
        }
        if (since != null) {
            moments.add(new TickWindow(since, Math.max(since, context.timeline().endTick())));
        }
        return List.copyOf(moments);
    }
}
