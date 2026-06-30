# Kiss Stats Screen — Design Document

**Date:** 2026-06-29
**Status:** Approved for implementation
**Mod:** kissmod (NeoForge 1.21.1)

## Overview

Add a toggleable in-game stats screen that displays the player's lifetime kiss total in a clean, vanilla-style GUI.

## Data Source

Reuses the existing `totalKisses` field in `KissPlayerData` (added in the stats-command spec). No new attachment fields, no new network payloads — the client already has the data via normal attachment sync.

## Screen Layout & Behavior

- Opens as a full-screen overlay (like vanilla achievements GUI): dark semi-transparent background covering the full window
- Centered content: a small heart icon (❤) above the text
- Main label: `"Kisses: 42"` rendered in the default Minecraft font at a large scale
- Optional small text at bottom: `"Press [key] to close"` (informational, Esc also works)
- No title bar, no tabs, no extra chrome
- Closes on `Esc` (handled by Screen base class) or pressing the same keybind again

## Keybinding

- New keybind entry in `KeybindHandler.java` alongside existing `key.kissmod.request`
- Suggested default: `K` or `J` (no conflict with existing `V` for kiss request)
- Toggle behavior: press opens if closed, closes if open
- Client-side only — no network traffic

## Implementation Mapping

| Concern | Approach |
|---------|----------|
| New file | `KissStatsScreen.java` — extends `Screen` |
| Screen rendering | `render()` method: dark background fill, draw heart via `font.draw()` (or as text), draw count string centered |
| Keybind registration | Add to `KeybindHandler.java` — new `key.kissmod.stats` |
| Keybind handling | In `kissmodClient.java` client tick event: if key pressed and no screen open, push `KissStatsScreen`; if screen already open, close it |
| Data access | `Minecraft.getInstance().player.getData(ModAttachments.kissData()).getTotalKisses()` |
| Localization | Add `"key.kissmod.stats"` and `"kissmod.stats_screen.title"` entries to `en_us.json` |

## Files Changed

| File | Change |
|------|--------|
| `KissStatsScreen.java` (new) | Screen class with layout and rendering |
| `KeybindHandler.java` | Register new `key.kissmod.stats` keybind |
| `kissmodClient.java` | Add client tick handler to open/close the screen on key press |
| `en_us.json` | Add localization entries for new keybind and screen title |

## Design Decisions

- **Full screen vs HUD widget:** User chose toggleable screen — gives more room for future stats without layout changes
- **No new packet:** `totalKisses` is already synced to client via the attachment data codec at login/respawn, so the screen reads the local copy directly
- **Esc to close:** Built into Minecraft's `Screen` class — no custom handling needed

## Testing

- Press keybind → screen opens with correct kiss count displayed
- Press keybind again → screen closes
- Press Esc → screen closes
- Counter reads correctly after a kiss event (re-open screen to verify)
- No crash when opening screen on a dedicated server (client-side only, no dist violation)

## Out of Scope

- Session counters
- Per-partner breakdown
- Leaderboards or scoreboard integration
- HUD/toggleable overlay
- Decorative textures or custom rendering — text-only for initial pass
