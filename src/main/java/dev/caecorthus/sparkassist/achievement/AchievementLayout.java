package dev.caecorthus.sparkassist.achievement;

/**
 * Where the n-th unlocked achievement sits on its advancement page. Earned achievements form a track in unlock
 * order, {@link #COLUMNS} per row; each row starts off the root and every other entry hangs off the one before it,
 * so vanilla's connector lines run along the rows. Only earned entries are placed, so there are no gaps that would
 * hint at what is still hidden.
 * 第 n 个已解锁成就在成就页上的位置。已达成的成就按解锁顺序排成轨道，每行 {@link #COLUMNS} 个；
 * 每行第一个连到根节点，其余的连到前一个，原版连线因此沿着每一行延伸。只摆放已达成的成就，
 * 不会留下暗示还有隐藏内容的空位。
 */
public final class AchievementLayout {
    /** Fits the vanilla advancement window (234 px) without panning. 不需拖动即可放进原版成就窗口（234 像素）。 */
    public static final int COLUMNS = 7;

    private AchievementLayout() {
    }

    /**
     * The slot of the {@code index}-th unlock (0-based); {@code parentIndex} is -1 for the root.
     * 第 {@code index} 个解锁（从 0 开始）的位置；{@code parentIndex} 为 -1 表示连到根节点。
     */
    public static Slot slot(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index " + index);
        }
        int row = index / COLUMNS;
        int column = index % COLUMNS;
        return new Slot(column + 1, row, column == 0 ? -1 : index - 1);
    }

    /** Grid position in advancement units (28 × 27 px). 以成就格为单位的位置（28 × 27 像素）。 */
    public record Slot(float x, float y, int parentIndex) {
    }
}
