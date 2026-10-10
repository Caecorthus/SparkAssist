package dev.caecorthus.sparkassist.ponder;

import java.util.List;

/** Caption geometry in GUI pixels, leaving Ponder's title and playback buttons visible. */
public final class PonderCaptionBounds {
    public static final int BORDER = 4;
    public static final int SIDE_MARGIN = 8;
    public static final int HEADER_BOTTOM = 58;
    public static final int FOOTER_HEIGHT = 58;
    public static final int LINE_HEIGHT = 9;

    private PonderCaptionBounds() {
    }

    public static int availableWidth(int screenWidth) {
        return Math.max(1, screenWidth - 2 * (SIDE_MARGIN + BORDER));
    }

    public static int preferredWidth(int screenWidth, int originalWidth) {
        int available = availableWidth(screenWidth);
        return Math.min(available, Math.min(180, Math.max(Math.min(96, available), originalWidth)));
    }

    public static int availableHeight(int screenHeight) {
        return Math.max(LINE_HEIGHT, screenHeight - HEADER_BOTTOM - FOOTER_HEIGHT - 2 * BORDER);
    }

    public static int textHeight(int lineCount) {
        return Math.max(1, lineCount) * LINE_HEIGHT;
    }

    /** Move the common origin before applying script offsets, preserving the distance between simultaneous captions. */
    public static float independentOrigin(int screenHeight, int offset) {
        return Math.max(HEADER_BOTTOM + BORDER, (screenHeight - 200) / 2f - 5) + offset;
    }

    /** Fits the content origin; BoxElement adds four pixels of border outside it. */
    public static Position fit(int screenWidth, int screenHeight, float textX, float textY,
                               int textWidth, int textHeight) {
        float left = SIDE_MARGIN + BORDER;
        float right = Math.max(left, screenWidth - SIDE_MARGIN - BORDER - textWidth);
        float top = HEADER_BOTTOM + BORDER;
        float bottom = Math.max(top, screenHeight - FOOTER_HEIGHT - BORDER - textHeight);
        return new Position(clamp(textX, left, right), clamp(textY, top, bottom));
    }

    public static Position fitIndependent(int screenWidth, int screenHeight, float textX, int offset,
                                          int textWidth, int textHeight, List<Placement> earlier) {
        Position position = fit(screenWidth, screenHeight, textX, independentOrigin(screenHeight, offset),
                textWidth, textHeight);
        float top = HEADER_BOTTOM + BORDER;
        float bottom = screenHeight - FOOTER_HEIGHT - BORDER - textHeight;
        float gap = 2 * BORDER + 4;
        for (Placement previous : earlier) {
            if (!overlaps(position, textWidth, textHeight, previous, gap)) {
                continue;
            }
            float below = previous.y() + previous.height() + gap;
            float above = previous.y() - textHeight - gap;
            boolean belowFirst = offset >= previous.offset();
            Position[] candidates = {
                    new Position(position.x(), belowFirst ? below : above),
                    new Position(position.x(), belowFirst ? above : below),
                    new Position(previous.x() - textWidth - gap, position.y()),
                    new Position(previous.x() + previous.width() + gap, position.y())
            };
            for (Position candidate : candidates) {
                if (candidate.x() < SIDE_MARGIN + BORDER
                        || candidate.x() + textWidth > screenWidth - SIDE_MARGIN - BORDER
                        || candidate.y() < top || candidate.y() > bottom) {
                    continue;
                }
                boolean clear = true;
                for (Placement placed : earlier) {
                    if (overlaps(candidate, textWidth, textHeight, placed, gap)) {
                        clear = false;
                        break;
                    }
                }
                if (clear) {
                    return candidate;
                }
            }
        }
        return position;
    }

    private static boolean overlaps(Position position, int width, int height, Placement other, float gap) {
        return position.x() + width + gap > other.x() && other.x() + other.width() + gap > position.x()
                && position.y() + height + gap > other.y() && other.y() + other.height() + gap > position.y();
    }

    /** First intersection of the pointer segment with the outer window; the visible segment ends there. */
    public static float pointerVisibleFraction(float deltaX, float deltaY, int textWidth, int textHeight) {
        float[] low = {deltaX - 10 - BORDER, deltaY + 3 - BORDER};
        float[] high = {deltaX - 10 + textWidth + BORDER, deltaY + 2 + textHeight + BORDER};
        float[] delta = {deltaX, deltaY};
        float enter = 0;
        float exit = 1;
        for (int axis = 0; axis < 2; axis++) {
            if (Math.abs(delta[axis]) < 1e-6f) {
                if (low[axis] > 0 || high[axis] < 0) {
                    return 1;
                }
                continue;
            }
            float a = low[axis] / delta[axis];
            float b = high[axis] / delta[axis];
            enter = Math.max(enter, Math.min(a, b));
            exit = Math.min(exit, Math.max(a, b));
        }
        return enter > exit ? 1 : clamp(enter, 0, 1);
    }

    private static float clamp(float value, float low, float high) {
        return Math.max(low, Math.min(high, value));
    }

    public record Position(float x, float y) {
    }

    public record Placement(float x, float y, int width, int height, int offset) {
    }
}
