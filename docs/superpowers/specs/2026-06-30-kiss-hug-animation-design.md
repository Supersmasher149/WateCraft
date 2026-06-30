# Kiss Hug Animation

## Problem

The kiss animation currently applies a uniform forward head/body tilt to both players (head nods down, body leans forward). Both players tilt in the same direction regardless of relative position. There is no arm movement, no position shift, and no visual sense of mutual embrace. The animation lives entirely inline in `PlayerRendererMixin.java`, making it hard to extend or test.

## Solution

Replace the simple head tilt with a full-body hug animation. When two players kiss, they move toward the shared midpoint, their arms wrap forward into a hugging pose, their bodies lean toward each other, and they gain invulnerability for the kiss duration. Animation logic moves from the mixin into a dedicated `KissAnimator` class.

## File Changes

### New Files

#### KissAnimator.java (client)
Static utility called from the mixin. Accepts the `PlayerModel`, `AbstractClientPlayer`, `KissPlayerData`, and partial tick. Calculates arm wrap, body lean, head tilt, and body rotation toward the partner.

- Finds partner in client level by `targetUUID`
- Computes direction vector from self → partner
- Applies phase-based interpolation (entering/holding/exiting inferred from `remainingKissTicks`)
- Arms: `leftArm.xRot` and `rightArm.xRot` rotate forward to hug position. Both arms rotate inward slightly (`zRot`)
- Body: `body.xRot` leans forward; `body.yRot` rotates toward partner
- Head: `head.xRot` tilts slightly down; `head.zRot` tilts toward partner
- No-op if partner not found or data not kissing

#### KissPoseManager.java (server)
Helper for position math. Methods:
- `computeHugPositions(ServerPlayer a, ServerPlayer b) → Vec3[]` — calculates hug positions at the midpoint, leaving ~0.4 blocks between players
- `tickPosition(ServerPlayer player, KissPlayerData data)` — called each `onPlayerTick`; advances phase and teleports player to the correct position

#### KissPhase.java (common)
Enum: `NONE`, `ENTERING`, `HOLDING`, `EXITING`

### Modified Files

#### PlayerRendererMixin.java
Mixin body replaced with a single delegation:
```java
KissAnimator.apply(model, entity, data, partialTick);
```
Remove: inline tilt calculations. Add: kiss phase gating (only `ENTERING`/`HOLDING`/`EXITING` trigger the animator).

#### KissPlayerData.java
Add fields:
- `kissPhase` (KissPhase, default `NONE`)
- `kissPhaseTicks` (int, default 0)
- `originalPos` (Vec3, nullable)
- `hugPos` (Vec3, nullable)

Serialized in CODEC with optional defaults for backward compatibility. Add getters/setters.

#### KissDetectionHandler.java
- `startKiss()`: call `KissPoseManager.computeHugPositions()`; store positions in both players' data; set phase to `ENTERING`
- `tickActiveKiss()`: advance phase ticks; transition ENTERING→HOLDING→EXITING based on tick thresholds; call `KissPoseManager.tickPosition()` each tick
- `endKiss()`: set phase to `EXITING`; after EXIT_TICKS, reset phase to `NONE` and restore original positions
- Add `onLivingDamagePre(LivingDamageEvent.Pre)` invulnerability handler (cancels damage during any kiss phase)
- `onPlayerDamage()`: keep existing `LivingDamageEvent.Post` handler — won't fire during kiss since Pre is cancelled

#### KissExecutePacket.java
Add fields: `kisserHugPos`, `targetHugPos`, `originalPosKisser`, `originalPosTarget` (all `Vec3`). StreamCodec updated with `ByteBufCodecs.VECTOR3D`.

#### ClientPayloadHandler.java
`handleKissExecute`: store hug positions and original positions in local attachment data for the kisser and target.

### No Changes
- `ModAttachments.java`, `ModPayloads.java` (no registration changes needed)
- `kissmodClient.java` (no new registrations needed)
- `Config.java` (no new config options)
- `KissEndPayload.java` (unchanged)
- `DebugRenderer.java`, `KeybindHandler.java`

## Constants

| Constant | Value | Location |
|----------|-------|----------|
| `ENTER_TICKS` | 10 | `KissAnimator` / `KissPoseManager` |
| `EXIT_TICKS` | 10 | `KissAnimator` / `KissPoseManager` |
| `HUG_DISTANCE` | 0.4 | `KissPoseManager` |
| Arm wrap angle | -1.2F (tuned visually) | `KissAnimator` |
| Head tilt max | 0.18F | `KissAnimator` |
| Body lean max | 0.15F | `KissAnimator` |

## Phase Inference (Client)

No explicit phase network sync. Client deduces phase from `remainingKissTicks`:
- `remainingKissTicks > DURATION - ENTER_TICKS` → `ENTERING`
- `remainingKissTicks <= EXIT_TICKS` → `EXITING`
- Otherwise → `HOLDING`

Progress within phase is `(phaseRemaining) / phaseDuration`, applied with smoothstep easing.

## Invulnerability

During any kiss phase, `LivingDamageEvent.Pre` is cancelled. The player takes no damage, knockback, or status effects from the damage source. The existing `onPlayerDamage` handler (`LivingDamageEvent.Post`) will not fire because the pre-phase event is cancelled. The existing `Regeneration` effect still applies after the kiss ends (unchanged behavior).

## Testing

- **GameTest**: Two players start kissing → verify positions converge to hug positions → verify positions return after kiss ends
- **GameTest**: Kissing player exposed to damage source → verify health unchanged
- **GameTest**: Kissing player pushed by knockback → verify player remains at hug position
- **Visual**: Manual inspection with two dev clients confirms arm wrapping, head tilt direction, and smooth transitions
- **Unit**: `KissPhase` inference from remaining ticks matches expected phase at all boundary conditions
- **Unit**: `KissPoseManager.computeHugPositions` returns correct positions for varying player offsets
