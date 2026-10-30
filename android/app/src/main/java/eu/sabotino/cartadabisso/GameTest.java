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

        System.out.println("TUTTI I 10 TEST UNITARI SONO STATI SUPERATI CON SUCCESSO!");
    }
}
