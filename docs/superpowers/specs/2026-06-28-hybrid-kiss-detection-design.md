# Hybrid Kiss Detection System

## Problem
The uncommitted working tree converted automatic server-side kiss detection to a keybind-based request/response system but gutted the auto-detection logic. No new kisses can start automatically. The new system also has validation holes (missing blocked-state, cooldown, distance checks).

## Solution
Restore automatic detection as the primary trigger while keeping the keybind request/response system as a secondary trigger. Both paths converge on `KissDetectionHandler.startKiss()`.

## File Changes

### KissDetectionHandler.java
Restore `onPlayerTick` auto-detection pipeline between `tickCooldown()` and the existing `if (!data.isKissing()) return`. Logic: blocked state -> cooldown -> eye-distance nearest-player search -> target state/cooldown/opt-out -> mutual look (gated by `Config.REQUIRE_LOOK`) -> LOS -> `startKiss()`. Keep `KissExecutePacket`, heart ring, `RequestManager` calls, cleaned style.

### KissRequestPacket.java
Add blocked-state, attachment-cooldown, and distance checks for both requester and target before creating a pending request.

### KissResponsePacket.java
Add blocked-state check for both, mutual look check if `Config.REQUIRE_LOOK`, before accepting.

### RequestManager.java
Change `REQUEST_COOLDOWN_MS` from 30000 to 2000 (anti-spam only; real cooldown uses attachment system).

### KeybindHandler.java
Default key: `GLFW.GLFW_KEY_V`.

### kissmodClient.java
Restore `NeoForge.EVENT_BUS.addListener(DebugRenderer::onRenderLevel)` alongside new registrations.

### CommandHandler.java
Restore `/kissmod stress` sub-commands and helpers. Keep `requestCooldownRemaining` in `debugState`.

### DebugRenderer.java
Restore from HEAD.

### StressTestState.java
Restore from HEAD.

## No changes
ClientPayloadHandler, KissPromptOverlay, KissPromptPacket, KissExecutePacket, KissEndPayload, KissPlayerData, ModAttachments, ModPayloads, Config, kissmod, mixins, game tests.

## Flow
- Auto (every 5th tick): onPlayerTick -> validate -> startKiss()
- Keybind [V]: raycast -> KissRequestPacket (validate) -> KissPromptPacket -> client Y/N -> KissResponsePacket (re-validate) -> startKiss()
