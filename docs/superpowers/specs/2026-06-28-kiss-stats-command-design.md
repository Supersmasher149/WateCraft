# Kiss Stats Command — Design Document

**Date:** 2026-06-28
**Status:** Approved for implementation
**Mod:** kissmod (NeoForge 1.21.1)

## Overview

Add a `/kissmod stats` chat command that displays the executing player's total kiss count.

## Storage

Add a single `totalKisses` field to `KissPlayerData`:

```
- Field: int totalKisses (default 0)
- Serialization: optionalFieldOf("totalKisses", 0) for backward compatibility with existing save data
```

## Increment Logic

In `KissDetectionHandler.startKiss()`, after setting kiss state on both players:

```
initiator.getData(ModAttachments.kissData()).totalKisses += 1
target.getData(ModAttachments.kissData()).totalKisses += 1
```

This counts every kiss start event for both participants.

## Command

Add a sub-command to the existing `/kissmod` command tree:

```
/kissmod stats
```

- No arguments (self-only, per user preference)
- Sends success message: `"Your kiss stats: X total kisses"`
- Uses `ctx.getSource().sendSuccess()`

## Files Changed

| File | Change |
|------|--------|
| `KissPlayerData.java` | Add `totalKisses` field, update constructor(s), getter/setter, CODEC |
| `KissDetectionHandler.java` | Increment `totalKisses` in `startKiss()` for both players |
| `CommandHandler.java` | Add `stats` sub-command to the `kissmod` literal |

## Testing

- `/kissmod stats` returns `"Your kiss stats: 0 total kisses"` for a fresh player
- After a kiss event, both participants' stat increments by 1
- Stats persist across server restarts (serialized in attachment data)
- Existing save data without `totalKisses` field loads gracefully (defaults to 0)

## Out of Scope

- Per-partner breakdown
- Duration tracking
- Leaderboards or scoreboard integration
- GUI screens
- Web export
