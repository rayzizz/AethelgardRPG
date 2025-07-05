package me.ray.aethelgardRPG.modules.admin.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.admin.AdminModule;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

// TODO: Implementar sistema de mute com duração e persistência
public class MuteCommand implements CommandExecutor {

    private final AethelgardRPG plugin;
    private final AdminModule adminModule; // Para acessar dados específicos do módulo se necessário

    public MuteCommand(AethelgardRPG plugin, AdminModule adminModule) {
        this.plugin = plugin;
        this.adminModule = adminModule;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String noPermissionMsg = (sender instanceof Player) ? plugin.getMessage((Player) sender, "general.no-permission") : plugin.getMessage("general.no-permission");
        String usageMsg = (sender instanceof Player) ? plugin.getMessage((Player) sender, "admin.chat.mute.usage") : plugin.getMessage("admin.chat.mute.usage");

        if (!sender.hasPermission("aethelgardrpg.admin.chat.mute")) {
            sender.sendMessage(noPermissionMsg);
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(usageMsg);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            if (sender instanceof Player) {
                sender.sendMessage(plugin.getMessage((Player) sender, "general.player-not-found", args[0]));
            } else {
                sender.sendMessage(plugin.getMessage("general.player-not-found", args[0]));
            }
            return true;
        }

        // Lógica de mute (placeholder)
        // Idealmente, você teria um MuteManager para lidar com mutes temporários/permanentes
        // e persistência no banco de dados.
        // Por agora, apenas uma mensagem de confirmação.
        // admin.chatModule.getMutedPlayers().add(target.getUniqueId()); // Exemplo se tivesse uma lista

        if (sender instanceof Player) {
            sender.sendMessage(plugin.getMessage((Player) sender, "admin.chat.mute.success", target.getName()));
        } else {
            sender.sendMessage(plugin.getMessage("admin.chat.mute.success", target.getName()));
        }
        // Muted message for the target, in their language
        target.sendMessage(plugin.getMessage(target, "admin.chat.mute.muted-message"));
        return true;
    }
}