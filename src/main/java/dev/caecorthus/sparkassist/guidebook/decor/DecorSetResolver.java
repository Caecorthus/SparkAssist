package dev.caecorthus.sparkassist.guidebook.decor;

import dev.caecorthus.sparkassist.guidebook.GuidebookEntry;
import dev.caecorthus.sparkassist.guidebook.GuidebookNavigation;
import java.util.List;

/**
 * Picks the decoration set for a page or a player. Pages resolve by tab and faction group (the directory's own
 * grouping, so the two never disagree), then take their own id (for their plate art and emblem) and theme colour,
 * and apply the entry's JSON overrides; players resolve the same way from their role id. Anything unknown,
 * including the credits page, rides the train: the civilian set and its viaduct.
 * 为页面或玩家选择点缀套别。页面按标签与阵营分组（沿用目录自身的分组，二者不会打架）解析，再带上自己的 id（取扉画
 * 与徽记）与主题色，最后套用条目 JSON 的覆盖；玩家由身份 id 以同样方式解析。未知情况（含鸣谢页）一律用好人套与它的
 * 高架桥。
 */
public final class DecorSetResolver {
    private DecorSetResolver() {
    }

    /** The set for one page: its id and theme colour. 某一页的套别：条目 id 与主题色。 */
    private static DecorSet personal(DecorSet base, String id, int theme) {
        return base.withPage(id, theme);
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
        int theme = entry.color() == GuidebookEntry.DEFAULT_COLOR ? base.colour() : entry.color();
        return personal(base, entry.id(), theme).withOverrides(entry.decor());
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
     * train. 由玩家身份 id 得到自身套别，指向该身份的页面，着阵营色（持有身份条目的调用方改用 forEntry 取得主题色）；
     * 没有身份时用好人套。 */
    public static DecorSet forRole(String roleId) {
        if (roleId == null) {
            return DecorSet.CIVILIAN;
        }
        DecorSet base = forGroup(GuidebookNavigation.roleGroup(roleId));
        return personal(base, roleId, base.colour());
    }

    public static DecorSet forCredits() {
        return DecorSet.CIVILIAN;
    }
}
