package me.ray.aethelgardRPG.core.modules;

import me.ray.aethelgardRPG.AethelgardRPG;

public interface RPGModule {

    String getName(); // Get the name of the module
    void onLoad();    // Called when the module is initially loaded (e.g., register commands/listeners, load configs)
    void onEnable();  // Called when the module is fully enabled (e.g., start tasks, connect to services)
    void onDisable(); // Called when the module is being disabled (e.g., save data, stop tasks)

    /**
     * Registra os comandos do módulo. A implementação padrão é vazia.
     * @param plugin A instância principal do plugin.
     */
    default void registerCommands(AethelgardRPG plugin) {}
}
