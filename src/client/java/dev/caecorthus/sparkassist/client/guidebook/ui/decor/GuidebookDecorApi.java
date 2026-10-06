package dev.caecorthus.sparkassist.client.guidebook.ui.decor;

import dev.caecorthus.sparkassist.client.guidebook.GuidebookClientState;
import dev.caecorthus.sparkassist.guidebook.GuidebookLayout.Region;
import dev.caecorthus.sparkassist.guidebook.GuidebookSessionState;
import dev.caecorthus.sparkassist.guidebook.decor.DecorPalette;
import dev.caecorthus.sparkassist.guidebook.decor.DecorRandom;
import dev.caecorthus.sparkassist.guidebook.decor.DecorSet;
import dev.caecorthus.sparkassist.guidebook.decor.DecorSetResolver;
import dev.caecorthus.sparkassist.guidebook.decor.DecorZones;
import dev.caecorthus.sparkassist.guidebook.decor.Foliage;
import dev.caecorthus.sparkassist.guidebook.decor.OffsetSink;
import dev.caecorthus.sparkassist.guidebook.decor.Ornaments;
import dev.caecorthus.sparkassist.guidebook.decor.PixelBuffer;
import dev.caecorthus.sparkassist.guidebook.decor.PixelSink;
import dev.caecorthus.sparkassist.guidebook.decor.Sigils;
import dev.caecorthus.sparkassist.guidebook.decor.ZonedSink;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

/**
 * The owner info card's decorations, drawn by SparkAssist on behalf of SparkWitch and SparkTraits. Those mods call
 * the two static methods reflectively (they only soft-depend on SparkAssist) at two points of their card draw:
 * {@link #cardChrome} right after the panel, {@link #cardOverlay} after the rows. Everything else — the player's
 * faction set, the round seed, the density and foliage settings — is read here, so the callers pass geometry only.
 * The signatures are part of the cross-mod contract: keep them stable.
 * 信息卡的点缀，由 SparkAssist 代 SparkWitch 与 SparkTraits 绘制。两个模组只软依赖 SparkAssist，因此在卡片绘制的两个
 * 时点反射调用这两个静态方法：面板之后 cardChrome，行之后 cardOverlay。玩家阵营套别、本局种子、密度与枝叶设置都在
 * 这里读取，调用方只传几何。方法签名是跨模组约定，保持稳定。
 */
public final class GuidebookDecorApi {
    private static final int SIGIL_RADIUS = 40;
    private static final int SIGIL_MIN_HEIGHT = 100;
    private static final int OVERLAY_MARGIN = 20;
    private static final DecorLayer SIGIL = new DecorLayer("sparkassist_card_sigil");
    private static final DecorLayer OVERLAY = new DecorLayer("sparkassist_card_overlay");

    private record SigilKey(int width, int height, DecorSet.Sigil sigil, long seed) {
    }

    private record OverlayKey(Region card, DecorSet set, long seed, int density, boolean foliage) {
    }

    private GuidebookDecorApi() {
    }

    /**
     * Frame ornaments and, at full density, the faction sigil engraved in the body. Call right after the card's
     * panel and before its rows; (x, y, width, height) is the panel rectangle.
     * 边框点缀，以及满密度时刻在卡身上的阵营暗纹。在卡片面板之后、行之前调用；参数为面板矩形。
     */
    public static void cardChrome(DrawContext context, int x, int y, int width, int height) {
        if (!DecorSettings.enabled() || width < 24 || height < 24) {
            return;
        }
        PixelSink sink = new DrawContextSink(context);
        Ornaments.innerLine(sink, x, y, width, height);
        Ornaments.cornerPlates(sink, x, y, width, height);
        if (DecorSettings.density() >= 2 && height >= SIGIL_MIN_HEIGHT) {
            DecorSet set = ownerSet();
            SigilKey key = new SigilKey(width, height, set.sigil(), roundSeed());
            if (!SIGIL.isCurrent(key)) {
                PixelBuffer buffer = new PixelBuffer(width - 6, height - 6);
                Sigils.draw(buffer, set.sigil(), buffer.width() / 2, buffer.height() - 43, SIGIL_RADIUS,
                        DecorPalette.WATERMARK_BRASS, new DecorRandom(DecorRandom.ownerSeed("card", roundSeed())));
                SIGIL.update(key, buffer);
            }
            SIGIL.draw(context, x + 3, y + 3);
        }
    }

    /**
     * The dog-ear tab peeking over the top-right corner, the charm hanging under the bottom edge and the faction
     * foliage along the left, bottom and (full density) top rims. Call after the card's rows.
     * 右上角探出的折角书签、底边下的吊坠，以及沿左沿、底沿与（满密度）顶沿的阵营枝叶。在卡片的行之后调用。
     */
    public static void cardOverlay(DrawContext context, int x, int y, int width, int height) {
        int density = DecorSettings.density();
        if (density < 0 || width < 24 || height < 24) {
            return;
        }
        Region card = new Region(x, y, width, height);
        OverlayKey key = new OverlayKey(card, ownerSet(), roundSeed(), density, DecorSettings.foliage());
        int ox = x - OVERLAY_MARGIN;
        int oy = y - OVERLAY_MARGIN;
        if (!OVERLAY.isCurrent(key)) {
            PixelBuffer buffer = new PixelBuffer(width + 2 * OVERLAY_MARGIN, height + 2 * OVERLAY_MARGIN);
            PixelSink sink = new OffsetSink(buffer, -ox, -oy);
            DecorSet set = key.set();
            DecorRandom rand = new DecorRandom(DecorRandom.ownerSeed("card-overlay", key.seed()));
            int right = x + width;
            int bottom = y + height;
            int tabTop = Math.max(DecorZones.HUD_HEIGHT, y - 4);
            Ornaments.ribbon(sink, right - 14, tabTop, y + 9,
                    DecorPalette.mix(DecorPalette.opaque(set.colour()), DecorPalette.INK, 0.25));
            if (density >= 1) {
                Ornaments.charm(sink, x + 16, bottom + 1, set.charm());
            }
            if (key.foliage()) {
                List<Region> forbidden = List.of(new Region(x + 3, y + 3, width - 6, height - 6),
                        new Region(x + 12, bottom, 10, Ornaments.CHARM_HEIGHT + 2),
                        new Region(right - 16, tabTop, 9, y + 10 - tabTop),
                        new Region(0, 0, Integer.MAX_VALUE / 2, DecorZones.HUD_HEIGHT));
                int[] lengths = {30, 50, 70};
                grow(sink, forbidden, new Region(x - 5, y + 20, 10, height - 24),
                        Foliage.Vine.alongRim(set.foliage(), x + 1, bottom - 8, -Math.PI / 2, lengths[density],
                                set.berry()), rand);
                grow(sink, forbidden, new Region(x + 24, bottom - 4, width - 30, 10),
                        Foliage.Vine.alongRim(set.foliage(), x + 28, bottom - 2, 0, lengths[density], set.berry()),
                        rand);
                if (density >= 2) {
                    grow(sink, forbidden, new Region(x + 8, Math.max(DecorZones.HUD_HEIGHT, y - 4), width - 30, 7),
                            Foliage.Vine.alongRim(set.foliage(), right - 20, y + 1, Math.PI, 80, set.berry()), rand);
                }
            }
            OVERLAY.update(key, buffer);
        }
        OVERLAY.draw(context, ox, oy);
    }

    /** Drop the card textures (disconnect); they come back on the next draw. 释放卡片纹理（断开连接时），下次绘制重建。 */
    public static void release() {
        SIGIL.close();
        OVERLAY.close();
    }

    private static void grow(PixelSink sink, List<Region> forbidden, Region strip, Foliage.Vine vine,
                             DecorRandom rand) {
        ZonedSink zoned = new ZonedSink(sink, List.of(strip), forbidden);
        Foliage.grow(zoned, zoned::allowed, vine, rand);
    }

    private static DecorSet ownerSet() {
        return DecorSetResolver.forRole(session().currentRoleId().orElse(null));
    }

    private static long roundSeed() {
        return session().roundSeed();
    }

    private static GuidebookSessionState session() {
        return GuidebookClientState.session();
    }
}
