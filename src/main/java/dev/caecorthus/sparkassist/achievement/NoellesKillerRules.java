package dev.caecorthus.sparkassist.achievement;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Noelle's Roles killer roles (Swapper, Party Animal, Scavenger, Morphling, Phantom, Silencer, Assassin, Bandit), plus
 * SparkTraits' Last Escape. Every window is in record ticks, measured from the record event that started it; "as role
 * X" is my role at that event.
 * Noelle's Roles 的杀手身份（交换者、派对狂、清道夫、变形者、幽灵、静语者、刺客、强盗），以及 SparkTraits 的绝处逢生。
 * 所有时间窗口都以记录 tick 计，从开启它的那条记录事件算起；“以 X 身份”指该事件发生时我的身份。
 */
final class NoellesKillerRules {
    static final String SWAPPER = "noellesroles:swapper";
    static final String PARTY_ANIMAL = "noellesroles:party_animal";
    static final String SCAVENGER = "noellesroles:scavenger";
    static final String MORPHLING = "noellesroles:morphling";
    static final String PHANTOM = "noellesroles:phantom";
    static final String SILENCER = "noellesroles:silencer";
    static final String ASSASSIN = "noellesroles:assassin";
    static final String BANDIT = "noellesroles:bandit";
    static final String NOISEMAKER = "noellesroles:noisemaker";

    static final String KNIFE_STAB = "wathe:knife_stab";
    static final String ASSASSINATED = "noellesroles:assassinated";
    static final String ASSASSIN_MISFIRE = "noellesroles:assassin_misfire";
    static final String THROWING_AXE = "noellesroles:throwing_axe";
    /** The Scavenger's shop entry that resets the knife cooldown. 清道夫商店中重置刀冷却的条目。 */
    static final String RESET_KNIFE_COOLDOWN = "noellesroles:scavenger_reset_knife_cd";
    /** Hidden from its owner until it triggers. 触发之前对持有者隐藏。 */
    static final String LAST_ESCAPE = "sparktraits:last_escape";

    static final String SWAP_ACTION = "swap";
    static final String BUZZ_ACTION = "buzz";
    static final String MORPH_ACTION = "morph";
    static final String INVISIBLE_ACTION = "invisible";
    static final String SILENCE_ACTION = "silence";

    static final String PHANTOM_BACKPACK_STARTED = "sparkstrength:phantom_backpack_invisibility_started";
    static final String PHANTOM_BACKPACK_ENDED = "sparkstrength:phantom_backpack_invisibility_ended";
    static final String MORPH_REAGENT_MARKED = "sparkstrength:morph_reagent_marked";
    static final String MORPH_MARK_TRIGGERED = "sparkstrength:morph_mark_triggered";
    static final String MORPH_MARK_ENDED = "sparkstrength:morph_mark_ended";

    /** 10 seconds. 10 秒。 */
    static final int SWAP_TO_DEATH_TICKS = 200;
    /** 5 seconds. 5 秒。 */
    static final int KILL_TO_SWAP_TICKS = 100;
    /** 10 seconds. 10 秒。 */
    static final int KNIFE_PAIR_TICKS = 200;
    static final int TOP_BUZZ_LEVEL = 3;
    /** A voice change lasts 7.5 minutes; buzzing again restarts it. 变声持续 7.5 分钟；再次释放会重新计时。 */
    static final int BUZZ_TICKS = 9000;
    static final int CHORUS_SIZE = 5;
    /** The Morphling's own morph, 35 s. 变形者自身的变形，35 秒。 */
    static final int MORPH_TICKS = 700;
    /** A triggered morph-reagent disguise, 60 s. 已触发的变形试剂伪装，60 秒。 */
    static final int REAGENT_TICKS = 1200;
    /** The Phantom's invisibility and the backpack's, 30 s each. 幽灵自身隐身与背包隐身，各 30 秒。 */
    static final int INVISIBILITY_TICKS = 600;
    static final int INVISIBLE_KILLS = 2;
    /** A silence, 60 s. 沉默，60 秒。 */
    static final int SILENCE_TICKS = 1200;
    static final int SILENT_PSYCHO_KILLS = 5;
    static final int FLAWLESS_ASSASSINATIONS = 3;
    static final double HUNDRED_PACES_BLOCKS = 25;

    private NoellesKillerRules() {
    }

    // ---- Swapper 交换者 ----

    /**
     * 借刀杀人: within 10 s of my swap as the Swapper, one of the two swapped players (not me) dies with a known faction
     * other than killer, by anyone's hand but mine or by accident (no killer). Disconnects are not deaths.
     * 以交换者身份交换后 10 秒内，被交换的两人之一（不是我）死亡，其阵营已知且不是杀手，凶手不是我（他人所杀或没有凶手的意外）。
     * 断线不算死亡。
     */
    static boolean swapThenSomeoneElseKills(AchievementContext context) {
        UUID self = context.self();
        List<MatchEvent> deaths = context.timeline().deaths().stream()
                .filter(death -> !death.isDisconnect() && death.target() != null && !death.target().equals(self)
                        && !(self != null && self.equals(death.actor())) && victimNotKiller(context, death))
                .toList();
        return mySwaps(context).stream().anyMatch(swap -> deaths.stream().anyMatch(death ->
                swapped(swap).contains(death.target())
                        && death.tick() >= swap.tick() && death.tick() <= swap.tick() + SWAP_TO_DEATH_TICKS));
    }

    /**
     * 金蝉脱壳: within 5 s after a kill as the Swapper, a swap of myself with another player.
     * 以交换者身份击杀后 5 秒内，把自己与另一名玩家交换。
     */
    static boolean killThenSwapAway(AchievementContext context) {
        UUID self = context.self();
        List<MatchEvent> escapes = mySwaps(context).stream()
                .filter(swap -> swapped(swap).contains(self) && swapped(swap).size() == 2)
                .toList();
        return Rules.myKills(context, kill -> Rules.killedAs(context, kill, SWAPPER)).stream()
                .anyMatch(kill -> escapes.stream().anyMatch(swap ->
                        swap.tick() >= kill.tick() && swap.tick() <= kill.tick() + KILL_TO_SWAP_TICKS));
    }

    /** My swaps as the Swapper (both teleports succeeded). 我以交换者身份完成的交换（两次传送都成功）。 */
    private static List<MatchEvent> mySwaps(AchievementContext context) {
        return Rules.mySkillUses(context, SWAPPER).stream()
                .filter(swap -> SWAP_ACTION.equals(swap.value("action", SWAP_ACTION))
                        && Rules.iWas(context, swap, SWAPPER))
                .toList();
    }

    /** The players a swap moved ({@code target1}, {@code target2}). 一次交换移动的玩家。 */
    private static Set<UUID> swapped(MatchEvent swap) {
        Set<UUID> players = new LinkedHashSet<>();
        addIfPresent(players, swap.uuid("target1"));
        addIfPresent(players, swap.uuid("target2"));
        if (players.isEmpty()) {
            addIfPresent(players, swap.target());
        }
        return players;
    }

    // ---- Party Animal 派对狂 ----

    /**
     * 唐老鸭: as the Party Animal, a buzz that brings another player's voice change to level 3.
     * 以派对狂身份释放，使另一名玩家的变声达到 3 级。
     */
    static boolean buzzToTopLevel(AchievementContext context) {
        return myBuzzes(context).stream().anyMatch(buzz -> buzz.intValue("level", 0) >= TOP_BUZZ_LEVEL);
    }

    /**
     * 群鸭乱舞: right after one of my buzzes as the Party Animal, 5 living players other than me are each within 7.5 min
     * of a buzz of mine. Buzzes by anyone else do not count.
     * 我以派对狂身份的某次释放刚生效时，有 5 名我以外的存活玩家各自处在我某次释放后的 7.5 分钟内。别人的释放不算。
     */
    static boolean fiveVoicesAtOnce(AchievementContext context) {
        List<MatchEvent> buzzes = myBuzzes(context);
        for (MatchEvent moment : buzzes) {
            Set<UUID> alive = context.timeline().aliveAfter(moment);
            long buzzing = buzzes.stream()
                    .filter(buzz -> buzz.tick() <= moment.tick() && moment.tick() - buzz.tick() < BUZZ_TICKS
                            && alive.contains(buzz.target()))
                    .map(MatchEvent::target)
                    .distinct()
                    .count();
            if (buzzing >= CHORUS_SIZE) {
                return true;
            }
        }
        return false;
    }

    /** My buzzes on another player as the Party Animal. 我以派对狂身份对其他玩家的释放。 */
    private static List<MatchEvent> myBuzzes(AchievementContext context) {
        UUID self = context.self();
        return Rules.mySkillUses(context, PARTY_ANIMAL).stream()
                .filter(buzz -> BUZZ_ACTION.equals(buzz.value("action")) && buzz.target() != null
                        && !buzz.target().equals(self) && Rules.iWas(context, buzz, PARTY_ANIMAL))
                .toList();
    }

    // ---- Scavenger 清道夫 ----

    /**
     * 钞能力: as the Scavenger, two knife kills of two players no more than 10 s apart, with a knife-cooldown reset
     * bought between them (both ends included).
     * 以清道夫身份，两次相隔不超过 10 秒、刺死两名不同玩家的刀杀，且两次之间（含两端）买过重置刀冷却。
     */
    static boolean resetKnifeForDoubleKill(AchievementContext context) {
        List<MatchEvent> resets = context.myEvents(MatchEvent.SHOP_PURCHASE).stream()
                .filter(purchase -> RESET_KNIFE_COOLDOWN.equals(purchase.value("item"))
                        && Rules.iWas(context, purchase, SCAVENGER))
                .toList();
        List<MatchEvent> stabs = Rules.myKills(context, kill -> Rules.reason(kill, KNIFE_STAB)
                && Rules.killedAs(context, kill, SCAVENGER));
        for (int i = 0; i < stabs.size(); i++) {
            MatchEvent first = stabs.get(i);
            for (MatchEvent second : stabs.subList(i + 1, stabs.size())) {
                if (second.tick() - first.tick() <= KNIFE_PAIR_TICKS
                        && first.target() != null && second.target() != null
                        && !second.target().equals(first.target())
                        && resets.stream().anyMatch(reset ->
                                reset.tick() >= first.tick() && reset.tick() <= second.tick())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 意外之“喜”: as the Scavenger, a knife kill of the Noisemaker. 以清道夫身份用刀杀死大嗓门。 */
    static boolean knifedNoisemaker(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> Rules.reason(kill, KNIFE_STAB)
                && Rules.killedAs(context, kill, SCAVENGER)
                && NOISEMAKER.equals(context.timeline().victimRole(kill)));
    }

    // ---- Morphling 变形者 ----

    /**
     * 真假美猴王: as the Morphling, kill the player I look like, as SparkStrength decides it for its kill reward: my own
     * morph (35 s, ended early if that player disconnects) or, when none is on, a morph reagent triggered on myself
     * (60 s).
     * 以变形者身份杀死我当时伪装成的玩家，判定与 SparkStrength 的击杀奖励相同：我自身的变形（35 秒，被伪装者断线会提前结束），
     * 没有变形时则看对自己触发的变形试剂（60 秒）。
     */
    static boolean killTheOneIMimic(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> kill.target() != null
                && Rules.killedAs(context, kill, MORPHLING)
                && kill.target().equals(selfDisguiseAt(context, kill)));
    }

    /**
     * 替罪羊: a player wearing a morph-reagent disguise I marked as the Morphling is killed while it lasts (60 s), with
     * both their faction and the killer's 好人 at the death.
     * 我以变形者身份标记的变形试剂伪装生效期间（60 秒），被伪装者被杀死，死亡时其本人与凶手都是好人。
     */
    static boolean civilianKilledInMyDisguise(AchievementContext context) {
        UUID self = context.self();
        return context.timeline().deaths().stream().anyMatch(death -> death.target() != null
                && !death.target().equals(self) && death.actor() != null && !death.actor().equals(death.target())
                && Factions.isCivilian(context.timeline().victimFaction(death))
                && Factions.isCivilian(context.timeline().killerFaction(death))
                && markedByMe(context, reagentDisguiseBefore(context.timeline(), death.target(), death)));
    }

    private static boolean markedByMe(AchievementContext context, ReagentDisguise disguise) {
        return disguise != null && context.self() != null && context.self().equals(disguise.mark().actor())
                && Rules.iWas(context, disguise.mark(), MORPHLING);
    }

    /**
     * Who I looked like at {@code event}: my latest morph within 35 s (unless that player disconnected since), else an
     * active morph reagent on myself; null when neither.
     * {@code event} 时我伪装成谁：35 秒内我最近一次变形（期间那名玩家断线则不算），否则看我身上生效的变形试剂；都没有时为 null。
     */
    private static UUID selfDisguiseAt(AchievementContext context, MatchEvent event) {
        MatchEvent morph = null;
        for (MatchEvent candidate : Rules.mySkillUses(context, MORPHLING)) {
            if (candidate.tick() <= event.tick() && MORPH_ACTION.equals(candidate.value("action", MORPH_ACTION))) {
                morph = candidate;
            }
        }
        if (morph != null && event.tick() - morph.tick() < MORPH_TICKS) {
            UUID disguise = morph.uuid("disguise_as") != null ? morph.uuid("disguise_as") : morph.target();
            int from = morph.tick();
            boolean left = disguise == null || context.timeline().events(MatchEvent.PLAYER_LEAVE).stream()
                    .anyMatch(leave -> disguise.equals(leave.actor())
                            && leave.tick() >= from && leave.tick() <= event.tick());
            if (!left) {
                return disguise;
            }
        }
        ReagentDisguise reagent = reagentDisguiseBefore(context.timeline(), context.self(), event);
        return reagent == null ? null : reagent.sample();
    }

    /**
     * The morph-reagent disguise {@code player} wore just before {@code event}, or null: the latest mark on them was
     * triggered no more than 60 s earlier, and since then they were not marked again, the disguise did not end and they
     * did not die.
     * {@code event} 发生前一刻 {@code player} 身上的变形试剂伪装，没有则为 null：对其最近一次标记在 60 秒内被触发，且此后没有被
     * 再次标记、伪装没有结束、本人也没有死亡。
     */
    private static ReagentDisguise reagentDisguiseBefore(MatchTimeline timeline, UUID player, MatchEvent event) {
        if (player == null) {
            return null;
        }
        MatchEvent mark = null;
        MatchEvent trigger = null;
        for (MatchEvent earlier : timeline.events()) {
            if (earlier == event) {
                break;
            }
            if (earlier.isGlobal(MORPH_REAGENT_MARKED) && player.equals(earlier.target())) {
                // A new mark replaces an active disguise and waits for the device again. 新标记会取代生效中的伪装，并重新等待遥控器。
                mark = earlier;
                trigger = null;
            } else if (earlier.isGlobal(MORPH_MARK_TRIGGERED) && player.equals(earlier.actor())) {
                trigger = mark == null ? null : earlier;
            } else if (earlier.isGlobal(MORPH_MARK_ENDED) && player.equals(earlier.actor())
                    || earlier.is(MatchEvent.DEATH) && player.equals(earlier.target())) {
                mark = null;
                trigger = null;
            }
        }
        if (trigger == null || event.tick() - trigger.tick() >= REAGENT_TICKS) {
            return null;
        }
        UUID sample = trigger.uuid("sample") != null ? trigger.uuid("sample") : mark.uuid("sample");
        return new ReagentDisguise(mark, sample);
    }

    /** An active morph-reagent disguise: who marked it and whom it shows. 生效中的变形试剂伪装：谁标记的、伪装成谁。 */
    private record ReagentDisguise(MatchEvent mark, UUID sample) {
    }

    // ---- Phantom 幽灵 ----

    /**
     * 隐形杀手: a player I made invisible with the backpack as the Phantom kills 2 players whose known faction is not
     * killer, before that invisibility ends (its end record, at most 30 s).
     * 我以幽灵身份用背包隐身的玩家，在隐身结束（结束记录，最多 30 秒）之前击杀 2 名阵营已知且不是杀手的玩家。
     */
    static boolean invisibleAllyKillsTwo(AchievementContext context) {
        UUID self = context.self();
        List<MatchEvent> events = context.timeline().events();
        for (int i = 0; i < events.size(); i++) {
            MatchEvent start = events.get(i);
            UUID invisible = start.target();
            if (!start.isGlobal(PHANTOM_BACKPACK_STARTED) || self == null || !self.equals(start.actor())
                    || invisible == null || invisible.equals(self) || !Rules.iWas(context, start, PHANTOM)) {
                continue;
            }
            int end = start.tick() + INVISIBILITY_TICKS;
            for (MatchEvent later : events.subList(i + 1, events.size())) {
                if (later.isGlobal(PHANTOM_BACKPACK_ENDED) && invisible.equals(later.actor())) {
                    end = Math.min(end, later.tick());
                    break;
                }
            }
            int from = start.tick();
            int to = end;
            long kills = context.timeline().killsBy(invisible).stream()
                    .filter(kill -> kill.tick() >= from && kill.tick() <= to && victimNotKiller(context, kill))
                    .map(MatchEvent::target)
                    .distinct()
                    .count();
            if (kills >= INVISIBLE_KILLS) {
                return true;
            }
        }
        return false;
    }

    /**
     * 皇帝的新衣: as the Phantom, die within the 30 s of my own invisibility skill. Other sources of invisibility do not
     * count; disconnects are not deaths.
     * 以幽灵身份在自身隐身技能的 30 秒内死亡。其他来源的隐身不算；断线不算死亡。
     */
    static boolean diedWhileInvisible(AchievementContext context) {
        UUID self = context.self();
        List<MatchEvent> casts = Rules.mySkillUses(context, PHANTOM).stream()
                .filter(cast -> INVISIBLE_ACTION.equals(cast.value("action", INVISIBLE_ACTION))
                        && Rules.iWas(context, cast, PHANTOM))
                .toList();
        return context.timeline().eventsOn(MatchEvent.DEATH, self).stream().anyMatch(death -> !death.isDisconnect()
                && PHANTOM.equals(context.timeline().victimRole(death))
                && casts.stream().anyMatch(cast -> death.tick() >= cast.tick()
                        && death.tick() - cast.tick() < INVISIBILITY_TICKS));
    }

    // ---- Silencer 静语者 ----

    /**
     * 杀人灭口: as the Silencer, kill a player within the 60 s of my silence on them.
     * 以静语者身份，在我对某名玩家的沉默生效的 60 秒内亲手杀死他。
     */
    static boolean killTheSilenced(AchievementContext context) {
        List<MatchEvent> silences = Rules.mySkillUses(context, SILENCER).stream()
                .filter(silence -> silence.target() != null
                        && SILENCE_ACTION.equals(silence.value("action", SILENCE_ACTION))
                        && Rules.iWas(context, silence, SILENCER))
                .toList();
        return Rules.myKills(context, kill -> Rules.killedAs(context, kill, SILENCER)).stream()
                .anyMatch(kill -> silences.stream().anyMatch(silence -> silence.target().equals(kill.target())
                        && kill.tick() >= silence.tick() && kill.tick() <= silence.tick() + SILENCE_TICKS));
    }

    /**
     * 此时无声胜有声: 5 kills as the Silencer in one of my psycho windows. A Silencer's psycho is the silent one bought
     * in its shop; the record does not tell psycho kinds apart.
     * 以静语者身份在一次疯魔中击杀 5 人。静语者的疯魔就是其商店里的静默疯魔；记录本身不区分疯魔种类。
     */
    static boolean silentPsychoQuintuple(AchievementContext context) {
        return Rules.mostInOneWindow(
                Rules.myKills(context, kill -> Rules.killedAs(context, kill, SILENCER)),
                context.timeline().psychoWindows(context.self())) >= SILENT_PSYCHO_KILLS;
    }

    // ---- Assassin 刺客 ----

    /**
     * 百发百中: as the Assassin, 3 players assassinated in one round (a guess that killed, or that ended my returning
     * Vendetta) and no wrong guess (a guess with {@code correct} false, or my misfire death); needs the complete
     * record.
     * 以刺客身份一局内刺杀 3 名玩家（猜中并致死，或终结回来复仇的仇杀客），且没有猜错（{@code correct} 为假的猜测，或我的走火死亡）；
     * 需要完整记录。
     */
    static boolean threeAssassinationsNoMiss(AchievementContext context) {
        MatchTimeline timeline = context.timeline();
        if (!timeline.isComplete()) {
            return false;
        }
        boolean missed = Rules.mySkillUses(context, ASSASSIN).stream()
                .anyMatch(guess -> guess.has("correct") && !guess.bool("correct"))
                || timeline.eventsOn(MatchEvent.DEATH, context.self()).stream()
                        .anyMatch(death -> Rules.reason(death, ASSASSIN_MISFIRE));
        if (missed) {
            return false;
        }
        Set<UUID> assassinated = new HashSet<>();
        Rules.myKills(context, kill -> Rules.reason(kill, ASSASSINATED) && Rules.killedAs(context, kill, ASSASSIN))
                .forEach(kill -> addIfPresent(assassinated, kill.target()));
        assassinatedVendettas(context).forEach(terminal -> addIfPresent(assassinated, terminal.target()));
        return assassinated.size() >= FLAWLESS_ASSASSINATIONS;
    }

    /**
     * 你已经死了: as the Assassin, a correct guess on the Vendetta who came back for me. It never writes a death: it is
     * a {@code sparkwitch:vendetta_terminal} with me as actor and the assassination as {@code death_reason} (only the
     * bound killer can end their own Vendetta this way).
     * 以刺客身份猜中回来向我复仇的仇杀客。这不会写下死亡记录，而是一条 {@code sparkwitch:vendetta_terminal}，执行者是我，
     * {@code death_reason} 为刺杀（只有被绑定的凶手才能这样终结自己的仇杀客）。
     */
    static boolean assassinatedReturningVendetta(AchievementContext context) {
        return !assassinatedVendettas(context).isEmpty();
    }

    private static List<MatchEvent> assassinatedVendettas(AchievementContext context) {
        return Rules.myGlobalEvents(context, KillRules.VENDETTA_TERMINAL).stream()
                .filter(terminal -> Rules.reason(terminal, ASSASSINATED) && Rules.iWas(context, terminal, ASSASSIN))
                .toList();
    }

    // ---- Bandit 强盗 ----

    /**
     * 百步穿杨: as the Bandit, a throwing-axe kill with the victim 25+ blocks from me at the kill (feet to feet, so where
     * I stand when the axe lands, not where I threw it).
     * 以强盗身份用飞斧击杀，击杀时死者与我相距 25 格及以上（脚到脚，取飞斧命中时我所在的位置，而非出手的位置）。
     */
    static boolean longAxeKill(AchievementContext context) {
        return context.kills().stream().anyMatch(kill -> Rules.reason(kill, THROWING_AXE)
                && Rules.killedAs(context, kill, BANDIT)
                && kill.doubleValue("distance", 0) >= HUNDRED_PACES_BLOCKS);
    }

    // ---- Last Escape 绝处逢生 ----

    /**
     * 大难不死: Last Escape showed up among my owner-visible traits. It stays hidden from its holder until it triggers,
     * and only an original killer role without Conscience or Impostor can hold or trigger it, so seeing it means I
     * triggered it as a killer.
     * 绝处逢生出现在我自身可见的词条里。它在触发前对持有者隐藏，且只有不带良知或内鬼的原生杀手身份才能持有和触发，
     * 所以能看到它就说明我以杀手身份触发了它。
     */
    static boolean triggeredLastEscape(AchievementContext context) {
        return context.hadTrait(LAST_ESCAPE);
    }

    // ---- Shared 共用 ----

    /**
     * The victim's faction at the death is known and is not killer (好人, neutrals and witches all count).
     * 死者死亡时的阵营已知且不是杀手（好人、中立、魔女都算）。
     */
    private static boolean victimNotKiller(AchievementContext context, MatchEvent death) {
        String faction = context.timeline().victimFaction(death);
        return (Factions.isCivilian(faction) || Factions.isNonCivilian(faction)) && !Factions.KILLER.equals(faction);
    }

    private static void addIfPresent(Set<UUID> players, UUID player) {
        if (player != null) {
            players.add(player);
        }
    }
}
