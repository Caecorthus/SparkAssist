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
            if (locale.equals("zh_cn")) {
                assertEquals("诅咒者", translations.get("announcement.role.curser").getAsString());
                assertEquals("守护天使", translations.get("announcement.role.guardian_angel").getAsString());
                assertEquals("破坏者", translations.get("announcement.role.saboteur").getAsString());
                assertEquals("仇杀客", translations.get("announcement.role.vendetta").getAsString());
                assertEquals("风精灵", translations.get("announcement.role.wind_spirit").getAsString());
                assertEquals("冤魂", translations.get("announcement.role.wraith").getAsString());
            } else {
                assertEquals("Curser", translations.get("announcement.role.curser").getAsString());
                assertEquals("Guardian Angel", translations.get("announcement.role.guardian_angel").getAsString());
                assertEquals("Saboteur", translations.get("announcement.role.saboteur").getAsString());
                assertEquals("Vendetta", translations.get("announcement.role.vendetta").getAsString());
                assertEquals("Wind Spirit", translations.get("announcement.role.wind_spirit").getAsString());
                assertEquals("Wraith", translations.get("announcement.role.wraith").getAsString());
            }
            assertEquals("猎人", translations.get("announcement.role.hunter").getAsString());
            assertEquals("忍者", translations.get("announcement.role.ninja").getAsString());
            assertEquals("骨科大夫", translations.get("announcement.role.orthopedist").getAsString());
            assertEquals("调香师", translations.get("announcement.role.perfumer").getAsString());
            assertEquals("圣徒", translations.get("announcement.role.saint").getAsString());
            assertEquals("灵探", translations.get("trait.sparktraits.spirit_sleuth.name").getAsString());
        }
    }

    @Test
    void wraithGuidePagesResolveDistinctChineseAndEnglishText() throws IOException {
        JsonObject chinese = translations("zh_cn");
        JsonObject english = translations("en_us");
        Set<String> wraithTextKeys = authoredWraithTextKeys();

        assertEquals(59, wraithTextKeys.size());
        assertTrue(wraithTextKeys.stream().allMatch(chinese::has));
        assertTrue(wraithTextKeys.stream().allMatch(english::has));
        assertTrue(wraithTextKeys.stream().allMatch(key ->
                !chinese.get(key).getAsString().equals(english.get(key).getAsString())));
        assertTrue(chinese.get("guidebook.sparkassist.sparkwitch.wraith.17").getAsString()
                .contains("不会立即改变身份"));
        assertTrue(english.get("guidebook.sparkassist.sparkwitch.wraith.17").getAsString()
                .contains("does not change the role immediately"));
        assertEquals("转化与本局快照",
                chinese.get("guidebook.sparkassist.sparkwitch.wraith.0").getAsString());
        assertEquals("Conversion and round snapshot",
                english.get("guidebook.sparkassist.sparkwitch.wraith.0").getAsString());
        assertTrue(chinese.get("guidebook.sparkassist.sparkwitch.wraith.9").getAsString()
                .contains("守护天使可以打开并使用文字聊天"));
        assertTrue(english.get("guidebook.sparkassist.sparkwitch.wraith.9").getAsString()
                .contains("Guardian Angel may open and use text chat"));
        assertTrue(chinese.get("guidebook.sparkassist.sparkwitch.wind_spirit.5").getAsString()
                .contains("绝不会影响基础冤魂或任何晋升冤魂身份"));
        assertTrue(english.get("guidebook.sparkassist.sparkwitch.wind_spirit.5").getAsString()
                .contains("never base Wraiths or any promoted Wraith role"));
        assertTrue(chinese.get("guidebook.sparkassist.sparkwitch.guardian_angel.5").getAsString()
                .contains("可以打开并使用文字聊天"));
        assertTrue(english.get("guidebook.sparkassist.sparkwitch.guardian_angel.5").getAsString()
                .contains("may open and use text chat"));

        String renderer = Files.readString(Path.of(
                "src/client/java/dev/caecorthus/sparkassist/client/guidebook/render/GuidebookContentRenderer.java"
        ));
        assertTrue(renderer.contains("Text.translatable(run.translationKey())"));
    }

    private static JsonObject translations(String locale) throws IOException {
        return JsonParser.parseString(Files.readString(LANG_ROOT.resolve(locale + ".json")))
                .getAsJsonObject();
    }

    private static Set<String> authoredWraithTextKeys() throws IOException {
        try (var paths = Files.list(GUIDEBOOK_ROOT.resolve("roles/sparkwitch"))) {
            return paths
                    .filter(path -> path.getFileName().toString().matches(
                            "(wraith|wind_spirit|guardian_angel|vendetta|saboteur|curser)\\.json"))
                    .flatMap(path -> parse(path).entries().stream())
                    .flatMap(entry -> entry.pages().stream())
                    .flatMap(page -> page.blocks().stream())
                    .flatMap(block -> block.runs().stream())
                    .map(dev.caecorthus.sparkassist.guidebook.content.GuidebookRun::translationKey)
                    .collect(Collectors.toUnmodifiableSet());
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
