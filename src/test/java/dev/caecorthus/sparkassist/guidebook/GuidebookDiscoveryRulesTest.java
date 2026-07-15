package dev.caecorthus.sparkassist.guidebook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuidebookDiscoveryRulesTest {
    @Test
    void excludesRoleOwnedSkillsOnlyFromTheSkillTab() {
        assertFalse(GuidebookDiscoveryRules.includes(
                GuidebookTab.SKILL, "sparkwitch:death_omen"));
        assertFalse(GuidebookDiscoveryRules.includes(
                GuidebookTab.SKILL, "sparkwitch:pig_chase"));
        assertTrue(GuidebookDiscoveryRules.includes(
                GuidebookTab.SKILL, "sparkwitch:mighty_force"));
        assertTrue(GuidebookDiscoveryRules.includes(
                GuidebookTab.ROLE, "sparkwitch:death_omen"));
    }
}
