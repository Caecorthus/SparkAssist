package dev.caecorthus.sparkassist.client.guidebook;

import dev.caecorthus.sparkassist.guidebook.GuidebookCatalog;
import dev.caecorthus.sparkassist.guidebook.GuidebookEntry;
import dev.caecorthus.sparkassist.guidebook.GuidebookTab;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Composes authored resource entries with read-only entries discovered from installed registries.
 * 将资源文件中的正文条目与已安装模组注册表中的只读条目合并。
 */
public final class GuidebookRuntimeCatalog {
    private static final Logger LOGGER = LoggerFactory.getLogger("SparkAssist/Guidebook");

    private GuidebookRuntimeCatalog() {
    }

    public static GuidebookCatalog load(MinecraftClient client) {
        GuidebookCatalog authored = loadAuthoredEntries(client);
        Set<String> authoredIds = new HashSet<>();
        authored.entries().forEach(entry -> authoredIds.add(entry.id()));

        List<GuidebookEntry> discovered = new ArrayList<>();
        discoverRoles(discovered, authoredIds);
        discoverOptionalRegistry(
                discovered,
                authoredIds,
                "sparktraits",
                "dev.caecorthus.sparktraits.api.TraitRegistry",
                GuidebookTab.TRAIT,
                "trait"
        );
        discoverOptionalRegistry(
                discovered,
                authoredIds,
                "sparkwitch",
                "dev.caecorthus.sparkwitch.api.WitchSkillRegistry",
                GuidebookTab.SKILL,
                "skill"
        );

        GuidebookCatalog combined = GuidebookCatalog.merge(List.of(
                authored,
                GuidebookCatalog.of(discovered)
        ));
        return combined.availableFor(loadedModIds());
    }

    private static GuidebookCatalog loadAuthoredEntries(MinecraftClient client) {
        Map<Identifier, Resource> resources = client.getResourceManager().findResources(
                "guidebook",
                id -> id.getPath().endsWith(".json")
        );
        List<GuidebookCatalog> catalogs = new ArrayList<>();
        resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
                .forEach(entry -> {
                    try (var reader = entry.getValue().getReader()) {
                        catalogs.add(GuidebookCatalog.parse(reader.lines().reduce("", (left, right) -> left + right)));
                    } catch (IOException | RuntimeException exception) {
                        LOGGER.error("Could not load guidebook resource {}", entry.getKey(), exception);
                    }
                });
        try {
            return GuidebookCatalog.merge(catalogs);
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Guidebook resources contain duplicate entry ids", exception);
            return GuidebookCatalog.of(List.of());
        }
    }

    private static void discoverRoles(List<GuidebookEntry> entries, Set<String> authoredIds) {
        int order = 1_000;
        for (Role role : WatheRoles.ROLES) {
            Identifier id = role.identifier();
            if (role == WatheRoles.NO_ROLE
                    || role == WatheRoles.DISCOVERY_CIVILIAN
                    || authoredIds.contains(id.toString())) {
                continue;
            }
            entries.add(new GuidebookEntry(
                    id.toString(),
                    GuidebookTab.ROLE,
                    id.getNamespace(),
                    "announcement.role." + id.getPath(),
                    "guidebook.sparkassist.content.role.overview",
                    List.of(
                            "guidebook.sparkassist.content.role.overview",
                            "guidebook.sparkassist.content.role.abilities",
                            "guidebook.sparkassist.content.role.items"
                    ),
                    List.of(),
                    List.of(id.getNamespace()),
                    order++
            ));
        }
    }

    private static void discoverOptionalRegistry(
            List<GuidebookEntry> entries,
            Set<String> authoredIds,
            String modId,
            String registryClassName,
            GuidebookTab tab,
            String translationPrefix
    ) {
        if (!FabricLoader.getInstance().isModLoaded(modId)) {
            return;
        }
        try {
            Class<?> registryClass = Class.forName(registryClassName);
            Method valuesMethod = registryClass.getMethod("values");
            Collection<?> values = (Collection<?>) valuesMethod.invoke(null);
            int order = 1_000;
            for (Object value : values) {
                Identifier id = (Identifier) value.getClass().getMethod("id").invoke(value);
                if (authoredIds.contains(id.toString())) {
                    continue;
                }
                String path = id.getPath().replace('/', '.');
                String baseKey = translationPrefix + "." + id.getNamespace() + "." + path;
                entries.add(new GuidebookEntry(
                        id.toString(),
                        tab,
                        modId,
                        baseKey + ".name",
                        baseKey + ".description",
                        List.of(baseKey + ".description"),
                        ownerRoleIds(tab, id),
                        List.of(modId),
                        order++
                ));
            }
        } catch (ClassNotFoundException exception) {
            LOGGER.debug("Optional guidebook registry {} is unavailable", registryClassName);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException exception) {
            LOGGER.warn("Could not read optional guidebook registry {}", registryClassName, exception);
        }
    }

    private static List<String> ownerRoleIds(GuidebookTab tab, Identifier id) {
        if (tab != GuidebookTab.SKILL || !id.getNamespace().equals("sparkwitch")) {
            return List.of();
        }
        return switch (id.getPath()) {
            case "ceremonial_sword" -> List.of("sparkwitch:grand_witch");
            case "mighty_force", "swift_step", "murder_sense", "healing", "clairvoyance" ->
                    List.of("sparkwitch:apprentice_witch");
            case "pig_chase" -> List.of("sparkwitch:pig_god");
            case "death_ray" -> List.of("sparkwitch:murderous_witch");
            default -> List.of();
        };
    }

    private static Set<String> loadedModIds() {
        Set<String> ids = new HashSet<>();
        FabricLoader.getInstance().getAllMods().forEach(mod -> ids.add(mod.getMetadata().getId()));
        return Set.copyOf(ids);
    }
}
