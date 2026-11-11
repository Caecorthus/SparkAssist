package dev.caecorthus.sparkassist.client.charge;

/**
 * Turns "held for this long" into what the indicator shows.
 * 把「已按住多久」换算成指示要显示的内容。
 */
public final class ChargeMeter {
    /**
     * How long an item has to be held.
     * 一件道具需要按住多久。
     *
     * @param readyTicks first held tick on which a release counts / 松手开始有效的第一个按住刻
     * @param fullTicks  held tick at which holding longer adds nothing; equal to {@code readyTicks} for everything
     *                   except items whose throw keeps gaining power / 再按也不会更强的按住刻；除了投掷力度随蓄力增长的
     *                   道具以外都等于 {@code readyTicks}
     */
    public record Window(int readyTicks, int fullTicks) {
        public Window {
            if (readyTicks < 1 || fullTicks < readyTicks) {
                throw new IllegalArgumentException("window " + readyTicks + "/" + fullTicks);
            }
        }

        public static Window of(int ticks) {
            return new Window(ticks, ticks);
        }
    }

    /**
     * One frame of the indicator.
     * 指示的一帧。
     *
     * @param fill  0..1 of the gauge / 仪表填充比例
     * @param ready whether releasing now counts / 此刻松手是否有效
     */
    public record Reading(float fill, boolean ready) {
    }

    private ChargeMeter() {
    }

    /**
     * @param heldTicks   whole ticks the item has been in use / 物品已使用的整刻数
     * @param partialTick 0..1 progress into the next tick, so the gauge moves between ticks instead of stepping
     *                    / 进入下一刻的进度，使仪表在两刻之间连续移动而不是跳格
     */
    public static Reading read(int heldTicks, float partialTick, Window window) {
        float held = Math.max(0, heldTicks) + Math.max(0.0F, Math.min(1.0F, partialTick));
        boolean ready = heldTicks >= window.readyTicks();
        // The partial tick runs ahead of the game: the gauge may only read full once the tick that makes it so has
        // really happened.
        // 帧内插值跑在游戏前面：只有让它读满的那一刻真的发生之后，仪表才可以读满。
        float fill = heldTicks >= window.fullTicks() ? 1.0F : Math.min(held / window.fullTicks(), 0.999F);
        return new Reading(fill, ready);
    }
}
