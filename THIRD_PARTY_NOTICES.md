# Third-party notices

Original Gui-animations source and guidance are covered by the repository's
[MIT license](LICENSE). The following files and runtime dependencies retain
their own licenses.

| Material | Source / version | License and distribution |
| --- | --- | --- |
| Inter Regular font | [Inter 4.1](https://github.com/rsms/inter/releases/tag/v4.1), original unmodified static TTF | SIL OFL 1.1; [license](design/fonts/OFL.txt), [provenance and checksums](design/fonts/README.md). Included in runtime resources and the skill starter. |
| Gradle wrapper JAR and scripts | [Gradle 9.4.0](https://github.com/gradle/gradle/tree/v9.4.0) | Apache 2.0; [distribution license](licenses/Gradle-LICENSE.txt) and [notice](licenses/Gradle-NOTICE.txt). The distribution and wrapper JAR hashes are pinned in `gradle/wrapper`. |
| ModernUI / ModernUI-MC | [ModernUI](https://github.com/BloCamLimb/ModernUI), [Minecraft integration](https://github.com/BloCamLimb/ModernUI-MC) | LGPL-3.0-or-later. External build/runtime dependencies; their implementation is not vendored into this source repository or claimed as MIT. |
| Fabric Loader and Fabric API | [Fabric](https://fabricmc.net/) | Apache 2.0. External Minecraft development/runtime dependencies. |
| Forge Config API Port | [ForgeConfigAPIPort](https://github.com/Fuzss/forgeconfigapiport) | External runtime dependency; retain the artifact's own license notices when redistributing it. |
| JUnit | [JUnit](https://junit.org/) | EPL 2.0. Test-only dependency. |

Minecraft itself is not included in the skill package or source repository.
The build resolves Mojang's development artifacts through Fabric Loom. Distribute
your mod and its required libraries according to their respective terms.

The [research references](https://github.com/italixcs/Gui-animations/blob/main/docs/RESEARCH.md)
are inspiration and evidence. Their
screenshots and restricted implementations are not bundled as project assets.
Screenshots or videos captured from this project's own showcase are identified
as runtime verification evidence.
