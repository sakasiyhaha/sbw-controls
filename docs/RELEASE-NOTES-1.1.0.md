# SBW Controls 1.1.0 — Release Notes

**Release date:** 2026-11-07

## Highlights

This release focuses on camera feel: the third-person dynamic camera now uses a fixed-substep physical spring, exposes its damping and input smoothing, and softly saturates the turn-lag limits so the camera no longer stops abruptly at the edge of its travel.

## Camera changes

- **Fixed 120 Hz substep spring:** offset yaw/pitch, roll and position use a semi-implicit spring integration with up to 12 substeps per frame. Behaviour is much more consistent at low FPS, and the camera no longer snaps to its target in a single frame.
- **Spring damping ratio** (`camera.spring_damping`, default `0.90`, range 0.5–1.5):
  - `1.0` = critically damped / most stable
  - below `1.0` = slightly bouncy and more organic
  - above `1.0` = stiffer and settles faster
- **Angular-rate smoothing** (`camera.rate_smoothing`, default `0.10 s`, range 0–0.3): low-pass time for yaw/pitch/roll rates. Higher values soften the initial camera response.
- **Turn-lag limit softness** (`camera.soft_limit`, default `1.0`, range 0–1): the ±12° yaw / ±8° pitch camera offsets now use tanh soft saturation, so the camera decelerates as it approaches the limit instead of hitting a hard clamp. `0` restores the old hard-clamp behaviour.
- First-person / ADS zoom keeps critically damped behaviour and remains stock by default.

## UI / telemetry

- **Camera → Dynamics** now exposes:
  - Spring damping ratio
  - Angular-rate smoothing
  - Turn-lag limit softness
- Telemetry now records `springDamping`, `rateSmoothing`, and `softLimit`.
- Reset-page defaults include the new camera values.

## Unchanged

- Control schemes, B-key/config sync, vehicle categories and multiplayer degradation are unchanged.
- Tanks, cars, boats and other non-aircraft vehicles remain stock.
- Requirements are unchanged: Minecraft 1.20.1, Forge 47.3.0, Java 17, Superb Warfare 0.8.9.2, Kotlin for Forge 4.12.0.

## Install

1. Install Minecraft 1.20.1 + Forge 47.3.0 + Java 17.
2. Put into `mods/`:
   - `sbwcontrol-1.1.0.jar`
   - `superbwarfare-0.8.9.2-all.jar`
   - `kotlinforforge-4.12.0-all.jar`
3. Launch.

## License

GPL-3.0. Unofficial addon; not affiliated with the Superb Warfare team.

Source: https://github.com/sakasiyhaha/sbw-controls
