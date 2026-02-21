package org.titago.worldsinventories.command;

import org.titago.worldsinventories.WorldsInventories;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class AdminCommand implements CommandExecutor, TabCompleter {
    private final WorldsInventories plugin;

    public AdminCommand(final WorldsInventories plugin) {
        this.plugin = plugin;
    }

    public boolean handleCommand(
            @NotNull final CommandSender sender,
            @NotNull final String label,
            @NotNull final String[] args
    ) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /" + label + " reload|debug|group");
            return true;
        }

        final var sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "reload" -> handleReload(sender, label);
            case "debug" -> handleDebug(sender, label, args);
            case "group" -> handleGroup(sender, label, args);
            default -> {
                sender.sendMessage("Usage: /" + label + " reload|debug|group");
                yield true;
            }
        };
    }

    public List<String> suggest(
            @NotNull final CommandSender sender,
            @NotNull final String[] args
    ) {
        if (args.length == 1) return filterStartsWith(List.of("reload", "debug", "group"), args[0]);
        if (args.length == 2 && "debug".equalsIgnoreCase(args[0])) {
            return filterStartsWith(List.of("on", "off"), args[1]);
        }
        if (args.length == 2 && "group".equalsIgnoreCase(args[0])) {
            return filterStartsWith(List.of("list", "info", "create", "delete", "addworld", "removeworld", "set"), args[1]);
        }
        if (args.length == 3 && "group".equalsIgnoreCase(args[0])) {
            return filterStartsWith(getGroupNames(), args[2]);
        }
        return List.of();
    }

    @Override
    public boolean onCommand(
            @NotNull final CommandSender sender,
            @NotNull final Command command,
            @NotNull final String label,
            @NotNull final String[] args
    ) {
        return handleCommand(sender, label, args);
    }

    private boolean handleDebug(final CommandSender sender, final String label, final String[] args) {
        if (!sender.hasPermission("worldsinventories.debug")) {
            sender.sendMessage("You do not have permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /" + label + " debug <on|off>");
            return true;
        }
        final var value = args[1].toLowerCase(Locale.ROOT);
        final var enabled = "on".equals(value) || "true".equals(value);
        plugin.setDebugEnabled(enabled);
        sender.sendMessage("WorldsInventories debug " + (enabled ? "enabled" : "disabled") + ".");
        plugin.getLogger().info("Debug set to " + enabled + " by " + sender.getName());
        return true;
    }

    private boolean handleReload(final CommandSender sender, final String label) {
        if (!sender.hasPermission("worldsinventories.reload")) {
            sender.sendMessage("You do not have permission.");
            return true;
        }
        plugin.reloadAddonConfig();
        sender.sendMessage("WorldsInventories reloaded.");
        plugin.getLogger().info("Config reloaded by " + sender.getName());
        return true;
    }

    private boolean handleGroup(final CommandSender sender, final String label, final String[] args) {
        if (!sender.hasPermission("worldsinventories.group")) {
            sender.sendMessage("You do not have permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /" + label + " group <list|info|create|delete|addworld|removeworld|set>");
            return true;
        }

        final var action = args[1].toLowerCase(Locale.ROOT);
        return switch (action) {
            case "list" -> {
                final var groups = getGroupNames();
                sender.sendMessage("Groups: " + String.join(", ", groups));
                yield true;
            }
            case "info" -> {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /" + label + " group info <name>");
                    yield true;
                }
                final var name = args[2];
                final var section = getGroupSection(name);
                if (section == null) {
                    sender.sendMessage("Group not found: " + name);
                    yield true;
                }
                final var worlds = section.getStringList("worlds");
                sender.sendMessage("Group " + name + " worlds: " + String.join(", ", worlds));
                yield true;
            }
            case "create" -> {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /" + label + " group create <name>");
                    yield true;
                }
                final var name = args[2];
                if (getGroupSection(name) != null) {
                    sender.sendMessage("Group already exists: " + name);
                    yield true;
                }
                final var groups = plugin.getConfig().getConfigurationSection("groups");
                final var created = groups != null ? groups.createSection(name) : null;
                if (created == null) {
                    sender.sendMessage("Failed to create group: " + name);
                    yield true;
                }
                created.set("worlds", List.of());
                plugin.saveAndReloadConfig();
                sender.sendMessage("Group created: " + name);
                yield true;
            }
            case "delete" -> {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /" + label + " group delete <name>");
                    yield true;
                }
                final var name = args[2];
                final var groups = plugin.getConfig().getConfigurationSection("groups");
                if (groups == null || !groups.isConfigurationSection(name)) {
                    sender.sendMessage("Group not found: " + name);
                    yield true;
                }
                groups.set(name, null);
                plugin.saveAndReloadConfig();
                sender.sendMessage("Group deleted: " + name);
                yield true;
            }
            case "addworld" -> {
                if (args.length < 4) {
                    sender.sendMessage("Usage: /" + label + " group addworld <group> <world>");
                    yield true;
                }
                final var group = args[2];
                final var world = args[3];
                final var section = getOrCreateGroup(group);
                final var worlds = new ArrayList<>(section.getStringList("worlds"));
                if (!worlds.contains(world)) worlds.add(world);
                section.set("worlds", worlds);
                plugin.saveAndReloadConfig();
                sender.sendMessage("Added world " + world + " to group " + group);
                yield true;
            }
            case "removeworld" -> {
                if (args.length < 4) {
                    sender.sendMessage("Usage: /" + label + " group removeworld <group> <world>");
                    yield true;
                }
                final var group = args[2];
                final var world = args[3];
                final var section = getGroupSection(group);
                if (section == null) {
                    sender.sendMessage("Group not found: " + group);
                    yield true;
                }
                final var worlds = new ArrayList<>(section.getStringList("worlds"));
                worlds.remove(world);
                section.set("worlds", worlds);
                plugin.saveAndReloadConfig();
                sender.sendMessage("Removed world " + world + " from group " + group);
                yield true;
            }
            case "set" -> {
                if (args.length < 4) {
                    sender.sendMessage("Usage: /" + label + " group set <world> <group>");
                    yield true;
                }
                final var world = args[2];
                final var group = args[3];
                final var groupsSection = plugin.getConfig().getConfigurationSection("groups");
                if (groupsSection == null) {
                    sender.sendMessage("No groups configured.");
                    yield true;
                }
                for (final var name : groupsSection.getKeys(false)) {
                    final var section = groupsSection.getConfigurationSection(name);
                    if (section == null) continue;
                    final var worlds = new ArrayList<>(section.getStringList("worlds"));
                    worlds.remove(world);
                    section.set("worlds", worlds);
                }
                final var target = getOrCreateGroup(group);
                final var targetWorlds = new ArrayList<>(target.getStringList("worlds"));
                if (!targetWorlds.contains(world)) targetWorlds.add(world);
                target.set("worlds", targetWorlds);
                plugin.saveAndReloadConfig();
                sender.sendMessage("World " + world + " set to group " + group);
                yield true;
            }
            default -> {
                sender.sendMessage("Usage: /" + label + " group <list|info|create|delete|addworld|removeworld|set>");
                yield true;
            }
        };
    }

    private List<String> getGroupNames() {
        final var groups = plugin.getConfig().getConfigurationSection("groups");
        if (groups == null) return List.of();
        return groups.getKeys(false).stream().sorted().collect(Collectors.toList());
    }

    private ConfigurationSection getGroupSection(final String name) {
        final var groups = plugin.getConfig().getConfigurationSection("groups");
        if (groups == null) return null;
        return groups.getConfigurationSection(name);
    }

    private ConfigurationSection getOrCreateGroup(final String name) {
        final var groups = plugin.getConfig().getConfigurationSection("groups");
        if (groups != null) {
            final var existing = groups.getConfigurationSection(name);
            if (existing != null) return existing;
            return groups.createSection(name);
        }
        final var createdGroups = plugin.getConfig().createSection("groups");
        return createdGroups.createSection(name);
    }

    @Override
    public List<String> onTabComplete(
            @NotNull final CommandSender sender,
            @NotNull final Command command,
            @NotNull final String alias,
            @NotNull final String[] args
    ) {
        return suggest(sender, args);
    }

    private List<String> filterStartsWith(final List<String> items, final String token) {
        final var lower = token.toLowerCase(Locale.ROOT);
        return items.stream()
                .filter(item -> item.toLowerCase(Locale.ROOT).startsWith(lower))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }
}
