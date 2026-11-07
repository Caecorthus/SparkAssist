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
 * Charge meter for any held item that charges, drawn between the action bar and the hotbar.
 * 在动作栏与快捷栏之间绘制任意蓄力道具的蓄力条。
 *
 * <p>Charging items all share one signal: they set a positive {@code getMaxUseTime}, because Minecraft only keeps an
 * item in use while that value is above zero. Wathe's knife, SparkWitch's shuriken, swordfish, cross-sword, rifle,
 * vendetta knife and potion launcher all take that route, so the meter keys off it instead of an item list, and food
 * and potions are excluded because they also report a use time without charging anything.
 * 蓄力道具共用一个信号：{@code getMaxUseTime} 为正——Minecraft 只有在该值大于零时才让物品保持使用状态。Wathe 的刀、
 * SparkWitch 的手里剑、剑鱼、十字剑、步枪、复仇刀、药水发射器都走这条路，因此这里按该信号判断而不是维护物品清单；
 * 食物与药水被排除，因为它们同样报告使用时间但并不蓄力。</p>
 *
 * <p>The knife caps its own gauge at the 10-tick minimum stab charge; everything else fills across its full use time,
 * since only the knife is known to have a meaningful in-between threshold.
 * 刀把仪表上限压到 10 tick 的最短可刺蓄力；其余物品按完整使用时间填充，因为目前只有刀已知存在有意义的中途阈值。</p>
 */
public final class KnifeChargeHud {
    private static final int BAR_WIDTH = 208;
    private static final int BAR_HEIGHT = 3;
    /** Wathe refuses a stab below this many ticks; the bar spans exactly this window. Wathe 低于此 tick 不允许刺杀，进度条正好覆盖这一段。 */
    private static final int MIN_CHARGE_TICKS = 10;
    /** SparkWitch's {@code FisherRules.SWORDFISH_MIN_HOLD_TICKS}. SparkWitch 的 {@code FisherRules.SWORDFISH_MIN_HOLD_TICKS}。 */
    private static final int SWORDFISH_MIN_CHARGE_TICKS = 11;
    /** SparkStrength's {@code M67Rules.CHARGE_TICKS}. SparkStrength 的 {@code M67Rules.CHARGE_TICKS}。 */
    private static final int M67_CHARGE_TICKS = 20;
    /** SparkWitch's {@code NinjaShurikenItem.MIN_CHARGE_TICKS}; its use time is a 7200-tick hold limit, not a charge. SparkWitch 的 {@code NinjaShurikenItem.MIN_CHARGE_TICKS}；它的使用时间是 7200 tick 的按住上限，不是蓄力时长。 */
    private static final int SHURIKEN_MIN_CHARGE_TICKS = 4;
    private static final String SWORDFISH_CLASS =
            "dev.caecorthus.sparkwitch.roles.civilian.fisher.swordfish.SwordfishItem";
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
     * <p>Three different mechanisms feed this bar, because the items called "charged" in this pack are not all charged:
     * a held charge ({@code isUsingItem} plus a positive use time), the vanilla attack cooldown the bat gates kills on,
     * and the flat item cooldown the grenade runs after a throw.
     * 三种机制喂这条进度条，因为这个整合包里被叫作「蓄力」的道具并不都是蓄力：按住的使用时间（{@code isUsingItem} 且
     * 使用时间为正）、球棒据以判定击杀的原版攻击冷却、以及手雷投出后那份固定的物品冷却。</p>
     */
    private static float progressOf(ClientPlayerEntity player, ItemStack stack) {
        if (!SparkAssistClientSettings.chargeBar()) {
            return -1.0F;
        }
        Item item = stack.getItem();
        if (item instanceof KnifeItem) {
            return chargeProgress(player, stack, MIN_CHARGE_TICKS);
        }
        // SparkWitch's swordfish is the knife pattern exactly: a 100-tick use window with an 11-tick minimum.
        // SparkWitch 的剑鱼和刀是同一套：100 tick 使用窗口，最小有效 11 tick。
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
            return chargeProgress(player, stack, SHURIKEN_MIN_CHARGE_TICKS);
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
