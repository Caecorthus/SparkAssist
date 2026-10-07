package dev.caecorthus.sparkassist.client.guidebook.ui.decor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.caecorthus.sparkassist.SparkAssist;
import dev.caecorthus.sparkassist.guidebook.decor.PlateArt;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads each page's hand-made chapter plate from {@code assets/sparkassist/guidebook_plates/<namespace>/<path>.png}
 * with its {@code .json} (focus and emblem position), once per entry. A page without one, or with a broken one,
 * falls back to its faction's procedural scene.
 * 读取每页手绘扉画：assets/sparkassist/guidebook_plates/&lt;命名空间&gt;/&lt;路径&gt;.png 及同名 .json（故事中心与徽记位置），
 * 每个条目只读一次。没有或损坏时退回阵营的程序化景。
 */
public final class PlateArtLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/PlateArt");
    private static final String ROOT = "guidebook_plates/";
    private static final Map<String, Optional<PlateArt>> CACHE = new ConcurrentHashMap<>();

    private PlateArtLoader() {
    }

    public static Optional<PlateArt> forEntry(String entryId) {
        int colon = entryId == null ? -1 : entryId.indexOf(':');
        if (colon <= 0) {
            return Optional.empty();
        }
        return CACHE.computeIfAbsent(entryId, id -> load(MinecraftClient.getInstance().getResourceManager(),
                id.substring(0, colon), id.substring(colon + 1)));
    }

    /** Forget every plate so they are read again (resource reload). 清空缓存，下次重新读取（资源重载时）。 */
    public static void clear() {
        CACHE.clear();
    }

    private static Optional<PlateArt> load(ResourceManager resources, String namespace, String path) {
        String base = ROOT + namespace + "/" + path;
        Optional<Resource> image = resources.getResource(Identifier.of(SparkAssist.MOD_ID, base + ".png"));
        Optional<Resource> meta = resources.getResource(Identifier.of(SparkAssist.MOD_ID, base + ".json"));
        if (image.isEmpty() || meta.isEmpty()) {
            return Optional.empty();
        }
        try (InputStream in = image.get().getInputStream(); NativeImage picture = NativeImage.read(in);
             Reader reader = meta.get().getReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray emblem = json.getAsJsonArray("emblem");
            int width = picture.getWidth();
            int height = picture.getHeight();
            int[] argb = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int abgr = picture.getColor(x, y);
                    argb[y * width + x] = (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF);
                }
            }
            return Optional.of(PlateArt.of(width, height, argb, json.get("focus").getAsInt(),
                    emblem.get(0).getAsInt(), emblem.get(1).getAsInt()));
        } catch (Exception broken) {
            LOGGER.warn("Could not read guidebook plate {}", base, broken);
            return Optional.empty();
        }
    }
}
