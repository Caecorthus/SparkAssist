package dev.caecorthus.sparkassist.client.ponder;

import net.createmod.ponder.api.scene.SceneBuilder;

/**
 * The plain independent caption most demo steps use: {@code showText(duration).text(text).independent()}, optionally
 * followed by {@code attachKeyFrame()}. Ponder derives each caption's lang key from the scene id and the order of its
 * {@code text} calls ({@code text_1}, {@code text_2}, ...), so a call stands exactly where the chain stood and calls
 * must never be reordered. Captions of any other shape (colour, pointer, offset) stay written out at their call site.
 * 大多数演示步骤用的普通独立字幕：{@code showText(duration).text(text).independent()}，可选再接
 * {@code attachKeyFrame()}。思索按场景 id 与 {@code text} 调用的先后顺序生成每条字幕的语言键（text_1、text_2……），
 * 所以调用必须留在原链式调用的位置，且不得调换顺序。其他形态的字幕（颜色、指向、偏移）仍在调用处原样书写。
 */
final class SceneCaptions {
    private SceneCaptions() {
    }

    /** An independent caption shown for {@code duration} ticks. 显示 duration tick 的独立字幕。 */
    static void caption(SceneBuilder scene, int duration, String text) {
        scene.overlay().showText(duration).text(text).independent();
    }

    /** {@link #caption}, then a keyframe at this point. 独立字幕，并在此处附加关键帧。 */
    static void keyframeCaption(SceneBuilder scene, int duration, String text) {
        scene.overlay().showText(duration).text(text).independent().attachKeyFrame();
    }
}
