package me.ray.aethelgardRPG.modules.admin.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.admin.AdminModule;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InfiniteStatsCommand implements CommandExecutor {

    private final AethelgardRPG plugin;
    private final AdminModule adminModule;

    public InfiniteStatsCommand(AethelgardRPG plugin, AdminModule adminModule) {
        this.plugin = plugin;
        this.adminModule = adminModule;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String noPermissionMsg = (sender instanceof Player) ? plugin.getMessage((Player) sender, "general.no-permission") : plugin.getMessage("general.no-permission");
        String usageMsg = (sender instanceof Player) ? plugin.getMessage((Player) sender, "admin.chat.infinitestats.usage") : plugin.getMessage("admin.chat.infinitestats.usage");

        if (!sender.hasPermission("aethelgardrpg.admin.infinitestats")) {
            sender.sendMessage(noPermissionMsg);
            return true;
        }

        Player targetPlayer;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(usageMsg);
                return true;
            }
            targetPlayer = (Player) sender;
        } else {
            targetPlayer = Bukkit.getPlayerExact(args[0]);
            if (targetPlayer == null) {
                if (sender instanceof Player) {
                    sender.sendMessage(plugin.getMessage((Player) sender, "general.player-not-found", args[0]));
                } else {
                    sender.sendMessage(plugin.getMessage("general.player-not-found", args[0]));
                }
                return true;
            }
        }

        boolean enabled = adminModule.toggleInfiniteStats(targetPlayer.getUniqueId());

        if (enabled) {
            // Define a vida e a fome no máximo ao habilitar
            targetPlayer.setHealth(targetPlayer.getMaxHealth());
            targetPlayer.setFoodLevel(20);
            targetPlayer.setSaturation(20f);
            if (sender instanceof Player) {
                sender.sendMessage(plugin.getMessage((Player) sender, "admin.chat.infinitestats.enabled", targetPlayer.getName()));
                sender.sendMessage(plugin.getMessage((Player) sender, "admin.chat.infinitestats.stamina-enabled"));
            } else {
                sender.sendMessage(plugin.getMessage("admin.chat.infinitestats.enabled", targetPlayer.getName()));
                sender.sendMessage(plugin.getMessage("admin.chat.infinitestats.stamina-enabled"));
            }
            if (sender != targetPlayer) {
                targetPlayer.sendMessage(plugin.getMessage(targetPlayer, "admin.chat.infinitestats.changed-by-other", "habilitado"));
                targetPlayer.sendMessage(plugin.getMessage(targetPlayer, "admin.chat.infinitestats.stamina-enabled"));
            }
        } else {
            // Ao desabilitar, não é necessário redefinir vida/fome, pois os listeners permitirão o comportamento normal.
            if (sender instanceof Player) {
                sender.sendMessage(plugin.getMessage((Player) sender, "admin.chat.infinitestats.disabled", targetPlayer.getName()));
                sender.sendMessage(plugin.getMessage((Player) sender, "admin.chat.infinitestats.stamina-disabled"));
            } else {
                sender.sendMessage(plugin.getMessage("admin.chat.infinitestats.disabled", targetPlayer.getName()));
                sender.sendMessage(plugin.getMessage("admin.chat.infinitestats.stamina-disabled"));
            }
            if (sender != targetPlayer) {
                targetPlayer.sendMessage(plugin.getMessage(targetPlayer, "admin.chat.infinitestats.changed-by-other", "desabilitado"));
                targetPlayer.sendMessage(plugin.getMessage(targetPlayer, "admin.chat.infinitestats.stamina-disabled"));
            }
        }

        return true;
    }
}