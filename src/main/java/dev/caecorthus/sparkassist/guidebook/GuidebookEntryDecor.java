package dev.caecorthus.sparkassist.guidebook;

import java.util.Objects;
import java.util.Optional;

/**
 * An entry's optional {@code decor} JSON object: single decoration choices that override its faction defaults.
 * Values are the enum names of {@code DecorSet.Plate}, {@code DecorSet.Sigil} and {@code DecorSet.Foliage}, case
 * insensitive; a missing or unknown value keeps the default.
 * 条目可选的 decor JSON 对象：单项覆盖阵营默认的点缀选择。取值为 DecorSet 中 Plate、Sigil、Foliage 的枚举名
 * （不区分大小写），缺省或未知时保留默认。
 */
public record GuidebookEntryDecor(Optional<String> plate, Optional<String> sigil, Optional<String> foliage) {
    public static final GuidebookEntryDecor NONE =
            new GuidebookEntryDecor(Optional.empty(), Optional.empty(), Optional.empty());

    public GuidebookEntryDecor {
        Objects.requireNonNull(plate, "plate");
        Objects.requireNonNull(sigil, "sigil");
        Objects.requireNonNull(foliage, "foliage");
    }

    public boolean isEmpty() {
        return plate.isEmpty() && sigil.isEmpty() && foliage.isEmpty();
    }
}
