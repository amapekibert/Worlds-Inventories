package org.titago.worldsinventories;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.titago.worldsinventories.command.AdminCommand;
import org.titago.worldsinventories.config.AddonConfig;
import org.titago.worldsinventories.data.PlayerStateStore;
import org.titago.worldsinventories.listener.PlayerStateListener;
import org.titago.worldsinventories.listener.WorldAutoGroupListener;
import java.util.Collection;

public class WorldsInventories extends JavaPlugin {
    private AddonConfig addonConfig;
    private PlayerStateStore stateStore;
    private boolean debugEnabled;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadAddonConfig();
        stateStore = new PlayerStateStore(this);

        final var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerStateListener(this), this);
        pluginManager.registerEvents(new WorldAutoGroupListener(this), this);

        final var adminCommand = new AdminCommand(this);
        registerCommand("worldsinv", new BasicCommand() {
            @Override
            public void execute(final CommandSourceStack source, final String[] args) {
                final CommandSender sender = source.getSender();
                adminCommand.handleCommand(sender, "worldsinv", args);
            }

            @Override
            public Collection<String> suggest(final CommandSourceStack source, final String[] args) {
                return adminCommand.suggest(source.getSender(), args);
            }
        });
    }

    public void reloadAddonConfig() {
        reloadConfig();
        addonConfig = AddonConfig.from(getConfig());
    }

    public void saveAndReloadConfig() {
        saveConfig();
        reloadAddonConfig();
    }

    public AddonConfig addonConfig() {
        return addonConfig;
    }

    public PlayerStateStore stateStore() {
        return stateStore;
    }

    public void setDebugEnabled(final boolean enabled) {
        this.debugEnabled = enabled;
    }

    public void log(final String message) {
        getLogger().info(message);
    }

    public void debugLog(final String message) {
        if (debugEnabled || (addonConfig != null && addonConfig.general().debug())) {
            getLogger().info(message);
        }
    }
}
