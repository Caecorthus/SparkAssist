package dev.caecorthus.sparkassist.sound;

import java.util.List;
import net.minecraft.sound.SoundCategory;

/**
 * Which category SparkAssist pokes so vanilla recalculates playing volumes. In 1.21.1 {@code SoundSystem
 * .updateSoundVolume} recalculates every playing sound for any category except MASTER (MASTER only sets the listener
 * gain), whatever the sounds' own categories, so one non-MASTER call covers event sounds on AMBIENT, MUSIC, PLAYERS
 * and MASTER alike.
 * SparkAssist 用来让原版重算正在播放音量的分类。在 1.21.1 中，对 MASTER 以外的任何分类调用
 * {@code SoundSystem.updateSoundVolume} 都会重算所有正在播放的声音（MASTER 只设置监听器增益），与声音自身的分类无关，
 * 因此一次非 MASTER 调用就覆盖了 AMBIENT、MUSIC、PLAYERS 乃至 MASTER 上的事件音效。
 */
public final class EventSoundRefreshCategories {
    private static final List<SoundCategory> REFRESH_CATEGORIES = List.of(SoundCategory.AMBIENT);

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
