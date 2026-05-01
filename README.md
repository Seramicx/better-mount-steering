# Better Mount Steering

Makes riding mounts in third-person feel less janky. Forge 1.20.1.

## What it does

- Mouse moves the camera independently of the mount. WASD steers the mount toward where you're looking. (BTP-style.)
- If you have Better Lockon and lock onto something while mounted, the body smoothly trails the target instead of snapping between 8 directions.
- Fixes the split-second flicker when you release lock-on while mounted.
- Plays nice with Shoulder Surfing Reloaded, Epic Fight TPS mode, and Controllable.

## Config

- `mountTurnSpeed` - how fast the mount turns to follow the camera (default 0.25).
- `bloLockOnTurnSmoothness` - how much trail behind the locked target (default 0.5; lower = more trail).
- `smoothLockOnMountTurn` - turn the lock-on smoothing off if you don't want it.

## Requires

Minecraft 1.20.1, Forge 47+. Better Lockon, Epic Fight, SSR, and Controllable are all optional.

## License

MIT
