package dev.caecorthus.sparkassist.client.charge;

import dev.caecorthus.sparkassist.client.charge.ChargeItems.Kind;
import dev.caecorthus.sparkassist.client.config.SparkAssistClientSettings;
import dev.caecorthus.sparkassist.config.ChargeIndicatorStyle;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * One charge indicator for every weapon that has to be wound up: it fills while the weapon is raised and turns bright
 * on the first tick a release will count. Nothing in Wathe shows this today — its knife glyph under the crosshair is a
 * cooldown gauge and simply reads full during the raise.
 * 所有需要蓄力的武器共用的一个蓄力指示：举起武器时逐渐填充，在松手开始有效的第一刻变亮。Wathe 目前没有任何东西显示这一点——
 * 它准星下的刀图标是冷却仪表，举刀期间只是一直读满。
 *
 * <p>It is drawn only while a listed weapon is actually being raised, so it never sits on the screen between uses,
 * and only where Wathe's own round HUD is showing.
 * 只在清单上的武器确实被举起时绘制，因此两次使用之间不会停留在屏幕上；并且只在 Wathe 自身的对局 HUD 显示时绘制。</p>
 */
public final class ChargeIndicatorHud {
    /** NoellesRoles' Scavenger stabs on the click itself and never winds the knife up. NoellesRoles 的清道夫点击即出刀，从不蓄力。 */
    private static final Identifier SCAVENGER = Identifier.of("noellesroles", "scavenger");

    private ChargeIndicatorHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(ChargeIndicatorHud::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        ChargeIndicatorStyle style = SparkAssistClientSettings.chargeIndicatorStyle();
        if (style == ChargeIndicatorStyle.OFF) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        // Cheapest exit first: on almost every frame nothing is being used. 先做开销最小的判断：绝大多数帧里并没有在使用物品。
        if (player == null || !player.isUsingItem()) {
            return;
        }
        ItemStack stack = player.getActiveItem();
        Kind kind = ChargeItems.kindOf(stack.getItem());
        if (kind == null) {
            return;
        }
        // HudRenderCallback fires even with F1, unlike Wathe's own HUD layer; follow Wathe's train HUD, and keep out
        // of third person, where Wathe draws no crosshair to hang the indicator on.
        // HudRenderCallback 在 F1 下仍会触发，与 Wathe 自身 HUD 层不同；跟随 Wathe 列车 HUD 的显示状态，并且不在第三人称下
        // 绘制——那时 Wathe 不画准星，指示无处可依。
        if (client.options.hudHidden || !client.options.getPerspective().isFirstPerson()
                || WatheClient.trainComponent == null || !WatheClient.trainComponent.hasHud()
                || !GameFunctions.isPlayerPlayingAndAlive(player)) {
            return;
        }
        if (kind == Kind.KNIFE && isScavenger(player)) {
            return;
        }
        // A forced-melee lock throws the release away, and SparkTraits already puts its own mark under the crosshair.
        // 强制近战锁定会作废这次松手，而 SparkTraits 已经在准星下画了自己的标记。
        if (SparkTraitsCharge.meleeLocked(player, stack)) {
            return;
        }
        ChargeIndicatorRenderer.draw(context, style, ChargeMeter.read(player.getItemUseTime(),
                tickCounter.getTickDelta(false), ChargeItems.window(kind, player, stack)));
    }

    private static boolean isScavenger(ClientPlayerEntity player) {
        Role role = GameWorldComponent.KEY.maybeGet(player.getWorld()).map(game -> game.getRole(player)).orElse(null);
        return role != null && SCAVENGER.equals(role.identifier());
    }
}
