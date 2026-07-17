package dev.caecorthus.sparkassist.guidebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class GuidebookLocalizationResourcesTest {
    private static final Path GUIDEBOOK_ROOT = Path.of(
            "src/client/resources/assets/sparkassist/guidebook"
    );
    private static final Path LANG_ROOT = Path.of(
            "src/client/resources/assets/sparkassist/lang"
    );

    @Test
    void bothLocalesProvideChineseFallbackNamesForEveryAuthoredEntry() throws IOException {
        Set<String> nameKeys = authoredNameKeys();
        assertEquals(107, nameKeys.size());

        for (String locale : List.of("zh_cn", "en_us")) {
            JsonObject translations = JsonParser.parseString(
                    Files.readString(LANG_ROOT.resolve(locale + ".json"))
            ).getAsJsonObject();
            assertTrue(nameKeys.stream().allMatch(translations::has), locale);
            assertEquals("黑羽鸦", translations.get("announcement.role.black_raven").getAsString());
            assertEquals("诅咒者", translations.get("announcement.role.curser").getAsString());
            assertEquals("守护天使", translations.get("announcement.role.guardian_angel").getAsString());
            assertEquals("猎人", translations.get("announcement.role.hunter").getAsString());
            assertEquals("骨科大夫", translations.get("announcement.role.orthopedist").getAsString());
            assertEquals("调香师", translations.get("announcement.role.perfumer").getAsString());
            assertEquals("绑架者", translations.get("announcement.role.kidnapper").getAsString());
            assertEquals("忍者", translations.get("announcement.role.ninja").getAsString());
            assertEquals("圣徒", translations.get("announcement.role.saint").getAsString());
            assertEquals("破坏者", translations.get("announcement.role.saboteur").getAsString());
            assertEquals("仇杀客", translations.get("announcement.role.vendetta").getAsString());
            assertEquals("风精灵", translations.get("announcement.role.wind_spirit").getAsString());
            assertEquals("冤魂", translations.get("announcement.role.wraith").getAsString());
            assertEquals("灵探", translations.get("trait.sparktraits.spirit_sleuth.name").getAsString());
        }
    }

    private static Set<String> authoredNameKeys() throws IOException {
        try (var paths = Files.walk(GUIDEBOOK_ROOT)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .flatMap(path -> parse(path).entries().stream())
                    .map(GuidebookEntry::nameKey)
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    private static GuidebookCatalog parse(Path path) {
        try {
            return GuidebookCatalog.parse(Files.readString(path));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + path, exception);
        }
    }
}
