package me.ray.aethelgardRPG.modules.spell.spells.warrior;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import me.ray.aethelgardRPG.modules.spell.animation.AnimationFrame;
import me.ray.aethelgardRPG.modules.spell.animation.AnimationSequence;
import me.ray.aethelgardRPG.modules.spell.animation.effects.ParticleEffect;
import me.ray.aethelgardRPG.modules.spell.animation.effects.SoundEffect;
import me.ray.aethelgardRPG.modules.spell.spells.BaseSpell;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastContext;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastResult;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public class WarriorSpinningFurySpell extends BaseSpell {

    private static final double DAMAGE_RADIUS = 4.0; // Raio de alcance da habilidade
    private static final double DAMAGE_AMOUNT = 8.0; // Dano base

    public WarriorSpinningFurySpell(AethelgardRPG plugin) {
        super(plugin,
                "warrior_spinning_fury",
                "spell.warrior.spinning_fury.name",
                "spell.warrior.spinning_fury.description",
                15, // Cooldown
                25, // Custo de mana
                RPGClass.GUERREIRO,
                1,  // Nível Requerido (NOVO)
                0   // Alcance
        );
        this.animation = createAnimation();
    }

    private AnimationSequence createAnimation() {
        AnimationSequence sequence = new AnimationSequence();

        // Frame 1: Som de ataque e partículas de varredura
        AnimationFrame frame1 = new AnimationFrame()
                .addSoundEffect(new SoundEffect(Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 1.0f))
                .addParticleEffect(new ParticleEffect(Particle.SWEEP_ATTACK, 10, 0.5, 0.5, 0.5, 0.1, null));
        sequence.addFrame(frame1, 0L); // Executa imediatamente

        // Frame 2: Partículas de crítico um pouco depois
        AnimationFrame frame2 = new AnimationFrame()
                .addParticleEffect(new ParticleEffect(Particle.CRIT, 20, 1.0, 0.5, 1.0, 0.1, null));
        sequence.addFrame(frame2, 5L); // 5 ticks após o frame 1

        return sequence;
    }

    @Override
    public SpellCastResult execute(SpellCastContext context) {
        Player caster = context.getCaster();

        // Efeitos visuais e sonoros (já definidos na animação, mas pode ter extras aqui)
        // caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.0F); // Removido, está na animação
        // caster.getWorld().spawnParticle(Particle.SWEEP_ATTACK, caster.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1); // Removido, está na animação
        // caster.getWorld().spawnParticle(Particle.CRIT, caster.getLocation().add(0, 1.2, 0), 20, 1.0, 0.5, 1.0, 0.1); // Removido, está na animação

        // Lógica de dano
        List<Entity> nearbyEntities = caster.getNearbyEntities(DAMAGE_RADIUS, DAMAGE_RADIUS, DAMAGE_RADIUS);
        int targetsHit = 0;
        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity && !entity.equals(caster) && entity.isValid()) {
                // Adicionar verificações de facção/PvP se necessário
                LivingEntity target = (LivingEntity) entity;
                target.damage(DAMAGE_AMOUNT, caster);
                target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, target.getHeight() / 2, 0), (int) (DAMAGE_AMOUNT / 2), 0.3, 0.3, 0.3, 0);
                targetsHit++;
            }
        }

        if (targetsHit > 0) {
            caster.sendMessage(plugin.getMessage(caster, "spell.warrior.spinning_fury.success", targetsHit));
        } else {
            caster.sendMessage(plugin.getMessage(caster, "spell.warrior.spinning_fury.no_targets_hit"));
        }

        return SpellCastResult.SUCCESS;
    }
}