package me.ray.aethelgardRPG.modules.custommob.abilities.impl;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.MobAbility;
import me.ray.aethelgardRPG.modules.custommob.abilities.MobAbilityTemplate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class AreaOfEffectDamageAbility implements MobAbility {

    private final MobAbilityTemplate template;
    private final AethelgardRPG plugin;

    public AreaOfEffectDamageAbility(MobAbilityTemplate template, AethelgardRPG plugin) {
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
        // --- NOVOS PARÂMETROS ---
        Number chargeDurationNumber = template.getParameter("charge-duration-ticks", 25L);
        long chargeDurationTicks = chargeDurationNumber.longValue();
        double jumpHeight = template.getParameter("jump-height", 1.2);

        String chargeParticleName = template.getParameter("charge-particle-effect", "LAVA");
        int chargeParticleCount = template.getParameter("charge-particle-count", 20);
        double chargeParticleOffset = template.getParameter("charge-particle-offset", 0.5);
        String chargeSoundName = template.getParameter("charge-sound-effect", "ENTITY_GHAST_WARN");
        float chargeSoundVolume = ((Number) template.getParameter("charge-sound-volume", 1.0)).floatValue();
        float chargeSoundPitch = ((Number) template.getParameter("charge-sound-pitch", 0.8)).floatValue();

        playSound(caster.getLocation(), chargeSoundName, chargeSoundVolume, chargeSoundPitch);

        new BukkitRunnable() {
            long ticksElapsed = 0;

            @Override
            public void run() {
                if (!caster.isValid() || caster.isDead()) {
                    this.cancel();
                    return;
                }

                try {
                    Particle particle = Particle.valueOf(chargeParticleName.toUpperCase());
                    caster.getWorld().spawnParticle(particle, caster.getLocation().add(0, 0.1, 0),
                            chargeParticleCount, chargeParticleOffset, 0.2, chargeParticleOffset, 0.05);
                } catch (Exception e) { /* silent fail */ }

                if (ticksElapsed >= chargeDurationTicks) {
                    // --- NOVA LÓGICA DE SALTO ---
                    caster.setVelocity(new Vector(0, jumpHeight, 0));
                    waitForLandingAndImpact(caster);
                    this.cancel();
                    return;
                }
                ticksElapsed++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void waitForLandingAndImpact(LivingEntity caster) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!caster.isValid() || caster.isDead()) {
                    this.cancel();
                    return;
                }
                // Espera o mob tocar o chão para causar o impacto
                if (caster.isOnGround()) {
                    performImpact(caster);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 5L, 1L); // Começa a checar após 5 ticks
    }


    private void performImpact(LivingEntity caster) {
        double damage = template.getParameter("impact-damage-amount", 30.0);
        double radius = template.getParameter("impact-radius", 5.0);
        double knockbackStrength = template.getParameter("impact-knockback-strength", 0.8);
        String impactParticleName = template.getParameter("impact-particle-effect", "EXPLOSION_LARGE");
        String impactSoundName = template.getParameter("impact-sound-effect", "ENTITY_GENERIC_EXPLODE");
        float impactSoundVolume = ((Number) template.getParameter("impact-sound-volume", 1.2)).floatValue();
        float impactSoundPitch = ((Number) template.getParameter("impact-sound-pitch", 0.7)).floatValue();

        // --- NOVOS PARÂMETROS DA ONDA DE CHOQUE ---
        String shockwaveParticleName = template.getParameter("shockwave-particle", "CRIT");
        int shockwaveParticleDensity = template.getParameter("shockwave-density", 3);

        Location impactLocation = caster.getLocation();
        playSound(impactLocation, impactSoundName, impactSoundVolume, impactSoundPitch);

        // Partícula principal do impacto
        try {
            Particle particle = Particle.valueOf(impactParticleName.toUpperCase());
            impactLocation.getWorld().spawnParticle(particle, impactLocation, 1);
        } catch (Exception e) { /* silent fail */ }

        // --- NOVA LÓGICA DA ONDA DE CHOQUE ---
        try {
            Particle shockwaveParticle = Particle.valueOf(shockwaveParticleName.toUpperCase());
            BlockData groundBlock = impactLocation.clone().subtract(0, 0.1, 0).getBlock().getBlockData();

            new BukkitRunnable() {
                double currentRadius = 1.0;

                @Override
                public void run() {
                    if (currentRadius > radius) {
                        this.cancel();
                        return;
                    }
                    // Desenha um círculo de partículas que se expande
                    for (int i = 0; i < currentRadius * 2 * Math.PI * shockwaveParticleDensity; i++) {
                        double angle = (double) i / (shockwaveParticleDensity * currentRadius) * 2 * Math.PI;
                        double x = Math.cos(angle) * currentRadius;
                        double z = Math.sin(angle) * currentRadius;
                        Location particleLoc = impactLocation.clone().add(x, 0.2, z);
                        particleLoc.getWorld().spawnParticle(shockwaveParticle, particleLoc, 1, 0, 0, 0, 0);
                    }
                    // Adiciona partículas de "chão quebrando"
                    impactLocation.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, impactLocation.clone().add(0, 0.2, 0), 15, currentRadius * 0.5, 0.1, currentRadius * 0.5, 0, groundBlock);

                    currentRadius += 0.5;
                }
            }.runTaskTimer(plugin, 0L, 1L);

        } catch (Exception e) {
            plugin.getLogger().warning("Partícula da onda de choque inválida: " + shockwaveParticleName);
        }


        // Dano e knockback
        for (Entity nearbyEntity : caster.getNearbyEntities(radius, radius, radius)) {
            if (nearbyEntity instanceof Player) {
                Player pTarget = (Player) nearbyEntity;
                pTarget.damage(damage, caster);

                if (knockbackStrength > 0) {
                    Vector direction = pTarget.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize();
                    direction.setY(0.4); // Joga um pouco para cima
                    pTarget.setVelocity(direction.multiply(knockbackStrength));
                }
            }
        }
        plugin.getLogger().info(caster.getName() + " used " + template.getDisplayName(null) + " impacting a radius of " + radius);
    }

    private void playSound(Location location, String soundName, float volume, float pitch) {
        try {
            // Converte "ENTITY_GHAST_WARN" para "minecraft:entity.ghast.warn"
            String soundKey = soundName.toLowerCase().replace('_', '.');
            org.bukkit.Sound sound = org.bukkit.Registry.SOUNDS.get(org.bukkit.NamespacedKey.minecraft(soundKey));

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