// C:/Users/r/IdeaProjects/AethelgardRPG/src/main/java/me/ray/aethelgardRPG/modules/skill/listeners/PlayerFishSkillListener.java
package me.ray.aethelgardRPG.modules.skill.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.skill.SkillModule;
import me.ray.aethelgardRPG.modules.skill.SkillType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerFishSkillListener implements Listener {

    private final AethelgardRPG plugin;
    private final SkillModule skillModule;

    public PlayerFishSkillListener(AethelgardRPG plugin, SkillModule skillModule) {
        this.plugin = plugin;
        this.skillModule = skillModule;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Player player = event.getPlayer();
            if (event.getCaught() instanceof Item) {
                Item caughtItemEntity = (Item) event.getCaught();
                ItemStack caughtItemStack = caughtItemEntity.getItemStack();

                // Obtém o nome do material do item pescado
                String caughtItemName = caughtItemStack.getType().name();

                // Busca o valor de XP no SkillManager a partir da configuração
                double xpToGive = skillModule.getSkillManager().getXpForAction(SkillType.PESCA, caughtItemName);

                if (xpToGive > 0) {
                    skillModule.getSkillManager().addExperience(player, SkillType.PESCA, xpToGive);
                }
            }
        }
    }
}