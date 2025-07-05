package me.ray.aethelgardRPG.modules.spell.api;

import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import me.ray.aethelgardRPG.modules.spell.combo.Combo;
import me.ray.aethelgardRPG.modules.spell.spells.Spell;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface SpellAPI {

    /**
     * Tenta lançar uma spell diretamente (útil para itens ou comandos, não para combos).
     * @param player O jogador que está lançando.
     * @param spellId O ID da spell.
     * @return true se a spell foi lançada com sucesso (ou iniciada), false caso contrário.
     */
    boolean castSpellById(Player player, String spellId);

}
