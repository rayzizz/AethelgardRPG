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

public class EntityDamageListenerMob implements Listener {

    private final AethelgardRPG plugin;
    private final CustomMobModule customMobModule;
    private final CustomMobManager customMobManager;
    private final ItemModule itemModule;

    public EntityDamageListenerMob(AethelgardRPG plugin, CustomMobModule customMobModule) {
        this.plugin = plugin;
        this.customMobModule = customMobModule;
        this.customMobManager = customMobModule.getCustomMobManager();
        this.itemModule = plugin.getModuleManager().getModule(ItemModule.class);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCustomMobDamaged(EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        if (!customMobManager.isCustomMob(victim)) return;

        ActiveMobInfo mobInfo = customMobManager.getActiveMobInfo(victim.getUniqueId());
        if (mobInfo == null) return;

        CustomMobData mobData = mobInfo.getBaseData();
        // Verifica se é um lacaio invocado e se é dia
        if (mobData.isPlayerSummonedMinion()) {
            long worldTime = victim.getWorld().getTime();
            // Dia: 0 a 11999 ticks. Noite: 12000 a 23999 ticks.
            if (worldTime >= 0 && worldTime < 12000) {
                event.setDamage(0.0);
                event.setCancelled(true);

                // Opcional: Notificar o atacante se for um jogador
                if (event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) event;
                    Entity damagerEntity = edbe.getDamager();
                    Player damagerPlayer = null;
                    if (damagerEntity instanceof Player) damagerPlayer = (Player) damagerEntity;
                    else if (damagerEntity instanceof Projectile && ((Projectile) damagerEntity).getShooter() instanceof Player) damagerPlayer = (Player) ((Projectile) damagerEntity).getShooter();

                    if (damagerPlayer != null) {
                        damagerPlayer.sendMessage(plugin.getMessage(damagerPlayer, "custommob.chat.minion-invulnerable-day"));
                    }
                }
                return;
            }
        }
        // --- Impedir que o dono cause dano aos seus próprios lacaios ---
        if (event instanceof EntityDamageByEntityEvent && mobData.isPlayerSummonedMinion()) {
            EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) event;
            Entity damagerEntity = edbe.getDamager();
            Player ownerDamager = null;

            if (damagerEntity instanceof Player) {
                ownerDamager = (Player) damagerEntity;
            } else if (damagerEntity instanceof Projectile) {
                Projectile projectile = (Projectile) damagerEntity;
                if (projectile.getShooter() instanceof Player) {
                    ownerDamager = (Player) projectile.getShooter();
                }
            }

            if (ownerDamager != null) {
                String victimOwnerUUIDString = victim.getPersistentDataContainer().get(SummonedMinionAIImpl.SUMMON_OWNER_KEY, PersistentDataType.STRING);
                if (victimOwnerUUIDString != null && ownerDamager.getUniqueId().toString().equals(victimOwnerUUIDString)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // --- Prevenir dano entre lacaios do mesmo dono ---
        if (event instanceof EntityDamageByEntityEvent && mobData.isPlayerSummonedMinion()) {
            EntityDamageByEntityEvent edbe = (EntityDamageByEntityEvent) event;
            Entity damagerEntity = edbe.getDamager();
            LivingEntity actualDamager = null;

            if (damagerEntity instanceof LivingEntity) {
                actualDamager = (LivingEntity) damagerEntity;
            } else if (damagerEntity instanceof Projectile) {
                Projectile projectile = (Projectile) damagerEntity;
                if (projectile.getShooter() instanceof LivingEntity) {
                    actualDamager = (LivingEntity) projectile.getShooter();
                }
            }

            if (actualDamager != null && customMobManager.isCustomMob(actualDamager)) {
                Optional<CustomMobData> damagerMobDataOpt = customMobManager.getMobDataFromEntity(actualDamager); // Corrigido: aceita Entity
                if (damagerMobDataOpt.isPresent() && damagerMobDataOpt.get().isPlayerSummonedMinion()) {
                    String victimOwnerUUID = victim.getPersistentDataContainer().get(SummonedMinionAIImpl.SUMMON_OWNER_KEY, PersistentDataType.STRING);
                    String damagerOwnerUUID = actualDamager.getPersistentDataContainer().get(SummonedMinionAIImpl.SUMMON_OWNER_KEY, PersistentDataType.STRING);

                    if (victimOwnerUUID != null && victimOwnerUUID.equals(damagerOwnerUUID)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
        // --- Fim da prevenção de dano entre lacaios ---

        double incomingDamage = event.getDamage();
        double actualDamageCalculated;

        boolean isDamageFromPlayerWithNonRPGWeapon = false;
        Player playerDamager = null;

        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent edbeEvent = (EntityDamageByEntityEvent) event;
            Entity damagerEntity = edbeEvent.getDamager();

            if (damagerEntity instanceof Player) {
                playerDamager = (Player) damagerEntity;
            } else if (damagerEntity instanceof Projectile) {
                Projectile projectile = (Projectile) damagerEntity;
                if (projectile.getShooter() instanceof Player) {
                    playerDamager = (Player) projectile.getShooter();
                }
            }

            if (playerDamager != null && itemModule != null) {
                ItemStack weapon = playerDamager.getInventory().getItemInMainHand();
                if (!itemModule.isRPGItem(weapon)) {
                    isDamageFromPlayerWithNonRPGWeapon = true;
                }
            }
        }

        if (isDamageFromPlayerWithNonRPGWeapon) {
            actualDamageCalculated = 0.0;
        } else {
            EntityDamageEvent.DamageCause cause = event.getCause();
            // CORREÇÃO: Chamada do método calculateDamageReduction
            actualDamageCalculated = calculateDamageReduction(incomingDamage, mobData, cause);
        }

        if (actualDamageCalculated < 0) actualDamageCalculated = 0;

        double currentCustomHealth = mobInfo.getCurrentHealth();
        currentCustomHealth -= actualDamageCalculated;
        currentCustomHealth = Math.max(0, currentCustomHealth);

        if (actualDamageCalculated > 0.01) {
            mobInfo.setLastCombatTimeMillis(System.currentTimeMillis());
        }

        if (actualDamageCalculated > 0.01 && !mobData.isPlayerSummonedMinion()) {
            boolean damageDirectlyFromPlayer = (event instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent) event).getDamager() instanceof Player);
            boolean damageFromPlayerProjectile = (event instanceof EntityDamageByEntityEvent &&
                    ((EntityDamageByEntityEvent) event).getDamager() instanceof Projectile &&
                    ((Projectile) ((EntityDamageByEntityEvent) event).getDamager()).getShooter() instanceof Player);

            if (damageDirectlyFromPlayer || damageFromPlayerProjectile) {
                mobInfo.setInCombat(true);
            }
        }

        mobInfo.setCurrentHealth(currentCustomHealth);
        PersistentDataContainer pdc = victim.getPersistentDataContainer();
        pdc.set(customMobManager.CURRENT_HEALTH_KEY, PersistentDataType.DOUBLE, currentCustomHealth);

        if (actualDamageCalculated > 0.01) {
            // Usa o método spawnFloatingText do CustomMobModule
            // A mensagem é traduzida no momento da chamada, mas o display é global.
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
                double vanillaMaxHealth = victim.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
                victim.setHealth(Math.max(0.001, Math.min(vanillaMaxHealth, vanillaMaxHealth * healthPercentage)));
            } else {
                victim.setHealth(0.001);
            }

            plugin.getLogger().info(String.format("%s tomou %.2f de dano (%.2f mitigado). Vida restante: %.1f/%.1f",
                    mobData.getDisplayName(), incomingDamage, actualDamageCalculated, currentCustomHealth, mobData.getMaxHealth()));
        }
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