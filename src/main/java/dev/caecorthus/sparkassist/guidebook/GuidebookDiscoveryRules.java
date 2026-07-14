package dev.caecorthus.sparkassist.guidebook;

/**
 * Defines which optional registry entries belong in the GuideBook index.
 * 定义可选注册表中的哪些条目属于指南书目录。
 */
public final class GuidebookDiscoveryRules {
    private static final String PIG_CHASE_SKILL_ID = "sparkwitch:pig_chase";

    private GuidebookDiscoveryRules() {
    }

    public static boolean includes(GuidebookTab tab, String id) {
        return tab != GuidebookTab.SKILL || !PIG_CHASE_SKILL_ID.equals(id);
    }
}
