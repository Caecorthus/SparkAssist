package dev.caecorthus.sparkassist.achievement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Achievements read from the record events SparkWitch writes for them (contract W8-W17): the Ninja's parry, the
 * Poison Apple, the Perfumer's mark, the Prophecy, the Saint's Karma, the Judge's verdict, Bone Setting against a trap
 * and the Magician's puppet kills. Servers without that SparkWitch never write these events, so those rules simply find
 * nothing. Two rules need none of them: Emma's quick revolver pair (death records) and the Seeker's broken device (an
 * older SparkWitch event). "As role X" is my real role at that event from the match record, so a Black Raven acting as
 * X never counts.
 * 依据 SparkWitch 专为成就写下的记录事件（契约 W8-W17）判断的成就：忍者格挡、毒苹果、调香师标记、先知预言、圣徒业障、法官判决、
 * 正骨挡下捕兽夹以及魔术师皮套击杀。没有该版本 SparkWitch 的服务器不会写这些事件，因此那些规则只是什么也找不到。另有两条规则不需要
 * 这些事件：艾玛左轮连杀（死亡记录）与搜寻者设备被打坏（较早已有的 SparkWitch 事件）。“以 X 身份”指对局记录中该事件发生时我的真实身份，
 * 因此扮演 X 的黑羽鸦永远不算。
 */
final class WitchRecordRules {
    static final String PROPHET = "sparkwitch:prophet";
    static final String JUDGE = "sparkwitch:judge";

    /** W8: actor = the Ninja, target = the attacker whose killing blow the parry stopped. W8：忍者格挡挡下致命一击。 */
    static final String NINJA_PARRY = "sparkwitch:ninja_parry";
    /**
     * W9: target took a serving from a platter actor armed with a Poison Apple; {@code poisoned} on the poisoned
     * (second) serving.
     * W9：target 从 actor 布置了毒苹果的餐盘上取走一份；被下毒的那一份（第二份）带 {@code poisoned}。
     */
    static final String POISON_APPLE_TAKE = "sparkwitch:poison_apple_take";
    /** W10: actor = the Perfumer, target = the player Perfume Essence marked. W10：调香师用香精标记了 target。 */
    static final String PERFUME_MARK = "sparkwitch:perfume_mark";
    /**
     * W11: actor = the Prophet, target = the dead; {@code correct}, {@code attempt} (from 1 per death), and
     * {@code killer} / {@code killer_faction} when a correct guess revealed someone.
     * W11：actor 为先知，target 为死者；{@code correct}、{@code attempt}（每次死亡从 1 起），猜中并揭示了某人时带
     * {@code killer} / {@code killer_faction}。
     */
    static final String PROPHECY = "sparkwitch:prophecy";
    /** W12: actor = the dead Saint, target = the killer who took Karma. W12：死去的圣徒让 target 背上业障。 */
    static final String SAINT_KARMA = "sparkwitch:saint_karma";
    /** W13: actor = the Judge, target = the defendant; {@code sentenced}, {@code duration}. W13：法官的一次审判。 */
    static final String JUDGE_VERDICT = "sparkwitch:judge_verdict";
    /**
     * A global event (not one of W8-W17) when a Seeker device breaks: {@code owner} = the Seeker; actor and
     * {@code breaker} = the player who broke it, only when a player did.
     * 搜寻者设备被打坏时的全局事件（不属于 W8-W17）：{@code owner} 为搜寻者；actor 与 {@code breaker} 为打坏它的玩家（仅当由玩家造成时）。
     */
    static final String SEEKER_DEVICE_BROKEN = "sparkwitch:seeker_device_broken";
    /** W17: actor = the Magician, target = the puppet's victim; {@code disguise_dead}. W17：魔术师皮套击杀。 */
    static final String MAGICIAN_PUPPET_KILL = "sparkwitch:magician_puppet_kill";
    /** W16, on {@code sparkwitch:hunter_trap_triggered}: an active Bone Setting absorbed the fracture. W16。 */
    static final String BONE_SETTING_ABSORBED = "bone_setting_absorbed";
    /** The {@code action} of an Orthopedist skill use that applied Bone Setting. 骨科大夫施加正骨时技能记录的 {@code action}。 */
    static final String BONE_SETTING_ACTION = "bone_setting";
    static final String FOOD_POISON = "wathe:food";

    /** 3 seconds. 3 秒。 */
    static final int WITCH_TRIAL_TICKS = 60;
    /** 10 seconds. 10 秒。 */
    static final int ENTRAPMENT_TICKS = 200;

    private WitchRecordRules() {
    }

    // ---- Killer side 杀手方 ----

    /**
     * 见切: as the Ninja, my parry stopped a killing blow ({@code sparkwitch:ninja_parry}; never written for a
     * Ceremonial Sword blow, which pierces the parry).
     * 以忍者身份用格挡挡下一次致命攻击（{@code sparkwitch:ninja_parry}；仪礼剑会穿透格挡，绝不会写入）。
     */
    static boolean ninjaParry(AchievementContext context) {
        return context.myEvents(NINJA_PARRY).stream()
                .anyMatch(parry -> Rules.iWas(context, parry, WitchKillerRules.NINJA));
    }

    /**
     * 白雪公主: as the Witch Maiden, another player takes the poisoned serving of my Poison Apple
     * ({@code sparkwitch:poison_apple_take} with {@code poisoned}) and later dies of poison, their latest poisoning
     * before that death being food poisoned by me after the take. A serving passed on and eaten by someone else is not
     * counted: the record cannot tie that eater to the apple.
     * 以巫女身份让另一名玩家取走我毒苹果被下毒的那一份（带 {@code poisoned} 的 {@code sparkwitch:poison_apple_take}），此人之后毒发身亡，
     * 且死亡前最近一次中毒是在取走之后、由我下毒的食物。转手给别人吃下的那一份不算：记录无法把吃下的人和毒苹果对应起来。
     */
    static boolean poisonAppleKill(AchievementContext context) {
        UUID self = context.self();
        if (self == null) {
            return false;
        }
        return context.myEvents(POISON_APPLE_TAKE).stream().anyMatch(take -> take.bool("poisoned")
                && take.target() != null && !self.equals(take.target())
                && Rules.iWas(context, take, WitchKillerRules.WITCH_MAIDEN)
                && diesOfMyFoodPoisonAfter(context.timeline(), take.target(), take, self));
    }

    /**
     * 借尸还魂: as the Magician, my playback puppet disguised as a player who was already dead kills someone
     * ({@code sparkwitch:magician_puppet_kill} with {@code disguise_dead}; written only when the kill took, with the
     * knife, the bat or the revolver).
     * 以魔术师身份让伪装成已死亡玩家的回放皮套击杀一名玩家（带 {@code disguise_dead} 的 {@code sparkwitch:magician_puppet_kill}；
     * 只在刀、球棒或左轮的击杀生效时写入）。
     */
    static boolean puppetAsTheDead(AchievementContext context) {
        return context.myEvents(MAGICIAN_PUPPET_KILL).stream().anyMatch(kill -> kill.bool("disguise_dead")
                && kill.target() != null && Rules.iWas(context, kill, WitchKillerRules.MAGICIAN));
    }

    // ---- Civilian side 好人方 ----

    /**
     * 闻香识凶手: as the Perfumer, mark a 非好人 with Perfume Essence ({@code sparkwitch:perfume_mark}; the faction by
     * {@link StrengthRecordRules#factionAround}), and they later kill someone while still marked, which turns the mark
     * into the blood scent: their first kill after the mark, with no death of theirs in between (a death clears the
     * mark) and me still the Perfumer at that kill (only a Perfumer's marks turn bloody, dead or alive).
     * 以调香师身份用香精标记一名非好人（{@code sparkwitch:perfume_mark}；阵营按 {@link StrengthRecordRules#factionAround} 判断），
     * 对方之后在仍被标记时行凶，标记随之变为血腥气味：取标记之后其第一次击杀，期间对方没有死亡（死亡会清除标记），且该次击杀时我仍是
     * 调香师（无论生死，只有调香师的标记会染上血腥气味）。
     */
    static boolean markedKillerSmellsOfBlood(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        return context.myEvents(PERFUME_MARK).stream().anyMatch(mark -> mark.target() != null
                && !mark.target().equals(self)
                && Rules.iWas(context, mark, WitchCivilianRules.PERFUMER)
                && Factions.isNonCivilian(StrengthRecordRules.factionAround(timeline, mark.target(), mark))
                && nextKillWhileMarked(context, mark.target(), mark));
    }

    /**
     * 上医治未病: as the Orthopedist, apply Bone Setting to a player (an Orthopedist skill use with {@code action}
     * bone_setting), and that Bone Setting absorbs the fracture of a hunter trap they step on
     * ({@code sparkwitch:hunter_trap_triggered} with {@code bone_setting_absorbed}). Bone Setting cannot be applied again
     * while active, so the absorbing one is the latest applied to that player before the trap.
     * 以骨科大夫身份为一名玩家正骨（{@code action} 为 bone_setting 的骨科大夫技能记录），对方踩中捕兽夹时这次正骨吸收了骨折
     * （带 {@code bone_setting_absorbed} 的 {@code sparkwitch:hunter_trap_triggered}）。正骨生效期间不能再次施加，所以吸收骨折的就是
     * 踩夹之前最近一次施加给此人的正骨。
     */
    static boolean boneSettingAbsorbedTrap(AchievementContext context) {
        UUID self = context.self();
        if (self == null) {
            return false;
        }
        Map<UUID, MatchEvent> latestSetting = new HashMap<>();
        for (MatchEvent event : context.timeline().events()) {
            if (event.is(MatchEvent.SKILL_USE) && event.target() != null
                    && WitchCivilianRules.ORTHOPEDIST_SKILL.equals(event.value("skill"))
                    && BONE_SETTING_ACTION.equals(event.value("action"))) {
                latestSetting.put(event.target(), event);
            } else if (event.isGlobal(WitchKillerRules.HUNTER_TRAP_TRIGGERED) && event.target() != null
                    && event.bool(BONE_SETTING_ABSORBED)) {
                MatchEvent setting = latestSetting.get(event.target());
                if (setting != null && self.equals(setting.actor())
                        && Rules.iWas(context, setting, WitchCivilianRules.ORTHOPEDIST)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 一眼丁真: as the Prophet, my first guess on a death ({@code attempt} 1) is correct and reveals a responsible
     * player ({@code killer}) who is a 非好人 by the {@code killer_faction} SparkWitch wrote at that moment (else by
     * {@link StrengthRecordRules#factionAround}).
     * 以先知身份对一次死亡的第一次猜测（{@code attempt} 为 1）就猜中，并揭示出一名责任人（{@code killer}），按 SparkWitch 当时写下的
     * {@code killer_faction}（没有时按 {@link StrengthRecordRules#factionAround}）判断为非好人。
     */
    static boolean firstProphecyNamesNonCivilian(AchievementContext context) {
        MatchTimeline timeline = context.timeline();
        return context.myEvents(PROPHECY).stream().anyMatch(prophecy -> prophecy.bool("correct")
                && prophecy.intValue("attempt", 0) == 1
                && prophecy.uuid("killer") != null
                && Rules.iWas(context, prophecy, PROPHET)
                && Factions.isNonCivilian(prophecy.has("killer_faction") ? prophecy.value("killer_faction")
                        : StrengthRecordRules.factionAround(timeline, prophecy.uuid("killer"), prophecy)));
    }

    /**
     * 我不入地狱，谁入地狱: as the Saint, die with Hellfire active and my killer takes Karma
     * ({@code sparkwitch:saint_karma}; written only for a killer outside the civilian side who passed the Karma
     * immunity check).
     * 以圣徒身份在业火中被杀死，凶手背上业障（{@code sparkwitch:saint_karma}；只为通过业障免疫检查、不在好人一方的凶手写入）。
     */
    static boolean karmaOnMyKiller(AchievementContext context) {
        UUID self = context.self();
        return context.myEvents(SAINT_KARMA).stream().anyMatch(karma -> karma.target() != null
                && !karma.target().equals(self) && Rules.iWas(context, karma, WitchCivilianRules.SAINT));
    }

    /**
     * 法外狂徒张三: as the Judge, sentence a player ({@code sparkwitch:judge_verdict} with {@code sentenced}), then,
     * still the Judge, kill them myself after the verdict and at most {@code duration} ticks after it, they being a
     * 非好人 at that death.
     * 以法官身份判刑一名玩家（带 {@code sentenced} 的 {@code sparkwitch:judge_verdict}），之后仍以法官身份在判决之后、不晚于判决后
     * {@code duration} tick 亲手将其击杀，对方死亡时为非好人。
     */
    static boolean killedWhileSentenced(AchievementContext context) {
        MatchTimeline timeline = context.timeline();
        return context.myEvents(JUDGE_VERDICT).stream().anyMatch(verdict -> verdict.bool("sentenced")
                && verdict.target() != null && Rules.iWas(context, verdict, JUDGE)
                && context.kills().stream().anyMatch(kill -> verdict.target().equals(kill.target())
                        && Rules.killedAs(context, kill, JUDGE) && Rules.victimNonCivilian(context, kill)
                        && StrengthRecordRules.comesAfter(timeline, kill, verdict)
                        && kill.tick() - verdict.tick() <= verdict.intValue("duration", 0)));
    }

    /**
     * 魔女裁判: as Emma, two revolver kills ({@code wathe:gun_shot} with {@code killer_item} {@code wathe:revolver}) of
     * two different players who were 非好人 at death, at most 3 s apart. Read from the death records alone, which say
     * all of it; the hunt reward ({@code sparkwitch:emma_hunt_reward}) is only the usual way to get the revolver back
     * that fast, so it is not required.
     * 以樱羽艾玛身份用左轮（{@code wathe:gun_shot} 且 {@code killer_item} 为 {@code wathe:revolver}）击杀两名不同的、死亡时为非好人的
     * 玩家，两次击杀相隔不超过 3 秒。只看死亡记录就足够判断；猎杀奖励（{@code sparkwitch:emma_hunt_reward}）只是让左轮这么快就绪的常见
     * 途径，因此不作要求。
     */
    static boolean emmaRevolverPair(AchievementContext context) {
        List<MatchEvent> kills = Rules.myKills(context, kill -> kill.target() != null
                && Rules.reason(kill, NoellesCivilianRules.GUN_SHOT)
                && NoellesCivilianRules.REVOLVER.equals(kill.value("killer_item"))
                && Rules.killedAs(context, kill, WitchCivilianRules.EMMA)
                && Rules.victimNonCivilian(context, kill));
        for (int i = 0; i < kills.size(); i++) {
            for (int j = i + 1; j < kills.size(); j++) {
                if (!kills.get(i).target().equals(kills.get(j).target())
                        && Math.abs(kills.get(j).tick() - kills.get(i).tick()) <= WITCH_TRIAL_TICKS) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 钓鱼执法: as the Seeker, a player breaks my device ({@code sparkwitch:seeker_device_broken} with {@code owner}
     * me; the breaker is its actor, or {@code breaker}) and I kill that player myself, still the Seeker, after the
     * break and within 10 s of it, they being a 非好人 at death. A break with no player behind it (falling off the
     * train, leaving the play area) names no breaker. No tablet is needed: unlike the mark, the break is recorded either
     * way.
     * 以搜寻者身份，一名玩家打坏我的设备（{@code owner} 为我的 {@code sparkwitch:seeker_device_broken}；打坏者为其 actor 或
     * {@code breaker}），我在损坏之后的 10 秒内仍以搜寻者身份亲手击杀此人，对方死亡时为非好人。没有玩家造成的损坏（掉出列车、离开游戏区域）
     * 不记录打坏者。不需要平板：与标记不同，损坏无论如何都会被记录。
     */
    static boolean killedDeviceBreaker(AchievementContext context) {
        UUID self = context.self();
        MatchTimeline timeline = context.timeline();
        if (self == null) {
            return false;
        }
        return timeline.events(MatchEvent.GLOBAL_EVENT).stream().anyMatch(broken -> {
            UUID breaker = broken.actor() != null ? broken.actor() : broken.uuid("breaker");
            return broken.isGlobal(SEEKER_DEVICE_BROKEN) && self.equals(broken.uuid("owner"))
                    && breaker != null && !breaker.equals(self)
                    && Rules.iWas(context, broken, WitchCivilianRules.SEEKER)
                    && context.kills().stream().anyMatch(kill -> breaker.equals(kill.target())
                            && Rules.killedAs(context, kill, WitchCivilianRules.SEEKER)
                            && Rules.victimNonCivilian(context, kill)
                            && StrengthRecordRules.comesAfter(timeline, kill, broken)
                            && kill.tick() - broken.tick() <= ENTRAPMENT_TICKS);
        });
    }

    // ---- Helpers 辅助 ----

    /**
     * Whether {@code player} later dies of poison ({@code wathe:poison}) with their latest poisoning before that death
     * being food ({@code wathe:food}) poisoned by {@code poisoner} after {@code take}. Any death clears the poison
     * (Wathe resets it), so a poisoning before an earlier death of theirs no longer counts.
     * {@code player} 之后是否毒发身亡（{@code wathe:poison}），且死亡前最近一次中毒是在 {@code take} 之后由 {@code poisoner} 下毒的食物
     * （{@code wathe:food}）。任何死亡都会清除中毒（Wathe 会重置），所以此人更早一次死亡之前的中毒不再算数。
     */
    private static boolean diesOfMyFoodPoisonAfter(MatchTimeline timeline, UUID player, MatchEvent take, UUID poisoner) {
        List<MatchEvent> events = timeline.events();
        MatchEvent poisoning = null;
        for (int i = StrengthRecordRules.positionOf(events, take) + 1; i > 0 && i < events.size(); i++) {
            MatchEvent event = events.get(i);
            if (event.is(MatchEvent.PLAYER_POISONED) && player.equals(event.target())) {
                poisoning = event;
            } else if (event.is(MatchEvent.DEATH) && player.equals(event.target())) {
                if (poisoning != null && Rules.reason(event, KillRules.POISON)
                        && FOOD_POISON.equals(poisoning.value("source"))
                        && poisoner.equals(poisoning.has("poisoner_uuid") ? poisoning.uuid("poisoner_uuid")
                                : poisoning.actor())) {
                    return true;
                }
                poisoning = null;
            }
        }
        return false;
    }

    /**
     * Whether {@code marked}'s first kill after {@code mark} comes before any death of theirs and while I am the
     * Perfumer.
     * {@code marked} 在 {@code mark} 之后的第一次击杀是否发生在其任何死亡之前，且当时我是调香师。
     */
    private static boolean nextKillWhileMarked(AchievementContext context, UUID marked, MatchEvent mark) {
        List<MatchEvent> events = context.timeline().events();
        for (int i = StrengthRecordRules.positionOf(events, mark) + 1; i > 0 && i < events.size(); i++) {
            MatchEvent event = events.get(i);
            if (!event.is(MatchEvent.DEATH)) {
                continue;
            }
            if (marked.equals(event.target())) {
                return false;
            }
            if (marked.equals(event.actor())) {
                return WitchCivilianRules.PERFUMER.equals(context.roleAt(context.self(), event));
            }
        }
        return false;
    }
}
