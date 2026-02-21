package org.titago.worldsinventories.listener;

import org.titago.worldsinventories.WorldsInventories;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerStateListener implements Listener {
    private final WorldsInventories plugin;
    private final Map<UUID, String> currentGroup = new HashMap<>();
    private final Map<UUID, String> pendingTeleportFrom = new HashMap<>();
    private final Map<UUID, String> lastWorldName = new HashMap<>();
    private final Map<UUID, io.papermc.paper.threadedregions.scheduler.ScheduledTask> pollTasks = new HashMap<>();

    public PlayerStateListener(final WorldsInventories plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        plugin.debugLog("Join " + event.getPlayer().getName() + " world=" + event.getPlayer().getWorld().getName());
        lastWorldName.put(event.getPlayer().getUniqueId(), event.getPlayer().getWorld().getName());
        startPolling(event.getPlayer());
        handleSwitch(event.getPlayer(), null);
    }

    public void startPolling(final org.bukkit.entity.Player player) {
        stopPolling(player.getUniqueId());
        lastWorldName.put(player.getUniqueId(), player.getWorld().getName());
        final var task = player.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (!player.isOnline()) return;
            final var currentWorld = player.getWorld().getName();
            final var lastWorld = lastWorldName.get(player.getUniqueId());
            if (lastWorld != null && lastWorld.equals(currentWorld)) return;
            plugin.debugLog("[Debug] Poll world change " + player.getName()
                    + " " + lastWorld + " -> " + currentWorld);
            handleSwitch(player, lastWorld);
            lastWorldName.put(player.getUniqueId(), currentWorld);
        }, () -> {
        }, 1L, 5L);
        pollTasks.put(player.getUniqueId(), task);
    }

    private void stopPolling(final UUID uuid) {
        final var task = pollTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(final PlayerChangedWorldEvent event) {
        plugin.debugLog("[Debug] PlayerChangedWorldEvent " + event.getPlayer().getName()
                + " from=" + event.getFrom().getName()
                + " to=" + event.getPlayer().getWorld().getName());
        final var pending = pendingTeleportFrom.remove(event.getPlayer().getUniqueId());
        if (pending != null && pending.equals(event.getFrom().getName())) {
            plugin.debugLog("Skip PlayerChangedWorldEvent for " + event.getPlayer().getName());
            return;
        }
        handleSwitch(event.getPlayer(), event.getFrom().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(final PlayerTeleportEvent event) {
        final var from = event.getFrom().getWorld();
        final var to = event.getTo() != null ? event.getTo().getWorld() : null;
        if (from == null || to == null || from.equals(to)) return;
        final var player = event.getPlayer();
        final var fromName = from.getName();
        final var toName = to.getName();
        pendingTeleportFrom.put(player.getUniqueId(), fromName);
        plugin.debugLog("[Debug] PlayerTeleportEvent " + player.getName()
                + " " + fromName + " -> " + toName + " cause=" + event.getCause());
        plugin.debugLog("Teleport " + player.getName() + " " + fromName + " -> " + toName);

        player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) return;
            if (!player.getWorld().getName().equals(toName)) return;
            handleSwitch(player, fromName);
        }, () -> {
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(final org.bukkit.event.player.PlayerMoveEvent event) {
        final var player = event.getPlayer();
        final var fromWorld = event.getFrom().getWorld();
        final var toWorld = event.getTo() != null ? event.getTo().getWorld() : null;
        if (fromWorld == null || toWorld == null || fromWorld.equals(toWorld)) return;
        final var last = lastWorldName.get(player.getUniqueId());
        if (last != null && last.equals(toWorld.getName())) return;
        plugin.debugLog("[Debug] PlayerMoveEvent world change " + player.getName()
                + " " + fromWorld.getName() + " -> " + toWorld.getName());
        handleSwitch(player, fromWorld.getName());
        lastWorldName.put(player.getUniqueId(), toWorld.getName());
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        final var player = event.getPlayer();
        final var group = currentGroup.getOrDefault(
                player.getUniqueId(),
                plugin.addonConfig().groupResolver().groupForWorld(player.getWorld().getName())
        );
        final var groupConfig = plugin.addonConfig().groupConfig(group);
        plugin.stateStore().saveCurrent(player, group, groupConfig.options());
        currentGroup.remove(player.getUniqueId());
        lastWorldName.remove(player.getUniqueId());
        pendingTeleportFrom.remove(player.getUniqueId());
        stopPolling(player.getUniqueId());
    }

    private void handleSwitch(final org.bukkit.entity.Player player, final String fromWorld) {
        final var resolver = plugin.addonConfig().groupResolver();

        final var newGroup = resolver.groupForWorld(player.getWorld().getName());
        final var oldGroup = fromWorld == null
                ? currentGroup.get(player.getUniqueId())
                : resolver.groupForWorld(fromWorld);

        if (oldGroup != null && oldGroup.equals(newGroup)) {
            currentGroup.put(player.getUniqueId(), newGroup);
            return;
        }

        final var newConfig = plugin.addonConfig().groupConfig(newGroup);
        plugin.log("Switch " + player.getName() + " " + oldGroup + " -> " + newGroup);

        if (oldGroup != null) {
            final var oldConfig = plugin.addonConfig().groupConfig(oldGroup);
            plugin.stateStore().saveCurrent(player, oldGroup, oldConfig.options());
            plugin.debugLog("Saved state for " + player.getName() + " in group " + oldGroup);
        }

        final var loaded = plugin.stateStore().load(player.getUniqueId(), newGroup);
        if (loaded.isPresent() && loaded.get().isComplete(newConfig.options())) {
            loaded.get().apply(player, newConfig.options());
            plugin.debugLog("Loaded state for " + player.getName() + " in group " + newGroup);
        } else {
            final var state = org.titago.worldsinventories.data.PlayerState.capture(player, newConfig.options());
            state.reset(player, newConfig.options());
            plugin.stateStore().save(player.getUniqueId(), newGroup, org.titago.worldsinventories.data.PlayerState.capture(player, newConfig.options()));
            plugin.debugLog("Reset state for " + player.getName() + " in group " + newGroup);
        }

        currentGroup.put(player.getUniqueId(), newGroup);
    }
}
