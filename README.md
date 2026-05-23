# Better Mount Steering

![Showcase](assets/showcase.gif)

Decoupled mount-camera steering and smooth lock-on body rotation for mounted combat. Available for Forge 1.20.1, NeoForge 1.21.1, and Fabric 1.20.1.

## Features

### All loaders

- Decoupled mount camera. While riding a horse, pig, or strider in third person, the mouse moves the camera independently of the mount, and pressing WASD smoothly steers the mount toward the camera direction. Similar to Better Third Person, but without needing it installed.
- Shoulder Surfing Reloaded support. While mount-rotate is active, suppresses SSR's `followPlayerRotations` lerp so the camera doesn't drag toward the mount's steering direction.
- Controllable support. The right stick drives the decoupled mount camera the same way the mouse does.

### Forge 1.20.1 only

- Smooth body rotation during Better Lockon mount lock-on. By default, BLO's mount lock-on snaps your body between 8 fixed sprint directions every tick, which feels rigid on a horse. This replaces the snap with a proportional ease-out lerp so the body smoothly trails the locked target.
- Lock-off flicker fix. BLO holds onto its lock-on camera transform for a few ticks after you release lock-on, which produces a brief visible flicker when you're mounted. This forces BLO to release the camera immediately, so lock-off is clean.
- Epic Fight TPS mode support. Cancels EF's TPS camera while mount-rotate is decoupled, then hands control back when you dismount or stop steering.

### NeoForge 1.21.1

- Epic Fight TPS mode support (same as Forge).

## Config

`config/bettermountsteering-client.toml`:

- `mountTurnSpeed` - per-tick body lerp factor while mount-rotating (default `0.25`). Lower = smoother / slower body turn. Higher = snappier.

Forge 1.20.1 also exposes:

- `smoothLockOnMountTurn` - whether to smooth Better Lockon's mount lock-on instead of letting it snap (default `true`). Set to `false` if you want BLO's vanilla snap behavior back.
- `bloLockOnTurnSmoothness` - per-tick lerp factor for body yaw during BLO + mount lock-on (default `0.5`). `0.10` = very smooth with a long trail behind the target. `0.5` = balanced. `0.85` = responsive. `1.0` = no smoothing (BLO's snap passes through unchanged).

## Requires

| Loader   | Minecraft | Loader version |
|----------|-----------|----------------|
| Forge    | 1.20.1    | Forge 47+      |
| NeoForge | 1.21.1    | NeoForge 21.1+ |
| Fabric   | 1.20.1    | Fabric Loader 0.15+, Fabric API, Forge Config API Port |

Optional and auto-detected:

- Forge 1.20.1: Better Lockon, Epic Fight, Shoulder Surfing Reloaded, Controllable.
- NeoForge 1.21.1: Epic Fight, Shoulder Surfing Reloaded, Controllable.
- Fabric 1.20.1: Shoulder Surfing Reloaded, Controllable.

## Manual install

1. Install your loader for the matching Minecraft version.
2. Download the matching jar from the [latest release](https://github.com/Seramicx/better-mount-steering/releases/latest).
3. Drop it into your `.minecraft/mods/` folder. Fabric users also need Fabric API and Forge Config API Port.

## Building from source

The repo is a multiloader monorepo with shared sources under `common/`. Each loader has its own subproject.

```
./gradlew :forge-1.20.1:build
./gradlew :neoforge-1.21.1:build
./gradlew :fabric-1.20.1:build
```

Jars land in `<loader>/build/libs/`.

## License

MIT
