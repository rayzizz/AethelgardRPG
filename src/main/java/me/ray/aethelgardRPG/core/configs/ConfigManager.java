package me.ray.aethelgardRPG.core.configs;

import me.ray.aethelgardRPG.AethelgardRPG;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ConfigManager {

    private final AethelgardRPG plugin;
    private FileConfiguration mainConfig;
    private File mainConfigFile;

    private FileConfiguration databaseConfig;
    private FileConfiguration performanceConfig;
    private FileConfiguration characterSettingsConfig;
    private FileConfiguration customMobSettingsConfig;
    private FileConfiguration combatSettingsConfig;
    private FileConfiguration skillSettingsConfig; // Adicionado: Configuração para habilidades

    public ConfigManager(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        // Load main config.yml
        mainConfigFile = new File(plugin.getDataFolder(), "config.yml");
        if (!mainConfigFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        mainConfig = YamlConfiguration.loadConfiguration(mainConfigFile);
        plugin.getLogger().info("config.yml loaded successfully.");

        // Load other specific configs
        databaseConfig = loadSpecificConfig("database.yml");
        performanceConfig = loadSpecificConfig("performance.yml");
        characterSettingsConfig = loadSpecificConfig("character_settings.yml");
        customMobSettingsConfig = loadSpecificConfig("custom_mob_settings.yml");
        combatSettingsConfig = loadSpecificConfig("combat_settings.yml");
        skillSettingsConfig = loadSpecificConfig("skills.yml"); // Adicionado: Carrega skills.yml
    }

    private FileConfiguration loadSpecificConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
        }
        FileConfiguration specificConfig = YamlConfiguration.loadConfiguration(configFile);

        // Load defaults from JAR
        try (InputStream defaultConfigResource = plugin.getResource(fileName);
             InputStreamReader defaultConfigStream = new InputStreamReader(Objects.requireNonNull(defaultConfigResource), StandardCharsets.UTF_8)) {
            if (defaultConfigResource != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(defaultConfigStream);
                specificConfig.setDefaults(defaultConfig);
            }
        } catch (IOException | NullPointerException e) {
            plugin.getLogger().warning("Could not load default config from JAR for " + fileName + ": " + e.getMessage());
        }
        plugin.getLogger().info(fileName + " loaded successfully.");
        return specificConfig;
    }

    public FileConfiguration getMainConfig() {
        if (mainConfig == null) {
            // MELHORIA: Lança uma exceção clara se a configuração principal não foi carregada.
            // Isso indica um erro grave na inicialização do plugin.
            throw new IllegalStateException("A configuração principal (config.yml) não foi carregada. O plugin não pode continuar.");
        }
        return mainConfig;
    }

    private void ensureConfigLoaded(FileConfiguration config, String configName) {
        if (config == null) {
            throw new IllegalStateException("A configuração '" + configName + "' não foi carregada durante a inicialização do plugin.");
        }
    }

    public FileConfiguration getDatabaseConfig() {
        ensureConfigLoaded(databaseConfig, "database.yml");
        return databaseConfig;
    }

    public FileConfiguration getPerformanceConfig() {
        ensureConfigLoaded(performanceConfig, "performance.yml");
        return performanceConfig;
    }

    public FileConfiguration getCharacterSettingsConfig() {
        ensureConfigLoaded(characterSettingsConfig, "character_settings.yml");
        return characterSettingsConfig;
    }

    public FileConfiguration getCustomMobSettingsConfig() {
        ensureConfigLoaded(customMobSettingsConfig, "custom_mob_settings.yml");
        return customMobSettingsConfig;
    }

    public FileConfiguration getCombatSettingsConfig() {
        ensureConfigLoaded(combatSettingsConfig, "combat_settings.yml");
        return combatSettingsConfig;
    }

    public FileConfiguration getSkillSettingsConfig() {
        ensureConfigLoaded(skillSettingsConfig, "skills.yml");
        return skillSettingsConfig;
    }

    public void reloadAllConfigs() {
        if (mainConfigFile == null) {
            mainConfigFile = new File(plugin.getDataFolder(), "config.yml");
        }
        mainConfig = YamlConfiguration.loadConfiguration(mainConfigFile);
        // Reload other specific configs
        databaseConfig = loadSpecificConfig("database.yml");
        performanceConfig = loadSpecificConfig("performance.yml");
        characterSettingsConfig = loadSpecificConfig("character_settings.yml");
        customMobSettingsConfig = loadSpecificConfig("custom_mob_settings.yml");
        combatSettingsConfig = loadSpecificConfig("combat_settings.yml");
        skillSettingsConfig = loadSpecificConfig("skills.yml"); // Adicionado: Recarrega skills.yml
        plugin.getLogger().info("All configuration files reloaded.");
    }

    // Add methods to save configs if needed
}