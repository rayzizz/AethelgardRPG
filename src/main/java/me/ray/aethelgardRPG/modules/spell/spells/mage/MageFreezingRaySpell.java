package me.ray.aethelgardRPG.modules.spell.spells.mage;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.utils.TargetFinder;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Optional;

public class MageFreezingRaySpell extends BaseSpell {

    private static final double MAX_RANGE = 20.0;
    private static final double DAMAGE_AMOUNT = 10.0;
    private static final int SLOWNESS_DURATION_TICKS = 100; // 5 segundos (20 ticks/segundo)
    private static final int SLOWNESS_AMPLIFIER = 2; // Lentidão III

    private final TargetFinder targetFinder;

    public MageFreezingRaySpell(AethelgardRPG plugin) {
        super(plugin,
                "mage_freezing_ray",
                "spell.mage.freezing_ray.name",
                "spell.mage.freezing_ray.description",
                12, // Cooldown
                30, // Custo de mana
                RPGClass.MAGO,
                1,  // Nível Requerido (NOVO)
                MAX_RANGE
        );
        this.targetFinder = new TargetFinder(plugin); // Utiliza o TargetFinder que você já possui
        this.animation = createAnimation(); // Define a animação para esta spell
    }

    private AnimationSequence createAnimation() {
        AnimationSequence sequence = new AnimationSequence();

        // Frame 1: Som inicial do cast
        AnimationFrame frame1 = new AnimationFrame()
                .addSoundEffect(new SoundEffect(Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.7f, 1.5f));
        sequence.addFrame(frame1, 0L); // Executa imediatamente

        // Frame 2: Partículas do raio (simulado, pois o raio é desenhado no execute)
        // Este frame é mais para um efeito inicial ou final, o raio em si é dinâmico
        AnimationFrame frame2 = new AnimationFrame()
                .addParticleEffect(new ParticleEffect(Particle.SNOWFLAKE, 10, 0.5, 0.5, 0.5, 0.1, null));
        sequence.addFrame(frame2, 5L); // 5 ticks após o frame 1

        return sequence;
    }

    @Override
    public SpellCastResult execute(SpellCastContext context) {
        Player caster = context.getCaster();

        Optional<LivingEntity> targetOpt = targetFinder.findTarget(caster, MAX_RANGE);

        if (targetOpt.isEmpty()) {
            caster.sendMessage(plugin.getMessage(caster, "spell.mage.freezing_ray.no_target"));
            // MELHORIA: Usa o valor correto do enum após a refatoração.
            return SpellCastResult.FAIL_NO_TARGET;
        }

        LivingEntity target = targetOpt.get();

        // Efeitos visuais e sonoros que são dinâmicos (dependem do alvo)
        drawRay(caster.getEyeLocation(), target.getEyeLocation().subtract(0,0.2,0));
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0F, 0.8F);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0F, 1.0F);

        // Dano e efeito de lentidão
        target.damage(DAMAGE_AMOUNT, caster);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SLOWNESS_DURATION_TICKS, SLOWNESS_AMPLIFIER, false, true, true));

        caster.sendMessage(plugin.getMessage(caster, "spell.mage.freezing_ray.success", target.getName()));

        return SpellCastResult.SUCCESS;
    }

    private void drawRay(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);
        for (double d = 0; d < distance; d += 0.25) { // Densidade das partículas
            Location particleLoc = start.clone().add(direction.clone().multiply(d));
            start.getWorld().spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0, 0, 0, 0);
        }
    }
}