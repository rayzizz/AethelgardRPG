package me.ray.aethelgardRPG.modules.character.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final AethelgardRPG plugin;
    private final CharacterModule characterModule;

    public PlayerJoinListener(AethelgardRPG plugin, CharacterModule characterModule) {
        this.plugin = plugin;
        this.characterModule = characterModule;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 1. Cria uma sessão para o jogador.
        plugin.getSessionManager().createSession(player);

        // 2. Limpa o inventário para garantir um estado inicial limpo antes da seleção.
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // 3. Inicia o fluxo de seleção de personagem.
        // O jogador será teleportado para um lobby/local de seleção se necessário.
        // A lógica dentro deste método irá abrir a GUI de seleção ou criação.
        characterModule.openCharacterSelectionFor(player);
    }
}