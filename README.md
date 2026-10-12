# Death Camera

This mod enhances your gameplay by introducing streamlined mechanics and practical features designed to fit naturally into the game. It focuses on visual clarity, seamless integration, and convenience without cluttering the vanilla experience.

![demo](https://cdn.jsdelivr.net/gh/monkey52x/deathcam@261309b0176b3a45c08ed8b495063ca86dd98bb4/assets/1.gif)

---

## Features

### Orbiting Death Camera
After death the camera smoothly flies out of your head into a slow orbit around your remains: eased fly-in, interpolated rotation at any FPS, smart wall collision and a steady picture without vanilla shake and tilt.

<details>
<summary>🎬 View gameplay demonstration</summary>

![orbit-demo](https://cdn.jsdelivr.net/gh/monkey52x/deathcam@261309b0176b3a45c08ed8b495063ca86dd98bb4/assets/1.gif)

</details>

---

### Static Third-Person View
A fixed third-person view without rotation. Camera height is fixed and the view stays locked on your remains. Optional dynamic FOV zoom can be disabled in the config.

<details>
<summary>🎬 View gameplay demonstration</summary>

![static](https://cdn.jsdelivr.net/gh/monkey52x/deathcam@261309b0176b3a45c08ed8b495063ca86dd98bb4/assets/3.gif)

</details>

---

### Random Camera Perspective
A static, non-rotating camera placed at a random viewpoint around the player for a cinematic angle.

<details>
<summary>🎬 View gameplay demonstration</summary>

![random](https://cdn.jsdelivr.net/gh/monkey52x/deathcam@261309b0176b3a45c08ed8b495063ca86dd98bb4/assets/4.gif)

</details>

---

### Corpse Mod Compatibility
Pairs seamlessly with the **Corpse** mod (or similar mods), focusing the camera smoothly on your remains upon death.

<details>
<summary>📷 View screenshot</summary>

![corpse](https://cdn.jsdelivr.net/gh/monkey52x/deathcam@bd1d6ffbdc8a590f5e410342a54c056376e65faa/assets/corpse.png)

</details>

---

## ⚙️ Installation

1. Make sure you have **Fabric** or **NeoForge** (Minecraft 1.21.1, Java 21) installed.
2. Download the latest `.jar` file of this mod (Fabric and NeoForge builds are separate files).
3. Place the downloaded `.jar` into your `.minecraft/mods` folder.
4. Launch the game!

> 🌐 **Client / Server:** The mod works fine purely client-side and in singleplayer. Installing it on the server is optional and only needed to receive live world updates around the player (kept chunks and mobs).
>
> ⚙️ **In-Game Configuration:** To configure the mod's settings directly inside the game menu, make sure to install **[Cloth Config API](https://modrinth.com/mod/cloth-config)** (plus [ModMenu](https://modrinth.com/mod/modmenu) on Fabric for the settings button). Without it, edit `config/deathcamera.json` by hand — it is re-read on every death, no restart needed.
>
> 💡 **Recommended:** For the best experience, it is strongly recommended to play alongside the **[Corpse](https://modrinth.com/mod/corpse)** mod (or any similar gravestone/corpse alternative), as showcased in Feature 4.

---

## Configuration

All settings live in `config/deathcamera.json` (or the Cloth Config screen in-game).

| Setting | Default | What it does |
|---|---|---|
| `enableOrbit` | `true` | Rotate the camera around the corpse. Ignored while third-person view on death is off. |
| `staticMode` | `follow` | Camera mode without rotation: `follow` (plain), `lock_on_corpse` (fixed height, locked on remains), `random_point` (instant random cinematic angle). Only when rotation is off. |
| `forceThirdPerson` | `true` | Switch to third-person view on death. Restored after respawn. |
| `speedDegPerTick` | `0.45` | Orbit speed, degrees per tick (~one lap in 40 s). |
| `transitionDurationSec` | `1.5` | Fly-in time to the orbit point. Third person only — from first person the camera is placed instantly. |
| `height` | `2.2` | Camera height above the death point (-5 to 5). |
| `targetHeightOffset` | `1.0` | Look-at height above the death point, ~chest level (1 to 5). |
| `cinematicFov` | `true` | Widen FOV on first-person death. When off, FOV stays at your setting. |
| `fovStart` | `90` | FOV the sweep starts from (30–100, always ends at 110). |
| `fovDurationSec` | `6.0` | Seconds for the FOV sweep. |
| `keepChunksUpdating` | `true` | Don't pause the world on the death screen; needs the mod on the server. |
| `deathKeepSeconds` | `300` | How long to keep the world held after death. `0` is vanilla, `-1` is unlimited. |
| `hideCorpse` | `false` | Never show your own corpse, from the first frame. |
| `removeRedScreen` | `true` | Remove the red gradient on the death screen. |
| `compactDeathScreen` | `false` | Compact death-screen layout. |
| `buttonsBottomMargin` / `textsShiftUp` | `70` / `18` | Death-screen layout offsets, pixels. |
| `hideHud` | `false` | Hide HUD on the death screen. |

---

## About Development

I do not know how to code, but I had a clear vision for this mod and really wanted to bring it to life. Because of that, this project was developed entirely with the assistance of **AI**.

If you run into any bugs or unexpected behavior, please submit an issue or leave a comment so it can be fixed!

---

## License

MIT — see [LICENSE](LICENSE).
