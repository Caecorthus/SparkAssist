package dev.caecorthus.sparkassist.client.settings;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.text.Text;

/**
 * One tab of the settings screen. 设置界面的一个页签。
 *
 * @param id     stable id used to open the screen on this page / 用于直接打开此页的稳定 id
 * @param title  tab label / 页签文字
 * @param groups non-empty groups in display order / 按显示顺序排列的非空分组
 */
public record SettingsPage(String id, Text title, List<Group> groups) {
    public SettingsPage {
        groups = groups.stream().filter(group -> !group.options().isEmpty()).toList();
    }

    public Stream<SettingsOption> options() {
        return groups.stream().flatMap(group -> group.options().stream());
    }

    public boolean isEmpty() {
        return groups.isEmpty();
    }

    /** A titled block of rows inside a page. 页内带标题的一组行。 */
    public record Group(Text title, List<SettingsOption> options) {
        public Group {
            options = List.copyOf(options);
        }
    }
}
