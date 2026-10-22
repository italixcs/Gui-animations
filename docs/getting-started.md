# Get started

Gui-animations 0.1.0 targets Minecraft **26.1.2**, **Fabric**, and **JDK 25**.
The installer needs Python 3.10 or later and uses only its standard library.
See [compatibility](compatibility.md) for the exact stack and
[verification evidence](verification-report.md) for what was actually tested.

## Try the showcase

```sh
git clone https://github.com/italixcs/Gui-animations.git
cd Gui-animations
./gradlew build
./gradlew :examples:showcase-mod:runClient
```

Set `JAVA_HOME` to a JDK 25 installation if your default Java is another version.
On Windows use `gradlew.bat` in place of `./gradlew`.
The first build downloads the pinned Minecraft and library artifacts.

Press **F8** at the title screen or in game to open the showcase. No server or
world is needed. Escape or the screen's Close control returns to the previous
screen; when a dialog is open, dismiss it first.

The showcase demonstrates the shared font and theme, responsive navigation,
settings controls, popup, tooltip, toast, and transitions. Its settings belong
to this demo; they do not change unrelated Minecraft settings.

For automated inspection, open the showcase after the title screen loads:

```sh
./gradlew :examples:showcase-mod:runClient -Dguianimations.demo.autoOpen=true
```

## Add the skill to an existing client or mod

From the toolkit checkout, run:

```sh
python3 scripts/install.py --project /path/to/your-client --agent codex --dry-run
python3 scripts/install.py --project /path/to/your-client --agent codex
```

For Claude Code use `--agent claude`. Installation adds the main skill, its
supporting references and templates, a short managed section in `AGENTS.md`
or `CLAUDE.md`, and `gui-toolkit.lock.json`. Existing project rules are preserved.
The default installation does not rewrite your mod's build or source files.

Then open **your client/mod project** in your coding agent and ask:

> Use minecraft-gui to build my settings screen. Follow this project's actual
> Minecraft version and loader, reuse the shared UI theme and components, and
> connect the controls to my settings system. Include smooth transitions,
> keyboard navigation, reduced motion, and verification in Minecraft.

The main skill reads the relevant design, implementation, motion, and review
guides. It imports or adapts the matching Java sources and build dependencies
into your project. It identifies unsupported targets before promising a working
adapter; installing instructions does not establish runtime compatibility.

See [the usage guide](USAGE.md) for how shared code and project rules keep future
screens consistent across sessions.

## Create a runnable starter project

The `--starter` option also imports the complete validated three-module showcase
source and its build files. It can create a new project directory:

```sh
python3 scripts/install.py --project ../my-client --agent codex --starter
cd ../my-client
./gradlew build
./gradlew :examples:showcase-mod:runClient
```

This starts from the showcase's `dev.guianimations` Java packages and
`guianimations` resource namespace. Ask the agent to rename both consistently
when adapting it to your client. Keep the bundled font's license notices.

The starter provides the UI foundation; your client's gameplay features and
domain-specific settings are implemented in your own project.

## Update safely

Get the desired toolkit revision, then rerun the same installer command.
The package and lock record contain content hashes and executable modes.
Repeated installation is idempotent. An update that conflicts with local edits
fails before applying changes, with paths to inspect. Preserve or reconcile
those edits before retrying; the installer has no blanket overwrite flag.

Use `--starter` on updates only when you also want the imported starter source
included in the update plan. Without it, installation updates the skill package
and retains the existing source-import record.

## Rebuild the packaged skill

Contributors edit canonical design assets and Java sources, then regenerate the
self-contained package:

```sh
python3 scripts/package_skill.py
python3 scripts/package_skill.py --check
```

The package is committed under `.agents/skills/minecraft-gui`. It contains the
guidance, tokens, licensed font, installer, and runnable source snapshot needed
outside this checkout. Its manifest identifies every payload file by SHA-256.

## Run checks

```sh
python3 design/validate.py
python3 -m unittest discover -s tests/design -v
python3 -m unittest discover -s tests/python -v
./gradlew build
python3 scripts/package_skill.py --check
python3 tests/verification/verify_consumer.py --build-starter
python3 scripts/validate_repo.py
```

These commands validate tokens and contrast, exercise installer failure paths,
test motion and settings logic, compile the mod, and build a separate consumer
from the installed templates. Runtime screenshots and input checks complement
these automated checks; see the [verification report](verification-report.md).
