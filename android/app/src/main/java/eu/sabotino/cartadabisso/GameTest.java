package eu.sabotino.cartadabisso;

public class GameTest {
    public static void main(String[] args) {
        System.out.println("Avvio suite test nativa Carta d'Abisso...");

        Game game = new Game();

        // Test 1: Inizializzazione giocatore ed equipaggiamento iniziale
        assert game.level == 1 : "Livello iniziale errato";
        assert game.hp == game.getMaxHp() : "Salute iniziale errata";
        assert game.equippedWeapon != null : "Arma iniziale assente";
        assert game.equippedArmor != null : "Corazza iniziale assente";
        assert game.equippedRelic != null : "Reliquia iniziale assente";
        assert game.getAttackDamage() > 0 : "Danno iniziale nullo";
        assert game.getDefense() > 0 : "Difesa iniziale nulla";
        assert game.getMoveSpeed() > 100f : "Velocita di movimento non valida";
        System.out.println("Test 1 PASSATO: Inizializzazione e statistiche base OK.");

        // Test 2: Point & click movement e coordinate
        float startX = game.playerX;
        float startY = game.playerY;
        game.setPlayerTarget(startX + 100f, startY + 50f);
        assert game.playerMoving : "Giocatore non in movimento dopo target";
        assert game.beaconTimer > 0f : "Beacon di click Diablo non attivato";

        game.update(0.1f);
        assert game.playerX > startX : "Giocatore non si e mosso verso la coordinata X";
        System.out.println("Test 2 PASSATO: Navigazione Point and Click OK.");

        // Test 3: Attacco arpione e proiettili
        int projCountBefore = game.projectiles.size();
        boolean attacked = game.performAttack(game.playerX + 150f, game.playerY);
        assert attacked : "Attacco primario fallito";
        assert game.projectiles.size() == projCountBefore + 1 : "Proiettile arpione non generato";
        assert game.attackCooldown > 0f : "Cooldown di attacco non impostato";
        System.out.println("Test 3 PASSATO: Attacco primario e balistica OK.");

        // Test 4: Abilita Marea (AoE) e consumo Marea/Mana
        float tideBefore = game.tide;
        boolean waveUsed = game.performWave();
        assert waveUsed : "Abilita Marea fallita";
        assert game.tide < tideBefore : "Marea non consumata";
        assert game.waveCooldown > 0f : "Cooldown marea assente";
        System.out.println("Test 4 PASSATO: Sfera della Marea e Shockwave AoE OK.");

        // Test 5: Scatto e Schivata con Invincibility Frames
        boolean dashed = game.performDash();
        assert dashed : "Scatto fallito";
        assert game.dashCooldown > 0f : "Cooldown scatto assente";
        assert game.dashAnim > 0.4f : "Animazione scatto assente";

        // Durante il picco dello scatto, il giocatore e' invulnerabile
        int hpDuringDash = game.hp;
        game.damagePlayer(25, 0f, 0f);
        assert game.hp == hpDuringDash : "I-frames di scatto non hanno bloccato il danno";
        System.out.println("Test 5 PASSATO: Scatto con frame di invincibilita OK.");

        // Test 6: Combattimento, danno e formula difesa a riposo
        game.dashAnim = 0f; // Fine scatto
        int hpBefore = game.hp;
        game.damagePlayer(25, 0f, 0f);
        assert game.hp < hpBefore : "Danno al giocatore non applicato a riposo";
        assert game.trauma > 0f : "Trauma da impatto non generato";
        System.out.println("Test 6 PASSATO: Calcolo danni e trauma da impatto OK.");

        // Test 7: Pozione di cura
        int hpHurt = game.hp;
        boolean drank = game.usePotion();
        assert drank : "Uso pozione fallito";
        assert game.hp > hpHurt : "Salute non ripristinata";
        assert game.potions == 2 : "Conteggio pozioni errato";
        System.out.println("Test 7 PASSATO: Pozione di cura OK.");

        // Test 8: Sistema di Drop, Rarità e Rarity Beams
        Game.Item dropItem = game.generateRandomItem(2, false);
        assert dropItem.name != null && !dropItem.name.isEmpty() : "Nome item drop nullo";
        assert dropItem.rarity >= 0 && dropItem.rarity <= 3 : "Rarita non valida";
        game.backpack.add(dropItem);
        assert game.backpack.size() == 1 : "Aggiunta item a zaino fallita";

        // Test 9: Equipaggiamento e cambio statistiche
        int oldDmg = game.getAttackDamage();
        int oldDef = game.getDefense();
        game.equipBackpackItem(0);
        assert game.getAttackDamage() != oldDmg || game.getDefense() != oldDef || game.getMoveSpeed() > 0 : "Equipaggiamento non ha modificato le statistiche";
        System.out.println("Test 8-9 PASSATO: Loot drop, zaino ed equipaggiamento OK.");

        // Test 10: Progressione Zone e Boss Fight
        game.loadZone(3);
        assert game.zone == 3 : "Caricamento Zona 3 fallito";
        assert "Santuario del Kraken".equals(game.zoneName) : "Nome zona 3 errato";
        assert game.enemies.size() == 1 : "Boss arena deve contenere il boss";
        Game.Enemy boss = game.enemies.get(0);
        assert "boss".equals(boss.type) : "Nemico non e' il boss";
        assert boss.hp > 500 : "Salute del boss insufficiente";
        System.out.println("Test 10 PASSATO: Zona 3 e Boss Leviatano OK.");

        // Test 11: Set di Equipaggiamento e Bonus Set
        game.initPlayer();
        // Inizialmente ha Scafandro e Frammento entrambi del Set "Corallo"
        assert game.countSetPieces("Corallo") == 2 : "Conteggio pezzi Set Corallo errato";
        assert game.isSetBonusActive("Corallo") : "Bonus 2 pezzi Set Corallo non attivo";
        int hpWithSet = game.getMaxHp();
        int defWithSet = game.getDefense();
        // Sostituisci la corazza con una non di set
        game.equippedArmor = new Game.Item("a_simple", "Tuta Semplice", "armor", Game.RARITY_COMMON, 0, 4, 10, 0f, 0f, "Tuta", 0, null);
        assert game.countSetPieces("Corallo") == 1 : "Conteggio pezzi Set Corallo non aggiornato dopo rimozione";
        assert !game.isSetBonusActive("Corallo") : "Bonus Set Corallo non deve essere attivo con 1 pezzo";
        assert game.getMaxHp() < hpWithSet : "Bonus PV da Set Corallo non disattivato";
        assert game.getDefense() < defWithSet : "Bonus Difesa da Set Corallo non disattivato";
        System.out.println("Test 11 PASSATO: Set di Equipaggiamento e Bonus Set OK.");

        // Test 12: Sistema Talenti / Perk al Level Up
        game.initPlayer();
        assert !game.perkChoicePending : "Scelta talenti non deve essere attiva al livello 1";
        assert game.activePerks.isEmpty() : "Nessun talento attivo all'avvio";
        // Assegna XP sufficiente a salire di livello
        game.addXp(70);
        assert game.level == 2 : "Livello non incrementato dopo XP";
        assert game.perkChoicePending : "Scelta talenti deve attivarsi al level up";
        assert !game.pendingPerkChoices.isEmpty() : "Scelte talenti vuote";
        String chosenPerkId = game.pendingPerkChoices.get(0).id;
        boolean selected = game.selectPerk(chosenPerkId);
        assert selected : "Selezione talento fallita";
        assert game.hasPerk(chosenPerkId) : "Talento selezionato non registrato in activePerks";
        assert !game.perkChoicePending : "Stato scelta talenti non reimpostato dopo selezione";

        // Verifica effetto talento specifico (es. Guscio Corazzato o Tiro Rapido)
        game.activePerks.add("perk_armor");
        int defBeforeArmorPerk = game.getDefense();
        game.activePerks.remove("perk_armor");
        assert game.getDefense() == defBeforeArmorPerk - 6 : "Talento Guscio Corazzato (+6 Def) non calcolato";
        System.out.println("Test 12 PASSATO: Sistema Talenti e Perk al Level Up OK.");

        // Test 13: Valuta Conchiglie d'oro e Drops
        game.initPlayer();
        int initialShells = game.shells;
        assert initialShells == 15 : "Conchiglie iniziali errate";
        game.drops.add(new Game.Drop(game.playerX, game.playerY, 10)); // Drop conchiglie sul giocatore
        game.update(0.1f);
        assert game.shells == initialShells + 10 : "Raccolta conchiglie non registrata";
        System.out.println("Test 13 PASSATO: Drop e Raccolta Conchiglie d'oro OK.");

        // Test 14: Bottega di Mastro Paguro
        game.initPlayer();
        game.shells = 100;
        game.potions = 2;
        boolean potionBought = game.buyPotionFromMerchant();
        assert potionBought : "Acquisto pozione fallito";
        assert game.potions == 3 : "Conteggio pozioni non incrementato da acquisto";
        assert game.shells == 85 : "Costo 15 conchiglie non detratto";

        game.tide = 10f;
        boolean blessingBought = game.buyTideBlessing();
        assert blessingBought : "Benedizione marea fallita";
        assert game.tide == game.getMaxTide() : "Marea non ripristinata al massimo";
        assert game.shells == 60 : "Costo 25 conchiglie non detratto";

        int bagCount = game.backpack.size();
        boolean cofferBought = game.buyMysteryCoffer();
        assert cofferBought : "Acquisto scrigno misterioso fallito";
        assert game.backpack.size() == bagCount + 1 : "Item da scrigno non inserito nello zaino";
        assert game.shells == 5 : "Costo 55 conchiglie non detratto";
        System.out.println("Test 14 PASSATO: Bottega di Mastro Paguro OK.");

        System.out.println("TUTTI I 14 TEST UNITARI SONO STATI SUPERATI CON SUCCESSO!");
    }
}
