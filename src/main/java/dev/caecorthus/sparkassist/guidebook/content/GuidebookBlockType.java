package dev.caecorthus.sparkassist.guidebook.content;

/**
 * Small, stable set of content blocks supported by the in-game book.
 * 游戏内书本支持的小型稳定内容块集合。
 */
public enum GuidebookBlockType {
    SECTION,
    QUOTE,
    PARAGRAPH,
    BULLET,
    SPACER,
    /**
     * A button that plays an animated demo (Ponder scene); the block's {@code target} names the demo. Laid out only
     * when the demo can be played, so books without Ponder show nothing.
     * 播放动画演示（Ponder 场景）的按钮；块的 target 指明演示。只有演示可播放时才排版，未安装 Ponder 时不显示。
     */
    DEMO
}
