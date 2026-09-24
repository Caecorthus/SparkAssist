package dev.caecorthus.sparkassist.guidebook;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Reference categories describe base roles and trait eligibility, not live player allegiance.
 * 参考目录按基础身份和词条获取条件分类，不使用玩家当前的实际阵营。
 */
public final class GuidebookNavigation {
    private static final List<String> ROOTS = List.of("roles", "traits", "skills");
    private static final List<String> GROUPS = List.of(
            "roles.civilian", "roles.killer", "roles.witch", "roles.neutral", "roles.other",
            "traits.global", "traits.civilian", "traits.police", "traits.killer", "traits.other",
            "skills.apprentice", "skills.murderous", "skills.grand"
    );
    private static final Map<String, String> ROLE_GROUPS = new HashMap<>();
    private static final Map<String, String> TRAIT_GROUPS = new HashMap<>();
    private static final Map<String, String> WITCH_GROUPS = Map.of(
            "sparkwitch:apprentice_witch", "skills.apprentice",
            "sparkwitch:murderous_witch", "skills.murderous",
            "sparkwitch:grand_witch", "skills.grand"
    );

    static {
        assign(ROLE_GROUPS, "roles.civilian", "wathe", "civilian", "vigilante", "veteran");
        assign(ROLE_GROUPS, "roles.civilian", "noellesroles",
                "conductor", "bartender", "toxicologist", "noisemaker", "recaller", "coroner",
                "voodoo", "undercover", "time_keeper", "awesome_binglus", "reporter", "professor",
                "attendant", "survival_master", "engineer", "bodyguard", "demon_hunter",
                "spiritualist", "detective", "waiter", "mermaid");
        assign(ROLE_GROUPS, "roles.civilian", "sparkwitch",
                "orthopedist", "saint", "pig_god", "apprentice_witch", "perfumer", "tarot_reader",
                "prophet", "wind_spirit", "guardian_angel", "vendetta");
        assign(ROLE_GROUPS, "roles.killer", "wathe", "killer", "secret_killer");
        assign(ROLE_GROUPS, "roles.killer", "noellesroles",
                "phantom", "swapper", "the_insane_damned_paranoid_killer", "morphling", "assassin",
                "scavenger", "bomber", "serial_killer", "silencer", "party_animal", "poisoner", "bandit");
        assign(ROLE_GROUPS, "roles.killer", "sparkwitch",
                "hunter", "ninja", "kidnapper", "black_raven", "witch_maiden", "saboteur", "bell_ringer");
        assign(ROLE_GROUPS, "roles.witch", "sparkwitch", "grand_witch", "accomplice", "curser");
        assign(ROLE_GROUPS, "roles.neutral", "wathe", "loose_end");
        assign(ROLE_GROUPS, "roles.neutral", "sparkwitch", "murderous_witch");
        assign(ROLE_GROUPS, "roles.neutral", "noellesroles",
                "corrupt_cop", "taotie", "pathogen", "vulture", "jester", "shadow_jester");
        assign(TRAIT_GROUPS, "traits.global", "sparktraits", "cautious", "task_master", "fast_hands",
                "childish", "pig", "steady", "excellent_physique", "spirit_sleuth");
        assign(TRAIT_GROUPS, "traits.civilian", "sparktraits", "last_stand", "impostor", "extroverted",
                "introverted", "money_tree", "focus", "depression");
        assign(TRAIT_GROUPS, "traits.police", "sparktraits", "marksman", "fast_reload", "heavy_artillery",
                "niko", "well_trained", "going_dark");
        assign(TRAIT_GROUPS, "traits.killer", "sparktraits", "conscience", "bloodthirsty", "the_showman",
                "plunderer", "charisma", "paranoid", "thrust", "second_strike", "oppressive", "cornered");
    }

    private GuidebookNavigation() {
    }

    public static List<Row> rows(GuidebookCatalog catalog, Set<String> expandedIds,
                                 Predicate<GuidebookEntry> matches, boolean searching) {
        Map<String, List<GuidebookEntry>> grouped = new HashMap<>();
        for (GuidebookEntry entry : catalog.entries()) {
            if (matches.test(entry)) {
                for (String group : groupsFor(entry)) {
                    grouped.computeIfAbsent(group, key -> new ArrayList<>()).add(entry);
                }
            }
        }
        List<Row> rows = new ArrayList<>();
        for (String root : ROOTS) {
            List<String> groups = GROUPS.stream().filter(group -> group.startsWith(root + "."))
                    .filter(group -> grouped.containsKey(group) || !searching && !group.endsWith(".other"))
                    .toList();
            if (searching && groups.isEmpty()) {
                continue;
            }
            boolean rootOpen = searching || expandedIds.contains("root:" + root);
            rows.add(new Row("root:" + root, 0, labelKey(root), null, rootOpen));
            if (!rootOpen) {
                continue;
            }
            for (String group : groups) {
                boolean groupOpen = searching || expandedIds.contains("group:" + group);
                rows.add(new Row("group:" + group, 1, labelKey(group), null, groupOpen));
                if (groupOpen) {
                    grouped.getOrDefault(group, List.of()).stream()
                            .sorted(Comparator.comparingInt(entry -> isOverview(group, entry) ? 0 : 1))
                            .forEach(entry -> rows.add(new Row("group:" + group + "/" + entry.id(), 2,
                                    isOverview(group, entry)
                                            ? labelKey(entry.tab() == GuidebookTab.FACTION ? "faction_overview"
                                                    : group.equals("skills.grand") ? "grand_overview" : "role_overview")
                                            : entry.nameKey(), entry, false)));
                }
            }
        }
        return List.copyOf(rows);
    }

    public static List<String> groupsFor(GuidebookEntry entry) {
        return switch (entry.tab()) {
            case ROLE -> {
                // Wraith retains one of three previous factions; the reference is shared.
                // 冤魂保留生前三种阵营之一，因此在相应目录共享同一篇说明。
                if (entry.id().equals("sparkwitch:wraith")) {
                    yield List.of("roles.civilian", "roles.killer", "roles.witch");
                }
                String faction = ROLE_GROUPS.getOrDefault(entry.id(), "roles.other");
                String skills = WITCH_GROUPS.get(entry.id());
                yield skills == null ? List.of(faction) : List.of(faction, skills);
            }
            case FACTION -> List.of(switch (entry.id()) {
                case "sparkassist:faction/wathe/civilian" -> "roles.civilian";
                case "sparkassist:faction/wathe/killer" -> "roles.killer";
                case "sparkassist:faction/wathe/neutral" -> "roles.neutral";
                case "sparkassist:faction/sparkwitch/witch" -> "roles.witch";
                default -> "roles.other";
            });
            case TRAIT -> List.of(TRAIT_GROUPS.getOrDefault(entry.id(), "traits.other"));
            case SKILL -> GuidebookDiscoveryRules.ownerRoleIds(entry.id()).stream()
                    .map(WITCH_GROUPS::get).filter(java.util.Objects::nonNull).toList();
        };
    }

    public static String labelKey(String category) {
        return "guidebook.sparkassist.tree." + category;
    }

    private static boolean isOverview(String group, GuidebookEntry entry) {
        return entry.tab() == GuidebookTab.FACTION || group.startsWith("skills.") && entry.tab() == GuidebookTab.ROLE;
    }

    private static void assign(Map<String, String> target, String group, String namespace, String... paths) {
        for (String path : paths) {
            target.put(namespace + ":" + path, group);
        }
    }

    public record Row(String key, int depth, String nameKey, GuidebookEntry entry, boolean expanded) {
    }
}
