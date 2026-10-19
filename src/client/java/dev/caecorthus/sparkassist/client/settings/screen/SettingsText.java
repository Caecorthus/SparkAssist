package dev.caecorthus.sparkassist.client.settings.screen;

import dev.caecorthus.sparkassist.client.guidebook.render.CjkLineBreaker;
import dev.caecorthus.sparkassist.client.guidebook.render.CjkLineBreaker.Cell;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.util.Language;

/**
 * Wraps description text with the guide's CJK line breaker, so Chinese never splits before closing punctuation.
 * 用指南的中日韩断行器给说明文字换行，中文不会在闭合标点前断开。
 */
final class SettingsText {
    private SettingsText() {
    }

    static List<OrderedText> wrap(TextRenderer font, StringVisitable text, int width) {
        List<Cell> cells = CjkLineBreaker.normalise(CjkLineBreaker.cells(text), true);
        List<OrderedText> lines = new ArrayList<>();
        for (List<Cell> line : CjkLineBreaker.wrap(cells, Math.max(1, width),
                cell -> font.getWidth(OrderedText.styledForwardsVisitedString(
                        Character.toString(cell.codePoint()), cell.style())))) {
            lines.add(CjkLineBreaker.toOrderedText(line));
        }
        return lines;
    }

    /** Cuts {@code text} to {@code width} with an ellipsis. 超宽时截断并加省略号。 */
    static OrderedText ellipsize(TextRenderer font, StringVisitable text, int width) {
        if (font.getWidth(text) <= width) {
            return Language.getInstance().reorder(text);
        }
        String ellipsis = "…";
        StringVisitable cut = font.trimToWidth(text, Math.max(0, width - font.getWidth(ellipsis)));
        return Language.getInstance().reorder(StringVisitable.concat(cut,
                StringVisitable.plain(ellipsis)));
    }
}
