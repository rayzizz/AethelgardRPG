package me.ray.aethelgardRPG.modules.spell.animation;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastContext;

public class AnimationManager implements AnimationAPI {

    private final AethelgardRPG plugin;

    public AnimationManager(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public void playAnimation(Animation animation, SpellCastContext context) {
        if (animation == null) {
            plugin.getLogger().warning("Tentativa de executar uma animação nula.");
            return;
        }
        animation.run(context, plugin); // Passa o plugin para BukkitRunnables
    }
}
