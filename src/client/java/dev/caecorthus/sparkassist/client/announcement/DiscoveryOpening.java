package dev.caecorthus.sparkassist.client.announcement;

import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;

/**
 * Times the exploration opening. Vanilla never sends an announcement packet in Discovery mode — its game mode simply
 * assigns roles and starts — so there is no {@code welcomeTime} to animate against and the client has to clock it.
 * 为探索模式的开场计时。原版在探索模式下从不发送报幕包——它的游戏模式只是分配角色然后开始——因此没有
 * {@code welcomeTime} 可供驱动动画，只能由客户端自己计时。
 *
 * <p>The clock starts on the tick a Discovery round begins running and stops when it is no longer running, so the
 * opening replays for each new round rather than only the first.
 * 时钟在探索对局开始运行的 tick 启动，在不再运行时清零，因此每次新对局都会重新播一遍开场，而不是只播第一次。</p>
 */
public final class DiscoveryOpening {
    /** Same window as Wathe's own announcement, so the pacing matches the other modes. 与 Wathe 自身报幕同一窗口，使节奏与其他模式一致。 */
    public static final int TOTAL = ExpressAnnouncement.TOTAL;

    private static long startedAt = -1L;
    private static boolean wasRunning;

    private DiscoveryOpening() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(DiscoveryOpening::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null) {
            startedAt = -1L;
            wasRunning = false;
            return;
        }
        GameMode mode = GameWorldComponent.KEY.maybeGet(client.world)
                .map(GameWorldComponent::getGameMode)
                .orElse(null);
        boolean discovery = mode != null && WatheGameModes.DISCOVERY_ID.equals(mode.identifier);
        boolean running = GameWorldComponent.KEY.maybeGet(client.world)
                .map(GameWorldComponent::isRunning)
                .orElse(false);
        if (!discovery || !running) {
            startedAt = -1L;
            wasRunning = false;
            return;
        }
        if (!wasRunning) {
            startedAt = Util.getMeasuringTimeMs();
        }
        wasRunning = true;
    }

    /** Remaining ticks of the exploration opening, 0 when none is running. 探索开场的剩余 tick；没有在进行时返回 0。 */
    public static int remainingTicks() {
        if (startedAt < 0L) {
            return 0;
        }
        long elapsedMs = Util.getMeasuringTimeMs() - startedAt;
        int elapsedTicks = (int) (elapsedMs * 20L / 1000L);
        return Math.max(0, TOTAL - elapsedTicks);
    }
}
