package dev.caecorthus.sparkassist.ponder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class PonderCaptionBoundsTest {
    @Test
    void longCaptionAboveBottomControlsAtGuiScaleThree() {
        int height = PonderCaptionBounds.textHeight(10);
        var position = PonderCaptionBounds.fit(427, 267, 320, 230, 159, height);
        assertTrue(position.x() + 159 + PonderCaptionBounds.BORDER <= 427 - PonderCaptionBounds.SIDE_MARGIN);
        assertTrue(position.y() + height + PonderCaptionBounds.BORDER <= 267 - PonderCaptionBounds.FOOTER_HEIGHT);
    }

    @Test
    void targetOutsideSceneStillKeepsCaptionInsideViewport() {
        var position = PonderCaptionBounds.fit(640, 400, -200, -100, 180, 54);
        assertEquals(12, position.x());
        assertEquals(62, position.y());
    }

    @Test
    void measuredLineCountDefinesHeightWithoutRewrapping() {
        assertEquals(36, PonderCaptionBounds.textHeight(4));
        assertEquals(9, PonderCaptionBounds.textHeight(0));
    }

    @Test
    void narrowScreenHasUsableWidthAndSpaceForBorders() {
        assertEquals(159, PonderCaptionBounds.preferredWidth(427, 159));
        assertEquals(96, PonderCaptionBounds.preferredWidth(320, 20));
        assertEquals(56, PonderCaptionBounds.preferredWidth(80, 180));
        assertEquals(143, PonderCaptionBounds.availableHeight(267));
    }

    @Test
    void independentOffsetsKeepTheirDistanceAfterTheCommonOriginMoves() {
        var first = PonderCaptionBounds.fitIndependent(427, 267, 250, 0, 159, 27, List.of());
        var second = PonderCaptionBounds.fitIndependent(427, 267, 250, 40, 159, 27,
                List.of(new PonderCaptionBounds.Placement(first.x(), first.y(), 159, 27, 0)));
        assertEquals(62, first.y());
        assertEquals(40, second.y() - first.y());
    }

    @Test
    void tallIndependentCaptionPushesTheNextWindowClearOfItsBorder() {
        var first = PonderCaptionBounds.fitIndependent(427, 267, 250, 0, 159, 54, List.of());
        var second = PonderCaptionBounds.fitIndependent(427, 267, 250, 40, 159, 36,
                List.of(new PonderCaptionBounds.Placement(first.x(), first.y(), 159, 54, 0)));
        assertTrue(second.y() >= first.y() + 54 + 2 * PonderCaptionBounds.BORDER);
        assertTrue(second.y() + 36 + PonderCaptionBounds.BORDER <= 267 - PonderCaptionBounds.FOOTER_HEIGHT);
    }

    @Test
    void extremeIndependentOffsetsStillFitAtTheTopAndBottom() {
        assertEquals(62, PonderCaptionBounds.fitIndependent(427, 267, 250, -180, 159, 45, List.of()).y());
        assertEquals(160, PonderCaptionBounds.fitIndependent(427, 267, 250, 200, 159, 45, List.of()).y());
    }

    @Test
    void footerConflictMovesTheSecondCaptionAsideInsteadOfClampingItOntoTheFirst() {
        var first = new PonderCaptionBounds.Placement(250, 62, 159, 108, 0);
        var second = PonderCaptionBounds.fitIndependent(427, 267, 250, 40, 159, 54, List.of(first));
        assertTrue(second.x() + 159 + 2 * PonderCaptionBounds.BORDER <= first.x());
        assertTrue(second.y() >= PonderCaptionBounds.HEADER_BOTTOM + PonderCaptionBounds.BORDER);
        assertTrue(second.y() + 54 + PonderCaptionBounds.BORDER <= 267 - PonderCaptionBounds.FOOTER_HEIGHT);
    }

    @Test
    void thirdCaptionCannotMoveBackOverTheFirstWhileAvoidingTheSecond() {
        var first = PonderCaptionBounds.fitIndependent(427, 267, 250, 0, 159, 54, List.of());
        var firstBox = new PonderCaptionBounds.Placement(first.x(), first.y(), 159, 54, 0);
        var second = PonderCaptionBounds.fitIndependent(427, 267, 250, 40, 159, 36, List.of(firstBox));
        var secondBox = new PonderCaptionBounds.Placement(second.x(), second.y(), 159, 36, 40);
        var third = PonderCaptionBounds.fitIndependent(427, 267, 250, 80, 159, 36,
                List.of(firstBox, secondBox));
        assertEquals(62, first.y());
        assertEquals(128, second.y());
        assertTrue(third.x() + 159 + 2 * PonderCaptionBounds.BORDER <= first.x());
        assertTrue(third.x() + 159 + 2 * PonderCaptionBounds.BORDER <= second.x());
        assertTrue(third.y() >= PonderCaptionBounds.HEADER_BOTTOM + PonderCaptionBounds.BORDER);
        assertTrue(third.y() + 36 + PonderCaptionBounds.BORDER <= 267 - PonderCaptionBounds.FOOTER_HEIGHT);
    }

    @Test
    void upwardPointerStopsAtTheBottomBorderBeforeCrossingText() {
        float fraction = PonderCaptionBounds.pointerVisibleFraction(0, -100, 180, 45);
        assertEquals(0.49f, fraction, 1e-5f);
        assertEquals(-49, -100 * fraction, 1e-5f);
    }

    @Test
    void diagonalPointerStopsAtTheFirstSideBorder() {
        float fraction = PonderCaptionBounds.pointerVisibleFraction(80, -100, 120, 36);
        assertEquals(0.825f, fraction, 1e-5f);
        assertEquals(66, 80 * fraction, 1e-5f);
    }

    @Test
    void anchorCoveredByTheMovedWindowDrawsNoPointerThroughIt() {
        assertEquals(0, PonderCaptionBounds.pointerVisibleFraction(0, -30, 180, 45));
    }
}
