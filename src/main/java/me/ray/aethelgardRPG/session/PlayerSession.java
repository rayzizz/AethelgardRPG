package me.ray.aethelgardRPG.session;

import me.ray.aethelgardRPG.modules.character.CharacterData;

import java.util.Optional;
import java.util.UUID;

/**
 * Representa a sessão de um jogador logado.
 * Contém o UUID da conta e os dados do personagem atualmente selecionado.
 */
public class PlayerSession {

    private final UUID accountUuid;
    private CharacterData activeCharacter;

    public PlayerSession(UUID accountUuid) {
        this.accountUuid = accountUuid;
        this.activeCharacter = null; // Começa sem personagem ativo
    }

    public UUID getAccountUuid() {
        return accountUuid;
    }

    public Optional<CharacterData> getActiveCharacter() {
        return Optional.ofNullable(activeCharacter);
    }

    public void setActiveCharacter(CharacterData activeCharacter) {
        this.activeCharacter = activeCharacter;
    }

    public boolean hasActiveCharacter() {
        return activeCharacter != null;
    }
}