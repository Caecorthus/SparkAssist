package dev.caecorthus.sparkassist.client.announcement;

import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

/**
 * Redraws the opening role announcement: the welcome line breathes in and out, a brass rule grows from the centre
 * outwards, the two lines under it type themselves in, and the round's SparkTraits badges sit twenty pixels below the
 * goal.
 * 重绘身份报幕：欢迎语淡入淡出，一根黄铜细线由中心向两边伸长，下面两行逐字打出，本局词条放在目标下方二十像素处。
 *
 * <p>Only the opening phase is replaced. Once {@code welcomeTime} runs out the caller lets vanilla draw the round-end
 * title and the player cards unchanged, so nothing here can break settlement.
 * 只替换开局阶段。{@code welcomeTime} 归零后由调用方交还原版绘制结算标题与玩家卡片，因此这里不会影响结算。</p>
 */
public final class ExpressAnnouncement {
    /** Total ticks the opening announcement lasts, matching Wathe's {@code WELCOME_DURATION}. 开局报幕总时长。 */
    public static final int TOTAL = 280;
    /** Ticks spent fading the welcome line in, against the piano's first note. 欢迎语淡入 tick，对齐钢琴首个音符。 */
    private static final int FADE_IN = 10;
    /**
     * The brass rule starts this many ticks in, once the welcome line is readable, and takes {@code RULE_GROW} to reach
     * full width.
     * 铜线在欢迎语可读后开始，用 {@code RULE_GROW} 长到满宽。
     */
    private static final int RULE_START = 8;
    private static final int RULE_GROW = 28;
    /** The body starts typing when the rule finishes. 正文在铜线展开后开始打字。 */
    private static final int TYPE_START = RULE_START + RULE_GROW;
    /** Whole typing animation budget: 2.5 seconds, as requested. 打字动画总预算：2.5 秒。 */
    private static final int TYPE_TICKS = 50;
    /**
     * Fade-out begins when {@code riser.ogg} reaches its tenth second. The riser starts with the announcement, so that is
     * tick 200 of the 280-tick window; the remaining 80 ticks fade everything out.
     * 渐隐在 {@code riser.ogg} 播到第 10 秒时开始。riser 与报幕同时开始，即 280 tick 窗口中的第 200 tick，剩下 80 tick
     * 用于整体渐隐。
     */
    private static final int RISER_FADE_START = 200;
    private static final int FADE_OUT = TOTAL - RISER_FADE_START;

    /** Vertical offsets, in the centred coordinate space the caller sets up. 在居中标系里的纵向偏移。 */
    private static final int WELCOME_Y = -26;
    /** The rule sits well clear of the welcome line's baseline so the two never touch. 铜线远离欢迎语基线。 */
    private static final int RULE_Y = 8;
    private static final int PREMISE_Y = 18;
    private static final int GOAL_Y = 32;
    /** Badges sit twenty pixels below the goal line, as requested. 词条位于目标下方二十像素。 */
    private static final int BADGE_Y = GOAL_Y + 20;

    private static final float WELCOME_SCALE = 2.6F;
    private static final float PREMISE_SCALE = 1.2F;
    /** Brass, matching the guide book and the menu chrome. 与指南书、菜单装饰一致的黄铜色。 */
    private static final int BRASS = 0xFFC5A244;
    private static final int BRASS_DIM = 0xFF815A15;
    /** Welcome line in the same warm off-white as the body, as requested. 欢迎语使用与正文相同的暖白。 */
    private static final int WELCOME_COLOR = 0xFFEFE2C8;
    /** Fallback when a role definition carries no colour. 角色没有配色时的兜底。 */
    private static final int ROLE_COLOR_FALLBACK = 0xFFEFE2C8;
    private static final int BODY_COLOR = 0xFFEFE2C8;
    private static final int BADGE_COLOR = 0xFFB4A080;

    private ExpressAnnouncement() {
    }

    /**
     * Draws one frame of the opening announcement.
     * 绘制开局报幕的一帧。
     *
     * @param welcomeTime remaining ticks, 280 down to 0 剩余 tick，280 递减到 0
     */
    public static void render(DrawContext context, TextRenderer font, ClientPlayerEntity player,
                              RoleAnnouncementTexts.RoleAnnouncementText role, int welcomeTime,
                              int killers, int targets) {
        if (role == null) {
            return;
        }
        int elapsed = TOTAL - welcomeTime;
        float alpha = alpha(elapsed, welcomeTime);
        // Below this the text would be invisible anyway; skipping avoids the alpha-is-zero rewrite below.
        // 低于此值本来也看不见；跳过还能避开下面说的「alpha 为 0 会被改写」问题。
        if (alpha < 0.01F) {
            return;
        }
        int centreX = context.getScaledWindowWidth() / 2;
        int centreY = context.getScaledWindowHeight() / 2;
        int roleColor = role.colour == 0 ? ROLE_COLOR_FALLBACK : (role.colour | 0xFF000000);

        context.getMatrices().push();
        context.getMatrices().translate((float) centreX, (float) centreY, 3.5F);

        // Welcome line: scaled up, breathing with the block. 欢迎语：放大显示，跟随整体呼吸。
        scaledCentred(context, font, role.welcomeText, WELCOME_Y, WELCOME_SCALE, fade(WELCOME_COLOR, alpha));

        // Brass rule: lengthens on entry, shortens to nothing on exit. 黄铜线：入场伸长，退场缩为消失。
        rule(context, font, role.welcomeText, elapsed, welcomeTime);

        // Body: typed in, then faded with the rest. 正文：逐字打出，随后随整体渐隐。
        Text premise = role.premiseText == null ? Text.empty() : role.premiseText.apply(killers);
        Text goal = role.goalText == null ? Text.empty() : role.goalText.apply(targets);
        // Both lines type at one shared rate, so the pair finishes exactly on the 2.5-second budget whichever is longer.
        // 两行共用同一速度，因此无论哪行更长，两行都在 2.5 秒预算上同时打完。
        int premiseLength = premise.getString().length();
        int goalLength = goal.getString().length();
        float charsPerTick = (premiseLength + goalLength) / (float) Math.max(1, TYPE_TICKS);
        int premiseChars = typedChars(elapsed, premiseLength, charsPerTick);
        int goalChars = typedChars(elapsed, goalLength, charsPerTick);

        context.getMatrices().push();
        context.getMatrices().scale(PREMISE_SCALE, PREMISE_SCALE, 1.0F);
        String premiseText = slice(premise, premiseChars);
        if (!premiseText.isEmpty()) {
            context.drawCenteredTextWithShadow(font, Text.literal(premiseText), 0,
                    Math.round(PREMISE_Y / PREMISE_SCALE), fade(BODY_COLOR, alpha));
        }
        context.getMatrices().pop();

        // The goal line carries the role's own colour, so the card reads as "this is your job".
        // 目标行用角色自己的配色，看起来就是「这是你的活」。
        String goalText = slice(goal, goalChars);
        if (!goalText.isEmpty()) {
            context.drawCenteredTextWithShadow(font, Text.literal(goalText), 0, GOAL_Y,
                    fade(roleColor, alpha));
        }

        // Trait badges on their own line, only once the goal has finished typing.
        // 词条单独一行，等目标打完后才出现。
        if (goalChars >= goal.getString().length()) {
            Text badges = TraitBadges.line(player);
            if (badges != null) {
                context.drawCenteredTextWithShadow(font, badges, 0, BADGE_Y, fade(BADGE_COLOR, alpha));
            }
        }

        context.getMatrices().pop();
    }


    /** Overall opacity: fade in at the start, fade out at the end. 整体不透明度：开头淡入，结尾淡出。 */
    private static float alpha(int elapsed, int welcomeTime) {
        float in = Math.min(1.0F, elapsed / (float) FADE_IN);
        float out = Math.min(1.0F, welcomeTime / (float) FADE_OUT);
        return Math.max(0.0F, Math.min(1.0F, in * out));
    }

    /**
     * Draws the brass rule under the welcome line. Its half-width grows from zero to the welcome text's own width, so it
     * always frames the line above it whatever the language.
     * 在欢迎语下方绘制黄铜线。半宽从零长到欢迎语自身的宽度，因此无论什么语言都正好框住上面那行。
     */
    private static void rule(DrawContext context, TextRenderer font, Text welcome, int elapsed, int welcomeTime) {
        // Grow on entry, shrink on exit: two independent factors on the same half-width.
        // 入场伸长、退场缩短：同一个半宽上的两个独立因子。
        float grow = Math.min(1.0F, Math.max(0.0F, (elapsed - RULE_START) / (float) RULE_GROW));
        float shrink = Math.min(1.0F, welcomeTime / (float) FADE_OUT);
        float progress = grow * shrink;
        // Deliberately longer than the welcome line so it reads as a separator, not an underline.
        // 刻意比欢迎语更宽，读起来是分隔线而不是下划线。
        int full = Math.max(200, font.getWidth(welcome));
        int half = Math.round(full * progress);
        if (half <= 0) {
            return;
        }
        context.fill(-half, RULE_Y, half, RULE_Y + 1, BRASS);
        context.fill(-half, RULE_Y + 1, half, RULE_Y + 2, BRASS_DIM);
        // Bright caps at both ends, so the growth reads as an extending rod. 两端点亮，使伸长读起来像一根抽出的杆。
        context.fill(-half - 1, RULE_Y - 1, -half + 1, RULE_Y + 2, BRASS);
        context.fill(half - 1, RULE_Y - 1, half + 1, RULE_Y + 2, BRASS);
    }

    /** How many characters of {@code text} should be visible at this tick. 本 tick 应显示多少个字符。 */
    private static int typedChars(int elapsed, int length, float charsPerTick) {
        if (elapsed <= TYPE_START || charsPerTick <= 0.0F) {
            return 0;
        }
        return Math.min(length, Math.round((elapsed - TYPE_START) * charsPerTick));
    }

    /** The visible prefix, keeping the original style. 可见前缀，保留原样式。 */
    private static String slice(Text text, int chars) {
        String full = text.getString();
        if (chars <= 0) {
            return "";
        }
        return chars >= full.length() ? full : full.substring(0, chars);
    }

    /** Centred line drawn at {@code scale} in the centred coordinate space. 在居中标系里按 {@code scale} 绘制居中文本。 */
    private static void scaledCentred(DrawContext context, TextRenderer font, Text text, int y, float scale, int color) {
        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawCenteredTextWithShadow(font, text, 0, Math.round(y / scale), color);
        context.getMatrices().pop();
    }

    /**
     * Packs {@code alpha} into a colour, never zero. Minecraft rewrites a text colour whose alpha byte is zero to fully
     * opaque, so the first fading frame would flash at full brightness; clamping to 1 keeps the fade continuous.
     * 把 {@code alpha} 打进颜色，且永不为 0。Minecraft 会把 alpha 字节为 0 的文字颜色改写成完全不透明，导致淡入的第一帧
     * 闪成全亮；夹到 1 才能让渐变连续。
     */
    private static int fade(int rgb, float alpha) {
        int a = Math.max(1, Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** True while the opening announcement should own the frame. 开局报幕是否应接管本帧。 */
    public static boolean owns(@Nullable RoleAnnouncementTexts.RoleAnnouncementText role, int welcomeTime) {
        return role != null && welcomeTime > 0;
    }
}
