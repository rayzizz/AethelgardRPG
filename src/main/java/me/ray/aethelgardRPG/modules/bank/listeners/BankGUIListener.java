package me.ray.aethelgardRPG.modules.bank.listeners;

import me.ray.aethelgardRPG.modules.bank.PlayerBankAccount;
import me.ray.aethelgardRPG.modules.bank.bank.BankManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class BankGUIListener implements Listener {

    private final BankManager bankManager;

    public BankGUIListener(BankManager bankManager) {
        this.bankManager = bankManager;
    }

    @EventHandler
    public void onBankGUIClose(InventoryCloseEvent event) {
        Inventory closedInventory = event.getInventory();

        // Verifica se o inventário fechado é um banco, checando se o "dono" (holder) é uma PlayerBankAccount.
        if (closedInventory.getHolder() instanceof PlayerBankAccount) {
            PlayerBankAccount account = (PlayerBankAccount) closedInventory.getHolder();

            // Atualiza o inventário real da conta bancária com o conteúdo da GUI que foi fechada.
            account.getItemInventory().setContents(closedInventory.getContents());

            // MELHORIA: Salva a conta no banco de dados após o jogador fechar a GUI.
            if (event.getPlayer() instanceof Player) {
                bankManager.savePlayerBankAccount(event.getPlayer().getUniqueId());
            }
        }
    }
}