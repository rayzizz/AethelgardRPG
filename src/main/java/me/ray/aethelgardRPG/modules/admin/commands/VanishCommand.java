package me.ray.aethelgardRPG.modules.admin.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.admin.AdminModule;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class VanishCommand implements CommandExecutor {

    private final AethelgardRPG plugin;
    private final AdminModule adminModule;

    public VanishCommand(AethelgardRPG plugin, AdminModule adminModule) {
        this.plugin = plugin;
        this.adminModule = adminModule;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("general.player-only-command")); // Console message
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("aethelgardrpg.admin.chat.vanish")) {
            player.sendMessage(plugin.getMessage(player, "general.no-permission"));
            return true;
        }

        boolean isVanished = adminModule.isVanished(player.getUniqueId());
        adminModule.setVanished(player.getUniqueId(), !isVanished);

        if (!isVanished) {
            // Tornar invisível para outros jogadores
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.hasPermission("aethelgardrpg.admin.chat.vanish.see")) { // Permissão para ver jogadores em vanish
                    onlinePlayer.hidePlayer(plugin, player);
                }
            }
            player.sendMessage(plugin.getMessage(player, "admin.chat.vanish.enabled"));
        } else {
            // Tornar visível
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.showPlayer(plugin, player);
            }
            player.sendMessage(plugin.getMessage(player, "admin.chat.vanish.disabled"));
        }
        return true;
    }
}