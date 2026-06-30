# kissmod / KissCraft v1.0.0

A NeoForge mod for Minecraft 1.21.1 that adds player kissing with both automatic detection and a keybind-based consent system, plus visual/audio effects and stats tracking.

## Architecture

### Detection & Request (Server-side)

- **Auto-detection** via `KissDetectionHandler` runs every 5th tick per player (`PlayerTickEvent.Post`): finds nearest player within `max_head_distance`, checks mutual look (dot product > 0.8), line-of-sight (raycast), cooldown, and opt-out
- **Manual request** via V key: `KeybindHandler` raycasts to targeted player, sends `KissRequestPacket` (C2S) → server validates conditions → `KissPromptPacket` (S2C) to target → target sees overlay and presses Y/N → `KissResponsePacket` → server re-validates and calls `startKiss`
- Both paths call the same `startKiss()` function
- `RequestManager` tracks pending requests and per-player 2s request cooldown
- `LivingDamageEvent.Post` ends kiss on damage; `PlayerLoggedOutEvent` ends kiss + cleans up requests on disconnect
- Debug mode triples effective distance and caps cooldown to 20 ticks

### State Management

- `KissPlayerData` is a NeoForge attachment (codec-serialized, `copyOnDeath`) on each player entity
- Tracks: `kissing`, `targetUUID`, `cooldownTicks`, `remainingKissTicks`, `optedOut`, `totalKisses`
- Registered via `ModAttachments.kissData()`

### Network

| Payload | Direction | Fields | Purpose |
|---|---|---|---|
| `KissRequestPacket` | C2S | `targetUUID` | Player initiates a kiss request |
| `KissResponsePacket` | C2S | `requesterUUID`, `accepted` | Target accepts/declines |
| `KissPromptPacket` | S2C | `requesterUUID`, `requesterName` | Shows prompt overlay on target |
| `KissExecutePacket` | S2C | `kisserUUID`, `targetUUID`, `durationTicks` | Starts kiss animation on both clients |
| `KissEndPayload` | S2C | `playerUUID`, `partnerUUID` | Ends kiss state on both clients |

- `ClientPayloadHandler` handles inbound packets, updating attachment state for all affected players
- Registered via `ModPayloads` with a versioned (`"1.0"`) registrar

### Animation (Client-side)

- `PlayerRendererMixin` injects at `setupAnim` TAIL on `PlayerModel`
- Applies head forward tilt (`head.xRot`), side-to-side head tilt (`head.zRot * 0.3F`), and body forward tilt (`body.xRot * 0.4F`) scaled by `remainingKissTicks / ANIMATION_DURATION_TICKS` (linear fade)
- `remainingKissTicks` ticks down on client via `ClientTickEvent.Post` so the tilt naturally fades out
- Toggleable via `disable_head_tilt` config

### Particles & Effects

- `startKiss`: 6 heart burst at eye midpoint + 20 hearts in a 1.5-radius ring (center burst + ring)
- Controlled by `enable_hearts` config
- `endKiss`: `Regeneration I` (100 ticks) and `Glowing` (200 ticks) applied to **both** players individually
- `kiss_sound` SoundEvent registered (requires `sounds/kiss_sound.ogg` asset)

### Keybinds

| Key | Action |
|---|---|
| V | Send kiss request to targeted player (500ms rate limit) |
| K | Open kiss stats screen |

### Stats System

- `totalKisses` field in `KissPlayerData` incremented on each `startKiss`
- `/kissmod stats` shows total kisses in chat
- `KissStatsScreen` GUI (opened with K key) shows heart icon and kiss count

### Debug Tools

- `/kissmod toggle` - opt out of kissing
- `/kissmod stats` - display kiss count
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
  kissmodClient.java        - Client entry point + tick/keybind/prompt/stats setup
  CommandHandler.java       - /kissmod command tree
  Config.java               - ModConfigSpec builder
  DebugRenderer.java        - Client debug wireframe overlay
  KeybindHandler.java       - V (kiss) and K (stats) keybinds + raycast targeting
  KissDetectionHandler.java - Core detection + kiss lifecycle + damage/disconnect handlers
  KissLog.java              - Structured SLF4J markers
  KissPlayerData.java       - Attachment data + codec (includes totalKisses)
  KissPromptOverlay.java    - On-screen kiss request prompt + Y/N key handling
  KissStatsScreen.java      - GUI screen showing total kiss count
  ModAttachments.java       - Attachment type registration
  RequestManager.java       - Pending kiss requests + per-player request cooldown
  Sounds.java               - SoundEvent registry
  StressTestState.java      - Stress test counters
  mixin/
    PlayerRendererMixin.java - Head/body tilt animation mixin (forward + side-to-side)
  network/
    ClientPayloadHandler.java - Inbound packet handler (client)
    KissRequestPacket.java    - C2S kiss request payload
    KissResponsePacket.java   - C2S kiss response payload
    KissPromptPacket.java     - S2C prompt overlay payload
    KissExecutePacket.java    - S2C kiss execution payload
    KissEndPayload.java       - S2C kiss end payload (carries both UUIDs)
    ModPayloads.java          - Payload registration (versioned)
  gametest/
    KissDevelopmentGameTests.java - GameTest suite
```

## Build & Run

- Java 21, NeoForge 1.21.1, Gradle wrapper
- `gradlew.bat build` - compile + jar
- `gradlew.bat runGameTestServer` - run GameTests
- `gradlew.bat runServer`/`runClientPlayerOne`/`runClientPlayerTwo` - dev runs
