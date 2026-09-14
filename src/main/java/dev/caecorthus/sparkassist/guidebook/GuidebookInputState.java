package dev.caecorthus.sparkassist.guidebook;

import java.util.HashSet;
import java.util.Set;

/** Retains input ownership through dismissal. 关闭正文后仍接管该次输入的释放事件。 */
public final class GuidebookInputState {
    private final Set<Integer> mouseButtons = new HashSet<>();
    private final Set<Integer> keys = new HashSet<>();

    public boolean pressMouse(int button, boolean captured) {
        if (captured) {
            mouseButtons.add(button);
        }
        return captured;
    }

    public boolean dragMouse(int button, boolean modal) {
        return modal || mouseButtons.contains(button);
    }

    public boolean releaseMouse(int button, boolean modal) {
        return mouseButtons.remove(button) || modal;
    }

    public boolean pressKey(int key, boolean captured) {
        if (captured) {
            keys.add(key);
        }
        return keys.contains(key);
    }

    public boolean releaseKey(int key, boolean modal) {
        return keys.remove(key) || modal;
    }
}
