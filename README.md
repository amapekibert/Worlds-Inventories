# Worlds-Inventories

Worlds-Inventories is an addon for the Worlds plugin that lets you use different player stats per world group. It can separate inventories, health, hunger, experience, potion effects, and gamemode between groups of worlds.

Requires Java 21. See [here](https://docs.papermc.io/misc/java-install) how to update your installed Java version.

## Features
- Per-world (or per-group) player state separation
- Auto-add new worlds to the default group
- Configurable options per group
- Optional debug logging
- Folia/Paper compatible

## Requirements
- Java 21
- Paper/Folia 1.21.x
- [Worlds plugin](https://github.com/TheNextLvl-net/worlds)

## Installation
1. Build the plugin or download the jar.
2. Place the jar into your server `plugins/` folder.
3. Start the server to generate `plugins/WorldsInventories/config.yml`.
4. Edit the config and restart or use the reload command.

## Default config.yml:

```yaml
default_group: default
default_group_per_world: true
auto_add_to_default: true
debug: false

groups:
  default:
    worlds:
      - world
      - world_nether
      - world_the_end
    options:
      inventory: true
      enderchest: true
      health: true
      hunger: true
      experience: true
      potion_effects: true
      gamemode: true
```

### Key options
- `default_group`: Group name used for worlds without a mapping.
- `default_group_per_world`: If true, each world under the default group gets its own sub-group.
- `auto_add_to_default`: Auto-adds new worlds into the default group list.
- `debug`: Enables debug logs.

## Commands
- `/worldsinv reload` - Reloads configuration.
- `/worldsinv debug <on|off>` - Toggle debug logging at runtime.
- `/worldsinv group <list|info|create|delete|addworld|removeworld|set>` - Manage groups and world mappings.

## Permissions
- `worldsinventories.admin` - Access to all admin commands (default: op)
- `worldsinventories.reload` - Reload config
- `worldsinventories.debug` - Toggle debug logs
- `worldsinventories.group` - Manage groups
