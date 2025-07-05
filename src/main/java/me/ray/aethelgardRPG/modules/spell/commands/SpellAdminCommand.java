package me.ray.aethelgardRPG.modules.spell.commands;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.spell.PlayerSpellManager;
import me.ray.aethelgardRPG.modules.spell.SpellModule;
import me.ray.aethelgardRPG.modules.spell.SpellRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SpellAdminCommand implements CommandExecutor, TabCompleter {

    private final AethelgardRPG plugin;
    private final PlayerSpellManager playerSpellManager;
    private final SpellRegistry spellRegistry;

    public SpellAdminCommand(AethelgardRPG plugin, SpellModule spellModule) {
        this.plugin = plugin;
        this.playerSpellManager = spellModule.getPlayerSpellManager();
        this.spellRegistry = spellModule.getSpellRegistry();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("aethelgardrpg.admin.spell")) {
            // CORREÇÃO: Verifica o tipo do sender para a mensagem
            if (sender instanceof Player) {
                sender.sendMessage(plugin.getMessage((Player) sender, "general.no-permission"));
            } else {
                sender.sendMessage(plugin.getMessage("general.no-permission"));
            }
            return true;
        }

        if (args.length < 3) {
            // CORREÇÃO: Verifica o tipo do sender para a mensagem
            if (sender instanceof Player) {
                sender.sendMessage(plugin.getMessage((Player) sender, "spell.admin_command.usage"));
            } else {
                sender.sendMessage(plugin.getMessage("spell.admin_command.usage"));
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);
        String spellId = args[2];

        if (target == null) {
            // CORREÇÃO: Verifica o tipo do sender para a mensagem
            if (sender instanceof Player) {
                sender.sendMessage(plugin.getMessage((Player) sender, "spell.admin_command.player_not_found", args[1]));
            } else {
                sender.sendMessage(plugin.getMessage("spell.admin_command.player_not_found", args[1]));
            }
            return true;
        }

        if (spellRegistry.getSpellById(spellId).isEmpty()) {
            // CORREÇÃO: Verifica o tipo do sender para a mensagem
            if (sender instanceof Player) {
                sender.sendMessage(plugin.getMessage((Player) sender, "spell.admin_command.spell_not_found", spellId));
            } else {
                sender.sendMessage(plugin.getMessage("spell.admin_command.spell_not_found", spellId));
            }
            return true;
        }

        switch (subCommand) {
            case "learn":
            case "give":
                if (playerSpellManager.learnSpell(target, spellId)) {
                    // CORREÇÃO: Verifica o tipo do sender para a mensagem
                    if (sender instanceof Player) {
                        sender.sendMessage(plugin.getMessage((Player) sender, "spell.admin_command.learn_success_sender", spellId, target.getName()));
                    } else {
                        sender.sendMessage(plugin.getMessage("spell.admin_command.learn_success_sender", spellId, target.getName()));
                    }
                } else {
                    // CORREÇÃO: Verifica o tipo do sender para a mensagem
                    if (sender instanceof Player) {
                        sender.sendMessage(plugin.getMessage((Player) sender, "spell.admin_command.learn_fail_sender", spellId, target.getName()));
                    } else {
                        sender.sendMessage(plugin.getMessage("spell.admin_command.learn_fail_sender", spellId, target.getName()));
                    }
                }
                break;

            case "unlearn":
            case "remove":
            case "take":
                if (playerSpellManager.unlearnSpell(target, spellId)) {
                    // CORREÇÃO: Verifica o tipo do sender para a mensagem
                    if (sender instanceof Player) {
                        sender.sendMessage(plugin.getMessage((Player) sender, "spell.admin_command.unlearn_success_sender", spellId, target.getName()));
                    } else {
                        sender.sendMessage(plugin.getMessage("spell.admin_command.unlearn_success_sender", spellId, target.getName()));
                    }
                } else {
                    // CORREÇÃO: Verifica o tipo do sender para a mensagem
                    if (sender instanceof Player) {
                        sender.sendMessage(plugin.getMessage((Player) sender, "spell.admin_command.unlearn_fail_sender", spellId, target.getName()));
                    } else {
                        sender.sendMessage(plugin.getMessage("spell.admin_command.unlearn_fail_sender", spellId, target.getName()));
                    }
                }
                break;

            default:
                // CORREÇÃO: Verifica o tipo do sender para a mensagem
                if (sender instanceof Player) {
                    sender.sendMessage(plugin.getMessage((Player) sender, "spell.admin_command.usage"));
                } else {
                    sender.sendMessage(plugin.getMessage("spell.admin_command.usage"));
                }
                break;
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("aethelgardrpg.admin.spell")) {
            return null;
        }

        if (args.length == 1) {
            return List.of("learn", "unlearn", "give", "take").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3) {
            return spellRegistry.getAllSpells().keySet().stream()
                    .filter(id -> id.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}