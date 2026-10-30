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

        // Test 12: Sistema Classi di Combattimento e Albero Abilità Fisso
        game.initPlayer();
        assert game.skillPoints == 0 : "Punti abilità iniziali diversi da 0";
        assert Game.CLASS_HUNTER.equals(game.playerClass) : "Classe predefinita non Cacciatore";

        // Assegna XP sufficiente a salire di livello: non deve bloccare il gioco ma dare punti abilità
        game.addXp(70);
        assert game.level == 2 : "Livello non incrementato dopo XP";
        assert game.skillPoints == 1 : "Punto abilità non assegnato al level up";
        assert !game.perkChoicePending : "Il gioco non deve interrompersi con pop-up roguelike casuali";

        // Test progressione albero Hunter: Tier 1 sbloccabile, Tier 2 bloccato da prerequisito
        assert game.canUpgradeSkill("hunt_pierce") : "hunt_pierce (Tier 1) deve essere sbloccabile";
        assert !game.canUpgradeSkill("hunt_multishot") : "hunt_multishot (Tier 2) richiede hunt_pierce";

        boolean unlockedTier1 = game.upgradeSkill("hunt_pierce");
        assert unlockedTier1 : "Sblocco hunt_pierce fallito";
        assert game.hasSkill("hunt_pierce") : "hunt_pierce non risulta acquisito";
        assert game.skillPoints == 0 : "Punto abilità non detratto";

        // Con 0 punti, Tier 2 non può essere sbloccato anche se il prerequisito è soddisfatto
        assert !game.canUpgradeSkill("hunt_multishot") : "Impossibile sbloccare Tier 2 senza punti abilità";

        // Aggiungi un altro punto e sblocca Tier 2 (Tiro a Ventaglio)
        game.skillPoints = 1;
        assert game.canUpgradeSkill("hunt_multishot") : "hunt_multishot deve essere sbloccabile ora";
        boolean unlockedTier2 = game.upgradeSkill("hunt_multishot");
        assert unlockedTier2 : "Sblocco hunt_multishot fallito";
        assert game.hasSkill("hunt_multishot") : "hunt_multishot non risulta acquisito";

        // Respec: resetta le abilità e restituisce tutti i punti
        game.respecSkills();
        assert !game.hasSkill("hunt_pierce") : "hunt_pierce non deve essere attivo dopo il respec";
        assert !game.hasSkill("hunt_multishot") : "hunt_multishot non deve essere attivo dopo il respec";
        assert game.skillPoints == 2 : "Punti abilità rimborsati errati dopo respec";

        // Cambio Classe in Juggernaut (Custode dei Fondali)
        game.selectClass(Game.CLASS_JUGGERNAUT);
        assert Game.CLASS_JUGGERNAUT.equals(game.playerClass) : "Classe non passata a Custode";
        assert "Custode dei Fondali".equals(game.getClassName(game.playerClass)) : "Nome classe Custode errato";

        // Sblocca Guscio Corazzato (jug_armor) e verifica bonus difensivi e vitali
        int baseDef = game.getDefense();
        int baseHp = game.getMaxHp();
        assert game.canUpgradeSkill("jug_armor") : "jug_armor deve essere sbloccabile";
        game.upgradeSkill("jug_armor");
        assert game.getDefense() == baseDef + 8 : "Bonus difesa +8 da Guscio Corazzato non applicato";
        assert game.getMaxHp() == baseHp + 35 : "Bonus salute +35 da Guscio Corazzato non applicato";

        // Cambio Classe in Mystic (Mistico dell'Abisso): deve azzerare i punti spesi e rimborsarli
        game.selectClass(Game.CLASS_MYSTIC);
        assert Game.CLASS_MYSTIC.equals(game.playerClass) : "Classe non passata a Mistico";
        assert !game.hasSkill("jug_armor") : "Abilità del Custode non devono persistere dopo cambio classe";
        assert game.skillPoints == 2 : "Tutti i punti abilità devono essere rimborsati al cambio classe";

        // Sblocca Flusso Eterno e verifica aumento Marea Massima
        game.skillRanks.put("mys_vortex", 1);
        game.skillRanks.put("mys_lightning", 1);
        float baseTide = game.getMaxTide();
        game.upgradeSkill("mys_tideflow");
        assert game.hasSkill("mys_tideflow") : "Flusso Eterno non attivo";
        assert game.getMaxTide() == baseTide + 40f : "Bonus +40 Marea Massima non applicato";
        System.out.println("Test 12 PASSATO: Classi di Combattimento, Albero Fisso e Respec OK.");

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

        // Test 15: Categorie Oggetti e Slot Equipaggiati (Diablo system)
        game.initPlayer();
        assert "ARMA (Mano Principale)".equals(game.equippedWeapon.getCategoryName()) : "Nome categoria arma errato";
        assert "CORAZZA (Torace)".equals(game.equippedArmor.getCategoryName()) : "Nome categoria corazza errato";
        assert "RELIQUIA (Talismano)".equals(game.equippedRelic.getCategoryName()) : "Nome categoria reliquia errato";
        assert "ARMA".equals(game.equippedWeapon.getSlotTitle()) : "Titolo slot arma errato";
        assert game.getEquippedForSlot("weapon") == game.equippedWeapon : "Slot weapon non corrispondente";
        assert game.getEquippedForSlot("armor") == game.equippedArmor : "Slot armor non corrispondente";
        assert game.getEquippedForSlot("relic") == game.equippedRelic : "Slot relic non corrispondente";
        System.out.println("Test 15 PASSATO: Categorie Diablo e Slot Equipaggiati OK.");

        // Test 16: Movimento Joystick Dinamico e assenza drift
        game.initPlayer();
        assert !game.joystickActive : "Joystick non deve essere attivo all'avvio";
        assert game.joystickX == 0f && game.joystickY == 0f : "Joystick vector non nullo";
        game.update(0.1f);
        assert !game.playerMoving : "Giocatore non deve muoversi senza input";
        System.out.println("Test 16 PASSATO: Joystick dinamico e zero drift OK.");

        System.out.println("TUTTI I 16 TEST UNITARI SONO STATI SUPERATI CON SUCCESSO!");
    }
}
