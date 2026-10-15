package dev.caecorthus.sparkassist.achievement;

import dev.caecorthus.sparkassist.achievement.MatchTimeline.TickWindow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * SparkWitch's killer roles (Hunter, Kidnapper, Magician, Ninja, Time Stealer, Witch Maiden) and the Insider, the
 * neutral who wins with the Corrupt Cop. "As role X" is my role at the event, from the record's {@code killer_role}
 * for a kill.
 * SparkWitch 的杀手身份（猎人、绑架者、魔术师、忍者、窃时者、巫女）以及与黑警一同获胜的中立身份内应。“以 X 身份”指该事件
 * 发生时我的身份，击杀取记录中的 {@code killer_role}。
 */
final class WitchKillerRules {
    static final String HUNTER = "sparkwitch:hunter";
    static final String KIDNAPPER = "sparkwitch:kidnapper";
    static final String MAGICIAN = "sparkwitch:magician";
    static final String NINJA = "sparkwitch:ninja";
    static final String TIME_STEALER = "sparkwitch:time_stealer";
    static final String WITCH_MAIDEN = "sparkwitch:witch_maiden";
    static final String INSIDER = "sparkwitch:insider";
    static final String CORRUPT_COP = KillRules.CORRUPT_COP;

    static final String DOUBLE_BARREL_SHOTGUN = "sparkwitch:double_barrel_shotgun";
    static final String GUN_SHOT = "wathe:gun_shot";
    static final String HUNTER_TRAP_TRIGGERED = "sparkwitch:hunter_trap_triggered";
    static final String TRAP_POISON = "sparkwitch:trap";
    /** The trap owner on a trap poisoning ({@code HunterTrapEntity.POISON_PLACER_NBT_KEY}). 捕兽夹中毒记录上的放置者。 */
    static final String TRAP_PLACER = "SparkWitchTrapPlacer";
    static final String KNOCKOUT_DRUG = "sparkwitch:knockout_drug";
    static final String KIDNAPPER_RELEASE = "sparkwitch:kidnapper_release";
    static final String PLAYBACK_STARTED = "sparkwitch:magician_playback_started";
    static final List<String> PLAYBACK_ENDS = List.of("sparkwitch:magician_playback_finished",
            "sparkwitch:magician_playback_stopped_early", "sparkwitch:magician_playback_forced_end");
    static final String KNIFE_STAB = "wathe:knife_stab";
    static final String BAT_HIT = "wathe:bat_hit";
    static final String SHURIKEN_KILL = "sparkwitch:ninja_shuriken_kill";
    static final String POCKET_WATCH = "sparkwitch:time_stealer_pocket_watch";
    static final String TIME_STOLEN = "sparkwitch:time_stolen";
    static final String TOFANA_ELIXIR = "sparkwitch:tofana_elixir";
    /** Wathe's win status when the killers win. 杀手获胜时 Wathe 的胜利状态。 */
    static final String KILLERS = "KILLERS";

    /** A Hunter trap roots its prey for 3 s ({@code HunterRules.TRAP_ROOT_TICKS}). 捕兽夹定身 3 秒。 */
    static final int TRAP_ROOT_TICKS = 60;
    /** A knockout-drug hijack lasts 30 s ({@code KidnapperRules.KNOCKOUT_DRUG_CONTROL_TICKS}). 迷药劫持持续 30 秒。 */
    static final int HIJACK_TICKS = 600;
    static final double ALIBI_BLOCKS = 15;
    static final double FAR_SHURIKEN_BLOCKS = 20;
    static final int STOLEN_TO_DEATH = 3;

    private WitchKillerRules() {
    }

    // ---- Hunter 猎人 ----

    /**
     * 瓮中捉鳖: as the Hunter, a Double-Barrel Shotgun kill ({@code wathe:gun_shot} with my shotgun {@code item_use} on
     * the victim in the same tick) of a player my trap rooted: my {@code sparkwitch:hunter_trap_triggered} on them less
     * than 3 s before. Nothing records the root ending early; a new root starts a fresh 3 s.
     * 以猎人身份用双管猎枪击杀（{@code wathe:gun_shot}，同一 tick 有我对死者的猎枪 {@code item_use}）一名被我的捕兽夹定住的
     * 玩家：此前不到 3 秒内我的 {@code sparkwitch:hunter_trap_triggered} 夹中了此人。定身提前结束没有记录；再次被夹会重新定身 3 秒。
     */
    static boolean shotgunTrappedPrey(AchievementContext context) {
        List<MatchEvent> shots = Rules.myItemUses(context, DOUBLE_BARREL_SHOTGUN);
        List<MatchEvent> traps = Rules.myGlobalEvents(context, HUNTER_TRAP_TRIGGERED);
        return context.kills().stream().anyMatch(kill -> kill.target() != null && Rules.reason(kill, GUN_SHOT)
                && Rules.killedAs(context, kill, HUNTER)
                && shots.stream().anyMatch(shot -> shot.tick() == kill.tick() && kill.target().equals(shot.target()))
                && traps.stream().anyMatch(trap -> kill.target().equals(trap.target())
                        && kill.tick() >= trap.tick() && kill.tick() - trap.tick() < TRAP_ROOT_TICKS));
    }

    /**
     * 联合狩猎: as the Hunter, a poison death whose latest poisoning was my trap ({@code source}
     * {@code sparkwitch:trap}, {@code SparkWitchTrapPlacer} me) poisoned by someone else ({@code poisoner_uuid}; only
     * killer-faction roles can poison a trap). The kill is credited to the poisoner, not to me.
     * 以猎人身份，一起毒死，死者最近一次中毒来自我的捕兽夹（{@code source} 为 {@code sparkwitch:trap}，
     * {@code SparkWitchTrapPlacer} 是我），而毒是别人淬的（{@code poisoner_uuid}；只有杀手阵营身份能给捕兽夹淬毒）。
     * 击杀记在淬毒者名下，不记在我名下。
     */
    static boolean teammatePoisonedTrap(AchievementContext context) {
        UUID self = context.self();
        if (self == null || !context.timeline().roleHistory(self).contains(HUNTER)) {
            return false;
        }
        Map<UUID, MatchEvent> latestPoisoning = new HashMap<>();
        for (MatchEvent event : context.timeline().events()) {
            if (event.is(MatchEvent.PLAYER_POISONED) && event.target() != null) {
                latestPoisoning.put(event.target(), event);
            } else if (event.is(MatchEvent.DEATH) && Rules.reason(event, KillRules.POISON) && event.target() != null) {
                MatchEvent poisoning = latestPoisoning.get(event.target());
                UUID poisoner = poisoning == null ? null : poisoning.uuid("poisoner_uuid");
                if (poisoning != null && TRAP_POISON.equals(poisoning.value("source"))
                        && self.equals(poisoning.uuid(TRAP_PLACER)) && poisoner != null && !poisoner.equals(self)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---- Kidnapper 绑架者 ----

    /**
     * 撕票: as the Kidnapper, I killed the player I was hijacking: my knockout-drug {@code item_use} on them, then my
     * kill of them within the 30 s hijack with no {@code sparkwitch:kidnapper_release} of it in between. A Coroner
     * disguised as the Kidnapper can drug too, so the kill must be made as the Kidnapper.
     * 以绑架者身份亲手杀死正被我劫持的人：我对其使用迷药（{@code item_use}），随后在 30 秒劫持期内、中间没有
     * {@code sparkwitch:kidnapper_release} 时由我击杀此人。伪装成绑架者的验尸官也能用迷药，所以击杀时必须是绑架者。
     */
    static boolean killedMyHostage(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> Rules.killedAs(context, kill, KIDNAPPER)
                && hijackedAt(context, kill, false));
    }

    /**
     * 送货上门: as the Kidnapper, a player I was hijacking (as above) was killed by a killer-faction teammate.
     * 以绑架者身份，正被我劫持的玩家（同上）被杀手阵营的队友杀死。
     */
    static boolean hostageDelivered(AchievementContext context) {
        UUID self = context.self();
        return context.timeline().deaths().stream().anyMatch(death -> death.actor() != null && self != null
                && !death.actor().equals(self) && !death.actor().equals(death.target())
                && Factions.KILLER.equals(context.timeline().killerFaction(death))
                && hijackedAt(context, death, true));
    }

    /**
     * Whether {@code death}'s victim was under my hijack: my latest knockout-drug use on them before it (made as the
     * Kidnapper when {@code asKidnapper}) at most 30 s earlier, and no release of that player in between.
     * {@code death} 的死者此时是否正被我劫持：此前我对其最近一次使用迷药（{@code asKidnapper} 时须以绑架者身份）不超过 30 秒，
     * 且其间没有释放此人。
     */
    private static boolean hijackedAt(AchievementContext context, MatchEvent death, boolean asKidnapper) {
        UUID self = context.self();
        UUID victim = death.target();
        if (self == null || victim == null) {
            return false;
        }
        MatchEvent drug = null;
        for (MatchEvent event : context.timeline().events()) {
            if (event == death) {
                break;
            }
            if (event.is(MatchEvent.ITEM_USE) && KNOCKOUT_DRUG.equals(event.value("item"))
                    && victim.equals(event.target())) {
                drug = self.equals(event.actor()) ? event : null;
            } else if (drug != null && isRelease(event, victim)) {
                drug = null;
            }
        }
        return drug != null && death.tick() - drug.tick() <= HIJACK_TICKS
                && (!asKidnapper || Rules.iWas(context, drug, KIDNAPPER));
    }

    /**
     * A release of {@code victim}: by the Kidnapper (actor, {@code victim} as target) or a natural end (the victim as
     * actor, no target).
     * 对 {@code victim} 的释放：绑架者主动释放（执行者为绑架者、目标为 {@code victim}），或自然结束（执行者为被劫持者，无目标）。
     */
    private static boolean isRelease(MatchEvent event, UUID victim) {
        return event.is(MatchEvent.SKILL_USE) && KIDNAPPER_RELEASE.equals(event.value("skill"))
                && (victim.equals(event.target()) || event.target() == null && victim.equals(event.actor()));
    }

    // ---- Magician 魔术师 ----

    /**
     * 不在场证明: as the Magician, during my playback, a knife or bat kill ({@code wathe:knife_stab} /
     * {@code wathe:bat_hit}, credited to me) of a player 15+ blocks from me ({@code distance} is from the Magician
     * himself; a real knife reaches 3 blocks, so such a kill is the puppet's). Playback runs from
     * {@code sparkwitch:magician_playback_started} to its finished / stopped-early / forced-end event, my death or the
     * end of the record.
     * 以魔术师身份在播放期间，一次刀或球棒击杀（{@code wathe:knife_stab} / {@code wathe:bat_hit}，记在我名下），死者离我
     * 15 格及以外（{@code distance} 是魔术师本人到死者的距离；真人的刀只够得到 3 格，所以这样的击杀出自皮套）。播放期从
     * {@code sparkwitch:magician_playback_started} 到其结束、提前停止或被迫结束事件、我死亡或记录结束。
     */
    static boolean puppetAlibi(AchievementContext context) {
        List<TickWindow> playback = playbackWindows(context);
        return context.kills().stream().anyMatch(kill -> (Rules.reason(kill, KNIFE_STAB) || Rules.reason(kill, BAT_HIT))
                && Rules.killedAs(context, kill, MAGICIAN)
                && kill.doubleValue("distance", 0) >= ALIBI_BLOCKS
                && playback.stream().anyMatch(window -> window.contains(kill.tick())));
    }

    private static List<TickWindow> playbackWindows(AchievementContext context) {
        UUID self = context.self();
        List<TickWindow> windows = new ArrayList<>();
        Integer openSince = null;
        for (MatchEvent event : context.timeline().events()) {
            if (self == null) {
                break;
            }
            boolean mine = self.equals(event.actor());
            if (mine && event.isGlobal(PLAYBACK_STARTED)) {
                openSince = openSince == null ? event.tick() : openSince;
            } else if (openSince != null && (mine && PLAYBACK_ENDS.stream().anyMatch(event::isGlobal)
                    || event.is(MatchEvent.DEATH) && self.equals(event.target()))) {
                windows.add(new TickWindow(openSince, Math.max(openSince, event.tick())));
                openSince = null;
            }
        }
        if (openSince != null) {
            windows.add(new TickWindow(openSince, Math.max(openSince, context.timeline().endTick())));
        }
        return List.copyOf(windows);
    }

    // ---- Ninja 忍者 ----

    /**
     * 小李飞刀: as the Ninja, a shuriken kill ({@code sparkwitch:ninja_shuriken_kill}) of a player 20+ blocks away. The
     * recorded {@code distance} is measured when the shuriken lands, not when it was thrown (approximation).
     * 以忍者身份用手里剑击杀（{@code sparkwitch:ninja_shuriken_kill}）20 格及以外的玩家。记录的 {@code distance} 是手里剑命中时
     * 的距离，不是出手时的距离（近似）。
     */
    static boolean farShurikenKill(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> Rules.reason(kill, SHURIKEN_KILL)
                && Rules.killedAs(context, kill, NINJA)
                && kill.doubleValue("distance", 0) >= FAR_SHURIKEN_BLOCKS);
    }

    // ---- Time Stealer 窃时者 ----

    /**
     * 迟到的钟声: after my (real) death, a player whose time I stole as the Time Stealer ran out of it: a
     * {@code sparkwitch:time_stolen} death whose latest pocket-watch theft was mine. The theft decides, not the killer:
     * a Time Stealer who left or became a Wraith is no longer credited with the kill.
     * 我（真正）死亡之后，被我以窃时者身份窃取时间的玩家时光耗尽而死：一次 {@code sparkwitch:time_stolen} 死亡，死者最近
     * 一次被怀表窃时是我干的。以窃时记录为准，而不是击杀者：窃时者离线或变成冤魂后不再记为击杀者。
     */
    static boolean lateChime(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        MatchEvent myDeath = timeline.realDeaths().stream()
                .filter(death -> self != null && self.equals(death.target())).findFirst().orElse(null);
        if (myDeath == null) {
            return false;
        }
        int after = timeline.events().indexOf(myDeath);
        return stolenToDeath(context).stream().anyMatch(death -> timeline.events().indexOf(death) > after);
    }

    /**
     * 时间管理大师: in one round, 3 players outside the killer faction died of time I stole as the Time Stealer (as
     * above), and the killer faction won.
     * 一局内 3 名杀手阵营以外的玩家死于我以窃时者身份窃取的时间（同上），且杀手阵营获胜。
     */
    static boolean stoleThreeToDeath(AchievementContext context) {
        if (!killerFactionWon(context)) {
            return false;
        }
        Set<UUID> victims = new HashSet<>();
        for (MatchEvent death : stolenToDeath(context)) {
            String faction = context.timeline().victimFaction(death);
            if (isFaction(faction) && !Factions.KILLER.equals(faction)) {
                victims.add(death.target());
            }
        }
        return victims.size() >= STOLEN_TO_DEATH;
    }

    /**
     * {@code sparkwitch:time_stolen} deaths whose victim's latest pocket-watch theft before it was mine, as the Time
     * Stealer.
     * 死者此前最近一次被怀表窃时是我（以窃时者身份）所为的 {@code sparkwitch:time_stolen} 死亡。
     */
    private static List<MatchEvent> stolenToDeath(AchievementContext context) {
        UUID self = context.self();
        Map<UUID, MatchEvent> latestTheft = new HashMap<>();
        List<MatchEvent> deaths = new ArrayList<>();
        for (MatchEvent event : context.timeline().events()) {
            if (event.is(MatchEvent.ITEM_USE) && POCKET_WATCH.equals(event.value("item")) && event.target() != null) {
                latestTheft.put(event.target(), event);
            } else if (event.is(MatchEvent.DEATH) && Rules.reason(event, TIME_STOLEN) && event.target() != null) {
                MatchEvent theft = latestTheft.get(event.target());
                if (theft != null && self != null && self.equals(theft.actor())
                        && Rules.iWas(context, theft, TIME_STEALER)) {
                    deaths.add(event);
                }
            }
        }
        return List.copyOf(deaths);
    }

    private static boolean killerFactionWon(AchievementContext context) {
        return Factions.KILLER.equals(context.outcome().winningFactionId())
                || KILLERS.equals(context.outcome().winStatus());
    }

    // ---- Witch Maiden 巫女 ----

    /**
     * 以彼之道: as the Witch Maiden, my Tofana Elixir's counter-kill ({@code sparkwitch:tofana_elixir}, credited to me;
     * it only follows a kill attempt on me) of a player outside the killer faction.
     * 以巫女身份，托法娜仙液的反杀（{@code sparkwitch:tofana_elixir}，记在我名下；只会在有人对我下杀手之后发生）杀死一名杀手阵营
     * 以外的玩家。
     */
    static boolean tofanaCounterKill(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> Rules.reason(kill, TOFANA_ELIXIR)
                && Rules.killedAs(context, kill, WITCH_MAIDEN)
                && isFaction(context.timeline().victimFaction(kill))
                && !Factions.KILLER.equals(context.timeline().victimFaction(kill)));
    }

    // ---- Insider 内应 ----

    /** 无间道: as the Insider, a kill of a police role. 以内应身份击杀一名警职。 */
    static boolean insiderKilledPolice(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> Rules.killedAs(context, kill, INSIDER)
                && RoleGroups.isPolice(context.timeline().victimRole(kill)));
    }

    /**
     * 孤胆嘉豪: I ended the round as the Insider, never died and won, and every Corrupt Cop of the round died (a real
     * death, not a Last Stand false death or a disconnect). Needs the complete record.
     * 我以内应身份结束本局、从未死亡并获胜，且本局每名黑警都已阵亡（真实死亡，不含背水一战假死与断线）。需要完整记录。
     */
    static boolean insiderOutlivedTheCop(AchievementContext context) {
        MatchTimeline timeline = context.timeline();
        UUID self = context.self();
        if (!context.won() || !timeline.isComplete() || RoundRules.died(context) || self == null
                || !INSIDER.equals(timeline.finalRole(self))) {
            return false;
        }
        Set<UUID> cops = new HashSet<>();
        for (UUID player : timeline.startingPlayers()) {
            if (timeline.roleHistory(player).contains(CORRUPT_COP)) {
                cops.add(player);
            }
        }
        Set<UUID> fallen = new HashSet<>();
        for (MatchEvent death : timeline.realDeaths()) {
            if (!death.isDisconnect() && cops.contains(death.target())) {
                fallen.add(death.target());
            }
        }
        Set<UUID> aliveAtEnd = timeline.aliveAt(timeline.endTick());
        return !cops.isEmpty() && fallen.containsAll(cops) && cops.stream().noneMatch(aliveAtEnd::contains);
    }

    private static boolean isFaction(String factionId) {
        return factionId != null && !factionId.isBlank() && !Objects.equals(Factions.NONE, factionId);
    }
}
