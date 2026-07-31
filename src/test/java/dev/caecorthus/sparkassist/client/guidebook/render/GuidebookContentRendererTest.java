package dev.caecorthus.sparkassist.client.guidebook.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.caecorthus.sparkassist.guidebook.content.GuidebookRun;
import dev.caecorthus.sparkassist.guidebook.content.GuidebookTone;
import org.junit.jupiter.api.Test;

class GuidebookContentRendererTest {
    @Test
    void resolvesStructuredKeysWithTheProvidedResolver() {
        GuidebookRun translated = GuidebookRun.translated(
                "guidebook.test.key",
                false,
                false,
                GuidebookTone.DEFAULT
        );

        assertEquals("中文正文", GuidebookContentRenderer.resolveText(
                translated,
                key -> key.equals("guidebook.test.key") ? "中文正文" : key
        ));
    }

    @Test
    void leavesLiteralRunsUnchanged() {
        GuidebookRun literal = new GuidebookRun("字面正文", false, false, GuidebookTone.DEFAULT);

        assertEquals("字面正文", GuidebookContentRenderer.resolveText(literal, key -> "不应使用"));
    }
}
