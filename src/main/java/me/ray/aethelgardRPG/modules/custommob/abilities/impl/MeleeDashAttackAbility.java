package me.ray.aethelgardRPG.modules.custommob.abilities.impl;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.MobAbility;
import me.ray.aethelgardRPG.modules.custommob.abilities.MobAbilityTemplate;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class MeleeDashAttackAbility implements MobAbility {

    private final MobAbilityTemplate template;
    private final AethelgardRPG plugin;

    public MeleeDashAttackAbility(MobAbilityTemplate template, AethelgardRPG plugin) {
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
            plugin.getLogger().info(caster.getName() + " tentou usar " + template.getDisplayName(null) + " mas não tinha alvo.");
            return;
        }

        Player target = potentialTargets.get(0);
        if (target == null || !target.isValid() || target.isDead()) {
            plugin.getLogger().info(caster.getName() + " tentou usar " + template.getDisplayName(null) + " mas o alvo é inválido.");
            return;
        }

        Number chargeDurationNumber = template.getParameter("charge-duration-ticks", 20);
        long chargeDurationTicks = chargeDurationNumber.longValue();

        String chargeParticleEffect = template.getParameter("charge-particle-effect", "CRIT_MAGIC");
        int chargeParticleCount = template.getParameter("charge-particle-count", 10);
        double chargeParticleOffset = template.getParameter("charge-particle-offset", 0.5);
        String chargeSoundEffect = template.getParameter("charge-sound-effect", "ENTITY_ENDER_DRAGON_GROWL");

        double dashSpeedMultiplier = template.getParameter("dash-speed-multiplier", 2.0);
        double maxDashDistance = template.getParameter("max-dash-distance", 10.0);
        String dashParticleTrail = template.getParameter("dash-particle-trail", "SMOKE_NORMAL");
        String dashSoundEffect = template.getParameter("dash-sound-effect", "ENTITY_PLAYER_ATTACK_SWEEP");

        double impactDamageAmount = template.getParameter("impact-damage-amount", 10.0);
        double impactRadius = template.getParameter("impact-radius", 1.5);
        double impactKnockbackStrength = template.getParameter("impact-knockback-strength", 1.0);
        String impactParticleEffect = template.getParameter("impact-particle-effect", "EXPLOSION_LARGE");
        String impactSoundEffect = template.getParameter("impact-sound-effect", "ENTITY_GENERIC_EXPLODE");

        plugin.getLogger().info(caster.getName() + " está usando " + template.getDisplayName(null) + " no jogador " + target.getName());

        // --- Charge Phase ---
        try {
            NamespacedKey soundChargeKey = NamespacedKey.minecraft(chargeSoundEffect.toLowerCase().replace('.', '_'));
            Sound soundCharge = Registry.SOUNDS.get(soundChargeKey);
            if (soundCharge == null) {
                plugin.getLogger().warning("Som de carga inválido para " + getId() + ": " + chargeSoundEffect + ". Usando padrão."); // CORREÇÃO
                soundCharge = Sound.ENTITY_ENDER_DRAGON_GROWL;
            }
            Number chargeVolumeNumber = template.getParameter("charge-sound-volume", 1.0);
            float chargeVolume = chargeVolumeNumber.floatValue();
            Number chargePitchNumber = template.getParameter("charge-sound-pitch", 1.0);
            float chargePitch = chargePitchNumber.floatValue();

            caster.getWorld().playSound(caster.getLocation(), soundCharge, chargeVolume, chargePitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao tocar som de carga para " + getId() + ": " + chargeSoundEffect + ". Erro: " + e.getMessage()); // CORREÇÃO
        }

        new BukkitRunnable() {
            long ticksElapsed = 0;

            @Override
            public void run() {
                if (!caster.isValid() || caster.isDead() || !target.isValid() || target.isDead()) {
                    this.cancel();
                    return;
                }

                try {
                    Particle particleCharge = Particle.valueOf(chargeParticleEffect.toUpperCase());
                    caster.getWorld().spawnParticle(particleCharge, caster.getLocation().add(0, caster.getHeight() / 2, 0),
                            chargeParticleCount, chargeParticleOffset, chargeParticleOffset, chargeParticleOffset, 0.05);
                } catch (IllegalArgumentException e) {
                    // Log silencioso ou de aviso
                }

                if (ticksElapsed >= chargeDurationTicks) {
                    this.cancel();
                    // --- Dash Phase ---
                    performDash(caster, target, dashSpeedMultiplier, maxDashDistance, dashParticleTrail, dashSoundEffect,
                            impactDamageAmount, impactRadius, impactKnockbackStrength, impactParticleEffect, impactSoundEffect);
                    return;
                }
                ticksElapsed++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void performDash(LivingEntity caster, Player target, double dashSpeedMultiplier, double maxDashDistance,
                             String dashParticleTrail, String dashSoundEffect, double impactDamageAmount,
                             double impactRadius, double impactKnockbackStrength, String impactParticleEffect, String impactSoundEffect) {

        Location targetLocation = target.getLocation();
        Vector direction = targetLocation.toVector().subtract(caster.getLocation().toVector()).normalize();

        double distanceToTarget = caster.getLocation().distance(targetLocation);
        double actualDashDistance = Math.min(distanceToTarget, maxDashDistance);

        if (actualDashDistance <= 0.5) {
            performImpact(caster, impactDamageAmount, impactRadius, impactKnockbackStrength, impactParticleEffect, impactSoundEffect);
            return;
        }

        Vector velocity = direction.multiply(dashSpeedMultiplier * 0.4);

        try {
            NamespacedKey soundDashKey = NamespacedKey.minecraft(dashSoundEffect.toLowerCase().replace('.', '_'));
            Sound soundDash = Registry.SOUNDS.get(soundDashKey);
            if (soundDash == null) {
                plugin.getLogger().warning("Som de dash inválido para " + getId() + ": " + dashSoundEffect + ". Usando padrão."); // CORREÇÃO
                soundDash = Sound.ENTITY_PLAYER_ATTACK_SWEEP;
            }
            Number dashVolumeNumber = template.getParameter("dash-sound-volume", 1.0);
            float dashVolume = dashVolumeNumber.floatValue();
            Number dashPitchNumber = template.getParameter("dash-sound-pitch", 1.0);
            float dashPitch = dashPitchNumber.floatValue();

            caster.getWorld().playSound(caster.getLocation(), soundDash, dashVolume, dashPitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao tocar som de dash para " + getId() + ": " + dashSoundEffect + ". Erro: " + e.getMessage()); // CORREÇÃO
        }

        final Location startLocation = caster.getLocation().clone();
        new BukkitRunnable() {
            double distanceTraveled = 0;
            final double tickDistance = velocity.length();

            @Override
            public void run() {
                if (!caster.isValid() || caster.isDead()) {
                    this.cancel();
                    return;
                }

                if (distanceTraveled >= actualDashDistance || caster.getLocation().distanceSquared(target.getLocation()) < impactRadius * impactRadius) {
                    performImpact(caster, impactDamageAmount, impactRadius, impactKnockbackStrength, impactParticleEffect, impactSoundEffect);
                    caster.setVelocity(new Vector(0, 0, 0));
                    this.cancel();
                    return;
                }

                Vector currentDirection = target.getLocation().add(target.getVelocity().multiply(0.5)).toVector().subtract(caster.getLocation().toVector()).normalize();
                caster.setVelocity(currentDirection.multiply(dashSpeedMultiplier * 0.4));
                distanceTraveled += tickDistance;

                try {
                    Particle particleTrail = Particle.valueOf(dashParticleTrail.toUpperCase());
                    caster.getWorld().spawnParticle(particleTrail, caster.getLocation(), 3, 0.1, 0.1, 0.1, 0);
                } catch (IllegalArgumentException e) {
                    // Log silencioso
                }

                if (caster.getLocation().distanceSquared(startLocation) > (maxDashDistance + 5) * (maxDashDistance + 5)) {
                    plugin.getLogger().warning("Dash de " + caster.getName() + " excedeu a distância de segurança.");
                    caster.setVelocity(new Vector(0, 0, 0));
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void performImpact(LivingEntity caster, double damage, double radius, double knockbackStrength, String particleEffect, String impactSoundEffect) {
        // Efeitos de Partícula e Som no local do impacto
        try {
            Particle particleImpact = Particle.valueOf(particleEffect.toUpperCase());
            caster.getWorld().spawnParticle(particleImpact, caster.getLocation(), 20, radius * 0.5, radius * 0.5, radius * 0.5, 0.1);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Partícula de impacto inválida para " + getId() + ": " + particleEffect); // CORREÇÃO
        }
        try {
            NamespacedKey soundImpactKey = NamespacedKey.minecraft(impactSoundEffect.toLowerCase().replace('.', '_'));
            Sound soundImpact = Registry.SOUNDS.get(soundImpactKey);
            if (soundImpact == null) {
                plugin.getLogger().warning("Som de impacto inválido para " + getId() + ": " + impactSoundEffect + ". Usando padrão."); // CORREÇÃO
                soundImpact = Sound.ENTITY_GENERIC_EXPLODE;
            }
            Number impactVolumeNumber = template.getParameter("impact-sound-volume", 1.0);
            float impactVolume = impactVolumeNumber.floatValue();
            Number impactPitchNumber = template.getParameter("impact-sound-pitch", 1.0);
            float impactPitch = impactPitchNumber.floatValue();

            caster.getWorld().playSound(caster.getLocation(), soundImpact, impactVolume, impactPitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao tocar som de impacto para " + getId() + ": " + impactSoundEffect + ". Erro: " + e.getMessage()); // CORREÇÃO
        }

        // Aplica dano e knockback a todos os jogadores no raio de impacto
        for (Entity nearbyEntity : caster.getNearbyEntities(radius, radius, radius)) {
            // CORREÇÃO: Atinge qualquer jogador no raio, não apenas o alvo original.
            // Isso faz com que a habilidade funcione como uma verdadeira AoE.
            if (nearbyEntity instanceof Player) {
                Player pTarget = (Player) nearbyEntity;
                // TODO: Integrar com o sistema PlayerDamageListener para aplicar defesas do jogador
                pTarget.damage(damage, caster);

                if (knockbackStrength > 0) {
                    Vector direction = pTarget.getLocation().toVector().subtract(caster.getLocation().toVector()).normalize();
                    direction.setY(0.5); // Joga um pouco para cima
                    pTarget.setVelocity(direction.multiply(knockbackStrength));
                }
                plugin.getLogger().info(caster.getName() + " impactou " + template.getDisplayName(null) + " em " + pTarget.getName()); // Updated log message
            }
        }
    }
}