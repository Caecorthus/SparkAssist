package dev.caecorthus.sparkassist.client.ponder;

import net.createmod.catnip.gui.element.ScreenElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

/**
 * Key caps for control hints, shared by the Witch killer and civilian demos. The Wraith demo keeps its own cap, which
 * takes the last matching binding rather than the first.
 * 控制提示用的键帽，魔女杀手与好人演示共用。冤魂演示保留自己的键帽，它取最后一个匹配的按键而不是第一个。
 */
final class ControlHints {
    private ControlHints() {
    }

    /**
     * A key cap for a control hint, labelled with the key the player has bound to {@code keyId} ({@code fallback} when
     * that binding is missing): Ponder's own hints only draw mouse buttons.
     * 控制提示用的键帽图标，标着玩家给 keyId 绑定的按键（找不到该按键时用 fallback）：Ponder 自带的提示只画鼠标按键。
     */
    static ScreenElement key(String keyId, String fallback) {
        return (graphics, x, y) -> {
            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            Text label = boundKey(keyId, fallback);
            int width = Math.max(14, font.getWidth(label) + 6);
            graphics.fill(x + 1, y + 1, x + 1 + width, y + 15, 0xFFA0A0A0);
            graphics.fill(x + 2, y + 2, x + width, y + 13, 0xFF3A3A3A);
            graphics.drawText(font, label, x + 1 + (width - font.getWidth(label)) / 2, y + 4, 0xFFFFFFFF, false);
        };
    }

    /**
     * The key bound to {@code keyId} (first matching binding), or {@code fallback} when none matches.
     * keyId 绑定的按键（取第一个匹配的绑定），没有匹配时为 fallback。
     */
    static Text boundKey(String keyId, String fallback) {
        for (KeyBinding binding : MinecraftClient.getInstance().options.allKeys) {
            if (binding.getTranslationKey().equals(keyId)) {
                return binding.getBoundKeyLocalizedText();
            }
        }
        return Text.literal(fallback);
    }
}
