package dev.guianimations.modernui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SettingsStoreTest {
    @TempDir Path directory;

    @Test void missingFileReturnsDefaultsAndSavedValuesSurviveAnotherInstance() throws IOException {
        Path path = directory.resolve("nested/settings.properties");
        SettingsStore store = new SettingsStore(path);
        assertEquals(DemoSettings.defaults(), store.load());
        DemoSettings chosen = new DemoSettings(false, true, 42);
        store.save(chosen);
        assertEquals(chosen, new SettingsStore(path).load());
    }

    @Test void invalidFieldsUseDefaultsAndOutOfRangeNumbersAreClamped() throws IOException {
        Path path = directory.resolve("settings.properties");
        Files.writeString(path, "notifications=bad\nreducedMotion=true\nanimationStrength=999\n");
        assertEquals(new DemoSettings(true, true, 100), new SettingsStore(path).load());
        Files.writeString(path, "animationStrength=not-a-number\n");
        assertEquals(DemoSettings.defaults(), new SettingsStore(path).load());
    }

    @Test void malformedPropertiesBecomeARecoverableIoFailure() throws IOException {
        Path path = directory.resolve("settings.properties");
        Files.writeString(path, "broken=" + "\\" + "uNOTHEX\n");
        assertThrows(IOException.class, () -> new SettingsStore(path).load());
    }

    @Test void replacingAnExistingSaveLeavesNoTemporaryFiles() throws IOException {
        Path path = directory.resolve("settings.properties");
        SettingsStore store = new SettingsStore(path);
        store.save(DemoSettings.defaults());
        store.save(new DemoSettings(false, false, 0));
        assertEquals(new DemoSettings(false, false, 0), store.load());
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }
}
