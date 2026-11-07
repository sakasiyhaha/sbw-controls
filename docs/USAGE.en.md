# SBW Controls — User Guide

**Flight control schemes & BF-style dynamic third-person aiming camera for Superb Warfare.**

- Mod name: **SBW Controls**
- Mod ID: `sbwcontrol`
- Version: 1.1.0
- Author: sakasiyhaha
- Environment: Minecraft **1.20.1**, Forge **47.3.0**, Java **17**
- Dependencies: Superb Warfare **0.8.9.2**, Kotlin for Forge **4.12.0**
- License: **GPL-3.0** — unofficial addon, not affiliated with the Superb Warfare team

---

## 1. What this mod does

Superb Warfare already has its own flight model and vehicle camera. SBW Controls adds:

- a **roll-bird style** control scheme: mouse X for roll, A/D for yaw, mouse Y for pitch;
- a **BF-style dynamic third-person camera** with turn lag, roll inertia, spring-arm position lag, adaptive damping and speed effects;
- **independent first-person / ADS zoom camera settings** so right-click zoom does not inherit third-person camera motion;
- **full configuration** through the in-game config screen, TOML files, and native key bindings.

It does not use Mixin and does not modify the Superb Warfare jar. Server-side control injection uses Superb Warfare's public input fields; camera work is done through Forge's `ComputeCameraAngles` event.

---

## 2. Features

### Control schemes

| Scheme | Fixed-wing | Helicopter | Notes |
|---|---|---|---|
| **YAW_BIRD** | Stock | Stock | The addon does not take over |
| **ROLL_BIRD** | Mouse X = roll, A/D = yaw, mouse Y = pitch | Same | Default, recommended starting point |
| **ROLL_YAW_HYBRID** | Mouse X blends roll and yaw | Same | Mix of both styles |
| **CUSTOM** | Pitch/roll/yaw each use Mouse X, Mouse Y or keys | Same | Full three-axis customization |

- A/D yaw adds an extra direct yaw rate on the server (about 30°/s by default), because Superb Warfare's normal mouse-X yaw is clamped to roughly 6°/s in level flight.
- While a scheme is active, A/D no longer leaks into Superb Warfare's stock roll input.
- CUSTOM axis selectors are editable only when the scheme is CUSTOM.

### B key: switch the current vehicle's scheme

When riding a fixed-wing or helicopter, press **B**:

- fixed-wing: cycles the **fixed-wing default scheme**;
- helicopter: cycles the **helicopter default scheme**;
- the B key and the config screen edit the **same config values** in both directions.

When not riding a valid aircraft:

- on foot: shows “ride a fixed-wing/helicopter before pressing B”;
- in a tank/car/boat: shows “B only works in fixed-wing/helicopter”; config is not changed.

If you never use B, change the two defaults directly in **Config → Control**. Both entries are the same settings.

**Other vehicles stay vanilla**: tanks, cars, boats, etc. are not taken over — not by controls, not by the camera, and B does not modify their settings.

### Dynamic third-person camera

**Basic**

| Option | Default | Purpose |
|---|---|---|
| Camera enabled | On | Master switch |
| Camera preset | BATTLEFIELD | BATTLEFIELD / DELTA / WARTHUNDER / VANILLA / CUSTOM |
| Yaw smoothing | 0.10 s | Yaw catch-up speed |
| Pitch smoothing | 0.08 s | Pitch catch-up speed |
| Turn lag | 0.22 s | Camera offset = angular rate (°/s) × seconds |
| Speed FOV | 0.10 | FOV expands with speed |
| Dynamic distance | 0.5 | Camera pulls back slightly with speed |
| ADS scale | 0.35 | Scales camera dynamics while aiming |
| Roll follow ratio | 1.0 | 1.0 = follow aircraft roll; 0 = horizon stable |
| Center lateral | 1.0 | Pulls offset seats (AH-6, Mi-28, etc.) to the centerline |

**Dynamics**

| Option | Default | Purpose |
|---|---|---|
| Invert turn lag (horizontal) | Off | Flip left/right camera trail |
| Invert turn lag (vertical) | Off | Flip vertical camera trail |
| Roll inertia | 0.10 s | Camera trails aircraft roll |
| Position spring | 0.08 s | Spring-arm position lag |
| Adaptive damping | 1.5 | More agile camera at high yaw/roll rates; 0 = fixed |
| Spring damping ratio | 0.90 | 1 = critically damped, below 1 = slightly bouncy, above 1 = stiffer |
| Angular-rate smoothing | 0.10 s | Low-pass time for mouse input; higher = softer onset | 
| Turn-lag limit softness | 1.0 | tanh soft saturation near the ±12° / ±8° offsets; 0 = old hard clamp |

**First person / crosshair**

| Option | Default | Purpose |
|---|---|---|
| First-person dynamics | Off | When off, first person and ADS zoom stay fully stock |
| FP yaw/pitch smoothing | 0.03 s | Small first-person smoothing |
| FP turn lag / roll inertia / position spring | 0 | No sway/roll/spring in first person by default |
| FP adaptive gain | 0 | Fixed damping in first person |
| Nose/gun marker | Off | Projected gun direction marker (debug/aim aid) |
| Replace third-person crosshair | Off | Hides the stock third-person vehicle crosshair and draws a projected reticle; first-person/ADS keeps the stock crosshair |

Implementation highlights:

- offsetYaw/Pitch, roll and position use a **fixed 120 Hz substep spring** with velocity carried across frames, not per-frame lerp; damping ratio 0.90 is near critical; 
- turn offsets use **tanh soft saturation** near the ±12° / ±8° limits, so the camera decelerates instead of hitting a hard clamp;
- roll target is unwrapped across ±180°, so a fully inverted attitude can smoothly recover;
- angular rates use tick-level angle deltas with a low-pass filter to avoid frame-rate spikes;
- first-person/ADS zoom has a separate parameter set.

### Settings UI

**Mods → SBW Controls → Config** opens one window with five tabs:

- **Control**: fixed-wing default, helicopter default, CUSTOM axis sources; read-only note that other vehicles stay vanilla.
- **Camera**: Basic / Dynamics / First Person & Crosshair.
- **Operation**: input model (rate / virtual stick), mouse sensitivity/smoothing/deadzone, stick centering/gain, per-axis gains, inversions, hybrid yaw coupling, “Open vanilla key binds” and “Reset addon keys”.
- **Server**: A/D yaw rate, channel scales, input timeout, allowed schemes, vehicle-type application, logging (single-player only).
- **Diagnostics**: debug HUD, telemetry toggle and interval.

Every page has **Reset page**. Camera reset applies to the current sub-tab.

### Key bindings

All keys are registered in **Options → Controls → Key Binds → SBW Controls**:

| Default | Action |
|---|---|
| A / D | A/D yaw left/right |
| Q / E | Q/E roll left/right (CUSTOM key axes) |
| unbound | Custom pitch up / down (CUSTOM + key source) |
| B | Toggle current vehicle scheme |
| unbound | Toggle dynamic camera |
| unbound | Open SBW Controls config |

The Operation tab has buttons for the vanilla key-bind screen and resetting only these 9 addon keys.

---

## 3. Installation

### Standalone

1. Install Minecraft 1.20.1, Forge 47.3.0 and Java 17.
2. Put these files in `mods/`:
   - `sbwcontrol-1.1.0.jar`
   - `superbwarfare-0.8.9.2-all.jar`
   - `kotlinforforge-4.12.0-all.jar`
3. Launch.

GeckoLib, Cloth Config, Curios, Flywheel, Ponder, SimpleBedrockModel and Rhino are embedded in the Superb Warfare `-all` jar through JarJar; they do not need to be downloaded separately.

### Modpack

Import the `SBW-Controls-Pack-1.1.0` folder (or zip) into Prism Launcher / MultiMC, or copy `mods/`, `config/` and `defaultconfigs/` into a 1.20.1 + Forge 47.3.0 instance.

---

## 4. Quick start

1. Sit in a fixed-wing or helicopter.
2. Default scheme is **ROLL_BIRD**: mouse X rolls, A/D yaws, mouse Y pitches.
3. Press **B** to cycle schemes.
4. Tune the feel in **Mods → SBW Controls → Config → Camera**.
5. Change keys in **Options → Controls → Key Binds → SBW Controls**.
6. Right-click ADS zoom uses the stock camera by default; enable “First-person dynamics” if you want a small first-person smoothing.

---

## 5. Tuning guide

**Camera feels stiff / glued**
- increase Turn lag (0.22 → 0.30–0.40)
- increase Roll inertia (0.10 → 0.15–0.20)
- increase Position spring (0.08 → 0.12–0.18)
- increase Adaptive damping (1.5 → 2–3)
- lower Spring damping ratio (0.90 → 0.82–0.88) for a little bounce
- increase Angular-rate smoothing (0.10 → 0.12–0.15) for a softer onset
- keep Turn-lag limit softness at 1.0

**Camera feels too swingy / motion sickness**
- decrease Turn lag
- decrease Roll inertia
- decrease Position spring
- lower Roll follow ratio to 0.7 or lower
- raise Spring damping ratio (0.95–1.05) to reduce bounce
- lower Angular-rate smoothing (0.08–0.10) to reduce lag

**Left/right or up/down is inverted**
- enable the corresponding “Invert turn lag” toggle

**ADS zoom still moves**
- keep “First-person dynamics” off; or set FP turn lag/roll inertia/position spring to 0

**The turn-lag limit still feels abrupt**
- keep Turn-lag limit softness at 1.0; increase Turn lag (0.22 → 0.28–0.36) so the target reaches the limit more slowly
- if the maximum offset now feels too small, increase Turn lag or use 0.85–0.95 as a compromise

**Low FPS makes the camera choppy**
- check F3; if FPS is very low, fix performance first. A per-frame camera cannot look smoother than the frame rate.

---

## 6. Configuration files and commands

| File | Path |
|---|---|
| Client | `config/sbwcontrol-client.toml` |
| Single-player server | `saves/<world>/serverconfig/sbwcontrol-server.toml` |

Client commands:

```
/sbwctl status
/sbwctl selftest
/sbwctl scheme aircraft|helicopter <scheme>
/sbwctl camera on|off|status
/sbwctl camera delta
/sbwctl camera turnlag <0~0.6>
/sbwctl camera rolllag <seconds>
/sbwctl camera rollratio <0~1.5>
/sbwctl camera lateral <0~1>
/sbwctl camera adaptive <0~5>
/sbwctl camera poslag <seconds>
/sbwctl camera ads <0~1>
/sbwctl debug on|off
/sbwctl telemetry on|off|status
```

Server commands (OP level 2):

```
/sbwctlserver status
/sbwctlserver scale yaw|roll|pitch <value>
/sbwctlserver yawrate <0~180>
/sbwctlserver timeout <ticks>
/sbwctlserver allow ROLL_BIRD|ROLL_YAW_HYBRID|CUSTOM true|false
```

---

## 7. Multiplayer

- **Server has the addon**: clients can select schemes; the server applies them subject to its allowed list and scaling.
- **Server does not have the addon**: the client degrades gracefully:
  - scheme switching is disabled;
  - dynamic camera, keybinds and client settings still work;
  - the HUD shows an “unsupported server” notice.
- The server has the final say; disallowed schemes are ignored.

The jar uses `displayTest="IGNORE_ALL_VERSION"`, so a missing server-side addon does not show a red X on the server list.

---

## 8. Compatibility & limitations

- Forge 1.20.1 only; no Fabric.
- Only fixed-wing and helicopter vehicles are taken over. Tanks, cars, boats, etc. remain stock.
- Only the first passenger/pilot seat is affected.
- Not taken over: loiter autopilot, wrecks (`isWreck`), TOM6 special engine, Superb Warfare free camera.
- Depends on public Superb Warfare 0.8.9.2 input fields/camera methods; a major update may require an addon update. Capability probing disables affected features instead of crashing.
- First-person/ADS zoom is independent; with `first_person=false` it stays fully stock.

---

## 9. FAQ

**B does nothing**
- Make sure you are riding a fixed-wing/helicopter in the pilot seat.
- On a server without the addon, scheme switching is disabled.

**Camera has no effect**
- Check that Dynamic Camera is enabled and the preset is not VANILLA.
- First-person/ADS does not use third-person dynamics by default.

**A/D does not yaw**
- Check the scheme (YAW_BIRD is stock; use ROLL_BIRD / HYBRID / CUSTOM).
- On a server, check that the server allows the scheme and the A/D yaw rate.

**Ads zoom still sways**
- Keep First-person dynamics off, or set FP turn lag/roll inertia/position spring to 0.

**Inverted camera does not recover**
- 1.1.0 fixes the ±180° roll discontinuity. If it still happens, set `camera.roll_ratio` to 0 and report with logs.

---

## 10. Credits & license

- Addon: sakasiyhaha
- Superb Warfare: Atsuishio, Roki27, Light_Quanta and contributors (GPL-3.0)
- Kotlin for Forge: thedarkcolour (LGPL-2.1)

SBW Controls is licensed under **GPL-3.0**. Source code is available in the release/source package and should be linked from every download page.

> Unofficial addon; not affiliated with the Superb Warfare team.
