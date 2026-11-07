package dev.caecorthus.sparkassist.config;

/**
 * How Wathe Blood's particles are tinted. The particle texture is a plain white sprite, so a tint is the whole colour;
 * every option except {@link #DEFAULT} replaces the original red outright.
 * Wathe Blood 的粒子如何着色。粒子纹理是纯白图，因此 tint 就是全部颜色；除 {@link #DEFAULT} 外每个选项都直接替换原本的红色。
 */
public enum BloodAppearance {
    /** Leaves the mod's own colour untouched. 保留模组原本的颜色。 */
    DEFAULT("default"),
    /** Deeper, less saturated red. 更深、更不刺眼的红。 */
    DARK("dark"),
    /** Plant-green, for players who would rather not see red. 植物绿，给不想看红色的玩家。 */
    GREEN("green"),
    /** Cool blue. 冷蓝。 */
    BLUE("blue"),
    /** Neutral grey. 中性灰。 */
    GREY("grey"),
    /** Fully transparent; the particles still spawn but are invisible. 完全透明；粒子仍生成但不可见。 */
    OFF("off");

    private final String id;

    BloodAppearance(String id) {
        this.id = id;
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

    /** RGB in 0..1 for the particle tint, or {@code null} to leave the original colour alone. 粒子 tint 的 0..1 RGB；{@code null} 表示不改原色。 */
    public float[] tint() {
        return switch (this) {
            case DEFAULT -> null;
            case DARK -> new float[] {0.34F, 0.02F, 0.02F};
            case GREEN -> new float[] {0.22F, 0.55F, 0.20F};
            case BLUE -> new float[] {0.18F, 0.38F, 0.72F};
            case GREY -> new float[] {0.55F, 0.55F, 0.55F};
            case OFF -> new float[] {0.0F, 0.0F, 0.0F};
        };
    }
}
