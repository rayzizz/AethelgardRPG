// C:/Users/r/IdeaProjects/AethelgardRPG/src/main/java/me/ray/aethelgardRPG/modules/skill/listeners/BlockBreakSkillListener.java
package me.ray.aethelgardRPG.modules.skill.listeners;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.skill.SkillModule;
import me.ray.aethelgardRPG.modules.skill.SkillType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakSkillListener implements Listener {

    private final AethelgardRPG plugin;
    private final SkillModule skillModule;

    public BlockBreakSkillListener(AethelgardRPG plugin, SkillModule skillModule) {
        this.plugin = plugin;
        this.skillModule = skillModule;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Obtém o nome do material do bloco quebrado
        String blockMaterialName = block.getType().name();

        // Busca o valor de XP no SkillManager a partir da configuração
        double xpToGive = skillModule.getSkillManager().getXpForAction(SkillType.MINERACAO, blockMaterialName);

        if (xpToGive > 0) {
            skillModule.getSkillManager().addExperience(player, SkillType.MINERACAO, xpToGive);
        }
    }
}