package me.ray.aethelgardRPG.modules.bank;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.UUID;

public class PlayerBankAccount implements InventoryHolder {
    private final UUID ownerId;
    private double moneyBalance;
    private int itemSlots; // Número total de slots para itens
    private final Inventory itemInventory; // Inventário para armazenar os itens

    private static final int DEFAULT_INITIAL_SLOTS = 27; // 3 linhas
    private static final int MAX_SLOTS = 54; // Máximo de 6 linhas

    public PlayerBankAccount(UUID ownerId) {
        this.ownerId = ownerId;
        this.moneyBalance = 0.0;
        this.itemSlots = DEFAULT_INITIAL_SLOTS;
        // O título agora é dinâmico. O inventário é criado com 'this' como holder.
        this.itemInventory = Bukkit.createInventory(this, this.itemSlots);
    }

    // Construtor para carregar do banco de dados
    public PlayerBankAccount(UUID ownerId, double moneyBalance, int itemSlots, ItemStack[] items) {
        this.ownerId = ownerId;
        this.moneyBalance = moneyBalance;
        this.itemSlots = Math.min(itemSlots, MAX_SLOTS);
        this.itemInventory = Bukkit.createInventory(this, this.itemSlots);
        if (items != null) {
            // Garante que não copie mais itens do que o inventário pode conter
            if (items.length <= this.itemSlots) {
                this.itemInventory.setContents(items);
            } else {
                ItemStack[] truncatedItems = Arrays.copyOf(items, this.itemSlots);
                this.itemInventory.setContents(truncatedItems);
            }
        }
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        // O InventoryHolder deve retornar o inventário que ele possui.
        return this.itemInventory;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public double getMoneyBalance() {
        return moneyBalance;
    }

    public void setMoneyBalance(double moneyBalance) {
        this.moneyBalance = Math.max(0, moneyBalance);
    }

    public void depositMoney(double amount) {
        if (amount > 0) this.moneyBalance += amount;
    }

    public boolean withdrawMoney(double amount) {
        if (amount > 0 && this.moneyBalance >= amount) {
            this.moneyBalance -= amount;
            return true;
        }
        return false;
    }

    public int getItemSlots() {
        return itemSlots;
    }

    public Inventory getItemInventory() {
        return itemInventory;
    }

    // TODO: Lógica para expandir slots (setItemSlots e recriar/redimensionar o inventário)
    // public boolean upgradeSlots(int additionalSlots, double cost) { ... }
}