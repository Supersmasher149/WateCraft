# KissCraft Multiplayer Testing Checklist

## Detection

| Done | Scenario | Expected result | Notes |
|---|---|---|---|
| [ ] | Standing still | Kiss starts only when both players meet distance, look, line-of-sight, cooldown, and opt-out checks |  |
| [ ] | Walking | Detection remains stable while players move slowly |  |
| [ ] | Sprinting | No repeated spam or stuck state while players pass through range |  |
| [ ] | Crouching | Eye-distance logic still behaves predictably |  |
| [ ] | Swimming | Blocked or allowed behavior matches design |  |
| [ ] | Gliding | Interaction is blocked while fall flying |  |
| [ ] | Riding entities | Interaction is blocked while passenger |  |

## Networking

| Done | Scenario | Expected result | Notes |
|---|---|---|---|
| [ ] | Normal localhost | Both clients see start/end animation state |  |
| [ ] | Added latency | No permanent desync or duplicate packets |  |
| [ ] | Reconnect | Attachment state is clean after reconnect |  |
| [ ] | Disconnect during animation | Partner state is cleared and end packet is sent |  |
| [ ] | Teleport | Out-of-range or cross-position state ends cleanly |  |
| [ ] | Dimension change | Partner cleanup works across dimensions |  |

## Animation

| Done | Scenario | Expected result | Notes |
|---|---|---|---|
| [ ] | Normal completion | Animation ends after configured duration |  |
| [ ] | Interruption | Damage/disconnect cancels animation |  |
| [ ] | Interpolation | Head/body tilt appears smooth |  |
| [ ] | Multiple players nearby | Only one valid target starts |  |
| [ ] | Third-person | Visual state is clear on both clients |  |
| [ ] | First-person | No camera-breaking side effects |  |

## Effects

| Done | Scenario | Expected result | Notes |
|---|---|---|---|
| [ ] | Particles enabled | Hearts spawn at midpoint |  |
| [ ] | Particles disabled | No hearts spawn |  |
| [ ] | Sound enabled | Sound event plays if asset exists |  |
| [ ] | Regeneration enabled | Both players receive Regeneration I |  |
| [ ] | Glowing enabled | Both players receive Glowing |  |

## Config

| Done | Scenario | Expected result | Notes |
|---|---|---|---|
| [ ] | Max distance | Detection range changes after reload/restart as supported |  |
| [ ] | Require look | Kisses can start without mutual look when disabled |  |
| [ ] | Cooldown | Cooldown duration matches config |  |
| [ ] | Animation duration | Remaining kiss ticks match configured duration |  |
| [ ] | Disable head tilt | Client animation stops rendering |  |
| [ ] | Debug mode | Debug logs, commands, and renderer toggle together |  |
