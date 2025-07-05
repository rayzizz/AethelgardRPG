package me.ray.aethelgardRPG.modules.spell;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import me.ray.aethelgardRPG.modules.spell.combo.Combo;
import me.ray.aethelgardRPG.modules.spell.spells.Spell;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerSpellManager {

    private final AethelgardRPG plugin;
    private final SpellRegistry spellRegistry;

    public PlayerSpellManager(AethelgardRPG plugin, SpellRegistry spellRegistry) {
        this.plugin = plugin;
        this.spellRegistry = spellRegistry;
    }

    public boolean learnSpell(Player player, String spellId) {
        Optional<Spell> spellOpt = spellRegistry.getSpellById(spellId);
        if (spellOpt.isEmpty()) {
            plugin.getLogger().warning("Tentativa de aprender magia com ID inválido: " + spellId + " para " + player.getName());
            player.sendMessage(plugin.getMessage(player, "spell.learn.fail.invalid_spell", spellId));
            return false;
        }

        Optional<CharacterData> playerDataOpt = plugin.getCharacterData(player);
        if (playerDataOpt.isPresent()) {
            CharacterData characterData = playerDataOpt.get();
            if (characterData.getKnownSpellIds().add(spellId.toLowerCase())) { // Salva em minúsculas
                player.sendMessage(plugin.getMessage(player, "spell.learn.success", spellOpt.get().getDisplayName(player)));
                plugin.getCharacterAPI().saveCharacterDataAsync(characterData);
                return true;
            } else {
                player.sendMessage(plugin.getMessage(player, "spell.learn.already_known", spellOpt.get().getDisplayName(player)));
                return true; // Já conhece, então tecnicamente não é uma falha.
            }
        }
        plugin.getLogger().warning("PlayerData ausente para " + player.getName() + " ao tentar aprender magia.");
        player.sendMessage(plugin.getMessage(player, "spell.error.player_data_missing"));
        return false;
    }

    public boolean unlearnSpell(Player player, String spellId) {
        Optional<Spell> spellOpt = spellRegistry.getSpellById(spellId);
        if (spellOpt.isEmpty()) {
            plugin.getLogger().warning("Tentativa de desaprender magia com ID inválido: " + spellId + " para " + player.getName());
            player.sendMessage(plugin.getMessage(player, "spell.unlearn.fail.invalid_spell", spellId));
            return false;
        }

        Optional<CharacterData> playerDataOpt = plugin.getCharacterData(player);
        if (playerDataOpt.isPresent()) {
            CharacterData characterData = playerDataOpt.get();
            if (characterData.getKnownSpellIds().remove(spellId.toLowerCase())) {
                // Remove a magia de todos os combos atribuídos
                characterData.unassignActiveSpell(spellId.toLowerCase());

                player.sendMessage(plugin.getMessage(player, "spell.unlearn.success", spellOpt.get().getDisplayName(player)));
                plugin.getCharacterAPI().saveCharacterDataAsync(characterData);
                return true;
            } else {
                player.sendMessage(plugin.getMessage(player, "spell.unlearn.not_known", spellOpt.get().getDisplayName(player)));
                return false; // Não conhecia a magia, então é uma falha.
            }
        }
        plugin.getLogger().warning("PlayerData ausente para " + player.getName() + " ao tentar desaprender magia.");
        player.sendMessage(plugin.getMessage(player, "spell.error.player_data_missing"));
        return false;
    }


    public boolean assignSpellToCombo(Player player, Combo combo, String spellId) {
        Optional<CharacterData> playerDataOpt = plugin.getCharacterData(player);
        if (playerDataOpt.isEmpty()) {
            player.sendMessage(plugin.getMessage(player, "spell.error.player_data_missing"));
            return false;
        }

        CharacterData characterData = playerDataOpt.get();
        RPGClass playerClass = characterData.getSelectedClass();

        if (playerClass == null || playerClass == RPGClass.NONE) {
            player.sendMessage(plugin.getMessage(player, "spell.assign.fail.no_class"));
            return false;
        }

        Optional<Spell> spellOpt = spellRegistry.getSpellById(spellId);
        if (spellOpt.isEmpty()) {
            player.sendMessage(plugin.getMessage(player, "spell.assign.fail.invalid_spell", spellId));
            return false;
        }

        Spell spellToAssign = spellOpt.get();

        if (!characterData.getKnownSpellIds().contains(spellId.toLowerCase())) {
            player.sendMessage(plugin.getMessage(player, "spell.assign.fail.not_known", spellToAssign.getDisplayName(player)));
            return false;
        }

        if (spellToAssign.getRequiredRPGClass() != playerClass) {
            player.sendMessage(plugin.getMessage(player, "spell.assign.fail.wrong_class", spellToAssign.getDisplayName(player), playerClass.getDisplayName(player)));
            return false;
        }

        characterData.assignActiveSpell(playerClass, combo, spellId.toLowerCase());
        player.sendMessage(plugin.getMessage(player, "spell.assign.success", spellToAssign.getDisplayName(player), combo.getDisplayString()));
        plugin.getCharacterAPI().saveCharacterDataAsync(characterData);
        return true;
    }

    public boolean unassignSpellFromCombo(Player player, Combo combo) {
        Optional<CharacterData> playerDataOpt = plugin.getCharacterData(player);
        if (playerDataOpt.isEmpty()) {
            player.sendMessage(plugin.getMessage(player, "spell.error.player_data_missing"));
            return false;
        }

        CharacterData characterData = playerDataOpt.get();
        RPGClass playerClass = characterData.getSelectedClass();

        if (playerClass == null || playerClass == RPGClass.NONE) {
            return false;
        }

        Map<Combo, String> assignments = characterData.getActiveSpellAssignments().get(playerClass);

        if (assignments != null && assignments.containsKey(combo)) {
            String unassignedSpellId = assignments.remove(combo);
            spellRegistry.getSpellById(unassignedSpellId).ifPresent(spell ->
                    player.sendMessage(plugin.getMessage(player, "spell.unassign.success", spell.getDisplayName(player), combo.getDisplayString()))
            );
            plugin.getCharacterAPI().saveCharacterDataAsync(characterData);
            return true;
        }
        return false;
    }

    // --- NOVO MÉTODO ---
    /**
     * Retorna uma lista de magias que o jogador conhece para sua classe atual,
     * mas que ainda não foram atribuídas a nenhum combo.
     * @param player O jogador.
     * @return Uma lista de magias não atribuídas.
     */
    public List<Spell> getUnassignedKnownSpellsForClass(Player player) {
        return plugin.getCharacterData(player)
                .map(playerData -> {
                    RPGClass playerClass = playerData.getSelectedClass();
                    if (playerClass == null || playerClass == RPGClass.NONE) {
                        return Collections.<Spell>emptyList();
                    }

                    // Pega os IDs de todas as magias atribuídas para a classe atual
                    Set<String> assignedSpellIds = playerData.getActiveSpellAssignments()
                            .getOrDefault(playerClass, Collections.emptyMap())
                            .values().stream()
                            .collect(Collectors.toSet());

                    // Filtra a lista de magias conhecidas, removendo as que já estão atribuídas
                    return playerData.getKnownSpellIds().stream()
                            .filter(spellId -> !assignedSpellIds.contains(spellId)) // Apenas as não atribuídas
                            .map(spellRegistry::getSpellById)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .filter(spell -> spell.getRequiredRPGClass() == playerClass)
                            .collect(Collectors.toList());
                })
                .orElse(Collections.emptyList());
    }


    public List<Spell> getKnownSpellsForClass(Player player) {
        return plugin.getCharacterData(player)
                .map(playerData -> {
                    RPGClass playerClass = playerData.getSelectedClass();
                    if (playerClass == null || playerClass == RPGClass.NONE) {
                        return Collections.<Spell>emptyList();
                    }
                    return playerData.getKnownSpellIds().stream()
                            .map(spellRegistry::getSpellById)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .filter(spell -> spell.getRequiredRPGClass() == playerClass)
                            .collect(Collectors.toList());
                })
                .orElse(Collections.emptyList());
    }

    public Optional<Spell> getActiveSpellForCombo(Player player, Combo combo) {
        return plugin.getCharacterData(player)
                .flatMap(playerData -> {
                    RPGClass playerClass = playerData.getSelectedClass();
                    if (playerClass == null || playerClass == RPGClass.NONE) {
                        return Optional.empty();
                    }
                    return Optional.ofNullable(playerData.getActiveSpellAssignments().get(playerClass))
                            .flatMap(assignments -> Optional.ofNullable(assignments.get(combo)))
                            .flatMap(spellRegistry::getSpellById);
                });
    }
}