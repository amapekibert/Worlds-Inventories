package org.titago.worldsinventories.data;

import org.titago.worldsinventories.config.AddonConfig.Options;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PlayerStateStore {
    private final JavaPlugin plugin;
    private final File dataFolder;

    public PlayerStateStore(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create data folder");
        }
    }

    public void saveCurrent(final Player player, final String group, final Options options) {
        final var state = PlayerState.capture(player, options);
        save(player.getUniqueId(), group, state);
    }

    public void save(final UUID uuid, final String group, final PlayerState state) {
        final var file = playerFile(uuid);
        final var config = YamlConfiguration.loadConfiguration(file);
        final var section = config.createSection("groups." + group);

        if (state.inventory() != null) section.set("inventory", state.inventory());
        if (state.armor() != null) section.set("armor", state.armor());
        if (state.extra() != null) section.set("extra", state.extra());
        if (state.enderChest() != null) section.set("ender_chest", state.enderChest());
        if (state.health() != null) section.set("health", state.health());
        if (state.maxHealth() != null) section.set("max_health", state.maxHealth());
        if (state.food() != null) section.set("food", state.food());
        if (state.saturation() != null) section.set("saturation", state.saturation());
        if (state.exhaustion() != null) section.set("exhaustion", state.exhaustion());
        if (state.level() != null) section.set("level", state.level());
        if (state.exp() != null) section.set("exp", state.exp());
        if (state.totalExp() != null) section.set("total_exp", state.totalExp());
        if (state.potionEffects() != null) section.set("potion_effects", state.potionEffects());
        if (state.gameMode() != null) section.set("gamemode", state.gameMode().name());

        try {
            config.save(file);
        } catch (final IOException e) {
            plugin.getLogger().warning("Failed to save data for " + uuid + ": " + e.getMessage());
        }
    }

    public Optional<PlayerState> load(final UUID uuid, final String group) {
        final var file = playerFile(uuid);
        if (!file.exists()) return Optional.empty();

        final var config = YamlConfiguration.loadConfiguration(file);
        final var section = config.getConfigurationSection("groups." + group);
        if (section == null) return Optional.empty();

        final var inventory = readItemStackArray(section, "inventory");
        final var armor = readItemStackArray(section, "armor");
        final var extra = readItemStackArray(section, "extra");
        final var ender = readItemStackArray(section, "ender_chest");

        final var health = section.contains("health") ? section.getDouble("health") : null;
        final var maxHealth = section.contains("max_health") ? section.getDouble("max_health") : null;

        final var food = section.contains("food") ? section.getInt("food") : null;
        final var saturation = section.contains("saturation") ? (float) section.getDouble("saturation") : null;
        final var exhaustion = section.contains("exhaustion") ? (float) section.getDouble("exhaustion") : null;

        final var level = section.contains("level") ? section.getInt("level") : null;
        final var exp = section.contains("exp") ? (float) section.getDouble("exp") : null;
        final var totalExp = section.contains("total_exp") ? section.getInt("total_exp") : null;

        final var effects = readPotionEffects(section, "potion_effects");

        final var gameMode = section.contains("gamemode")
                ? org.bukkit.GameMode.valueOf(section.getString("gamemode", "SURVIVAL"))
                : null;

        return Optional.of(PlayerState.fromLoaded(
                inventory,
                armor,
                extra,
                ender,
                health,
                maxHealth,
                food,
                saturation,
                exhaustion,
                level,
                exp,
                totalExp,
                effects,
                gameMode
        ));
    }

    private File playerFile(final UUID uuid) {
        return new File(dataFolder, uuid + ".yml");
    }

    private ItemStack[] readItemStackArray(final ConfigurationSection section, final String key) {
        final var list = section.getList(key);
        if (list == null) return null;
        final var items = new ItemStack[list.size()];
        for (int i = 0; i < list.size(); i++) {
            items[i] = (ItemStack) list.get(i);
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    private List<PotionEffect> readPotionEffects(final ConfigurationSection section, final String key) {
        final var list = section.getList(key);
        if (list == null) return null;
        return (List<PotionEffect>) list;
    }
}
