package me.ray.aethelgardRPG.modules.custommob.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.CustomMobData;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;
import me.ray.aethelgardRPG.modules.custommob.ai.minionimpl.SummonedMinionAIImpl;
import me.ray.aethelgardRPG.modules.custommob.mobs.CustomMobManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public class MinionTargetListener implements Listener {

    private final CustomMobManager customMobManager;

    public MinionTargetListener(AethelgardRPG plugin, CustomMobModule customMobModule) {
        this.customMobManager = customMobModule.getCustomMobManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMinionTargetsAnotherMinion(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entityTargeting) || !(event.getTarget() instanceof LivingEntity entityBeingTargeted)) {
            return;
        }

        if (!customMobManager.isCustomMob(entityTargeting) || !customMobManager.isCustomMob(entityBeingTargeted)) {
            return;
        }

        Optional<CustomMobData> targetingMobDataOpt = customMobManager.getMobDataFromEntity(entityTargeting); // Corrigido: aceita Entity
        Optional<CustomMobData> targetedMobDataOpt = customMobManager.getMobDataFromEntity(entityBeingTargeted); // Corrigido: aceita Entity

        if (targetingMobDataOpt.isEmpty() || targetedMobDataOpt.isEmpty()) {
            return;
        }

        if (targetingMobDataOpt.get().isPlayerSummonedMinion() && targetedMobDataOpt.get().isPlayerSummonedMinion()) {
            String ownerUuidTargeting = entityTargeting.getPersistentDataContainer().get(SummonedMinionAIImpl.SUMMON_OWNER_KEY, PersistentDataType.STRING);
            String ownerUuidTargeted = entityBeingTargeted.getPersistentDataContainer().get(SummonedMinionAIImpl.SUMMON_OWNER_KEY, PersistentDataType.STRING);

            if (ownerUuidTargeting != null && ownerUuidTargeting.equals(ownerUuidTargeted)) {
                event.setCancelled(true);
            }
        }
    }
}