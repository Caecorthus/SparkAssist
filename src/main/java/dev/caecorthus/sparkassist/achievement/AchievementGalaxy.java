package dev.caecorthus.sparkassist.achievement;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The achievement page drawn as a galaxy. Every achievement has a fixed star: level 1 on the outermost ring, each
 * higher level one ring further in, and {@link #CENTER} alone in the middle. Within a ring the stars are grouped by
 * theme into six {@link Arm}s, and the rings twist like a spiral galaxy.
 * <p>
 * Each achievement links to one to three related achievements of the same or a higher level. The page shows every
 * earned achievement and every achievement an earned one links to, the latter with its condition but not its title;
 * the rest stays hidden. The middle star is earned by earning all the others and shows only once earned. Links decide only what is
 * shown: any achievement can be earned at any time.
 * <p>
 * 以银河绘制的成就页。每个成就都有固定的位置：1 级在最外圈，等级每高一级就往里一圈，{@link #CENTER} 独自位于正中。
 * 同一圈里的成就按主题分成六条 {@link Arm}，各圈像旋涡星系一样扭转。
 * 每个成就连向一到三个同级或更高级的相关成就。成就页显示所有已达成的成就，以及已达成成就连向的成就（只显示条件，不显示标题）；
 * 其余的保持隐藏。正中的成就要达成其他所有成就才能获得，获得后才会出现。连线只决定显示什么：任何成就随时都能达成。
 */
public final class AchievementGalaxy {
    public static final String CENTER = "tiny_victory";

    // Ring radius per level in GUI pixels (index = level), and the lanes each ring is split into: the stars of a ring
    // take its lanes in turn, LANE pixels apart, so a crowded ring holds twice as many. Stars are 26 px wide and kept
    // at least GAP pixels apart; LANE must stay above GAP * √3 / 2 for two lanes to double a ring.
    // 每个等级的圈半径，单位为 GUI 像素（下标即等级），以及每圈分成几道：同一圈的成就依次轮流占用各道，道与道相距 LANE 像素，
    // 因此拥挤的圈能放下两倍的成就。成就宽 26 像素，中心至少相距 GAP 像素；LANE 必须大于 GAP × √3 / 2，两道才能让一圈容量翻倍。
    private static final double[] RING = {0, 400, 348, 296, 258, 220, 0};
    private static final int[] LANES = {1, 1, 2, 1, 1, 1, 1};
    private static final double LANE = 28;
    private static final double GAP = 31;
    // Extra turn, in radians, of a ring at the very middle against the outermost one. 正中处的圈相对最外圈多转的角度（弧度）。
    private static final double TWIST = 1.5;
    // Rounds of reordering each arm toward the stars it links with, then at most this many rounds of swaps.
    // 让每条旋臂内的成就靠近与之相连的成就的排序轮数，以及之后两两交换的最多轮数。
    private static final int SORT_ROUNDS = 20;
    private static final int UNTANGLE_ROUNDS = 30;
    // Far more than all the links together are long. 远大于所有连线的总长。
    private static final double CROSSING_COST = 1_000_000;

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
                    entry("not_undercover", List.of("deadly_calibration")),
                    entry("red_handed", List.of("quick_justice", "passing_the_torch")),
                    entry("debt_has_owner", List.of("last_scream")),
                    entry("last_scream", List.of("dimmed_light")),
                    entry("dimmed_light", List.of("exclusive_interview", "scent_of_blood")),
                    entry("trap_the_turtle", List.of("safe_arrival")),
                    entry("eyes_of_the_masses", List.of("no_wasted_bullet", "outlaw_zhang_san")),
                    entry("close_your_eyes", List.of("old_soldiers")),
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
                    // Level 4 四级
                    entry("snowball", List.of("snowball_plus")),
                    entry("safe_arrival", List.of("high_noon")),
                    // Level 5 五级
                    entry("snowball_plus", List.of(CENTER)),
                    entry("hexagon_warrior", List.of(CENTER)),
                    entry("seven_weapons", List.of(CENTER)),
                    entry("high_noon", List.of(CENTER)),
                    entry("one_rod_clear", List.of(CENTER))),
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
    private static final double RADIUS = RING[1] + (LANES[1] - 1) * LANE / 2;

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
     * an earned one.
     * 成就页显示的内容：已达成的、已显示但尚未达成的，以及显示中的成就之间、至少一端已达成的连线。
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

    /** The radius of a level's ring, between its lanes if it has several. 某等级所在圈的半径，分道时取各道正中。 */
    public static double ring(int level) {
        if (level < Achievement.MIN_LEVEL || level > Achievement.MAX_LEVEL) {
            throw new IllegalArgumentException("level " + level);
        }
        return RING[level];
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
     * Lays the stars out. Each arm owns the same slice of every ring, wide enough for the ring where the arm is most
     * crowded, so an arm runs straight from the rim to the middle before the twist turns it into a spiral. Inside each
     * slice the stars are first sorted toward the stars they link with, then swapped pairwise while that removes link
     * crossings or shortens the links.
     * 排布成就。每条旋臂在每一圈上占同样的扇区，宽度足以容纳该旋臂最拥挤的那一圈，因此扭转之前每条旋臂都从外缘径直通向中心，
     * 扭转后成为螺旋。每个扇区内先把成就挪向与之相连的成就，再两两交换，只要能减少连线交叉或缩短连线就保留。
     */
    private static Map<String, Star> layout(List<Achievement> unsorted) {
        // Worked in id order, so moving an entry within the catalog leaves the galaxy as it is.
        // 按 id 顺序处理，因此在目录里挪动条目不会改变银河。
        List<Achievement> catalog = unsorted.stream().sorted(Comparator.comparing(Achievement::id)).toList();
        Map<String, Achievement> byId = new HashMap<>();
        catalog.forEach(achievement -> byId.put(achievement.id(), achievement));
        // Links between stars on the rings; links into the middle are left out, as the middle is not placed yet.
        // 环上成就之间的连线；指向正中的连线不计，因为正中还没摆放。
        List<String[]> segments = new ArrayList<>();
        Map<String, List<String>> neighbours = new HashMap<>();
        for (Achievement achievement : catalog) {
            for (String to : links(achievement.id())) {
                if (!to.equals(CENTER)) {
                    segments.add(new String[] {achievement.id(), to});
                    neighbours.computeIfAbsent(achievement.id(), id -> new ArrayList<>()).add(to);
                    neighbours.computeIfAbsent(to, id -> new ArrayList<>()).add(achievement.id());
                }
            }
        }

        Map<Arm, Double> sliceStart = new EnumMap<>(Arm.class);
        Map<Arm, Double> sliceWidth = new EnumMap<>(Arm.class);
        Map<Arm, Double> crowding = new EnumMap<>(Arm.class);
        for (Arm arm : Arm.values()) {
            double most = 0;
            for (int level = Achievement.MIN_LEVEL; level < Achievement.MAX_LEVEL; level++) {
                int ringLevel = level;
                long members = catalog.stream()
                        .filter(achievement -> achievement.level() == ringLevel && armOf(achievement.id()) == arm)
                        .count();
                most = Math.max(most, members / capacity(level));
            }
            crowding.put(arm, most);
        }
        double crowded = crowding.values().stream().mapToDouble(Double::doubleValue).sum();
        double start = 0;
        for (Arm arm : Arm.values()) {
            sliceStart.put(arm, start);
            sliceWidth.put(arm, crowding.get(arm) / crowded * 2 * Math.PI);
            start += sliceWidth.get(arm);
        }

        Map<Integer, List<String>> rings = new LinkedHashMap<>();
        for (int level = Achievement.MIN_LEVEL; level < Achievement.MAX_LEVEL; level++) {
            int ringLevel = level;
            rings.put(level, new ArrayList<>(catalog.stream()
                    .filter(achievement -> achievement.level() == ringLevel)
                    .sorted(Comparator.comparing((Achievement achievement) -> armOf(achievement.id())))
                    .map(Achievement::id)
                    .toList()));
        }
        Map<String, Double> angles = new HashMap<>();
        rings.values().forEach(ring -> spread(ring, angles, sliceStart, sliceWidth));
        for (int round = 0; round < SORT_ROUNDS; round++) {
            for (List<String> ring : rings.values()) {
                List<String> sorted = new ArrayList<>();
                for (Arm arm : Arm.values()) {
                    double middle = sliceStart.get(arm) + sliceWidth.get(arm) / 2;
                    ring.stream().filter(id -> armOf(id) == arm)
                            .sorted(Comparator.comparingDouble(id -> wrap(pull(id, neighbours, angles) - middle)))
                            .forEach(sorted::add);
                }
                ring.clear();
                ring.addAll(sorted);
                spread(ring, angles, sliceStart, sliceWidth);
            }
        }

        Untangler untangler = new Untangler(place(rings, angles, byId), segments);
        untangler.run(rings);
        Map<String, Star> placed = untangler.stars();
        catalog.stream().filter(achievement -> achievement.level() == Achievement.MAX_LEVEL)
                .forEach(achievement -> placed.put(achievement.id(), new Star(achievement, 0, 0)));
        return Map.copyOf(placed);
    }

    /**
     * About how many stars fit on a level's ring: its innermost lane's length over {@link #GAP}, once per lane.
     * 某等级的圈大约能放下多少个成就：最内道的周长除以 {@link #GAP}，每道各算一次。
     */
    private static double capacity(int level) {
        double innermost = RING[level] - (LANES[level] - 1) * LANE / 2;
        return 2 * Math.PI * innermost / GAP * LANES[level];
    }

    /** Spreads each arm's stars evenly over its slice of the ring. 把每条旋臂的成就均匀铺满它在这一圈上的扇区。 */
    private static void spread(List<String> ring, Map<String, Double> angles, Map<Arm, Double> sliceStart,
                               Map<Arm, Double> sliceWidth) {
        for (Arm arm : Arm.values()) {
            List<String> group = ring.stream().filter(id -> armOf(id) == arm).toList();
            for (int i = 0; i < group.size(); i++) {
                angles.put(group.get(i), sliceStart.get(arm) + (i + 0.5) / group.size() * sliceWidth.get(arm));
            }
        }
    }

    /**
     * The stars of rings 1–5 at their angles, each ring turned by its twist; the stars of a ring take its lanes in turn.
     * 1–5 圈的成就按各自角度就位，每圈按扭转角度转动；同一圈的成就依次轮流占用各道。
     */
    private static Map<String, Star> place(Map<Integer, List<String>> rings, Map<String, Double> angles,
                                           Map<String, Achievement> byId) {
        Map<String, Star> stars = new HashMap<>();
        rings.forEach((level, ring) -> {
            double twist = TWIST * (1 - RING[level] / RING[Achievement.MIN_LEVEL]);
            for (int i = 0; i < ring.size(); i++) {
                String id = ring.get(i);
                double radius = RING[level] + (i % LANES[level] - (LANES[level] - 1) / 2.0) * LANE;
                double angle = angles.get(id) - Math.PI / 2 + twist;
                stars.put(id, new Star(byId.get(id), radius * Math.cos(angle), radius * Math.sin(angle)));
            }
        });
        return stars;
    }

    /**
     * Swaps stars of the same arm and ring while that makes the links less tangled: each crossing outweighs any amount
     * of extra length. Two such stars trade places exactly, so a swap is judged by the links touching them alone.
     * 交换同一圈、同一旋臂的成就，只要能让连线更不乱就保留：一处交叉比任何长度差都重要。
     * 这样的两个成就恰好互换位置，因此只需看与它们相连的连线就能判断一次交换的好坏。
     */
    private static final class Untangler {
        private final Map<String, Integer> index = new HashMap<>();
        private final List<Achievement> achievements = new ArrayList<>();
        private final double[] x;
        private final double[] y;
        private final int[][] segments;
        private final int[][] touching;
        private final boolean[] moving;

        Untangler(Map<String, Star> stars, List<String[]> links) {
            stars.keySet().stream().sorted().forEach(id -> {
                index.put(id, achievements.size());
                achievements.add(stars.get(id).achievement());
            });
            x = new double[achievements.size()];
            y = new double[achievements.size()];
            stars.forEach((id, star) -> {
                x[index.get(id)] = star.x();
                y[index.get(id)] = star.y();
            });
            segments = new int[links.size()][];
            List<List<Integer>> touches = new ArrayList<>();
            achievements.forEach(achievement -> touches.add(new ArrayList<>()));
            for (int s = 0; s < links.size(); s++) {
                segments[s] = new int[] {index.get(links.get(s)[0]), index.get(links.get(s)[1])};
                touches.get(segments[s][0]).add(s);
                touches.get(segments[s][1]).add(s);
            }
            touching = touches.stream().map(list -> list.stream().mapToInt(Integer::intValue).toArray())
                    .toArray(int[][]::new);
            moving = new boolean[segments.length];
        }

        /**
         * Swaps until no swap helps or the rounds run out. After the first round only pairs with a star that moved, or
         * links to one that moved, in the round before are tried again; the rest found nothing better last time.
         * 交换到再无改进或轮数用尽。第一轮之后，只重试含有上一轮移动过、或与移动过的成就相连的成就的组合；其余组合上一轮已经找不到更好的位置。
         */
        void run(Map<Integer, List<String>> rings) {
            boolean[] restless = new boolean[achievements.size()];
            Arrays.fill(restless, true);
            boolean improved = true;
            for (int round = 0; improved && round < UNTANGLE_ROUNDS; round++) {
                improved = false;
                boolean[] moved = new boolean[achievements.size()];
                for (List<String> ring : rings.values()) {
                    for (int i = 0; i < ring.size(); i++) {
                        for (int j = i + 1; j < ring.size(); j++) {
                            int p = index.get(ring.get(i));
                            int q = index.get(ring.get(j));
                            if (armOf(ring.get(i)) == armOf(ring.get(j)) && (restless[p] || restless[q])
                                    && trySwap(p, q)) {
                                Collections.swap(ring, i, j);
                                stir(moved, p);
                                stir(moved, q);
                                improved = true;
                            }
                        }
                    }
                }
                restless = moved;
            }
        }

        /** Marks a star and the stars it links with. 标记一个成就及与之相连的成就。 */
        private void stir(boolean[] marks, int star) {
            marks[star] = true;
            for (int s : touching[star]) {
                marks[segments[s][0]] = true;
                marks[segments[s][1]] = true;
            }
        }

        Map<String, Star> stars() {
            Map<String, Star> stars = new HashMap<>();
            index.forEach((id, i) -> stars.put(id, new Star(achievements.get(i), x[i], y[i])));
            return stars;
        }

        private boolean trySwap(int p, int q) {
            int[] moved = new int[touching[p].length + touching[q].length];
            int count = 0;
            for (int[] links : new int[][] {touching[p], touching[q]}) {
                for (int s : links) {
                    if (!moving[s]) {
                        moving[s] = true;
                        moved[count++] = s;
                    }
                }
            }
            double before = tangle(moved, count);
            swap(p, q);
            boolean better = tangle(moved, count) < before - 1e-9;
            if (!better) {
                swap(p, q);
            }
            for (int i = 0; i < count; i++) {
                moving[moved[i]] = false;
            }
            return better;
        }

        private void swap(int p, int q) {
            double px = x[p];
            double py = y[p];
            x[p] = x[q];
            y[p] = y[q];
            x[q] = px;
            y[q] = py;
        }

        /** How tangled the moving links are with every link. 移动中的连线与所有连线的纠缠程度。 */
        private double tangle(int[] moved, int count) {
            double cost = 0;
            for (int i = 0; i < count; i++) {
                int[] a = segments[moved[i]];
                cost += Math.hypot(x[a[0]] - x[a[1]], y[a[0]] - y[a[1]]);
                for (int t = 0; t < segments.length; t++) {
                    // A crossing between two moving links is met from both of them. 两条移动中的连线之间的交叉会从两边各遇到一次。
                    if (t != moved[i] && crosses(a, segments[t])) {
                        cost += moving[t] ? CROSSING_COST / 2 : CROSSING_COST;
                    }
                }
            }
            return cost;
        }

        private boolean crosses(int[] a, int[] b) {
            if (a[0] == b[0] || a[0] == b[1] || a[1] == b[0] || a[1] == b[1]) {
                return false;
            }
            return turn(b[0], b[1], a[0]) * turn(b[0], b[1], a[1]) < 0
                    && turn(a[0], a[1], b[0]) * turn(a[0], a[1], b[1]) < 0;
        }

        private double turn(int a, int b, int c) {
            return (x[b] - x[a]) * (y[c] - y[a]) - (y[b] - y[a]) * (x[c] - x[a]);
        }
    }

    /** The mean direction of the stars this one links with, or its own angle. 与之相连的成就的平均方向，没有则为自身角度。 */
    private static double pull(String id, Map<String, List<String>> neighbours, Map<String, Double> angles) {
        double x = 0;
        double y = 0;
        for (String other : neighbours.getOrDefault(id, List.of())) {
            Double angle = angles.get(other);
            if (angle != null) {
                x += Math.cos(angle);
                y += Math.sin(angle);
            }
        }
        return x == 0 && y == 0 ? angles.get(id) : Math.atan2(y, x);
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
