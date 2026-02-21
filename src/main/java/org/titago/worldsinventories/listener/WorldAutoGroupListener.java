package org.titago.worldsinventories.listener;

import org.titago.worldsinventories.WorldsInventories;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import java.util.ArrayList;

public final class WorldAutoGroupListener implements Listener {
    private final WorldsInventories plugin;

    public WorldAutoGroupListener(final WorldsInventories plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(final WorldLoadEvent event) {
        final var general = plugin.addonConfig().general();
        final var resolver = plugin.addonConfig().groupResolver();
        if (!general.autoAddToDefault()) return;

        final var worldName = event.getWorld().getName();
        if (resolver.worldToGroup().containsKey(worldName)) return;

        final var groups = plugin.getConfig().getConfigurationSection("groups");
        if (groups == null) return;
        final var defaultGroup = resolver.defaultGroup();
        final var section = groups.getConfigurationSection(defaultGroup);
        if (section == null) return;

        final var worlds = new ArrayList<>(section.getStringList("worlds"));
        if (worlds.contains(worldName)) return;
        worlds.add(worldName);
        section.set("worlds", worlds);

        plugin.saveAndReloadConfig();
        plugin.log("Added world " + worldName + " to group " + defaultGroup);
    }
}
