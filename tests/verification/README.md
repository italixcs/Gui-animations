# Independent consumer verification

Run from the repository root after generating the self-contained skill package:

```sh
python3 tests/verification/verify_consumer.py
python3 tests/verification/verify_consumer.py --build-starter
```

The second command requires the JDK documented by the packaged starter and
network access for its pinned build dependencies. Use `JAVA_HOME` to select that
JDK. A successful build establishes source compatibility, not in-game appearance.

To retain consumer projects and save a machine-readable result:

```sh
python3 tests/verification/verify_consumer.py \
  --build-starter \
  --workdir /tmp/gui-toolkit-consumer-check \
  --output /tmp/gui-toolkit-consumer-check-report.json
```

`--workdir` must name a directory that does not exist yet. The harness deliberately
uses separate projects, invokes the installer as a command, and imports no
installer internals. It checks:

- Fresh Codex and Claude installs preserve existing project rules and source.
- Dry-runs leave files unchanged; repeated installs are content-idempotent.
- Every installed payload file matches its manifest hash and mode.
- Installed Markdown links resolve inside the installed package.
- Local edits cause a refusal without partial project changes.
- Both agents can coexist with separate install records.
- A corrupted source package and destination symlinks are refused before writes.
- With `--build-starter`, source from the installed package builds outside the
  toolkit checkout.

The runtime acceptance checks are documented with actual outcomes in
[the verification report](../../docs/verification-report.md). A build or installer
test does not substitute for exercising the screen inside Minecraft.
