package dev.caecorthus.sparkassist.client.ponder;

import dev.caecorthus.sparkassist.client.guidebook.render.CjkLineBreaker;
import dev.caecorthus.sparkassist.client.guidebook.render.CjkLineBreaker.Cell;
import dev.caecorthus.sparkassist.ponder.PonderCaptionBounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.TextVisitFactory;

/** Uses one set of styled lines for both Ponder's caption window and its text. */
public final class PonderCaptionText {
    private static final List<PonderCaptionBounds.Placement> INDEPENDENT_CAPTIONS = new ArrayList<>();

    private PonderCaptionText() {
    }

    public static void beginOverlay() {
        INDEPENDENT_CAPTIONS.clear();
    }

    public static PonderCaptionBounds.Position placeIndependent(int screenWidth, int screenHeight, float textX,
                                                                int offset, int textWidth, int textHeight) {
        PonderCaptionBounds.Position position = PonderCaptionBounds.fitIndependent(screenWidth, screenHeight,
                textX, offset, textWidth, textHeight, INDEPENDENT_CAPTIONS);
        INDEPENDENT_CAPTIONS.add(new PonderCaptionBounds.Placement(position.x(), position.y(),
                textWidth, textHeight, offset));
        return position;
    }

    public static List<StringVisitable> wrap(TextRenderer font, String text, int screenWidth, int screenHeight,
                                            int originalWidth) {
        int width = PonderCaptionBounds.preferredWidth(screenWidth, originalWidth);
        List<StringVisitable> lines = wrap(font, text, width);
        if (PonderCaptionBounds.textHeight(lines.size()) > PonderCaptionBounds.availableHeight(screenHeight)) {
            // Long captions use the available screen width before being moved above the controls.
            // 长字幕先利用屏幕宽度，再移到播放按钮上方。
            lines = wrap(font, text, PonderCaptionBounds.availableWidth(screenWidth));
        }
        return lines;
    }

    private static List<StringVisitable> wrap(TextRenderer font, String text, int width) {
        if (text.codePoints().noneMatch(PonderCaptionText::cjk)) {
            return font.getTextHandler().wrapLines(text, width, Style.EMPTY);
        }
        List<Cell> cells = new ArrayList<>();
        TextVisitFactory.visitFormatted(text, Style.EMPTY, (index, style, codePoint) -> {
            cells.add(new Cell(codePoint, style));
            return true;
        });
        List<StringVisitable> lines = new ArrayList<>();
        for (List<Cell> line : CjkLineBreaker.wrap(cells, width,
                cell -> font.getWidth(OrderedText.styledForwardsVisitedString(
                        Character.toString(cell.codePoint()), cell.style())))) {
            List<StringVisitable> parts = new ArrayList<>();
            for (Cell cell : line) {
                parts.add(StringVisitable.styled(Character.toString(cell.codePoint()), cell.style()));
            }
            lines.add(StringVisitable.concat(parts));
        }
        return lines;
    }

    private static boolean cjk(int codePoint) {
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        return script == Character.UnicodeScript.HAN || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA || script == Character.UnicodeScript.HANGUL;
    }
}
