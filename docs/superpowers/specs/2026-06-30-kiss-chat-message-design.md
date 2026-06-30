# Kiss Chat Message — Design Document

**Date:** 2026-06-30
**Status:** Approved for implementation
**Mod:** kissmod (NeoForge 1.21.1)

## Overview

Broadcast a chat announcement to all players on the server when two players naturally complete a kiss. Adds a config toggle for the feature.

## Scope

- Message fires **only on natural completion** (the EXITING phase fully plays out), not on interruption, death, or disconnect
- Broadcast to **all players on the server** (across all dimensions)
- Message is `"[Player1] and [Player2] kissed!"` using each player's display name
- Config option to enable/disable (default: `true`)

## Config

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `ENABLE_CHAT_MESSAGE` | boolean | `true` | Broadcast chat message on natural kiss completion |

Added to the **Effects** section of `Config.java` alongside `ENABLE_HEARTS`, `ENABLE_REGENERATION`, `ENABLE_GLOWING`.

## Localization

Add to `en_us.json`:
```json
"chat.kissmod.kiss_ended": "%s and %s kissed!"
```

## Implementation Mapping

| Concern | Approach |
|---------|----------|
| Message composition | `Component.translatable("chat.kissmod.kiss_ended", player.getName(), target.getName())` |
| Broadcast call | `player.getServer().getPlayerList().broadcastSystemMessage(component, false)` — system-style announcement |
| Broadcast location | `KissDetectionHandler.tickActiveKiss()` EXITING case, immediately before `cleanupKiss(player)` when `phaseTicks >= EXIT_TICKS` |
| Double-broadcast guard | Only the player with the lexicographically smaller UUID sends the message: `player.getUUID().compareTo(targetId) < 0` |
| Null safety | Guard with `if (target != null)` before accessing `target.getName()` |

## Delivery Method

`ServerPlayerList.broadcastSystemMessage(Component, false)` — sends as a gray italic system message visible to all players, consistent with vanilla join/leave announcements. The second parameter (`false`) means it is not included in the player's chat history in the log.

## Edge Cases

| Case | Behavior |
|------|----------|
| Target disconnects mid-kiss | Disconnect handler calls `cleanupKiss` directly — EXITING path never reached, no message |
| Kiss interrupted (target moves away, dies) | `cleanupKiss` called from HOLDING/ENTERING — no message |
| Both players reach EXITING completion same tick | UUID guard ensures exactly one broadcast |
| Config disabled | Config check at the broadcast site skips message entirely |

## Files Changed

| File | Change |
|------|--------|
| `Config.java` | Add `ENABLE_CHAT_MESSAGE` boolean config field |
| `KissDetectionHandler.java` | Add message broadcast in `tickActiveKiss` EXITING completion path |
| `en_us.json` | Add `chat.kissmod.kiss_ended` translation entry |

## Testing

- Two players naturally complete a kiss → all server players see `"Player1 and Player2 kissed!"` in chat
- Kiss is interrupted (player walks away, dies, disconnects) → no chat message
- Config `ENABLE_CHAT_MESSAGE = false` → no message on kiss completion

## Out of Scope

- Different messages for start vs end (user chose end only)
- Per-player toggles or opt-out per individual
- Custom message formatting or styling
- Rich text, emoji, or hover events in the message
