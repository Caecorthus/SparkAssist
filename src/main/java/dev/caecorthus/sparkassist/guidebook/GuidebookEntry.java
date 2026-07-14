package dev.caecorthus.sparkassist.guidebook;

import dev.caecorthus.sparkassist.guidebook.content.GuidebookPage;
import java.util.List;
import java.util.Objects;

public record GuidebookEntry(
        String id,
        GuidebookTab tab,
        String sourceModId,
        String nameKey,
        String summaryKey,
        List<String> pageKeys,
        List<GuidebookPage> pages,
        List<String> ownerRoleIds,
        List<String> requiredModIds,
        int color,
        int order
) {
    public static final int DEFAULT_COLOR = 0x3B2A1A;

    public GuidebookEntry(
            String id,
            GuidebookTab tab,
            String sourceModId,
            String nameKey,
            String summaryKey,
            List<String> pageKeys,
            List<String> ownerRoleIds,
            List<String> requiredModIds,
            int order
    ) {
        this(
                id,
                tab,
                sourceModId,
                nameKey,
                summaryKey,
                pageKeys,
                List.of(),
                ownerRoleIds,
                requiredModIds,
                DEFAULT_COLOR,
                order
        );
    }

    public GuidebookEntry(
            String id,
            GuidebookTab tab,
            String sourceModId,
            String nameKey,
            String summaryKey,
            List<String> pageKeys,
            List<String> ownerRoleIds,
            List<String> requiredModIds,
            int color,
            int order
    ) {
        this(
                id,
                tab,
                sourceModId,
                nameKey,
                summaryKey,
                pageKeys,
                List.of(),
                ownerRoleIds,
                requiredModIds,
                color,
                order
        );
    }

    public GuidebookEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tab, "tab");
        Objects.requireNonNull(sourceModId, "sourceModId");
        Objects.requireNonNull(nameKey, "nameKey");
        Objects.requireNonNull(summaryKey, "summaryKey");
        pageKeys = List.copyOf(Objects.requireNonNull(pageKeys, "pageKeys"));
        pages = List.copyOf(Objects.requireNonNull(pages, "pages"));
        ownerRoleIds = List.copyOf(Objects.requireNonNull(ownerRoleIds, "ownerRoleIds"));
        requiredModIds = List.copyOf(Objects.requireNonNull(requiredModIds, "requiredModIds"));
        if (color < 0 || color > 0xFFFFFF) {
            throw new IllegalArgumentException("color must be a 24-bit RGB value");
        }
    }
}
