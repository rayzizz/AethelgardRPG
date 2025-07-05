package me.ray.aethelgardRPG.modules.bank.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.bank.BankModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BankCommand implements CommandExecutor {

    private final AethelgardRPG plugin;
    private final BankModule bankModule;

    public BankCommand(AethelgardRPG plugin, BankModule bankModule) {
        this.plugin = plugin;
        this.bankModule = bankModule;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("general.player-only-command"));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            bankModule.getBankManager().openBankGUI(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "deposit":
                // TODO: /bank deposit <quantia> ou /bank deposit all
                player.sendMessage(plugin.getMessage(player, "bank.deposit-placeholder"));
                return true;
            case "withdraw":
                // TODO: /bank withdraw <quantia>
                player.sendMessage(plugin.getMessage(player, "bank.withdraw-placeholder"));
                return true;
            // case "upgrade":
            // TODO: /bank upgrade slots
            // player.sendMessage(plugin.getMessage("bank.upgrade-placeholder"));
            // return true;
            default:
                player.sendMessage(plugin.getMessage(player, "general.unknown-subcommand", subCommand));
                return true;
        }
    }
}