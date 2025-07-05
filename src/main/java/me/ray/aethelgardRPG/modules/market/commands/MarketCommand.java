package me.ray.aethelgardRPG.modules.market.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.market.MarketModule;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MarketCommand implements CommandExecutor {

    private final AethelgardRPG plugin;
    private final MarketModule marketModule;

    public MarketCommand(AethelgardRPG plugin, MarketModule marketModule) {
        this.plugin = plugin;
        this.marketModule = marketModule;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("general.player-only-command"));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            // CORREÇÃO: Adicionado 'player'
            player.sendMessage(plugin.getMessage(player, "market.gui-open-placeholder"));
            // Ex: marketModule.getMarketGUI().openMainMarketGUI(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "sell":
                // /market sell <preço>
                if (args.length < 2) {
                    // CORREÇÃO: Adicionado 'player'
                    player.sendMessage(plugin.getMessage(player, "market.sell-usage"));
                    return true;
                }
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getType() == Material.AIR) {
                    // CORREÇÃO: Adicionado 'player'
                    player.sendMessage(plugin.getMessage(player, "market.sell-no-item-in-hand"));
                    return true;
                }
                try {
                    double price = Double.parseDouble(args[1]);
                    if (marketModule.getMarketManager().listItem(player, itemInHand, price)) {
                        player.getInventory().setItemInMainHand(null); // Remove o item da mão após listar com sucesso
                    }
                } catch (NumberFormatException e) {
                    // CORREÇÃO: Adicionado 'player'
                    player.sendMessage(plugin.getMessage(player, "market.invalid-price-format"));
                }
                return true;

            // case "view":
            // TODO: /market view [jogador] -> Ver listagens de um jogador específico
            // break;
            // case "history":
            // TODO: /market history -> Ver seu histórico de transações
            // break;
            default:
                // CORREÇÃO: Adicionado 'player'
                player.sendMessage(plugin.getMessage(player, "general.unknown-subcommand", subCommand));
                return true;
        }
    }
}