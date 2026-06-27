# KissCraft Developer Guide

## Quick Start

Use Java 21 and the Gradle wrapper from the repository root.

Launch these in separate terminals or IntelliJ run configurations:

```powershell
.\gradlew.bat runServer
.\gradlew.bat runClientPlayerOne
.\gradlew.bat runClientPlayerTwo
```

`runClientPlayerOne` and `runClientPlayerTwo` use separate usernames and separate game directories. Both clients pass `--quickPlayMultiplayer localhost` so they attempt to connect to the local dedicated server automatically.

## Game Directories

Development instances are isolated:

| Run config | Directory |
|---|---|
| `runClientPlayerOne` | `run/client1/` |
| `runClientPlayerTwo` | `run/client2/` |
| `runServer` | `run/server/` |
| `runGameTestServer` | `run/gametest/` |

Do not point both clients at the same game directory. Separate directories prevent shared config, logs, saves, screenshots, and resource packs from hiding multiplayer bugs.

## Debug Mode

Set this in the common config for the active run directory:

```toml
[debug]
debug_mode = true
```

When enabled, KissCraft increases effective interaction distance, reduces cooldowns, enables verbose structured logs, enables debug commands, and renders client-side debug visuals.

## Debug Commands

Debug commands require `debug_mode=true` and permission level 2.

| Command | Purpose |
|---|---|
| `/kissmod debug state` | Print current attachment state |
| `/kissmod debug cooldown [seconds]` | Set current player's cooldown |
| `/kissmod debug force [player]` | Force-start an interaction |
| `/kissmod debug cancel` | Cancel current interaction |
| `/kissmod debug particles` | Spawn test particles |
| `/kissmod debug animation <ticks>` | Set remaining animation ticks |
| `/kissmod debug target` | Print nearest target diagnostics |
| `/kissmod stress spawn <count>` | Spawn nearby armor stands for scene load testing |
| `/kissmod stress rapid <iterations>` | Repeatedly start/stop interactions |
| `/kissmod stress packet <count>` | Record simulated packet spam volume |
| `/kissmod stress report` | Print stress counters |
| `/kissmod stress reset` | Reset stress counters |

## Debug Rendering

When debug mode is enabled, the client renders:

| Visual | Meaning |
|---|---|
| Green box | Local player eye position |
| Green line | Local player look vector |
| Yellow box | Effective interaction radius |
| Red box | Target eye position |
| Red line | Target look vector |
| White line | Detection line between eyes |
| Blue box | Target entity bounding box |

## Logging

Structured log prefixes are used for filtering:

```text
[KissCraft][Detection]
[KissCraft][Networking]
[KissCraft][Animation]
[KissCraft][Capability]
[KissCraft][Config]
[KissCraft][Debug]
```

Detection, networking, animation, and capability logs are quiet unless debug mode or the matching system property is enabled, for example:

```powershell
.\gradlew.bat runServer -Dkissmod.log.detection=true
```

## Profiling With Spark

Spark is pinned as a dev-only `localRuntime` dependency (`1.10.124-neoforge-1.21.1`). It is available in development runs but is not published as a runtime dependency for users.

Common commands:

```text
/spark profiler start
/spark profiler stop
/spark profiler open
/spark healthreport
/spark tickmonitor
```

Profile these hotspots before adding gameplay features:

| Area | What to inspect |
|---|---|
| Player tick handler | `KissDetectionHandler.onPlayerTick` |
| Detection logic | distance, line of sight, nearest player lookup |
| Networking | kiss start/end payload volume |
| Rendering | `DebugRenderer.onRenderLevel` |

## Automated Validation

Build and run Java tests:

```powershell
.\gradlew.bat build
.\gradlew.bat test
```

Run Minecraft GameTests:

```powershell
.\gradlew.bat runGameTestServer
```

CI runs build, unit tests, and GameTests on every push and pull request.

## Troubleshooting

If a client does not auto-connect, start the server first and use Multiplayer -> Direct Connection -> `localhost`.

If config changes do not appear, edit the config under the active game directory, not a different run directory.

If Gradle run configs look stale in IntelliJ, reload the Gradle project.

If Spark commands are missing, verify the pinned Spark dependency resolved successfully and inspect the run log for mod loading errors.

If debug rendering does not appear, verify `debug_mode=true` in that client's common config and reload/restart the client.
