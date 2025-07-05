package me.ray.aethelgardRPG.modules.admin.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.item.ItemModule;
import me.ray.aethelgardRPG.modules.item.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class GetArmorCommand implements CommandExecutor {

    private final AethelgardRPG plugin;

    public GetArmorCommand(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("general.player-only-command")); // Console message
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("aethelgardrpg.admin.chat.getarmor")) {
            player.sendMessage(plugin.getMessage(player, "general.no-permission"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.getMessage(player, "admin.chat.getarmor.usage"));
            return true;
        }

        String armorId = args[0]; // O ID pode conter '/', ex: "armor/leather_set/leather_helmet"

        ItemModule itemModule = plugin.getModuleManager().getModule(ItemModule.class);
        if (itemModule == null) {
            player.sendMessage(plugin.getMessage(player, "admin.chat.getarmor.itemmodule-not-found"));
            return true;
        }
        ItemAPI itemAPI = itemModule;

        Optional<ItemStack> armorOpt = itemAPI.getRPGItemById(armorId, player); // Pass player context

        if (armorOpt.isEmpty()) {
            player.sendMessage(plugin.getMessage(player, "admin.chat.getarmor.item-not-found", armorId));
            return true;
        }

        ItemStack armor = armorOpt.get();
        // Verificação simples se é uma armadura (pode ser melhorada com NBT tag 'is-armor')
        String materialName = armor.getType().name();
        if (!(materialName.endsWith("_HELMET") || materialName.endsWith("_CHESTPLATE") || materialName.endsWith("_LEGGINGS") || materialName.endsWith("_BOOTS")) || !itemAPI.isRPGItem(armor)) {
            player.sendMessage(plugin.getMessage(player, "admin.chat.getarmor.not-an-armor", armorId));
            return true;
        }

        player.getInventory().addItem(armor);
        player.sendMessage(plugin.getMessage(player, "admin.chat.getarmor.success", armor.getItemMeta() != null ? armor.getItemMeta().getDisplayName() : armorId));

        return true;
    }
}
