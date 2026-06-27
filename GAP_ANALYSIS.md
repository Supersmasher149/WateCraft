# KissMod — Feature Gap Analysis

## CRITICAL

### 🐛 Kiss never auto-ends (`KissDetectionHandler.java:110-126`)
`ANIMATION_DURATION_TICKS` (default 60 ticks / 3s) is fetched in `startKiss`, packed into the network packet, and **never used anywhere**. `tickActiveKiss` only checks target validity (null, dead, too far, not kissing back). There is no duration counter in `KissPlayerData`, no decrement in `tickActiveKiss`, and the client handler ignores `durationTicks` entirely. Kisses persist until interrupted by damage, disconnect, or walking away.

**Fix needed:** Add `remainingKissTicks` field to `KissPlayerData`. Decrement in `tickActiveKiss`. End kiss when it reaches zero.

### 🐛 Effects only applied to one player (`KissDetectionHandler.java:178-183`)
In `endKiss`, regeneration and glowing are only applied to the player whose `endKiss` was called. The target's state is cleared at lines 162-165 but effects are **not** applied to them. If the kiss ends because Player A walked away, only Player A gets the buff. If both damage handlers fire in the same tick, the second is gated by `isKissing()` (already false from the first), so the second player never gets effects either.

**Fix needed:** Apply regen/glowing to both players, or remove the one-sided application.

### 🐛 Hardcoded `getNearestPlayer` radius 2.0 (`KissDetectionHandler.java:53`)
`player.level().getNearestPlayer(player, 2.0D)` uses a hardcoded search radius of 2.0 blocks. `MAX_DISTANCE` is configurable up to 10.0. If a user sets `MAX_DISTANCE = 5.0`, `getNearestPlayer` only searches within 2.0, so players 2.1–5.0 blocks away are never found — the config value is unreachable. These two values must be kept in sync.

**Fix needed:** Use `MAX_DISTANCE` for the search radius, or add a separate config.

### 🔇 Missing sound file (`assets/kissmod/sounds/kiss_sound.ogg`)
Declared in `sounds.json` and referenced by `Sounds.java`, but the `.ogg` file does not exist. The kiss fires silently. Documented in AGENTS.md as a known issue but never fixed.

**Fix needed:** Add a kiss sound effect (CC0 license or original).

### 🚫 No opt-out mechanism
Any player within range who meets the look/LOS checks will be kissed automatically. There is no command, keybind, config toggle, or `/tag` system to decline. This is a consent/privacy concern for social servers.

**Fix needed:** Add per-player opt-out (e.g., `/kissmod toggle`, or a `kissmod:opt_out` scoreboard criterion, or a config boolean on the player's attachment data).

---

## HIGH

### 🔄 State desync — kiss partner disconnects mid-kiss (`KissDetectionHandler.java:207-213`)
`onPlayerDisconnect` calls `endKiss(player)` which clears both players' data and sends `KissEndPayload`. But the order of operations: the target player already has their player entity removed or invalidated. `player.level().getPlayerByUUID(targetId)` may return null, so the target's state is never cleared on the server side. If the target reconnects, their attachment data (which survives death via `copyOnDeath()`) still shows `kissing=true` — they are stuck in a kissing state permanently until manually triggered.

**Fix needed:** On disconnect, clear the disconnected player's data AND also clear the remaining partner's data. Or check if target exists in the world (not just by UUID lookup).

### 📐 Head tilt works on only one model (`PlayerRendererMixin.java:14`)
Mixins into `PlayerModel.class` — this only covers the default Steve/Alex model. Other player model mods (Customizable Player Models, Figura, etc.) completely bypass `PlayerModel.setupAnim`. No crash, but the tilt silently does nothing.

**Fix needed:** Either document this limitation, or integrate with CPM/Figura APIs for broader model support.

### 📡 `KissEndPayload` can be sent while player is in a different dimension
`endKiss` calls `PacketDistributor.sendToPlayersInDimension((ServerLevel) player.level(), ...)`. If one player changes dimension while kissing, `tickActiveKiss` detects they're too far (`distanceToSqr > 16.0`) and calls `endKiss`. But now the target is in a different dimension, so the end packet never reaches the target's client. The target's client stays in `kissing=true` permanently.

**Fix needed:** Send end packet to both dimensions, or ensure cross-dimension cleanup happens.

### 🔄 Race condition in `endKiss` when both players damaged same tick
If both players in a kiss take damage simultaneously (e.g., both hit by the same explosion), `LivingDamageEvent.Post` fires for both. The first handler clears both players' states. The second handler's `isKissing()` guard prevents re-entry, but **only the first player gets the effects**. Which player gets them is non-deterministic (event order).

**Fix needed:** Make `endKiss` idempotent and apply effects to both participants regardless of who triggered the end.

### ⏱ `onPlayerDamage` / `onPlayerDisconnect` not throttled
The `tickCount % 5` throttle protects `onPlayerTick`, but `onPlayerDamage` and `onPlayerDisconnect` fire on every frame. If a player takes rapid damage (falling, fire, poison), `endKiss` is called multiple times per second. The `cooldownTicks` is reset each time (already harmless since the guard prevents re-entry), but redundant network packets are sent.

**Fix needed:** Add a guard or throttle to prevent redundant `endKiss` calls.

---

## MEDIUM

### 🎮 No `/kissmod` command system
No debug, administrative, or player commands exist. Server ops cannot:
- Start/end a kiss between players
- Set a player's cooldown
- Reload config without restart
- Check kissing state
- See who is kissing whom

**Fix needed:** Register a simple command with sub-commands.

### 📊 No statistics or advancements
No `PlayerStats` tracking for kisses. No advancements (e.g., "First Kiss", "Kiss 100 Times", "Kiss in the Nether", "Kiss While Invisible"). These are the primary retention drivers on social servers.

**Fix needed:** Add `CriteriaTriggers`-based advancements and statistics.

### 🎭 No animation progression
Head tilt is a static offset applied every frame in `setupAnim`. There is no:
- Tilt-in animation (ramp up over 5-10 ticks)
- Hold phase at full tilt
- Tilt-out animation (ramp down over 5-10 ticks)
- Head movement toward target's face position

**Fix needed:** Use the `remainingKissTicks` / `ANIMATION_DURATION_TICKS` ratio to animate tilt intensity over the kiss duration.

### 📦 No data generator classes
`src/generated/resources/` is empty. No `Datagen` classes exist for:
- `LanguageProvider` (auto-generate English translations)
- `SoundDefinitionsProvider` (auto-generate `sounds.json`)
- `AdvancementProvider`
- `TagsProvider`

**Fix needed:** Add datagen classes.

### 🎮 Client-side `durationTicks` ignored in handler (`ClientPayloadHandler.java:9-18`)
`KissStartPayload.durationTicks` is received on the client but never used. The client could use it for a HUD timer, cooldown bar, or animation timing, but currently ignores it.

**Fix needed:** Use `durationTicks` for client-side animation timing.

### 🌐 UUID serialized as string instead of raw longs
Both `KissStartPayload` and `KissEndPayload` serialize UUIDs as UTF-8 strings (36 bytes each). Using `ByteBufCodecs.UUID` would reduce each UUID to 16 bytes and eliminate parsing overhead.

**Fix needed:** Replace `ByteBufCodecs.STRING_UTF8.map(...)` with `ByteBufCodecs.UUID`.

### ⚠️ `endKiss` has no `isKissing` guard internally
`endKiss` unconditionally clears state and resets cooldown. If a non-kissing player somehow reaches it (e.g., via a future code path), cooldown is reset to max unnecessarily. Currently all callers guard externally, but this is fragile.

**Fix needed:** Add `if (!data.isKissing()) return;` as first line of `endKiss`.

### 🔀 Mixin filename / target mismatch
File `PlayerRendererMixin.java` mixes into `PlayerModel`, not `PlayerRenderer`. Confusing for maintainers.

**Fix needed:** Rename to `PlayerModelMixin.java` and update `mixins.json`.

---

## LOW

### 🔄 Client-side cooldown not synced
Cooldown is tracked on the server's attachment and decremented every 5 ticks. The client never receives cooldown state. Players have no visual feedback showing how long until they can kiss again.

**Fix needed:** Include cooldown in `KissEndPayload` or send a separate sync packet. Show a subtle cooldown indicator in the HUD.

### 🔄 No periodic state reconciliation
If a packet is dropped (rare with TCP, but possible on server lag/overflow), the client stays in the wrong state (e.g., stuck `kissing=true`). There's no periodic "are you still kissing?" sync.

**Fix needed:** Add a lightweight periodic sync (e.g., every 20 ticks, include kissing state in a keepalive-like packet).

### 👥 Detection radius mismatch
Config `MAX_DISTANCE` default is 1.5. The `hasLineOfSight` raycast range is hardcoded at 3.0. Both default to different values but are conceptually related.

**Fix needed:** Derive `RAYCAST_RANGE` from `MAX_DISTANCE` (with a small buffer).

### ⚡ Performance: particles sent every 5 ticks during kiss
`startKiss` sends particles once. But `tickActiveKiss` does not re-send particles during the kiss. This might be intentional (hearts only on start) or a missing feature (continuous hearts during kiss).

**Fix needed:** Either document as intentional or add periodic heart particles during the kiss.

### 📋 Event log level
`[kiss]` log messages use `LOGGER.info` (visible in default console). Should be `LOGGER.debug` for production builds.

**Fix needed:** Change to `LOGGER.debug` or guard behind a config flag.

---

## MOD COMPATIBILITY (CROSS-CUTTING)

| Mod | Risk | Impact | Mitigation |
|---|---|---|---|
| **Customizable Player Models (CPM)** | **High** | CPM replaces `PlayerModel` entirely. Mixin silently does nothing. | Detect CPM presence; skip mixin; provide API hook. |
| **First Person Model / Better Third Person** | **Medium** | Head tilt applied on server-side model may cause camera jitter in first-person if camera follows head. | Test; add a config to disable first-person tilt. |
| **Not Enough Animations / Player Animator** | **Medium** | Mixin injection at `TAIL` of `setupAnim` could conflict with other `setupAnim` modifiers. | Use `@Redirect` or lower priority injection; test compatibility. |
| **Simple Voice Chat** | **Medium** | Proximity whisper/volume should ideally lower during kiss for immersion. | Provide a `KissEvent` that SVC or other mods can listen to. |
| **Freecam / Camera Perspective mods** | **Low** | Look-direction check uses actual player head yaw/pitch, not camera. In freecam the head might not point at the camera. | Document as known behavior; no fix needed. |
| **Create / other physics mods** | **Low** | No known conflicts. | None needed. |

---

## "WOW" FACTOR SUGGESTIONS

### 1. Kiss Chemistry System *(Medium effort, big retention)*
- Each player pair accumulates "chemistry" (stored per pair in a capability/attachment)
- Chemistry tiers unlock better effects: longer buffs, better particles, new potion effects (Speed, Haste, Resistance)
- Display chemistry level in the chat or HUD on kiss

### 2. Environment-Responsive Kisses *(Low effort, high charm)*
- Detect biome/biome tag at kiss location
- Water → bubble particles + Dolphin's Grace
- Nether → flame particles + Fire Resistance
- The End → purple portal particles + Slow Falling
- Flower forest → extra hearts
- Underground → cave sounds ambience
- Campfire nearby → warm orange particles
- Each adds ~5 lines of code; 8-10 biomes covers the "wow" factor

### 3. Kiss Event API for other mods *(Low effort, high integration value)*
- Fire `KissStartedEvent` and `KissEndedEvent` on `NeoForge.EVENT_BUS` with `@Cancelable` flag
- Any mod (voice chat, roleplay, dating mods) can listen, cancel, or react
- Supports `ICustomParticle` / `ICustomSound` overrides so other mods can customize effects
- Transforms kissmod from a standalone mod into a **social interaction platform**

---

## SUMMARY

| Priority | Count | Key items |
|---|---|---|
| **Critical** | 5 | Kiss never ends, effects one-sided, hardcoded 2.0 search, missing sound file, no opt-out |
| **High** | 5 | Disconnect state desync, cross-dimension packet loss, damage race condition, rapid damage spam, model compat |
| **Medium** | 8 | No commands, no advancements, static animation, no datagen, unused `durationTicks`, UUID perf, fragile endKiss, mixin name mismatch |
| **Low** | 5 | No cooldown sync, no reconciliation, mismatched ranges, missing particles, log level |
| **Compat** | 6 | CPM critical, FPM/animations/voice chat medium, freecam/create low |
| **Wow** | 3 | Chemistry system, environmental kisses, event API |
