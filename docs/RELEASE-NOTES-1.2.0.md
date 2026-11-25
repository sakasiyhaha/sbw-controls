# SBW Controls 1.2.0 — Release Notes

**Release date:** 2026-11-24

## Highlights

This release focuses on the RollBird firing direction bug, fixed-wing camera quality and config cleanup.

- Fixed cannon shells/rockets drifting toward the rotation tangent during A/D turns.
- Added a server-side `getShootVec` Mixin so the actual projectile direction follows the client-frame aim.
- Fixed turret yaw compensation and 180° follow-camera roll handling.
- Added the fixed-wing independent camera backend.
- Cleaned obsolete server scaling config and refactored input application.

## RollBird firing direction

### Helicopter A/D yaw

A/D yaw now uses the native `mouseMoveSpeedX` channel while preserving the previous rate formula:

```text
mouseX = yaw * max_yaw_rate / (40 * syncedPropellerRot)
```

The native engine then applies:

```text
yRot += yawSpeed * clamp(2 * mouseX * syncedPropellerRot, -10, 10)
```

This keeps the previous `max_yaw_rate` / smoothing / sensitivity feel while keeping the flight model on the native input path.

### Mouse roll

Mouse roll is a direct analog `deltaRot` input:

```text
deltaRot = -roll * roll_scale
```

Roll smoothing is independent from yaw:

```toml
roll_smoothing = 0.02
roll_scale = 6.0
```

This keeps roll crisp and decoupled from the yaw feel formula. An earlier 6-tick pulse-key approach was removed because SW's key path ramps `deltaRot` through `holdTick`, which made roll feel heavy and delayed.

### Projectile direction

New `VehicleShootVecMixin` overrides `VehicleEntity.getShootVec(weaponName, partialTicks)` on the server while RollBird center aim is active, returning the client-frame aim direction. This applies to:

- fixed guns (AH-6 / A-10 / Ju-87 style)
- turret cannons (MI-28 etc.)
- rockets

The result: during A/D circular motion, shells/rockets leave along the client-frame aircraft/launcher direction instead of a delayed server heading.

### Turret fix

- Local turret yaw compensation: `bodyYaw - aimYaw` (matches SW `getBarrelYRot()` sign).
- Clamp to the real SW turret envelope `[-turretMaxYaw, -turretMinYaw]`.
- Center aim enabled in first person, follow and dynamic third-person modes.

## Camera changes

- Fixed-wing independent camera backend:
  - `fixed_wing_backend = INDEPENDENT` or `SW_OVERRIDE`
  - bounding-box center target
  - absolute independent distance and height
  - own collision ray and distance smoothing
- Follow camera 180°+ roll:
  - shortest-angle interpolation for yaw/pitch/roll
  - continuous camera Euler angles across ±180°
  - direct body basis for hard-follow
- Dynamic camera position no longer rotates with body roll.

## Config / UI

- View-mode page: fixed-wing backend, independent distance and height.
- Distance page: EXTRA/TOTAL, airplane/helicopter distances, collision and smoothing.
- Input feel page: yaw smoothing, independent roll smoothing, gains and deadzone.
- Server page: `max_yaw_rate`, `pitch_scale`, `roll_scale`, input timeout, allowed schemes, vehicle types, debug log.
- Removed obsolete `yaw_scale` and `separate_yaw` from config, UI, commands and locale; `roll_scale` is kept for roll feel.
- Refactored `ServerControlHandler.applyToVehicle()` into `helicopterMouseYaw()` and direct roll application.

## Network / compatibility

- Control packet protocol bumped to **3**.
- Client and server must both use `sbwcontrol-1.2.0`.
- New Mixin config `sbwcontrol.mixins.json`, registered in `mods.toml`.
- Requirements unchanged:
  - Minecraft 1.20.1
  - Forge 47.3.0
  - Java 17
  - Superb Warfare 0.8.9.2
  - Kotlin for Forge 4.12.0

## Testing

Forge GameTest 4/4 passed:

- field injection
- MI-28 turret center-aim sign
- AH-6 fixed gun `getShootVec` override
- helicopter native yaw formula

Independent Python verification:

- matrix-chain turret yaw derivation
- SW auto-aim fixed-point
- launch/impact error analysis
- old-sign future-side heatmap

## Known limitations

- Fixed-wing A/D yaw still uses direct `yRot` because the fixed-wing engine has no equivalent mouse-X yaw term.
- Dynamic camera screen center is not the nose; projectile direction follows the launcher/aircraft forward.
- HUD circle / “工” marker still uses Superb Warfare's actual fire-direction projection; a separate ballistic prediction HUD is not implemented yet.
- Large roll/pitch can have ~1°–7° Euler coupling error in SW.

## Install

1. Minecraft 1.20.1 + Forge 47.3.0 + Java 17.
2. Put into `mods/`:
   - `sbwcontrol-1.2.0.jar`
   - `superbwarfare-0.8.9.2-all.jar`
   - `kotlinforforge-4.12.0-all.jar`
3. Launch.

## License

GPL-3.0. Unofficial addon; not affiliated with the Superb Warfare team.

Source: https://github.com/sakasiyhaha/sbw-controls
