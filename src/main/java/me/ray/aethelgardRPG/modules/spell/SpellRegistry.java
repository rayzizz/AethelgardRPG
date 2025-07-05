package me.ray.aethelgardRPG.modules.spell;

import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.classcombat.RPGClass;
import me.ray.aethelgardRPG.modules.spell.combo.Combo;
import me.ray.aethelgardRPG.modules.spell.combo.ClickType;
import me.ray.aethelgardRPG.modules.spell.spells.Spell;
import me.ray.aethelgardRPG.modules.spell.spells.archer.ArcherPotentShotSpell;
import me.ray.aethelgardRPG.modules.spell.spells.mage.MageFreezingRaySpell;
import me.ray.aethelgardRPG.modules.spell.spells.necromancer.SummonSkeletonSpell;
import me.ray.aethelgardRPG.modules.spell.spells.warrior.WarriorSpinningFurySpell;
import me.ray.aethelgardRPG.modules.spell.spells.warrior.WarriorTauntSpell;
// Importe a nova magia HeroicLeapSpell
import me.ray.aethelgardRPG.modules.spell.spells.warrior.HeroicLeapSpell; // <-- Adicione esta linha

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SpellRegistry {

    private final AethelgardRPG plugin;
    // Agora, apenas um registro de todas as spells por ID
    private final Map<String, Spell> allSpellsById;

    public SpellRegistry(AethelgardRPG plugin) {
        this.plugin = plugin;
        this.allSpellsById = new HashMap<>();
    }

    public void initializeSpells() {
        // Registrar todas as spells disponíveis no jogo
        // Elas não estão mais vinculadas a combos aqui, apenas registradas globalmente.

        // Magias do Guerreiro
        registerSpell(new WarriorSpinningFurySpell(plugin));
        registerSpell(new WarriorTauntSpell(plugin));
        registerSpell(new HeroicLeapSpell(plugin)); // <-- Adicione esta linha para registrar o Salto Heroico

        // Magias do Mago
        registerSpell(new MageFreezingRaySpell(plugin));

        // Magias do arqueiro
        registerSpell(new ArcherPotentShotSpell(plugin));
        // Magias do Necromante
        SummonSkeletonSpell summonSkeletonSpell = new SummonSkeletonSpell(plugin);
        summonSkeletonSpell.setNumberOfSkeletonsToSummon(2);
        summonSkeletonSpell.setSkeletonDurationSeconds(30);
        summonSkeletonSpell.setCanDamageOwner(false);
        registerSpell(summonSkeletonSpell);

        plugin.getLogger().info("Total de " + allSpellsById.size() + " spells registradas globalmente.");
    }

    /**
     * Registra uma spell no sistema.
     * @param spell A instância da spell a ser registrada.
     */
    public void registerSpell(Spell spell) {
        if (allSpellsById.containsKey(spell.getId().toLowerCase())) {
            plugin.getLogger().warning("Tentativa de registrar spell com ID duplicado: " + spell.getId());
            return;
        }
        allSpellsById.put(spell.getId().toLowerCase(), spell);
        plugin.getLogger().info("Spell registrada: " + spell.getId() + " (" + spell.getNameKey() + ")");
    }

    /**
     * Obtém uma spell pelo seu ID.
     * @param spellId O ID da spell.
     * @return Um Optional contendo a spell, se encontrada.
     */
    public Optional<Spell> getSpellById(String spellId) {
        return Optional.ofNullable(allSpellsById.get(spellId.toLowerCase()));
    }

    public int getSpellCount() {
        return allSpellsById.size();
    }

    /**
     * Retorna uma coleção de todas as spells registradas.
     * @return Uma coleção imutável de todas as spells.
     */
    public Map<String, Spell> getAllSpells() {
        return new HashMap<>(allSpellsById); // Retorna uma cópia para evitar modificações externas
    }
}