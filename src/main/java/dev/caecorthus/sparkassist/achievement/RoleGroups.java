package dev.caecorthus.sparkassist.achievement;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Role lists achievements share, and the kill groups {@link LifetimeStats} keeps killed-role sets for. A kill belongs
 * to a group by the killer's role and effective faction at the moment of the kill.
 * 成就共用的身份列表，以及 {@link LifetimeStats} 按其保存击杀身份集合的击杀分组。一次击杀属于哪个分组，
 * 取决于击杀那一刻击杀者的身份和实际阵营。
 */
public final class RoleGroups {
    /** Kills made as a police role while on the civilian side. 以警职身份、站在好人一方时的击杀。 */
    public static final String POLICE = "police";
    /** Kills made while on the killer faction (any role, trait flips included). 站在杀手阵营时的击杀（任何身份，含词条翻转）。 */
    public static final String KILLER = "killer";
    /** Kills made as the Grand Witch. 以大魔女身份的击杀。 */
    public static final String GRAND_WITCH = "grand_witch";
    /** Groups whose streak set lasts one life across rounds. 连续击杀集合跨局延续、死亡即清空的分组。 */
    public static final Set<String> STREAK_GROUPS = Set.of(POLICE, KILLER);

    public static final String GRAND_WITCH_ROLE = "sparkwitch:grand_witch";

    /**
     * The seven police roles (警职). Corrupt Cop and Insider are not police here, even though SparkFactionAPI's
     * police registry lists them.
     * 七个警职身份。黑警和内应在这里不算警职，尽管 SparkFactionAPI 的警察注册表包含它们。
     */
    public static final Set<String> POLICE_ROLES = Set.of(
            "wathe:vigilante",
            "wathe:veteran",
            "sparkwitch:judge",
            "sparkwitch:emma",
            "sparkwitch:control_expert",
            "sparkwitch:seeker",
            "sparkwitch:usec");

    /** All 22 killer-faction roles of Wathe, Noelle's Roles and SparkWitch. Wathe、Noelle's Roles 与 SparkWitch 的全部 22 个杀手身份。 */
    public static final Set<String> KILLER_ROLES = Set.of(
            "wathe:killer",
            "noellesroles:swapper",
            "noellesroles:phantom",
            "noellesroles:morphling",
            "noellesroles:the_insane_damned_paranoid_killer",
            "noellesroles:bomber",
            "noellesroles:assassin",
            "noellesroles:scavenger",
            "noellesroles:serial_killer",
            "noellesroles:silencer",
            "noellesroles:party_animal",
            "noellesroles:poisoner",
            "noellesroles:bandit",
            "sparkwitch:ninja",
            "sparkwitch:black_raven",
            "sparkwitch:witch_maiden",
            "sparkwitch:hunter",
            "sparkwitch:kidnapper",
            "sparkwitch:bell_ringer",
            "sparkwitch:time_stealer",
            "sparkwitch:magician",
            "sparkwitch:saboteur");

    private RoleGroups() {
    }

    public static boolean isPolice(String roleId) {
        return roleId != null && POLICE_ROLES.contains(roleId);
    }

    public static boolean isKillerRole(String roleId) {
        return roleId != null && KILLER_ROLES.contains(roleId);
    }

    /**
     * The kill groups a player with {@code roleId} and effective {@code factionId} is in: {@link #POLICE} for a
     * police role on the civilian side, {@link #KILLER} on the killer faction, {@link #GRAND_WITCH} as the Grand
     * Witch.
     * 身份为 {@code roleId}、实际阵营为 {@code factionId} 的玩家所在的击杀分组：好人一方的警职为 {@link #POLICE}，
     * 杀手阵营为 {@link #KILLER}，大魔女为 {@link #GRAND_WITCH}。
     */
    public static Set<String> groupsOf(String roleId, String factionId) {
        Set<String> groups = new LinkedHashSet<>();
        if (isPolice(roleId) && Factions.isCivilian(factionId)) {
            groups.add(POLICE);
        }
        if (Factions.KILLER.equals(factionId)) {
            groups.add(KILLER);
        }
        if (GRAND_WITCH_ROLE.equals(roleId)) {
            groups.add(GRAND_WITCH);
        }
        return Set.copyOf(groups);
    }
}
