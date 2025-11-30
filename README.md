# Tool Switcher
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/3CTNwVgW?style=for-the-badge&logo=modrinth&color=%2300AF5C)](https://modrinth.com/mod/3CTNwVgW) [![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/maganoos/tool-switcher/total?style=for-the-badge&logo=github&color=whitesmoke)](https://github.com/maganoos/tool-switcher) ![Modrinth Version](https://img.shields.io/modrinth/v/3CTNwVgW?style=for-the-badge&logo=semver)<br>
Tool Switcher is a simple utility mod that enables you to automatically switch tools depending on what block you are trying to break.
### Features
- Automatically selects the optimal tool for mining, taking into account factors such as efficiency.
- Disables itself while you are sneaking, if enabled.
- Optionally prevents switching if the current tool has Silk Touch or Fortune.
- Returns to the previously selected hotbar slot after mining if enabled.
- Fully customizable list of disabled blocks and tools.

### Config
| Option                       | Type       | Default | Description                                                               |
|------------------------------|------------|---------|---------------------------------------------------------------------------|
| `Mod Enabled`                | true/false | `true`  | Enable/disable the mod. Can be toggled with a keybind.                    |
| `Show Message`               | true/false | `true`  | Whether to show a message on the screen when keybind is pressed.          |
| `Turn Off when Sneaking`     | true/false | `true`  | Whether Tool Switcher remains active while sneaking.                      |
| `Go Back to Previous Slot`   | true/false | `true`  | Whether to switch back to the slot you were on before mining.             |
| `Respect Silk Touch/Fortune` | true/false | `true`  | Whether to not switch tools if you are holding a silk touch/fortune tool. |
| `Disabled Tools`             | List       | `empty` | Tools that don't get switched to.                                         |
| `Disabled Blocks`            | List       | `empty` | Blocks that don't trigger tool switcher.                                  |
<footer>Tip: the last two have to be put in as minecraft's raw id format, e.g. <code>minecraft:diamond_pickaxe</code> or <code>minecraft:dirt</code></footer>

### Required/Recommended mods
- [Fabric API](https://modrinth.com/mod/fabric-api) (Required)
- [MidnightLib](https://modrinth.com/mod/midnightlib) (Required)
- [Mod Menu](https://modrinth.com/mod/modmenu) (Recommended)

### Issues/Feature Requests
Always welcome. Please submit an issue to the repo: https://github.com/Maganoos/tool-switcher/issues
#### Forge/Neoforge?
Don't make me steal your kneecaps. ([Automatic Tool Swap](https://modrinth.com/mod/automatic-tool-swap))