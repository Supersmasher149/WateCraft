# AGENTS.md - kissmod / KissCraft

## Environment

- This is a NeoForge `1.21.1` mod, not 1.20.1. Use Java 21 and the Gradle wrapper.
- Run commands from the repo root. On Windows use `gradlew.bat`; CI uses `./gradlew`.
- Gradle configuration cache is enabled in `gradle.properties`; if adding/changing Gradle tasks, verify they still work with config cache.

## Verification Commands

- `gradlew.bat build` - compile/package; currently no separate Java unit tests are present, so `test` is `NO-SOURCE`.
- `gradlew.bat runGameTestServer` - launches the GameTest server, runs KissCraft GameTests, then exits.
- CI runs `build`, `test`, then `runGameTestServer` on push/PR.
- `gradlew.bat runData` - data generation output goes to `src/generated/resources/`; treat generated resources as generated, not hand-authored.

## Development Runs

- `gradlew.bat runServer` - dedicated server, game dir `run/server`, `--nogui`.
- `gradlew.bat runClientPlayerOne` - username `PlayerOne`, game dir `run/client1`, attempts `localhost` quick-play.
- `gradlew.bat runClientPlayerTwo` - username `PlayerTwo`, game dir `run/client2`, attempts `localhost` quick-play.
- Keep client game dirs separate. Do not repoint both clients at the same directory; isolated configs/logs/saves are intentional for multiplayer debugging.
- Spark is a dev-only `localRuntime` dependency pinned to `maven.modrinth:spark:1.10.124-neoforge-1.21.1`.

## Source Layout

- Main source: `src/main/java/com/wally/kissmod/`.
- Main entrypoint: `kissmod.java`; client-only entrypoint: `kissmodClient.java`.
- Static resources: `src/main/resources/`; mod metadata template: `src/main/templates/META-INF/neoforge.mods.toml`.
- GameTest structures for this MC version use singular `data/<namespace>/structure/*.nbt`; the local empty template is `src/main/resources/data/kissmod/structure/empty.nbt`.
- `src/generated/resources/` is included in main resources; BlockBench files and generated `.cache` files are excluded from final jars.

## Kiss Feature Wiring

- Kissing is automatic server-side detection in `KissDetectionHandler.onPlayerTick`, every 5 ticks; there is no keybind/right-click trigger.
- Start checks include eye distance, mutual look when enabled, line of sight, cooldown, active-kiss state, blocked player states, and both players' opt-out flags.
- `KissPlayerData` is a NeoForge attachment from `ModAttachments.kissData()` with serialized fields: `kissing`, optional `targetUUID`, `cooldownTicks`, `remainingKissTicks`, `optedOut`.
- Network payloads are `KissStartPayload` and `KissEndPayload`; client handling updates attachment state for rendering/head tilt.
- Client animation is in `mixin/PlayerRendererMixin.java`; debug rendering is in `DebugRenderer.java` and registered from `kissmodClient.java`.
- `kiss_sound` is registered, but `assets/kissmod/sounds/kiss_sound.ogg` is missing, so sound playback is silent unless the asset is added.

## Debug/Profiling Tools

- Enable debug mode in the active run dir's common config: `[debug] debug_mode = true`.
- Debug mode increases effective distance, reduces cooldown, enables structured debug logs, enables `/kissmod debug ...`, `/kissmod stress ...`, and client debug rendering.
- Structured log prefixes come from `KissLog`: `[KissCraft][Detection]`, `[Networking]`, `[Animation]`, `[Capability]`, `[Config]`, `[Debug]`.
- Spark commands are available in dev runs, e.g. `/spark profiler start`, `/spark profiler stop`, `/spark healthreport`.

## Dist Safety

- Do not put `@OnlyIn(Dist.CLIENT)` on classes referenced by common registration/network code; `RuntimeDistCleaner` can strip them and cause server bootstrap failures.
- If common code needs client APIs, guard at runtime before touching them, e.g. `if (!context.player().level().isClientSide()) return;` before `Minecraft.getInstance()`.

## Repo Conventions

- Mod ID is `kissmod`; keep it aligned across `gradle.properties`, `@Mod`, resources, payload IDs, and templates.
- Use `DeferredRegister` for registered content.
- Built artifacts are under `build/libs/`; `gradlew.bat publish` publishes to local `repo/`.
- See `DEVELOPER_GUIDE.md` for full multiplayer/debug/profiling workflow and `TESTING_CHECKLIST.md` for manual QA coverage.
