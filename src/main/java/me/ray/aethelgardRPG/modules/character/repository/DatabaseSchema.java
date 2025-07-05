package me.ray.aethelgardRPG.modules.character.repository;

import java.util.List;

/**
 * Centraliza todas as queries SQL de criação de tabelas para o sistema de personagens.
 */
public final class DatabaseSchema {

    private DatabaseSchema() {}

    public static List<String> getTableCreationQueries(String driver) {
        boolean isMysql = driver.toLowerCase().contains("mysql") || driver.toLowerCase().contains("mariadb");

        // --- Definições de Chave Estrangeira ---
        String foreignKeyAccount = isMysql
                ? "FOREIGN KEY (account_uuid) REFERENCES accounts(account_uuid) ON DELETE CASCADE"
                : "FOREIGN KEY (account_uuid) REFERENCES accounts(account_uuid) ON DELETE CASCADE";

        String foreignKeyCharacter = isMysql
                ? "FOREIGN KEY (character_id) REFERENCES characters(character_id) ON DELETE CASCADE"
                : "FOREIGN KEY (character_id) REFERENCES characters(character_id) ON DELETE CASCADE";

        // --- Definições de AUTO_INCREMENT ---
        String autoIncrement = isMysql ? "AUTO_INCREMENT" : "AUTOINCREMENT";

        // --- Queries de Criação ---
        String createAccountsTable = "CREATE TABLE IF NOT EXISTS accounts ("
                + "account_uuid VARCHAR(36) PRIMARY KEY NOT NULL,"
                + "last_active_character_id INT NULL,"
                + "creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ");";

        String createCharactersTable = "CREATE TABLE IF NOT EXISTS characters ("
                + "character_id INT PRIMARY KEY " + (isMysql ? "AUTO_INCREMENT" : "") + ","
                + "account_uuid VARCHAR(36) NOT NULL,"
                + "character_name VARCHAR(64) NOT NULL," // <--- ALTERADO DE VARCHAR(16) PARA VARCHAR(64)
                + "rpg_class VARCHAR(50) NOT NULL,"
                + "level INT NOT NULL DEFAULT 1,"
                + "experience DOUBLE NOT NULL DEFAULT 0.0,"
                + "money DOUBLE NOT NULL DEFAULT 0.0,"
                + "language_preference VARCHAR(10) DEFAULT 'pt_br',"
                + "world_name VARCHAR(100) NOT NULL,"
                + "pos_x DOUBLE NOT NULL,"
                + "pos_y DOUBLE NOT NULL,"
                + "pos_z DOUBLE NOT NULL,"
                + "is_deleted BOOLEAN NOT NULL DEFAULT FALSE,"
                + "is_pending_deletion BOOLEAN NOT NULL DEFAULT FALSE," // NOVO
                + "deletion_scheduled_timestamp DATETIME NULL," // NOVO
                + "creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + (isMysql ? "" : "PRIMARY KEY(character_id " + autoIncrement + "),")
                + foreignKeyAccount + ","
                + "UNIQUE(account_uuid, character_name)"
                + ");";

        String createAttributesTable = "CREATE TABLE IF NOT EXISTS character_attributes ("
                + "character_id INT PRIMARY KEY NOT NULL,"
                + "strength INT NOT NULL DEFAULT 0,"
                + "intelligence INT NOT NULL DEFAULT 0,"
                + "faith INT NOT NULL DEFAULT 0,"
                + "dexterity INT NOT NULL DEFAULT 0,"
                + "agility INT NOT NULL DEFAULT 0,"
                + "attribute_points_available INT NOT NULL DEFAULT 0,"
                + foreignKeyCharacter
                + ");";

        String createInventoryTable = "CREATE TABLE IF NOT EXISTS character_inventory ("
                + "inventory_entry_id INT " + (isMysql ? "PRIMARY KEY AUTO_INCREMENT" : "") + ","
                + "character_id INT NOT NULL,"
                + "slot_index INT NOT NULL,"
                + (isMysql ? "item_base64 MEDIUMTEXT NOT NULL," : "item_base64 TEXT NOT NULL,")
                + (isMysql ? "" : "PRIMARY KEY(inventory_entry_id " + autoIncrement + "),")
                + foreignKeyCharacter + ","
                + "UNIQUE(character_id, slot_index)"
                + ");";

        String createEquipmentTable = "CREATE TABLE IF NOT EXISTS character_equipment ("
                + "character_id INT PRIMARY KEY NOT NULL,"
                + "helmet_base64 TEXT, "
                + "chestplate_base64 TEXT, "
                + "leggings_base64 TEXT, "
                + "boots_base64 TEXT, "
                + foreignKeyCharacter
                + ");";

        String createQuestsTable = "CREATE TABLE IF NOT EXISTS character_quests ("
                + "quest_entry_id INT " + (isMysql ? "PRIMARY KEY AUTO_INCREMENT" : "") + ","
                + "character_id INT NOT NULL,"
                + "quest_id VARCHAR(255) NOT NULL,"
                + "status VARCHAR(20) NOT NULL,"
                + "progress_json TEXT,"
                + (isMysql ? "" : "PRIMARY KEY(quest_entry_id " + autoIncrement + "),")
                + foreignKeyCharacter + ","
                + "UNIQUE(character_id, quest_id)"
                + ");";

        String createSkillsTable = "CREATE TABLE IF NOT EXISTS character_skills ("
                + "skill_entry_id INT " + (isMysql ? "PRIMARY KEY AUTO_INCREMENT" : "") + ","
                + "character_id INT NOT NULL,"
                + "skill_type VARCHAR(50) NOT NULL,"
                + "level INT NOT NULL DEFAULT 1,"
                + "experience DOUBLE NOT NULL DEFAULT 0.0,"
                + (isMysql ? "" : "PRIMARY KEY(skill_entry_id " + autoIncrement + "),")
                + foreignKeyCharacter + ","
                + "UNIQUE(character_id, skill_type)"
                + ");";

        String createSpellsTable = "CREATE TABLE IF NOT EXISTS character_spells ("
                + "spell_entry_id INT " + (isMysql ? "PRIMARY KEY AUTO_INCREMENT" : "") + ","
                + "character_id INT NOT NULL,"
                + "spell_id VARCHAR(255) NOT NULL,"
                + (isMysql ? "" : "PRIMARY KEY(spell_entry_id " + autoIncrement + "),")
                + foreignKeyCharacter + ","
                + "UNIQUE(character_id, spell_id)"
                + ");";

        String createSpellAssignmentsTable = "CREATE TABLE IF NOT EXISTS character_spell_assignments ("
                + "assignment_id INT " + (isMysql ? "PRIMARY KEY AUTO_INCREMENT" : "") + ","
                + "character_id INT NOT NULL,"
                + "rpg_class VARCHAR(50) NOT NULL,"
                + "combo_string VARCHAR(50) NOT NULL,"
                + "spell_id VARCHAR(255) NOT NULL,"
                + (isMysql ? "" : "PRIMARY KEY(assignment_id " + autoIncrement + "),")
                + foreignKeyCharacter + ","
                + "UNIQUE(character_id, rpg_class, combo_string)"
                + ");";


        return List.of(
                createAccountsTable,
                createCharactersTable,
                createAttributesTable,
                createInventoryTable,
                createEquipmentTable,
                createQuestsTable,
                createSkillsTable,
                createSpellsTable,
                createSpellAssignmentsTable
        );
    }
}