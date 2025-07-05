package me.ray.aethelgardRPG.modules.quest.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.utils.PDCKeys;
import me.ray.aethelgardRPG.modules.quest.Quest;
import me.ray.aethelgardRPG.modules.quest.QuestModule;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

public class NPCInteractQuestListener implements Listener {

    private final AethelgardRPG plugin;
    private final QuestModule questModule;

    public NPCInteractQuestListener(AethelgardRPG plugin, QuestModule questModule) {
        this.plugin = plugin;
        this.questModule = questModule;
    }

    @EventHandler
    public void onNPCInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return; // Evitar duplo evento

        Player player = event.getPlayer();
        Entity clickedEntity = event.getRightClicked();

        // MELHORIA: Identificar NPCs de forma robusta usando PersistentDataContainer.
        // A tag PDCKeys.NPC_ID_KEY deve ser adicionada à entidade do NPC
        // quando ele for criado (ex: por um comando /npc create <id> ou pela API do Citizens).
        if (!clickedEntity.getPersistentDataContainer().has(PDCKeys.NPC_ID_KEY, PersistentDataType.STRING)) {
            return; // A entidade clicada não é um NPC do nosso sistema.
        }

        String npcId = clickedEntity.getPersistentDataContainer().get(PDCKeys.NPC_ID_KEY, PersistentDataType.STRING);
        if (npcId == null || npcId.isEmpty()) return;

        // Verificar se este NPC oferece alguma quest para o jogador
        for (Quest quest : questModule.getQuestManager().getRegisteredQuests()) {
            if (quest.getStartingNpcId().equalsIgnoreCase(npcId)) {
                questModule.getQuestManager().offerQuest(player, quest.getId());
                // Poderia haver um sistema de diálogo aqui antes de oferecer
                return; // Processa apenas uma quest por interação por simplicidade
            }
        }

        // TODO: Lógica para entregar quests a NPCs
    }
}