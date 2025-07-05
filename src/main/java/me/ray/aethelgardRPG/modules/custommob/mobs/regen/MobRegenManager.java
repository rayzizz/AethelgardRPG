package me.ray.aethelgardRPG.modules.custommob.mobs.regen;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;

// Classe placeholder para gerenciar a regeneração de vida dos mobs.
public class MobRegenManager {

    private final AethelgardRPG plugin;

    public MobRegenManager(AethelgardRPG plugin, CustomMobModule customMobModule) {
        this.plugin = plugin;
        // customMobModule não é usado diretamente aqui, mas pode ser útil para futuras integrações.
    }

    public void loadRegenProfiles() {
        // Lógica para carregar perfis de regeneração.
        plugin.getLogger().info("Mob regen profiles loaded (placeholder).");
    }

    public void startRegenTask() {
        // Lógica para iniciar a tarefa de regeneração.
        plugin.getLogger().info("Mob regen task started (placeholder).");
    }

    public void stopRegenTask() {
        // Lógica para parar a tarefa de regeneração.
        plugin.getLogger().info("Mob regen task stopped (placeholder).");
    }
}