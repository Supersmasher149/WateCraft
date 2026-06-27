# AGENTS.md — kissmod (NeoForge 1.21.1)

## Build & run

- **Gradle wrapper** — use `gradlew.bat` (Windows) or `gradlew` (Unix). Wrapper version: Gradle 9.2.1.
- **Target**: Java 21.
- **Key commands** (run from repo root):
  - `gradlew.bat runClient` — launch Minecraft client with the mod
  - `gradlew.bat runServer` — headless dedicated server
  - `gradlew.bat runGameTestServer` — auto-run all registered gametests then exit
  - `gradlew.bat runData` — run data generators (output: `src/generated/resources/`)
  - `gradlew.bat --refresh-dependencies` — refresh local Gradle cache
  - `gradlew.bat clean` — wipe `build/`
- **Gradle flags enabled by default** in `gradle.properties`: daemon, parallel execution, build caching, **configuration caching**. If you add tasks that use `project.files()` or similar dynamic inputs, verify they work with config caching.

## Project structure

| Path | Purpose |
|---|---|
| `src/main/java/com/wally/kissmod/` | Main mod source |
| `src/main/resources/` | Static assets (lang, models, textures, mixin config) |
| `src/main/templates/META-INF/neoforge.mods.toml` | Mod metadata template — expanded via Gradle `ProcessResources` |
| `src/generated/resources/` | **Data generator output** — committed, never hand-edited |
| `src/main/resources/kissmod.mixins.json` | Mixin config — package `com.wally.kissmod.mixin`, currently empty |

## Important classes

- `kissmod.java` — main `@Mod` entrypoint. Registers deferred registers and event listeners in constructor.
- `kissmodClient.java` — client-only (`@Mod(..., dist = Dist.CLIENT)`). Registers NeoForge config screen.
- `Config.java` — NeoForge `ModConfigSpec` (type `COMMON`).

## Conventions

- **Mod ID**: `kissmod` — must match `@Mod` annotation, `gradle.properties:mod_id`, and the `neoforge.mods.toml` template.
- **Base package**: `com.wally.kissmod`.
- **Content registration**: use `DeferredRegister` (`BLOCKS`, `ITEMS`, `CREATIVE_MODE_TABS`) in the main mod class.
- **No tests configured in this template** — gametests can be added and run via `runGameTestServer`.
- **BlockBench files** (`*.bbmodel`) and datagen cache (`src/generated/**/.cache`) are excluded from the final JAR.
- **IDE integration**: IntelliJ recommended. `neoForge.ideSyncTask generateModMetadata` ensures the mod metadata template is expanded on project reload/sync.
- **Run output**: Minecraft instance files go to `run/` (gitignored).

## Publishing

- Built artifacts land in `build/libs/` named `<mod_id>-<version>.jar`.
- Local Maven publishing (`gradlew.bat publish`) outputs to `repo/` (gitignored).
