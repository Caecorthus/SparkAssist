package dev.caecorthus.sparkassist.config;

/**
 * How Wathe Blood's particles are tinted. The particle texture is a plain white sprite, so a tint is the whole colour;
 * every option except {@link #DEFAULT} replaces the original red outright.
 * Wathe Blood 的粒子如何着色。粒子纹理是纯白图，因此 tint 就是全部颜色；除 {@link #DEFAULT} 外每个选项都直接替换原本的红色。
 */
public enum BloodAppearance {
    /** Leaves the mod's own colour untouched. 保留模组原本的颜色。 */
    DEFAULT("default", null),
    /** Deeper, less saturated red. 更深、更不刺眼的红。 */
    DARK("dark", new float[] {0.34F, 0.02F, 0.02F}),
    /** Plant-green, for players who would rather not see red. 植物绿，给不想看红色的玩家。 */
    GREEN("green", new float[] {0.22F, 0.55F, 0.20F}),
    /** Cool blue. 冷蓝。 */
    BLUE("blue", new float[] {0.18F, 0.38F, 0.72F}),
    /** Neutral grey. 中性灰。 */
    GREY("grey", new float[] {0.55F, 0.55F, 0.55F}),
    /** Not drawn; the particles still spawn but are never built. 不绘制；粒子仍生成，但不会被构建。 */
    OFF("off", null);

    private final String id;
    private final float[] tint;

    BloodAppearance(String id, float[] tint) {
        this.id = id;
        this.tint = tint;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "option.sparkassist.blood_appearance.value." + id;
    }

    /** Stable lookup for config deserialisation; falls back to {@link #DEFAULT}. 配置反序列化用的稳定查找，失败回落到默认。 */
    public static BloodAppearance fromId(String value) {
        for (BloodAppearance appearance : values()) {
            if (appearance.id.equals(value)) {
                return appearance;
            }
        }
        return DEFAULT;
    }

    /**
     * RGB in 0..1 for the particle tint, or {@code null} when nothing is recoloured: the original colour, and
     * {@link #OFF}, which is not drawn at all. Read once per particle per frame, so the array is shared, not copied.
     * 粒子 tint 的 0..1 RGB；不重新上色时为 {@code null}：即原色，以及根本不绘制的 {@link #OFF}。每个粒子每帧读取一次，
     * 因此数组是共享的，不做拷贝。
     */
    public float[] tint() {
        return tint;
    }
}
