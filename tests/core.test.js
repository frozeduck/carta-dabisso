import test from 'node:test';
import assert from 'node:assert/strict';
import {Expedition,ROCKS,ZONES,distance} from '../dist/core.js';
test('melee obeys range, facing and cooldown; kills award pearls',()=>{
 const g=new Expedition(()=>.5),p=g.player,e=g.enemies[0];e.x=p.x+70;e.y=p.y;e.hp=10;p.angle=0;
 const behind=g.enemies[1];behind.x=p.x-70;behind.y=p.y;const hp=behind.hp;
 assert.equal(g.attack(),true);assert.equal(e.hp,0);assert.equal(behind.hp,hp);assert.equal(g.attack(),false);assert.equal(g.kills,1);assert.ok(g.pearls>0);
});
test('wave, healing and dash respect resource limits and invulnerability',()=>{
 const g=new Expedition(),p=g.player;p.invuln=0;p.hp=20;
 assert.equal(g.heal(),true);assert.equal(p.hp,80);assert.equal(p.potions,2);assert.equal(g.wave(),true);assert.equal(g.wave(),false);assert.equal(g.dash(1,0),true);g.hurtPlayer(99);assert.equal(p.hp,80);assert.equal(g.dash(),false);
});
test('movement stays inside the map and outside rocks',()=>{
 const g=new Expedition(),p=g.player;p.x=125;p.y=200;g.update(.05,{x:-1,y:-1});assert.ok(p.x>=120&&p.y>=180);
 p.x=ROCKS[0].x-100;p.y=ROCKS[0].y;g.moveActor(p,75,0);assert.ok(distance(p,ROCKS[0])>=ROCKS[0].r+21.99);
});
test('three cleared zones require the portal and finish with victory',()=>{
 const g=new Expedition(()=>.5);
 for(let z=0;z<3;z++){assert.equal(g.zone,z);assert.equal(g.enemies.length,ZONES[z].count);for(const e of g.enemies)g.hurtEnemy(e,10000);assert.equal(g.portal,true);assert.equal(g.won,false);g.player.x=1710;g.player.y=700;g.update(.01);}
 assert.equal(g.won,true);assert.ok(g.events.some(e=>e.type==='victory'));
});
test('death is terminal and upgrades apply to the active player',()=>{
 const g=new Expedition(),p=g.player;g.upgrade('damage');assert.equal(p.damage,18);g.upgrade('health');assert.equal(p.maxHp,130);p.invuln=0;g.hurtPlayer(999);const x=p.x;g.update(.05,{x:1});assert.equal(g.dead,true);assert.equal(p.x,x);assert.equal(g.attack(),false);
});
test('crabs telegraph damage and tide cancels nearby projectiles',()=>{
 const g=new Expedition(()=>.5),p=g.player,e=g.enemies[0];g.enemies=[e];p.invuln=0;e.x=p.x+65;e.y=p.y;e.cd=0;
 g.update(.05);assert.ok(e.tell>0);assert.equal(p.hp,100);
 for(let i=0;i<10;i++)g.update(.05);assert.equal(p.hp,89);
 g.shots=[{x:p.x+50,y:p.y,life:4},{x:p.x+500,y:p.y,life:4}];g.wave();assert.equal(g.shots.length,1);assert.equal(g.shots[0].x,p.x+500);
});
