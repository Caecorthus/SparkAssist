package dev.caecorthus.sparkassist.guidebook.decor;

import dev.caecorthus.sparkassist.guidebook.GuidebookEntry;
import dev.caecorthus.sparkassist.guidebook.GuidebookNavigation;
import java.util.List;

/**
 * Picks the decoration set for a page or a player. Pages resolve by tab and faction group (the directory's own
 * grouping, so the two never disagree), then apply the entry's JSON overrides; players resolve by their role's
 * faction group. Anything unknown, including the credits page and the basics, rides the train: the civilian set.
 * 为页面或玩家选择点缀套别。页面按标签与阵营分组（沿用目录自身的分组，二者不会打架）解析，再套用条目 JSON 的覆盖；
 * 玩家按身份所属阵营解析。未知情况（含鸣谢页与基础规则）一律用好人套，也就是那列火车。
 */
public final class DecorSetResolver {
    private DecorSetResolver() {
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
        return base.withOverrides(entry.decor());
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

    /** The player's own set from their role id; null (lobby, spectator without a role) rides the train.
     * 由玩家身份 id 得到自身套别；没有身份时用好人套。 */
    public static DecorSet forRole(String roleId) {
        if (roleId == null) {
            return DecorSet.CIVILIAN;
        }
        return forGroup(GuidebookNavigation.roleGroup(roleId));
    }

    public static DecorSet forCredits() {
        return DecorSet.CIVILIAN;
    }
}
