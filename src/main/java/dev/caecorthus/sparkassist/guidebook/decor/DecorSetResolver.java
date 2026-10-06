package dev.caecorthus.sparkassist.guidebook.decor;

import dev.caecorthus.sparkassist.guidebook.GuidebookEntry;
import dev.caecorthus.sparkassist.guidebook.GuidebookNavigation;
import dev.caecorthus.sparkassist.guidebook.GuidebookTab;
import java.util.List;

/**
 * Picks the decoration set for a page or a player. Pages resolve by tab and faction group (the directory's own
 * grouping, so the two never disagree), then take their own id (for their plate art and emblem) and theme colour,
 * and apply the entry's JSON overrides; players resolve the same way from their role id. The ribbon and seal
 * follow the directory group too: a faction's own colour (killer traits are red like killers), grey for pages that
 * belong to no faction (global traits, basics, credits, no role). Unknown scenery rides the train: the civilian set
 * and its viaduct.
 * 为页面或玩家选择点缀套别。页面按标签与阵营分组（沿用目录自身的分组，二者不会打架）解析，再带上自己的 id（取扉画
 * 与徽记）与主题色，最后套用条目 JSON 的覆盖；玩家由身份 id 以同样方式解析。丝带与火漆印同样跟随目录分组：阵营自己的
 * 颜色（杀手词条与杀手同为红色），不属于任何阵营的页面（全局词条、新手入门、鸣谢、无身份）为灰色。未知的景致一律用
 * 好人套与它的高架桥。
 */
public final class DecorSetResolver {
    /** Ribbon and seal of a page that belongs to no faction. 不属于任何阵营的页面的丝带与火漆印。 */
    public static final int NO_FACTION = 0x8C8C8C;
    /** Police traits: the vigilante's blue. 警类词条：义警的蓝。 */
    public static final int POLICE = 0x1B8AE5;

    private DecorSetResolver() {
    }

    /** The set for one page: its id and theme colour. 某一页的套别：条目 id 与主题色。 */
    private static DecorSet personal(DecorSet base, String id, int theme) {
        return base.withPage(id, theme);
    }

    public static DecorSet forEntry(GuidebookEntry entry) {
        List<String> groups = GuidebookNavigation.groupsFor(entry);
        String group = groups.isEmpty() ? (entry.tab() == GuidebookTab.SKILL ? "skills.grand" : "roles.other")
                : groups.getFirst();
        DecorSet base = accented(switch (entry.tab()) {
            case TRAIT -> DecorSet.TRAIT;
            case SKILL -> DecorSet.SKILL;
            case GUIDE -> DecorSet.CIVILIAN;
            case ROLE, FACTION -> forGroup(group);
        }, group);
        int theme = entry.color() == GuidebookEntry.DEFAULT_COLOR ? base.colour() : entry.color();
        return personal(base, entry.id(), theme).withOverrides(entry.decor());
    }

    /** The faction's ribbon and seal for a directory group; grey when the group belongs to no faction.
     * 目录分组所属阵营的丝带与火漆印；不属于任何阵营时为灰色。 */
    static DecorSet accented(DecorSet set, String group) {
        return switch (group) {
            case "roles.civilian", "traits.civilian" -> set.withAccent(DecorSet.CIVILIAN.colour(),
                    DecorSet.CIVILIAN.mark());
            case "traits.police" -> set.withAccent(POLICE, DecorSet.CIVILIAN.mark());
            case "roles.killer", "traits.killer" -> set.withAccent(DecorSet.KILLER.colour(), DecorSet.KILLER.mark());
            case "roles.witch", "skills.grand", "skills.apprentice", "skills.murderous" ->
                    set.withAccent(DecorSet.WITCH.colour(), DecorSet.WITCH.mark());
            case "roles.neutral" -> set.withAccent(DecorSet.NEUTRAL.colour(), DecorSet.NEUTRAL.mark());
            default -> set.withAccent(NO_FACTION, set.mark());
        };
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

    /** The player's own set from their role id, their role's page in the faction colour (callers holding the
     * role's entry use {@link #forEntry} for its theme colour); null (lobby, spectator without a role) rides the
     * train under a grey ribbon. 由玩家身份 id 得到自身套别，指向该身份的页面，着阵营色（持有身份条目的调用方改用
     * forEntry 取得主题色）；没有身份时用好人套的景致、灰色丝带。 */
    public static DecorSet forRole(String roleId) {
        if (roleId == null) {
            return accented(DecorSet.CIVILIAN, "");
        }
        String group = GuidebookNavigation.roleGroup(roleId);
        DecorSet base = accented(forGroup(group), group);
        return personal(base, roleId, base.colour());
    }

    public static DecorSet forCredits() {
        return accented(DecorSet.CIVILIAN, "");
    }
}
