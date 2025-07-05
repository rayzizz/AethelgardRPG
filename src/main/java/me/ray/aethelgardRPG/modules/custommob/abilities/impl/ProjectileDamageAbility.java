package me.ray.aethelgardRPG.modules.custommob.abilities.impl;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.MobAbility;
import me.ray.aethelgardRPG.modules.custommob.abilities.MobAbilityTemplate;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Objects;

public class ProjectileDamageAbility implements MobAbility {

    private final MobAbilityTemplate template;
    private final AethelgardRPG plugin;

    public ProjectileDamageAbility(MobAbilityTemplate template, AethelgardRPG plugin) {
        this.template = template;
        this.plugin = plugin;
    }

    @Override
    public String getId() { // CORREÇÃO: getAbilityId() para getId()
        return template.getId();
    }

    @Override
    public long getCooldown() { // Alterado para long
        return template.getCooldownTicks();
    }

    @Override
    public double getRange() {
        return template.getRange();
    }

    @Override
    public MobAbilityTemplate getTemplate() {
        return template;
    }

    @Override
    public String getDisplayName(Player playerContext) { // NOVO MÉTODO
        return template.getDisplayName(playerContext);
    }

    @Override
    public void execute(LivingEntity caster, List<Player> potentialTargets) {
        if (potentialTargets == null || potentialTargets.isEmpty()) {
            return;
        }

        Player target = potentialTargets.get(0);
        if (target == null || !target.isValid() || target.isDead()) {
            return;
        }

        String projectileTypeName = template.getParameter("projectile-type", "FIREBALL");
        double damageAmount = template.getParameter("damage-amount", 10.0);
        double explosionRadius = template.getParameter("explosion-radius", 2.0);
        String particleTrailName = template.getParameter("particle-trail", "FLAME");
        String soundOnCastName = template.getParameter("sound-on-cast", "ENTITY_BLAZE_SHOOT");
        String soundOnHitName = template.getParameter("sound-on-hit", "ENTITY_GENERIC_EXPLODE");
        double projectileSpeed = template.getParameter("projectile-speed", 1.5);
        int projectileLifeTicks = template.getParameter("projectile-life-ticks", 100);

        EntityType projectileType;
        try {
            projectileType = EntityType.valueOf(projectileTypeName.toUpperCase());
            if (!Projectile.class.isAssignableFrom(projectileType.getEntityClass())) {
                plugin.getLogger().warning("Invalid projectile-type for " + getId() + ": " + projectileTypeName + ". Not a projectile. Defaulting to FIREBALL."); // CORREÇÃO
                projectileType = EntityType.FIREBALL;
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid projectile-type for " + getId() + ": " + projectileTypeName + ". Defaulting to FIREBALL."); // CORREÇÃO
            projectileType = EntityType.FIREBALL;
        }

        playSound(caster.getLocation(), soundOnCastName, 1.0f, 1.0f);

        Vector direction = target.getEyeLocation().toVector().subtract(caster.getEyeLocation().toVector()).normalize();
        Projectile projectile = caster.launchProjectile(Objects.requireNonNull(projectileType.getEntityClass()).asSubclass(Projectile.class), direction.multiply(projectileSpeed));
        projectile.setShooter(caster);

        new BukkitRunnable() {
            private int ticksLived = 0;

            @Override
            public void run() {
                if (!projectile.isValid() || projectile.isDead() || projectile.isOnGround() || ticksLived > projectileLifeTicks) {
                    performImpact(projectile.getLocation(), caster, damageAmount, explosionRadius, soundOnHitName);
                    if (projectile.isValid()) {
                        projectile.remove();
                    }
                    this.cancel();
                    return;
                }

                try {
                    Particle particleTrail = Particle.valueOf(particleTrailName.toUpperCase());
                    // --- NOVO: Rastro mais denso ---
                    projectile.getWorld().spawnParticle(particleTrail, projectile.getLocation(), 3, 0.1, 0.1, 0.1, 0.01);
                } catch (Exception e) { /* silent fail */ }

                ticksLived++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void performImpact(Location impactLocation, LivingEntity caster, double damage, double radius, String soundName) {
        playSound(impactLocation, soundName, 1.0f, 1.0f);

        // --- NOVO: EFEITO DE EXPLOSÃO EM CAMADAS ---
        impactLocation.getWorld().spawnParticle(Particle.EXPLOSION, impactLocation, 2, 0, 0, 0, 0);
        impactLocation.getWorld().spawnParticle(Particle.LAVA, impactLocation, 15, radius * 0.5, radius * 0.5, radius * 0.5, 0);
        impactLocation.getWorld().spawnParticle(Particle.LARGE_SMOKE, impactLocation, 25, radius, radius, radius, 0.05);


        for (Entity nearbyEntity : impactLocation.getWorld().getNearbyEntities(impactLocation, radius, radius, radius)) {
            if (nearbyEntity instanceof Player && !nearbyEntity.equals(caster)) {
                Player pTarget = (Player) nearbyEntity;
                pTarget.damage(damage, caster);
            }
        }
    }

    private void playSound(Location location, String soundName, float volume, float pitch) {
        try {
            // Converte "ENTITY_BLAZE_SHOOT" para "entity.blaze.shoot"
            String soundKey = soundName.toLowerCase().replace('_', '.');
            // Usa o registro para obter o som de forma segura
            Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundKey));

            if (sound != null) {
                location.getWorld().playSound(location, sound, volume, pitch);
            } else {
                plugin.getLogger().warning("Som inválido para " + getId() + ": " + soundName); // CORREÇÃO
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao tocar som para " + getId() + ": " + soundName + ". Erro: " + e.getMessage()); // CORREÇÃO
        }
    }
}