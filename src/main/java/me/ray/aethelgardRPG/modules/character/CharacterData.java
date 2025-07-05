package me.ray.aethelgardRPG.modules.character;

import me.ray.aethelgardRPG.AethelgardRPG; // Import adicionado
import me.ray.aethelgardRPG.modules.classcombat.PlayerAttributes;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import me.ray.aethelgardRPG.modules.quest.Quest;
import me.ray.aethelgardRPG.modules.skill.PlayerSkillProgress;
import me.ray.aethelgardRPG.modules.skill.SkillType;
import me.ray.aethelgardRPG.modules.spell.combo.Combo;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CharacterData {

    private int characterId;
    private UUID accountUuid;
    private String characterName;
    private RPGClass selectedClass;
    private int level;
    private double experience;
    private double money;
    private int attributePoints;
    private Location lastLocation;
    private String languagePreference;
    private boolean isPendingDeletion;
    private LocalDateTime deletionScheduledTimestamp;
    private boolean lastActive;

    // --- CAMPOS PARA DADOS SERIALIZADOS ---
    private ItemStack[] inventoryContents;
    private ItemStack[] armorContents;
    private PlayerAttributes attributes;
    private List<Quest> activeQuests;
    private Set<String> completedQuestIds;
    private Map<SkillType, PlayerSkillProgress> skillProgressMap;
    private Set<String> knownSpellIds;
    private Map<RPGClass, Map<Combo, String>> activeSpellAssignments;

    // --- Campos para controle de combate/sprint ---
    private long lastCombatTimeMillis;
    private long lastSprintTimeMillis;

    public CharacterData(int characterId, UUID accountUuid, String characterName) {
        this.characterId = characterId;
        this.accountUuid = accountUuid;
        this.characterName = characterName;
        this.selectedClass = RPGClass.NONE; // Default
        this.level = 1;
        this.experience = 0;
        this.money = 0;
        this.attributePoints = 0;
        this.lastLocation = null;
        this.languagePreference = null;
        this.isPendingDeletion = false;
        this.deletionScheduledTimestamp = null;
        this.lastActive = false;

        // Inicializa os campos que serão serializados/desserializados
        this.inventoryContents = new ItemStack[36];
        this.armorContents = new ItemStack[4];
        // CORREÇÃO: Fornece a instância do plugin para o construtor de PlayerAttributes
        this.attributes = new PlayerAttributes(this.selectedClass, AethelgardRPG.getInstance());
        this.activeQuests = new ArrayList<>();
        this.completedQuestIds = new HashSet<>();
        this.skillProgressMap = new EnumMap<>(SkillType.class);
        this.knownSpellIds = new HashSet<>();
        this.activeSpellAssignments = new EnumMap<>(RPGClass.class);

        this.lastCombatTimeMillis = System.currentTimeMillis();
        this.lastSprintTimeMillis = System.currentTimeMillis();
    }

    // Construtor para criação de novo personagem (sem ID ainda)
    public CharacterData(UUID accountUuid, String characterName, RPGClass selectedClass, Location lastLocation, String languagePreference) {
        this(0, accountUuid, characterName); // Chama o construtor principal com ID 0 (será gerado pelo DB)
        this.selectedClass = selectedClass;
        this.lastLocation = lastLocation;
        this.languagePreference = languagePreference;
        // CORREÇÃO: Fornece a instância do plugin para o construtor de PlayerAttributes
        this.attributes = new PlayerAttributes(this.selectedClass, AethelgardRPG.getInstance());
    }

    // Getters
    public int getCharacterId() { return characterId; }
    public UUID getAccountUuid() { return accountUuid; }
    public String getCharacterName() { return characterName; }
    public RPGClass getSelectedClass() { return selectedClass; }
    public int getLevel() { return level; }
    public double getExperience() { return experience; }
    public double getMoney() { return money; }
    public int getAttributePoints() { return attributePoints; }
    public Location getLastLocation() { return lastLocation; }
    public String getLanguagePreference() { return languagePreference; }
    public boolean isPendingDeletion() { return isPendingDeletion; }
    public LocalDateTime getDeletionScheduledTimestamp() { return deletionScheduledTimestamp; }
    public boolean isLastActive() { return lastActive; }

    public ItemStack[] getInventoryContents() { return inventoryContents; }
    public ItemStack[] getArmorContents() { return armorContents; }
    public PlayerAttributes getAttributes() { return attributes; }
    public List<Quest> getActiveQuests() { return activeQuests; }
    public Set<String> getCompletedQuestIds() { return completedQuestIds; }
    public Map<SkillType, PlayerSkillProgress> getSkillProgressMap() { return skillProgressMap; }
    public Set<String> getKnownSpellIds() { return knownSpellIds; }
    public Map<RPGClass, Map<Combo, String>> getActiveSpellAssignments() { return activeSpellAssignments; }
    public long getLastCombatTimeMillis() { return lastCombatTimeMillis; }
    public long getLastSprintTimeMillis() { return lastSprintTimeMillis; }

    // Setters
    public void setCharacterId(int characterId) { this.characterId = characterId; }
    public void setAccountUuid(UUID accountUuid) { this.accountUuid = accountUuid; }
    public void setCharacterName(String characterName) { this.characterName = characterName; }
    public void setSelectedClass(RPGClass selectedClass) {
        this.selectedClass = selectedClass;
        // Ao mudar a classe, reinicializa os atributos baseados na nova classe
        // CORREÇÃO: Fornece a instância do plugin para o construtor de PlayerAttributes
        this.attributes = new PlayerAttributes(selectedClass, AethelgardRPG.getInstance());
    }
    public void setLevel(int level) { this.level = level; }
    public void setExperience(double experience) { this.experience = experience; }
    public void setMoney(double money) { this.money = money; }
    public void setAttributePoints(int attributePoints) { this.attributePoints = attributePoints; }
    public void setLastLocation(Location lastLocation) { this.lastLocation = lastLocation; }
    public void setLanguagePreference(String languagePreference) { this.languagePreference = languagePreference; }
    public void setPendingDeletion(boolean pendingDeletion) { isPendingDeletion = pendingDeletion; }
    public void setDeletionScheduledTimestamp(LocalDateTime deletionScheduledTimestamp) { this.deletionScheduledTimestamp = deletionScheduledTimestamp; }
    public void setLastActive(boolean lastActive) { this.lastActive = lastActive; }

    public void setInventoryContents(ItemStack[] inventoryContents) { this.inventoryContents = inventoryContents; }
    public void setArmorContents(ItemStack[] armorContents) { this.armorContents = armorContents; }
    public void setAttributes(PlayerAttributes attributes) { this.attributes = attributes; }
    public void setActiveQuests(List<Quest> activeQuests) { this.activeQuests = activeQuests; }
    public void setCompletedQuestIds(Set<String> completedQuestIds) { this.completedQuestIds = completedQuestIds; }
    public void setSkillProgressMap(Map<SkillType, PlayerSkillProgress> skillProgressMap) { this.skillProgressMap = skillProgressMap; }
    public void setKnownSpellIds(Set<String> knownSpellIds) { this.knownSpellIds = knownSpellIds; }
    public void setActiveSpellAssignments(Map<RPGClass, Map<Combo, String>> activeSpellAssignments) { this.activeSpellAssignments = activeSpellAssignments; }
    public void setLastCombatTimeMillis(long lastCombatTimeMillis) { this.lastCombatTimeMillis = lastCombatTimeMillis; }
    public void setLastSprintTimeMillis(long lastSprintTimeMillis) { this.lastSprintTimeMillis = lastSprintTimeMillis; }

    // --- Métodos de Conveniência para Coleções ---
    public void addActiveQuest(Quest quest) {
        if (this.activeQuests == null) this.activeQuests = new ArrayList<>();
        this.activeQuests.add(quest);
    }

    public void removeActiveQuest(String questId) {
        if (this.activeQuests == null) return;
        this.activeQuests.removeIf(q -> q.getId().equalsIgnoreCase(questId));
    }

    public void addCompletedQuestId(String questId) {
        if (this.completedQuestIds == null) this.completedQuestIds = new HashSet<>();
        this.completedQuestIds.add(questId.toLowerCase());
    }

    public PlayerSkillProgress getSkillProgress(SkillType skillType) {
        if (this.skillProgressMap == null) this.skillProgressMap = new EnumMap<>(SkillType.class);
        return this.skillProgressMap.computeIfAbsent(skillType, PlayerSkillProgress::new);
    }

    public void assignActiveSpell(RPGClass rpgClass, Combo combo, String spellId) {
        if (this.activeSpellAssignments == null) this.activeSpellAssignments = new EnumMap<>(RPGClass.class);
        this.activeSpellAssignments.computeIfAbsent(rpgClass, k -> new HashMap<>()).put(combo, spellId);
    }

    public void unassignActiveSpell(String spellId) {
        if (this.activeSpellAssignments == null) return;
        for (Map<Combo, String> assignments : this.activeSpellAssignments.values()) {
            assignments.entrySet().removeIf(entry -> entry.getValue().equalsIgnoreCase(spellId));
        }
    }

    /**
     * Garante que todas as coleções (List, Set, Map) não sejam nulas após a desserialização,
     * inicializando-as se necessário. Isso evita NullPointerExceptions.
     */
    public void ensureNonNullCollections() {
        if (this.inventoryContents == null) this.inventoryContents = new ItemStack[36];
        if (this.armorContents == null) this.armorContents = new ItemStack[4];
        // CORREÇÃO: Fornece a instância do plugin para o construtor de PlayerAttributes
        if (this.attributes == null) this.attributes = new PlayerAttributes(this.selectedClass, AethelgardRPG.getInstance());
        if (this.activeQuests == null) this.activeQuests = new ArrayList<>();
        if (this.completedQuestIds == null) this.completedQuestIds = new HashSet<>();
        if (this.skillProgressMap == null) this.skillProgressMap = new EnumMap<>(SkillType.class);
        if (this.knownSpellIds == null) this.knownSpellIds = new HashSet<>();
        if (this.activeSpellAssignments == null) this.activeSpellAssignments = new EnumMap<>(RPGClass.class);
    }

    public long getRemainingDeletionTimeSeconds() {
        if (isPendingDeletion && deletionScheduledTimestamp != null) {
            long scheduledMillis = deletionScheduledTimestamp.toInstant(ZoneOffset.UTC).toEpochMilli();
            long currentMillis = System.currentTimeMillis();
            return Math.max(0, (scheduledMillis - currentMillis) / 1000);
        }
        return 0;
    }
}