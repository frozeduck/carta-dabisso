export const ZONES = [
  {name:'Il giardino sommerso',depth:240,count:8,color:'#66bfa9'},
  {name:'La biblioteca del silenzio',depth:680,count:12,color:'#a3a6ca'},
  {name:'Il cuore della fossa',depth:1240,count:7,color:'#d3a278'}
];
export const ROCKS = [{x:740,y:410,r:80},{x:1030,y:950,r:95},{x:1370,y:380,r:70},{x:400,y:1110,r:55}];
export const distance = (a,b) => Math.hypot(a.x-b.x,a.y-b.y);
export const clamp = (n,a,b) => Math.max(a,Math.min(b,n));
export class Expedition {
  constructor(random=Math.random){
    this.random=random;this.events=[];this.time=0;this.zone=0;this.kills=0;this.pearls=0;this.won=false;this.dead=false;
    this.player={x:310,y:700,hp:100,maxHp:100,damage:12,speed:220,level:1,xp:0,nextXp:5,potions:3,angle:0,attackCd:0,waveCd:0,dashCd:0,invuln:0,dashTime:0,combo:0,comboTime:0,attackTime:0};
    this.shots=[];this.drops=[];this.loadZone();
  }
  emit(type,data={}){this.events.push({type,...data});}
  loadZone(){
    this.kills=0;this.shots=[];this.drops=[];this.enemies=[];this.portal=false;
    const z=ZONES[this.zone];
    for(let i=0;i<z.count;i++){
      const boss=this.zone===2&&i===z.count-1;
      const type=boss?'boss':i%3===1?'jelly':'crab';
      const hp=boss?440:type==='jelly'?24+this.zone*8:32+this.zone*10;
      this.enemies.push({id:i,type,x:boss?1400:700+(i%4)*255+(this.random()-.5)*70,y:boss?680:350+Math.floor(i/4)*260+this.random()*110,hp,maxHp:hp,r:boss?74:25,cd:1+this.random(),flash:0,tell:0,phase:this.random()*6,active:false});
    }
    this.player.x=310;this.player.y=700;this.player.invuln=1.5;
  }
  moveActor(a,dx,dy){
    a.x=clamp(a.x+dx,120,1790);a.y=clamp(a.y+dy,180,1220);
    for(const rock of ROCKS){const d=distance(a,rock),limit=rock.r+(a.r||22);if(d<limit){const angle=Math.atan2(a.y-rock.y,a.x-rock.x);a.x=rock.x+Math.cos(angle)*limit;a.y=rock.y+Math.sin(angle)*limit;}}
  }
  hurtEnemy(e,amount){
    if(e.hp<=0)return;
    e.hp=Math.max(0,e.hp-amount);e.flash=.2;e.active=true;
    this.emit('hit',{x:e.x,y:e.y,amount});
    if(e.hp===0){
      this.kills++;this.pearls+=e.type==='boss'?50:3+Math.floor(this.random()*5);
      this.emit('death',{x:e.x,y:e.y,boss:e.type==='boss'});
      this.player.xp+=e.type==='boss'?8:1;
      if(this.random()<.22)this.drops.push({x:e.x,y:e.y,type:'heal',life:25});
      if(this.kills===3||this.kills===8)this.drops.push({x:e.x,y:e.y,type:'relic',life:45});
      if(this.player.xp>=this.player.nextXp){this.player.xp-=this.player.nextXp;this.player.level++;this.player.nextXp+=3;this.player.hp=Math.min(this.player.maxHp,this.player.hp+25);this.emit('level');}
      if(this.kills===ZONES[this.zone].count){this.portal=true;this.emit('clear',{final:this.zone===2});}
    }
  }
  hurtPlayer(amount){
    const p=this.player;if(p.invuln>0||this.dead||this.won)return;
    p.hp=Math.max(0,p.hp-amount);p.invuln=.65;this.emit('hurt',{amount,x:p.x,y:p.y});
    if(p.hp===0){this.dead=true;this.emit('defeat');}
  }
  attack(){
    const p=this.player;if(p.attackCd>0||this.dead||this.won)return false;
    p.combo=p.comboTime>0?(p.combo+1)%3:0;p.comboTime=.95;p.attackCd=.32;p.attackTime=.22;
    const heavy=p.combo===2;
    for(const e of this.enemies){const d=distance(p,e),dot=Math.cos(Math.atan2(e.y-p.y,e.x-p.x)-p.angle);if(d<112+e.r&&dot>-.05)this.hurtEnemy(e,Math.round(p.damage*(heavy?1.8:1)));}
    this.emit('attack',{heavy});return true;
  }
  wave(){
    const p=this.player;if(p.waveCd>0||this.dead||this.won)return false;
    p.waveCd=5.5;this.emit('wave',{x:p.x,y:p.y});
    for(const e of this.enemies)if(distance(p,e)<270+e.r){this.hurtEnemy(e,p.damage*2+10);if(e.type!=='boss'){const a=Math.atan2(e.y-p.y,e.x-p.x);this.moveActor(e,Math.cos(a)*65,Math.sin(a)*65);}}
    this.shots=this.shots.filter(s=>distance(p,s)>290);return true;
  }
  dash(dx=0,dy=0){
    const p=this.player;if(p.dashCd>0||this.dead||this.won)return false;
    const angle=Math.hypot(dx,dy)>.1?Math.atan2(dy,dx):p.angle;
    p.dashAngle=angle;p.dashTime=.2;p.invuln=.4;p.dashCd=1.5;this.emit('dash',{x:p.x,y:p.y});return true;
  }
  heal(){
    const p=this.player;if(p.potions<=0||p.hp>=p.maxHp||this.dead||this.won)return false;
    p.potions--;p.hp=Math.min(p.maxHp,p.hp+60);this.emit('heal',{x:p.x,y:p.y});return true;
  }
  upgrade(kind){
    const p=this.player;
    if(kind==='damage')p.damage+=6;
    if(kind==='health'){p.maxHp+=30;p.hp=Math.min(p.maxHp,p.hp+45);}
    if(kind==='tide'){p.damage+=2;p.speed+=25;p.potions++;}
  }
  update(dt,input={}){
    if(this.dead||this.won)return;
    dt=clamp(dt,0,.05);this.time+=dt;
    const p=this.player;
    for(const key of ['attackCd','waveCd','dashCd','invuln','comboTime','attackTime'])p[key]=Math.max(0,p[key]-dt);
    let dx=input.x||0,dy=input.y||0;const len=Math.hypot(dx,dy);if(len>1){dx/=len;dy/=len;}
    if(input.angle!==undefined)p.angle=input.angle;else if(len>.1)p.angle=Math.atan2(dy,dx);
    if(p.dashTime>0){p.dashTime-=dt;this.moveActor(p,Math.cos(p.dashAngle)*850*dt,Math.sin(p.dashAngle)*850*dt);}else this.moveActor(p,dx*p.speed*dt,dy*p.speed*dt);
    if(input.attack)this.attack();
    for(const e of this.enemies){
      if(e.hp<=0)continue;e.flash=Math.max(0,e.flash-dt);e.cd-=dt;
      const d=distance(e,p);e.active=e.active||d<490;if(!e.active)continue;
      const a=Math.atan2(p.y-e.y,p.x-e.x);
      if(e.tell>0){e.tell-=dt;if(e.tell<=0){
        if(e.type==='boss'){if(d<235)this.hurtPlayer(26);this.emit('slam',{x:e.x,y:e.y});for(let i=0;i<10;i++){const angle=i*Math.PI/5;this.shots.push({x:e.x,y:e.y,vx:Math.cos(angle)*160,vy:Math.sin(angle)*160,life:4,r:10});}e.cd=3.3;}
        else{if(d<115)this.hurtPlayer(11+this.zone*3);this.emit('claw',{x:e.x,y:e.y,angle:a});e.cd=1.25;}
      }continue;}
      if(e.type==='jelly'){
        if(d>270)this.moveActor(e,Math.cos(a)*80*dt,Math.sin(a)*80*dt);
        if(d<170)this.moveActor(e,-Math.cos(a)*65*dt,-Math.sin(a)*65*dt);
        if(e.cd<=0&&d<650){this.shots.push({x:e.x,y:e.y,vx:Math.cos(a)*190,vy:Math.sin(a)*190,life:4,r:8});e.cd=2.5;this.emit('spit',{x:e.x,y:e.y});}
      }else{
        const speed=e.type==='boss'?78:100+this.zone*14;
        if(d>(e.type==='boss'?155:60))this.moveActor(e,Math.cos(a)*speed*dt,Math.sin(a)*speed*dt);
        if(d<(e.type==='boss'?230:100)&&e.cd<=0)e.tell=e.type==='boss'?.95:.42;
      }
    }
    for(let i=0;i<this.enemies.length;i++)for(let j=i+1;j<this.enemies.length;j++){const a=this.enemies[i],b=this.enemies[j];if(a.hp<=0||b.hp<=0)continue;const d=distance(a,b),limit=a.r+b.r;if(d<limit&&d>.1){const n=(limit-d)*.5;const ux=(a.x-b.x)/d,uy=(a.y-b.y)/d;this.moveActor(a,ux*n,uy*n);this.moveActor(b,-ux*n,-uy*n);}}
    for(const s of this.shots){s.x+=s.vx*dt;s.y+=s.vy*dt;s.life-=dt;if(distance(p,s)<23+s.r){this.hurtPlayer(10+this.zone*2);s.life=0;}}
    this.shots=this.shots.filter(s=>s.life>0);
    for(const drop of this.drops){drop.life-=dt;if(distance(drop,p)<65){drop.life=0;if(drop.type==='heal'){p.hp=Math.min(p.maxHp,p.hp+18);this.emit('pickup',{text:'+18 vita',x:drop.x,y:drop.y});}else{p.damage+=3;this.emit('pickup',{text:'Reliquia trovata · Arpione +3',x:drop.x,y:drop.y});}}}
    this.drops=this.drops.filter(d=>d.life>0);
    if(this.portal&&distance(p,{x:1710,y:700})<90){
      if(this.zone===2){this.won=true;this.emit('victory');}
      else{this.zone++;p.hp=Math.min(p.maxHp,p.hp+35);this.loadZone();this.emit('zone');}
    }
  }
}
