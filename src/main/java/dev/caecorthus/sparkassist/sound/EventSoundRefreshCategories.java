package dev.caecorthus.sparkassist.sound;

import java.util.List;
import net.minecraft.sound.SoundCategory;

/**
 * Refresh metadata for categories that can contain SparkAssist event sounds.
 * 可能包含 SparkAssist 事件音效的分类刷新元数据。
 */
public final class EventSoundRefreshCategories {
    private static final List<SoundCategory> REFRESH_CATEGORIES = List.of(
            SoundCategory.AMBIENT,
            SoundCategory.MUSIC,
            SoundCategory.PLAYERS
    );

    private EventSoundRefreshCategories() {
    }

    /**
     * Categories whose active sounds need vanilla volume recalculation after slider changes.
     * 滑块变化后需要让原版重新计算活动声音音量的分类。
     */
    public static List<SoundCategory> refreshCategories() {
        return REFRESH_CATEGORIES;
    }
}
