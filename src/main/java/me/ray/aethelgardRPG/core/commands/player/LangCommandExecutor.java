package me.ray.aethelgardRPG.core.commands.player;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.CharacterModule;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.item.ItemData;
import me.ray.aethelgardRPG.modules.item.ItemModule;
import me.ray.aethelgardRPG.modules.item.items.ItemManager;
import me.ray.aethelgardRPG.modules.custommob.CustomMobModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class LangCommandExecutor implements CommandExecutor {

    private final AethelgardRPG plugin;

    public LangCommandExecutor(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("general.player-only-command"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("aethelgardrpg.command.lang")) {
            player.sendMessage(plugin.getMessage(player, "general.no-permission"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.getMessage(player, "general.lang.usage"));
            Set<String> availableLangs = plugin.getLanguageManager().getLoadedLanguages();
            if (!availableLangs.isEmpty()) {
                player.sendMessage(plugin.getMessage(player, "general.lang.available", String.join(", ", availableLangs)));
            }
            return true;
        }

        String langCode = args[0].toLowerCase();
        Set<String> availableLangs = plugin.getLanguageManager().getLoadedLanguages();

        if (!availableLangs.contains(langCode)) {
            player.sendMessage(plugin.getMessage(player, "general.lang.not-found", langCode, String.join(", ", availableLangs)));
            return true;
        }

        CharacterModule characterModule = plugin.getModuleManager().getModule(CharacterModule.class);
        if (characterModule == null) {
            player.sendMessage(plugin.getMessage(player, "general.module-not-found", "Character"));
            return true;
        }

        Optional<CharacterData> characterDataOpt = characterModule.getCharacterData(player);
        if (characterDataOpt.isEmpty()) {
            player.sendMessage(plugin.getMessage(player, "character.gui.profile.no-data", player.getName()));
            return true;
        }

        CharacterData characterData = characterDataOpt.get();
        characterData.setLanguagePreference(langCode); // Define a preferência no objeto de dados
        characterModule.saveCharacterDataAsync(characterData); // Salva o CharacterData (que agora inclui a preferência de idioma)

        // Atualiza o idioma do LanguageManager para o jogador imediatamente
        plugin.getLanguageManager().setPlayerLanguage(player, langCode);

        player.sendMessage(plugin.getMessage(player, "general.lang.changed", langCode));

        updatePlayerInventoryLanguage(player);

        CustomMobModule customMobModule = plugin.getModuleManager().getModule(CustomMobModule.class);
        if (customMobModule != null && plugin.getModuleManager().isModuleEnabled(CustomMobModule.class)) {
            customMobModule.updateAllMobDisplaysForPlayer(player);
            plugin.getLogger().info("Updating mob displays for " + player.getName() + "'s new language (global update).");
        }

        if (characterModule != null) {
            characterModule.refreshAccessoryPlaceholders(player);
        }

        return true;
    }

    private void updatePlayerInventoryLanguage(Player player) {
        ItemModule itemModule = plugin.getModuleManager().getModule(ItemModule.class);
        if (itemModule == null || !plugin.getModuleManager().isModuleEnabled(ItemModule.class)) {
            plugin.getLogger().warning("ItemModule not available to update inventory for " + player.getName() + " after language change.");
            return;
        }

        ItemManager itemManager = itemModule.getItemManager();
        if (itemManager == null) {
            plugin.getLogger().warning("ItemManager not available to update inventory for " + player.getName() + " after language change.");
            return;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack currentItemStack = contents[i];

            if (currentItemStack == null || currentItemStack.getType().isAir()) {
                continue;
            }

            if (itemManager.isRPGItem(currentItemStack)) {
                String itemId = itemManager.getItemId(currentItemStack);
                if (itemId == null) {
                    continue;
                }

                Optional<ItemData> itemDataOpt = itemManager.getItemData(itemId);
                if (itemDataOpt.isEmpty()) {
                    continue;
                }

                ItemData itemData = itemDataOpt.get();
                int amount = currentItemStack.getAmount();
                ItemStack updatedItemStack = itemManager.createItemStack(itemData, player);
                updatedItemStack.setAmount(amount);
                inventory.setItem(i, updatedItemStack);
            }
        }
    }
}