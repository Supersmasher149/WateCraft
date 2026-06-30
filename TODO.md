# KissMod TODO

## HIGH

- [ ] Add kiss_sound.ogg sound file to assets/kissmod/sounds/
- [ ] Switch UUID serialization from STRING_UTF8 to ByteBufCodecs.UUID

## MEDIUM

- [ ] Rename PlayerRendererMixin -> PlayerModelMixin (update mixins.json)
- [ ] Add animation progression (tilt-in/hold/tilt-out curves)
- [ ] Add advancements/statistics for kiss events
- [ ] Create data generator classes (LanguageProvider, AdvancementProvider, etc.)
- [ ] Add throttle guard on damage/disconnect endKiss calls
- [ ] Sync cooldown to client for HUD feedback

## LOW

- [ ] Add periodic state reconciliation (keepalive sync)
- [ ] Derive RAYCAST_RANGE from MAX_DISTANCE config
- [ ] Clean up empty client/ package or remove it
