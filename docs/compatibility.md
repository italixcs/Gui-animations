# Compatibility

## First-release target

| Layer | Exact target |
| --- | --- |
| Minecraft Java Edition | 26.1.2 |
| Java runtime / compiler | JDK 25 |
| Fabric Loader | 0.19.2 |
| Fabric API | 0.149.0+26.1.2 |
| ModernUI Fabric development artifact | 26.1.2-3.13.0.5 |
| ModernUI core | 3.13.0 |
| Forge Config API Port | 26.1.3, Fabric artifact |
| Gradle distribution | 9.4.0, SHA-256 pinned |
| Fabric Loom | 1.16.3 |
| Renderer backend | OpenGL; Vulkan is not verified by this adapter |

The authoritative dependency declarations are
[gradle.properties](../gradle.properties), [root build](../build.gradle), and
[showcase build](../examples/showcase-mod/build.gradle). Minecraft 26.1.2 uses
official unobfuscated names; this project does not mix in older Yarn mappings.
The core animation recipe compiles to Java 21 bytecode; the complete Minecraft
showcase still needs JDK 25.

Development uses ModernUI's Maven API artifact for compilation and its pinned
universal mod distribution at runtime. These artifacts serve different purposes.
Read the actual showcase dependency block when reproducing the environment.

The showcase bundles this project's adapter and motion core. A normal Minecraft
installation also needs Fabric Loader, Fabric API, ModernUI, and Forge Config
API Port for the target version. The Gradle development run resolves its declared
runtime dependencies automatically. Referenced libraries retain their licenses.

## Host render-state bridge

The showcase's two version-pinned mixins keep Minecraft's command-encoder cache
coherent after ModernUI performs external OpenGL drawing. Runtime diagnostics
showed the encoder otherwise skipped its program bind and uploaded sampler
uniforms to the wrong active program. The bridge marks external work after
ModernUI's GL cleanup, before compositing, and invalidates cached program and
pipeline references once per generation. Minecraft then rebinds normally.

It does not suppress GL errors, query driver state on every draw, or destroy
the shader cache. The exact targets are Minecraft 26.1.2 and ModernUI 3.13.0.5;
the mixin configuration must travel with the host integration when adapting the
starter. Revalidate these injection points when porting to another version.

## Scope of support

The shipped renderer adapter and source templates target this exact stack.
Fabric development runs and build checks establish only the coverage recorded
in the [verification report](verification-report.md). Other loaders, older
Minecraft releases, and physical GPU/OS combinations require their own checks.

The agent skill can inspect an existing project on another stack and apply its
design principles. It must first verify or implement a matching adapter before
claiming the included Java templates work there. It should preserve the
consuming project's established version and renderer unless the user requests
a migration.

The installer supports project-local Codex and Claude Code skill locations.
Other agents can use the self-contained `SKILL.md` and references when explicitly
loaded; automatic discovery outside the documented locations is not claimed.

## Resource and coordinate contract

The design [tokens](../design/tokens.json) map lengths to ModernUI **dp** and text
sizes to **sp**. ModernUI handles density and framebuffer pixels. Do not multiply
these values by Minecraft GUI scale again. The canonical Inter font is stored
with its [OFL license](../design/fonts/OFL.txt) and copied into runtime resources
by the build. Its file identity is checked by the design validator.

Use the selected renderer's matrix, clipping, hit-testing, focus, and lifecycle
facilities together. A screen screenshot alone does not verify input alignment
or cancellation during close/replacement.
