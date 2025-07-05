package me.ray.aethelgardRPG.modules.character.api;

import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CharacterAPI {

    /**
     * Obtém os dados do personagem ATIVO de um jogador.
     * @param player O jogador.
     * @return Um Optional contendo CharacterData se o jogador estiver online e com um personagem selecionado.
     */
    Optional<CharacterData> getCharacterData(Player player);

    /**
     * Obtém os dados do personagem ATIVO de um jogador pelo UUID.
     * @param playerUUID O UUID do jogador.
     * @return Um Optional contendo CharacterData se o jogador estiver online e com um personagem selecionado.
     */
    Optional<CharacterData> getCharacterData(UUID playerUUID);

    /**
     * Salva os dados do personagem de forma assíncrona.
     * @param characterData Os dados do personagem a serem salvos.
     * @return Um CompletableFuture indicando o sucesso da operação.
     */
    CompletableFuture<Void> saveCharacterDataAsync(CharacterData characterData);

    /**
     * Adiciona experiência ao personagem de um jogador.
     * @param player O jogador.
     * @param amount A quantidade de experiência a ser adicionada.
     */
    void addExperience(Player player, double amount);

    /**
     * Obtém o nível atual do personagem de um jogador.
     * @param player O jogador.
     * @return O nível do personagem, ou 0 se os dados não forem encontrados.
     */
    int getPlayerLevel(Player player);

    /**
     * Obtém a classe de RPG do personagem de um jogador.
     * @param player O jogador.
     * @return Um Optional contendo a classe de RPG, ou vazio se os dados não forem encontrados.
     */
    Optional<RPGClass> getPlayerClass(Player player);
}