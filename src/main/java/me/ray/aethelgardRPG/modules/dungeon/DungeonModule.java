package me.ray.aethelgardRPG.modules.dungeon;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.modules.RPGModule;
import me.ray.aethelgardRPG.modules.dungeon.commands.DungeonCommand;
import me.ray.aethelgardRPG.modules.dungeon.commands.tabcompleters.DungeonCommandTabCompleter; // Import
import me.ray.aethelgardRPG.modules.dungeon.dungeons.DungeonManager;

import java.util.Objects;

public class DungeonModule implements RPGModule {

    private final AethelgardRPG plugin;
    private DungeonManager dungeonManager;

    public DungeonModule(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Dungeon";
    }

    @Override
    public void onLoad() {
        plugin.getLogger().info("Carregando módulo Dungeon...");
        this.dungeonManager = new DungeonManager(plugin, this);
    }

    @Override
    public void onEnable() {
        plugin.getLogger().info("Módulo Dungeon habilitado.");
        dungeonManager.loadDungeonTemplates(); // Carregar templates de dungeons
    }

    @Override
    public void onDisable() {
        plugin.getLogger().info("Módulo Dungeon desabilitado.");
        dungeonManager.shutdownAllDungeons(); // Limpar instâncias ativas
    }

    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }

    public void registerCommands(AethelgardRPG mainPlugin) {
        mainPlugin.registerCommand("dungeon", new DungeonCommand(mainPlugin, this), new DungeonCommandTabCompleter(mainPlugin));
    }
}