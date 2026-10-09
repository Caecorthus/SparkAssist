package dev.caecorthus.sparkassist.achievement;

import java.util.Locale;

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
     * The id for one of Wathe's base faction names as written in {@code role_assigned} ({@code CIVILIAN} →
     * {@code wathe:civilian}), or null.
     * {@code role_assigned} 中 Wathe 基础阵营名对应的 id（{@code CIVILIAN} → {@code wathe:civilian}），没有则为 null。
     */
    static String fromWatheName(String name) {
        return name == null || name.isBlank() ? null : "wathe:" + name.trim().toLowerCase(Locale.ROOT);
    }
}
