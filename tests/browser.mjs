import {chromium} from '/opt/docker/mecha-run/e2e/node_modules/playwright/index.mjs';
import {readFile,mkdir} from 'node:fs/promises';
import assert from 'node:assert/strict';
const artifacts=new URL('../artifacts/',import.meta.url);
const android=process.argv.includes('--android');
const baseUrl=android?'https://appassets.androidplatform.net/':'http://127.0.0.1:8143/';
async function serveAndroidAssets(context){
 if(!android)return;
 await context.setOffline(true);
 await context.route('https://appassets.androidplatform.net/**',async route=>{
  const name=new URL(route.request().url()).pathname.slice(1)||'index.html';
  if(!['index.html','style.css','game.js','core.js'].includes(name)){await route.abort();return;}
  await route.fulfill({contentType:name.endsWith('.js')?'application/javascript':name.endsWith('.css')?'text/css':'text/html',body:await readFile(new URL('../android/app/src/main/assets/web/'+name,import.meta.url))});
 });
}
await mkdir(artifacts,{recursive:true});
const browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH||'/root/.cache/ms-playwright/chromium-1243/chrome-linux64/chrome',args:['--no-sandbox']});
const errors=[];
try{
 const context=await browser.newContext({viewport:{width:1440,height:900},reducedMotion:'reduce'});
 await serveAndroidAssets(context);
 await context.route('**/game.js',async route=>{const body=await readFile(new URL('../dist/game.js',import.meta.url),'utf8');await route.fulfill({contentType:'text/javascript',body:body+'\nglobalThis.__test={get game(){return game},get mode(){return mode}};'});});
 const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
 await page.goto(baseUrl);await page.waitForFunction(()=>window.__test);await page.waitForTimeout(700);
 await page.screenshot({path:new URL('desktop.png',artifacts).pathname});
 await page.locator('#start').click();await page.waitForTimeout(150);assert.equal(await page.locator('#health-text').isVisible(),true);
 const before=await page.evaluate(()=>__test.game.player.x);await page.keyboard.down('d');await page.waitForTimeout(450);await page.keyboard.up('d');assert.ok(await page.evaluate(()=>__test.game.player.x)>before+40);
 await page.evaluate(()=>{const g=__test.game,p=g.player,e=g.enemies[0];e.x=p.x+60;e.y=p.y;e.hp=12;p.angle=0;});
 await page.keyboard.press('j');await page.waitForTimeout(120);assert.ok(await page.evaluate(()=>__test.game.kills)>=1);
 await page.keyboard.press('e');await page.waitForTimeout(120);assert.ok(await page.evaluate(()=>__test.game.player.waveCd)>0);
 await page.keyboard.press(' ');await page.waitForTimeout(100);assert.ok(await page.evaluate(()=>__test.game.player.dashCd)>0);
 await page.keyboard.press('Escape');await page.waitForTimeout(80);assert.equal(await page.evaluate(()=>__test.mode),'pause');await page.locator('#modal-action').click();
 await page.screenshot({path:new URL('gameplay.png',artifacts).pathname});
 for(let zone=0;zone<3;zone++){
   await page.evaluate(()=>{const g=__test.game;for(const e of g.enemies)if(e.hp>0)g.hurtEnemy(e,9999);});await page.waitForTimeout(120);
   if(await page.locator('.choice').first().isVisible())await page.locator('.choice').first().click();
   assert.equal(await page.evaluate(()=>__test.game.portal),true);
   await page.evaluate(()=>{__test.game.player.x=1710;__test.game.player.y=700;});await page.waitForTimeout(200);
 }
 assert.equal(await page.evaluate(()=>__test.mode),'victory');await page.screenshot({path:new URL('victory.png',artifacts).pathname});
 await page.locator('#modal-action').click();await page.evaluate(()=>{const g=__test.game;g.player.invuln=0;g.hurtPlayer(999);});await page.waitForTimeout(100);assert.equal(await page.evaluate(()=>__test.mode),'defeat');
 await page.locator('#modal-action').click();assert.equal(await page.evaluate(()=>__test.game.player.hp),100);assert.equal(await page.evaluate(()=>__test.game.zone),0);
 await context.close();
 const mobile=await browser.newContext({viewport:{width:android?360:390,height:844},isMobile:true,hasTouch:true,reducedMotion:'reduce'});await serveAndroidAssets(mobile);const mp=await mobile.newPage();mp.on('pageerror',e=>errors.push(e.message));
 await mp.goto(baseUrl);await mp.waitForTimeout(700);await mp.screenshot({path:new URL('mobile.png',artifacts).pathname});await mp.locator('#start').tap();assert.equal(await mp.locator('#joystick').isVisible(),true);await mp.locator('#attack').tap();await mp.locator('#wave').tap();await mp.screenshot({path:new URL('mobile-gameplay.png',artifacts).pathname});
 if(android){const joy=await mp.locator('#joystick').boundingBox(),attack=await mp.locator('#attack').boundingBox();assert.ok(joy.x+joy.width<attack.x,'Joystick e pulsanti si sovrappongono');assert.ok(attack.width>=44,'Pulsante touch troppo piccolo');}
 assert.equal(await mp.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);assert.deepEqual(errors,[]);await mobile.close();
 console.log('Browser OK'+(android?' con gli asset Android e rete disattivata':'')+': desktop, movimento, combattimento, abilità, pausa, potenziamenti, tre aree, vittoria, sconfitta, riavvio, touch e layout mobile.');
}finally{await browser.close();}
