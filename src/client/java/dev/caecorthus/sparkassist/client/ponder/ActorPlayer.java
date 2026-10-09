package dev.caecorthus.sparkassist.client.ponder;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

/**
 * The player entity a demo actor is drawn with. It never joins the world: {@link ActorElement} sets its pose
 * fields each tick and hands it to the vanilla player renderer, so Wathe's own arm poses (the knife's charge, the
 * levelled gun) and held-item rendering apply exactly as in a round. Skin = the role colour; no vanilla name tag.
 * 演示演员绘制时使用的玩家实体。它从不加入世界：ActorElement 每 tick 设置它的姿态字段，再交给原版玩家渲染器，
 * 因此 Wathe 自己的手臂姿势（刀的蓄力、端平的枪）和手持物品渲染与对局中完全一致。皮肤为身份颜色，不显示原版名牌。
 */
final class ActorPlayer extends OtherClientPlayerEntity {
    private final SkinTextures skin;
    boolean charging;

    ActorPlayer(ClientWorld world, int color) {
        super(world, new GameProfile(UUID.randomUUID(), "demo_actor"));
        this.skin = new SkinTextures(ActorSkins.of(color), null, null, null, SkinTextures.Model.WIDE, false);
    }

    @Override
    public SkinTextures getSkinTextures() {
        return skin;
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

    /** Hides the vanilla name tag; the actor draws its own in the role colour. 隐藏原版名牌；演员自己绘制身份色名牌。 */
    @Override
    public boolean isInvisibleTo(PlayerEntity player) {
        return true;
    }
}
