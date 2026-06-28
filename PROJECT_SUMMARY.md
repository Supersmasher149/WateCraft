# kissmod / KissCraft v1.0.0

A NeoForge mod for Minecraft 1.21.1 that adds automatic player kissing detection with visual/audio effects.

## Architecture

### Detection (Server-side)

- `KissDetectionHandler` runs every 5th tick per player via `PlayerTickEvent.Post`
- Uses eye-position distance, mutual look-direction dot product (threshold 0.8), and block raycasting for line-of-sight
- Both players must be within `max_head_distance` (default 1.5 blocks, configurable), not blocked by state (flying, sleeping, riding), not on cooldown, and not opted out
- Debug mode triples effective distance and caps cooldown to 20 ticks

### State Management

- `KissPlayerData` is a NeoForge attachment (codec-serialized, `copyOnDeath`) on each player entity
- Tracks: `kissing`, `targetUUID`, `cooldownTicks`, `remainingKissTicks`, `optedOut`
- Registered via `ModAttachments.kissData()`

### Network

- `KissStartPayload` (client-bound): carries `playerUUID`, `targetUUID`, `durationTicks` using binary UUID stream codecs
- `KissEndPayload` (client-bound): carries `playerUUID` using binary UUID stream codec
- Handled by `ClientPayloadHandler` which updates attachment state for the affected player entity

### Animation (Client-side)

- `PlayerRendererMixin` injects at `setupAnim` TAIL on `PlayerModel`
- Applies head/body rotation tilt scaled by `remainingKissTicks / ANIMATION_DURATION_TICKS` (linear fade)
- `remainingKissTicks` ticks down on client via `ClientTickEvent.Post` so the tilt naturally fades out
- Toggleable via `disable_head_tilt` config

### Particles & Effects

- `startKiss`: 6 heart burst at eye midpoint + 20 hearts in a 1.5-radius ring (center burst + ring)
- Controlled by `enable_hearts` config
- `endKiss`: `Regeneration I` (100 ticks) and `Glowing` (200 ticks) applied to both players on kiss end
- `kiss_sound` SoundEvent registered (requires `sounds/kiss_sound.ogg` asset)

### Debug Tools

- `/kissmod toggle` - opt out of kissing
- `/kissmod debug state|cooldown|force|cancel|particles|animation|target` (requires debug_mode + op 2)
- `/kissmod stress spawn|rapid|packet|report|reset` (requires debug_mode + op 2)
- `DebugRenderer` - visual wireframe overlays for eye positions, look vectors, radius, and bounding boxes

### Config (kissmod-common.toml)

| Category | Key | Default | Description |
|---|---|---|---|
| detection | `max_head_distance` | 1.5 | Max eye-to-eye distance |
| detection | `require_look_at_each_other` | true | Mutual look check |
| detection | `cooldown_seconds` | 5 | Cooldown between kisses |
| animation | `animation_duration_ticks` | 60 | Kiss duration (3s) |
| animation | `disable_head_tilt` | false | Disable head tilt |
| effects | `enable_hearts` | true | Heart particles |
| effects | `apply_regeneration_effect` | true | Regen on end |
| effects | `enable_glowing` | true | Glowing on end |
| debug | `debug_mode` | false | Dev tools + verbose logs |

## File Structure

```
src/main/java/com/wally/kissmod/
  kissmod.java              - Main mod class (common)
  kissmodClient.java        - Client entry point + tick handler
  CommandHandler.java       - /kissmod command tree
  Config.java               - ModConfigSpec builder
  DebugRenderer.java        - Client debug wireframe overlay
  KissDetectionHandler.java - Core detection + kiss lifecycle
  KissLog.java              - Structured SLF4J markers
  KissMath.java             - Distance/dot product helpers
  KissPlayerData.java       - Attachment data + codec
  KissValidationResult.java - Enum of validation failure reasons
  ModAttachments.java       - Attachment type registration
  Sounds.java               - SoundEvent registry
  StressTestState.java      - Stress test counters
  mixin/
    PlayerRendererMixin.java - Head tilt animation mixin
  network/
    ClientPayloadHandler.java - Inbound packet handler (client)
    KissStartPayload.java     - Kiss start network payload
    KissEndPayload.java       - Kiss end network payload
    ModPayloads.java          - Payload registration
  gametest/
    KissDevelopmentGameTests.java - GameTest suite
```

## Build & Run

- Java 21, NeoForge 1.21.1, Gradle wrapper
- `gradlew.bat build` - compile + jar
- `gradlew.bat runGameTestServer` - run 3 GameTests (math, serialization, command)
- `gradlew.bat runServer`/`runClientPlayerOne`/`runClientPlayerTwo` - dev runs
