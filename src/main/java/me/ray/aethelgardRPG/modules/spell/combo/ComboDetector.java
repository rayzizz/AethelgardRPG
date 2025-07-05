package me.ray.aethelgardRPG.modules.spell.combo;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.utils.PDCKeys; // Importar a classe de chaves
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import me.ray.aethelgardRPG.modules.item.ItemModule;
import me.ray.aethelgardRPG.modules.item.items.ItemManager;
import me.ray.aethelgardRPG.modules.spell.PlayerSpellManager;
import me.ray.aethelgardRPG.modules.spell.SpellManager;
import me.ray.aethelgardRPG.modules.spell.spells.Spell;
import me.ray.aethelgardRPG.modules.spell.spells.SpellCastResult;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class ComboDetector {

    private final AethelgardRPG plugin;
    private final SpellManager spellManager;
    private final PlayerSpellManager playerSpellManager;
    private final Map<UUID, PlayerComboInput> playerInputs;
    private final Map<UUID, Combo> lockedInCombos;
    private final long maxTimeBetweenClicksMillis;
    private final ItemManager itemManager;
    private final ItemModule itemModule; // Armazena a referência ao ItemModule

    public ComboDetector(AethelgardRPG plugin, SpellManager spellManager, PlayerSpellManager playerSpellManager) {
        this.plugin = plugin;
        this.spellManager = spellManager;
        this.playerSpellManager = playerSpellManager;
        this.playerInputs = new HashMap<>();
        this.lockedInCombos = new HashMap<>();
        long maxComboWindowMillis = plugin.getConfigManager().getMainConfig().getLong("spell.combo.max-window-millis", 1500L);
        this.maxTimeBetweenClicksMillis = plugin.getConfigManager().getMainConfig().getLong("spell.combo.max-time-between-clicks-millis", maxComboWindowMillis / 2);

        // Obtém a referência ao ItemModule
        ItemModule loadedItemModule = plugin.getModuleManager().getModule(ItemModule.class);
        if (loadedItemModule != null) {
            this.itemModule = loadedItemModule;
            this.itemManager = loadedItemModule.getItemManager();
        } else {
            this.itemModule = null;
            this.itemManager = null; // Garante que itemManager também seja nulo se o módulo estiver faltando
            plugin.getLogger().log(Level.SEVERE, "[ComboDetector] ItemModule não encontrado! A validação de arma para magias pode ser comprometida.");
        }
    }

    /**
     * Verifica se o jogador está em um estado válido para iniciar ou continuar um combo.
     * Isso inclui ter uma classe atribuída e segurar a arma correta para essa classe.
     *
     * @param player O jogador.
     * @return true se o jogador pode performar combos, false caso contrário.
     */
    private boolean canPerformCombos(Player player) {
        // Garante que o ItemModule esteja carregado antes de tentar usá-lo
        if (itemModule == null) {
            // Já logado no construtor, não precisa spammar aqui.
            return false;
        }

        // 1. Verificar PlayerData e Classe
        Optional<CharacterData> playerDataOpt = plugin.getCharacterData(player);
        if (playerDataOpt.isEmpty()) {
            // Dados do jogador não carregados, não é possível validar a classe.
            return false;
        }
        CharacterData characterData = playerDataOpt.get();
        RPGClass playerClass = characterData.getSelectedClass();

        if (playerClass == RPGClass.NONE) {
            // Jogador não tem classe atribuída.
            return false;
        }

        // 2. Verificar Item na Mão
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // Se a mão estiver vazia, não há combos.
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            return false;
        }

        // Verificar se é um item RPG
        if (!itemModule.isRPGItem(itemInHand)) {
            // Se não for um item RPG, não é uma arma válida para conjuração de magias.
            return false;
        }

        // 3. Verificar Requisito de Classe da Arma
        ItemMeta meta = itemInHand.getItemMeta();
        if (meta == null) {
            // Não deveria acontecer para itens RPG, mas é uma verificação defensiva.
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        // *** CORREÇÃO APLICADA AQUI ***
        // Acessa a chave diretamente da classe PDCKeys
        NamespacedKey itemClassKey = PDCKeys.REQUIRED_CLASS_KEY;

        if (pdc.has(itemClassKey, PersistentDataType.STRING)) {
            String itemClassString = pdc.get(itemClassKey, PersistentDataType.STRING);
            if (itemClassString != null) {
                RPGClass itemRequiredClass = RPGClass.fromString(itemClassString);
                // Verifica se a classe exigida pelo item corresponde à classe atual do jogador
                return itemRequiredClass == playerClass;
            } else {
                // itemClassString é nulo, o que significa que a tag NBT existe, mas está vazia.
                // Tratar como inválido para armas específicas de classe.
                return false;
            }
        } else {
            // O item não possui uma tag de requisito de classe.
            // Para magias que exigem armas específicas, isso significa que não é a arma correta.
            // Assumimos que todas as armas de conjuração de magia *devem* ter essa tag.
            return false;
        }
    }

    public void recordClick(Player player, ClickType clickType) {
        if (!canPerformCombos(player)) {
            // Se o jogador não pode performar combos (classe/arma errada), limpa qualquer input/combo travado existente.
            // Isso garante que o display do combo desapareça e nenhuma entrada adicional seja processada.
            if (lockedInCombos.remove(player.getUniqueId()) != null) {
                player.sendTitle("", "", 0, 1, 0); // Limpa o título do combo travado
            }
            PlayerComboInput existingInput = playerInputs.get(player.getUniqueId());
            if (existingInput != null && !existingInput.isEmpty()) {
                existingInput.clear();
                player.sendTitle("", "", 0, 1, 0); // Limpa o display do input atual
            }
            return; // Interrompe o processamento deste clique
        }

        PlayerComboInput inputState = playerInputs.computeIfAbsent(player.getUniqueId(), k -> new PlayerComboInput(maxTimeBetweenClicksMillis));

        // Se um combo estava travado, ele é "destravado" quando uma nova entrada começa.
        if (lockedInCombos.remove(player.getUniqueId()) != null) {
            // O título será limpo ou atualizado abaixo.
        }

        inputState.addClick(clickType);
        String currentInputDisplay = inputState.getCurrentInputDisplayString();

        if (!inputState.isComplete()) {
            if (player.isSneaking()) {
                if (!currentInputDisplay.isEmpty()) {
                    player.sendTitle("", currentInputDisplay, 0, 20, 5);
                } else {
                    player.sendTitle("", "", 0, 1, 0); // Limpa se a entrada estiver vazia (ex: após expirar)
                }
            } else {
                player.sendTitle("", "", 0, 1, 0); // Limpa se não estiver agachado
            }
        }

        if (inputState.isComplete()) {
            Combo combo = inputState.getAsCombo();
            inputState.clear(); // Limpa a entrada para o próximo combo

            if (!player.isSneaking()) {
                // Se o combo foi completado, mas o jogador não está agachado, ignora e limpa.
                lockedInCombos.remove(player.getUniqueId());
                player.sendTitle("", "", 0, 1, 0);
                return;
            }

            // Tenta obter a magia atribuída a este combo para o jogador
            Optional<Spell> spellOpt = playerSpellManager.getActiveSpellForCombo(player, combo);

            if (spellOpt.isPresent()) {
                Spell spellToCast = spellOpt.get();
                SpellCastResult castResult = spellManager.castSpell(player, spellToCast);

                if (castResult == SpellCastResult.SUCCESS) {
                    lockedInCombos.remove(player.getUniqueId()); // Remove o combo travado após o sucesso
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.8f); // Som de sucesso
                    player.sendTitle("", plugin.getMessage(player, "spell.combo.cast_success", spellToCast.getDisplayName(player)), 10, 40, 10);
                } else {
                    // Se a magia falhou por qualquer motivo (cooldown, mana, classe/arma errada, etc.), remove o combo travado.
                    lockedInCombos.remove(player.getUniqueId());
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.5f); // Som de falha
                    // A mensagem específica (ex: "classe errada", "arma errada") já é enviada pelo SpellManager.
                    player.sendTitle("", "", 0, 1, 0); // Limpa o título do combo
                }
            } else {
                // Nenhuma magia atribuída a este combo para a classe do jogador
                player.sendMessage(plugin.getMessage(player, "spell.combo.no_spell_assigned", combo.getDisplayString()));
                lockedInCombos.remove(player.getUniqueId()); // Limpa o combo travado
                player.sendTitle("", "", 0, 1, 0); // Limpa o título
            }
        }
    }

    public void clearPlayerInput(UUID playerId) {
        playerInputs.remove(playerId);
        if (lockedInCombos.remove(playerId) != null) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendTitle("", "", 0, 1, 0);
            }
        }
    }

    public void clearAllPlayerInputs() {
        playerInputs.clear();
        for (UUID playerId : lockedInCombos.keySet()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendTitle("", "", 0, 1, 0);
            }
        }
        lockedInCombos.clear();
    }

    public void handlePlayerStopSneak(Player player) {
        // Quando o jogador para de agachar, limpa o combo travado e a entrada atual.
        if (lockedInCombos.remove(player.getUniqueId()) != null) {
            player.sendTitle("", "", 0, 1, 0);
        }
        PlayerComboInput existingInput = playerInputs.get(player.getUniqueId());
        if (existingInput != null && !existingInput.isEmpty()) {
            existingInput.clear();
            player.sendTitle("", "", 0, 1, 0);
        }
    }
}