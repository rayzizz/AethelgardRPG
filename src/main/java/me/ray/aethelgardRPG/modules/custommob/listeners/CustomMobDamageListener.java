package me.ray.aethelgardRPG.modules.custommob.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.CustomMobData;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;
import me.ray.aethelgardRPG.modules.custommob.ai.minionimpl.SummonedMinionAIImpl;
import me.ray.aethelgardRPG.modules.custommob.mobs.ActiveMobInfo;
import me.ray.aethelgardRPG.modules.custommob.mobs.CustomMobManager;
import me.ray.aethelgardRPG.modules.item.ItemModule;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public class CustomMobDamageListener implements Listener {

    private final AethelgardRPG plugin;
    private final CustomMobModule customMobModule;
    private final CustomMobManager customMobManager;
    private final ItemModule itemModule;

    public CustomMobDamageListener(AethelgardRPG plugin, CustomMobModule customMobModule) {
        this.plugin = plugin;
        this.customMobModule = customMobModule;
        this.customMobManager = customMobModule.getCustomMobManager();
        this.itemModule = plugin.getModuleManager().getModule(ItemModule.class);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCustomMobDamaged(EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        if (!customMobManager.isCustomMob(victim)) return;

        ActiveMobInfo mobInfo = customMobManager.getActiveMobInfo(victim.getUniqueId());
        if (mobInfo == null) return;

        CustomMobData mobData = mobInfo.getBaseData();

        // Lógica para prevenir dano em lacaios
        if (isFriendlyFire(event, victim, mobData)) {
            event.setCancelled(true);
            return;
        }

        double incomingDamage = event.getDamage();
        double actualDamageCalculated;
        Player playerDamager = getPlayerDamager(event);

        if (playerDamager != null && itemModule != null && !itemModule.isRPGItem(playerDamager.getInventory().getItemInMainHand())) {
            actualDamageCalculated = 0.0;
        } else {
            EntityDamageEvent.DamageCause cause = event.getCause();
            // CORREÇÃO: Chamada do método calculateDamageReduction
            actualDamageCalculated = calculateDamageReduction(incomingDamage, mobData, cause);
        }

        if (actualDamageCalculated < 0) actualDamageCalculated = 0;

        double currentCustomHealth = mobInfo.getCurrentHealth() - actualDamageCalculated;
        mobInfo.setCurrentHealth(currentCustomHealth);

        if (actualDamageCalculated > 0.01) {
            mobInfo.setLastCombatTimeMillis(System.currentTimeMillis());
            if (playerDamager != null && !mobData.isPlayerSummonedMinion()) {
                mobInfo.setInCombat(true);
            }
            // O texto flutuante é global, mas a string é traduzida para o jogador que causou o dano.
            customMobModule.spawnFloatingText(victim.getLocation().add(0, victim.getEyeHeight() * 0.75, 0),
                    plugin.getMessage(playerDamager, "custommob.floating-text.damage-indicator", String.format("%.1f", actualDamageCalculated)), 30L);
        }

        if (currentCustomHealth <= 0) {
            event.setDamage(victim.getHealth() + victim.getAbsorptionAmount() + 1.0);

            plugin.getLogger().info(mobInfo.getBaseData().getDisplayName() + " foi derrotado (vida customizada zerada).");
        } else {
            if (actualDamageCalculated > 0.01) {
                event.setDamage(0.01);
            } else {
                event.setDamage(0.0);
            }

            if (mobInfo.getBaseData().getMaxHealth() > 0) {
                double healthPercentage = currentCustomHealth / mobInfo.getBaseData().getMaxHealth();
                double vanillaMaxHealth = victim.getAttribute(Attribute.MAX_HEALTH).getValue();
                victim.setHealth(Math.max(0.001, Math.min(vanillaMaxHealth, vanillaMaxHealth * healthPercentage)));
            } else {
                victim.setHealth(0.001);
            }

            plugin.getLogger().info(String.format("%s tomou %.2f de dano (%.2f mitigado). Vida restante: %.1f/%.1f",
                    mobData.getDisplayName(), incomingDamage, actualDamageCalculated, currentCustomHealth, mobData.getMaxHealth()));
        }
    }

    private boolean isFriendlyFire(EntityDamageEvent event, LivingEntity victim, CustomMobData victimData) {
        if (!victimData.isPlayerSummonedMinion()) return false;
        if (!(event instanceof EntityDamageByEntityEvent edbe)) return false;

        Entity damagerEntity = edbe.getDamager();
        LivingEntity actualDamager = null;

        if (damagerEntity instanceof LivingEntity) {
            actualDamager = (LivingEntity) damagerEntity;
        } else if (damagerEntity instanceof Projectile && ((Projectile) damagerEntity).getShooter() instanceof LivingEntity) {
            actualDamager = (LivingEntity) ((Projectile) damagerEntity).getShooter();
        }

        if (actualDamager == null) return false;

        String victimOwnerUUID = victim.getPersistentDataContainer().get(SummonedMinionAIImpl.SUMMON_OWNER_KEY, PersistentDataType.STRING);
        if (victimOwnerUUID == null) return false;

        // Dono atacando seu próprio lacaio
        if (actualDamager.getUniqueId().toString().equals(victimOwnerUUID)) {
            return true;
        }

        // Lacaio atacando outro lacaio do mesmo dono
        if (customMobManager.isCustomMob(actualDamager)) {
            Optional<CustomMobData> damagerDataOpt = customMobManager.getMobDataFromEntity(actualDamager); // Corrigido: aceita Entity
            if (damagerDataOpt.isPresent() && damagerDataOpt.get().isPlayerSummonedMinion()) {
                String damagerOwnerUUID = actualDamager.getPersistentDataContainer().get(SummonedMinionAIImpl.SUMMON_OWNER_KEY, PersistentDataType.STRING);
                return victimOwnerUUID.equals(damagerOwnerUUID);
            }
        }
        return false;
    }

    private Player getPlayerDamager(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent edbe) {
            if (edbe.getDamager() instanceof Player player) {
                return player;
            }
            if (edbe.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    // CORREÇÃO: Assinatura do método alterada para incluir CustomMobData e DamageCause
    private double calculateDamageReduction(double damage, CustomMobData mobData, EntityDamageEvent.DamageCause cause) {
        double defense = isMagicalDamage(cause) ? mobData.getMagicalDefense() : mobData.getPhysicalDefense();
        if (defense <= 0) return damage;
        return damage * (100.0 / (100.0 + defense));
    }

    private boolean isMagicalDamage(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.MAGIC ||
                cause == EntityDamageEvent.DamageCause.POISON ||
                cause == EntityDamageEvent.DamageCause.WITHER ||
                cause == EntityDamageEvent.DamageCause.DRAGON_BREATH;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCustomMobCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity entity = (LivingEntity) event.getEntity();
        if (!customMobManager.isCustomMob(entity)) return;

        long worldTime = entity.getWorld().getTime();
        if (worldTime >= 0 && worldTime < 12000) {
            event.setCancelled(true);
        }
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

        CustomMobData targetingMobData = targetingMobDataOpt.get();
        CustomMobData targetedMobData = targetedMobDataOpt.get();

        if (targetingMobData.isPlayerSummonedMinion() && targetedMobData.isPlayerSummonedMinion()) {
            PersistentDataContainer pdcTargeting = entityTargeting.getPersistentDataContainer();
            PersistentDataContainer pdcTargeted = entityBeingTargeted.getPersistentDataContainer();

            if (pdcTargeting.has(SummonedMinionAIImpl.SUMMON_OWNER_KEY, PersistentDataType.STRING) &&
                    pdcTargeted.has(SummonedMinionAIImpl.SUMMON_OWNER_KEY, PersistentDataType.STRING)) {

                String ownerUuidTargeting = pdcTargeting.get(SummonedMinionAIImpl.SUMMON_OWNER_KEY, PersistentDataType.STRING);
                String ownerUuidTargeted = pdcTargeted.get(SummonedMinionAIImpl.SUMMON_OWNER_KEY, PersistentDataType.STRING);

                if (ownerUuidTargeting != null && ownerUuidTargeting.equals(ownerUuidTargeted)) {
                    event.setCancelled(true);
                }
            }
        }
    }
}