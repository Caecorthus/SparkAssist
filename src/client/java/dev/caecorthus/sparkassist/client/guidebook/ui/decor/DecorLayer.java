package dev.caecorthus.sparkassist.client.guidebook.ui.decor;

import dev.caecorthus.sparkassist.guidebook.decor.PixelBuffer;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * One cached decoration layer: a {@link PixelBuffer} uploaded to a dynamic texture and drawn as a single quad. The
 * owner regenerates the buffer only when its {@code key} (geometry, seed, settings, set) changes; every other frame
 * costs one texture draw. Textures are registered lazily and destroyed with {@link #close()}; the image is reused
 * while the size stays the same.
 * 一块缓存的点缀层：PixelBuffer 上传为动态纹理，每帧只画一个四边形。只有 key（几何、种子、设置、套别）变化时才重新
 * 生成；纹理延迟注册、close() 时销毁；尺寸不变时复用同一张图。
 */
public final class DecorLayer implements AutoCloseable {
    private final String name;
    @Nullable
    private NativeImageBackedTexture texture;
    @Nullable
    private Identifier id;
    @Nullable
    private Object key;
    private int width;
    private int height;

    /** @param name texture name prefix, e.g. {@code sparkassist_guide_reader} / 纹理名前缀 */
    public DecorLayer(String name) {
        this.name = name;
    }

    /** True when the layer already holds the pixels for {@code key}. 已缓存 key 对应的像素时为真。 */
    public boolean isCurrent(Object key) {
        return id != null && Objects.equals(this.key, key);
    }

    /** Upload {@code buffer} as this layer's pixels for {@code key}. 把 buffer 作为 key 的像素上传。 */
    public void update(Object key, PixelBuffer buffer) {
        TextureManager textures = MinecraftClient.getInstance().getTextureManager();
        if (texture == null || width != buffer.width() || height != buffer.height()) {
            // A GL texture keeps the size it was created with, so a resize means a fresh texture, not a re-upload.
            // GL 纹理尺寸在创建时固定，尺寸变化必须新建纹理，不能只重新上传。
            close();
            NativeImage image = new NativeImage(NativeImage.Format.RGBA, buffer.width(), buffer.height(), false);
            write(image, buffer);
            texture = new NativeImageBackedTexture(image);
            id = textures.registerDynamicTexture(name, texture);
            width = buffer.width();
            height = buffer.height();
            this.key = key;
            return;
        }
        this.key = key;
        NativeImage image = texture.getImage();
        if (image == null) {
            return;
        }
        write(image, buffer);
        texture.upload();
    }

    private static void write(NativeImage image, PixelBuffer buffer) {
        int[] abgr = buffer.toAbgr();
        int w = buffer.width();
        for (int y = 0; y < buffer.height(); y++) {
            for (int x = 0; x < w; x++) {
                image.setColor(x, y, abgr[y * w + x]);
            }
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Draw the whole layer with its top-left at (x, y). 以 (x, y) 为左上角绘制整层。 */
    public void draw(DrawContext context, int x, int y) {
        if (id == null) {
            return;
        }
        context.drawTexture(id, x, y, 0, 0, width, height, width, height);
    }

    /** Draw a sub-rectangle of the layer (u, v, w, h) with its top-left at (x, y). 绘制层内的一块子矩形。 */
    public void draw(DrawContext context, int x, int y, int u, int v, int w, int h) {
        if (id == null) {
            return;
        }
        context.drawTexture(id, x, y, u, v, w, h, width, height);
    }

    @Override
    public void close() {
        if (id != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
        }
        texture = null;
        id = null;
        key = null;
        width = 0;
        height = 0;
    }
}
