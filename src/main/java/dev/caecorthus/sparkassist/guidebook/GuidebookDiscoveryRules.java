package dev.caecorthus.sparkassist.guidebook;

import java.util.List;
import java.util.Map;

/**
 * Defines which optional registry entries belong in the GuideBook index.
 * 定义可选注册表中的哪些条目属于指南书目录。
 */
public final class GuidebookDiscoveryRules {
    private static final Map<String, String> WITCH_SKILL_OWNER_ROLE_IDS = Map.ofEntries(
            Map.entry("sparkwitch:ceremonial_sword", "sparkwitch:grand_witch"),
            Map.entry("sparkwitch:death_ray", "sparkwitch:murderous_witch"),
            Map.entry("sparkwitch:mighty_force", "sparkwitch:apprentice_witch"),
            Map.entry("sparkwitch:swift_step", "sparkwitch:apprentice_witch"),
            Map.entry("sparkwitch:murder_sense", "sparkwitch:apprentice_witch"),
            Map.entry("sparkwitch:healing", "sparkwitch:apprentice_witch"),
            Map.entry("sparkwitch:clairvoyance", "sparkwitch:apprentice_witch")
    );

    private GuidebookDiscoveryRules() {
    }

    public static boolean includes(GuidebookTab tab, String id) {
        return tab != GuidebookTab.SKILL || WITCH_SKILL_OWNER_ROLE_IDS.containsKey(id);
    }

    public static List<String> ownerRoleIds(String skillId) {
        String roleId = WITCH_SKILL_OWNER_ROLE_IDS.get(skillId);
        return roleId == null ? List.of() : List.of(roleId);
    }
}
