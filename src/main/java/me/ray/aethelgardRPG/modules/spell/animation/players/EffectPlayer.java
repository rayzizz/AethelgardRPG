package me.ray.aethelgardRPG.modules.spell.animation.players;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastContext;

// Interface para classes que "tocam" um tipo específico de efeito (ex: partículas, sons)
public interface EffectPlayer<T> { // T seria o tipo de efeito, ex: ParticleEffect

    void play(T effect, SpellCastContext context, AethelgardRPG plugin);
}
