package dev.caecorthus.sparkassist.achievement;

/**
 * Where the n-th unlocked achievement sits on its advancement page. Earned achievements fill a grid in unlock order,
 * {@link #COLUMNS} per row, to the right of the root; each stands on its own, with no connector lines. Only earned
 * entries are placed, so there are no gaps that would hint at what is still hidden.
 * 第 n 个已解锁成就在成就页上的位置。已达成的成就按解锁顺序填入根节点右侧的网格，每行 {@link #COLUMNS} 个；
 * 各自独立，没有连线。只摆放已达成的成就，不会留下暗示还有隐藏内容的空位。
 */
public final class AchievementLayout {
    /** Fits the vanilla advancement window (234 px) without panning. 不需拖动即可放进原版成就窗口（234 像素）。 */
    public static final int COLUMNS = 7;

    private AchievementLayout() {
    }

    /** The slot of the {@code index}-th unlock (0-based). 第 {@code index} 个解锁（从 0 开始）的位置。 */
    public static Slot slot(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index " + index);
        }
        return new Slot(index % COLUMNS + 1, index / COLUMNS);
    }

    /** Grid position in advancement units (28 × 27 px). 以成就格为单位的位置（28 × 27 像素）。 */
    public record Slot(float x, float y) {
    }
}
