package me.ray.aethelgardRPG.modules.quest.quests;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.quest.Quest;
import me.ray.aethelgardRPG.modules.quest.QuestModule;
import me.ray.aethelgardRPG.modules.quest.QuestObjective;
import me.ray.aethelgardRPG.modules.quest.QuestObjectiveType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public class QuestManager {

    private final AethelgardRPG plugin;
    private final QuestModule questModule;
    private final Map<String, Quest> registeredQuests; // ID da Quest -> Template da Quest

    public QuestManager(AethelgardRPG plugin, QuestModule questModule) {
        this.plugin = plugin;
        this.questModule = questModule;
        this.registeredQuests = new HashMap<>();
    }

    /**
     * Carrega todas as definições de missões da pasta 'quests' do plugin.
     */
    public void loadQuestConfigurations() {
        registeredQuests.clear();
        File questsFolder = new File(plugin.getDataFolder(), "quests");
        if (!questsFolder.exists()) {
            if (!questsFolder.mkdirs()) {
                plugin.getLogger().severe("Não foi possível criar a pasta 'quests'. Nenhuma missão será carregada.");
                return;
            }
            // Opcional: Extrair uma missão de exemplo
            plugin.saveResource("quests/exemplo_cacador_goblins.yml", false);
        }

        File[] questFiles = questsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (questFiles == null) {
            plugin.getLogger().warning("Não foi possível ler os arquivos na pasta 'quests'.");
            return;
        }

        for (File questFile : questFiles) {
            try {
                FileConfiguration questConfig = YamlConfiguration.loadConfiguration(questFile);
                String questId = questFile.getName().replace(".yml", "");

                // Usar chaves de tradução para título e descrição
                String titleKey = questConfig.getString("title-key", "quests." + questId + ".title");
                String descriptionKey = questConfig.getString("description-key", "quests." + questId + ".description");

                String startingNpcId = questConfig.getString("start-npc-id");
                int requiredLevel = questConfig.getInt("requirements.level", 1);
                List<String> requiredQuests = questConfig.getStringList("requirements.quests");

                List<QuestObjective> objectives = new ArrayList<>();
                ConfigurationSection objectivesSection = questConfig.getConfigurationSection("objectives");
                if (objectivesSection != null) {
                    for (String key : objectivesSection.getKeys(false)) {
                        ConfigurationSection objSection = objectivesSection.getConfigurationSection(key);
                        if (objSection != null) {
                            QuestObjectiveType type = QuestObjectiveType.valueOf(objSection.getString("type").toUpperCase());
                            String target = objSection.getString("target");
                            int amount = objSection.getInt("amount");
                            // Usar chave de tradução para a descrição do objetivo
                            String objDescriptionKey = objSection.getString("description-key", "quest.objective.description." + type.name().toLowerCase());
                            objectives.add(new QuestObjective(type, target, amount, objDescriptionKey));
                        }
                    }
                }

                List<String> rewardCommands = questConfig.getStringList("rewards.commands");
                double rewardExperience = questConfig.getDouble("rewards.experience", 0);
                double rewardMoney = questConfig.getDouble("rewards.money", 0);

                Quest quest = new Quest(questId, titleKey, descriptionKey, startingNpcId, requiredLevel, requiredQuests,
                        objectives, rewardCommands, rewardExperience, rewardMoney);
                registerQuest(quest);

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao carregar a missão do arquivo: " + questFile.getName(), e);
            }
        }
        plugin.getLogger().info(registeredQuests.size() + " missões carregadas.");
    }

    public void registerQuest(Quest quest) {
        if (quest == null || quest.getId() == null) {
            plugin.getLogger().warning("Tentativa de registrar uma missão nula ou com ID nulo.");
            return;
        }
        registeredQuests.put(quest.getId(), quest);
        plugin.getLogger().info("Missão registrada: " + quest.getId() + " (Chave do Título: " + quest.getTitleKey() + ")");
    }

    public Quest getQuestTemplate(String questId) {
        return registeredQuests.get(questId);
    }

    public Collection<Quest> getRegisteredQuests() {
        return registeredQuests.values();
    }

    public boolean canPlayerStartQuest(Player player, Quest quest) {
        CharacterModule characterModule = plugin.getModuleManager().getModule(CharacterModule.class);
        if (characterModule == null || quest == null) return false;

        Optional<CharacterData> playerDataOpt = characterModule.getCharacterData(player);
        if (playerDataOpt.isEmpty()) return false;
        CharacterData characterData = playerDataOpt.get();

        // Já completou?
        if (characterData.getCompletedQuestIds().contains(quest.getId())) return false;
        // Já está ativa?
        if (characterData.getActiveQuests().stream().anyMatch(q -> q.getId().equals(quest.getId()))) return false;

        // Nível suficiente?
        if (characterData.getLevel() < quest.getRequiredLevel()) {
            player.sendMessage(plugin.getMessage(player, "quest.cannot-start-level", quest.getRequiredLevel()));
            return false;
        }
        // Quests pré-requisito completas?
        for (String reqQuestId : quest.getRequiredQuests()) {
            if (!characterData.getCompletedQuestIds().contains(reqQuestId)) {
                Quest requiredQuest = getQuestTemplate(reqQuestId);
                String requiredQuestName = (requiredQuest != null) ? requiredQuest.getTitle(player) : reqQuestId;
                player.sendMessage(plugin.getMessage(player, "quest.cannot-start-quest", requiredQuestName));
                return false;
            }
        }
        return true;
    }

    public void offerQuest(Player player, String questId) {
        Quest questTemplate = getQuestTemplate(questId);
        if (questTemplate == null) {
            player.sendMessage(plugin.getMessage(player, "quest.not-found", questId));
            return;
        }

        if (canPlayerStartQuest(player, questTemplate)) {
            player.sendMessage(plugin.getMessage(player, "quest.offer", questTemplate.getTitle(player), questTemplate.getDescription(player)));
            player.sendMessage(plugin.getMessage(player, "quest.accept-prompt", questId));
        }
        // A mensagem de falha agora é enviada diretamente de canPlayerStartQuest
    }

    public void acceptQuest(Player player, String questId) {
        CharacterModule characterModule = plugin.getModuleManager().getModule(CharacterModule.class);
        Quest questTemplate = getQuestTemplate(questId);
        if (characterModule == null || questTemplate == null) return;

        Optional<CharacterData> playerDataOpt = characterModule.getCharacterData(player);
        if (playerDataOpt.isEmpty()) return;
        CharacterData characterData = playerDataOpt.get();

        if (canPlayerStartQuest(player, questTemplate)) {
            Quest playerQuestInstance = questTemplate.createPlayerInstance();
            characterData.addActiveQuest(playerQuestInstance);
            player.sendMessage(plugin.getMessage(player, "quest.accepted", questTemplate.getTitle(player)));
            showQuestLog(player, playerQuestInstance); // Mostra os objetivos ao aceitar
        }
        // A mensagem de falha é enviada de canPlayerStartQuest se o jogador tentar aceitar sem poder
    }

    public void completeQuest(Player player, Quest quest) {
        CharacterModule characterModule = plugin.getModuleManager().getModule(CharacterModule.class);
        if (characterModule == null || quest == null) return;

        Optional<CharacterData> playerDataOpt = characterModule.getCharacterData(player);
        if (playerDataOpt.isEmpty()) return;
        CharacterData characterData = playerDataOpt.get();

        if (!quest.areAllObjectivesComplete()) {
            player.sendMessage(plugin.getMessage(player, "quest.objectives-not-complete", quest.getTitle(player)));
            return;
        }

        // Dar recompensas
        characterModule.addExperience(player, quest.getRewardExperience());
        characterData.setMoney(characterData.getMoney() + quest.getRewardMoney());
        // TODO: Dar itens de recompensa

        for (String command : quest.getRewardCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
        }

        characterData.removeActiveQuest(quest.getId());
        characterData.addCompletedQuestId(quest.getId());

        player.sendMessage(plugin.getMessage(player, "quest.completed", quest.getTitle(player)));
        if (quest.getRewardExperience() > 0)
            player.sendMessage(plugin.getMessage(player, "quest.reward.xp", String.format("%.0f", quest.getRewardExperience())));
        if (quest.getRewardMoney() > 0)
            player.sendMessage(plugin.getMessage(player, "quest.reward.money", String.format("%.2f", quest.getRewardMoney())));

        // TODO: Lógica para quests seguintes (chain quests)
    }

    public void updateQuestObjective(Player player, QuestObjectiveType type, String target, int amount) {
        Optional<CharacterData> playerDataOpt = plugin.getCharacterData(player);
        if (playerDataOpt.isEmpty()) return;

        CharacterData characterData = playerDataOpt.get();
        boolean objectiveUpdated = false;

        for (Quest activeQuest : characterData.getActiveQuests()) {
            for (QuestObjective objective : activeQuest.getObjectives()) {
                if (objective.getType() == type && objective.getTarget().equalsIgnoreCase(target) && !objective.isCompleted()) {
                    objective.incrementAmount(amount);
                    objectiveUpdated = true;
                    // TODO: Enviar feedback de progresso para o jogador (ex: action bar)
                    // Ex: player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("..."));
                }
            }
            if (objectiveUpdated && activeQuest.areAllObjectivesComplete()) {
                // TODO: Notificar o jogador que todos os objetivos estão completos e ele pode entregar a quest.
                // Ex: player.sendMessage("Todos os objetivos da missão '" + activeQuest.getTitle(player) + "' foram concluídos!");
            }
        }
    }

    /**
     * Exibe um resumo de uma missão específica para o jogador no chat.
     * @param player O jogador para quem mostrar o diário.
     * @param quest A missão a ser exibida.
     */
    public void showQuestLog(Player player, Quest quest) {
        player.sendMessage(plugin.getMessage(player, "quest.log.title-separator"));
        player.sendMessage(plugin.getMessage(player, "quest.log.quest-title", quest.getTitle(player)));
        player.sendMessage(plugin.getMessage(player, "quest.log.quest-description", quest.getDescription(player)));
        player.sendMessage(""); // Linha vazia para espaçamento
        player.sendMessage(plugin.getMessage(player, "quest.log.objectives-header"));
        for (QuestObjective objective : quest.getObjectives()) {
            player.sendMessage(objective.getFormattedProgress(player));
        }
        player.sendMessage(plugin.getMessage(player, "quest.log.title-separator"));
    }
}