package dev.caecorthus.sparkassist.guidebook;

import java.util.List;
import java.util.Map;

/**
 * Defines which optional registry entries belong in the GuideBook index.
 * 定义可选注册表中的哪些条目属于指南书目录。
 */
public final class GuidebookDiscoveryRules {
    // Grand Witch: the main-key Witch Factor plus the two unlocks after 2 tasks. Only Witch Factor is a registry
    // skill; Recruit Accomplice (second key) and the permanent Ceremonial Sword appear only through authored pages.
    // 大魔女：主技能键的魔女因子，以及完成 2 个任务后解锁的两项能力。只有魔女因子在技能注册表中；
    // 招募同伙（第二技能键）与常驻仪礼剑只会通过人工编写的页面出现。
    // The owner also files every skill page under its directory group (GuidebookNavigation.groupsFor), so authored
    // pages that are never discovered, like recruit_accomplice, must stay mapped here; an owner without a skills
    // group leaves its skill with no directory row.
    // 所属身份同时决定技能页所在的目录分组（GuidebookNavigation.groupsFor），因此从不被自动发现的人工页面（如招募同伙）
    // 也必须保留在此映射中；所属身份没有技能分组时，该技能在目录中不会出现。
    private static final Map<String, String> WITCH_SKILL_OWNER_ROLE_IDS = Map.ofEntries(
            Map.entry("sparkwitch:witch_factor", "sparkwitch:grand_witch"),
            Map.entry("sparkwitch:recruit_accomplice", "sparkwitch:grand_witch"),
            Map.entry("sparkwitch:ceremonial_sword", "sparkwitch:grand_witch"),
            Map.entry("sparkwitch:emma_factor", "sparkwitch:emma"),
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
