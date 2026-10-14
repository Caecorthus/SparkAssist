package dev.caecorthus.sparkassist.client.achievement;

import dev.caecorthus.sparkassist.SparkAssist;
import dev.caecorthus.sparkassist.achievement.Achievement;
import dev.caecorthus.sparkassist.achievement.AchievementCatalog;
import dev.caecorthus.sparkassist.achievement.AchievementLedger;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.AdvancementManager;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.advancement.criterion.ImpossibleCriterion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Puts the achievement page in the vanilla advancements screen as one extra tab. The tab is a single root that lives
 * only in this client's advancement tree: it is added back every time the server resends its advancements, selecting
 * it is never reported to the server, and it does not exist until the first achievement is earned. Selecting it opens
 * {@link AchievementGalaxyScreen}, which draws the page full screen.
 * 在原版成就界面里以额外一页放入成就页。这一页只是一个根节点，只存在于本客户端的进度树中：服务器每次重发进度后都会补回，
 * 选中它不会告知服务器，第一个成就达成之前它不存在。选中它会打开全屏绘制成就页的 {@link AchievementGalaxyScreen}。
 */
public final class AchievementAdvancements {
    public static final Identifier ROOT_ID = SparkAssist.id("achievements/root");
    // Shown by vanilla only if the galaxy screen fails to open. 仅在银河界面没能打开时由原版显示。
    private static final Identifier BACKGROUND = SparkAssist.id("textures/gui/achievements/background.png");
    private static final String CRITERION = "earned";

    private AchievementAdvancements() {
    }

    public static boolean isOurs(Identifier id) {
        return id != null && SparkAssist.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith("achievements/");
    }

    /**
     * Adds the tab to the client's advancement tree once an achievement is earned.
     * 一旦有成就达成，就把这一页补进客户端的进度树。
     */
    public static void restore(ClientAdvancementManager manager) {
        if (!hasEarnedAny(AchievementStorage.ledger(MinecraftClient.getInstance()))) {
            return;
        }
        AdvancementManager tree = manager.getManager();
        if (tree.get(ROOT_ID) == null) {
            tree.addAll(List.of(root()));
        }
    }

    /**
     * Makes sure the tab exists and shows a vanilla-style toast headed "Level x 成就达成" for each new achievement.
     * 确保这一页存在，并为每个新成就显示原版样式、标题为“Level x 成就达成”的弹窗。
     */
    public static void announce(MinecraftClient client, List<Achievement> earned) {
        if (earned.isEmpty()) {
            return;
        }
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler != null) {
            restore(handler.getAdvancementHandler());
        }
        for (Achievement achievement : earned) {
            client.getToastManager().add(new AchievementToast(icon(achievement.icon()), title(achievement), achievement.level()));
        }
    }

    /** The title in its level's colour. 以等级配色显示的标题。 */
    static MutableText title(Achievement achievement) {
        return Text.literal(achievement.title()).formatted(levelColor(achievement.level()));
    }

    /**
     * Rarity colours by level, from common white to legendary gold for the top level.
     * 按等级的稀有度配色，从普通的白色到最高等级传说的金色。
     */
    static Formatting levelColor(int level) {
        return switch (level) {
            case 1 -> Formatting.WHITE;
            case 2 -> Formatting.GREEN;
            case 3 -> Formatting.AQUA;
            case 4 -> Formatting.LIGHT_PURPLE;
            case 5 -> Formatting.RED;
            default -> Formatting.GOLD;
        };
    }

    static ItemStack icon(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        Item item = id == null ? Items.AIR : Registries.ITEM.get(id);
        return new ItemStack(item == Items.AIR ? Items.PAPER : item);
    }

    private static boolean hasEarnedAny(AchievementLedger ledger) {
        return ledger.unlocks().stream().anyMatch(unlock -> AchievementCatalog.byId(unlock.id()).isPresent());
    }

    private static AdvancementEntry root() {
        AdvancementDisplay display = new AdvancementDisplay(
                new ItemStack(Items.MINECART),
                Text.literal("Spark"),
                Text.literal("走过的路，才会出现在这里。"),
                Optional.of(BACKGROUND),
                AdvancementFrame.TASK,
                false,
                false,
                false);
        display.setPos(0, 0);
        return Advancement.Builder.createUntelemetered()
                .display(display)
                .criterion(CRITERION, Criteria.IMPOSSIBLE.create(new ImpossibleCriterion.Conditions()))
                .build(ROOT_ID);
    }
}
