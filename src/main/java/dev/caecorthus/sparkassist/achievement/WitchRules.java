package dev.caecorthus.sparkassist.achievement;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * The SparkWitch witch faction (Grand Witch, Accomplices and the Bewitched), the Murderous Witch and the Fiend. The
 * witch faction is {@link Factions#WITCH}; the Murderous Witch has a faction of her own
 * ({@link Factions#MURDEROUS_WITCH}) and is not in it. Accomplices are never drawn at round start: every one is a
 * Bewitched promoted after two tasks (SparkWitch {@code BewitchedPromotionService}).
 * SparkWitch 的魔女阵营（大魔女、共犯与魔化使）、杀意魔女与魔人。魔女阵营为 {@link Factions#WITCH}；杀意魔女有自己的阵营
 * （{@link Factions#MURDEROUS_WITCH}），不属于魔女阵营。共犯从不在开局抽取：每名共犯都是完成两个任务后晋升的魔化使
 * （SparkWitch {@code BewitchedPromotionService}）。
 */
final class WitchRules {
    static final String GRAND_WITCH = RoleGroups.GRAND_WITCH_ROLE;
    static final String MURDEROUS_WITCH = "sparkwitch:murderous_witch";
    static final String BEWITCHED = "sparkwitch:bewitched";
    static final String ACCOMPLICE = "sparkwitch:accomplice";
    static final String ABYSS_LISTENER = "sparkwitch:abyss_listener";
    static final String RIFTWALKER = "sparkwitch:riftwalker";
    static final String POTION_GUNNER = "sparkwitch:potion_gunner";
    static final String FIEND = "sparkwitch:fiend";
    /** The plain Accomplice and its three special variants. 普通共犯及其三种特殊变体。 */
    static final Set<String> ACCOMPLICES = Set.of(ACCOMPLICE, ABYSS_LISTENER, RIFTWALKER, POTION_GUNNER);
    /** The special Accomplices a Bewitched promotion rolls. 魔化使晋升抽取的特殊共犯。 */
    static final List<String> SPECIAL_ACCOMPLICES = List.of(ABYSS_LISTENER, RIFTWALKER, POTION_GUNNER);
    /** The Bewitched and every role its promotion gives. 魔化使及其晋升可得的所有身份。 */
    static final Set<String> BEWITCHED_LINE = Set.of(BEWITCHED, ACCOMPLICE, ABYSS_LISTENER, RIFTWALKER, POTION_GUNNER);

    static final String PIERCED_BY_RAY = "sparkwitch:pierced_by_ray";
    static final String FELL_OUT_OF_TRAIN = "wathe:fell_out_of_train";
    static final String SHRIEK_GUN = "sparkwitch:shriek_gun";
    static final String FIRE_POKER = "sparkwitch:fire_poker";
    static final String WARDENS_SHRIEK = "sparkwitch:wardens_shriek";
    static final String BEWITCHED_PROMOTION = "sparkwitch:bewitched_promotion";
    static final String BLINDNESS_ENTRY = "sparkwitch_blindness";
    static final String FEAR_ENTRY = "sparkwitch_fear";
    static final String WITCHES_SABBATH = "sparkwitch:witches_sabbath";
    static final String PULLED = "pulled";
    static final String PORTAL_CRUSH = "sparkwitch:portal_crush";
    static final String PORTAL_CRUSHED = "sparkwitch:portal_crushed";
    static final String FIEND_MOMENT_START = "sparkwitch:fiend_moment_start";
    static final String FIEND_MOMENT_END = "sparkwitch:fiend_moment_end";
    static final String FIEND_KEY = "fiend";
    static final String DURATION_TICKS = "duration_ticks";
    static final String KNIFE_STAB = "wathe:knife_stab";
    static final String GUN_SHOT = "wathe:gun_shot";
    static final String REVOLVER = "wathe:revolver";
    static final String KILLER_ITEM = "killer_item";

    /**
     * How long a push keeps the train-fall kill credit, 10 s (SparkWitch
     * {@code FirePokerRules.FALL_ATTRIBUTION_WINDOW_TICKS}).
     * 推人保留坠车击杀归属的时长（SparkWitch {@code FirePokerRules.FALL_ATTRIBUTION_WINDOW_TICKS}，10 秒）。
     */
    static final int FALL_CREDIT_TICKS = 200;
    /** The Grand Witch's Blindness lasts 20 s ({@code GrandWitchSpell.BLINDNESS}). 大魔女的失明持续 20 秒。 */
    static final int BLINDNESS_TICKS = 400;
    /** The Grand Witch's Fear lasts 10 s ({@code GrandWitchSpell.FEAR}). 大魔女的恐惧持续 10 秒。 */
    static final int FEAR_TICKS = 200;
    /** 15 seconds. 15 秒。 */
    static final int SABBATH_FOLLOW_UP_TICKS = 300;
    /** The Fiend Moment, 120 s, for a start without {@code duration_ticks} ({@code FiendRules}). 魔人时刻 120 秒。 */
    static final int FIEND_MOMENT_TICKS = 2400;
    static final int SHRIEK_TARGETS = 4;
    static final int BEATEN_RICH_COINS = 300;
    static final int HAT_TRICK_WINS = 3;

    private WitchRules() {
    }

    // ---- Murderous Witch 杀意魔女 ----

    /**
     * 糖葫芦: as the Murderous Witch, 3 kills by Death Ray ({@code sparkwitch:pierced_by_ray}) in one record tick, like
     * 反载 (one beam kills everyone on it in the same tick).
     * 以杀意魔女身份，同一记录 tick 内 3 次死亡射线击杀，写法同反载（一道射线在同一 tick 击杀其上所有人）。
     */
    static boolean deathRayTriple(AchievementContext context) {
        return Rules.mostAtOneTick(Rules.myKills(context, kill -> Rules.reason(kill, PIERCED_BY_RAY)
                && Rules.killedAs(context, kill, MURDEROUS_WITCH))) >= 3;
    }

    // ---- Abyss Listener 聆渊者 ----

    /**
     * 下一站：深渊: as the Abyss Listener, a train-fall kill ({@code wathe:fell_out_of_train}, credited to me) of a
     * player outside the witch faction, whose push was my Shriek Gun: my latest push of that player (Shriek Gun or Fire
     * Poker {@code item_use} on them) before the fall is a Shriek Gun shot within the 10 s credit window.
     * 以聆渊者身份，记在我名下的坠车击杀（{@code wathe:fell_out_of_train}），死者不在魔女阵营，且推下去的是我的啸音铳：
     * 坠车前我对此人的最近一次推击（对其使用的啸音铳或烧火棍 {@code item_use}）是 10 秒归属窗口内的一发啸音铳。
     */
    static boolean shriekGunFall(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> Rules.reason(kill, FELL_OUT_OF_TRAIN)
                && Rules.killedAs(context, kill, ABYSS_LISTENER)
                && outsideWitchFaction(context.timeline().victimFaction(kill))
                && pushedByShriekGun(context, kill));
    }

    private static boolean pushedByShriekGun(AchievementContext context, MatchEvent fall) {
        if (fall.target() == null) {
            return false;
        }
        MatchEvent latest = null;
        for (MatchEvent use : context.myEvents(MatchEvent.ITEM_USE)) {
            if (use.tick() > fall.tick()) {
                break;
            }
            boolean push = SHRIEK_GUN.equals(use.value("item")) || FIRE_POKER.equals(use.value("item"));
            if (push && fall.target().equals(use.target())) {
                latest = use;
            }
        }
        return latest != null && SHRIEK_GUN.equals(latest.value("item"))
                && fall.tick() - latest.tick() <= FALL_CREDIT_TICKS;
    }

    /**
     * 全体肃静: as the Abyss Listener, one Warden's Shriek that affected 4 players ({@code targets}, the non-allies it
     * hit).
     * 以聆渊者身份，一次监守之啸影响 4 名玩家（{@code targets}，被命中的非队友人数）。
     */
    static boolean shriekFour(AchievementContext context) {
        return Rules.mySkillUses(context, WARDENS_SHRIEK).stream().anyMatch(shriek ->
                shriek.intValue("targets", 0) >= SHRIEK_TARGETS && Rules.iWas(context, shriek, ABYSS_LISTENER));
    }

    // ---- The Bewitched 魔化使 ----

    /**
     * 图穷匕见: a promotion of mine out of the Bewitched: a {@code role_changed} with cause
     * {@code sparkwitch:bewitched_promotion}, which SparkWitch writes once the second task is done.
     * 我作为魔化使的一次晋升：原因为 {@code sparkwitch:bewitched_promotion} 的 {@code role_changed}，SparkWitch 在完成第二个任务后写下。
     */
    static boolean bewitchedPromoted(AchievementContext context) {
        return myRoleChanges(context).stream().anyMatch(change ->
                BEWITCHED_PROMOTION.equals(change.value("cause")) && Rules.iWas(context, change, BEWITCHED));
    }

    /**
     * 我全都要: across rounds, been promoted out of the Bewitched into each of the Abyss Listener, the Riftwalker and
     * the Potion Gunner (the promotion cause recorded for each, {@link LifetimeStats#roleTakenCauses}).
     * 跨局累计，分别由魔化使晋升为聆渊者、隙行者和药炮手（每个身份都记录有晋升原因，{@link LifetimeStats#roleTakenCauses}）。
     */
    static boolean promotedToEverySpecialAccomplice(AchievementContext context) {
        return SPECIAL_ACCOMPLICES.stream().allMatch(role ->
                context.stats().roleTakenCauses(role).contains(BEWITCHED_PROMOTION));
    }

    // ---- Accomplices 共犯 ----

    /**
     * 浑水摸鱼: as an Accomplice (plain or a special variant), a kill of a player outside the witch faction while a
     * Blindness the Grand Witch cast was in effect: within 20 s of a {@code sparkwitch_blindness} purchase by a player
     * who was the Grand Witch then (the purchase is the cast; it is recorded only when the spell went off).
     * 以共犯身份（普通或特殊变体），在大魔女施放的失明生效期间击杀魔女阵营以外的玩家：在当时身为大魔女的玩家购买
     * {@code sparkwitch_blindness} 之后 20 秒内（购买即施放；只有法术成功才会记录）。
     */
    static boolean killDuringBlindness(AchievementContext context) {
        List<MatchEvent> blindness = context.timeline().events(MatchEvent.SHOP_PURCHASE).stream()
                .filter(purchase -> BLINDNESS_ENTRY.equals(purchase.value("entry_id"))
                        && GRAND_WITCH.equals(context.roleAt(purchase.actor(), purchase)))
                .toList();
        return context.kills().stream().anyMatch(kill -> isAccomplice(context.timeline().killerRole(kill))
                && outsideWitchFaction(context.timeline().victimFaction(kill))
                && blindness.stream().anyMatch(cast -> kill.tick() >= cast.tick()
                        && kill.tick() <= cast.tick() + BLINDNESS_TICKS));
    }

    /**
     * 接过遗志: the witch faction won; I ended as an Accomplice (plain or a variant) and never died; and the Grand Witch
     * died this round (a real death, not a Last Stand false death or a disconnect). Needs the complete record.
     * 魔女阵营获胜；我最终是共犯（普通或变体）且从未死亡；本局大魔女阵亡（真实死亡，不含背水一战假死与断线）。需要完整记录。
     */
    static boolean survivedTheGrandWitch(AchievementContext context) {
        MatchTimeline timeline = context.timeline();
        return timeline.isComplete() && witchFactionWon(context) && !RoundRules.died(context)
                && isAccomplice(timeline.finalRole(context.self()))
                && timeline.realDeaths().stream().anyMatch(death -> !death.isDisconnect()
                        && !Objects.equals(context.self(), death.target())
                        && GRAND_WITCH.equals(timeline.victimRole(death)));
    }

    // ---- Riftwalker 隙行者 ----

    /**
     * 复仇者集结: as the Riftwalker, a Witches' Sabbath and, within 15 s of it, a kill by me or by a teammate it pulled
     * ({@code pulled}) of a player outside the witch faction.
     * 以隙行者身份发动魔女集会，并在其后 15 秒内由我或被召集的队友（{@code pulled}）击杀一名魔女阵营以外的玩家。
     */
    static boolean sabbathThenKill(AchievementContext context) {
        UUID self = context.self();
        List<MatchEvent> deaths = context.timeline().deaths();
        return Rules.mySkillUses(context, WITCHES_SABBATH).stream().anyMatch(sabbath -> {
            if (!Rules.iWas(context, sabbath, RIFTWALKER)) {
                return false;
            }
            Set<UUID> killers = new HashSet<>(uuids(sabbath.list(PULLED)));
            killers.add(self);
            return deaths.stream().anyMatch(death -> death.actor() != null && killers.contains(death.actor())
                    && !death.actor().equals(death.target())
                    && death.tick() >= sabbath.tick() && death.tick() <= sabbath.tick() + SABBATH_FOLLOW_UP_TICKS
                    && outsideWitchFaction(context.timeline().victimFaction(death)));
        });
    }

    /**
     * 查无此人: as the Riftwalker inside a Rift Gate, a Swapper's swap on me was cancelled and the Swapper was crushed:
     * a {@code sparkwitch:portal_crush} global event with me as its target (the occupant) and the Swapper as actor,
     * then that Swapper's death by {@code sparkwitch:portal_crushed}.
     * 以隙行者身份待在裂隙门内时，换位者对我的交换被取消且换位者被压死：以我为目标（门内的人）、换位者为执行者的
     * {@code sparkwitch:portal_crush} 全局事件，随后该换位者以 {@code sparkwitch:portal_crushed} 死亡。
     */
    static boolean swapperCrushed(AchievementContext context) {
        UUID self = context.self();
        List<MatchEvent> deaths = context.timeline().deaths();
        return context.timeline().events(MatchEvent.GLOBAL_EVENT).stream().anyMatch(crush ->
                crush.isGlobal(PORTAL_CRUSH) && self != null && self.equals(crush.target()) && crush.actor() != null
                        && Rules.iWas(context, crush, RIFTWALKER)
                        && deaths.stream().anyMatch(death -> crush.actor().equals(death.target())
                                && death.tick() >= crush.tick() && Rules.reason(death, PORTAL_CRUSHED)));
    }

    // ---- Fiend 魔人 ----

    /**
     * 挨打致富: 300 coins gained in one round while the Fiend for the whole round. The Fiend has no task or starting
     * money, so its only income is the gold for being hit (and for a bomb passed to it), and SparkFactionAPI's
     * {@code sparkfactionapi:income} counts every coin credited this round.
     * 整局都是魔人时，一局内获得 300 金币。魔人没有任务收入与开局金币，唯一收入是挨打（以及被转手炸弹）的赏金，而
     * SparkFactionAPI 的 {@code sparkfactionapi:income} 计入本局入账的全部金币。
     */
    static boolean fiendIncome(AchievementContext context) {
        return context.timeline().roleHistory(context.self()).equals(List.of(FIEND))
                && context.myEvents(MatchEvent.INCOME).stream()
                        .anyMatch(income -> income.intValue("amount", 0) >= BEATEN_RICH_COINS);
    }

    /**
     * 魔人降临: as the Fiend, I won after a Fiend Moment of mine ran its course: my
     * {@code sparkwitch:fiend_moment_start} with no {@code sparkwitch:fiend_moment_end} of mine after it (an end is
     * written when I die, leave, am swallowed or lose the role) and the match ending no earlier than its duration.
     * Needs the complete record.
     * 以魔人身份撑过我的魔人时刻并获胜：我的 {@code sparkwitch:fiend_moment_start} 之后没有我的
     * {@code sparkwitch:fiend_moment_end}（死亡、离开、被吞噬或失去身份时会写），且对局结束不早于时刻的时长。需要完整记录。
     */
    static boolean outlastedFiendMoment(AchievementContext context) {
        MatchTimeline timeline = context.timeline();
        UUID self = context.self();
        if (!context.won() || !timeline.isComplete() || self == null) {
            return false;
        }
        int endTick = timeline.matchEnd().map(MatchEvent::tick).orElse(-1);
        List<MatchEvent> moments = timeline.events(MatchEvent.GLOBAL_EVENT).stream()
                .filter(event -> (event.isGlobal(FIEND_MOMENT_START) || event.isGlobal(FIEND_MOMENT_END))
                        && self.equals(event.uuid(FIEND_KEY) != null ? event.uuid(FIEND_KEY) : event.actor()))
                .toList();
        for (int i = 0; i < moments.size(); i++) {
            MatchEvent start = moments.get(i);
            if (start.isGlobal(FIEND_MOMENT_START) && Rules.iWas(context, start, FIEND)
                    && moments.subList(i + 1, moments.size()).stream().noneMatch(end -> end.isGlobal(FIEND_MOMENT_END))
                    && endTick >= start.tick() + Math.max(0, start.intValue(DURATION_TICKS, FIEND_MOMENT_TICKS))) {
                return true;
            }
        }
        return false;
    }

    // ---- Witch wins 魔女的胜利 ----

    /**
     * 帽子戏法: 3 wins as the Grand Witch, counting rounds where I held the Grand Witch but ended as something else (a
     * Wraith, then a Curser) — {@link LifetimeStats#winsHolding}.
     * 以大魔女身份获胜 3 次；当过大魔女但最终是别的身份（冤魂，再到诅咒者）的获胜局也算（{@link LifetimeStats#winsHolding}）。
     */
    static boolean grandWitchHatTrick(AchievementContext context) {
        return winsHolding(context, GRAND_WITCH) >= HAT_TRICK_WINS;
    }

    /**
     * 瓦尔普吉斯之夜: a win as each of the Grand Witch, the Murderous Witch and the Bewitched line (the Bewitched or any
     * role its promotion gives: the Accomplice and its variants are never drawn otherwise), each counted by any role I
     * held in the won round ({@link LifetimeStats#winsHolding}).
     * 分别以大魔女、杀意魔女和魔化使一系（魔化使或其晋升得到的任何身份：共犯及其变体不会以其他方式出现）各获胜一次，
     * 按获胜那局担任过的任何身份计（{@link LifetimeStats#winsHolding}）。
     */
    static boolean walpurgisNight(AchievementContext context) {
        return winsHolding(context, GRAND_WITCH) >= 1 && winsHolding(context, MURDEROUS_WITCH) >= 1
                && BEWITCHED_LINE.stream().anyMatch(role -> winsHolding(context, role) >= 1);
    }

    /**
     * 一个都不能少: I won on the witch faction, at least 2 players were on it this round, and none of them died (any
     * death record counts, a Last Stand false death too; a disconnect does not). Needs the complete record.
     * 以魔女阵营身份获胜，本局魔女阵营至少 2 人，且无人死亡（任何死亡记录都算，背水一战假死也算；断线不算）。需要完整记录。
     */
    static boolean witchesAllSurvived(AchievementContext context) {
        MatchTimeline timeline = context.timeline();
        if (!timeline.isComplete() || !witchFactionWon(context)) {
            return false;
        }
        Set<UUID> witches = timeline.playersEverIn(Factions.WITCH::equals);
        return witches.size() >= 2 && timeline.deaths().stream().noneMatch(death -> !death.isDisconnect()
                && (witches.contains(death.target()) || Factions.WITCH.equals(death.value("victim_faction"))));
    }

    /**
     * 既生瑜，何生亮: as the Grand Witch or the Murderous Witch, I killed the other of the two when only she and I were
     * alive, and won. Needs the complete record.
     * 以大魔女或杀意魔女身份，在场上只剩我和她时亲手击杀另一位魔女并获胜。需要完整记录。
     */
    static boolean killedTheRivalWitch(AchievementContext context) {
        MatchTimeline timeline = context.timeline();
        UUID self = context.self();
        if (!context.won() || !timeline.isComplete()) {
            return false;
        }
        return context.kills().stream().anyMatch(kill -> kill.target() != null
                && isWitchLeader(timeline.killerRole(kill)) && isWitchLeader(timeline.victimRole(kill))
                && !timeline.isFakeDeath(kill)
                && timeline.aliveBefore(kill).equals(Set.of(self, kill.target())));
    }

    /**
     * 全武行: as the Grand Witch, within the 10 s of a Fear I cast ({@code sparkwitch_fear} purchase), kill a player
     * outside the witch faction with each of the Ceremonial Sword ({@code sparkwitch:ceremonial_blade}), the knife
     * ({@code wathe:knife_stab}) and the revolver ({@code wathe:gun_shot} with {@code killer_item} {@code
     * wathe:revolver}). Cutting down my bound Vendetta with the sword has no death record and is not counted.
     * 以大魔女身份，在我施放的一次恐惧（购买 {@code sparkwitch_fear}）的 10 秒内，分别用仪礼剑（{@code sparkwitch:ceremonial_blade}）、
     * 刀（{@code wathe:knife_stab}）和左轮手枪（{@code killer_item} 为 {@code wathe:revolver} 的 {@code wathe:gun_shot}）
     * 各击杀一名魔女阵营以外的玩家。用剑斩杀与我绑定的仇杀客没有死亡记录，不计入。
     */
    static boolean fullArsenalInFear(AchievementContext context) {
        List<MatchEvent> kills = Rules.myKills(context, kill -> Rules.killedAs(context, kill, GRAND_WITCH)
                && outsideWitchFaction(context.timeline().victimFaction(kill)));
        return context.myEvents(MatchEvent.SHOP_PURCHASE).stream()
                .filter(purchase -> FEAR_ENTRY.equals(purchase.value("entry_id"))
                        && Rules.iWas(context, purchase, GRAND_WITCH))
                .anyMatch(fear -> {
                    List<MatchEvent> inFear = kills.stream().filter(kill -> kill.tick() >= fear.tick()
                            && kill.tick() <= fear.tick() + FEAR_TICKS).toList();
                    return inFear.stream().anyMatch(kill -> Rules.reason(kill, KillRules.CEREMONIAL_BLADE))
                            && inFear.stream().anyMatch(kill -> Rules.reason(kill, KNIFE_STAB))
                            && inFear.stream().anyMatch(kill -> Rules.reason(kill, GUN_SHOT)
                                    && REVOLVER.equals(kill.value(KILLER_ITEM)));
                });
    }

    // ---- Helpers 辅助 ----

    /**
     * Outside the witch faction: a known faction other than {@link Factions#WITCH} ({@link Factions#NONE} is no
     * faction).
     * 魔女阵营以外：已知且不是 {@link Factions#WITCH} 的阵营（{@link Factions#NONE} 不算阵营）。
     */
    static boolean outsideWitchFaction(String factionId) {
        return factionId != null && !factionId.isBlank() && !Factions.NONE.equals(factionId)
                && !Factions.WITCH.equals(factionId);
    }

    /** I won and the witch faction is the side that won. 我获胜，且获胜的是魔女阵营。 */
    static boolean witchFactionWon(AchievementContext context) {
        return context.won() && (Factions.WITCH.equals(context.outcome().winningFactionId())
                || Factions.WITCH.equals(context.faction()));
    }

    /** Whether {@code roleId} is the Accomplice or one of its variants; false for null. 是否为共犯或其变体；null 为否。 */
    private static boolean isAccomplice(String roleId) {
        return roleId != null && ACCOMPLICES.contains(roleId);
    }

    private static boolean isWitchLeader(String roleId) {
        return GRAND_WITCH.equals(roleId) || MURDEROUS_WITCH.equals(roleId);
    }

    /** Rounds won holding {@code roleId} at some point, this round counted. 担任过 {@code roleId} 且获胜的局数（已计入本局）。 */
    private static int winsHolding(AchievementContext context, String roleId) {
        return context.stats().winsHolding(roleId);
    }

    /** My {@code role_changed} events (by {@code player}, else the actor). 我的 {@code role_changed} 事件。 */
    private static List<MatchEvent> myRoleChanges(AchievementContext context) {
        UUID self = context.self();
        return context.timeline().events(MatchEvent.ROLE_CHANGED).stream()
                .filter(change -> self != null
                        && self.equals(change.uuid("player") != null ? change.uuid("player") : change.actor()))
                .toList();
    }

    private static List<UUID> uuids(List<String> values) {
        return values.stream().map(value -> {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException malformed) {
                return null;
            }
        }).filter(Objects::nonNull).toList();
    }
}
