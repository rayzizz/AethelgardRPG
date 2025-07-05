package me.ray.aethelgardRPG.api;

import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.character.api.CharacterAPI;
import me.ray.aethelgardRPG.modules.custommob.api.CustomMobAPI;
import me.ray.aethelgardRPG.modules.item.api.ItemAPI;
import me.ray.aethelgardRPG.modules.skill.api.SkillAPI;
import me.ray.aethelgardRPG.modules.spell.api.SpellAPI;
import me.ray.aethelgardRPG.core.utils.TargetFinder;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface AethelgardAPI {

    SkillAPI getSkillAPI();
    ItemAPI getItemAPI();
    CustomMobAPI getCustomMobAPI();
    CharacterAPI getCharacterAPI();
    SpellAPI getSpellAPI();
    TargetFinder getTargetFinder();

    /**
     * Obtém os dados do personagem ATIVO de um jogador.
     * @param player O jogador.
     * @return Um Optional contendo CharacterData se o jogador estiver online e com um personagem selecionado.
     */
    Optional<CharacterData> getCharacterData(Player player);

}