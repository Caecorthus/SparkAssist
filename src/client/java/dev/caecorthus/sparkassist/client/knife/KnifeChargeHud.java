package dev.caecorthus.sparkassist.client.knife;

import dev.caecorthus.sparkassist.client.config.SparkAssistClientSettings;
import dev.doctor4t.wathe.item.BatItem;
import dev.doctor4t.wathe.item.KnifeItem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Charge meter for the held weapons that have to be wound up, drawn between the action bar and the hotbar.
 * 在动作栏与快捷栏之间为需要蓄力的手持武器绘制蓄力条。
 *
 * <p>Each item is listed with the hold its own release logic asks for, because {@code getMaxUseTime} is only how long
 * the item may be held — 100 ticks for the knife, 7200 for the shuriken, {@code Integer.MAX_VALUE} for the M67 — and
 * says nothing about when a release starts to count. The bar reads full on the first tick a release succeeds.
 * 每个道具都按其自身松手逻辑要求的按住时长列出，因为 {@code getMaxUseTime} 只是物品最多能按住多久——刀 100 tick、
 * 手里剑 7200、M67 为 {@code Integer.MAX_VALUE}——并不说明松手从何时起有效。进度条在松手首次有效的那一 tick 读满。</p>
 *
 * <p>Known gap: SparkTraits' Seasoned and Close Quarters traits shorten the knife's windup, and SparkTraits exposes no
 * call for it yet, so with either trait the knife is ready before the bar is full.
 * 已知缺口：SparkTraits 的「老练」与「狭路相逢」天赋会缩短刀的前摇，而 SparkTraits 目前没有对应的查询接口，因此带这两个天赋时
 * 刀会在进度条读满之前就绪。</p>
 */
public final class KnifeChargeHud {
    private static final int BAR_WIDTH = 208;
    private static final int BAR_HEIGHT = 3;
    /**
     * Wathe's {@code KnifeItem.onStoppedUsing} returns unless more than 10 ticks have elapsed, so the first release
     * that stabs is on tick 11.
     * Wathe 的 {@code KnifeItem.onStoppedUsing} 要求已按住超过 10 tick，因此第一个能刺出的松手是第 11 tick。
     */
    private static final int KNIFE_CHARGE_TICKS = 11;
    /** SparkWitch's {@code VendettaKnifeRules.MINIMUM_HOLD_TICKS}; unlike the knife it extends, 10 ticks is enough. SparkWitch 的 {@code VendettaKnifeRules.MINIMUM_HOLD_TICKS}；与它继承的刀不同，按满 10 tick 即可。 */
    private static final int VENDETTA_KNIFE_CHARGE_TICKS = 10;
    /** SparkWitch's {@code FisherRules.SWORDFISH_MIN_HOLD_TICKS}. SparkWitch 的 {@code FisherRules.SWORDFISH_MIN_HOLD_TICKS}。 */
    private static final int SWORDFISH_MIN_CHARGE_TICKS = 11;
    /** SparkStrength's {@code M67Rules.CHARGE_TICKS}. SparkStrength 的 {@code M67Rules.CHARGE_TICKS}。 */
    private static final int M67_CHARGE_TICKS = 20;
    /** SparkWitch's {@code NinjaShurikenItem.MIN_CHARGE_TICKS}; its use time is a 7200-tick hold limit, not a charge. SparkWitch 的 {@code NinjaShurikenItem.MIN_CHARGE_TICKS}；它的使用时间是 7200 tick 的按住上限，不是蓄力时长。 */
    private static final int SHURIKEN_MIN_CHARGE_TICKS = 4;
    private static final String SWORDFISH_CLASS =
            "dev.caecorthus.sparkwitch.roles.civilian.fisher.swordfish.SwordfishItem";
    private static final String VENDETTA_KNIFE_CLASS =
            "dev.caecorthus.sparkwitch.roles.civilian.vendetta.VendettaKnifeItem";
    private static final String M67_CLASS = "annina.sparkstrength.item.M67Item";
    private static final String SHURIKEN_CLASS = "dev.caecorthus.sparkwitch.item.ninja.NinjaShurikenItem";

    private static final int BACKDROP = 0xC0101010;
    private static final int BORDER = 0xFFC5A244;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private KnifeChargeHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(KnifeChargeHud::render);
    }

    private static void render(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.options.hudHidden) {
            return;
        }
        ItemStack stack = player.getMainHandStack();
        float progress = progressOf(player, stack);
        if (progress < 0.0F) {
            return;
        }

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int left = width / 2 - BAR_WIDTH / 2;
        // The hotbar's top edge sits at height - 22 and the action bar around height - 72, so this lands between them.
        // 快捷栏上沿在 height - 22，动作栏约在 height - 72，这条正好落在两者之间。
        int top = height - 50;

        // Frame and track. 边框与底槽。
        context.fill(left - 1, top - 1, left + BAR_WIDTH + 1, top + BAR_HEIGHT + 1, BORDER);
        context.fill(left, top, left + BAR_WIDTH, top + BAR_HEIGHT, BACKDROP);

        // Filled portion, red at empty through amber to green at full. 填充段：空为红，经琥珀，满为绿。
        int filled = Math.round(BAR_WIDTH * progress);
        if (filled > 0) {
            context.fill(left, top, left + filled, top + BAR_HEIGHT, chargeColor(progress));
        }

        // Percentage centred on the bar. 条中央的百分比。
        Text label = Text.literal(Math.round(progress * 100.0F) + "%");
        TextRenderer font = client.textRenderer;
        int textY = top + (BAR_HEIGHT - font.fontHeight) / 2;
        context.drawTextWithShadow(font, label, width / 2 - font.getWidth(label) / 2, textY, TEXT_COLOR);
    }

    /**
     * The progress to show, or a negative value when nothing should be drawn.
     * 要显示的进度；负数表示不应绘制。
     *
     * <p>Two mechanisms feed this bar, because the items called "charged" in this pack are not all charged: a held
     * charge ({@code isUsingItem} measured against the item's own threshold) and the attack cooldown the bat gates
     * kills on.
     * 两种机制喂这条进度条，因为这个整合包里被叫作「蓄力」的道具并不都是蓄力：按住蓄力（{@code isUsingItem}，对照道具自身的
     * 阈值）以及球棒据以判定击杀的攻击冷却。</p>
     */
    private static float progressOf(ClientPlayerEntity player, ItemStack stack) {
        if (!SparkAssistClientSettings.chargeBar()) {
            return -1.0F;
        }
        Item item = stack.getItem();
        if (item instanceof KnifeItem) {
            // The Vendetta knife extends KnifeItem but replaces the release check with its own, one tick shorter.
            // 复仇刀继承自 KnifeItem，但用自己的松手判定替换了原判定，短一 tick。
            return chargeProgress(player, stack,
                    isVendettaKnife(item) ? VENDETTA_KNIFE_CHARGE_TICKS : KNIFE_CHARGE_TICKS);
        }
        // SparkWitch's swordfish follows the knife: a 100-tick use window, first valid release on tick 11.
        // SparkWitch 的剑鱼与刀一致：100 tick 使用窗口，第 11 tick 起松手有效。
        if (isSwordfish(item)) {
            return chargeProgress(player, stack, SWORDFISH_MIN_CHARGE_TICKS);
        }
        // SparkStrength's M67 reports Integer.MAX_VALUE as its use time — holding never completes — so the charge
        // window has to come from its own rule rather than from the item.
        // SparkStrength 的 M67 把使用时间报成 Integer.MAX_VALUE——按住永远不会完成——所以蓄力窗口必须来自它自己的规则，
        // 而不是物品本身。
        if (isM67(item)) {
            // The window is not fixed: Herculean Strength shortens it to 70%, decided server-side from synced state.
            // 窗口不是固定的：力气天赋会把它缩短到 70%，由服务端依据同步状态判定。
            return chargeProgress(player, stack, SparkTraitsCharge.adjusted(player, M67_CHARGE_TICKS));
        }
        // The shuriken's use time is 7200 ticks — a hold limit, not a charge — so dividing by it would always read 0%.
        // 手里剑的使用时间是 7200 tick——那是按住上限而不是蓄力时长——拿它作分母永远显示 0%。
        if (isShuriken(item)) {
            // SparkWitch tags the shuriken as a SparkTraits throwable, so Herculean Strength shortens it like the M67.
            // SparkWitch 把手里剑标记为 SparkTraits 的投掷物，因此「力大无穷」天赋会像对 M67 一样缩短它。
            return chargeProgress(player, stack, SparkTraitsCharge.adjusted(player, SHURIKEN_MIN_CHARGE_TICKS));
        }
        if (item instanceof BatItem) {
            // Vanilla already draws an attack indicator under the crosshair, so this bar is optional.
            // 原版准星下方已经画了攻击指示器，因此这条是可选的。
            if (!SparkAssistClientSettings.batChargeBar()) {
                return -1.0F;
            }
            float cooldown = player.getAttackCooldownProgress(0.5F);
            // Hide once recovered: a permanently full bar would sit on the screen for the whole round.
            // 冷却完成后隐藏：常驻的满格条会一直挡在屏幕上。
            return cooldown >= 0.999F ? -1.0F : cooldown;
        }
        return -1.0F;
    }

    /** Progress toward {@code threshold} ticks of holding, or -1 when the item is not being used. 按住 {@code threshold} tick 的进度；未在使用时返回 -1。 */
    private static float chargeProgress(ClientPlayerEntity player, ItemStack stack, int threshold) {
        if (!player.isUsingItem() || stack != player.getActiveItem() || threshold <= 0) {
            return -1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, player.getItemUseTime() / (float) threshold));
    }

    /** Matched by class name so SparkAssist keeps no compile-time dependency on SparkWitch. 按类名匹配，使 SparkAssist 不产生对 SparkWitch 的编译期依赖。 */
    private static boolean isSwordfish(Item item) {
        return SWORDFISH_CLASS.equals(item.getClass().getName());
    }

    private static boolean isVendettaKnife(Item item) {
        return VENDETTA_KNIFE_CLASS.equals(item.getClass().getName());
    }

    /** Same reasoning for SparkStrength. SparkStrength 同理。 */
    private static boolean isM67(Item item) {
        return M67_CLASS.equals(item.getClass().getName());
    }

    /** And for SparkWitch's shuriken. SparkWitch 的手里剑同理。 */
    private static boolean isShuriken(Item item) {
        return SHURIKEN_CLASS.equals(item.getClass().getName());
    }

    /**
     * Red to green through amber, so the bar reads as a gauge rather than a single interpolated hue.
     * 红经琥珀到绿，读起来像仪表而不是单一色相插值。
     */
    private static int chargeColor(float progress) {
        float red;
        float green;
        if (progress < 0.5F) {
            float t = progress / 0.5F;
            red = 1.0F;
            green = 0.23F + t * 0.57F;
        } else {
            float t = (progress - 0.5F) / 0.5F;
            red = 1.0F - t * 0.80F;
            green = 0.80F + t * 0.04F;
        }
        float blue = 0.10F + progress * 0.10F;
        return 0xFF000000 | ((int) (red * 255) << 16) | ((int) (green * 255) << 8) | (int) (blue * 255);
    }
}
