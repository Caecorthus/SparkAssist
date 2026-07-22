package dev.caecorthus.sparkassist.guidebook.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.caecorthus.sparkassist.guidebook.GuidebookEntry;
import dev.caecorthus.sparkassist.guidebook.GuidebookTab;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookBlock;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookBlockType;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookPage;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookRun;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookTone;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses authored GuideBook JSON without coupling the catalog to its storage format.
 * 解析指南书 JSON，并让目录模型与具体存储格式保持解耦。
 */
public final class GuidebookJsonParser {
    private GuidebookJsonParser() {
    }

    public static List<GuidebookEntry> parseEntries(String json) {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject() || !root.getAsJsonObject().has("entries")) {
                throw new IllegalArgumentException("Guidebook document must contain an entries array");
            }

            JsonArray jsonEntries = root.getAsJsonObject().getAsJsonArray("entries");
            List<GuidebookEntry> entries = new ArrayList<>(jsonEntries.size());
            for (JsonElement element : jsonEntries) {
                entries.add(parseEntry(element.getAsJsonObject()));
            }
            return List.copyOf(entries);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Malformed GuideBook document", exception);
        }
    }

    private static GuidebookEntry parseEntry(JsonObject json) {
        String summaryKey = json.get("summaryKey").getAsString();
        List<String> pageKeys = json.has("page_keys")
                ? parseStringList(json.getAsJsonArray("page_keys"))
                : List.of(summaryKey);
        List<GuidebookPage> pages = json.has("pages")
                ? parsePages(json.getAsJsonArray("pages"))
                : List.of();
        return new GuidebookEntry(
                json.get("id").getAsString(),
                GuidebookTab.valueOf(json.get("tab").getAsString().toUpperCase(Locale.ROOT)),
                json.get("sourceModId").getAsString(),
                json.get("nameKey").getAsString(),
                summaryKey,
                pageKeys,
                pages,
                parseStringList(json.getAsJsonArray("ownerRoleIds")),
                parseStringList(json.getAsJsonArray("requiredModIds")),
                parseColor(json),
                json.get("order").getAsInt()
        );
    }

    private static int parseColor(JsonObject json) {
        if (!json.has("color")) {
            return GuidebookEntry.DEFAULT_COLOR;
        }
        String value = json.get("color").getAsString();
        if (!value.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("GuideBook color must use #RRGGBB format");
        }
        return Integer.parseInt(value.substring(1), 16);
    }

    private static List<GuidebookPage> parsePages(JsonArray values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("GuideBook pages array cannot be empty");
        }
        List<GuidebookPage> pages = new ArrayList<>(values.size());
        for (JsonElement value : values) {
            JsonArray jsonBlocks = value.getAsJsonObject().getAsJsonArray("blocks");
            if (jsonBlocks == null) {
                throw new IllegalArgumentException("GuideBook page must contain a blocks array");
            }
            List<GuidebookBlock> blocks = new ArrayList<>(jsonBlocks.size());
            for (JsonElement blockValue : jsonBlocks) {
                blocks.add(parseBlock(blockValue.getAsJsonObject()));
            }
            pages.add(new GuidebookPage(blocks));
        }
        return List.copyOf(pages);
    }

    private static GuidebookBlock parseBlock(JsonObject json) {
        GuidebookBlockType type = GuidebookBlockType.valueOf(
                json.get("type").getAsString().toUpperCase(Locale.ROOT)
        );
        if (type == GuidebookBlockType.SPACER) {
            return new GuidebookBlock(type, List.of());
        }

        if (json.has("text") || json.has("textKey")) {
            return new GuidebookBlock(type, List.of(parseRun(json)));
        }
        JsonArray jsonRuns = json.getAsJsonArray("runs");
        if (jsonRuns == null) {
            throw new IllegalArgumentException("GuideBook block must contain text, textKey, or runs");
        }
        List<GuidebookRun> runs = new ArrayList<>(jsonRuns.size());
        for (JsonElement run : jsonRuns) {
            runs.add(parseRun(run.getAsJsonObject()));
        }
        return new GuidebookBlock(type, runs);
    }

    private static GuidebookRun parseRun(JsonObject json) {
        GuidebookTone tone = json.has("tone")
                ? GuidebookTone.valueOf(json.get("tone").getAsString().toUpperCase(Locale.ROOT))
                : GuidebookTone.DEFAULT;
        boolean bold = json.has("bold") && json.get("bold").getAsBoolean();
        boolean italic = json.has("italic") && json.get("italic").getAsBoolean();
        boolean hasText = json.has("text");
        boolean hasTextKey = json.has("textKey");
        if (hasText == hasTextKey) {
            throw new IllegalArgumentException("GuideBook runs must contain exactly one of text or textKey");
        }
        return hasText
                ? new GuidebookRun(json.get("text").getAsString(), bold, italic, tone)
                : GuidebookRun.translated(json.get("textKey").getAsString(), bold, italic, tone);
    }

    private static List<String> parseStringList(JsonArray values) {
        List<String> result = new ArrayList<>(values.size());
        for (JsonElement value : values) {
            result.add(value.getAsString());
        }
        return List.copyOf(result);
    }
}
