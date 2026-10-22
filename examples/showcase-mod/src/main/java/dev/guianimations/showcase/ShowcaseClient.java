package dev.guianimations.showcase;

import com.mojang.blaze3d.platform.InputConstants;
import dev.guianimations.modernui.DemoSettings;
import dev.guianimations.modernui.SettingsStore;
import dev.guianimations.modernui.ShowcaseFragment;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.mc.MuiScreen;
import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.mc.UIManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Minecraft-only lifecycle bridge. Press F8 at the title screen or in a world. */
public final class ShowcaseClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("guianimations");
    private final SettingsStore settings = new SettingsStore(FabricLoader.getInstance().getConfigDir()
            .resolve("guianimations-showcase.properties"));
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Gui-animations settings");
        thread.setDaemon(true);
        return thread;
    });
    private boolean previousF8, autoOpened, opening;
    private int readyTicks;

    @Override public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> io.shutdown());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean down = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_F8);
            if (down && !previousF8 && client.getOverlay() == null) open(client);
            previousF8 = down;
            if (!autoOpened && Boolean.getBoolean("guianimations.demo.autoOpen")
                    && client.screen instanceof TitleScreen && client.getOverlay() == null) {
                if (++readyTicks >= 5) { autoOpened = true; open(client); }
            }
        });
        LOGGER.info("Gui-animations ready. Press F8 to open the showcase.");
    }

    private void open(Minecraft client) {
        if (opening || client.screen instanceof MuiScreen) return;
        opening = true;
        Screen previous = client.screen;
        CompletableFuture.supplyAsync(() -> {
            try { return settings.load(); }
            catch (IOException failure) {
                LOGGER.warn("Cannot load showcase settings; opening with defaults", failure);
                return DemoSettings.defaults();
            }
        }, io).thenAccept(initial -> client.execute(() -> {
            opening = false;
            if (client.screen != previous) return;
            Screen[] ownScreen = new Screen[1];
            ShowcaseFragment fragment = new ShowcaseFragment(initial, snapshot -> CompletableFuture.runAsync(() -> {
                try { settings.save(snapshot); }
                catch (IOException failure) {
                    LOGGER.error("Cannot save showcase settings", failure);
                    throw new CompletionException(failure);
                }
            }, io), () -> client.execute(() -> {
                if (client.screen == ownScreen[0]) client.setScreen(previous);
            }), UIManager.getInstance().getOnBackPressedDispatcher());
            ownScreen[0] = MuiModApi.get().createScreen(fragment, new ScreenCallback() {
                @Override public boolean shouldBlurBackground() { return false; }
                @Override public boolean hasDefaultBackground() { return false; }
                @Override public boolean isPauseScreen() { return true; }
                @Override public boolean isBackKey(int keyCode, icyllis.modernui.view.KeyEvent event) {
                    return keyCode == icyllis.modernui.view.KeyEvent.KEY_ESCAPE;
                }
            }, previous, "Gui-animations showcase");
            client.setScreen(ownScreen[0]);
        }));
    }
}
