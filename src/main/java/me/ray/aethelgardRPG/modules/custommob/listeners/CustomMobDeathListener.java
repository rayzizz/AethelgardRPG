package me.ray.aethelgardRPG.modules.custommob.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule; // Import CustomMobModule
import me.ray.aethelgardRPG.modules.custommob.mobs.CustomMobManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class CustomMobDeathListener implements Listener {

    private final AethelgardRPG plugin;
    private final CustomMobModule customMobModule; // Referência ao CustomMobModule
    private final CustomMobManager customMobManager; // Referência ao CustomMobManager
    private final CharacterModule characterModule;

    public CustomMobDeathListener(AethelgardRPG plugin, CustomMobModule customMobModule) {
        this.plugin = plugin;
        this.customMobModule = customMobModule;
        this.customMobManager = customMobModule.getCustomMobManager();
        this.characterModule = plugin.getModuleManager().getModule(CharacterModule.class);
    }

    @EventHandler
    public void onCustomMobDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity killedEntity = event.getEntity();

        // Verifica se a entidade morta é um mob customizado
        if (!customMobManager.isCustomMob(killedEntity)) {
            return;
        }

        // Obtém o CustomMobData para o mob morto
        customMobManager.getMobDataFromEntity(killedEntity).ifPresent(killedMobData -> {
            // Lida com a queda de XP
            if (event.getEntity().getKiller() instanceof Player killer) {
                if (characterModule != null) {
                    // CORREÇÃO: Usa getExperienceDrop() em vez de getXpOnKill()
                    double xpToAward = killedMobData.getExperienceDrop();
                    if (xpToAward > 0) {
                        characterModule.addExperience(killer, xpToAward);
                    }
                }
            }

            // Lida com drops customizados (se houver)
            // TODO: Implementar lógica de drops customizados baseada em killedMobData.getDrops()

            // Remove o mob da lista de mobs ativos
            customMobManager.removeActiveMob(killedEntity.getUniqueId());
            plugin.getLogger().fine("Custom mob " + killedMobData.getId() + " (" + killedEntity.getUniqueId() + ") morreu.");
        });
    }
}