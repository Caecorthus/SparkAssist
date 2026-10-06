package dev.caecorthus.sparkassist.guidebook.decor;

import dev.caecorthus.sparkassist.guidebook.GuidebookEntry;
import dev.caecorthus.sparkassist.guidebook.GuidebookNavigation;
import dev.caecorthus.sparkassist.guidebook.decor.DecorSet.Plate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Picks the decoration set for a page or a player. Pages resolve by tab and faction group (the directory's own
 * grouping, so the two never disagree), take their own plate scene from the table below and their own emblem, then
 * apply the entry's JSON overrides; players resolve the same way from their role id. Anything unknown, including the
 * credits page, rides the train: the civilian set and its viaduct.
 * 为页面或玩家选择点缀套别。页面按标签与阵营分组（沿用目录自身的分组，二者不会打架）解析，再从下表取自己的扉画景与
 * 徽记，最后套用条目 JSON 的覆盖；玩家由身份 id 以同样方式解析。未知情况（含鸣谢页）一律用好人套与它的高架桥。
 */
public final class DecorSetResolver {
    /** Each page's plate scene, chosen by what the role or trait is about rather than by faction.
     * 每页的扉画景，按身份或词条的内容而非阵营挑选。 */
    private static final Map<String, Plate> SCENES = new HashMap<>();

    static {
        scene(Plate.CARRIAGE, "wathe:civilian", "noellesroles:conductor", "noellesroles:bartender",
                "noellesroles:attendant", "noellesroles:waiter", "sparkwitch:insider", "sparktraits:well_supplied",
                "sparktraits:impostor", "sparktraits:team_first");
        scene(Plate.STATION, "wathe:vigilante", "noellesroles:recaller", "noellesroles:time_keeper",
                "sparkwitch:control_expert", "sparkwitch:seeker", "sparkwitch:time_stealer", "noellesroles:swapper",
                "sparktraits:marksman", "sparktraits:fast_reload");
        scene(Plate.SEA, "noellesroles:mermaid", "sparkwitch:fisher", "sparkwitch:abyss_listener",
                "sparktraits:steady", "sparktraits:depression");
        scene(Plate.CHAPEL, "sparkwitch:saint", "sparkwitch:judge", "sparkwitch:guardian_angel", "sparkwitch:prophet",
                "sparkwitch:bell_ringer", "noellesroles:demon_hunter", "sparktraits:conscience", "sparkwitch:healing",
                "sparktraits:last_stand", "sparkwitch:ceremonial_sword");
        scene(Plate.GRAVEYARD, "noellesroles:coroner", "noellesroles:spiritualist", "noellesroles:vulture",
                "sparkwitch:wraith", "sparkwitch:vendetta", "noellesroles:the_insane_damned_paranoid_killer",
                "noellesroles:phantom", "sparkwitch:curser", "sparktraits:spirit_sleuth", "sparktraits:last_escape");
        scene(Plate.STUDY, "noellesroles:professor", "noellesroles:toxicologist", "sparkwitch:perfumer",
                "sparkwitch:orthopedist", "noellesroles:poisoner", "noellesroles:pathogen", "noellesroles:silencer",
                "sparktraits:focus");
        scene(Plate.ROOFTOPS, "sparkwitch:ninja", "noellesroles:assassin", "sparkwitch:black_raven",
                "noellesroles:scavenger", "sparktraits:cautious", "sparktraits:going_dark", "sparktraits:plunderer",
                "sparktraits:thrust", "sparktraits:niko", "sparkwitch:murder_sense");
        scene(Plate.FOREST, "sparkwitch:hunter", "noellesroles:survival_master", "wathe:veteran", "noellesroles:taotie",
                "sparkwitch:fiend", "sparkwitch:kidnapper", "sparkwitch:blind", "sparktraits:well_trained",
                "sparktraits:bloodthirsty", "sparktraits:cornered");
        scene(Plate.STAGE, "noellesroles:noisemaker", "sparkwitch:pig_god", "noellesroles:morphling",
                "sparktraits:the_showman", "sparktraits:charisma", "sparktraits:extroverted", "sparktraits:childish",
                "sparktraits:pig", "sparktraits:oppressive");
        scene(Plate.WORKSHOP, "noellesroles:engineer", "sparkwitch:saboteur", "noellesroles:bomber",
                "sparkwitch:potion_gunner", "sparktraits:master_saboteur", "sparktraits:fast_hands",
                "sparktraits:heavy_artillery", "sparktraits:manic", "sparktraits:herculean_strength",
                "sparktraits:second_strike", "sparkwitch:mighty_force");
        scene(Plate.MARSH, "sparkassist:faction/sparkwitch/witch", "sparkwitch:grand_witch",
                "sparkwitch:apprentice_witch", "sparkwitch:accomplice", "sparkwitch:murderous_witch",
                "sparkwitch:witch_maiden", "noellesroles:voodoo", "sparkwitch:recruit_accomplice");
        scene(Plate.VIADUCT, "sparkassist:faction/wathe/civilian", "sparkassist:guide/basics", "noellesroles:bandit",
                "noellesroles:awesome_binglus", "sparktraits:task_master", "noellesroles:bodyguard", "sparkwitch:emma");
        scene(Plate.RAIN, "sparkassist:faction/wathe/killer", "wathe:killer", "noellesroles:corrupt_cop",
                "wathe:loose_end", "noellesroles:serial_killer", "noellesroles:detective", "noellesroles:undercover",
                "noellesroles:reporter", "sparktraits:paranoid", "sparktraits:close_quarters", "sparktraits:seasoned",
                "sparkwitch:death_ray");
        scene(Plate.FAIR, "sparkassist:faction/wathe/neutral", "noellesroles:jester", "noellesroles:shadow_jester",
                "noellesroles:party_animal", "sparkwitch:tarot_reader");
        scene(Plate.FIELD, "sparktraits:money_tree", "sparktraits:snowball", "sparktraits:excellent_physique",
                "sparktraits:introverted", "sparktraits:exhilarated", "sparkwitch:wind_spirit", "sparkwitch:swift_step");
        scene(Plate.ARCANE, "sparkwitch:riftwalker", "sparkwitch:witch_factor", "sparkwitch:clairvoyance");
    }

    private DecorSetResolver() {
    }

    private static void scene(Plate scene, String... ids) {
        for (String id : ids) {
            SCENES.put(id, scene);
        }
    }

    /** The page's own scene, or {@code fallback} when the table has none. 页面自己的景，表中没有时用 fallback。 */
    public static Plate sceneFor(String id, Plate fallback) {
        return SCENES.getOrDefault(id, fallback);
    }

    /** The set with the page's own scene and, when one is drawn, its emblem. 换上页面自己的景与（若有）徽记。 */
    private static DecorSet personal(DecorSet base, String id) {
        return base.withPlate(sceneFor(id, base.plate()), Emblems.forEntry(id).isPresent() ? id : "");
    }

    public static DecorSet forEntry(GuidebookEntry entry) {
        DecorSet base = switch (entry.tab()) {
            case TRAIT -> DecorSet.TRAIT;
            case SKILL -> DecorSet.SKILL;
            case GUIDE -> DecorSet.CIVILIAN;
            case ROLE, FACTION -> {
                List<String> groups = GuidebookNavigation.groupsFor(entry);
                yield groups.isEmpty() ? DecorSet.CIVILIAN : forGroup(groups.getFirst());
            }
        };
        return personal(base, entry.id()).withOverrides(entry.decor());
    }

    /** By directory group id, e.g. {@code roles.witch}. 按目录分组 id。 */
    public static DecorSet forGroup(String group) {
        return switch (group) {
            case "roles.witch", "skills.grand", "skills.apprentice", "skills.murderous" -> DecorSet.WITCH;
            case "roles.killer" -> DecorSet.KILLER;
            case "roles.neutral" -> DecorSet.NEUTRAL;
            default -> group.startsWith("traits.") ? DecorSet.TRAIT : DecorSet.CIVILIAN;
        };
    }

    /** The player's own set from their role id, with their role's scene and emblem; null (lobby, spectator
     * without a role) rides the train. 由玩家身份 id 得到自身套别，连同该身份的景与徽记；没有身份时用好人套。 */
    public static DecorSet forRole(String roleId) {
        if (roleId == null) {
            return DecorSet.CIVILIAN;
        }
        return personal(forGroup(GuidebookNavigation.roleGroup(roleId)), roleId);
    }

    public static DecorSet forCredits() {
        return DecorSet.CIVILIAN;
    }
}
