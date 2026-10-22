package dev.caecorthus.sparkassist.achievement;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The achievement page drawn as a galaxy. Every achievement has a fixed star and {@link #CENTER} sits alone in the
 * middle. The other stars are grouped by theme into six {@link Arm}s: each arm owns a slice of the disc, sized by how
 * many stars it holds and twisted like a spiral galaxy, with a dark lane between neighbouring arms. Inside its slice a
 * star settles near the stars it links with, whatever its level.
 * <p>
 * Each achievement links to one to three related achievements of the same or a higher level. The page shows every
 * earned achievement and every achievement an earned one links to, the latter with its condition but not its title;
 * the rest stays hidden. The middle star is earned by earning all the others and shows only once earned. Links decide only what is
 * shown: any achievement can be earned at any time.
 * <p>
 * 以银河绘制的成就页。每个成就都有固定的位置，{@link #CENTER} 独自位于正中。其余成就按主题分成六条 {@link Arm}：
 * 每条旋臂占圆盘的一个扇区，大小取决于它有多少成就，并像旋涡星系一样扭转，相邻旋臂之间隔着一道暗带。
 * 在扇区内，成就不论等级，都落在与之相连的成就附近。
 * 每个成就连向一到三个同级或更高级的相关成就。成就页显示所有已达成的成就，以及已达成成就连向的成就（只显示条件，不显示标题）；
 * 其余的保持隐藏。正中的成就要达成其他所有成就才能获得，获得后才会出现。连线只决定显示什么：任何成就随时都能达成。
 */
public final class AchievementGalaxy {
    public static final String CENTER = "tiny_victory";

    // Stars are 26 px wide and kept at least GAP pixels apart. Linked stars of one arm are drawn toward LINK pixels
    // apart, linked stars of two arms toward CROSS_LINK. Each star is given about ROOM more pixels around it and the
    // stars fill PACKING of the disc, which sets the galaxy's size; none comes closer to the middle than HOLE + GAP / 2,
    // leaving room for the middle star and its halo.
    // 成就宽 26 像素，中心至少相距 GAP 像素。同一旋臂里相连的成就趋向相距 LINK 像素，不同旋臂之间相连的趋向相距 CROSS_LINK。
    // 每个成就周围再多留约 ROOM 像素，成就占满圆盘的 PACKING，由此决定银河的大小；离正中都不近于 HOLE + GAP / 2，
    // 给正中的成就和它的光环留出位置。
    private static final double GAP = 34;
    private static final double LINK = 50;
    private static final double CROSS_LINK = 80;
    private static final double ROOM = 14;
    private static final double PACKING = 0.78;
    private static final double HOLE = 62;
    // The dark lane between two arms, and the extra turn of an arm at the middle against the rim, both in radians.
    // 相邻旋臂之间暗带的宽度，以及旋臂在正中处相对外缘多转的角度，单位均为弧度。
    private static final double LANE = Math.toRadians(7);
    private static final double TWIST = 1.1;
    // Rounds of settling the stars along their links (pushing apart the ones too close every SPREAD_EVERY rounds),
    // then at most this many rounds of pushing apart.
    // 让成就沿连线就位的轮数（每 SPREAD_EVERY 轮推开一次太近的成就），以及之后最多推开的轮数。
    private static final int SETTLE_ROUNDS = 500;
    private static final int SPREAD_EVERY = 5;
    private static final int SPREAD_ROUNDS = 400;

    /** Themes, in their order around the galaxy. 主题，按其在银河中的环绕顺序。 */
    public enum Arm {
        FOOD,
        GUARD,
        POLICE,
        NEUTRAL,
        KILLER,
        WITCH
    }

    // Every achievement but the middle one, by arm and level, with the achievements it links to.
    // 除正中外的每个成就，按旋臂和等级排列，以及它连向的成就。
    private static final Map<Arm, List<Map.Entry<String, List<String>>>> LINK_MAP = Map.of(
            Arm.FOOD, List.of(
                    // Level 1 一级
                    entry("hooked_on_food", List.of("hungry_ghost", "perfect_attendance")),
                    entry("chug_chug", List.of("water_ghost", "perfect_attendance")),
                    // Level 2 二级
                    entry("hungry_ghost", List.of("die_full", "pigsy")),
                    entry("water_ghost", List.of("waterlogged", "never_drunk")),
                    entry("hooked_on_blue", List.of("smoke_session", "own_medicine")),
                    entry("smoke_session", List.of("last_meal", "last_supper")),
                    entry("perfect_attendance", List.of("full_banquet", "model_worker")),
                    entry("snow_white", List.of("last_supper")),
                    // Level 3 三级
                    entry("die_full", List.of("last_meal", "last_gasp")),
                    entry("waterlogged", List.of("last_meal", "never_skunked")),
                    entry("never_skunked", List.of("sea_king")),
                    // Level 4 四级
                    entry("last_meal", List.of("prized_pupil", "own_medicine")),
                    // Level 5 五级
                    entry("never_drunk", List.of(CENTER)),
                    entry("sea_king", List.of(CENTER)),
                    entry("pigsy", List.of(CENTER)),
                    entry("full_banquet", List.of(CENTER)),
                    entry("last_supper", List.of(CENTER)),
                    entry("own_medicine", List.of(CENTER)),
                    entry("last_gasp", List.of(CENTER))),
            Arm.GUARD, List.of(
                    // Level 1 一级
                    entry("survivor", List.of("outlasted", "strong_grow_weak", "fortune")),
                    entry("welcome_aboard", List.of("restless_ghost", "spring_rush")),
                    entry("restless_ghost", List.of("possession")),
                    entry("possession", List.of("got_you", "blessing")),
                    entry("your_umbrella", List.of("it_works", "democracy", "prevention")),
                    entry("friendly_fire", List.of("farmer_and_snake")),
                    entry("kabi", List.of("dumb_ways_to_die", "mental_collapse")),
                    entry("night_owl", List.of("model_worker")),
                    entry("save_load", List.of("corrupted_save", "time_returned")),
                    entry("wolf_in_the_house", List.of("on_call")),
                    entry("self_flash", List.of("blessing", "martyr")),
                    entry("blew_myself_up", List.of("through_the_crossfire")),
                    entry("power_is_back", List.of("night_light", "on_call", "trap_the_turtle")),
                    // Level 2 二级
                    entry("it_works", List.of("sons_of_tara", "prized_pupil")),
                    entry("got_you", List.of("grasping_at_wind")),
                    entry("democracy", List.of("savior", "through_the_crossfire")),
                    entry("strong_grow_weak", List.of("weak_grow_strong", "buzzer_beater")),
                    entry("blessing", List.of("sons_of_tara")),
                    entry("fortune", List.of("outlasted", "big_spender")),
                    entry("outlasted", List.of("weak_grow_strong", "hold_out")),
                    entry("hold_out", List.of("clock_out_early", "defy_fate")),
                    entry("spring_rush", List.of("win_streak")),
                    entry("buzzer_beater", List.of("final_duel", "lone_hero")),
                    entry("corrupted_save", List.of("dumb_ways_to_die")),
                    entry("night_light", List.of("hold_out")),
                    entry("on_call", List.of("clock_out_early")),
                    entry("mental_collapse", List.of("dumb_ways_to_die")),
                    entry("prevention", List.of("farmer_and_snake")),
                    entry("farmer_and_snake", List.of("dumb_ways_to_die")),
                    entry("martyr", List.of("sons_of_tara")),
                    // Level 3 三级
                    entry("sons_of_tara", List.of("savior")),
                    entry("weak_grow_strong", List.of("savior", "snowball")),
                    entry("lone_hero", List.of("defy_fate")),
                    entry("model_worker", List.of("nine_nine_six")),
                    entry("big_spender", List.of("clock_out_early")),
                    entry("final_duel", List.of("defy_fate")),
                    entry("win_streak", List.of("flawless_run")),
                    entry("time_returned", List.of("clock_out_early")),
                    // Level 4 四级
                    entry("savior", List.of("prized_pupil", "snowball_plus", "high_noon")),
                    entry("dumb_ways_to_die", List.of("many_faces")),
                    entry("clock_out_early", List.of("nine_nine_six")),
                    // Level 5 五级
                    entry("prized_pupil", List.of(CENTER)),
                    entry("many_faces", List.of(CENTER)),
                    entry("nine_nine_six", List.of(CENTER)),
                    entry("through_the_crossfire", List.of(CENTER)),
                    entry("flawless_run", List.of(CENTER)),
                    entry("defy_fate", List.of(CENTER))),
            Arm.POLICE, List.of(
                    // Level 1 一级
                    entry("justice_served", List.of("point_blank", "within_seven_steps", "red_handed")),
                    entry("deputy_sheriff", List.of("long_shot", "within_seven_steps", "passing_the_torch")),
                    entry("power_of_the_people", List.of("quick_justice", "blind_confidence", "swordfish_strike")),
                    entry("perfect_cover", List.of("not_undercover", "grasping_at_wind", "turncoat")),
                    entry("karma", List.of("depressed", "grasping_at_wind", "wheel_of_fortune")),
                    entry("doomed_path", List.of("not_prepared", "within_seven_steps", "close_your_eyes")),
                    entry("mutual_destruction", List.of("debt_has_owner")),
                    entry("backfire", List.of("witch_trial")),
                    entry("number_unavailable", List.of("exclusive_interview")),
                    // Level 2 二级
                    entry("point_blank", List.of("deadly_calibration")),
                    entry("grasping_at_wind", List.of("savior", "one_truth", "one_glance")),
                    entry("within_seven_steps", List.of("deadly_calibration")),
                    entry("quick_justice", List.of("snowball", "hexagon_warrior")),
                    entry("long_shot", List.of("deadly_calibration")),
                    entry("depressed", List.of("savior", "gg_bond")),
                    entry("blind_confidence", List.of("weak_grow_strong", "blind_snipe")),
                    entry("not_undercover", List.of("deadly_calibration", "flowerpot_swordsman")),
                    entry("red_handed", List.of("quick_justice", "passing_the_torch")),
                    entry("debt_has_owner", List.of("last_scream")),
                    entry("last_scream", List.of("dimmed_light")),
                    entry("dimmed_light", List.of("exclusive_interview", "scent_of_blood")),
                    entry("trap_the_turtle", List.of("safe_arrival")),
                    entry("eyes_of_the_masses", List.of("no_wasted_bullet", "outlaw_zhang_san")),
                    entry("close_your_eyes", List.of("old_soldiers", "night_demon")),
                    entry("scent_of_blood", List.of("cooling_oil_assist")),
                    entry("cooling_oil_assist", List.of("numb")),
                    entry("one_glance", List.of("exclusive_interview")),
                    entry("wheel_of_fortune", List.of("one_glance")),
                    entry("outlaw_zhang_san", List.of("seven_weapons")),
                    entry("numb", List.of("seven_weapons")),
                    entry("entrapment", List.of("seven_weapons")),
                    entry("return_my_car", List.of("entrapment")),
                    entry("no_fly_zone", List.of("entrapment")),
                    entry("one_truth", List.of("exclusive_interview", "eyes_of_the_masses")),
                    // Level 3 三级
                    entry("deadly_calibration", List.of("snowball", "seven_weapons", "hexagon_warrior")),
                    entry("passing_the_torch", List.of("high_noon")),
                    entry("not_prepared", List.of("safe_arrival")),
                    entry("exclusive_interview", List.of("safe_arrival")),
                    entry("no_wasted_bullet", List.of("seven_weapons")),
                    entry("old_soldiers", List.of("seven_weapons", "hexagon_warrior")),
                    entry("blind_snipe", List.of("one_rod_clear")),
                    entry("swordfish_strike", List.of("one_rod_clear")),
                    entry("witch_trial", List.of("seven_weapons")),
                    entry("gg_bond", List.of("high_noon")),
                    entry("flowerpot_swordsman", List.of("night_demon")),
                    // Level 4 四级
                    entry("snowball", List.of("snowball_plus")),
                    entry("safe_arrival", List.of("high_noon")),
                    entry("night_demon", List.of("nightmare")),
                    // Level 5 五级
                    entry("snowball_plus", List.of(CENTER)),
                    entry("hexagon_warrior", List.of(CENTER)),
                    entry("seven_weapons", List.of(CENTER)),
                    entry("high_noon", List.of(CENTER)),
                    entry("one_rod_clear", List.of(CENTER)),
                    entry("nightmare", List.of(CENTER))),
            Arm.NEUTRAL, List.of(
                    // Level 1 一级
                    entry("same_day_death", List.of("double_curtain_call", "fifty_shades")),
                    // Level 2 二级
                    entry("show_off", List.of("team_first", "infernal_affairs", "beaten_rich")),
                    entry("precision_guided", List.of("grenadier", "return_my_car", "no_fly_zone")),
                    entry("deja_vu", List.of("boar_charge", "one_to_ten")),
                    entry("fence_sitter", List.of("eternal_neutrality", "winner_winner")),
                    entry("one_to_ten", List.of("resident_evil")),
                    entry("winner_winner", List.of("fisherman")),
                    entry("fifty_shades", List.of("jester_feast", "who_tied_the_bell")),
                    entry("beaten_rich", List.of("fiend_ascends")),
                    entry("infernal_affairs", List.of("last_jiahao")),
                    // Level 3 三级
                    entry("resident_evil", List.of("eternal_neutrality")),
                    entry("fisherman", List.of("eternal_neutrality")),
                    entry("who_tied_the_bell", List.of("jester_feast")),
                    entry("double_curtain_call", List.of("best_actor")),
                    entry("fiend_ascends", List.of("jester_feast")),
                    entry("last_jiahao", List.of("eternal_neutrality")),
                    // Level 4 四级
                    entry("jester_feast", List.of("best_actor", "eternal_neutrality")),
                    // Level 5 五级
                    entry("eternal_neutrality", List.of(CENTER)),
                    entry("best_actor", List.of(CENTER))),
            Arm.KILLER, List.of(
                    // Level 1 一级
                    entry("first_blood", List.of("mgw", "dual_answer", "show_off")),
                    entry("bloodlust_bared", List.of("death_knell", "deadly_view", "never_miss")),
                    entry("windy_day", List.of("deja_vu", "precision_guided", "get_off_the_train")),
                    entry("needs_repair", List.of("haze", "moonless_night")),
                    entry("carried", List.of("method_actor", "lone_brave", "fence_sitter")),
                    entry("cant_hide_this", List.of("money_talks", "cicada_shell")),
                    entry("emperors_new_clothes", List.of("invisibility_killer", "real_monkey_king", "close_call")),
                    entry("silenced_for_good", List.of("silence_speaks", "donald_duck")),
                    entry("ransom_torn", List.of("home_delivery", "trapped_prey")),
                    // Level 2 二级
                    entry("deadly_view", List.of("grenadier", "good_night")),
                    entry("mgw", List.of("team_first", "boar_charge", "they_want_me_dead")),
                    entry("dual_answer", List.of("boar_charge", "candied_haws", "on_a_rampage")),
                    entry("turncoat", List.of("team_first", "put_down_the_blade")),
                    entry("method_actor", List.of("perfect_crime")),
                    entry("borrowed_knife", List.of("scapegoat")),
                    entry("cicada_shell", List.of("borrowed_knife")),
                    entry("donald_duck", List.of("duck_chorus")),
                    entry("money_talks", List.of("team_first")),
                    entry("real_monkey_king", List.of("scapegoat")),
                    entry("invisibility_killer", List.of("alibi")),
                    entry("close_call", List.of("ill_be_back")),
                    entry("candied_haws", List.of("hundred_paces")),
                    entry("trapped_prey", List.of("joint_hunt")),
                    entry("joint_hunt", List.of("perfect_crime")),
                    entry("home_delivery", List.of("perfect_crime")),
                    entry("alibi", List.of("borrowed_corpse")),
                    entry("mikiri", List.of("far_shuriken", "return_to_sender")),
                    entry("far_shuriken", List.of("hundred_paces")),
                    entry("late_chime", List.of("time_management")),
                    entry("return_to_sender", List.of("ill_be_back")),
                    entry("on_a_rampage", List.of("public_execution")),
                    entry("guard_broken", List.of("ill_be_back")),
                    entry("they_want_me_dead", List.of("guard_broken", "mikiri")),
                    entry("moonless_night", List.of("toll_road")),
                    entry("get_off_the_train", List.of("toll_road")),
                    entry("good_night", List.of("grenadier")),
                    entry("put_down_the_blade", List.of("eighteen_arts")),
                    // Level 3 三级
                    entry("grenadier", List.of("grand_slam")),
                    entry("boar_charge", List.of("grand_slam")),
                    entry("team_first", List.of("grand_slam")),
                    entry("perfect_crime", List.of("at_large")),
                    entry("scapegoat", List.of("perfect_crime")),
                    entry("silence_speaks", List.of("ten_at_once")),
                    entry("never_miss", List.of("already_dead")),
                    entry("hundred_paces", List.of("grand_slam")),
                    entry("borrowed_corpse", List.of("at_large")),
                    entry("time_management", List.of("at_large")),
                    entry("ill_be_back", List.of("eighteen_arts")),
                    entry("public_execution", List.of("eighteen_arts")),
                    entry("toll_road", List.of("eighteen_arts")),
                    entry("lone_brave", List.of("ten_at_once")),
                    // Level 4 四级
                    entry("grand_slam", List.of("grand_slam_plus")),
                    entry("duck_chorus", List.of("at_large")),
                    // Level 5 五级
                    entry("grand_slam_plus", List.of(CENTER)),
                    entry("eighteen_arts", List.of(CENTER)),
                    entry("at_large", List.of(CENTER)),
                    entry("ten_at_once", List.of(CENTER)),
                    entry("already_dead", List.of(CENTER))),
            Arm.WITCH, List.of(
                    // Level 1 一级
                    entry("thanks_to_you", List.of("haze", "muddy_waters")),
                    entry("mighty", List.of("anti_armor")),
                    entry("dizzy", List.of("haze", "hooked_on_blue", "snow_white")),
                    entry("dagger_revealed", List.of("collect_them_all", "next_stop_abyss", "assemble")),
                    // Level 2 二级
                    entry("haze", List.of("blood_feud", "full_arsenal")),
                    entry("anti_armor", List.of("grenadier", "blood_feud", "skewered")),
                    entry("death_knell", List.of("blood_feud", "late_chime")),
                    entry("next_stop_abyss", List.of("all_quiet")),
                    entry("all_quiet", List.of("carry_her_will")),
                    entry("muddy_waters", List.of("carry_her_will")),
                    entry("assemble", List.of("no_such_person")),
                    // Level 3 三级
                    entry("carry_her_will", List.of("not_one_less")),
                    // Level 4 四级
                    entry("blood_feud", List.of("grand_slam_plus", "hat_trick")),
                    entry("skewered", List.of("rival_witches")),
                    entry("collect_them_all", List.of("walpurgis_night")),
                    // Level 5 五级
                    entry("no_such_person", List.of(CENTER)),
                    entry("hat_trick", List.of(CENTER)),
                    entry("walpurgis_night", List.of(CENTER)),
                    entry("not_one_less", List.of(CENTER)),
                    entry("rival_witches", List.of(CENTER)),
                    entry("full_arsenal", List.of(CENTER))));

    private static final Map<String, Arm> ARMS = arms();
    private static final Map<String, List<String>> LINKS = links();
    private static final Map<String, Star> STARS = layout(AchievementCatalog.all());
    private static final double RADIUS = STARS.values().stream()
            .mapToDouble(star -> Math.hypot(star.x(), star.y())).max().orElse(0);

    private AchievementGalaxy() {
    }

    /** Where an achievement sits, in GUI pixels from the middle; y grows downward. 成就所在位置，以 GUI 像素计、相对正中，y 向下增长。 */
    public record Star(Achievement achievement, double x, double y) {
    }

    /**
     * A drawn link; {@code lit} when both ends are earned, otherwise only {@code from} or {@code to} is.
     * 一条要画的连线；两端都已达成时 {@code lit} 为真，否则只有 {@code from} 或 {@code to} 之一已达成。
     */
    public record Link(Star from, Star to, boolean lit) {
    }

    /**
     * What the page shows: earned stars, stars shown but not yet earned, and the links between shown stars that touch
     * an earned one. Links into the middle are never drawn: the middle star shines alone once everything is earned.
     * 成就页显示的内容：已达成的、已显示但尚未达成的，以及显示中的成就之间、至少一端已达成的连线。
     * 通向正中的连线从不画出：达成全部成就后，正中的成就独自亮起。
     */
    public record View(List<Star> earned, List<Star> shown, List<Link> links) {
        public boolean isEmpty() {
            return earned.isEmpty();
        }
    }

    public static Optional<Star> star(String id) {
        return Optional.ofNullable(STARS.get(id));
    }

    /** The achievements this one links to. 此成就连向的成就。 */
    public static List<String> links(String id) {
        return LINKS.getOrDefault(id, List.of());
    }

    /** The arm an achievement belongs to; empty for {@link #CENTER}. 成就所属的旋臂；{@link #CENTER} 没有。 */
    public static Optional<Arm> arm(String id) {
        return Optional.ofNullable(ARMS.get(id));
    }

    /** The farthest any star's middle sits from the galaxy's middle. 任一成就中心到银河中心的最远距离。 */
    public static double radius() {
        return RADIUS;
    }

    /**
     * Lays the galaxy out if that has not happened yet: the class does it once, as it loads, in about half a second.
     * The client calls this on a background thread at startup so that opening the page does not stall; a page opened
     * before it finishes waits for it.
     * 如果银河还没排布，就现在排布：这个类在加载时排布一次，约需半秒。客户端在启动时于后台线程调用它，打开成就页时就不会卡顿；
     * 在它完成之前打开的成就页会等它排完。
     */
    public static void warmUp() {
    }

    /** The page for this account. 该账号的成就页。 */
    public static View view(AchievementLedger ledger) {
        return view(ledger::isUnlocked);
    }

    static View view(Predicate<String> unlocked) {
        Set<String> shown = new LinkedHashSet<>();
        for (Achievement achievement : AchievementCatalog.all()) {
            if (unlocked.test(achievement.id())) {
                shown.add(achievement.id());
                for (String target : links(achievement.id())) {
                    if (!target.equals(CENTER) || unlocked.test(CENTER)) {
                        shown.add(target);
                    }
                }
            }
        }
        List<Star> earned = new ArrayList<>();
        List<Star> notYet = new ArrayList<>();
        List<Link> links = new ArrayList<>();
        for (Achievement achievement : AchievementCatalog.all()) {
            String id = achievement.id();
            if (!shown.contains(id)) {
                continue;
            }
            (unlocked.test(id) ? earned : notYet).add(STARS.get(id));
            for (String target : links(id)) {
                if (target.equals(CENTER)) {
                    continue;
                }
                boolean fromLit = unlocked.test(id);
                boolean toLit = unlocked.test(target);
                if (shown.contains(target) && (fromLit || toLit)) {
                    links.add(new Link(STARS.get(id), STARS.get(target), fromLit && toLit));
                }
            }
        }
        return new View(List.copyOf(earned), List.copyOf(notYet), List.copyOf(links));
    }

    /**
     * Lays the stars out: each arm's slice of the disc is sized by its share of the stars and twisted toward the middle;
     * the stars settle by stress majorization, each pulled toward the distance the links put it from every other star,
     * kept inside its slice, then pushed apart until no two are closer than {@link #GAP}. Every star starts from a spot
     * seeded by its own id, nearer the middle the higher its level, so the same catalog always gives the same galaxy.
     * 排布成就：每条旋臂在圆盘上的扇区按其成就数占比分配，并朝正中扭转；成就按应力优化就位，即每个成就都被拉向连线所定的、
     * 与其他每个成就之间的距离，并保持在自己的扇区内，最后互相推开，直到任意两个都不近于 {@link #GAP}。
     * 每个成就的起点由它自己的 id 决定，等级越高越靠近正中，因此同样的目录总会排出同样的银河。
     */
    static Map<String, Star> layout(List<Achievement> unsorted) {
        List<Achievement> catalog = unsorted.stream().sorted(Comparator.comparing(Achievement::id)).toList();
        Map<String, Star> placed = new HashMap<>();
        catalog.stream().filter(achievement -> achievement.level() == Achievement.MAX_LEVEL)
                .forEach(achievement -> placed.put(achievement.id(), new Star(achievement, 0, 0)));
        List<Achievement> stars = catalog.stream().filter(achievement -> achievement.level() != Achievement.MAX_LEVEL)
                .toList();
        Layout layout = new Layout(stars);
        layout.settle();
        for (int i = 0; i < stars.size(); i++) {
            placed.put(stars.get(i).id(), new Star(stars.get(i), layout.x[i], layout.y[i]));
        }
        return Map.copyOf(placed);
    }

    private static final class Layout {
        private final int count;
        private final int[] arm;
        private final double[] x;
        private final double[] y;
        // The distance the links put between two stars, and how hard that pair pulls toward it.
        // 连线决定的两个成就之间的距离，以及这一对向该距离靠拢的力度。
        private final double[][] target;
        private final double[][] weight;
        private final double[] sliceMiddle = new double[Arm.values().length];
        private final double[] sliceWidth = new double[Arm.values().length];
        private final double rim;

        Layout(List<Achievement> stars) {
            count = stars.size();
            arm = new int[count];
            x = new double[count];
            y = new double[count];
            Map<String, Integer> index = new HashMap<>();
            for (int i = 0; i < count; i++) {
                index.put(stars.get(i).id(), i);
                arm[i] = armOf(stars.get(i).id()).ordinal();
            }

            int[] members = new int[Arm.values().length];
            for (int i = 0; i < count; i++) {
                members[arm[i]]++;
            }
            double open = 2 * Math.PI - Arm.values().length * LANE;
            double start = -Math.PI / 2;
            for (int k = 0; k < members.length; k++) {
                sliceWidth[k] = open * members[k] / Math.max(count, 1);
                sliceMiddle[k] = start + sliceWidth[k] / 2;
                start += sliceWidth[k] + LANE;
            }
            rim = Math.sqrt(count * (GAP + ROOM) * (GAP + ROOM) / PACKING / Math.PI + HOLE * HOLE);

            target = new double[count][count];
            for (double[] row : target) {
                Arrays.fill(row, Double.POSITIVE_INFINITY);
            }
            for (int i = 0; i < count; i++) {
                target[i][i] = 0;
                for (String to : links(stars.get(i).id())) {
                    Integer j = index.get(to);
                    if (j != null && j != i) {
                        double length = arm[i] == arm[j] ? LINK : CROSS_LINK;
                        target[i][j] = Math.min(target[i][j], length);
                        target[j][i] = target[i][j];
                    }
                }
            }
            // Shortest paths through the links; stars far apart along them, or not joined at all, are only asked to
            // keep most of the disc between them. 沿连线的最短路径；沿连线相距很远或根本不相连的成就，只要求隔开大半个圆盘。
            for (int k = 0; k < count; k++) {
                for (int i = 0; i < count; i++) {
                    for (int j = 0; j < count; j++) {
                        if (target[i][k] + target[k][j] < target[i][j]) {
                            target[i][j] = target[i][k] + target[k][j];
                        }
                    }
                }
            }
            weight = new double[count][count];
            for (int i = 0; i < count; i++) {
                for (int j = 0; j < count; j++) {
                    target[i][j] = Math.min(target[i][j], rim * 0.9);
                    weight[i][j] = i == j ? 0 : 1 / (target[i][j] * target[i][j]);
                }
            }

            for (int i = 0; i < count; i++) {
                Random random = new Random(stars.get(i).id().hashCode());
                double depth = (double) (stars.get(i).level() - Achievement.MIN_LEVEL) / (Achievement.MAX_LEVEL - 1);
                double radius = HOLE + (rim - HOLE) * (1 - depth) * (0.6 + 0.4 * random.nextDouble());
                double angle = middle(arm[i], radius) + (random.nextDouble() - 0.5) * sliceWidth[arm[i]] * 0.8;
                x[i] = radius * Math.cos(angle);
                y[i] = radius * Math.sin(angle);
                keepInSlice(i);
            }
        }

        void settle() {
            for (int round = 0; round < SETTLE_ROUNDS; round++) {
                for (int i = 0; i < count; i++) {
                    double sumX = 0;
                    double sumY = 0;
                    double sumWeight = 0;
                    for (int j = 0; j < count; j++) {
                        if (j == i) {
                            continue;
                        }
                        double dx = x[i] - x[j];
                        double dy = y[i] - y[j];
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        double reach = distance < 1e-9 ? 0 : target[i][j] / distance;
                        sumX += weight[i][j] * (x[j] + reach * dx);
                        sumY += weight[i][j] * (y[j] + reach * dy);
                        sumWeight += weight[i][j];
                    }
                    if (sumWeight > 0) {
                        x[i] = sumX / sumWeight;
                        y[i] = sumY / sumWeight;
                    }
                    keepInSlice(i);
                }
                if (round % SPREAD_EVERY == SPREAD_EVERY - 1) {
                    spread(3);
                }
            }
            spread(SPREAD_ROUNDS);
        }

        /** Pushes apart every two stars closer than {@link #GAP}. 推开所有相距小于 {@link #GAP} 的两个成就。 */
        private void spread(int rounds) {
            for (int round = 0; round < rounds; round++) {
                double[] pushX = new double[count];
                double[] pushY = new double[count];
                boolean crowded = false;
                for (int i = 0; i < count; i++) {
                    for (int j = i + 1; j < count; j++) {
                        double dx = x[i] - x[j];
                        double dy = y[i] - y[j];
                        double distance = Math.sqrt(dx * dx + dy * dy);
                        if (distance >= GAP - 0.05) {
                            continue;
                        }
                        crowded = true;
                        if (distance < 1e-9) {
                            // Stacked stars part along a direction fixed by the pair. 重叠的成就沿由这一对决定的方向分开。
                            double angle = i * 2.399 + j;
                            dx = Math.cos(angle);
                            dy = Math.sin(angle);
                            distance = 1;
                        }
                        double push = (GAP - Math.sqrt((x[i] - x[j]) * (x[i] - x[j]) + (y[i] - y[j]) * (y[i] - y[j]))) / 2
                                / distance;
                        pushX[i] += dx * push;
                        pushY[i] += dy * push;
                        pushX[j] -= dx * push;
                        pushY[j] -= dy * push;
                    }
                }
                if (!crowded) {
                    return;
                }
                for (int i = 0; i < count; i++) {
                    x[i] += pushX[i];
                    y[i] += pushY[i];
                    keepInSlice(i);
                }
            }
        }

        /** The direction of an arm's slice at a distance from the middle. 旋臂扇区在离正中某距离处的方向。 */
        private double middle(int arm, double radius) {
            return sliceMiddle[arm] + TWIST * (1 - radius / rim);
        }

        /**
         * Moves a star back inside the disc and its arm's slice, with half a {@link #GAP} to spare at the slice's edges.
         * 把成就移回圆盘和其旋臂的扇区之内，扇区两侧各留半个 {@link #GAP}。
         */
        private void keepInSlice(int i) {
            double radius = Math.min(Math.max(Math.hypot(x[i], y[i]), HOLE + GAP / 2), rim);
            double middle = middle(arm[i], radius);
            double half = Math.max(0, sliceWidth[arm[i]] / 2 - GAP / 2 / radius);
            double offset = Math.min(Math.max(wrap(Math.atan2(y[i], x[i]) - middle), -half), half);
            x[i] = radius * Math.cos(middle + offset);
            y[i] = radius * Math.sin(middle + offset);
        }
    }

    /** {@code angle} folded into (-π, π]. 把 {@code angle} 折算到 (-π, π]。 */
    private static double wrap(double angle) {
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }

    // An achievement nobody assigned to an arm goes last, so the page still draws; the tests catch it.
    // 未分配旋臂的成就排在最后，成就页仍能绘制；测试会发现它。
    private static Arm armOf(String id) {
        return ARMS.getOrDefault(id, Arm.WITCH);
    }

    private static Map<String, Arm> arms() {
        Map<String, Arm> arms = new HashMap<>();
        for (Arm arm : Arm.values()) {
            for (Map.Entry<String, List<String>> links : LINK_MAP.get(arm)) {
                if (arms.put(links.getKey(), arm) != null) {
                    throw new IllegalStateException("Achievement " + links.getKey() + " is listed twice");
                }
            }
        }
        return Map.copyOf(arms);
    }

    /**
     * The links of every achievement in the catalog. A link to an achievement this build does not have yet passes on to
     * that achievement's own links, so no achievement is cut off while others are still to come.
     * 目录中每个成就的连线。连向本版本尚未收录的成就时，改为连向那个成就自己连向的成就，因此在其他成就尚未加入时，不会有成就被切断。
     */
    private static Map<String, List<String>> links() {
        Map<String, List<String>> listed = new HashMap<>();
        LINK_MAP.values().forEach(entries -> entries.forEach(entry -> listed.put(entry.getKey(), entry.getValue())));
        Set<String> known = new HashSet<>();
        AchievementCatalog.all().forEach(achievement -> known.add(achievement.id()));
        Map<String, List<String>> links = new HashMap<>();
        listed.keySet().stream().filter(known::contains)
                .forEach(id -> links.put(id, List.copyOf(targets(id, listed, known))));
        return Map.copyOf(links);
    }

    /** Every achievement the link table lists. 连线表列出的所有成就。 */
    static Set<String> listed() {
        return ARMS.keySet();
    }

    /** What {@code id} links to among the {@code known} achievements. {@code id} 在 {@code known} 成就中连向的成就。 */
    static Set<String> targets(String id, Map<String, List<String>> listed, Set<String> known) {
        Set<String> targets = new LinkedHashSet<>();
        for (String target : listed.getOrDefault(id, List.of())) {
            if (target.equals(CENTER) || known.contains(target)) {
                targets.add(target);
            } else {
                targets.addAll(targets(target, listed, known));
            }
        }
        return targets;
    }
}
