package me.ray.aethelgardRPG.modules.character.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import me.ray.aethelgardRPG.modules.classcombat.PlayerAttributes;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import me.ray.aethelgardRPG.modules.quest.Quest;
import me.ray.aethelgardRPG.modules.skill.PlayerSkillProgress;
import me.ray.aethelgardRPG.modules.skill.SkillType;
import me.ray.aethelgardRPG.modules.spell.combo.Combo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class CharacterRepository {

    private final AethelgardRPG plugin;
    private final Gson gson; // Instância do Gson para serialização

    public CharacterRepository(AethelgardRPG plugin) {
        this.plugin = plugin;
        // Configura o Gson para serializar enums como strings e permitir campos nulos
        this.gson = new GsonBuilder()
                .enableComplexMapKeySerialization() // Para Map<Enum, ...>
                .create();
    }

    public void createTables() {
        String driver = plugin.getConfigManager().getDatabaseConfig().getString("driver", "").toLowerCase();
        String autoIncrement = (driver.contains("mysql") || driver.contains("mariadb"))
                ? "INT AUTO_INCREMENT PRIMARY KEY"
                : "INTEGER PRIMARY KEY AUTOINCREMENT";

        String createCharactersTableSQL = "CREATE TABLE IF NOT EXISTS characters ("
                + "character_id " + autoIncrement + ","
                + "account_uuid VARCHAR(36) NOT NULL,"
                // *** CORREÇÃO APLICADA AQUI ***
                // Aumentado o limite de 16 para 32 caracteres.
                + "character_name VARCHAR(32) NOT NULL,"
                + "rpg_class VARCHAR(50) NOT NULL,"
                + "level INT DEFAULT 1,"
                + "experience DOUBLE DEFAULT 0,"
                + "money DOUBLE DEFAULT 0,"
                + "attribute_points INT DEFAULT 0,"
                + "last_location TEXT,"
                + "language_preference VARCHAR(10),"
                + "is_pending_deletion BOOLEAN DEFAULT FALSE,"
                + "deletion_scheduled_timestamp TIMESTAMP NULL,"
                + "last_active BOOLEAN DEFAULT FALSE,"
                + "last_combat_time_millis BIGINT DEFAULT 0,"
                + "last_sprint_time_millis BIGINT DEFAULT 0,"
                // Novas colunas JSON/Base64
                + "inventory_contents TEXT,"
                + "armor_contents TEXT,"
                + "attributes_json TEXT,"
                + "active_quests_json TEXT,"
                + "completed_quests_json TEXT,"
                + "skill_progress_json TEXT,"
                + "known_spells_json TEXT,"
                + "spell_assignments_json TEXT,"
                + "UNIQUE(account_uuid, character_name)"
                + ");";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createCharactersTableSQL);
            plugin.getLogger().info("Tabela 'characters' verificada/criada com sucesso.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível criar a tabela 'characters'.", e);
        }
    }

    public CompletableFuture<Void> saveCharacterData(CharacterData data) {
        return CompletableFuture.runAsync(() -> {
            String driver = plugin.getConfigManager().getDatabaseConfig().getString("driver", "").toLowerCase();
            String upsertSQL;

            // Colunas para INSERT/UPDATE
            String columns = "character_id, account_uuid, character_name, rpg_class, level, experience, money, attribute_points, last_location, language_preference, is_pending_deletion, deletion_scheduled_timestamp, last_active, last_combat_time_millis, last_sprint_time_millis, inventory_contents, armor_contents, attributes_json, active_quests_json, completed_quests_json, skill_progress_json, known_spells_json, spell_assignments_json";
            String placeholders = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?";

            if (driver.contains("mysql") || driver.contains("mariadb")) {
                upsertSQL = "INSERT INTO characters (" + columns + ") VALUES (" + placeholders + ") "
                        + "ON DUPLICATE KEY UPDATE "
                        + "account_uuid=VALUES(account_uuid), character_name=VALUES(character_name), rpg_class=VALUES(rpg_class), level=VALUES(level), experience=VALUES(experience), money=VALUES(money), attribute_points=VALUES(attribute_points), last_location=VALUES(last_location), language_preference=VALUES(language_preference), is_pending_deletion=VALUES(is_pending_deletion), deletion_scheduled_timestamp=VALUES(deletion_scheduled_timestamp), last_active=VALUES(last_active), last_combat_time_millis=VALUES(last_combat_time_millis), last_sprint_time_millis=VALUES(last_sprint_time_millis), inventory_contents=VALUES(inventory_contents), armor_contents=VALUES(armor_contents), attributes_json=VALUES(attributes_json), active_quests_json=VALUES(active_quests_json), completed_quests_json=VALUES(completed_quests_json), skill_progress_json=VALUES(skill_progress_json), known_spells_json=VALUES(known_spells_json), spell_assignments_json=VALUES(spell_assignments_json)";
            } else { // SQLite
                upsertSQL = "INSERT OR REPLACE INTO characters (" + columns + ") VALUES (" + placeholders + ")";
            }

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(upsertSQL)) {

                pstmt.setInt(1, data.getCharacterId());
                pstmt.setString(2, data.getAccountUuid().toString());
                pstmt.setString(3, data.getCharacterName());
                pstmt.setString(4, data.getSelectedClass().name());
                pstmt.setInt(5, data.getLevel());
                pstmt.setDouble(6, data.getExperience());
                pstmt.setDouble(7, data.getMoney());
                pstmt.setInt(8, data.getAttributePoints());
                pstmt.setString(9, serializeLocation(data.getLastLocation()));
                pstmt.setString(10, data.getLanguagePreference());
                pstmt.setBoolean(11, data.isPendingDeletion());
                pstmt.setTimestamp(12, data.getDeletionScheduledTimestamp() != null ? Timestamp.valueOf(data.getDeletionScheduledTimestamp()) : null);
                pstmt.setBoolean(13, data.isLastActive());
                pstmt.setLong(14, data.getLastCombatTimeMillis());
                pstmt.setLong(15, data.getLastSprintTimeMillis());

                // Serialização para Base64 (para ItemStack[])
                pstmt.setString(16, itemStackArrayToBase64(data.getInventoryContents()));
                pstmt.setString(17, itemStackArrayToBase64(data.getArmorContents()));

                // Serialização para JSON
                pstmt.setString(18, gson.toJson(data.getAttributes()));
                pstmt.setString(19, gson.toJson(data.getActiveQuests()));
                pstmt.setString(20, gson.toJson(data.getCompletedQuestIds()));
                pstmt.setString(21, gson.toJson(data.getSkillProgressMap()));
                pstmt.setString(22, gson.toJson(data.getKnownSpellIds()));
                pstmt.setString(23, gson.toJson(data.getActiveSpellAssignments()));

                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Falha ao salvar dados do personagem para " + data.getCharacterName(), e);
            }
        });
    }

    public CompletableFuture<CharacterData> loadCharacterData(int characterId) {
        return CompletableFuture.supplyAsync(() -> {
            String selectSQL = "SELECT * FROM characters WHERE character_id = ?";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {

                pstmt.setInt(1, characterId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    UUID accountUuid = UUID.fromString(rs.getString("account_uuid"));
                    String characterName = rs.getString("character_name");

                    CharacterData data = new CharacterData(characterId, accountUuid, characterName);

                    data.setSelectedClass(RPGClass.fromString(rs.getString("rpg_class")));
                    data.setLevel(rs.getInt("level"));
                    data.setExperience(rs.getDouble("experience"));
                    data.setMoney(rs.getDouble("money"));
                    data.setAttributePoints(rs.getInt("attribute_points"));
                    data.setLastLocation(deserializeLocation(rs.getString("last_location")));
                    data.setLanguagePreference(rs.getString("language_preference"));
                    data.setPendingDeletion(rs.getBoolean("is_pending_deletion"));
                    Timestamp delTimestamp = rs.getTimestamp("deletion_scheduled_timestamp");
                    if (delTimestamp != null) {
                        data.setDeletionScheduledTimestamp(delTimestamp.toLocalDateTime());
                    }
                    data.setLastActive(rs.getBoolean("last_active"));
                    data.setLastCombatTimeMillis(rs.getLong("last_combat_time_millis"));
                    data.setLastSprintTimeMillis(rs.getLong("last_sprint_time_millis"));

                    // Desserialização de Base64 (para ItemStack[])
                    data.setInventoryContents(itemStackArrayFromBase64(rs.getString("inventory_contents")));
                    data.setArmorContents(itemStackArrayFromBase64(rs.getString("armor_contents")));

                    // Desserialização do JSON
                    data.setAttributes(gson.fromJson(rs.getString("attributes_json"), PlayerAttributes.class));

                    Type questListType = new TypeToken<ArrayList<Quest>>() {}.getType();
                    data.setActiveQuests(gson.fromJson(rs.getString("active_quests_json"), questListType));

                    Type stringSetType = new TypeToken<HashSet<String>>() {}.getType();
                    data.setCompletedQuestIds(gson.fromJson(rs.getString("completed_quests_json"), stringSetType));
                    data.setKnownSpellIds(gson.fromJson(rs.getString("known_spells_json"), stringSetType));

                    Type skillMapType = new TypeToken<EnumMap<SkillType, PlayerSkillProgress>>() {}.getType();
                    data.setSkillProgressMap(gson.fromJson(rs.getString("skill_progress_json"), skillMapType));

                    Type spellAssignMapType = new TypeToken<EnumMap<RPGClass, Map<Combo, String>>>() {}.getType();
                    data.setActiveSpellAssignments(gson.fromJson(rs.getString("spell_assignments_json"), spellAssignMapType));

                    // Garante que os mapas e listas não sejam nulos após a desserialização
                    data.ensureNonNullCollections();

                    return data;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Falha ao carregar dados do personagem para ID: " + characterId, e);
            }
            return null;
        });
    }

    private String serializeLocation(Location loc) {
        if (loc == null) return null;
        return loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getYaw() + ";" + loc.getPitch();
    }

    private Location deserializeLocation(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] parts = s.split(";");
        if (parts.length != 6) return null;

        try {
            return new Location(
                    Bukkit.getWorld(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Float.parseFloat(parts[4]),
                    Float.parseFloat(parts[5])
            );
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Falha ao desserializar localização: " + s, e);
            return null;
        }
    }

    // --- Métodos de serialização/desserialização de ItemStack[] para Base64 ---
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
        } catch (IOException e) {
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
        } catch (ClassNotFoundException | IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível desserializar o array de itens de Base64", e);
            return new ItemStack[0];
        }
    }

    // MÉTODOS RESTANTES (ADAPTADOS OU MANTIDOS)
    public CompletableFuture<List<CharacterData>> findCharactersByAccount(UUID accountUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<CharacterData> characters = new ArrayList<>();
            String sql = "SELECT character_id FROM characters WHERE account_uuid = ?"; // Seleciona apenas o ID
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, accountUuid.toString());
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    int charId = rs.getInt("character_id");
                    // Carrega os dados completos de cada personagem encontrado usando loadCharacterData
                    CharacterData data = loadCharacterData(charId).join(); // .join() para esperar o resultado do CompletableFuture
                    if (data != null) {
                        characters.add(data);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao encontrar personagens para a conta: " + accountUuid, e);
            }
            return characters;
        });
    }

    public CompletableFuture<Integer> createCharacter(UUID accountUuid, String name, RPGClass rpgClass, Location location) {
        return CompletableFuture.supplyAsync(() -> {
            // Cria um CharacterData inicial para serializar os campos JSON vazios
            CharacterData newCharData = new CharacterData(accountUuid, name, rpgClass, location, plugin.getLanguageManager().getDefaultLang());

            String sql = "INSERT INTO characters (account_uuid, character_name, rpg_class, last_location, language_preference, inventory_contents, armor_contents, attributes_json, active_quests_json, completed_quests_json, skill_progress_json, known_spells_json, spell_assignments_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, newCharData.getAccountUuid().toString());
                pstmt.setString(2, newCharData.getCharacterName());
                pstmt.setString(3, newCharData.getSelectedClass().name());
                pstmt.setString(4, serializeLocation(newCharData.getLastLocation()));
                pstmt.setString(5, newCharData.getLanguagePreference());

                // Serialização para Base64 (para ItemStack[])
                pstmt.setString(6, itemStackArrayToBase64(newCharData.getInventoryContents()));
                pstmt.setString(7, itemStackArrayToBase64(newCharData.getArmorContents()));

                // Serialização para JSON
                pstmt.setString(8, gson.toJson(newCharData.getAttributes()));
                pstmt.setString(9, gson.toJson(newCharData.getActiveQuests()));
                pstmt.setString(10, gson.toJson(newCharData.getCompletedQuestIds()));
                pstmt.setString(11, gson.toJson(newCharData.getSkillProgressMap()));
                pstmt.setString(12, gson.toJson(newCharData.getKnownSpellIds()));
                pstmt.setString(13, gson.toJson(newCharData.getActiveSpellAssignments()));

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            return generatedKeys.getInt(1);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao criar novo personagem", e);
            }
            return -1;
        });
    }

    public void updateLastActiveCharacter(UUID accountUuid, int activeCharacterId) {
        CompletableFuture.runAsync(() -> {
            String clearSql = "UPDATE characters SET last_active = FALSE WHERE account_uuid = ?";
            String setSql = "UPDATE characters SET last_active = TRUE WHERE character_id = ?";
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement clearPstmt = conn.prepareStatement(clearSql);
                     PreparedStatement setPstmt = conn.prepareStatement(setSql)) {

                    clearPstmt.setString(1, accountUuid.toString());
                    clearPstmt.executeUpdate();

                    setPstmt.setInt(1, activeCharacterId);
                    setPstmt.executeUpdate();

                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao atualizar o último personagem ativo", e);
            }
        });
    }

    // Métodos de exclusão permanecem os mesmos, pois operam na mesma tabela `characters`
    public CompletableFuture<Void> softDeleteAndSchedule(int characterId, LocalDateTime deletionTime) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE characters SET is_pending_deletion = TRUE, deletion_scheduled_timestamp = ? WHERE character_id = ?";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(deletionTime));
                pstmt.setInt(2, characterId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao agendar exclusão do personagem para ID: " + characterId, e);
            }
        });
    }

    public CompletableFuture<Void> hardDeleteCharacter(int characterId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM characters WHERE character_id = ?";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, characterId);
                pstmt.executeUpdate();
                plugin.getLogger().info("Personagem com ID " + characterId + " excluído permanentemente do banco de dados.");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao realizar hard delete para o personagem ID: " + characterId, e);
            }
        });
    }

    public CompletableFuture<Void> restoreCharacter(int characterId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE characters SET is_pending_deletion = FALSE, deletion_scheduled_timestamp = NULL WHERE character_id = ?";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, characterId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao restaurar personagem com ID: " + characterId, e);
            }
        });
    }
}