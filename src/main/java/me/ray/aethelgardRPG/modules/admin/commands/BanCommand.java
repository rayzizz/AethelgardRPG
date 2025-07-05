package me.ray.aethelgardRPG.modules.admin.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class BanCommand implements CommandExecutor {

    private final AethelgardRPG plugin;

    public BanCommand(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String noPermissionMsg = (sender instanceof Player) ? plugin.getMessage((Player) sender, "general.no-permission") : plugin.getMessage("general.no-permission");
        String usageMsg = (sender instanceof Player) ? plugin.getMessage((Player) sender, "admin.chat.ban.usage") : plugin.getMessage("admin.chat.ban.usage");
        String defaultReasonMsg = (sender instanceof Player) ? plugin.getMessage((Player) sender, "admin.chat.ban.default-reason") : plugin.getMessage("admin.chat.ban.default-reason");

        if (!sender.hasPermission("aethelgardrpg.admin.chat.ban")) {
            sender.sendMessage(noPermissionMsg);
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(usageMsg);
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : defaultReasonMsg;

        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), reason, null, sender.getName());

        if (sender instanceof Player) {
            sender.sendMessage(plugin.getMessage((Player) sender, "admin.chat.ban.success", target.getName(), reason));
        } else {
            sender.sendMessage(plugin.getMessage("admin.chat.ban.success", target.getName(), reason));
        }

        if (target.isOnline() && target.getPlayer() != null) {
            Player targetPlayer = target.getPlayer();
            // CORREÇÃO: Usa o idioma do jogador alvo para a mensagem de kick.
            targetPlayer.kickPlayer(plugin.getMessage(targetPlayer, "admin.chat.ban.kick-message", reason));
        }

        return true;
    }
}