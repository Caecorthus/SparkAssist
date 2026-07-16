package dev.caecorthus.sparkassist.guidebook;

import java.util.Set;

/**
 * Defines which optional registry entries belong in the GuideBook index.
 * 定义可选注册表中的哪些条目属于指南书目录。
 */
public final class GuidebookDiscoveryRules {
    private static final Set<String> EXCLUDED_SKILL_IDS = Set.of(
            "sparkwitch:death_omen",
            "sparkwitch:focused_footsteps",
            "sparkwitch:pig_chase"
    );

    private GuidebookDiscoveryRules() {
    }

    public static boolean includes(GuidebookTab tab, String id) {
        return tab != GuidebookTab.SKILL || !EXCLUDED_SKILL_IDS.contains(id);
    }
}
