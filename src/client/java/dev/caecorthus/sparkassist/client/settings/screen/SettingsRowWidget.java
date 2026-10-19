package dev.caecorthus.sparkassist.client.settings.screen;

import dev.caecorthus.sparkassist.client.settings.ChoiceOption;
import dev.caecorthus.sparkassist.client.settings.SettingsOption;
import dev.caecorthus.sparkassist.client.settings.SliderOption;
import dev.caecorthus.sparkassist.client.settings.SoundPreview;
import dev.caecorthus.sparkassist.client.settings.ToggleOption;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * One option row: the name on the left, its control on the right. Left click toggles, cycles forward or grabs the
 * slider; right click cycles back; arrows and Enter do the same from the keyboard (Shift moves a slider ten steps).
 * 一行选项：左侧名称，右侧控件。左键切换开关、向后切换选项或抓取滑块；右键向前切换；键盘方向键与回车同理（按住
 * Shift 时滑块一次移动十格）。
 */
final class SettingsRowWidget extends ClickableWidget {
    static final int HEIGHT = 20;
    private static final int PAD = 6;
    private static final int TRACK_WIDTH = 64;
    private static final int VALUE_GAP = 6;
    /** Fixed slot for a slider's value, wide enough for "200%", so the preview button never moves. 滑块数值的固定宽度。 */
    private static final int VALUE_SLOT = 26;
    private static final int PREVIEW_SIZE = 11;

    private final SettingsOption option;
    private final Host host;
    private boolean dragging;

    /** What the row needs from its screen. 行对所在界面的需求。 */
    interface Host {
        /** True when the point is inside the scrolled list viewport. 点是否在滚动列表的可视区内。 */
        boolean inListViewport(double x, double y);

        /** A value changed; {@code finished} saves it now, otherwise on release. 值已改变；finished 为真时立即保存。 */
        void changed(SettingsOption option, boolean finished);
    }

    SettingsRowWidget(int x, int y, int width, SettingsOption option, Host host) {
        super(x, y, width, HEIGHT, option.title());
        this.option = option;
        this.host = host;
    }

    SettingsOption option() {
        return option;
    }

    boolean dragging() {
        return dragging;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY) && host.inListViewport(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return false;
        }
        if (!option.enabled()) {
            return true;
        }
        boolean back = button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        SoundPreview preview = option.preview();
        if (preview != null && !back && inPreviewButton(mouseX, mouseY)) {
            preview.toggle();
            return true;
        }
        if (option instanceof ToggleOption toggle && !back) {
            toggle.toggle();
            host.changed(option, true);
        } else if (option instanceof ChoiceOption<?> choice) {
            choice.cycle(back ? -1 : 1);
            host.changed(option, true);
        } else if (option instanceof SliderOption slider && !back) {
            dragging = true;
            slider.setFraction(trackFraction(mouseX));
            host.changed(option, false);
        } else {
            return true;
        }
        this.playDownSound(MinecraftClient.getInstance().getSoundManager());
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && option instanceof SliderOption slider && option.enabled()) {
            slider.setFraction(trackFraction(mouseX));
            host.changed(option, false);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return finishDrag();
    }

    /** Ends a slider drag and saves; safe to call when not dragging. 结束滑块拖动并保存；未拖动时调用也无妨。 */
    boolean finishDrag() {
        if (!dragging) {
            return false;
        }
        dragging = false;
        host.changed(option, true);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.active || !option.enabled()) {
            return false;
        }
        boolean left = keyCode == GLFW.GLFW_KEY_LEFT;
        boolean right = keyCode == GLFW.GLFW_KEY_RIGHT;
        boolean press = keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_SPACE;
        if (option instanceof ToggleOption toggle && (left || right || press)) {
            toggle.toggle();
        } else if (option instanceof ChoiceOption<?> choice && (left || right || press)) {
            choice.cycle(left ? -1 : 1);
        } else if (option instanceof SliderOption slider && (left || right)) {
            slider.nudge((left ? -1 : 1) * (Screen.hasShiftDown() ? 10 : 1));
        } else if (press && option.preview() != null) {
            option.preview().toggle();
            return true;
        } else {
            return false;
        }
        host.changed(option, true);
        return true;
    }

    private double lastMouseX;
    private double lastMouseY;

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        int x = getX();
        int y = getY();
        int right = x + width;
        boolean enabled = option.enabled();
        boolean hot = enabled && (isHovered() || dragging);

        context.fill(x, y, right, y + height, hot ? SettingsPalette.ROW_HOVER : SettingsPalette.ROW);
        if (hot) {
            context.fill(x, y, right, y + 1, SettingsPalette.ROW_HOVER_WASH);
        }
        if (isFocused() && MinecraftClient.getInstance().getNavigationType().isKeyboard()) {
            context.drawBorder(x, y, width, height, SettingsPalette.FOCUS);
        }

        int textY = y + (height - font.fontHeight) / 2 + 1;
        int controlLeft = renderControl(context, font, right - PAD, y, textY, enabled, hot);
        int labelX = x + PAD;
        if (option.accent() != 0) {
            // The role's guide colour, ringed so dark colours (Corrupt Cop navy) still read on a dark row.
            // 角色在指南中的颜色，加一圈浅色描边，深色（如黑警的藏青）在深色行上也看得清。
            int dotY = y + height / 2 - 3;
            context.fill(labelX, dotY, labelX + 6, dotY + 6, SettingsPalette.DOT_RING);
            context.fill(labelX + 1, dotY + 1, labelX + 5, dotY + 5, option.accent());
            labelX += 10;
        }
        int available = Math.max(0, controlLeft - VALUE_GAP - labelX);
        OrderedText label = SettingsText.ellipsize(font, option.label(), available);
        context.drawText(font, label, labelX, textY, enabled ? SettingsPalette.TEXT : SettingsPalette.TEXT_DISABLED,
                false);
        Text detail = option.detail();
        int used = font.getWidth(label) + 5;
        if (detail != null && available - used > 12) {
            context.drawText(font, SettingsText.ellipsize(font, detail, available - used), labelX + used, textY,
                    enabled ? SettingsPalette.TEXT_MUTED : SettingsPalette.TEXT_DISABLED, false);
        }
    }

    /** Draws the control right-aligned at {@code right}; returns its left edge. 右对齐绘制控件，返回其左边缘。 */
    private int renderControl(DrawContext context, TextRenderer font, int right, int y, int textY, boolean enabled,
                              boolean hot) {
        if (option instanceof SliderOption slider) {
            int trackLeft = right - TRACK_WIDTH;
            int trackY = y + height / 2;
            context.fill(trackLeft, trackY - 1, right, trackY + 1,
                    enabled ? SettingsPalette.TRACK : SettingsPalette.TRACK_DISABLED);
            int thumbX = trackLeft + (int) Math.round(slider.fraction() * (TRACK_WIDTH - 3));
            if (enabled) {
                context.fill(trackLeft, trackY - 1, thumbX, trackY + 1, SettingsPalette.ACCENT_FILL);
            }
            context.fill(thumbX, trackY - 5, thumbX + 3, trackY + 5, !enabled ? SettingsPalette.TEXT_DISABLED
                    : hot ? SettingsPalette.THUMB_HOT : SettingsPalette.THUMB);
            Text value = slider.valueText();
            int slotLeft = trackLeft - VALUE_GAP - VALUE_SLOT;
            int valueX = trackLeft - VALUE_GAP - font.getWidth(value);
            context.drawText(font, value, valueX, textY,
                    enabled ? SettingsPalette.TEXT : SettingsPalette.TEXT_DISABLED, false);
            SoundPreview preview = option.preview();
            if (preview == null) {
                return Math.min(valueX, slotLeft);
            }
            renderPreviewButton(context, previewLeft(), y + (height - PREVIEW_SIZE) / 2, preview.playing(), enabled);
            return previewLeft();
        }

        Text value = option.valueText();
        int color;
        if (!enabled) {
            color = SettingsPalette.TEXT_DISABLED;
        } else if (option instanceof ToggleOption toggle) {
            color = toggle.value() ? SettingsPalette.ON : SettingsPalette.OFF;
        } else {
            color = SettingsPalette.TEXT;
        }
        int valueX = right - font.getWidth(value);
        if (option instanceof ChoiceOption<?> && hot) {
            int arrowY = y + height / 2;
            arrow(context, valueX - 7, arrowY, -1, SettingsPalette.TEXT_MUTED);
            arrow(context, right + 3, arrowY, 1, SettingsPalette.TEXT_MUTED);
        }
        context.drawText(font, value, valueX, textY, color, false);
        return option instanceof ChoiceOption<?> ? valueX - 8 : valueX;
    }

    private int previewLeft() {
        return getX() + width - PAD - TRACK_WIDTH - VALUE_GAP - VALUE_SLOT - 4 - PREVIEW_SIZE;
    }

    private boolean inPreviewButton(double mouseX, double mouseY) {
        int left = previewLeft();
        int top = getY() + (height - PREVIEW_SIZE) / 2;
        return mouseX >= left - 2 && mouseX < left + PREVIEW_SIZE + 2 && mouseY >= top - 2
                && mouseY < top + PREVIEW_SIZE + 2;
    }

    /** A small square button: ▶ to play a sample, ■ while it plays. 小方按钮：▶ 试听，播放中显示 ■。 */
    private void renderPreviewButton(DrawContext context, int x, int y, boolean playing, boolean enabled) {
        boolean hot = isHovered() && inPreviewButton(lastMouseX, lastMouseY);
        int ink = playing ? SettingsPalette.ACCENT : hot ? SettingsPalette.TEXT
                : enabled ? SettingsPalette.TEXT_MUTED : SettingsPalette.TEXT_DISABLED;
        context.drawBorder(x, y, PREVIEW_SIZE, PREVIEW_SIZE, playing || hot ? ink : SettingsPalette.PREVIEW_EDGE);
        if (playing) {
            context.fill(x + 3, y + 3, x + PREVIEW_SIZE - 3, y + PREVIEW_SIZE - 3, ink);
        } else {
            for (int i = 0; i < 3; i++) {
                context.fill(x + 4 + i, y + 3 + i, x + 5 + i, y + PREVIEW_SIZE - 3 - i, ink);
            }
        }
    }

    /** A 3 px chevron pointing left ({@code -1}) or right ({@code 1}). 指向左或右的 3 像素箭头。 */
    private static void arrow(DrawContext context, int x, int centreY, int direction, int color) {
        for (int i = 0; i < 3; i++) {
            int column = direction < 0 ? x + i : x + 2 - i;
            context.fill(column, centreY - i, column + 1, centreY + i + 1, color);
        }
    }

    private double trackFraction(double mouseX) {
        int trackLeft = getX() + width - PAD - TRACK_WIDTH;
        return (mouseX - trackLeft - 1) / (double) (TRACK_WIDTH - 3);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, Text.translatable("screen.sparkassist.settings.narration.row",
                option.title(), option.valueText()));
        if (option.enabled()) {
            builder.put(NarrationPart.USAGE, Text.translatable(option.preview() != null
                    ? "screen.sparkassist.settings.narration.usage_preview"
                    : "screen.sparkassist.settings.narration.usage"));
        }
    }
}
