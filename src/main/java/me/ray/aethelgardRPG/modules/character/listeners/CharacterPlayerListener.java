package me.ray.aethelgardRPG.modules.character.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.session.SessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener para eventos de jogador relacionados ao módulo de personagem.
 * Gerencia o ciclo de vida dos dados do personagem (carregar ao entrar, salvar ao sair).
 */
public class CharacterPlayerListener implements Listener {

    private final AethelgardRPG plugin;
    private final CharacterModule characterModule;
    private final SessionManager sessionManager;

    public CharacterPlayerListener(AethelgardRPG plugin, CharacterModule characterModule) {
        this.plugin = plugin;
        this.characterModule = characterModule;
        this.sessionManager = plugin.getSessionManager();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Cria a sessão do jogador, que por sua vez carrega os dados do personagem.
        sessionManager.createSession(player);

        // Abre a tela de seleção de personagem para o jogador que acabou de entrar.
        characterModule.openCharacterSelectionFor(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Salva os dados do personagem ativo e limpa a sessão do jogador.
        // Esta é a correção crucial para salvar o inventário e outros dados ao sair.
        sessionManager.destroySession(player);

        // Limpa o cache de idioma para o jogador que saiu.
        plugin.getLanguageManager().clearPlayerLanguageCache(player);
    }
}