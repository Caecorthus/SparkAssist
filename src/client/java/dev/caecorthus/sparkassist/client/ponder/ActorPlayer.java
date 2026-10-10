package dev.caecorthus.sparkassist.client.ponder;

import com.mojang.authlib.GameProfile;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import java.util.UUID;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

/**
 * The player entity a demo actor is drawn with. It never joins the world: {@link ActorElement} sets its pose
 * fields each tick and hands it to the vanilla player renderer, so Wathe's own arm poses (the knife's charge, the
 * levelled gun) and held-item rendering apply exactly as in a round. Skin = the role colour; no vanilla name tag.
 * 演示演员绘制时使用的玩家实体。它从不加入世界：ActorElement 每 tick 设置它的姿态字段，再交给原版玩家渲染器，
 * 因此 Wathe 自己的手臂姿势（刀的蓄力、端平的枪）和手持物品渲染与对局中完全一致。皮肤为身份颜色，不显示原版名牌。
 * Public (with {@link #texture} and {@link #isDrawing}) only for SparkAssist's mixins that keep the reader's role views
 * off actors; it never touches Ponder, so those mixins load without it.
 * 仅为让 SparkAssist 的 mixin（把读者身份视角挡在演员之外）能访问而设为公开；本类不引用 Ponder，那些 mixin 在未安装
 * Ponder 时也能加载。
 */
public final class ActorPlayer extends OtherClientPlayerEntity {
    /** Wathe's psycho skin for a player without a psycho cosmetic. 没有疯魔外观时 Wathe 使用的疯魔皮肤。 */
    private static final Identifier PSYCHO_SKIN = Identifier.of("wathe", "textures/entity/psycho.png");
    private static boolean drawing;
    private SkinTextures skin;
    boolean charging;

    ActorPlayer(ClientWorld world, int color) {
        super(world, new GameProfile(UUID.randomUUID(), "demo_actor"));
        recolor(color);
    }

    /** Switch to the skin of another colour, keeping the stride and swing in progress. 换成另一种颜色的皮肤，保留进行中的步态与挥手。 */
    void recolor(int color) {
        skin = new SkinTextures(ActorSkins.of(color), null, null, null, SkinTextures.Model.WIDE, false);
    }

    @Override
    public SkinTextures getSkinTextures() {
        return skin;
    }

    /** The texture to draw: the role colour, or Wathe's psycho skin in psycho mode. 要绘制的纹理：身份颜色，疯魔模式时为 Wathe 的疯魔皮肤。 */
    public Identifier texture() {
        return PlayerPsychoComponent.KEY.get(this).getPsychoTicks() > 0 ? PSYCHO_SKIN : skin.texture();
    }

    /** Whether an actor is being drawn right now. 此刻是否正在绘制演员。 */
    public static boolean isDrawing() {
        return drawing;
    }

    static void setDrawing(boolean drawing) {
        ActorPlayer.drawing = drawing;
    }

    /** Outer skin layers show as on a real player (the psycho skin's hood and sleeves). 外层皮肤与真实玩家一样显示（疯魔皮肤的兜帽与袖子）。 */
    @Override
    public boolean isPartVisible(PlayerModelPart modelPart) {
        return true;
    }

    // Charging = using the main-hand item, which the renderer turns into that item's use pose (the knife's spear
    // pose). Overridden instead of going through the synced living flags, which only the server sets.
    // 蓄力即正在使用主手物品，渲染器会据此换成该物品的使用姿势（刀为掷矛姿势）。直接覆写，而不经由只有服务端才会
    // 设置的同步状态位。

    @Override
    public boolean isUsingItem() {
        return charging;
    }

    @Override
    public Hand getActiveHand() {
        return Hand.MAIN_HAND;
    }

    @Override
    public int getItemUseTimeLeft() {
        return charging ? getMainHandStack().getMaxUseTime(this) : 0;
    }

    /** Item models that check the item in use (a raised shield, a drawn bow) see the main-hand stack. 检查使用中物品的物品模型（举起的盾、拉开的弓）会看到主手物品。 */
    @Override
    public ItemStack getActiveItem() {
        return charging ? getMainHandStack() : ItemStack.EMPTY;
    }

    /** Hides the vanilla name tag; the actor draws its own in the role colour. 隐藏原版名牌；演员自己绘制身份色名牌。 */
    @Override
    public boolean isInvisibleTo(PlayerEntity player) {
        return true;
    }
}
