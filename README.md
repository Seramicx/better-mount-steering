# Seramicx's Better Mount Steering

Decoupled mount camera and smooth body trail for Better Lockon mount lock-on, on Forge 1.20.1.

## Features

- Decoupled mount camera. While riding a horse, pig, or strider in third-person, the mouse moves the camera independently of the mount. WASD makes the body lerp toward the camera direction, so the mount steers there. BTP-style.
- Smooth body trail during BLO mount lock-on. Better Lockon's mount lock-on writes `player.yRot` instantly to (target direction - sprint offset) every tick (8 discrete sprint directions, no smoothing), which feels rigid. This replaces that with a proportional ease-out lerp so the body smoothly trails the locked target instead of snapping to it.
- Lock-off flicker fix. BLO holds its lock-on camera transform for a beat after release (`BLOCameraSetting.transitionTick` + `EpicFightCameraAPI.blo$unlockDelayTick`), which gave a split-second flicker on a mount. Force-skips both timers via reflection on the lock-off edge so BLO drops the camera immediately.
- SSR coexistence. While mount-rotate is active, suppresses SSR's `followPlayerRotations` lerp (which would otherwise drag the camera toward the body's steering direction whenever you stop mouse-turning).
- Epic Fight coexistence. Cancels EF's TPS camera mode while mount-rotate decouple is active. Outside mount-rotate, EF's TPS mode is untouched.
- Controllable support. Right-stick input drives the decoupled camera too.

## Requirements

- Minecraft 1.20.1 + Forge 47+

## Optional companions

- [Better Lockon](https://www.curseforge.com/minecraft/mc-mods/better-lockon) - mount lock-on smoothing only runs when BLO is loaded and you're locked on.
- [Epic Fight](https://www.curseforge.com/minecraft/mc-mods/epic-fight) - TPS-mode coordination.
- [Shoulder Surfing Reloaded](https://www.curseforge.com/minecraft/mc-mods/shoulder-surfing-reloaded) - if loaded, mount-rotate reads SSR's camera yaw as the source of truth instead of `player.yRot`.
- [Controllable](https://www.curseforge.com/minecraft/mc-mods/controllable) - right-stick camera input.

## Config

`config/bettermountsteering-client.toml`:

- `mountTurnSpeed` - per-tick turn factor for the decoupled mount-rotate body lerp (default 0.25). Lower = smoother / slower.
- `smoothLockOnMountTurn` - whether to smooth BLO's mount lock-on instead of letting it snap (default true).
- `bloLockOnTurnSmoothness` - per-tick lerp factor for body yaw during BLO + mount lock-on smoothing. Higher = quicker / less trail. 0.10 is very smooth, 0.50 is balanced (default), 0.85 is responsive, 1.0 is no smoothing (BLO's snap passes through).

## License

MIT
