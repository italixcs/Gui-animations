package dev.guianimations.showcase.render;

/** Render-thread generation for Minecraft state invalidated by ModernUI's OpenGL drawing. */
public final class ExternalRenderState {
    private static long generation;

    private ExternalRenderState() {}

    public static void changed() { generation++; }
    public static long generation() { return generation; }
}
