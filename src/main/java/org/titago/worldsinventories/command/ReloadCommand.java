package org.titago.worldsinventories.command;

import org.titago.worldsinventories.WorldsInventories;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public final class ReloadCommand implements CommandExecutor, TabCompleter {
    private final WorldsInventories plugin;

    public ReloadCommand(final WorldsInventories plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull final CommandSender sender,
            @NotNull final Command command,
            @NotNull final String label,
            @NotNull final String[] args
    ) {
        if (args.length == 1 && "reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("worldsinventories.reload")) {
                sender.sendMessage("You do not have permission.");
                return true;
            }
            plugin.reloadAddonConfig();
            sender.sendMessage("WorldsInventories reloaded.");
            plugin.getLogger().info("Config reloaded by " + sender.getName());
            return true;
        }

        sender.sendMessage("Usage: /" + label + " reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(
            @NotNull final CommandSender sender,
            @NotNull final Command command,
            @NotNull final String alias,
            @NotNull final String[] args
    ) {
        if (args.length == 1) return List.of("reload");
        return List.of();
    }
}
