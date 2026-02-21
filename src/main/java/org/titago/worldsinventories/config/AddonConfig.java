package org.titago.worldsinventories.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record AddonConfig(
        GroupResolver groupResolver,
        java.util.Map<String, GroupConfig> groups,
        GeneralOptions general
) {
    public static AddonConfig from(final FileConfiguration config) {
        final var defaultGroup = config.getString("default_group", "default");
        final var defaultGroupPerWorld = config.getBoolean("default_group_per_world", true);
        final var autoAddToDefault = config.getBoolean("auto_add_to_default", true);
        final var debug = config.getBoolean("debug", false);

        final var groups = new HashMap<String, Set<String>>();
        final var groupConfigs = new HashMap<String, GroupConfig>();
        final var groupsSection = config.getConfigurationSection("groups");

        var defaultConfig = GroupConfig.hardDefaults();
        if (groupsSection != null) {
            final var defaultSection = groupsSection.getConfigurationSection(defaultGroup);
            if (defaultSection != null) defaultConfig = GroupConfig.from(defaultSection, defaultConfig);

            for (final var groupName : groupsSection.getKeys(false)) {
                final var section = groupsSection.getConfigurationSection(groupName);
                if (section == null) continue;
                final var worlds = new HashSet<>(section.getStringList("worlds"));
                groups.put(groupName, worlds);
                groupConfigs.put(groupName, GroupConfig.from(section, defaultConfig));
            }
        }

        final var resolver = new GroupResolver(groups, defaultGroup, defaultGroupPerWorld);
        final var general = new GeneralOptions(debug, defaultGroupPerWorld, autoAddToDefault);
        return new AddonConfig(resolver, Collections.unmodifiableMap(groupConfigs), general);
    }

    private static boolean readBoolean(final ConfigurationSection section, final String path, final boolean def) {
        if (section == null) return def;
        return section.getBoolean(path, def);
    }

    public GroupConfig groupConfig(final String groupName) {
        final var config = groups.get(groupName);
        if (config != null) return config;
        final var defaultGroup = groupResolver.defaultGroup();
        if (groupName.startsWith(defaultGroup + ":")) {
            final var fallback = groups.get(defaultGroup);
            if (fallback != null) return fallback;
        }
        return GroupConfig.hardDefaults();
    }

    public GroupConfig groupConfigForWorld(final String worldName) {
        return groupConfig(groupResolver.groupForWorld(worldName));
    }

    public static final class GroupResolver {
        private final Map<String, String> worldToGroup;
        private final String defaultGroup;
        private final boolean defaultGroupPerWorld;

        public GroupResolver(
                final Map<String, Set<String>> groups,
                final String defaultGroup,
                final boolean defaultGroupPerWorld
        ) {
            this.defaultGroup = defaultGroup;
            this.defaultGroupPerWorld = defaultGroupPerWorld;
            final var mapping = new HashMap<String, String>();
            for (final var entry : groups.entrySet()) {
                for (final var world : entry.getValue()) {
                    mapping.put(world, entry.getKey());
                }
            }
            this.worldToGroup = Collections.unmodifiableMap(mapping);
        }

        public String groupForWorld(final String worldName) {
            final var mapped = worldToGroup.get(worldName);
            if (mapped != null) {
                if (defaultGroupPerWorld && mapped.equals(defaultGroup)) return defaultGroup + ":" + worldName;
                return mapped;
            }
            return defaultGroupPerWorld ? defaultGroup + ":" + worldName : defaultGroup;
        }

        public String defaultGroup() {
            return defaultGroup;
        }

        public boolean defaultGroupPerWorld() {
            return defaultGroupPerWorld;
        }

        public Map<String, String> worldToGroup() {
            return worldToGroup;
        }
    }

    public record GeneralOptions(boolean debug, boolean defaultGroupPerWorld, boolean autoAddToDefault) {}

    public record Options(
            boolean separateInventory,
            boolean separateEnderChest,
            boolean separateHealth,
            boolean separateHunger,
            boolean separateExperience,
            boolean separatePotionEffects,
            boolean separateGamemode
    ) {}

    public record GroupConfig(Options options) {
        public static GroupConfig from(final ConfigurationSection section, final GroupConfig defaults) {
            final var optionsSection = section.getConfigurationSection("options");
            final var options = new Options(
                    readBoolean(optionsSection, "inventory", defaults.options.separateInventory()),
                    readBoolean(optionsSection, "enderchest", defaults.options.separateEnderChest()),
                    readBoolean(optionsSection, "health", defaults.options.separateHealth()),
                    readBoolean(optionsSection, "hunger", defaults.options.separateHunger()),
                    readBoolean(optionsSection, "experience", defaults.options.separateExperience()),
                    readBoolean(optionsSection, "potion_effects", defaults.options.separatePotionEffects()),
                    readBoolean(optionsSection, "gamemode", defaults.options.separateGamemode())
            );
            return new GroupConfig(options);
        }

        public static GroupConfig hardDefaults() {
            return new GroupConfig(new Options(true, true, true, true, true, true, true));
        }
    }
}
