package dev.caecorthus.sparkassist.client.guidebook.data;

import dev.caecorthus.sparkassist.SparkAssist;
import dev.caecorthus.sparkassist.guidebook.GuidebookCatalog;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recursively loads authored GuideBook entry files from resource packs.
 * 从资源包中递归加载人工编写的指南书条目文件。
 */
public final class GuidebookResourceLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Guidebook");

    private GuidebookResourceLoader() {
    }

    public static GuidebookCatalog load(ResourceManager resourceManager) {
        Map<Identifier, Resource> resources = resourceManager.findResources(
                "guidebook",
                id -> id.getNamespace().equals(SparkAssist.MOD_ID) && id.getPath().endsWith(".json")
        );
        List<GuidebookCatalog> catalogs = new ArrayList<>();
        Set<String> loadedEntryIds = new HashSet<>();
        resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
                .forEach(entry -> parseResource(catalogs, loadedEntryIds, entry));
        return GuidebookCatalog.merge(catalogs);
    }

    private static void parseResource(
            List<GuidebookCatalog> catalogs,
            Set<String> loadedEntryIds,
            Map.Entry<Identifier, Resource> entry
    ) {
        try (var reader = entry.getValue().getReader()) {
            GuidebookCatalog catalog = GuidebookCatalog.parse(reader.lines().collect(Collectors.joining()));
            List<String> duplicateIds = catalog.entries().stream()
                    .map(guidebookEntry -> guidebookEntry.id())
                    .filter(loadedEntryIds::contains)
                    .toList();
            if (!duplicateIds.isEmpty()) {
                LOGGER.error("Skipping guidebook resource {} with duplicate entry ids {}", entry.getKey(), duplicateIds);
                return;
            }
            catalog.entries().forEach(guidebookEntry -> loadedEntryIds.add(guidebookEntry.id()));
            catalogs.add(catalog);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Could not load guidebook resource {}", entry.getKey(), exception);
        }
    }
}
