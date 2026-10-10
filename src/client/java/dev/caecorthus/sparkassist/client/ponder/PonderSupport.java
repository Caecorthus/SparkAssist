package dev.caecorthus.sparkassist.client.ponder;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * Optional Ponder (思索) integration. This class never touches Ponder types, so it is safe to load without Ponder;
 * everything that does lives behind {@link #isAvailable()}.
 * 可选的 Ponder（思索）联动。本类不引用任何 Ponder 类型，未安装 Ponder 时也能安全加载；
 * 用到 Ponder 的代码都在 {@link #isAvailable()} 之后。
 */
public final class PonderSupport {
    public static final String MOD_ID = "ponder";

    private PonderSupport() {
    }

    public static boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static void init() {
        if (isAvailable()) {
            SparkPonderPlugin.register();
        }
    }

    /**
     * Whether the guide can play demo {@code target} now: Ponder is installed, the demo exists, and a world is loaded
     * (Ponder builds its scene world on top of the client world, so nothing plays on the title screen).
     * 指南书此刻能否播放演示：已安装 Ponder、演示存在且已进入世界（Ponder 的场景世界依附于客户端世界，标题界面无法播放）。
     */
    public static boolean canPlay(String target) {
        return isAvailable() && MinecraftClient.getInstance().world != null && SparkPonderDemos.has(target);
    }

    /** Open demo {@code target}; closing it returns to {@code returnTo}. 打开演示；关闭后回到 returnTo。 */
    public static void play(String target, @Nullable Screen returnTo) {
        if (canPlay(target)) {
            SparkPonderDemos.open(target, returnTo);
        }
    }
}
