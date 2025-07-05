// C:/Users/r/IdeaProjects/AethelgardRPG/src/main/java/me/ray/aethelgardRPG/modules/bank/BankRepository.java
package me.ray.aethelgardRPG.modules.bank;

import me.ray.aethelgardRPG.AethelgardRPG;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class BankRepository {
    private final AethelgardRPG plugin;

    public BankRepository(AethelgardRPG plugin) {
        this.plugin = plugin;
    }

    public void createBankTable() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS player_banks (" +
                "uuid VARCHAR(36) PRIMARY KEY NOT NULL," +
                "money_balance DOUBLE DEFAULT 0.0," +
                "item_slots INT DEFAULT 27," +
                "items_base64 TEXT" +
                ");";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(createTableSQL)) {
            pstmt.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível criar a tabela player_banks", e);
        }
    }

    public Optional<PlayerBankAccount> loadAccount(UUID playerUUID) {
        String selectSQL = "SELECT * FROM player_banks WHERE uuid = ?;";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                double money = rs.getDouble("money_balance");
                int slots = rs.getInt("item_slots");
                String itemsBase64 = rs.getString("items_base64");
                ItemStack[] items = itemStackArrayFromBase64(itemsBase64);
                return Optional.of(new PlayerBankAccount(playerUUID, money, slots, items));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível carregar a conta bancária para " + playerUUID, e);
        }
        return Optional.empty();
    }

    public void saveAccount(PlayerBankAccount account) {
        String driver = plugin.getConfigManager().getDatabaseConfig().getString("driver", "").toLowerCase();
        String upsertSQL;

        if (driver.contains("mysql") || driver.contains("mariadb")) {
            // Sintaxe para MySQL/MariaDB
            upsertSQL = "INSERT INTO player_banks (uuid, money_balance, item_slots, items_base64) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "money_balance = VALUES(money_balance), " +
                    "item_slots = VALUES(item_slots), " +
                    "items_base64 = VALUES(items_base64);";
        } else {
            // Sintaxe para SQLite (padrão)
            upsertSQL = "INSERT INTO player_banks (uuid, money_balance, item_slots, items_base64) VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET " +
                    "money_balance = excluded.money_balance, " +
                    "item_slots = excluded.item_slots, " +
                    "items_base64 = excluded.items_base64;";
        }

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(upsertSQL)) {
            pstmt.setString(1, account.getOwnerId().toString());
            pstmt.setDouble(2, account.getMoneyBalance());
            pstmt.setInt(3, account.getItemSlots());
            pstmt.setString(4, itemStackArrayToBase64(account.getItemInventory().getContents()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar a conta bancária para " + account.getOwnerId(), e);
        }
    }

    private String itemStackArrayToBase64(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível serializar o array de itens para Base64", e);
            return "";
        }
    }

    private ItemStack[] itemStackArrayFromBase64(String data) {
        if (data == null || data.isEmpty()) {
            return new ItemStack[0];
        }
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível desserializar o array de itens de Base64", e);
            return new ItemStack[0];
        }
    }
}