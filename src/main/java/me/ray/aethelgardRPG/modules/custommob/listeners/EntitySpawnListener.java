package me.ray.aethelgardRPG.modules.custommob.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class EntitySpawnListener implements Listener {

    private final AethelgardRPG plugin;
    private final CustomMobModule customMobModule;

    public EntitySpawnListener(AethelgardRPG plugin, CustomMobModule customMobModule) {
        this.plugin = plugin;
        this.customMobModule = customMobModule;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        FileConfiguration mobSettings = plugin.getConfigManager().getCustomMobSettingsConfig();
        boolean preventVanillaNaturalSpawns = mobSettings.getBoolean("spawning.prevent-vanilla-natural-spawns", false);

        if (preventVanillaNaturalSpawns) {
            // Se a configuração para prevenir spawns vanilla estiver ativa
            if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
                // Cancela o spawn natural. Mobs customizados spawnados pelo plugin
                // (que usam SpawnReason.CUSTOM ou similar) não serão afetados por esta checagem.
                event.setCancelled(true);
                // plugin.getLogger().fine("Cancelled natural spawn of " + event.getEntityType() + " at " + event.getLocation() + " due to config.");
            }
            // Você pode adicionar mais 'else if' aqui para outros SpawnReasons se quiser controlá-los também
            // Ex:
            // else if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER && mobSettings.getBoolean("spawning.prevent-vanilla-spawner-spawns", false)) {
            //    event.setCancelled(true);
            // }
        } else {
            // Se a prevenção de spawns vanilla NÃO estiver ativa, você pode colocar aqui
            // a lógica de substituição de mobs (o TODO original).
            if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
                // TODO: Lógica para substituir mobs vanilla por custom mobs com base na região, bioma, etc.
                // Exemplo: 10% de chance de um Zumbi ser substituído por um "Goblin Guerreiro"
                // if (event.getEntityType() == EntityType.ZOMBIE && Math.random() < 0.1) {
                //     event.setCancelled(true);
                //     customMobModule.getCustomMobManager().spawnCustomMob("goblin_warrior", event.getLocation());
                // }
            }
        }
    }
}