package dev.caecorthus.sparkassist.client.achievement;

import dev.caecorthus.sparkassist.achievement.AchievementFrame;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * The toast for an earned achievement. It draws exactly like vanilla's advancement toast (same frame texture, icon
 * position, colours, wrapping and fade, same duration and challenge fanfare) but its heading reads
 * "Level x 成就达成" instead of the frame's "达成进度！"/"达成目标！"/"完成挑战！".
 * 达成成就时的弹窗。绘制方式与原版进度弹窗完全一致（相同的边框贴图、图标位置、颜色、换行与淡入淡出，相同的时长和挑战号角声），
 * 只是标题行显示“Level x 成就达成”，而不是边框对应的“达成进度！”/“达成目标！”/“完成挑战！”。
 */
final class AchievementToast implements Toast {
    private static final Identifier TEXTURE = Identifier.ofVanilla("toast/advancement");
    private static final String HEADING_KEY = "toast.sparkassist.achievement.title";
    // Vanilla AdvancementToast values. 原版 AdvancementToast 的数值。
    private static final long DURATION_MS = 5000L;
    private static final int CHALLENGE_COLOR = 0xFF88FF;
    private static final int HEADING_COLOR = 0xFFFF00;
    private static final int TITLE_WIDTH = 125;
    private static final long HEADING_MS = 1500L;
    private static final float FADE_MS = 300.0F;

    private final ItemStack icon;
    private final Text title;
    private final Text heading;
    private final boolean challenge;
    private boolean soundPlayed;

    AchievementToast(ItemStack icon, Text title, int level) {
        this.icon = icon;
        this.title = title;
        this.heading = Text.translatable(HEADING_KEY, level);
        this.challenge = AchievementFrame.forLevel(level) == AchievementFrame.CHALLENGE;
    }

    @Override
    public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
        TextRenderer textRenderer = manager.getClient().textRenderer;
        context.drawGuiTexture(TEXTURE, 0, 0, getWidth(), getHeight());
        List<OrderedText> lines = textRenderer.wrapLines(title, TITLE_WIDTH);
        int headingColor = challenge ? CHALLENGE_COLOR : HEADING_COLOR;
        if (lines.size() == 1) {
            context.drawText(textRenderer, heading, 30, 7, headingColor | Colors.BLACK, false);
            context.drawText(textRenderer, lines.getFirst(), 30, 18, -1, false);
        } else if (startTime < HEADING_MS) {
            int alpha = MathHelper.floor(MathHelper.clamp((HEADING_MS - startTime) / FADE_MS, 0.0F, 1.0F) * 255.0F) << 24
                    | 0x04000000;
            context.drawText(textRenderer, heading, 30, 11, headingColor | alpha, false);
        } else {
            int alpha = MathHelper.floor(MathHelper.clamp((startTime - HEADING_MS) / FADE_MS, 0.0F, 1.0F) * 252.0F) << 24
                    | 0x04000000;
            int y = getHeight() / 2 - lines.size() * textRenderer.fontHeight / 2;
            for (OrderedText line : lines) {
                context.drawText(textRenderer, line, 30, y, 0xFFFFFF | alpha, false);
                y += textRenderer.fontHeight;
            }
        }

        if (!soundPlayed && startTime > 0L) {
            soundPlayed = true;
            if (challenge) {
                manager.getClient().getSoundManager().play(
                        PositionedSoundInstance.master(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F));
            }
        }

        context.drawItemWithoutEntity(icon, 8, 8);
        return startTime >= DURATION_MS * manager.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }
}
