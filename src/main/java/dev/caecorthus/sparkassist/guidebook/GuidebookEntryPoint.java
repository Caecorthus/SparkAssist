package dev.caecorthus.sparkassist.guidebook;

/**
 * Where the player reaches the guide in their current state. Both are behind the inventory key.
 * 玩家在当前状态下打开指南的入口。两者都在背包键之后。
 */
public enum GuidebookEntryPoint {
    /** Embedded in Wathe's limited inventory. 内嵌在 Wathe 受限背包中。 */
    LIMITED_INVENTORY,
    /** The book button at the top-right of the inventory the key opens. 背包键打开的背包界面右上角的书本按钮。 */
    INVENTORY_BUTTON;

    /**
     * Living round players get Wathe's limited inventory with the guide built in. Everyone else (lobby, outside a
     * round, dead spectators, creative) opens their usual inventory, which carries the book button in its top-right
     * corner; the pause menu no longer has one.
     * 对局中存活的玩家使用内嵌指南的 Wathe 受限背包。其他人（大厅、对局外、死亡旁观者、创造模式）打开各自平常的背包，
     * 背包界面右上角有书本按钮；暂停菜单不再提供书本入口。
     */
    public static GuidebookEntryPoint resolve(boolean roundHud, boolean aliveInSurvival) {
        return roundHud && aliveInSurvival ? LIMITED_INVENTORY : INVENTORY_BUTTON;
    }
}
