package dev.caecorthus.sparkassist.client.mixin;

import dev.caecorthus.sparkassist.client.announcement.ExpressAnnouncement;
import dev.caecorthus.sparkassist.client.config.SparkAssistClientSettings;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.client.gui.RoundTextRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Takes over the opening role announcement while it is running, leaving the round-end title and the player cards to
 * vanilla. {@link ExpressAnnouncement} draws the animated version; this mixin only decides when it owns the frame.
 * 开局报幕进行期间接管绘制，结算标题与玩家卡片仍交还原版。{@link ExpressAnnouncement} 负责绘制动画版本，本 mixin
 * 只决定何时接管。
 *
 * <p>The state is read through shadows instead of duplicated: {@code welcomeTime} is the same clock vanilla decrements,
 * so the animation and the dismissal stay in sync with the round.
 * 状态通过 shadow 读取而非复制：{@code welcomeTime} 就是原版递减的那个时钟，因此动画与退场始终跟对局同步。</p>
 */
@Mixin(RoundTextRenderer.class)
public abstract class RoundTextRendererMixin {
    /** The local player's announcement data for this round. 本局本人的报幕数据。 */
    @Shadow
    private static RoleAnnouncementTexts.RoleAnnouncementText role;
    /** Remaining opening-announcement ticks, 280 down to 0. 开局报幕剩余 tick。 */
    @Shadow
    private static int welcomeTime;
    /** Killer count handed to the premise line. 传给前提行的杀手数。 */
    @Shadow
    private static int killers;
    /** Target count handed to the goal line. 传给目标行的目标数。 */
    @Shadow
    private static int targets;

    @Inject(method = "renderHud", at = @At("HEAD"), cancellable = true)
    private static void sparkassist$expressAnnouncement(TextRenderer font, ClientPlayerEntity player,
                                                        DrawContext context, CallbackInfo ci) {
        if (!ExpressAnnouncement.owns(role, welcomeTime) || !SparkAssistClientSettings.customAnnouncement()) {
            return;
        }
        // Discovery mode never showed the announcement; keep it that way. 探索模式原本就不显示报幕，保持原状。
        GameWorldComponent game = GameWorldComponent.KEY.maybeGet(player.getWorld()).orElse(null);
        if (game != null && game.getGameMode() == WatheGameModes.DISCOVERY) {
            return;
        }
        ExpressAnnouncement.render(context, font, player, role, welcomeTime, killers, targets);
        ci.cancel();
    }
}
