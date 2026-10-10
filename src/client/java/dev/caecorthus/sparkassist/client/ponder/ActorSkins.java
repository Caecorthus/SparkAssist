package dev.caecorthus.sparkassist.client.ponder;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Player skins in one role colour for demo actors. Every base-layer face is a shade of that colour (arms a little
 * darker, legs darker still, each face edged one shade down) so the limbs stay readable on a one-colour figure;
 * the outer layer is empty, and two dark eyes show which way the actor faces. Generated once per colour.
 * 演示演员用的身份色玩家皮肤。底层每个面都是这种颜色的深浅变化（手臂略深、腿更深，每个面描一圈深一档的边），
 * 让单色人物的四肢依然清楚；外层透明，两只深色眼睛标出朝向。每种颜色只生成一次。
 */
public final class ActorSkins {
    private static final int SIZE = 64;
    // Base-layer cuboids of the 64x64 wide-arm skin: {u, v, width, height, depth, shade %}.
    // 64x64 宽手臂皮肤底层的长方体：{u, v, 宽, 高, 深, 明度百分比}。
    private static final int[][] PARTS = {
            {0, 0, 8, 8, 8, 100},
            {16, 16, 8, 12, 4, 100},
            {40, 16, 4, 12, 4, 88},
            {32, 48, 4, 12, 4, 88},
            {0, 16, 4, 12, 4, 78},
            {16, 48, 4, 12, 4, 78}
    };
    private static final int EDGE_SHADE = 82;
    // Eye pixels on the face (front of the head, x 8..15, y 8..15).
    // 脸上的眼睛像素（头部正面，x 8..15，y 8..15）。
    private static final int[][] EYES = {{9, 12}, {10, 12}, {13, 12}, {14, 12}};

    private static final Map<Integer, Identifier> CACHE = new HashMap<>();

    private ActorSkins() {
    }

    /** The skin texture for {@code rgb}, created on first use. 指定颜色的皮肤纹理，首次使用时生成。 */
    public static Identifier of(int rgb) {
        return CACHE.computeIfAbsent(rgb & 0xFFFFFF, ActorSkins::create);
    }

    private static Identifier create(int rgb) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, SIZE, SIZE, true);
        for (int[] part : PARTS) {
            int tone = scale(rgb, part[5] / 100.0);
            int u = part[0];
            int v = part[1];
            int w = part[2];
            int h = part[3];
            int d = part[4];
            face(image, u + d, v, w, d, tone);
            face(image, u + d + w, v, w, d, tone);
            face(image, u, v + d, d, h, tone);
            face(image, u + d, v + d, w, h, tone);
            face(image, u + d + w, v + d, d, h, tone);
            face(image, u + d + w + d, v + d, w, h, tone);
        }
        int eye = abgr(scale(rgb, 0.3));
        for (int[] pixel : EYES) {
            image.setColor(pixel[0], pixel[1], eye);
        }
        return MinecraftClient.getInstance().getTextureManager()
                .registerDynamicTexture("sparkassist_actor_" + Integer.toHexString(rgb), new NativeImageBackedTexture(image));
    }

    /** One cuboid face: the tone, edged one shade darker. 长方体的一个面：底色加一圈深一档的边。 */
    private static void face(NativeImage image, int x0, int y0, int w, int h, int tone) {
        int fill = abgr(tone);
        int edge = abgr(scale(tone, EDGE_SHADE / 100.0));
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                boolean border = x == x0 || y == y0 || x == x0 + w - 1 || y == y0 + h - 1;
                image.setColor(x, y, border ? edge : fill);
            }
        }
    }

    static int scale(int rgb, double factor) {
        int r = (int) Math.round(((rgb >> 16) & 0xFF) * factor);
        int g = (int) Math.round(((rgb >> 8) & 0xFF) * factor);
        int b = (int) Math.round((rgb & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    /** Opaque ABGR, the byte order {@link NativeImage#setColor} expects. NativeImage 要求的不透明 ABGR 顺序。 */
    private static int abgr(int rgb) {
        return 0xFF000000 | ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF);
    }
}
