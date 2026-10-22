# Gui-animations starter

This is a self-contained source snapshot of the toolkit's Minecraft showcase. It keeps the validated demo namespace; rename packages and resource identifiers deliberately when adapting it.

Install JDK 25, then run:

```sh
./gradlew build
./gradlew :examples:showcase-mod:runClient
```

Press **F8** from the title screen or a world to open the showcase. Escape closes the top dialog first, then the screen. For automated launch, use `./gradlew :examples:showcase-mod:runClient -Dguianimations.demo.autoOpen=true`.

Check `gradle.properties` and the module build files for exact dependency pins. The development client uses the mod's demo account; this is a client-mod starter, not an official Minecraft client distribution.

Use the installed minecraft-gui skill for design and implementation guidance. Keep the bundled font's OFL notice and the renderer's own licenses with redistributed dependencies.
