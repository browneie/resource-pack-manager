# Resource Pack Manager

A Minecraft 1.8.9 client mod that upgrades the vanilla Resource Packs screen. Made for
[OneClient](https://polyfrost.org)'s 1.8.9 profile, which runs on [Ornithe](https://ornithemc.net) (Fabric for old versions).

## Features

- **Search**: type in the box above the pack list to search every pack, in every folder.
  `Ctrl/Cmd+F` jumps to it; right-click it to clear.
- **Folders**: keep packs in subfolders of `resourcepacks/` (e.g. `resourcepacks/PvP/`) and click into them.
  Packs with the same file name in different folders stay separate.
- **Favorites**: right-click a pack to star it. Starred packs stay at the top of the list.
- **Sorting**: sort available packs A-Z, Z-A, newest or oldest.
- **Profiles**: save your selected packs under a name and load them back later in one click, then press Done.

Favorites and profiles are stored in `config/resourcepackmanager.json`.

## Install

1. Download the jar from [Releases](../../releases).
2. **OneClient:** open your 1.8.9 profile and drag the jar onto the window.
   **Other launchers:** put it in the `mods` folder of a 1.8.9 Ornithe/Fabric instance.

Requires Fabric Loader 0.17 or newer on Ornithe. No other dependencies.

## Building

```bash
./gradlew build
```

The jar is written to `build/libs/`.

## License

[MIT](LICENSE)
