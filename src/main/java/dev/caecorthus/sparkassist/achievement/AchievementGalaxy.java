package dev.caecorthus.sparkassist.achievement;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
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
 * theme into five {@link Arm}s, and the rings twist like a spiral galaxy.
 * <p>
 * Each achievement links to one to three related achievements of the same or a higher level. The page shows every
 * earned achievement and every achievement an earned one links to, the latter with its condition but not its title;
 * the rest stays hidden. The middle star is earned by earning all the others and shows only once earned. Links decide only what is
 * shown: any achievement can be earned at any time.
 * <p>
 * 以银河绘制的成就页。每个成就都有固定的位置：1 级在最外圈，等级每高一级就往里一圈，{@link #CENTER} 独自位于正中。
 * 同一圈里的成就按主题分成五条 {@link Arm}，各圈像旋涡星系一样扭转。
 * 每个成就连向一到三个同级或更高级的相关成就。成就页显示所有已达成的成就，以及已达成成就连向的成就（只显示条件，不显示标题）；
 * 其余的保持隐藏。正中的成就要达成其他所有成就才能获得，获得后才会出现。连线只决定显示什么：任何成就随时都能达成。
 */
public final class AchievementGalaxy {
    public static final String CENTER = "tiny_victory";

    // Ring radius per level in GUI pixels (index = level). Level 2 has 29 stars, so its ring is split into two lanes,
    // LANE pixels inside and outside, taken in turn. Stars are 26 px wide.
    // 每个等级的圈半径，单位为 GUI 像素（下标即等级）。2 级有 29 个成就，因此这一圈分成内外两道，各偏 LANE 像素，交替排布。成就宽 26 像素。
    private static final double[] RING = {0, 228, 174, 120, 82, 44, 0};
    private static final double LANE = 16;
    // Extra turn, in radians, of the innermost ring against the outermost. 最内圈相对最外圈多转的角度（弧度）。
    private static final double TWIST = 1.1;
    // Rounds of reordering each arm toward the stars it links with, then at most this many rounds of swaps.
    // 让每条旋臂内的成就靠近与之相连的成就的排序轮数，以及之后两两交换的最多轮数。
    private static final int SORT_ROUNDS = 6;
    private static final int UNTANGLE_ROUNDS = 30;
    // Far more than all the links together are long. 远大于所有连线的总长。
    private static final double CROSSING_COST = 1_000_000;

    /** Themes, in their order around the galaxy. 主题，按其在银河中的环绕顺序。 */
    public enum Arm {
        FOOD,
        GUARD,
        POLICE,
        KILLER,
        WITCH
    }

    private static final Map<String, Arm> ARMS = arms(Map.of(
            Arm.FOOD, List.of("hooked_on_food", "chug_chug", "hooked_on_blue", "smoke_session", "hungry_ghost",
                    "water_ghost", "die_full", "waterlogged", "last_meal"),
            Arm.GUARD, List.of("welcome_aboard", "restless_ghost", "possession", "your_umbrella", "survivor",
                    "it_works", "democracy", "blessing", "got_you", "fortune", "outlasted", "strong_grow_weak",
                    "sons_of_tara", "weak_grow_strong", "savior", "prized_pupil"),
            Arm.POLICE, List.of("karma", "perfect_cover", "justice_served", "deputy_sheriff", "power_of_the_people",
                    "grasping_at_wind", "depressed", "not_undercover", "point_blank", "within_seven_steps",
                    "long_shot", "quick_justice", "blind_confidence", "deadly_calibration", "snowball",
                    "snowball_plus"),
            Arm.KILLER, List.of("first_blood", "bloodlust_bared", "windy_day", "needs_repair", "turncoat", "mgw",
                    "show_off", "dual_answer", "deadly_view", "precision_guided", "deja_vu", "grenadier",
                    "boar_charge", "team_first", "grand_slam", "grand_slam_plus"),
            Arm.WITCH, List.of("thanks_to_you", "mighty", "dizzy", "haze", "anti_armor", "death_knell", "blood_feud")));

    private static final Map<String, List<String>> LINKS = Map.ofEntries(
            // Level 1 一级
            entry("survivor", List.of("outlasted", "strong_grow_weak", "fortune")),
            entry("welcome_aboard", List.of("restless_ghost")),
            entry("restless_ghost", List.of("possession")),
            entry("possession", List.of("got_you", "blessing")),
            entry("your_umbrella", List.of("it_works", "democracy")),
            entry("first_blood", List.of("mgw", "dual_answer", "show_off")),
            entry("bloodlust_bared", List.of("death_knell", "deadly_view")),
            entry("windy_day", List.of("deja_vu", "precision_guided")),
            entry("needs_repair", List.of("haze")),
            entry("justice_served", List.of("point_blank", "within_seven_steps")),
            entry("deputy_sheriff", List.of("long_shot", "within_seven_steps")),
            entry("power_of_the_people", List.of("quick_justice", "blind_confidence")),
            entry("perfect_cover", List.of("not_undercover", "grasping_at_wind", "turncoat")),
            entry("karma", List.of("depressed", "grasping_at_wind")),
            entry("hooked_on_food", List.of("hungry_ghost")),
            entry("chug_chug", List.of("water_ghost")),
            entry("thanks_to_you", List.of("haze")),
            entry("mighty", List.of("anti_armor")),
            entry("dizzy", List.of("haze", "hooked_on_blue")),
            // Level 2 二级
            entry("deja_vu", List.of("boar_charge")),
            entry("hungry_ghost", List.of("die_full")),
            entry("water_ghost", List.of("waterlogged")),
            entry("deadly_view", List.of("grenadier")),
            entry("point_blank", List.of("deadly_calibration")),
            entry("got_you", List.of("grasping_at_wind")),
            entry("hooked_on_blue", List.of("smoke_session")),
            entry("mgw", List.of("team_first", "boar_charge")),
            entry("it_works", List.of("sons_of_tara", "prized_pupil")),
            entry("grasping_at_wind", List.of("savior")),
            entry("within_seven_steps", List.of("deadly_calibration")),
            entry("quick_justice", List.of("snowball")),
            entry("long_shot", List.of("deadly_calibration")),
            entry("precision_guided", List.of("grenadier")),
            entry("depressed", List.of("savior")),
            entry("haze", List.of("blood_feud")),
            entry("anti_armor", List.of("grenadier", "blood_feud")),
            entry("democracy", List.of("savior")),
            entry("strong_grow_weak", List.of("weak_grow_strong")),
            entry("show_off", List.of("team_first")),
            entry("dual_answer", List.of("boar_charge")),
            entry("smoke_session", List.of("last_meal")),
            entry("blessing", List.of("sons_of_tara")),
            entry("death_knell", List.of("blood_feud")),
            entry("blind_confidence", List.of("weak_grow_strong")),
            entry("not_undercover", List.of("deadly_calibration")),
            entry("turncoat", List.of("team_first")),
            entry("fortune", List.of("outlasted")),
            entry("outlasted", List.of("weak_grow_strong")),
            // Level 3 三级
            entry("die_full", List.of("last_meal")),
            entry("waterlogged", List.of("last_meal")),
            entry("grenadier", List.of("grand_slam")),
            entry("deadly_calibration", List.of("snowball")),
            entry("boar_charge", List.of("grand_slam")),
            entry("sons_of_tara", List.of("savior")),
            entry("weak_grow_strong", List.of("savior", "snowball")),
            entry("team_first", List.of("grand_slam")),
            // Level 4 四级
            entry("last_meal", List.of("prized_pupil")),
            entry("savior", List.of("prized_pupil", "snowball_plus")),
            entry("snowball", List.of("snowball_plus")),
            entry("grand_slam", List.of("grand_slam_plus")),
            entry("blood_feud", List.of("grand_slam_plus")),
            // Level 5 五级
            entry("prized_pupil", List.of(CENTER)),
            entry("snowball_plus", List.of(CENTER)),
            entry("grand_slam_plus", List.of(CENTER)));

    private static final Map<String, Star> STARS = layout(AchievementCatalog.all());
    private static final double RADIUS = RING[1] + LANE;

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

    /** The radius of a level's ring, between its two lanes for level 2. 某等级所在圈的半径，2 级取两道之间。 */
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
     * Lays the stars out. Each arm owns the same slice of every ring, sized by how many achievements it has overall,
     * so an arm runs straight from the rim to the middle before the twist turns it into a spiral. Inside each slice
     * the stars are first sorted toward the stars they link with, then swapped pairwise while that removes link
     * crossings or shortens the links.
     * 排布成就。每条旋臂在每一圈上占同样的扇区，大小按该旋臂的成就总数分配，因此扭转之前每条旋臂都从外缘径直通向中心，
     * 扭转后成为螺旋。每个扇区内先把成就挪向与之相连的成就，再两两交换，只要能减少连线交叉或缩短连线就保留。
     */
    private static Map<String, Star> layout(List<Achievement> catalog) {
        Map<String, Achievement> byId = new HashMap<>();
        catalog.forEach(achievement -> byId.put(achievement.id(), achievement));
        Map<String, List<String>> neighbours = new HashMap<>();
        LINKS.forEach((from, targets) -> targets.stream().filter(to -> !to.equals(CENTER)).forEach(to -> {
            neighbours.computeIfAbsent(from, id -> new ArrayList<>()).add(to);
            neighbours.computeIfAbsent(to, id -> new ArrayList<>()).add(from);
        }));
        Map<Arm, Double> sliceStart = new EnumMap<>(Arm.class);
        Map<Arm, Double> sliceWidth = new EnumMap<>(Arm.class);
        long armed = catalog.stream().filter(achievement -> achievement.level() < Achievement.MAX_LEVEL).count();
        double start = 0;
        for (Arm arm : Arm.values()) {
            long members = catalog.stream()
                    .filter(achievement -> achievement.level() < Achievement.MAX_LEVEL)
                    .filter(achievement -> armOf(achievement.id()) == arm)
                    .count();
            sliceStart.put(arm, start);
            sliceWidth.put(arm, (double) members / armed * 2 * Math.PI);
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

        double best = tangle(place(rings, angles, byId));
        boolean improved = true;
        for (int round = 0; improved && round < UNTANGLE_ROUNDS; round++) {
            improved = false;
            for (List<String> ring : rings.values()) {
                for (int i = 0; i < ring.size(); i++) {
                    for (int j = i + 1; j < ring.size(); j++) {
                        if (armOf(ring.get(i)) != armOf(ring.get(j))) {
                            continue;
                        }
                        Collections.swap(ring, i, j);
                        spread(ring, angles, sliceStart, sliceWidth);
                        double tangle = tangle(place(rings, angles, byId));
                        if (tangle < best - 1e-9) {
                            best = tangle;
                            improved = true;
                        } else {
                            Collections.swap(ring, i, j);
                            spread(ring, angles, sliceStart, sliceWidth);
                        }
                    }
                }
            }
        }
        Map<String, Star> stars = place(rings, angles, byId);
        catalog.stream().filter(achievement -> achievement.level() == Achievement.MAX_LEVEL)
                .forEach(achievement -> stars.put(achievement.id(), new Star(achievement, 0, 0)));
        return Map.copyOf(stars);
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
     * The stars of rings 1–5 at their angles, each ring turned by its twist; level 2 alternates between its lanes.
     * 1–5 圈的成就按各自角度就位，每圈按扭转角度转动；2 级在两道之间交替。
     */
    private static Map<String, Star> place(Map<Integer, List<String>> rings, Map<String, Double> angles,
                                           Map<String, Achievement> byId) {
        Map<String, Star> stars = new HashMap<>();
        rings.forEach((level, ring) -> {
            double twist = TWIST * (1 - RING[level] / RING[Achievement.MIN_LEVEL]);
            for (int i = 0; i < ring.size(); i++) {
                String id = ring.get(i);
                double radius = RING[level] + (level == 2 ? (i % 2 == 0 ? -LANE : LANE) : 0);
                double angle = angles.get(id) - Math.PI / 2 + twist;
                stars.put(id, new Star(byId.get(id), radius * Math.cos(angle), radius * Math.sin(angle)));
            }
        });
        return stars;
    }

    /**
     * How tangled the links are: each crossing outweighs any amount of extra length. Links into the middle are left
     * out, as the middle is not placed yet.
     * 连线有多乱：一处交叉比任何长度差都重要。指向正中的连线不计，因为正中还没摆放。
     */
    private static double tangle(Map<String, Star> stars) {
        List<Star[]> segments = new ArrayList<>();
        LINKS.forEach((from, targets) -> targets.stream().filter(to -> !to.equals(CENTER))
                .forEach(to -> segments.add(new Star[] {stars.get(from), stars.get(to)})));
        double length = 0;
        int crossings = 0;
        for (int i = 0; i < segments.size(); i++) {
            Star[] a = segments.get(i);
            length += Math.hypot(a[0].x() - a[1].x(), a[0].y() - a[1].y());
            for (int j = i + 1; j < segments.size(); j++) {
                Star[] b = segments.get(j);
                if (a[0] != b[0] && a[0] != b[1] && a[1] != b[0] && a[1] != b[1] && cross(a[0], a[1], b[0], b[1])) {
                    crossings++;
                }
            }
        }
        return crossings * CROSSING_COST + length;
    }

    private static boolean cross(Star a, Star b, Star c, Star d) {
        return turn(c, d, a) * turn(c, d, b) < 0 && turn(a, b, c) * turn(a, b, d) < 0;
    }

    private static double turn(Star a, Star b, Star c) {
        return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
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

    private static Map<String, Arm> arms(Map<Arm, List<String>> members) {
        Map<String, Arm> arms = new HashMap<>();
        new EnumMap<>(members).forEach((arm, ids) -> ids.forEach(id -> {
            if (arms.put(id, arm) != null) {
                throw new IllegalStateException("Achievement " + id + " is on two arms");
            }
        }));
        return Map.copyOf(arms);
    }
}
