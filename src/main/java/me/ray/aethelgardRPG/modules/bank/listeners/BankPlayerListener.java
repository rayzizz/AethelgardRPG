package me.ray.aethelgardRPG.modules.bank.listeners;

import me.ray.aethelgardRPG.modules.bank.bank.BankManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener para eventos de jogador relacionados ao módulo de banco.
 */
public class BankPlayerListener implements Listener {

    private final BankManager bankManager;

    public BankPlayerListener(BankManager bankManager) {
        this.bankManager = bankManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Salva a conta bancária do jogador de forma assíncrona quando ele sai.
        bankManager.saveAndUnloadPlayerBankAccount(event.getPlayer().getUniqueId());
    }
}