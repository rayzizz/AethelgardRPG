package me.ray.aethelgardRPG.modules.area;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.modules.RPGModule;
import me.ray.aethelgardRPG.modules.area.areas.AreaManager;
import me.ray.aethelgardRPG.modules.area.commands.AreaCommand;
import me.ray.aethelgardRPG.modules.area.listeners.PlayerAreaListener;

import java.util.Objects;

public class AreaModule implements RPGModule {

    private final AethelgardRPG plugin;
    private AreaManager areaManager;

    public AreaModule(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Area";
    }

    @Override
    public void onLoad() {
        plugin.getLogger().info("Carregando módulo Area...");
        this.areaManager = new AreaManager(plugin, this);
        // Registrar listeners
        plugin.getServer().getPluginManager().registerEvents(new PlayerAreaListener(plugin, this), plugin);
    }

    @Override
    public void onEnable() {
        plugin.getLogger().info("Módulo Area habilitado.");
        areaManager.loadAreaConfigurations(); // Carregar definições de áreas
    }

    @Override
    public void onDisable() {
        plugin.getLogger().info("Módulo Area desabilitado.");
    }

    public AreaManager getAreaManager() {
        return areaManager;
    }

    @Override
    public void registerCommands(AethelgardRPG mainPlugin) {
        mainPlugin.registerCommand("areaadmin", new AreaCommand(mainPlugin, this));
    }
}