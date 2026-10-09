package dev.caecorthus.sparkassist.achievement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Kills: by side and role, by weapon, several at once, in a state (psycho, Show Off), and the killed-role
 * collections kept in {@link LifetimeStats}. Roles and factions are those at the kill ({@code killer_role} /
 * {@code killer_faction} / {@code victim_faction}); dying by your own hand is never a kill.
 * 击杀：按阵营与身份、按武器、一次多杀、在某种状态下（疯魔、展示豪度），以及 {@link LifetimeStats} 中的击杀身份收集。
 * 身份与阵营取击杀那一刻（{@code killer_role} / {@code killer_faction} / {@code victim_faction}）；死于自己之手从不算击杀。
 */
final class KillRules {
    static final String UNDERCOVER = "noellesroles:undercover";
    static final String APPRENTICE_WITCH = "sparkwitch:apprentice_witch";
    static final String PIG_GOD = "sparkwitch:pig_god";
    static final String BOMBER = "noellesroles:bomber";
    static final String USEC = "sparkwitch:usec";
    static final String CORRUPT_COP = "noellesroles:corrupt_cop";
    static final String SERIAL_KILLER = "noellesroles:serial_killer";
    static final String POISONER = "noellesroles:poisoner";

    static final String CEREMONIAL_BLADE = "sparkwitch:ceremonial_blade";
    static final String MIGHTY_FORCE = "sparkwitch:mighty_force";
    static final String BOMB = "noellesroles:bomb";
    static final String GRENADE = "wathe:grenade";
    static final String POTION_SHELL = "sparkwitch:potion_shell";
    static final String BELL_TOLL = "sparkwitch:bell_toll";
    static final String POISON = "wathe:poison";

    static final String VENDETTA_TERMINAL = "sparkwitch:vendetta_terminal";
    static final String MURDER_SENSE = "sparkwitch:murder_sense";
    static final String BOMB_DRONE_DETONATED = "sparkstrength:bomb_drone_detonated";
    static final String M67 = "sparkstrength:m67";
    static final String DRONE_GRENADE_DROPPED = "sparkstrength:drone_grenade_dropped";
    static final String USEC_RIFLE_FIRE = "sparkwitch:usec_rifle_fire";
    static final String FIRE_KIND = "fire";
    static final String SHOW_OFF = "sparkstrength:corrupt_cop_show_off";
    static final String GAS_BOMB = "noellesroles:gas_bomb";

    /** An M67 goes off this many ticks after it is thrown or dropped. M67 投出或投下后经过这么多 tick 爆炸。 */
    static final int GRENADE_FUSE_TICKS = 100;
    static final int GRENADE_FUSE_SLACK = 2;
    static final double POINT_BLANK_BLOCKS = 12;
    static final double CALIBRATED_BLOCKS = 50;

    private KillRules() {
    }

    // ---- Sides and roles 阵营与身份 ----

    /** 初露锋芒: a kill on the killer faction. 站在杀手阵营时的击杀。 */
    static boolean killAsKiller(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> Factions.KILLER.equals(context.timeline().killerFaction(kill)));
    }

    /** 正义制裁: a police role on the civilian side kills a 非好人. 好人一方的警职击杀非好人。 */
    static boolean policeKillOfOtherSide(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> civilianKillOfOtherSide(context, kill)
                && RoleGroups.isPolice(context.timeline().killerRole(kill)));
    }

    /** 群众的力量: a non-police 好人 kills a 非好人. 非警职的好人击杀非好人。 */
    static boolean civilianKillOfOtherSide(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> civilianKillOfOtherSide(context, kill)
                && !RoleGroups.isPolice(context.timeline().killerRole(kill)));
    }

    private static boolean civilianKillOfOtherSide(AchievementContext context, MatchEvent kill) {
        return Factions.isCivilian(context.timeline().killerFaction(kill)) && Rules.victimNonCivilian(context, kill);
    }

    /** 完美伪装: the Undercover kills a killer. 卧底击杀杀手。 */
    static boolean undercoverKillsKiller(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> Rules.killedAs(context, kill, UNDERCOVER)
                && Factions.KILLER.equals(context.timeline().victimFaction(kill)));
    }

    /**
     * 拜你所赐: the Grand Witch's Ceremonial Blade kill. Striking down the Vendetta bound to her is cancelled into a
     * {@code sparkwitch:vendetta_terminal} global event (her as actor, the blade as {@code death_reason}) with no
     * death record, and counts too.
     * 大魔女用仪礼剑的击杀。斩杀与她绑定的仇杀客会被取消，改记为 {@code sparkwitch:vendetta_terminal} 全局事件
     * （她为执行者，{@code death_reason} 为仪礼剑），没有死亡记录，同样计入。
     */
    static boolean ceremonialBladeKill(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> Rules.reason(kill, CEREMONIAL_BLADE)
                && Rules.killedAs(context, kill, RoleGroups.GRAND_WITCH_ROLE))
                || Rules.myGlobalEvents(context, VENDETTA_TERMINAL).stream().anyMatch(terminal ->
                Rules.reason(terminal, CEREMONIAL_BLADE) && Rules.iWas(context, terminal, RoleGroups.GRAND_WITCH_ROLE));
    }

    /** 力大无穷: the Apprentice Witch's Mighty Force kills a 非好人. 预备魔女用巨力击杀非好人。 */
    static boolean mightyForceKill(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> Rules.reason(kill, MIGHTY_FORCE)
                && Rules.killedAs(context, kill, APPRENTICE_WITCH) && Rules.victimNonCivilian(context, kill));
    }

    /** 杀意外露: the Apprentice Witch kills a 非好人 during Murder Sense. 预备魔女在杀意感知期间击杀非好人。 */
    static boolean killDuringMurderSense(AchievementContext context) {
        List<MatchEvent> senses = context.myEvents(MURDER_SENSE);
        return context.kills().stream().anyMatch(kill -> Rules.killedAs(context, kill, APPRENTICE_WITCH)
                && Rules.victimNonCivilian(context, kill)
                && senses.stream().anyMatch(sense -> sense.has("duration")
                        && kill.tick() >= sense.tick()
                        && kill.tick() <= sense.tick() + sense.intValue("duration", 0)));
    }

    // ---- Several at once 一次多杀 ----

    /** 致命视角: one bomb-drone detonation kills 3 as the Bomber. 炸弹客的自爆无人机一次炸死 3 人。 */
    static boolean bombDroneTriple(AchievementContext context) {
        List<MatchEvent> bombKills = Rules.myKills(context, kill -> Rules.reason(kill, BOMB)
                && Rules.killedAs(context, kill, BOMBER));
        return Rules.myGlobalEvents(context, BOMB_DRONE_DETONATED).stream().anyMatch(detonation ->
                bombKills.stream().filter(kill -> kill.tick() == detonation.tick()).count() >= 3);
    }

    /**
     * 掷弹兵: one of my M67s (thrown, or dropped by my drone) kills 3 when it goes off a fuse later.
     * 我的一颗 M67（投掷，或由无人机投下）在引信结束时炸死 3 人。
     */
    static boolean grenadeTriple(AchievementContext context) {
        List<MatchEvent> releases = new ArrayList<>(Rules.myItemUses(context, M67));
        releases.addAll(Rules.myGlobalEvents(context, DRONE_GRENADE_DROPPED));
        return MatchTimeline.groupByTick(Rules.myKills(context, kill -> Rules.reason(kill, GRENADE))).stream()
                .anyMatch(blast -> blast.size() >= 3 && releases.stream().anyMatch(release ->
                        Math.abs(blast.getFirst().tick() - GRENADE_FUSE_TICKS - release.tick()) <= GRENADE_FUSE_SLACK));
    }

    /** 反载: one TR shell kills 3. 一发 TR 弹击杀 3 人。 */
    static boolean potionShellTriple(AchievementContext context) {
        return Rules.mostAtOneTick(Rules.myKills(context, kill -> Rules.reason(kill, POTION_SHELL))) >= 3;
    }

    /** 丧钟为你而响: one toll of the bell takes 4. 钟声一次收割 4 人。 */
    static boolean bellTollQuadruple(AchievementContext context) {
        return Rules.mostAtOneTick(Rules.myKills(context, kill -> Rules.reason(kill, BELL_TOLL))) >= 4;
    }

    /**
     * 抽大烟: 4 poison deaths whose latest poisoning was my gas bomb, as the Poisoner.
     * 以毒师身份，4 起毒死且死者最近一次中毒来自我的毒气弹。
     */
    static boolean gasBombQuadruple(AchievementContext context) {
        UUID self = context.self();
        Map<UUID, MatchEvent> latestPoisoning = new HashMap<>();
        Set<UUID> victims = new HashSet<>();
        for (MatchEvent event : context.timeline().events()) {
            if (event.is(MatchEvent.PLAYER_POISONED) && event.target() != null) {
                latestPoisoning.put(event.target(), event);
            } else if (event.is(MatchEvent.DEATH) && Rules.reason(event, POISON) && event.target() != null
                    && !event.target().equals(self) && !creditedToSomeoneElse(event, self)) {
                MatchEvent poisoning = latestPoisoning.get(event.target());
                if (poisoning != null && GAS_BOMB.equals(poisoning.value("source"))
                        && self != null && self.equals(poisoning.uuid("poisoner_uuid"))
                        && Rules.iWas(context, event, POISONER)) {
                    victims.add(event.target());
                }
            }
        }
        return victims.size() >= 4;
    }

    /**
     * A poison death credited to another player: a Catalyst handed the poison over without a new poisoning record.
     * 记在别人名下的毒死：催化剂在不写新中毒记录的情况下转移了下毒者。
     */
    private static boolean creditedToSomeoneElse(MatchEvent death, UUID self) {
        return death.actor() != null && !death.actor().equals(self);
    }

    // ---- USEC 的 AXMC ----

    /** 危险零距离: an unscoped AXMC kill within 12 blocks. 不开镜在 12 格及以内击杀。 */
    static boolean unscopedCloseRifleKill(AchievementContext context) {
        return rifleKill(context, shot -> shot.has("scoped") && !shot.bool("scoped")
                && shot.doubleValue("distance", Double.MAX_VALUE) <= POINT_BLANK_BLOCKS);
    }

    /** 致命校准: an AXMC kill 50+ blocks away. 在 50 格及以外击杀。 */
    static boolean longRifleKill(AchievementContext context) {
        return rifleKill(context, shot -> shot.doubleValue("distance", 0) >= CALIBRATED_BLOCKS);
    }

    /**
     * A USEC rifle shot passing {@code shotTest} that killed the player it hit in the same tick. The innocent-shot
     * punishment is written as a second shot with {@code kind} {@code punish} and is not a shot.
     * 满足条件、且在同一 tick 击杀所命中玩家的 USEC 步枪射击。误杀惩罚会以 {@code kind} 为 {@code punish} 的第二条射击记录写下，不算射击。
     */
    private static boolean rifleKill(AchievementContext context, Predicate<MatchEvent> shotTest) {
        return Rules.myItemUses(context, USEC_RIFLE_FIRE).stream().anyMatch(shot -> shot.target() != null
                && FIRE_KIND.equals(shot.value("kind", FIRE_KIND)) && shot.has("distance") && shotTest.test(shot)
                && context.kills().stream().anyMatch(kill -> kill.tick() == shot.tick()
                        && shot.target().equals(kill.target()) && Rules.killedAs(context, kill, USEC)));
    }

    // ---- In a state 在某种状态下 ----

    /** MGW: 8 kills in one psycho window on the killer faction. 一次疯魔中以杀手阵营击杀 8 人。 */
    static boolean killerPsychoOctuple(AchievementContext context) {
        return Rules.mostInOneWindow(
                Rules.myKills(context, kill -> Factions.KILLER.equals(context.timeline().killerFaction(kill))),
                context.timeline().psychoWindows(context.self())) >= 8;
    }

    /** 野猪冲撞: the Pig God kills 3 非好人 in one chase. 皮革嘎在一次追击中击杀 3 名非好人。 */
    static boolean pigGodChaseTriple(AchievementContext context) {
        return Rules.mostInOneWindow(
                Rules.myKills(context, kill -> Rules.killedAs(context, kill, PIG_GOD) && Rules.victimNonCivilian(context, kill)),
                context.timeline().psychoWindows(context.self())) >= 3;
    }

    /** 展示豪度: the Corrupt Cop kills 8 while showing off, in one round. 黑警在展示豪度状态下一局击杀 8 人。 */
    static boolean showOffOctuple(AchievementContext context) {
        return Rules.countInWindows(
                Rules.myKills(context, kill -> Rules.killedAs(context, kill, CORRUPT_COP)),
                Rules.myToggleWindows(context, SHOW_OFF)) >= 8;
    }

    /** 双枪会给出答案: the Serial Killer kills 8 in psycho, in one round. 连环杀手在疯魔状态下一局击杀 8 人。 */
    static boolean serialKillerPsychoOctuple(AchievementContext context) {
        return Rules.myKills(context, kill -> Rules.killedAs(context, kill, SERIAL_KILLER)
                && (kill.bool("killer_psycho") || context.inPsycho(kill.tick()))).size() >= 8;
    }

    /**
     * 救世主: with 4+ killers at the start, kill at least half of the players other than me who were 非好人 at any
     * point (by any role they held, or the faction recorded at a death).
     * 开局有四名及以上杀手时，击杀除我之外曾经是非好人的玩家（按担任过的任一身份，或死亡记录中的阵营）中的至少一半。
     */
    static boolean killHalfTheOtherSide(AchievementContext context) {
        MatchTimeline timeline = context.timeline();
        Set<UUID> players = new HashSet<>();
        for (MatchEvent event : timeline.events()) {
            if (event.is(MatchEvent.ROLE_ASSIGNED) && event.uuid("player.uuid") != null) {
                players.add(event.uuid("player.uuid"));
            }
        }
        long killersAtStart = players.stream()
                .filter(player -> Factions.KILLER.equals(timeline.baseFaction(timeline.openingRole(player))))
                .count();
        if (killersAtStart < 4) {
            return false;
        }
        Set<UUID> otherSide = new HashSet<>();
        for (UUID player : players) {
            if (!player.equals(context.self()) && timeline.roleHistory(player).stream()
                    .anyMatch(role -> Factions.isNonCivilian(timeline.baseFaction(role)))) {
                otherSide.add(player);
            }
        }
        for (MatchEvent death : timeline.deaths()) {
            addIfNonCivilian(otherSide, death.target(), death.value("victim_faction"), context.self());
            addIfNonCivilian(otherSide, death.actor(), death.value("killer_faction"), context.self());
        }
        long killed = Rules.myKills(context, kill -> Rules.victimNonCivilian(context, kill)).stream()
                .map(MatchEvent::target).distinct().count();
        return !otherSide.isEmpty() && killed * 2 >= otherSide.size();
    }

    private static void addIfNonCivilian(Set<UUID> otherSide, UUID player, String faction, UUID self) {
        if (player != null && !player.equals(self) && Factions.isNonCivilian(faction)) {
            otherSide.add(player);
        }
    }

    // ---- Killed-role collections 击杀身份收集 ----

    /** 滚雪球: as police, ever killed 2/3 of the killer roles. 以警职累计击杀过 2/3 的杀手身份。 */
    static boolean policeKilledMostKillerRoles(AchievementContext context) {
        return killedTwoThirds(context.stats().killedRoles(RoleGroups.POLICE), RoleGroups.KILLER_ROLES);
    }

    /** 滚雪球+: the same in one police life. 在一条警职的命里完成。 */
    static boolean policeKilledMostKillerRolesInOneLife(AchievementContext context) {
        return killedTwoThirds(context.stats().streakKilledRoles(RoleGroups.POLICE), RoleGroups.KILLER_ROLES);
    }

    /** 大满贯: as a killer, ever killed 2/3 of the police roles. 以杀手身份累计击杀过 2/3 的警职。 */
    static boolean killerKilledMostPoliceRoles(AchievementContext context) {
        return killedTwoThirds(context.stats().killedRoles(RoleGroups.KILLER), RoleGroups.POLICE_ROLES);
    }

    /** 大满贯+: the same in one killer life. 在一条杀手的命里完成。 */
    static boolean killerKilledMostPoliceRolesInOneLife(AchievementContext context) {
        return killedTwoThirds(context.stats().streakKilledRoles(RoleGroups.KILLER), RoleGroups.POLICE_ROLES);
    }

    /** 血仇: as the Grand Witch, ever killed 2/3 of the police and killer roles. 以大魔女身份累计击杀过 2/3 的警职与杀手身份。 */
    static boolean grandWitchKilledMostPoliceAndKillerRoles(AchievementContext context) {
        Set<String> wanted = new HashSet<>(RoleGroups.POLICE_ROLES);
        wanted.addAll(RoleGroups.KILLER_ROLES);
        return killedTwoThirds(context.stats().killedRoles(RoleGroups.GRAND_WITCH), wanted);
    }

    /** 2/3 of {@code roles}, rounded up: 15 of 22, 5 of 7, 20 of 29. {@code roles} 的 2/3，向上取整。 */
    static int twoThirds(Set<String> roles) {
        return (roles.size() * 2 + 2) / 3;
    }

    private static boolean killedTwoThirds(Set<String> killed, Set<String> roles) {
        return killed.stream().filter(roles::contains).count() >= twoThirds(roles);
    }
}
