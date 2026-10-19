package dev.caecorthus.sparkassist.client.settings;

import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * One sound a volume row can play as a sample: the event id and the category it uses in game. It plays through the
 * normal sound system, so the vanilla category volume, the row's own volume and the SparkDLC master all apply, and
 * moving the slider changes it while it plays. Only one sample plays at a time and it stops after ten seconds.
 * 音量行可试听的一个声音：事件 id 及其在游戏里所用的分类。试听走正常的声音系统，因此原版分类音量、该行音量与 SparkDLC
 * 总音量都会生效，拖动滑块时正在播放的试听也随之变化。同一时间只播放一个试听，十秒后自动停止。
 *
 * @param sound    sound event id / 声音事件 id
 * @param category the category the game plays it on / 游戏中播放它所用的分类
 */
public record SoundPreview(Identifier sound, SoundCategory category) {
    /** How long a sample may play, in client ticks. 试听最长播放时间（客户端刻）。 */
    static final int MAX_TICKS = 200;

    @Nullable
    private static SoundInstance playing;
    @Nullable
    private static SoundPreview playingPreview;
    private static int ticks;

    public SoundPreview {
        Objects.requireNonNull(sound);
        Objects.requireNonNull(category);
    }

    /** Whether the sound manager knows this event (its mod and sounds.json are loaded). 声音管理器是否认识该事件。 */
    public boolean available() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.getSoundManager() != null && client.getSoundManager().get(sound) != null;
    }

    public boolean playing() {
        return this.equals(playingPreview) && playing != null
                && MinecraftClient.getInstance().getSoundManager().isPlaying(playing);
    }

    /** Starts this sample, stopping any other; stops it if it is already playing. 播放此试听（并停止其他试听）；已在播放时停止。 */
    public void toggle() {
        boolean wasPlaying = playing();
        stopAll();
        if (wasPlaying) {
            return;
        }
        SoundInstance instance = new PositionedSoundInstance(sound, category, 1.0F, 1.0F,
                SoundInstance.createRandom(), false, 0, SoundInstance.AttenuationType.NONE, 0.0D, 0.0D, 0.0D, true);
        MinecraftClient.getInstance().getSoundManager().play(instance);
        playing = instance;
        playingPreview = this;
        ticks = 0;
    }

    /** Stops whatever sample is playing. 停止正在播放的试听。 */
    public static void stopAll() {
        if (playing != null) {
            MinecraftClient.getInstance().getSoundManager().stop(playing);
        }
        playing = null;
        playingPreview = null;
        ticks = 0;
    }

    /** Called every client tick while the settings screen is open. 设置界面打开时每个客户端刻调用。 */
    public static void tick() {
        if (playing == null) {
            return;
        }
        if (++ticks >= MAX_TICKS || !MinecraftClient.getInstance().getSoundManager().isPlaying(playing)) {
            stopAll();
        }
    }
}
