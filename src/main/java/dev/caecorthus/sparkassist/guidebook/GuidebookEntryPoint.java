package dev.caecorthus.sparkassist.guidebook;

/**
 * Where the player reaches the guide in their current state.
 * 玩家在当前状态下打开指南的入口。
 */
public enum GuidebookEntryPoint {
    /** Embedded in Wathe's limited inventory. 内嵌在 Wathe 受限背包中。 */
    LIMITED_INVENTORY,
    /** The inventory key opens the full guide instead of the vanilla inventory. 背包键直接打开完整指南，取代原版背包。 */
    INVENTORY_KEY,
    /** The book button in the pause menu. 暂停菜单中的书本按钮。 */
    PAUSE_MENU;

    /**
     * Dead players spectate the round with an empty inventory, so their inventory key leads to the guide; lobby and
     * creative players keep the pause-menu button.
     * 死者以旁观身份观看对局且背包为空，因此背包键改为打开指南；大厅与创造模式玩家仍使用暂停菜单按钮。
     */
    public static GuidebookEntryPoint resolve(boolean roundHud, boolean aliveInSurvival, boolean spectator) {
        if (roundHud && aliveInSurvival) {
            return LIMITED_INVENTORY;
        }
        if (roundHud && spectator) {
            return INVENTORY_KEY;
        }
        return PAUSE_MENU;
    }
}
