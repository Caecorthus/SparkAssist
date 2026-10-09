package dev.caecorthus.sparkassist.client.achievement;

import dev.caecorthus.sparkassist.SparkAssist;
import dev.caecorthus.sparkassist.achievement.Achievement;
import dev.caecorthus.sparkassist.achievement.AchievementCatalog;
import dev.caecorthus.sparkassist.achievement.AchievementLayout;
import dev.caecorthus.sparkassist.achievement.AchievementLedger;
import dev.caecorthus.sparkassist.client.mixin.ClientAdvancementManagerAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.AdvancementManager;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.advancement.criterion.ImpossibleCriterion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Shows earned achievements as one extra page in the vanilla advancements screen. The page and its entries live only
 * in this client's advancement tree: they are added back every time the server resends its advancements, the tab
 * selection is never reported to the server, and nothing exists until the first achievement is earned. Every entry
 * has a single, already obtained criterion, so vanilla never draws a progress bar.
 * 在原版成就界面里以额外一页展示已达成的成就。这一页及其条目只存在于本客户端的进度树中：服务器每次重发进度后都会补回，
 * 切到这一页不会告知服务器，第一个成就达成之前这一页不存在。每个条目只有一个已完成的条件，所以原版从不绘制进度条。
 */
public final class AchievementAdvancements {
    public static final Identifier ROOT_ID = SparkAssist.id("achievements/root");
    // Tiled 16 × 16 GUI pixels by vanilla; the image may be any square size. 原版按 16 × 16 GUI 像素平铺；贴图可为任意正方形尺寸。
    private static final Identifier BACKGROUND = SparkAssist.id("textures/gui/achievements/background.png");
    private static final String CRITERION = "earned";
    // "Level x · <description>". “Level x · <描述>”。
    private static final String DESCRIPTION_KEY = "advancements.sparkassist.achievement.description";

    private AchievementAdvancements() {
    }

    public static boolean isOurs(Identifier id) {
        return id != null && SparkAssist.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith("achievements/");
    }

    /**
     * Adds every earned achievement still missing from the client's advancement tree.
     * 把客户端进度树中缺少的已达成成就补上。
     */
    public static void restore(ClientAdvancementManager manager) {
        List<AdvancementEntry> wanted = entries(AchievementStorage.ledger(MinecraftClient.getInstance()));
        if (wanted.isEmpty()) {
            return;
        }
        AdvancementManager tree = manager.getManager();
        List<AdvancementEntry> missing = wanted.stream().filter(entry -> tree.get(entry.id()) == null).toList();
        if (!missing.isEmpty()) {
            tree.addAll(missing);
        }
        ClientAdvancementManagerAccessor access = (ClientAdvancementManagerAccessor) manager;
        Map<AdvancementEntry, AdvancementProgress> progresses = access.sparkassist$progresses();
        for (AdvancementEntry wantedEntry : wanted) {
            PlacedAdvancement placed = tree.get(wantedEntry.id());
            if (placed == null || progresses.containsKey(placed.getAdvancementEntry())) {
                continue;
            }
            AdvancementProgress progress = new AdvancementProgress();
            progress.init(placed.getAdvancement().requirements());
            progress.obtain(CRITERION);
            progresses.put(placed.getAdvancementEntry(), progress);
            if (access.sparkassist$listener() != null) {
                access.sparkassist$listener().setProgress(placed, progress);
            }
        }
    }

    /**
     * Puts newly earned achievements on the page and shows a vanilla-style toast headed "Level x 成就达成" for each.
     * 把新达成的成就放上成就页，并为每个显示原版样式、标题为“Level x 成就达成”的弹窗。
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
            client.getToastManager().add(new AchievementToast(
                    icon(achievement.icon()), Text.literal(achievement.title()), achievement.level()));
        }
    }

    /** The root plus every earned, still-known achievement, in unlock order. 根节点加上所有已达成且仍存在的成就，按解锁顺序。 */
    private static List<AdvancementEntry> entries(AchievementLedger ledger) {
        List<Achievement> earned = new ArrayList<>();
        for (AchievementLedger.Unlock unlock : ledger.unlocks()) {
            AchievementCatalog.byId(unlock.id()).ifPresent(earned::add);
        }
        if (earned.isEmpty()) {
            return List.of();
        }
        AdvancementEntry root = root();
        List<AdvancementEntry> placed = new ArrayList<>(earned.size());
        for (int i = 0; i < earned.size(); i++) {
            AchievementLayout.Slot slot = AchievementLayout.slot(i);
            AdvancementEntry parent = slot.parentIndex() < 0 ? root : placed.get(slot.parentIndex());
            placed.add(entry(earned.get(i), slot, parent));
        }
        List<AdvancementEntry> entries = new ArrayList<>(placed.size() + 1);
        entries.add(root);
        entries.addAll(placed);
        return entries;
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
        return build(ROOT_ID, null, display);
    }

    private static AdvancementEntry entry(Achievement achievement, AchievementLayout.Slot slot, AdvancementEntry parent) {
        AdvancementDisplay display = new AdvancementDisplay(
                icon(achievement.icon()),
                Text.literal(achievement.title()),
                Text.translatable(DESCRIPTION_KEY, achievement.level(), achievement.description()),
                Optional.empty(),
                switch (achievement.frame()) {
                    case TASK -> AdvancementFrame.TASK;
                    case GOAL -> AdvancementFrame.GOAL;
                    case CHALLENGE -> AdvancementFrame.CHALLENGE;
                },
                true,
                false,
                false);
        display.setPos(slot.x(), slot.y());
        return build(idFor(achievement.id()), parent, display);
    }

    private static AdvancementEntry build(Identifier id, AdvancementEntry parent, AdvancementDisplay display) {
        Advancement.Builder builder = Advancement.Builder.createUntelemetered()
                .display(display)
                .criterion(CRITERION, Criteria.IMPOSSIBLE.create(new ImpossibleCriterion.Conditions()));
        if (parent != null) {
            builder.parent(parent);
        }
        return builder.build(id);
    }

    private static Identifier idFor(String achievementId) {
        return SparkAssist.id("achievements/" + achievementId);
    }

    private static ItemStack icon(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        Item item = id == null ? Items.AIR : Registries.ITEM.get(id);
        return new ItemStack(item == Items.AIR ? Items.PAPER : item);
    }
}
