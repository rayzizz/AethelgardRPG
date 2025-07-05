package me.ray.aethelgardRPG.modules.spell.animation;

import me.ray.aethelgardRPG.modules.spell.spells.SpellCastContext;

public interface AnimationAPI {

    /**
     * Executa uma animação definida.
     * @param animation A animação a ser executada.
     * @param context O contexto do cast da spell (para caster, alvos, etc.).
     */
    void playAnimation(Animation animation, SpellCastContext context);

}
