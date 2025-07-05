package me.ray.aethelgardRPG.modules.spell.animation;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastContext;

public interface Animation {

    /**
     * Executa a animação.
     * @param context O contexto do cast da spell.
     * @param plugin A instância do plugin, necessária para agendar tarefas.
     */
    void run(SpellCastContext context, AethelgardRPG plugin);
}
