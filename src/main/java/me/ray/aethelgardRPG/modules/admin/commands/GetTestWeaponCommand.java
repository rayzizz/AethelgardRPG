package me.ray.aethelgardRPG.modules.admin.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import me.ray.aethelgardRPG.modules.item.ItemModule;
import me.ray.aethelgardRPG.modules.item.ItemRarity;
import me.ray.aethelgardRPG.modules.item.api.ItemAPI;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Optional;
import java.util.List;

public class GetTestWeaponCommand implements CommandExecutor {

    private final AethelgardRPG plugin;

    public GetTestWeaponCommand(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("general.player-only-command")); // Console message
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("aethelgardrpg.admin.chat.gettestweapon")) {
            player.sendMessage(plugin.getMessage(player, "general.no-permission"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.getMessage(player, "admin.chat.gettestweapon.usage"));
            return true;
        }

        RPGClass targetClass;
        try {
            targetClass = RPGClass.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(plugin.getMessage(player, "admin.chat.gettestweapon.invalid-class", args[0]));
            return true;
        }

        ItemModule itemModule = plugin.getModuleManager().getModule(ItemModule.class);
        if (itemModule == null) {
            player.sendMessage(plugin.getMessage(player, "admin.chat.gettestweapon.itemmodule-not-found"));
            return true;
        }
        ItemAPI itemAPI = itemModule;

        // O ID do item agora corresponderá ao nome do arquivo YAML (sem .yml)
        // Convenção de nome: items/test_weapons/guerreiro_test_weapon.yml
        String subfolder = "test_weapons";
        String baseItemId = targetClass.name().toLowerCase() + "_test_weapon";
        String itemId = subfolder + "/" + baseItemId; // Usa '/' como separador, pois o ItemManager normaliza para '/'

        Optional<ItemStack> testWeaponOpt = itemAPI.getRPGItemById(itemId, player); // Pass player context

        if (testWeaponOpt.isEmpty()) {
            player.sendMessage(plugin.getMessage(player, "admin.chat.gettestweapon.weapon-creation-failed", targetClass.name()));
            player.sendMessage(plugin.getMessage(player, "admin.chat.gettestweapon.check-config", itemId));
            return true;
        }

        player.getInventory().addItem(testWeaponOpt.get());
        player.sendMessage(plugin.getMessage(player, "admin.chat.gettestweapon.success", targetClass.getDisplayName(player)));

        return true;
    }

    // O método createTestWeaponForClass não é mais necessário aqui,
    // pois os itens são carregados de arquivos de configuração.
    // Você precisará criar arquivos YAML para cada arma de teste, por exemplo:
    // items/test_weapons/guerreiro_test_weapon.yml
    // items/test_weapons/mago_test_weapon.yml
    // etc.
}