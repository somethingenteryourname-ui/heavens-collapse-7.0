package com.heavenscollapse;

import com.heavenscollapse.commands.HeavensCollapseCommand;
import com.heavenscollapse.listeners.AnvilProtectionListener;
import com.heavenscollapse.listeners.CombatListener;
import com.heavenscollapse.listeners.ItemSwitchListener;
import com.heavenscollapse.listeners.LightningObtainListener;
import com.heavenscollapse.listeners.PlayerQuitListener;
import com.heavenscollapse.util.AbilityManager;
import com.heavenscollapse.util.EffectsUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Main entry point for the Heaven's Collapse plugin.
 *
 * <p>Wires together the item manager, hit-counter (ability) manager,
 * cinematic effects helper, commands and listeners, and exposes the
 * loaded configuration values to the rest of the plugin.</p>
 */
public final class HeavensCollapsePlugin extends JavaPlugin {

    private HeavensCollapseItem itemManager;
    private AbilityManager abilityManager;
    private EffectsUtil effectsUtil;

    private final Map<String, String> messages = new HashMap<>();

    private int hitsRequired;
    private long idleResetMillis;

    private boolean lightningEnabled;
    private boolean lightningDamage;
    private boolean lightningFire;

    private boolean particlesEnabled;
    private boolean soundsEnabled;

    private boolean lightningObtainEnabled;
    private boolean negateLightningDamage;

    private boolean debugActionbarEnabled;

    private boolean resetOnItemSwitchEnabled;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        this.itemManager = new HeavensCollapseItem(this);
        this.abilityManager = new AbilityManager(hitsRequired, idleResetMillis);
        this.effectsUtil = new EffectsUtil(this);

        getServer().getPluginManager().registerEvents(
                new CombatListener(this, itemManager, abilityManager, effectsUtil), this);
        getServer().getPluginManager().registerEvents(
                new LightningObtainListener(this, itemManager), this);
        getServer().getPluginManager().registerEvents(
                new PlayerQuitListener(abilityManager), this);
        getServer().getPluginManager().registerEvents(
                new ItemSwitchListener(this, itemManager, abilityManager), this);
        getServer().getPluginManager().registerEvents(
                new AnvilProtectionListener(itemManager), this);

        HeavensCollapseCommand command = new HeavensCollapseCommand(this, itemManager);
        PluginCommand pluginCommand = getCommand("heavenscollapse");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        } else {
            getLogger().warning("Could not register /heavenscollapse command - check plugin.yml.");
        }

        getLogger().info("Heaven's Collapse has awakened.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Heaven's Collapse sleeps once more.");
    }

    /**
     * Re-reads config.yml into strongly typed fields. Falls back to sane
     * defaults for any missing or malformed value so a broken config.yml
     * never crashes the plugin.
     */
    private void loadConfigValues() {
        FileConfiguration cfg = getConfig();

        this.hitsRequired = Math.max(1, cfg.getInt("hits-required", 3));
        int idleSeconds = Math.max(0, cfg.getInt("idle-reset-seconds", 30));
        this.idleResetMillis = idleSeconds * 1000L;

        this.lightningEnabled = cfg.getBoolean("lightning.enabled", true);
        this.lightningDamage = cfg.getBoolean("lightning.damage", false);
        this.lightningFire = cfg.getBoolean("lightning.fire", false);

        this.particlesEnabled = cfg.getBoolean("effects.particles", true);
        this.soundsEnabled = cfg.getBoolean("effects.sounds", true);

        this.lightningObtainEnabled = cfg.getBoolean("lightning-obtain.enabled", true);
        this.negateLightningDamage = cfg.getBoolean("lightning-obtain.negate-damage", true);

        this.debugActionbarEnabled = cfg.getBoolean("debug.actionbar", true);

        this.resetOnItemSwitchEnabled = cfg.getBoolean("reset-on-item-switch", false);

        messages.clear();
        ConfigurationSection section = cfg.getConfigurationSection("messages");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String raw = section.getString(key, "");
                messages.put(key, ChatColor.translateAlternateColorCodes('&', raw));
            }
        }
    }

    /**
     * Looks up a configured, color-translated message. Returns an empty
     * string (never null) if the key is missing, so callers can safely
     * concatenate/send it without extra null checks.
     */
    public String getMessage(String key) {
        return messages.getOrDefault(key, "");
    }

    public HeavensCollapseItem getItemManager() {
        return itemManager;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public boolean isLightningEnabled() {
        return lightningEnabled;
    }

    public boolean isLightningDamage() {
        return lightningDamage;
    }

    public boolean isLightningFire() {
        return lightningFire;
    }

    public boolean isParticlesEnabled() {
        return particlesEnabled;
    }

    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }

    public boolean isLightningObtainEnabled() {
        return lightningObtainEnabled;
    }

    public boolean isNegateLightningDamage() {
        return negateLightningDamage;
    }

    public boolean isDebugActionbarEnabled() {
        return debugActionbarEnabled;
    }

    public boolean isResetOnItemSwitchEnabled() {
        return resetOnItemSwitchEnabled;
    }
}
