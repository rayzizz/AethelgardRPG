package me.ray.aethelgardRPG.session;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.character.api.CharacterAPI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia as sessões de todos os jogadores online.
 * Atua como a ponte entre o UUID de um jogador e seu personagem ativo.
 */
public class SessionManager {

    private final AethelgardRPG plugin;
    private final Map<UUID, PlayerSession> activeSessions = new ConcurrentHashMap<>();

    public SessionManager(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    /**
     * Cria e registra uma nova sessão para um jogador quando ele entra no servidor.
     * @param player O jogador que entrou.
     */
    public void createSession(Player player) {
        activeSessions.put(player.getUniqueId(), new PlayerSession(player.getUniqueId()));
        plugin.getLogger().info("Sessão criada para a conta: " + player.getName());
    }

    /**
     * Destrói a sessão de um jogador, salvando seus dados e removendo-os do cache.
     * Chamado quando o jogador sai do servidor.
     * @param player O jogador cuja sessão será destruída.
     */
    public void destroySession(Player player) {
        UUID playerUUID = player.getUniqueId();

        // Pega o personagem ativo da sessão que está sendo encerrada.
        Optional<CharacterData> characterDataOpt = getActiveCharacter(player);

        // Se houver um personagem ativo, salva seus dados de forma assíncrona.
        characterDataOpt.ifPresent(characterData -> {
            CharacterAPI characterAPI = plugin.getCharacterAPI();
            if (characterAPI != null) {
                plugin.getLogger().info("Salvando dados do personagem para " + player.getName() + " ao sair...");

                PlayerInventory inv = player.getInventory();

                // *** CORREÇÃO APLICADA AQUI ***
                // Usamos getStorageContents() que retorna um array de 36 slots (inventário principal + hotbar),
                // que é o tamanho esperado pelo inventário do CharacterData.
                // O método getContents() retorna 41 slots (incluindo armadura), o que causava o erro.
                characterData.setInventoryContents(inv.getStorageContents());

                // A armadura é salva separadamente e corretamente.
                characterData.setArmorContents(inv.getArmorContents());

                // Atualiza a última localização e o tempo de combate/sprint
                characterData.setLastLocation(player.getLocation());
                characterData.setLastCombatTimeMillis(characterData.getLastCombatTimeMillis()); // Mantém o último tempo de combate
                characterData.setLastSprintTimeMillis(characterData.getLastSprintTimeMillis()); // Mantém o último tempo de sprint

                characterAPI.saveCharacterDataAsync(characterData);
            }
        });

        // Remove o jogador do mapa de sessões.
        activeSessions.remove(playerUUID);
        plugin.getLogger().info("Sessão para " + player.getName() + " destruída.");
    }

    /**
     * Remove a sessão de um jogador quando ele sai.
     * Este método é mantido para casos onde a remoção é necessária sem salvamento.
     * @param player O jogador que saiu.
     */
    public void removeSession(Player player) {
        activeSessions.remove(player.getUniqueId());
        plugin.getLogger().info("Sessão removida para a conta: " + player.getName());
    }

    /**
     * Obtém a sessão de um jogador.
     * @param player O jogador.
     * @return Um Optional contendo a PlayerSession.
     */
    public Optional<PlayerSession> getSession(Player player) {
        return Optional.ofNullable(activeSessions.get(player.getUniqueId()));
    }

    /**
     * Obtém os dados do personagem ativo para um jogador.
     * Este é o principal método que outros módulos usarão para acessar os dados do personagem.
     * @param player O jogador.
     * @return Um Optional contendo o CharacterData do personagem ativo.
     */
    public Optional<CharacterData> getActiveCharacter(Player player) {
        return getSession(player).flatMap(PlayerSession::getActiveCharacter);
    }

    /**
     * Define o personagem ativo para a sessão de um jogador.
     * @param player O jogador.
     * @param characterData Os dados do personagem a serem ativados.
     */
    public void setActiveCharacter(Player player, CharacterData characterData) {
        getSession(player).ifPresent(session -> {
            session.setActiveCharacter(characterData);
            if (characterData != null) {
                plugin.getLogger().info("Personagem '" + characterData.getCharacterName() + "' (ID: " + characterData.getCharacterId() + ") definido como ativo para " + player.getName());
            } else {
                plugin.getLogger().info("Nenhum personagem ativo definido para " + player.getName() + " (retornou à seleção).");
            }
        });
    }
}