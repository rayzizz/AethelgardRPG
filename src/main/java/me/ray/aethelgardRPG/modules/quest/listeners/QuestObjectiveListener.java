package me.ray.aethelgardRPG.modules.quest.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.core.utils.PDCKeys;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.custommob.mobs.CustomMobManager;
import me.ray.aethelgardRPG.modules.quest.QuestModule;
import me.ray.aethelgardRPG.modules.quest.QuestObjectiveType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import java.util.Optional;

public class QuestObjectiveListener implements Listener {

    private final AethelgardRPG plugin;
    private final QuestModule questModule;
    private final CharacterModule characterModule;
    private final CustomMobManager customMobManager; // Para identificar mobs customizados

    public QuestObjectiveListener(AethelgardRPG plugin, QuestModule questModule) {
        this.plugin = plugin;
        this.questModule = questModule;
        this.characterModule = plugin.getModuleManager().getModule(CharacterModule.class);
        // Assumindo que CustomMobModule já foi carregado e CustomMobManager está disponível
        this.customMobManager = plugin.getModuleManager().getModule(me.ray.aethelgardRPG.modules.custommob.CustomMobModule.class) != null ?
                plugin.getModuleManager().getModule(me.ray.aethelgardRPG.modules.custommob.CustomMobModule.class).getCustomMobManager() : null;
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        Player killer = event.getEntity().getKiller();
        LivingEntity killed = event.getEntity();

        if (characterModule == null) return;
        Optional<CharacterData> playerDataOpt = characterModule.getCharacterData(killer);
        if (playerDataOpt.isEmpty()) return;

        String mobId = (customMobManager != null && customMobManager.isCustomMob(killed)) ?
                killed.getPersistentDataContainer().get(PDCKeys.CUSTOM_MOB_ID_KEY, org.bukkit.persistence.PersistentDataType.STRING)
                : killed.getType().name(); // Fallback para tipo de entidade vanilla

        questModule.getQuestManager().updateQuestObjective(killer, QuestObjectiveType.KILL_MOBS, mobId, 1);
    }

    // TODO: @EventHandler para PlayerPickupItemEvent ou InventoryClickEvent para COLLECT_ITEMS
    // TODO: @EventHandler para PlayerMoveEvent (com otimizações) para REACH_LOCATION
    // TODO: TALK_TO_NPC será gerenciado pelo NPCInteractQuestListener ou um sistema de diálogo
}