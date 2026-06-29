package dev.caecorthus.sparkassist.input;

import dev.caecorthus.sparkassist.config.SparkAssistConfig.InstinctKeyMode;

/**
 * Rising-edge toggle state for Wathe's hold-based instinct key.
 * 只在本能键从松开变为按下时翻转，避免一次按住被多次切换。
 */
public final class InstinctToggleState {
    private boolean toggled;
    private boolean previousPhysicalDown;

    public boolean effectivePressed(InstinctKeyMode mode, boolean physicalDown) {
        if (mode != InstinctKeyMode.TOGGLE) {
            this.previousPhysicalDown = physicalDown;
            this.toggled = false;
            return physicalDown;
        }

        if (physicalDown && !this.previousPhysicalDown) {
            this.toggled = !this.toggled;
        }
        this.previousPhysicalDown = physicalDown;
        return this.toggled;
    }

    public void reset() {
        this.toggled = false;
        this.previousPhysicalDown = false;
    }

    public boolean toggled() {
        return toggled;
    }
}
