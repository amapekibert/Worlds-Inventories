package org.titago.worldsinventories.data;

import org.titago.worldsinventories.config.AddonConfig.Options;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.List;

public final class PlayerState {
    private static final Attribute MAX_HEALTH_ATTRIBUTE = resolveMaxHealthAttribute();
    private final ItemStack[] inventory;
    private final ItemStack[] armor;
    private final ItemStack[] extra;
    private final ItemStack[] enderChest;
    private final Double health;
    private final Double maxHealth;
    private final Integer food;
    private final Float saturation;
    private final Float exhaustion;
    private final Integer level;
    private final Float exp;
    private final Integer totalExp;
    private final Collection<PotionEffect> potionEffects;
    private final GameMode gameMode;

    private PlayerState(
            final ItemStack[] inventory,
            final ItemStack[] armor,
            final ItemStack[] extra,
            final ItemStack[] enderChest,
            final Double health,
            final Double maxHealth,
            final Integer food,
            final Float saturation,
            final Float exhaustion,
            final Integer level,
            final Float exp,
            final Integer totalExp,
            final Collection<PotionEffect> potionEffects,
            final GameMode gameMode
    ) {
        this.inventory = inventory;
        this.armor = armor;
        this.extra = extra;
        this.enderChest = enderChest;
        this.health = health;
        this.maxHealth = maxHealth;
        this.food = food;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.level = level;
        this.exp = exp;
        this.totalExp = totalExp;
        this.potionEffects = potionEffects;
        this.gameMode = gameMode;
    }

    public static PlayerState capture(final Player player, final Options options) {
        final var inventory = options.separateInventory() ? player.getInventory().getContents() : null;
        final var armor = options.separateInventory() ? player.getInventory().getArmorContents() : null;
        final var extra = options.separateInventory() ? player.getInventory().getExtraContents() : null;
        final var ender = options.separateEnderChest() ? player.getEnderChest().getContents() : null;

        final var health = options.separateHealth() ? player.getHealth() : null;
        final var maxHealth = options.separateHealth() && MAX_HEALTH_ATTRIBUTE != null
                ? player.getAttribute(MAX_HEALTH_ATTRIBUTE).getBaseValue()
                : null;

        final var food = options.separateHunger() ? player.getFoodLevel() : null;
        final var saturation = options.separateHunger() ? player.getSaturation() : null;
        final var exhaustion = options.separateHunger() ? player.getExhaustion() : null;

        final var level = options.separateExperience() ? player.getLevel() : null;
        final var exp = options.separateExperience() ? player.getExp() : null;
        final var totalExp = options.separateExperience() ? player.getTotalExperience() : null;

        final var effects = options.separatePotionEffects()
                ? List.copyOf(player.getActivePotionEffects())
                : null;

        final var gameMode = options.separateGamemode() ? player.getGameMode() : null;

        return new PlayerState(
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
        );
    }

    public static PlayerState fromLoaded(
            final ItemStack[] inventory,
            final ItemStack[] armor,
            final ItemStack[] extra,
            final ItemStack[] enderChest,
            final Double health,
            final Double maxHealth,
            final Integer food,
            final Float saturation,
            final Float exhaustion,
            final Integer level,
            final Float exp,
            final Integer totalExp,
            final Collection<PotionEffect> potionEffects,
            final GameMode gameMode
    ) {
        return new PlayerState(
                inventory,
                armor,
                extra,
                enderChest,
                health,
                maxHealth,
                food,
                saturation,
                exhaustion,
                level,
                exp,
                totalExp,
                potionEffects,
                gameMode
        );
    }

    public void apply(final Player player, final Options options) {
        if (options.separateInventory() && inventory != null) {
            player.getInventory().setContents(inventory);
        }
        if (options.separateInventory() && armor != null) {
            player.getInventory().setArmorContents(armor);
        }
        if (options.separateInventory() && extra != null) {
            player.getInventory().setExtraContents(extra);
        }
        if (options.separateEnderChest() && enderChest != null) {
            player.getEnderChest().setContents(enderChest);
        }

        if (options.separateHealth() && maxHealth != null && health != null) {
            final var attribute = MAX_HEALTH_ATTRIBUTE != null
                    ? player.getAttribute(MAX_HEALTH_ATTRIBUTE)
                    : null;
            if (attribute != null) attribute.setBaseValue(maxHealth);
            final var max = attribute != null ? attribute.getValue() : maxHealth;
            player.setHealth(Math.min(health, max));
        }

        if (options.separateHunger() && food != null && saturation != null && exhaustion != null) {
            player.setFoodLevel(food);
            player.setSaturation(saturation);
            player.setExhaustion(exhaustion);
        }

        if (options.separateExperience() && level != null && exp != null && totalExp != null) {
            player.setLevel(level);
            player.setExp(exp);
            player.setTotalExperience(totalExp);
        }

        if (options.separatePotionEffects() && potionEffects != null) {
            for (final var effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            player.addPotionEffects(potionEffects);
        }

        if (options.separateGamemode() && gameMode != null) {
            player.setGameMode(gameMode);
        }
    }

    public void reset(final Player player, final Options options) {
        if (options.separateInventory()) player.getInventory().clear();
        if (options.separateInventory()) player.getInventory().setArmorContents(new ItemStack[4]);
        if (options.separateInventory()) player.getInventory().setExtraContents(new ItemStack[1]);
        if (options.separateEnderChest()) player.getEnderChest().clear();

        if (options.separateHealth()) {
            final var attribute = MAX_HEALTH_ATTRIBUTE != null
                    ? player.getAttribute(MAX_HEALTH_ATTRIBUTE)
                    : null;
            if (attribute != null) attribute.setBaseValue(20.0d);
            player.setHealth(20.0d);
        }

        if (options.separateHunger()) {
            player.setFoodLevel(20);
            player.setSaturation(5.0f);
            player.setExhaustion(0.0f);
        }

        if (options.separateExperience()) {
            player.setLevel(0);
            player.setExp(0.0f);
            player.setTotalExperience(0);
        }

        if (options.separatePotionEffects()) {
            for (final var effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
        }
    }

    public boolean isComplete(final Options options) {
        if (options.separateInventory() && (inventory == null || armor == null || extra == null)) return false;
        if (options.separateEnderChest() && enderChest == null) return false;
        if (options.separateHealth() && (health == null || maxHealth == null)) return false;
        if (options.separateHunger() && (food == null || saturation == null || exhaustion == null)) return false;
        if (options.separateExperience() && (level == null || exp == null || totalExp == null)) return false;
        if (options.separatePotionEffects() && potionEffects == null) return false;
        if (options.separateGamemode() && gameMode == null) return false;
        return true;
    }

    public ItemStack[] inventory() {
        return inventory;
    }

    public ItemStack[] armor() {
        return armor;
    }

    public ItemStack[] extra() {
        return extra;
    }

    public ItemStack[] enderChest() {
        return enderChest;
    }

    public Double health() {
        return health;
    }

    public Double maxHealth() {
        return maxHealth;
    }

    public Integer food() {
        return food;
    }

    public Float saturation() {
        return saturation;
    }

    public Float exhaustion() {
        return exhaustion;
    }

    public Integer level() {
        return level;
    }

    public Float exp() {
        return exp;
    }

    public Integer totalExp() {
        return totalExp;
    }

    public Collection<PotionEffect> potionEffects() {
        return potionEffects;
    }

    public GameMode gameMode() {
        return gameMode;
    }

    private static Attribute resolveMaxHealthAttribute() {
        final var genericKey = org.bukkit.NamespacedKey.minecraft("generic.max_health");
        final var generic = org.bukkit.Registry.ATTRIBUTE.get(genericKey);
        if (generic != null) return generic;
        return org.bukkit.Registry.ATTRIBUTE.get(org.bukkit.NamespacedKey.minecraft("max_health"));
    }
}
