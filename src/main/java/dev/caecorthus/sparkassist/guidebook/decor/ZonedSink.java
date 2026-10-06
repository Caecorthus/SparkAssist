package dev.caecorthus.sparkassist.guidebook.decor;

import dev.caecorthus.sparkassist.guidebook.GuidebookLayout.Region;
import java.util.List;

/**
 * A sink that admits a pixel only inside one of the {@code allowed} rectangles and outside every {@code forbidden}
 * one. Steam curls and vines paint through it, so no stroke can touch text, scrollbars, buttons or the HUD: the
 * rule is enforced per pixel, not by the generator.
 * 只放行“落在某个允许矩形内且不在任何禁区内”的像素。蒸汽卷云与枝条经由它落笔，笔画碰不到文字、滚动条、按钮与
 * HUD：规则逐像素执行，不依赖生成器自觉。
 */
public final class ZonedSink implements PixelSink {
    private final PixelSink inner;
    private final List<Region> allowed;
    private final List<Region> forbidden;

    /** An empty {@code allowed} list admits everything the inner sink accepts. 允许列表为空时不限制。 */
    public ZonedSink(PixelSink inner, List<Region> allowed, List<Region> forbidden) {
        this.inner = inner;
        this.allowed = List.copyOf(allowed);
        this.forbidden = List.copyOf(forbidden);
    }

    @Override
    public int width() {
        return inner.width();
    }

    @Override
    public int height() {
        return inner.height();
    }

    public boolean admits(int x, int y) {
        if (!allowed.isEmpty()) {
            boolean inside = false;
            for (Region region : allowed) {
                if (region.contains(x, y)) {
                    inside = true;
                    break;
                }
            }
            if (!inside) {
                return false;
            }
        }
        for (Region region : forbidden) {
            if (region.contains(x, y)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void set(int x, int y, int argb) {
        if (admits(x, y)) {
            inner.set(x, y, argb);
        }
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                set(x, y, argb);
            }
        }
    }
}
