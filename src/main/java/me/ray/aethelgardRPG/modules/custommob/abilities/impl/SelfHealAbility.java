package me.ray.aethelgardRPG.modules.custommob.abilities.impl;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;
import me.ray.aethelgardRPG.modules.custommob.MobAbility;
import me.ray.aethelgardRPG.modules.custommob.abilities.MobAbilityTemplate;
import me.ray.aethelgardRPG.modules.custommob.mobs.ActiveMobInfo;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class SelfHealAbility implements MobAbility {

    private final MobAbilityTemplate template;
    private final AethelgardRPG plugin;
    private final CustomMobModule customMobModule;

    public SelfHealAbility(MobAbilityTemplate template, AethelgardRPG plugin) {
        this.template = template;
        this.plugin = plugin;
        this.customMobModule = plugin.getModuleManager().getModule(CustomMobModule.class);
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
        if (customMobModule == null) {
            plugin.getLogger().severe("CustomMobModule not found for SelfHealAbility!");
            return;
        }

        ActiveMobInfo mobInfo = customMobModule.getCustomMobManager().getActiveMobInfo(caster.getUniqueId());
        if (mobInfo == null) return;

        double healPercentage = template.getParameter("heal-percentage", 0.0);
        double healAmount = template.getParameter("heal-amount", 20.0);
        String particleEffectName = template.getParameter("particle-effect", "HEART");
        String soundEffectName = template.getParameter("sound-effect", "ENTITY_ZOMBIE_VILLAGER_CURE");
        float soundVolume = ((Number) template.getParameter("sound-volume", 1.0)).floatValue();
        float soundPitch = ((Number) template.getParameter("sound-pitch", 1.2)).floatValue();

        double currentHealth = mobInfo.getCurrentHealth();
        double maxHealth = mobInfo.getBaseData().getMaxHealth();

        if (currentHealth >= maxHealth) return;

        double amountToHeal;
        if (healPercentage > 0) {
            amountToHeal = maxHealth * (healPercentage / 100.0);
        } else {
            amountToHeal = healAmount;
        }

        double newHealth = Math.min(maxHealth, currentHealth + amountToHeal);
        customMobModule.setCustomMobCurrentHealth(caster.getUniqueId(), newHealth);

        // --- NOVA ANIMAÇÃO DE CURA ---
        try {
            String soundKey = soundEffectName.toLowerCase().replace('_', '.');
            Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundKey));
            if (sound != null) {
                caster.getWorld().playSound(caster.getLocation(), sound, soundVolume, soundPitch);
            } else {
                plugin.getLogger().warning("Invalid sound for " + getId() + ": " + soundEffectName); // CORREÇÃO
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao tocar som para " + getId() + ": " + soundEffectName + ". Erro: " + e.getMessage()); // CORREÇÃO
        }

        try {
            Particle particle = Particle.valueOf(particleEffectName.toUpperCase());
            new BukkitRunnable() {
                private int duration = 40; // 2 segundos de efeito
                private double yOffset = 0;
                private double angle = 0;

                @Override
                public void run() {
                    if (!caster.isValid() || duration <= 0) {
                        this.cancel();
                        return;
                    }

                    // Efeito de levitação suave
                    if (duration > 20) caster.setVelocity(new Vector(0, 0.1, 0));

                    // Partículas em espiral ascendente
                    angle += Math.PI / 8;
                    double radius = 0.8;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    caster.getWorld().spawnParticle(particle, caster.getLocation().add(x, yOffset, z), 1, 0, 0, 0, 0);

                    yOffset += 0.05;
                    duration--;
                }
            }.runTaskTimer(plugin, 0L, 1L);

        } catch (Exception e) {
            plugin.getLogger().warning("Invalid particle for " + getId() + ": " + particleEffectName); // CORREÇÃO
        }

        plugin.getLogger().info(caster.getName() + " used " + template.getDisplayName(null) + " healing for " + String.format("%.1f", amountToHeal) + " health.");
    }
}