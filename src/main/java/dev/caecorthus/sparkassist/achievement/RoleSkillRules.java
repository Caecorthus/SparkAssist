package dev.caecorthus.sparkassist.achievement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Role skills and items (and the killer team purse), from the events the server mods write when they really happen.
 * "As role X" is my role at that event.
 * 身份技能与道具（以及杀手团队经济），依据服务端模组在其真正生效时写下的事件。“以 X 身份”指该事件发生时我的身份。
 */
final class RoleSkillRules {
    static final String REVOLVER = "wathe:revolver";
    static final String SABOTEUR = "sparkwitch:saboteur";
    static final String VULTURE = "noellesroles:vulture";
    static final String WAITER = "noellesroles:waiter";
    static final String POISONER = "noellesroles:poisoner";
    static final String PROFESSOR = "noellesroles:professor";
    static final String BARTENDER = "noellesroles:bartender";
    static final String CORONER = "noellesroles:coroner";
    static final String SURVIVAL_MASTER = "noellesroles:survival_master";

    static final String GUARDIAN_SKILL = "sparkwitch:guardian";
    static final String WAITER_SKILL = "noellesroles:waiter";
    static final String VENDETTA_KNIFE = "sparkwitch:vendetta_knife";
    static final String IRON_MAN_VIAL = "noellesroles:iron_man_vial";
    static final String BASE_SPIRIT = "noellesroles:base_spirit";
    static final String GHOSTFLAME_BITTERS = "ghostflame_bitters";
    static final String EMBER_SUGAR = "ember_sugar";
    /** A base spirit set down on a plate rather than drunk. 基酒被放上酒盘而不是喝下。 */
    static final String PLACE_ACTION = "place";
    static final String BLACKOUT_ENTRY = "blackout";
    static final List<String> MAGIC_ENTRIES = List.of(
            "sparkwitch_obscure", "sparkwitch_blindness", "sparkwitch_fear", "sparkwitch_heaviness");

    static final String WIND_SPIRIT_FALL = "sparkwitch:wind_spirit_fall";
    static final String SABOTAGE = "sparkwitch:sabotage";
    static final String CURSE = "sparkwitch:curse";
    static final String BLACK_RAVEN_PERCEIVED = "sparkwitch:black_raven_perceived";
    static final String HOLY_FLASH_BURST = "sparkwitch:holy_flash_burst";
    static final String SKATEBOARD_RIDE_STARTED = "sparkstrength:skateboard_ride_started";
    static final String TOXICOLOGIST_BLUE = "sparkstrength:toxicologist_blue";
    static final String DETECTIVE_INTERROGATE = "sparkstrength:detective_interrogate";
    static final String TAOTIE_VOLLEY = "sparkstrength:taotie_volley";
    static final String CORONER_DISGUISE = "sparkstrength:coroner_disguise";
    static final String SPIRIT_POSSESSION_STARTED = "sparkstrength:spirit_possession_started";
    static final String IRON_MAN_ACTIVATED = "iron_man_activated";
    static final String DEMOCRACY_SHIELD = "sparkstrength:democracy_shield";
    static final String TEAM_CONTRIBUTION = "sparkstrength:team_contribution";
    static final String SURVIVAL_MOMENT_START = "noellesroles:survival_moment_start";
    static final String SURVIVAL_MOMENT_END = "noellesroles:survival_moment_end";
    /** Wathe's win status when the passengers win. 乘客获胜时 Wathe 的胜利状态。 */
    static final String PASSENGERS = "PASSENGERS";

    /** 10 seconds. 10 秒。 */
    static final int SHORT_SPAN_TICKS = 200;
    static final int TEAM_COINS = 200;
    /**
     * The Survival Moment countdown, 120 s, for a start without {@code duration_ticks}.
     * 生存时刻倒计时 120 秒，用于没有 {@code duration_ticks} 的开始事件。
     */
    static final int SURVIVAL_MOMENT_TICKS = 2400;

    private RoleSkillRules() {
    }

    /** 替补警长: a non-police 好人 picks up a revolver. 非警职的好人捡到左轮手枪。 */
    static boolean civilianPicksUpRevolver(AchievementContext context) {
        return context.myEvents(MatchEvent.ITEM_PICKUP).stream().anyMatch(pickup -> REVOLVER.equals(pickup.value("item"))
                && !RoleGroups.isPolice(context.roleAt(context.self(), pickup))
                && Factions.isCivilian(context.factionAt(context.self(), pickup)));
    }

    /** 你的保护伞: the Guardian Angel shields a player. 守护天使为玩家上盾。 */
    static boolean guardianShield(AchievementContext context) {
        return !Rules.mySkillUses(context, GUARDIAN_SKILL).isEmpty();
    }

    /** 因果报应: the Vendetta's knife took the bound killer. 仇杀客的刀带走了绑定的凶手。 */
    static boolean vendettaKnife(AchievementContext context) {
        return !Rules.myItemUses(context, VENDETTA_KNIFE).isEmpty();
    }

    /** 今天风太大了: a player I knocked with a wind charge fell off the train. 被我的风弹击中的玩家坠下列车。 */
    static boolean windSpiritFall(AchievementContext context) {
        return !context.myEvents(WIND_SPIRIT_FALL).isEmpty();
    }

    /**
     * 你的车该修了: a sabotage that turned off a lamp, or a Blackout bought as the Saboteur.
     * 关掉了灯的破坏，或以破坏者身份购买的停电。
     */
    static boolean sabotagedLights(AchievementContext context) {
        return context.myEvents(SABOTAGE).stream().anyMatch(sabotage -> sabotage.intValue("lamps", 0) >= 1)
                || context.myEvents(MatchEvent.SHOP_PURCHASE).stream().anyMatch(purchase ->
                BLACKOUT_ENTRY.equals(purchase.value("entry_id")) && Rules.iWas(context, purchase, SABOTEUR));
    }

    /** 脑袋晕晕的: a curse that took. 成功施加诅咒。 */
    static boolean cursedSomeone(AchievementContext context) {
        return context.myEvents(CURSE).stream().anyMatch(curse -> curse.intValue("targets", 0) >= 1);
    }

    /** 逮虾户: the Vulture starts a skateboard ride while having Speed. 秃鹫在拥有速度时开始滑板。 */
    static boolean vultureSkatesWithSpeed(AchievementContext context) {
        return Rules.myGlobalEvents(context, SKATEBOARD_RIDE_STARTED).stream().anyMatch(ride ->
                ride.bool("has_speed") && Rules.iWas(context, ride, VULTURE));
    }

    /** 逮到你了: a civilian Black Raven perceives a 非好人. 善良黑羽鸦感知到非好人。 */
    static boolean civilianRavenPerceivesOtherSide(AchievementContext context) {
        return context.myEvents(BLACK_RAVEN_PERCEIVED).stream().anyMatch(perceived ->
                Factions.isCivilian(perceived.value("actor_faction")) && Factions.isNonCivilian(perceived.value("faction")));
    }

    /**
     * 断头饭: as the Waiter, serve the Poisoner something they poisoned themselves: my serve to X and, at the same
     * tick, X poisoned with X as the poisoner, while X was the Poisoner.
     * 以服务员身份把毒师自己下过毒的饮食送给毒师：我对 X 的服务，同一 tick 内 X 中毒且下毒者正是 X，而 X 当时是毒师。
     */
    static boolean servedPoisonerOwnPoison(AchievementContext context) {
        List<MatchEvent> poisonings = context.timeline().events(MatchEvent.PLAYER_POISONED);
        return Rules.mySkillUses(context, WAITER_SKILL).stream().anyMatch(serve -> serve.target() != null
                && Rules.iWas(context, serve, WAITER)
                && poisonings.stream().anyMatch(poisoned -> poisoned.tick() == serve.tick()
                        && serve.target().equals(poisoned.target())
                        && serve.target().equals(poisoned.uuid("poisoner_uuid"))
                        && POISONER.equals(context.roleAt(serve.target(), poisoned))));
    }

    /** 纯瘾大: the Toxicologist turns blue 5 times in a round. 毒理学家一局内 5 次进入蓝毒状态。 */
    static boolean toxicologistBlueFiveTimes(AchievementContext context) {
        return context.myEvents(TOXICOLOGIST_BLUE).size() >= 5;
    }

    /**
     * 我的药剂是有效的: as the Professor, my Iron Man vial blocks two hits for other players. An activation counts when
     * the latest vial given to that player before it was mine, and the player did not die in that same tick
     * (a pierced protection).
     * 以教授身份，我的铁人药剂为其他玩家挡下两次伤害。某次生效计入的条件：在它之前该玩家最近一次获得的药剂是我给的，
     * 且该玩家没有在同一 tick 死亡（护盾被击穿）。
     */
    static boolean ironManBlocksTwice(AchievementContext context) {
        UUID self = context.self();
        Map<UUID, MatchEvent> latestVial = new HashMap<>();
        List<MatchEvent> deaths = context.timeline().deaths();
        int blocks = 0;
        for (MatchEvent event : context.timeline().events()) {
            if (event.is(MatchEvent.ITEM_USE) && IRON_MAN_VIAL.equals(event.value("item")) && event.target() != null) {
                latestVial.put(event.target(), event);
            } else if (event.is(IRON_MAN_ACTIVATED) && event.actor() != null && !event.actor().equals(self)) {
                MatchEvent vial = latestVial.get(event.actor());
                boolean pierced = deaths.stream().anyMatch(death -> death.tick() == event.tick()
                        && event.actor().equals(death.target()));
                if (vial != null && self != null && self.equals(vial.actor()) && Rules.iWas(context, vial, PROFESSOR)
                        && !pierced) {
                    blocks++;
                }
            }
        }
        return blocks >= 2;
    }

    /** 捕风捉影: the Detective questions the real killer. 侦探盘问到真凶。 */
    static boolean interrogatedTheKiller(AchievementContext context) {
        return context.myEvents(DETECTIVE_INTERROGATE).stream().anyMatch(interrogation -> interrogation.bool("is_killer"));
    }

    /**
     * 塔拉之子: as the Bartender, kill a 非好人 within 10 s of drinking 生死相依 (a base spirit with Ghostflame Bitters
     * and Ember Sugar). Placing a glass on a plate is not drinking it.
     * 以酒保身份，在喝下生死相依（含幽焰苦精与火种方糖的基酒）后 10 秒内击杀非好人。把酒放上酒盘不算喝。
     */
    static boolean killSoonAfterLifeAndDeath(AchievementContext context) {
        List<MatchEvent> drinks = Rules.myItemUses(context, BASE_SPIRIT).stream()
                .filter(drink -> !PLACE_ACTION.equals(drink.value("action")) && drink.list("ingredients").containsAll(
                        List.of(GHOSTFLAME_BITTERS, EMBER_SUGAR)))
                .toList();
        return context.kills().stream().anyMatch(kill -> Rules.killedAs(context, kill, BARTENDER)
                && Rules.victimNonCivilian(context, kill)
                && drinks.stream().anyMatch(drink -> kill.tick() >= drink.tick()
                        && kill.tick() <= drink.tick() + SHORT_SPAN_TICKS));
    }

    /** 精确制导: the Taotie fires three heads in one volley. 饕餮一次发射三颗头颅导弹。 */
    static boolean taotieTripleVolley(AchievementContext context) {
        return context.myEvents(TAOTIE_VOLLEY).stream().anyMatch(volley -> volley.intValue("count", 0) >= 3);
    }

    /**
     * 阴霾: as the Grand Witch, buy every magic within 10 s.
     * 以大魔女身份在 10 秒内买下所有魔法。
     */
    static boolean boughtEveryMagicQuickly(AchievementContext context) {
        List<MatchEvent> purchases = context.myEvents(MatchEvent.SHOP_PURCHASE).stream()
                .filter(purchase -> MAGIC_ENTRIES.contains(purchase.value("entry_id", ""))
                        && Rules.iWas(context, purchase, RoleGroups.GRAND_WITCH_ROLE))
                .toList();
        return purchases.stream().anyMatch(first -> purchases.stream()
                .filter(purchase -> purchase.tick() >= first.tick() && purchase.tick() <= first.tick() + SHORT_SPAN_TICKS)
                .map(purchase -> purchase.value("entry_id"))
                .distinct().count() == MAGIC_ENTRIES.size());
    }

    /**
     * 民主之力: the Democracy Shield blocks 3 attacks in a round; two blocks in one tick from one attacker (a double
     * strike) are one attack.
     * 民主之盾一局内挡下 3 次攻击；同一攻击者在同一 tick 的两次格挡（二连击）算一次攻击。
     */
    static boolean democracyShieldBlocksThree(AchievementContext context) {
        Set<String> attacks = new HashSet<>();
        for (MatchEvent block : context.myEvents(MatchEvent.SHIELD_BLOCKED)) {
            if (DEMOCRACY_SHIELD.equals(block.value("source"))) {
                attacks.add(block.tick() + "/" + block.target());
            }
        }
        return attacks.size() >= 3;
    }

    /** 附身: the Spirit Walker possesses a Wraith's view. 灵界行者附身至冤魂的视角。 */
    static boolean possessedWraith(AchievementContext context) {
        return !Rules.myGlobalEvents(context, SPIRIT_POSSESSION_STARTED).isEmpty();
    }

    /** 恩赐: a Holy Flash affects 3 other players. 圣光弹影响 3 名其他玩家。 */
    static boolean holyFlashThree(AchievementContext context) {
        return context.myEvents(HOLY_FLASH_BURST).stream().anyMatch(burst -> burst.intValue("affected_others", 0) >= 3);
    }

    /**
     * 我不是卧底: as the Coroner, kill while my latest disguise is a killer role.
     * 以验尸官身份，在最近一次伪装为杀手身份时击杀。
     */
    static boolean killDisguisedAsKiller(AchievementContext context) {
        UUID self = context.self();
        boolean disguisedAsKiller = false;
        for (MatchEvent event : context.timeline().events()) {
            if (self == null) {
                return false;
            }
            if (event.is(CORONER_DISGUISE) && self.equals(event.actor())) {
                disguisedAsKiller = event.bool("killer");
            } else if (disguisedAsKiller && event.is(MatchEvent.DEATH) && self.equals(event.actor())
                    && !self.equals(event.target()) && Rules.killedAs(context, event, CORONER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 团队至上: my income raised the killer team purse by 200 coins in total this round. Only killer-team members
     * contribute, so no faction check is needed.
     * 本局我的收入共为杀手团队经济增加了 200 金币。只有杀手团队成员能贡献，所以无需检查阵营。
     */
    static boolean contributedToKillerTeam(AchievementContext context) {
        long total = context.myEvents(TEAM_CONTRIBUTION).stream()
                .mapToLong(contribution -> Math.max(0, contribution.intValue("amount", 0)))
                .sum();
        return total >= TEAM_COINS;
    }

    /**
     * 你已疾苦: as the Survival Master, start the Survival Moment and win by its countdown: the passengers won, I never
     * died, the moment was never ended after that start, and the round was decided no earlier than the countdown's
     * end (a plain Wathe {@code PASSENGERS} win, so an earlier wipe of the killers does not count).
     * 以生存大师身份触发生存时刻并靠倒计时获胜：乘客获胜、我从未死亡、该次开始之后生存时刻没有被结束，且胜负判定不早于倒计时结束
     * （这是普通的 Wathe {@code PASSENGERS} 胜利，所以提前消灭杀手不算）。
     */
    static boolean outlastedSurvivalMoment(AchievementContext context) {
        if (!context.won() || !PASSENGERS.equals(context.outcome().winStatus()) || RoundRules.died(context)) {
            return false;
        }
        List<MatchEvent> mine = context.myEvents(MatchEvent.GLOBAL_EVENT);
        for (int i = 0; i < mine.size(); i++) {
            MatchEvent start = mine.get(i);
            if (SURVIVAL_MOMENT_START.equals(start.value("event")) && Rules.iWas(context, start, SURVIVAL_MASTER)
                    && Rules.withValue(mine.subList(i + 1, mine.size()), "event", SURVIVAL_MOMENT_END).isEmpty()
                    && decidedByCountdown(context, start)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the round was decided only once the countdown that began at {@code start} ran out. The round clock and
     * the record clock may differ by {@link RoundFacts#RECORD_TICK_TOLERANCE}.
     * 胜负判定时，{@code start} 开始的倒计时是否已经结束。对局时钟与记录时钟可能相差 {@link RoundFacts#RECORD_TICK_TOLERANCE}。
     */
    private static boolean decidedByCountdown(AchievementContext context, MatchEvent start) {
        long countdownEnd = (long) start.tick() + Math.max(0, start.intValue("duration_ticks", SURVIVAL_MOMENT_TICKS));
        return context.facts().durationTicks() >= countdownEnd - RoundFacts.RECORD_TICK_TOLERANCE;
    }
}
