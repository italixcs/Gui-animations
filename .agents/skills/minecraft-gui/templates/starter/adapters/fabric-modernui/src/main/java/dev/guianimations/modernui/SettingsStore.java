package dev.guianimations.modernui;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.Properties;

/** Small local store with validated values and atomic replacement. Call disk operations off UI thread. */
public final class SettingsStore {
    private final Path path;
    public SettingsStore(Path path) { this.path = path; }

    public DemoSettings load() throws IOException {
        if (!Files.exists(path)) return DemoSettings.defaults();
        Properties values = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { values.load(reader); }
        catch (IllegalArgumentException malformed) { throw new IOException("Malformed showcase settings: " + path, malformed); }
        DemoSettings defaults = DemoSettings.defaults();
        int strength;
        try { strength = Integer.parseInt(values.getProperty("animationStrength", "75")); }
        catch (NumberFormatException ignored) { strength = defaults.animationStrength(); }
        return new DemoSettings(bool(values, "notifications", defaults.notifications()),
                bool(values, "reducedMotion", defaults.reducedMotion()), strength);
    }

    private static boolean bool(Properties values, String key, boolean fallback) {
        String text = values.getProperty(key);
        return "true".equalsIgnoreCase(text) ? true : "false".equalsIgnoreCase(text) ? false : fallback;
    }

    public synchronized void save(DemoSettings settings) throws IOException {
        Path absolute = path.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        Path temporary = Files.createTempFile(absolute.getParent(), ".guianimations-", ".tmp");
        try {
            Properties values = new Properties();
            values.setProperty("notifications", Boolean.toString(settings.notifications()));
            values.setProperty("reducedMotion", Boolean.toString(settings.reducedMotion()));
            values.setProperty("animationStrength", Integer.toString(settings.animationStrength()));
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                values.store(writer, "Gui-animations showcase settings");
            }
            try { Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
