package me.ray.aethelgardRPG.modules.admin.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.item.ItemModule;
import me.ray.aethelgardRPG.modules.item.api.ItemAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class GetAccessoryCommand implements CommandExecutor {

    private final AethelgardRPG plugin;

    public GetAccessoryCommand(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("general.player-only-command")); // Console message
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("aethelgardrpg.admin.chat.getaccessory")) {
            player.sendMessage(plugin.getMessage(player, "general.no-permission"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.getMessage(player, "admin.chat.getaccessory.usage"));
            return true;
        }

        String accessoryId = args[0]; // O ID pode conter '/', ex: "accessories/ring_of_strength"

        ItemModule itemModule = plugin.getModuleManager().getModule(ItemModule.class);
        if (itemModule == null) {
            player.sendMessage(plugin.getMessage(player, "admin.chat.getaccessory.itemmodule-not-found"));
            return true;
        }
        ItemAPI itemAPI = itemModule;

        Optional<ItemStack> accessoryOpt = itemAPI.getRPGItemById(accessoryId, player); // Passa o jogador

        if (accessoryOpt.isEmpty()) {
            player.sendMessage(plugin.getMessage(player, "admin.chat.getaccessory.item-not-found", accessoryId));
            return true;
        }

        ItemStack accessory = accessoryOpt.get();
        if (!itemAPI.isAccessory(accessory)) {
            player.sendMessage(plugin.getMessage(player, "admin.chat.getaccessory.not-an-accessory", accessoryId));
            return true;
        }

        player.getInventory().addItem(accessory);
        player.sendMessage(plugin.getMessage(player, "admin.chat.getaccessory.success", accessory.getItemMeta() != null ? accessory.getItemMeta().getDisplayName() : accessoryId));

        return true;
    }
}
