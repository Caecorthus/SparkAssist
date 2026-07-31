package dev.caecorthus.sparkassist.guidebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GuidebookDiscoveryRulesTest {
    private static final Map<String, String> INCLUDED_SKILLS = Map.of(
            "sparkwitch:ceremonial_sword", "sparkwitch:grand_witch",
            "sparkwitch:death_ray", "sparkwitch:murderous_witch",
            "sparkwitch:mighty_force", "sparkwitch:apprentice_witch",
            "sparkwitch:swift_step", "sparkwitch:apprentice_witch",
            "sparkwitch:murder_sense", "sparkwitch:apprentice_witch",
            "sparkwitch:healing", "sparkwitch:apprentice_witch",
            "sparkwitch:clairvoyance", "sparkwitch:apprentice_witch"
    );

    @Test
    void skillDiscoveryIncludesExactlyTheSupportedWitchSkills() {
        assertEquals(7, INCLUDED_SKILLS.size());
        INCLUDED_SKILLS.forEach((skillId, roleId) ->
                assertTrue(GuidebookDiscoveryRules.includes(GuidebookTab.SKILL, skillId), skillId));

        for (String excluded : List.of(
                "sparkwitch:death_omen",
                "sparkwitch:pig_chase",
                "sparkwitch:perception",
                "sparkwitch:unknown",
                "othermod:ceremonial_sword"
        )) {
            assertFalse(GuidebookDiscoveryRules.includes(GuidebookTab.SKILL, excluded), excluded);
        }
        assertTrue(GuidebookDiscoveryRules.includes(GuidebookTab.ROLE, "sparkwitch:death_omen"));
    }

    @Test
    void supportedSkillsShareTheClosedOwnerMapping() {
        INCLUDED_SKILLS.forEach((skillId, roleId) ->
                assertEquals(List.of(roleId), GuidebookDiscoveryRules.ownerRoleIds(skillId), skillId));
        assertEquals(List.of(), GuidebookDiscoveryRules.ownerRoleIds("sparkwitch:unknown"));
        assertEquals(List.of(), GuidebookDiscoveryRules.ownerRoleIds("othermod:ceremonial_sword"));
    }
}
