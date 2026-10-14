package dev.caecorthus.sparkassist.achievement;

import java.util.List;
import java.util.UUID;

/**
 * SparkStrength items: drones, grenades, the Professor's truth serum, the Engineer's power restoration and the
 * Bomber's bed trap, from the records SparkStrength (and the Wathe / Noelle's Roles code it drives) writes when they
 * really take effect. "As role X" is my real role at that event, so a Coroner disguised as X never counts.
 * SparkStrength 道具：无人机、手雷、教授的吐真试剂、工程师的电力恢复系统与炸弹客的床铺陷阱，依据 SparkStrength
 * （及其驱动的 Wathe / Noelle's Roles 代码）在其真正生效时写下的记录。“以 X 身份”指该事件发生时我的真实身份，
 * 因此伪装成 X 的验尸官永远不算。
 */
final class GadgetRules {
    static final String PROFESSOR = "noellesroles:professor";
    static final String ENGINEER = "noellesroles:engineer";

    static final String DRONE_DESTROYED = "sparkstrength:drone_destroyed";
    static final String SERUM_FED = "sparkstrength:professor_serum_fed";
    static final String TRUTH_REVEALED = "sparkstrength:professor_truth_revealed";
    static final String TRUTH_SERUM = "sparkstrength:truth_serum";
    static final String POWER_RESTORATION_ENTRY = "sparkstrength_power_restoration";
    static final String BED_BOMB_EMBEDDED = "sparkstrength:timed_bomb_bed_embedded";
    static final String BED_BOMB_TRIGGERED = "sparkstrength:timed_bomb_bed_triggered";
    static final String TRAY_BOMB_TRIGGERED = "sparkstrength:timed_bomb_tray_triggered";
    static final String TIMED_BOMB = "noellesroles:timed_bomb";
    static final String BOMBER_SKILL = "noellesroles:bomber";
    /**
     * How long a Wathe grenade may fly before it hits something and goes off; it has no fuse.
     * Wathe 手雷从投出到撞上东西爆炸的最长飞行时间；它没有引信。
     */
    static final int GRENADE_FLIGHT_TICKS = 200;

    private GadgetRules() {
    }

    /**
     * 禁飞区: break a drone that is someone else's ({@code sparkstrength:drone_destroyed}: actor = me, target = its
     * owner, who is not me).
     * 打坏一架别人的无人机（{@code sparkstrength:drone_destroyed}：actor 为我，target 为其主人且不是我）。
     */
    static boolean brokeSomeoneElsesDrone(AchievementContext context) {
        return Rules.myGlobalEvents(context, DRONE_DESTROYED).stream().anyMatch(destroyed -> destroyed.target() != null
                && !destroyed.target().equals(context.self()));
    }

    /**
     * 我炸我自己: die by a grenade I threw: a {@code wathe:grenade} death credited to myself that follows my own throw,
     * either an M67 whose 5 s fuse ends at that death or a Wathe grenade (which goes off on impact) at most 10 s
     * before it. An M67 dropped by my drone is not a throw.
     * 被自己扔出的手雷炸死：一次记在我自己名下的 {@code wathe:grenade} 死亡，且之前有我本人的投掷——引信 5 秒正好在这次死亡时结束的
     * M67，或者在其之前不超过 10 秒的 Wathe 手雷（撞击即爆）。无人机投下的 M67 不算投掷。
     */
    static boolean killedByMyOwnGrenade(AchievementContext context) {
        UUID self = context.self();
        if (self == null) {
            return false;
        }
        List<MatchEvent> m67Throws = Rules.myItemUses(context, KillRules.M67);
        List<MatchEvent> grenadeThrows = Rules.myItemUses(context, KillRules.GRENADE);
        return context.timeline().eventsOn(MatchEvent.DEATH, self).stream().anyMatch(death -> self.equals(death.actor())
                && Rules.reason(death, KillRules.GRENADE)
                && (m67Throws.stream().anyMatch(thrown -> Math.abs(death.tick() - KillRules.GRENADE_FUSE_TICKS
                        - thrown.tick()) <= KillRules.GRENADE_FUSE_SLACK)
                    || grenadeThrows.stream().anyMatch(thrown -> thrown.tick() <= death.tick()
                        && death.tick() - thrown.tick() <= GRENADE_FLIGHT_TICKS)));
    }

    /**
     * 真相只有一个！: as the Professor, feed the truth serum to another player who is a 非好人 then, and their real
     * role is announced to everyone ({@code sparkstrength:professor_truth_revealed} for them in the same tick).
     * 以教授身份让另一名当时是非好人的玩家喝下吐真试剂，并向所有人公布其真实身份（同一 tick 内有其
     * {@code sparkstrength:professor_truth_revealed}）。
     */
    static boolean truthSerumOnOtherSide(AchievementContext context) {
        List<MatchEvent> reveals = context.timeline().events(MatchEvent.GLOBAL_EVENT).stream()
                .filter(event -> event.isGlobal(TRUTH_REVEALED))
                .toList();
        return Rules.myGlobalEvents(context, SERUM_FED).stream().anyMatch(fed -> fed.target() != null
                && !fed.target().equals(context.self())
                && TRUTH_SERUM.equals(fed.value("item"))
                && Rules.iWas(context, fed, PROFESSOR)
                && Factions.isNonCivilian(context.factionAt(fed.target(), fed))
                && reveals.stream().anyMatch(reveal -> reveal.tick() == fed.tick()
                        && fed.target().equals(reveal.actor())));
    }

    /**
     * 来电了: as the Engineer, buy the power restoration system. The purchase only goes through (and is only recorded)
     * while a blackout is on, and it ends that blackout.
     * 以工程师身份购买电力恢复系统。只有停电期间购买才会成功（才会被记录），并且会结束这次停电。
     */
    static boolean restoredPower(AchievementContext context) {
        return context.myEvents(MatchEvent.SHOP_PURCHASE).stream().anyMatch(purchase ->
                POWER_RESTORATION_ENTRY.equals(purchase.value("entry_id")) && Rules.iWas(context, purchase, ENGINEER));
    }

    /**
     * 晚安: as the Bomber, hide a timed bomb in a bed, and the player who lies down on it and picks it up dies by that
     * bomb: after a bed trigger naming me as the bomber (and after my own bed planting as the Bomber), that player's
     * next death is my {@code noellesroles:bomb} kill, they never passed the bomb on in between, and no bomb drone went
     * off in that tick.
     * 以炸弹客身份把定时炸弹藏进床里，并让躺上去带走炸弹的玩家被这颗炸弹炸死：在一次炸弹客为我的床铺触发之后（且在我以炸弹客身份
     * 埋床之后），此人的下一次死亡是我的 {@code noellesroles:bomb} 击杀，期间他没有把炸弹传出去，且该 tick 没有炸弹无人机爆炸。
     */
    static boolean bedBombKill(AchievementContext context) {
        UUID self = context.self();
        if (self == null) {
            return false;
        }
        List<MatchEvent> events = context.timeline().events();
        boolean planted = false;
        for (int i = 0; i < events.size(); i++) {
            MatchEvent event = events.get(i);
            if (event.isGlobal(BED_BOMB_EMBEDDED) && self.equals(event.actor())
                    && Rules.iWas(context, event, KillRules.BOMBER)) {
                planted = true;
            } else if (planted && event.isGlobal(BED_BOMB_TRIGGERED) && self.equals(event.uuid("bomber"))
                    && event.actor() != null && !self.equals(event.actor())
                    && bombKillsCarrier(events, i, event.actor(), self)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether {@code carrier}, who picked up a bomb at {@code from}, next dies by it as my kill without passing it on
     * or picking up another one first.
     * 在 {@code from} 处拿到炸弹的 {@code carrier} 是否在把它传出或再拿到另一颗之前，下一次死亡就是被它炸死、且记为我的击杀。
     */
    private static boolean bombKillsCarrier(List<MatchEvent> events, int from, UUID carrier, UUID self) {
        for (int i = from + 1; i < events.size(); i++) {
            MatchEvent event = events.get(i);
            if (event.is(MatchEvent.DEATH) && carrier.equals(event.target())) {
                return self.equals(event.actor()) && Rules.reason(event, KillRules.BOMB)
                        && events.stream().noneMatch(other -> other.tick() == event.tick()
                                && other.isGlobal(KillRules.BOMB_DRONE_DETONATED));
            }
            if (passedOn(event, carrier) || pickedUpAnother(event, carrier)) {
                return false;
            }
        }
        return false;
    }

    private static boolean passedOn(MatchEvent event, UUID carrier) {
        return event.is(MatchEvent.ITEM_USE) && TIMED_BOMB.equals(event.value("item")) && carrier.equals(event.actor());
    }

    private static boolean pickedUpAnother(MatchEvent event, UUID carrier) {
        boolean trap = event.isGlobal(BED_BOMB_TRIGGERED) || event.isGlobal(TRAY_BOMB_TRIGGERED);
        boolean placed = event.is(MatchEvent.SKILL_USE) && BOMBER_SKILL.equals(event.value("skill"));
        boolean handedOver = event.is(MatchEvent.ITEM_USE) && TIMED_BOMB.equals(event.value("item"));
        return trap && carrier.equals(event.actor()) || (placed || handedOver) && carrier.equals(event.target());
    }
}
