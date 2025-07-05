package me.ray.aethelgardRPG.modules.spell.animation;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.spell.animation.effects.ParticleEffect;
import me.ray.aethelgardRPG.modules.spell.animation.effects.SoundEffect;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastContext;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class AnimationFrame {
    private final List<ParticleEffect> particleEffects;
    private final List<SoundEffect> soundEffects;
    // Adicionar outros tipos de efeitos (MovementEffect, etc.)

    public AnimationFrame() {
        this.particleEffects = new ArrayList<>();
        this.soundEffects = new ArrayList<>();
    }

    public AnimationFrame addParticleEffect(ParticleEffect effect) {
        this.particleEffects.add(effect);
        return this;
    }

    public AnimationFrame addSoundEffect(SoundEffect effect) {
        this.soundEffects.add(effect);
        return this;
    }

    public void execute(SpellCastContext context, AethelgardRPG plugin) {
        Location casterLocation = context.getCaster().getLocation();
        // Executa todos os efeitos de partícula
        particleEffects.forEach(effect -> effect.play(casterLocation));

        // Executa todos os efeitos sonoros
        soundEffects.forEach(effect -> effect.play(casterLocation));
    }
}