// C:/Users/r/IdeaProjects/AethelgardRPG/src/main/java/me/ray/aethelgardRPG/core/modules/ModuleManager.java
package me.ray.aethelgardRPG.core.modules;

import me.ray.aethelgardRPG.AethelgardRPG;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModuleManager {

    private final AethelgardRPG plugin;
    private final Map<String, RPGModule> modules = new LinkedHashMap<>();
    private final Map<String, Boolean> moduleEnabledStatus = new LinkedHashMap<>();

    public ModuleManager(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    public void loadModules() {
        plugin.getLogger().info("Loading RPG modules...");

        if (modules.isEmpty()) {
            plugin.getLogger().info("No modules registered to load.");
            return;
        }

        modules.forEach((name, module) -> {
            boolean isEnabledInConfig = plugin.getConfigManager().getMainConfig().getBoolean("modules." + name + ".enabled", true);
            moduleEnabledStatus.put(name, isEnabledInConfig);
            if (isEnabledInConfig) {
                try {
                    module.onLoad();
                    plugin.getLogger().info("Module '" + name + "' loaded.");
                } catch (Exception e) {
                    plugin.getLogger().severe("Error loading module '" + name + "': " + e.getMessage());
                    e.printStackTrace();
                    moduleEnabledStatus.put(name, false);
                }
            } else {
                plugin.getLogger().info("Module '" + name + "' is disabled in config.");
            }
        });
        plugin.getLogger().info(modules.size() + " modules processed for loading.");
    }

    public void registerModule(RPGModule module) {
        modules.put(module.getName().toLowerCase(), module);
    }

    /**
     * Registra os comandos de todos os módulos que estão habilitados.
     * Este método deve ser chamado após loadModules() e antes de enableModules().
     */
    public void registerModuleCommands() {
        plugin.getLogger().info("Registering module commands...");
        modules.forEach((name, module) -> {
            if (moduleEnabledStatus.getOrDefault(name, false)) {
                // Cada módulo agora é responsável por registrar seus próprios comandos.
                module.registerCommands(plugin);
            }
        });
    }

    public void enableModules() {
        modules.forEach((name, module) -> {
            if (moduleEnabledStatus.getOrDefault(name, false)) {
                plugin.getPerformanceMonitor().startTiming("moduleEnable." + name);
                module.onEnable();
                plugin.getPerformanceMonitor().stopTiming("moduleEnable." + name);
                plugin.getLogger().info("Module '" + name + "' enabled.");
            }
        });
    }

    public void disableModules() {
        List<RPGModule> reversedModules = new ArrayList<>(modules.values());
        java.util.Collections.reverse(reversedModules);

        for (RPGModule module : reversedModules) {
            if (moduleEnabledStatus.getOrDefault(module.getName().toLowerCase(), false)) {
                plugin.getPerformanceMonitor().startTiming("moduleDisable." + module.getName());
                module.onDisable();
                plugin.getPerformanceMonitor().stopTiming("moduleDisable." + module.getName());
                plugin.getLogger().info("Module '" + module.getName() + "' disabled.");
            }
        }
    }

    public <T extends RPGModule> T getModule(Class<T> moduleClass) {
        return modules.values().stream()
                .filter(moduleClass::isInstance)
                .map(moduleClass::cast)
                .findFirst()
                .orElse(null);
    }

    public RPGModule getModule(String name) {
        return modules.get(name.toLowerCase());
    }

    public boolean isModuleEnabled(Class<? extends RPGModule> moduleClass) {
        for (RPGModule module : modules.values()) {
            if (moduleClass.isInstance(module)) {
                return moduleEnabledStatus.getOrDefault(module.getName().toLowerCase(), false);
            }
        }
        return false;
    }
}