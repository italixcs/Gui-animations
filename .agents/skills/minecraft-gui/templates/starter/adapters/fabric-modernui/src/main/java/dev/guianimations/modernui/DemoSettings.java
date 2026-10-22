package dev.guianimations.modernui;

/** Settings owned by the showcase, never aliases for unrelated Minecraft options. */
public record DemoSettings(boolean notifications, boolean reducedMotion, int animationStrength) {
    public DemoSettings {
        animationStrength = Math.clamp(animationStrength, 0, 100);
    }

    public static DemoSettings defaults() { return new DemoSettings(true, false, 75); }
}
