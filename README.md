# Better Mount Steering

![Showcase](assets/showcase.gif)

Decoupled mount camera and smoother lock-on body turns while mounted. Forge 1.20.1, NeoForge 1.21.1, Fabric 1.20.1, Fabric 1.21.1.

## Loaders

| Loader | Minecraft | Mod version |
|---|---|---|
| Forge | 1.20.1 | 1.0.5 |
| NeoForge | 1.21.1 | 1.0.5 |
| Fabric | 1.20.1 | 1.0.0 |
| Fabric | 1.21.1 | 1.0.5 |

## Features

### All loaders

- Mouse moves the camera separately from the mount in third person; WASD steers the mount toward where you're looking. No Better Third Person required.
- With Shoulder Surfing Reloaded, stops SSR from dragging the camera toward mount steering while decoupled.
- Controllable: right stick moves the decoupled mount camera like the mouse.

### Forge 1.20.1 and NeoForge 1.21.1

- Smooth body turns during Better Lockon mount lock-on (BLO's default 8-way snap replaced with trailing)
- No camera flicker when you release lock-on on a mount
- No body snap back to mount forward on lock-off while sprinting
- Epic Fight TPS camera backs off while mount steering is active; returns on dismount or when you stop steering

## Config

`config/bettermountsteering-client.toml`:

- `mountTurnSpeed` - how fast the body catches the camera while steering (default `0.25`)

Forge and NeoForge also have:

- `smoothLockOnMountTurn` - smooth BLO mount lock-on (default `true`; `false` = vanilla BLO snap)
- `bloLockOnTurnSmoothness` - lock-on turn smoothness (default `0.5`)

## Requires

| Loader | Minecraft | Loader version |
|---|---|---|
| Forge | 1.20.1 | Forge 47+ |
| NeoForge | 1.21.1 | NeoForge 21.1+ |
| Fabric | 1.20.1 | Fabric Loader 0.15+, Fabric API, Forge Config API Port |
| Fabric | 1.21.1 | Fabric Loader 0.16+, Fabric API, Forge Config API Port |

Optional: Better Lockon, Epic Fight, Shoulder Surfing Reloaded, Controllable (varies by loader).

## Install

1. Loader + deps for your MC version.
2. Jar from [releases](https://github.com/Seramicx/better-mount-steering/releases/latest) into `mods/`.

## Building

```
./gradlew :forge-1.20.1:build
./gradlew :neoforge-1.21.1:build
./gradlew :fabric-1.20.1:build
```

Jars under each subproject's `build/libs/`.

## License

MIT
