package dev.caecorthus.sparkassist.client.guidebook;

import dev.caecorthus.sparkassist.SparkAssist;
import dev.caecorthus.sparkassist.client.guidebook.ui.decor.PlateArtLoader;
import dev.caecorthus.sparkassist.guidebook.GuidebookCatalog;
import dev.doctor4t.wathe.api.WatheRoles;
import java.util.List;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

/**
 * The guide's resource data, read once and kept until the next resource reload: the catalog, the Chinese translation
 * table and the chapter plates. Wathe builds a new inventory, and with it a new embedded guide, on every open, so
 * reading these per screen stalled every open.
 * 指南的资源数据只读一次，保留到下一次资源重载：目录、中文翻译表与扉画。Wathe 每次打开背包都会新建背包界面及其中的
 * 嵌入式指南，逐个界面读取会让每次打开都卡顿。
 */
public final class GuidebookResources {
    private static GuidebookCatalog catalog;
    private static int catalogRoles = -1;
    private static TranslationStorage chineseTranslations;

    private GuidebookResources() {
    }

    public static void register() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.of(SparkAssist.MOD_ID, "guidebook_resources");
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        catalog = null;
                        chineseTranslations = null;
                        PlateArtLoader.clear();
                    }
                });
    }

    public static GuidebookCatalog catalog(MinecraftClient client) {
        // Roles are registered at mod init; a different count means one came later, so discover them again.
        // 身份在模组初始化时注册；数量变化说明有身份晚到，重新发现一次。
        int roles = WatheRoles.ROLES.size();
        if (catalog == null || catalogRoles != roles) {
            catalog = GuidebookRuntimeCatalog.load(client);
            catalogRoles = roles;
        }
        return catalog;
    }

    public static TranslationStorage chineseTranslations(MinecraftClient client) {
        if (chineseTranslations == null) {
            chineseTranslations = TranslationStorage.load(client.getResourceManager(), List.of("zh_cn"), false);
        }
        return chineseTranslations;
    }
}
