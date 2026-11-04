package dev.caecorthus.sparkassist.achievement;

import dev.doctor4t.wathe.api.Faction;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Faction ids as SparkFactionAPI names them, and the two sides achievements talk about: "好人" is the civilian
 * faction; "非好人" is every other real faction (killers, neutrals, witches, …). Achievements judge a player by their
 * <em>effective</em> faction, so a Conscience or Impostor flip counts.
 * SparkFactionAPI 使用的阵营 id，以及成就里说的两方：“好人”即平民阵营；“非好人”是其他所有真实阵营（杀手、中立、魔女……）。
 * 成就按<em>实际</em>阵营判断玩家，良知、内鬼等词条造成的阵营翻转同样算数。
 */
public final class Factions {
    public static final String CIVILIAN = "wathe:civilian";
    public static final String KILLER = "wathe:killer";
    public static final String NEUTRAL = "wathe:neutral";
    /** No faction: no role, or a role outside every faction. 无阵营：没有角色，或角色不属于任何阵营。 */
    public static final String NONE = "wathe:none";
    public static final String WITCH = "sparkwitch:witch";
    public static final String MURDEROUS_WITCH = "sparkwitch:murderous_witch";
    /**
     * Team Jiahao (嘉豪阵营): the Insider and Noelle's Roles' Corrupt Cop, a native Wathe faction ({@code JIAHAO})
     * since 2026-11-04; before that both read as {@link #NEUTRAL}.
     * 嘉豪阵营：内应与 Noelle's Roles 的黑警，自 2026-11-04 起是 Wathe 原生阵营（{@code JIAHAO}）；此前两者都读作 {@link #NEUTRAL}。
     */
    public static final String JIAHAO = "sparkwitch:jiahao";

    /**
     * Native custom Wathe constants SparkWitch declares through SparkFactionAPI, used when the API cannot name them
     * (older SparkFactionAPI, or the lookup failed this time).
     * SparkWitch 通过 SparkFactionAPI 声明的 Wathe 原生自定义常量；API 无法给出 id 时（旧版 SparkFactionAPI 或本次查询失败）使用。
     */
    private static final Map<String, String> NATIVE_FALLBACK = Map.of("WITCH", WITCH, "JIAHAO", JIAHAO);

    private Factions() {
    }

    /** "好人": the civilian faction. “好人”：平民阵营。 */
    public static boolean isCivilian(String factionId) {
        return CIVILIAN.equals(factionId);
    }

    /**
     * "非好人": any known faction other than civilian; unknown (null) and {@link #NONE} are neither side.
     * “非好人”：平民以外的任何已知阵营；未知（null）和 {@link #NONE} 不属于任何一方。
     */
    public static boolean isNonCivilian(String factionId) {
        return factionId != null && !factionId.isBlank() && !CIVILIAN.equals(factionId) && !NONE.equals(factionId);
    }

    /**
     * The side a faction stood on before Team Jiahao became its own Wathe faction: {@link #JIAHAO} folds back into
     * {@link #NEUTRAL}, every other id (null too) is itself. Only for rules whose meaning was fixed while the Insider
     * and the Corrupt Cop read as neutral, so who counts as an "opponent" there does not change.
     * 嘉豪阵营独立之前的阵营归属：{@link #JIAHAO} 折回 {@link #NEUTRAL}，其他 id（包括 null）不变。只用于含义在内应与黑警
     * 仍读作中立时就已确定的规则，使这些规则里“对手”的范围保持不变。
     */
    public static String legacySide(String factionId) {
        return JIAHAO.equals(factionId) ? NEUTRAL : factionId;
    }

    /**
     * The id for a Wathe {@code Faction} constant name as written in {@code role_assigned} ({@code CIVILIAN} →
     * {@code wathe:civilian}), or null. SparkFactionAPI appends native custom constants ({@code WITCH},
     * {@code JIAHAO}) and names their ids through {@code factionIdOf(Faction.valueOf(name))}; without it (or when
     * that lookup fails) the built-ins keep the {@code wathe:} form and the known natives use their SparkWitch ids.
     * {@code role_assigned} 中 Wathe {@code Faction} 常量名对应的 id（{@code CIVILIAN} → {@code wathe:civilian}），没有则为
     * null。SparkFactionAPI 会追加原生自定义常量（{@code WITCH}、{@code JIAHAO}），并通过
     * {@code factionIdOf(Faction.valueOf(name))} 给出其 id；没有该 API（或本次查询失败）时，内置阵营沿用 {@code wathe:} 形式，
     * 已知的原生阵营使用 SparkWitch 的 id。
     */
    public static String fromWatheName(String name) {
        return fromWatheName(name, SparkFactionNames::idOf);
    }

    /**
     * {@link #fromWatheName(String)} with the SparkFactionAPI lookup given: it returns the id or null, and may throw;
     * either way the call falls back for this name only.
     * 指定 SparkFactionAPI 查询的 {@link #fromWatheName(String)}：查询返回 id 或 null，也可能抛出异常；无论哪种情况，都只对
     * 这一个名称回退。
     */
    static String fromWatheName(String name, Function<String, String> sparkFactionLookup) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String constant = name.trim();
        String id;
        try {
            id = sparkFactionLookup.apply(constant);
        } catch (RuntimeException | LinkageError failed) {
            id = null;
        }
        if (id != null && !id.isBlank()) {
            return id;
        }
        String known = NATIVE_FALLBACK.get(constant.toUpperCase(Locale.ROOT));
        return known != null ? known : "wathe:" + constant.toLowerCase(Locale.ROOT);
    }

    /**
     * SparkFactionAPI's {@code factionIdOf(Faction)}, reached by reflection and looked up once (SparkAssist never
     * requires SparkFactionAPI). Fails closed: a missing class or method (older API) answers null for good; a failing
     * call answers null for that call only.
     * 通过反射调用 SparkFactionAPI 的 {@code factionIdOf(Faction)}，只查找一次（SparkAssist 从不强制依赖 SparkFactionAPI）。
     * 失败时关闭：类或方法缺失（旧版 API）时始终返回 null；单次调用失败只让这一次返回 null。
     */
    private static final class SparkFactionNames {
        private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Achievements");
        private static final Method FACTION_ID_OF = find();
        private static volatile boolean warned;

        private static Method find() {
            try {
                Class<?> api = Class.forName("dev.caecorthus.sparkfactionapi.api.SparkFactionApi", false,
                        Factions.class.getClassLoader());
                return api.getMethod("factionIdOf", Faction.class);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException missing) {
                return null;
            }
        }

        static String idOf(String constant) {
            if (FACTION_ID_OF == null) {
                return null;
            }
            Faction faction;
            try {
                faction = Faction.valueOf(constant);
            } catch (IllegalArgumentException unknown) {
                return null;
            }
            try {
                Object result = FACTION_ID_OF.invoke(null, faction);
                Object id = result instanceof Optional<?> optional ? optional.orElse(null) : null;
                return id == null ? null : id.toString();
            } catch (ReflectiveOperationException | RuntimeException failed) {
                if (!warned) {
                    warned = true;
                    LOGGER.warn("SparkFactionAPI factionIdOf({}) failed; using SparkAssist's own faction names",
                            constant, failed);
                }
                return null;
            }
        }
    }
}
