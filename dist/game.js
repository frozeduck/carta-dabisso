import {Expedition,ZONES,ROCKS,clamp} from './core.js';

const $=id=>document.getElementById(id),canvas=$('sea'),ctx=canvas.getContext('2d');
const reduceMotion=matchMedia('(prefers-reduced-motion: reduce)').matches;
let W=innerWidth,H=innerHeight,DPR=Math.min(devicePixelRatio||1,2),t=0,last=0,mode='welcome',game=new Expedition(),camera={x:960,y:710},zoom=1,shake=0;
const keys=new Set(),mouse={x:0,y:0,down:false,aim:false},stick={x:0,y:0},effects=[],floaters=[];
let attackHeld=false,toastTimer,audio=null,soundOn=false,joyPointer=null;
const rng=(seed=>()=>{seed=(seed*1664525+1013904223)>>>0;return seed/4294967296;})(42107);
const decor=Array.from({length:125},(_,i)=>({x:60+rng()*1800,y:130+rng()*1150,s:.45+rng()*1.1,type:i%6,phase:rng()*6}));
const bubbles=Array.from({length:55},()=>({x:rng(),y:rng(),s:1+rng()*3,v:.008+rng()*.018}));
const colors={cream:'#e9dfbd',edge:'#143d43',gold:'#cfad6c',mint:'#76b7a0',pink:'#cc8c7c',navy:'#0b303b'};

function resize(){W=innerWidth;H=innerHeight;DPR=Math.min(devicePixelRatio||1,2);canvas.width=W*DPR;canvas.height=H*DPR;ctx.setTransform(DPR,0,0,DPR,0,0);}
addEventListener('resize',resize);resize();
function polygon(points,fill,stroke=colors.edge,lw=2){ctx.beginPath();points.forEach(([x,y],i)=>i?ctx.lineTo(x,y):ctx.moveTo(x,y));ctx.closePath();ctx.fillStyle=fill;ctx.fill();if(stroke){ctx.strokeStyle=stroke;ctx.lineWidth=lw;ctx.lineJoin='round';ctx.stroke();}}
function ellipse(x,y,rx,ry,fill,stroke=null,lw=2){ctx.beginPath();ctx.ellipse(x,y,Math.max(.1,rx),Math.max(.1,ry),0,0,Math.PI*2);ctx.fillStyle=fill;ctx.fill();if(stroke){ctx.strokeStyle=stroke;ctx.lineWidth=lw;ctx.stroke();}}
function line(points,color,width=2){ctx.beginPath();points.forEach(([x,y],i)=>i?ctx.lineTo(x,y):ctx.moveTo(x,y));ctx.strokeStyle=color;ctx.lineWidth=width;ctx.lineCap='round';ctx.lineJoin='round';ctx.stroke();}
function paper(points,fill,fold=true){ctx.save();ctx.translate(3,5);polygon(points,'#031e2866',null);ctx.restore();polygon(points,fill,colors.edge,2);if(fold&&points.length>3){ctx.save();ctx.globalAlpha=.12;polygon([points[0],points[1],[0,-12]],'#fff',null);ctx.restore();}}
function scratch(x,y,w,h,seed=0){ctx.save();ctx.strokeStyle='#163d4238';ctx.lineWidth=.7;for(let i=0;i<7;i++){const px=x+((i*17+seed*3)%31)/31*w,py=y+((i*11+seed)%29)/29*h;line([[px,py],[px+4,py-2]],'#163d4238',.7);}ctx.restore();}
function worldPoint(x,y){return{x:(x-camera.x)*zoom+W*.5,y:(y-camera.y)*zoom*.64+H*.53};}
function screenPoint(x,y){return{x:(x-W*.5)/zoom+camera.x,y:(y-H*.53)/(.64*zoom)+camera.y};}
function at(x,y,scale,fn){const p=worldPoint(x,y);if(p.x<-250||p.x>W+250||p.y<-320||p.y>H+220)return;ctx.save();ctx.translate(p.x,p.y);ctx.scale(zoom*scale,zoom*scale);fn();ctx.restore();}
function shadow(x=0,y=0,rx=35,ry=10){ellipse(x,y,rx,ry,'#05232b66');}

function coral(type,s,phase){
  const sway=reduceMotion?0:Math.sin(t*1.1+phase)*2.4;
  ctx.save();ctx.rotate(sway*.012);ctx.scale(s,s);
  if(type===0||type===4){
    const col=type===0?'#bd8272':'#85ad93';
    paper([[-9,0],[-13,-31],[-34,-46],[-38,-69],[-30,-72],[-25,-53],[-13,-47],[-16,-90],[-7,-100],[-4,-70],[7,-57],[21,-65],[25,-88],[33,-90],[33,-60],[13,-44],[11,-20],[25,-32],[38,-31],[21,-14],[10,1]],col);
    line([[-1,-3],[-5,-65]],'#f5d6aa55',2);scratch(-30,-80,60,80,phase*10);
  }else if(type===1){
    paper([[-13,0],[-18,-23],[-28,-43],[-24,-74],[-15,-48],[-6,-42],[-13,-80],[-1,-105],[3,-69],[11,-80],[20,-103],[23,-73],[12,-48],[17,-22],[7,0]],'#547e70');
    line([[0,-5],[-1,-72]],'#b6c79a77',1.5);
  }else if(type===2){
    for(let i=0;i<4;i++){ctx.save();ctx.translate((i-1.5)*12,0);const h=23+i%2*17;paper([[-8,0],[-9,-h],[0,-h-5],[8,-h],[6,0]],'#bba87b');ellipse(0,-h,7,3,'#435b53');ctx.restore();}
  }else if(type===3){
    paper([[-30,0],[-32,-17],[-23,-32],[-9,-21],[2,-37],[17,-30],[25,-10],[32,0]],'#476d66');
    ellipse(-11,-20,3,2,'#a8d9ba');ellipse(7,-27,2,2,'#a8d9ba');
  }else{
    for(let i=0;i<3;i++){ctx.save();ctx.translate((i-1)*11,-i*3);ctx.rotate((i-1)*.4);paper([[-6,0],[-13,-25],[-8,-45],[1,-55],[8,-38],[6,-16]],'#638b75');line([[0,-4],[0,-42]],'#d6d8b16b',1);ctx.restore();}
  }
  ctx.restore();
}

function rock(r){
  const s=r.r/80;ctx.save();ctx.scale(s,s);shadow(0,3,83,27);
  paper([[-77,3],[-84,-33],[-65,-78],[-20,-104],[38,-95],[76,-55],[82,-10],[39,17]],'#42645f');
  polygon([[-77,-33],[-65,-78],[-20,-104],[0,-52],[-24,-21]],'#527c6d',null);
  polygon([[0,-52],[38,-95],[76,-55],[82,-10],[39,17],[17,-20]],'#355850',null);
  line([[-64,-78],[-20,-60],[0,-52],[17,-20]],'#acc09a44',1.6);
  scratch(-60,-75,120,75,7);
  ctx.save();ctx.translate(-30,-64);coral(1,.44,1);ctx.restore();
  ctx.save();ctx.translate(35,-70);coral(0,.42,4);ctx.restore();
  ctx.restore();
}

function diver(p,large=false){
  const moving=mode==='play'&&(keys.size>0||Math.abs(stick.x)+Math.abs(stick.y)>.1||p.dashTime>0);
  const bob=reduceMotion?0:Math.sin(t*(moving?12:2.2))* (moving?3:2),face=Math.cos(p.angle||0)<0?-1:1;
  shadow(0,6,37,11);
  ctx.save();ctx.translate(0,bob);ctx.scale(face,1);
  if(p.invuln>0&&mode==='play'&&Math.floor(t*14)%2)ctx.globalAlpha=.5;
  ctx.save();ctx.translate(-18,-58);ctx.rotate(-.2);paper([[-15,20],[-17,-11],[-8,-20],[9,-20],[16,-6],[11,21]],'#80937c');line([[-12,-8],[10,-8]],'#d2cfaa',2);ctx.restore();
  const step=moving?Math.sin(t*12)*8:0;
  ctx.save();ctx.translate(-12,step*.3);paper([[-10,-22],[-9,-1],[-20,5],[-17,12],[3,12],[7,-20]],'#bc9862');line([[-12,6],[0,6]],'#f4d8a0',2);ctx.restore();
  ctx.save();ctx.translate(14,-step*.3);paper([[-8,-23],[-6,1],[-10,8],[-7,13],[17,12],[18,5],[8,-1],[10,-24]],'#c9a56d');ctx.restore();
  paper([[-27,-60],[-18,-77],[17,-77],[29,-57],[22,-24],[4,-15],[-22,-23]],'#e0c990');
  polygon([[-2,-72],[15,-70],[22,-54],[17,-29],[-1,-21]],'#bf9d66',null);line([[-25,-37],[19,-32]],'#7a7154',3);
  paper([[-24,-61],[-35,-54],[-39,-31],[-28,-25],[-17,-49]],'#d2b77d');
  ellipse(-32,-25,8,7,'#aaa482',colors.edge);paper([[20,-63],[31,-57],[39,-39],[29,-32],[16,-48]],'#e3cc93');
  ctx.save();ctx.translate(33,-39);ctx.rotate(p.attackTime>0?-1.05+Math.sin(p.attackTime/.22*Math.PI)*1.4:-.6);
  line([[0,15],[0,-58]],'#163c42',6);line([[0,15],[0,-58]],'#cdb078',3);
  paper([[-7,-53],[0,-76],[7,-53],[0,-59]],'#e8e1c6');line([[-5,8],[5,8]],'#ead7a2',3);ctx.restore();
  ellipse(0,-81,33,32,'#c8a466',colors.edge,3);ellipse(1,-84,27,26,'#e6cc91',colors.edge,1.5);
  ellipse(5,-84,20,20,'#193b43',colors.edge,2);ellipse(8,-87,15,15,'#336169');
  ellipse(12,-91,8,9,'#91c4ae44');line([[-8,-84],[22,-84]],'#bbaa7780',2);line([[7,-102],[7,-66]],'#bbaa7780',2);
  ellipse(-22,-85,3,3,'#e9deb4');ellipse(0,-109,3,2,'#f4e0ad');ellipse(24,-73,2,2,'#7d734b');
  paper([[-12,-113],[-11,-120],[3,-122],[11,-117],[8,-112]],'#ae9868');
  ctx.save();ctx.globalCompositeOperation='screen';const glow=ctx.createRadialGradient(3,-84,5,3,-84,75);glow.addColorStop(0,'#c3e9bc15');glow.addColorStop(1,'#b3e8bd00');ctx.fillStyle=glow;ctx.fillRect(-75,-160,160,150);ctx.restore();
  scratch(-20,-62,40,40,1);ctx.restore();
  if(large){ctx.save();ctx.globalAlpha=.4;line([[38,-65],[92,-98],[143,-98]],'#b6c3a2',.6);ellipse(38,-65,2,2,colors.gold);ctx.restore();}
}

function crab(e){
  shadow(0,3,33,10);const a=reduceMotion?0:Math.sin(t*7+e.phase)*4;
  const fill=e.flash>0?'#f4e5c0':'#b98070';
  for(const side of [-1,1]){ctx.save();ctx.scale(side,1);
    line([[15,-9],[31,-4],[39,4]],'#133e42',7);line([[15,-9],[31,-4],[39,4]],'#b9967a',3);
    line([[17,-17],[37,-17],[44,-8]],'#133e42',6);line([[17,-17],[37,-17],[44,-8]],'#c7a27c',3);
    ctx.save();ctx.translate(29,-29);ctx.rotate((e.tell>0?-.6:.1)+a*.025);paper([[-9,9],[-17,-4],[-14,-20],[-6,-28],[-3,-11],[5,-13],[10,-28],[18,-16],[15,-2],[4,12]],fill);ctx.restore();ctx.restore();}
  paper([[-26,-9],[-29,-28],[-17,-42],[6,-45],[27,-30],[27,-9],[5,0]],fill);
  polygon([[-18,-37],[0,-41],[16,-30],[-3,-25]],'#dcb89755',null);
  for(const x of [-10,12]){line([[x,-34],[x-2,-47]],colors.edge,4);ellipse(x-2,-49,5,6,'#e5dbad',colors.edge);ellipse(x,-49,2,3,'#142f38');}
  line([[-5,-14],[2,-11],[9,-14]],'#703d42',1.5);scratch(-20,-36,40,30,3);
}
function jelly(e,ambient=false){
  const bob=reduceMotion?0:Math.sin(t*2+e.phase)*7;
  shadow(0,6,28,8);ctx.save();ctx.translate(0,-23+bob);
  for(let i=0;i<5;i++){const x=(i-2)*9,s=reduceMotion?0:Math.sin(t*2+e.phase+i)*7;line([[x,-2],[x+s,16],[x-s*.3,33],[x+s*.5,43-Math.abs(i-2)*5]],'#152f3b',5);line([[x,-2],[x+s,16],[x-s*.3,33],[x+s*.5,43-Math.abs(i-2)*5]],ambient?'#9fbda9':'#a6a9bd',2.5);}
  paper([[-34,-5],[-31,-27],[-17,-43],[0,-48],[19,-42],[31,-25],[35,-5],[24,1],[12,-3],[1,3],[-10,-3],[-23,2]],e.flash>0?'#f1e6cb':ambient?'#8db9a6':'#969bad');
  polygon([[-28,-22],[-16,-37],[0,-42],[16,-35],[23,-22]],'#d7dec55c',null);
  line([[0,-44],[0,-10]],'#dde0c14d',1);ellipse(-10,-14,3,4,'#263e46');ellipse(12,-14,3,4,'#263e46');ellipse(-17,-10,4,2,'#ccaa9d');ellipse(19,-10,4,2,'#ccaa9d');
  ctx.restore();
}
function boss(e){
  const sway=reduceMotion?0:Math.sin(t*1.6)*4;shadow(0,9,105,26);ctx.save();ctx.translate(0,sway);
  for(let i=0;i<7;i++){const a=(i-3)*.3,tip=Math.sin(t*2+i)*12;ctx.save();ctx.translate((i-3)*17,-20);ctx.rotate(a);paper([[-10,0],[-16,30],[-8,62],[tip,83],[tip+14,78],[3,49],[13,9]],i%2?'#779b8e':'#abc0a0');ctx.restore();}
  paper([[-83,-31],[-91,-86],[-67,-145],[-18,-171],[43,-158],[83,-114],[92,-53],[50,-12],[-23,-8]],e.flash>0?'#fbebbf':'#c5ba91');
  polygon([[-83,-31],[-91,-86],[-67,-145],[-30,-125],[-51,-73],[-27,-19]],'#92a78c',null);
  ctx.beginPath();for(let a=0;a<Math.PI*5;a+=.06){const r=5+a*4.8;const x=6+Math.cos(a)*r,y=-89+Math.sin(a)*r;iLine(a,x,y);}ctx.strokeStyle='#496a62';ctx.lineWidth=5;ctx.stroke();
  ellipse(22,-80,27,22,'#173d43',colors.edge,3);ellipse(25,-80,18,15,'#8bc6aa');ellipse(29,-80,5,14,'#17343d');ellipse(32,-85,4,4,'#e4e3b8');
  paper([[-49,-156],[-61,-183],[-37,-173],[-25,-200],[-5,-172],[17,-196],[25,-165],[47,-177],[45,-154]],'#d0ac6e');
  scratch(-65,-130,120,100,5);ctx.restore();
}
function iLine(a,x,y){if(a===0)ctx.moveTo(x,y);else ctx.lineTo(x,y);}

function arch(portal=false,preview=false){
  shadow(0,0,94,23);
  if(portal){ctx.save();ctx.globalCompositeOperation='screen';const g=ctx.createRadialGradient(0,-60,5,0,-60,130);g.addColorStop(0,'#a3e1bb77');g.addColorStop(1,'#a3e1bb00');ctx.fillStyle=g;ctx.fillRect(-140,-200,280,280);ctx.restore();}
  paper([[-80,0],[-84,-127],[-67,-171],[-41,-191],[-29,-167],[-51,-139],[-51,-4]],'#688574');
  paper([[48,0],[50,-139],[28,-166],[41,-190],[71,-167],[84,-125],[79,0]],'#587c6e');
  paper([[-70,-163],[-43,-199],[0,-215],[43,-198],[72,-162],[44,-151],[18,-174],[-17,-174],[-43,-151]],'#9daf8b');
  line([[-29,-185],[0,-197],[31,-183]],'#d0c69d88',2);ellipse(0,-189,8,8,portal?'#ecdea7':'#43685e',colors.edge);
  for(const x of [-69,65]){line([[x,-24],[x-2,-91]],'#bdd0a155',2);line([[x-7,-60],[x+6,-60]],'#163f4144',2);}
  ctx.save();ctx.translate(-80,-7);coral(0,.65,1);ctx.restore();ctx.save();ctx.translate(89,0);coral(1,.8,4);ctx.restore();
  if(portal&&!preview){ctx.font='11px "DM Sans",sans-serif';ctx.textAlign='center';ctx.fillStyle='#f1d59c';ctx.fillText(game.zone===2?'RALLUMA IL CUORE':'DESCENDI ↘',0,-239);}
}
function column(s=1){ctx.save();ctx.scale(s,s);shadow(0,0,39,11);paper([[-29,0],[-32,-13],[-24,-18],[-21,-111],[-31,-121],[-27,-135],[27,-135],[32,-120],[22,-111],[25,-18],[33,-12],[29,0]],'#718977');polygon([[5,-112],[20,-111],[24,-18],[6,-20]],'#4d7163',null);line([[-10,-28],[-13,-104]],'#c8c6a255',2);paper([[-32,-125],[-27,-141],[5,-143],[28,-136],[32,-124]],'#a8b293');ctx.restore();}

function backdrop(){
  const g=ctx.createLinearGradient(0,0,0,H);g.addColorStop(0,'#123f48');g.addColorStop(.52,'#0b303b');g.addColorStop(1,'#061e2c');ctx.fillStyle=g;ctx.fillRect(0,0,W,H);
  ctx.save();ctx.globalCompositeOperation='screen';
  for(let i=0;i<5;i++){const x=W*(.3+i*.19)+(reduceMotion?0:Math.sin(t*.1+i)*30);const ray=ctx.createLinearGradient(x,0,x+70,H);ray.addColorStop(0,'#a4d9b208');ray.addColorStop(.7,'#78bba300');ctx.fillStyle=ray;polygon([[x-40,0],[x+15,0],[x+300,H],[x-70,H]],ray,null);}
  ctx.restore();
  for(let layer=0;layer<3;layer++){
    const base=H*(.64+layer*.1),shift=mode==='welcome'?Math.sin(t*.05)*10:(camera.x-900)*(.03+layer*.02);
    const pts=[[-100,H+20],[-100,base]];for(let i=0;i<14;i++)pts.push([i*W/12-shift,base-Math.sin(i*1.7+layer*2)*55-(i%3===0?90:0)]);pts.push([W+100,H+20]);polygon(pts,['#16414a','#10353e','#092b36'][layer],null);
  }
  for(const b of bubbles){const x=b.x*W+Math.sin(t*.3+b.s)*10,y=((b.y-t*b.v*.3)%1+1)%1*H;ctx.globalAlpha=.13+b.s*.025;ellipse(x,y,b.s,b.s,'#a2c6b8');}ctx.globalAlpha=1;
}

function ground(preview){
  ctx.save();const p=worldPoint(950,740);ctx.translate(p.x,p.y);ctx.scale(zoom,zoom*.64);
  const shape=[[-907,-350],[-830,-496],[-592,-580],[-320,-553],[-77,-590],[205,-570],[420,-520],[687,-542],[875,-387],[925,-120],[880,90],[918,325],[740,518],[485,573],[240,532],[-10,580],[-280,560],[-520,591],[-750,500],[-905,335],[-940,80]];
  ctx.save();ctx.translate(0,28);polygon(shape,'#133c42',null);ctx.restore();ctx.save();ctx.translate(0,14);polygon(shape,'#527b6b','#153c42',4);ctx.restore();polygon(shape,'#859d80','#234d4b',4);
  const inset=shape.map(([x,y])=>[x*.97,y*.94]);polygon(inset,'#93a488','#a5b190',1);
  ctx.save();ctx.globalAlpha=.19;
  for(let i=0;i<700;i++){const x=((i*197)%1773)-880,y=((i*113)%960)-480;ellipse(x,y,i%3===0?1.9:1,1,'#244c46');}ctx.restore();
  ctx.lineWidth=107;ctx.strokeStyle='#acb397';ctx.lineCap='round';ctx.beginPath();ctx.moveTo(-680,0);ctx.bezierCurveTo(-300,100,-200,-140,100,-20);ctx.bezierCurveTo(350,140,520,-80,750,0);ctx.stroke();
  ctx.setLineDash([3,18]);ctx.strokeStyle='#cecaac65';ctx.lineWidth=1;ctx.stroke();ctx.setLineDash([]);
  ctx.restore();
  if(!preview){
    at(250,700,1,()=>{ellipse(0,0,52,20,'#a8b39366','#d6d2ac55',1);line([[-15,0],[15,0]],'#ecdec18c',2);line([[0,-7],[0,7]],'#ecdec18c',2);});
    for(const e of game.enemies)if(e.hp>0&&e.tell>0)at(e.x,e.y,1,()=>{ctx.save();ctx.scale(1,.64);const r=e.type==='boss'?235:100;ellipse(0,0,r,r,'#bf6d6433','#df927c',2);ellipse(0,0,r*(1-e.tell/(e.type==='boss'?.95:.42)),r*(1-e.tell/(e.type==='boss'?.95:.42)),'#d7806d29');ctx.restore();});
  }
}

function drawWorld(preview){
  ground(preview);
  const objects=[];
  for(const d of decor){if(Math.abs(d.y-700)<(preview?90:150)&&d.x>230&&d.x<1750)continue;if(preview&&Math.abs(d.x-1190)<110&&d.y>760&&d.y<990)continue;objects.push({y:d.y,draw:()=>at(d.x,d.y,d.s,()=>{const p=game.player;if(!preview&&Math.abs(p.x-d.x)<65&&d.y>p.y&&d.y<p.y+150)ctx.globalAlpha=.3;coral(d.type,1,d.phase);})});}
  for(const r of ROCKS)objects.push({y:r.y+10,draw:()=>at(r.x,r.y,1,()=>rock(r))});
  objects.push({y:410,draw:()=>at(1600,410,.9,()=>column())},{y:1090,draw:()=>at(1510,1090,.68,()=>column())});
  objects.push({y:695,draw:()=>at(1710,700,1,()=>arch(preview||game.portal,preview))});
  if(game.zone===1&&!preview){for(let i=0;i<6;i++)objects.push({y:290+i*135,draw:()=>at(560+i%2*780,290+i*135,.7,()=>{paper([[-40,0],[-40,-45],[28,-45],[40,-30],[40,0]],'#9ca38b');for(let j=0;j<5;j++)paper([[-30+j*13,-6],[-30+j*13,-35],[j*13-21,-35],[j*13-21,-6]],j%2?'#547f75':'#bfa47d');})});}
  if(preview){
    objects.push({y:790,draw:()=>at(1190,790,2.1,()=>diver({angle:0,invuln:0,attackTime:0},true))});
    objects.push({y:470,draw:()=>at(1410,470,1.35,()=>jelly({phase:2,flash:0},true))});
    objects.push({y:840,draw:()=>at(1540,840,.9,()=>crab({phase:1,flash:0,tell:0}))});
  }else{
    for(const d of game.drops)objects.push({y:d.y,draw:()=>at(d.x,d.y,1,()=>{const bob=Math.sin(t*3)*4;shadow(0,0,16,5);ctx.save();ctx.translate(0,-15+bob);if(d.type==='relic'){paper([[0,-15],[10,0],[0,15],[-10,0]],'#dfbf7a');ellipse(0,0,3,4,'#41796b');}else{paper([[-8,9],[-10,-3],[-5,-9],[-5,-16],[5,-16],[5,-9],[10,-3],[8,9]],'#a5c3a3');line([[0,-3],[0,5]],'#436b5e',2);line([[-4,1],[4,1]],'#436b5e',2);}ctx.restore();})});
    for(const e of game.enemies)if(e.hp>0)objects.push({y:e.y,draw:()=>at(e.x,e.y,1,()=>{if(e.type==='boss')boss(e);else if(e.type==='jelly')jelly(e);else crab(e);if(e.hp<e.maxHp&&e.type!=='boss'){ctx.fillStyle='#173942';ctx.fillRect(-22,-83,44,4);ctx.fillStyle='#ddb486';ctx.fillRect(-22,-83,44*e.hp/e.maxHp,4);}})});
    objects.push({y:game.player.y,draw:()=>at(game.player.x,game.player.y,1,()=>diver(game.player))});
  }
  objects.sort((a,b)=>a.y-b.y);for(const o of objects)o.draw();
  if(!preview){
    for(const s of game.shots)at(s.x,s.y,1,()=>{shadow(0,0,10,3);ellipse(0,-25,s.r+5,s.r+5,'#aa91bb22');paper([[0,-25-s.r],[s.r,-25],[0,-25+s.r],[-s.r,-25]],'#c5a1c4');});
    drawEffects();
    for(const f of floaters){const p=worldPoint(f.x,f.y);ctx.save();ctx.globalAlpha=Math.min(1,f.life*2);ctx.font=`${f.type==='hurt'?17:15}px Georgia,serif`;ctx.textAlign='center';ctx.fillStyle=f.type==='hurt'?'#ffd1b0':'#fff0bd';ctx.shadowColor='#05232a';ctx.shadowBlur=3;ctx.fillText(f.text,p.x,p.y-65-(1-f.life)*45);ctx.restore();}
  }
}

function drawEffects(){
  for(const e of effects){
    at(e.x,e.y,1,()=>{
      ctx.globalAlpha=Math.min(1,e.life*2);
      if(e.type==='wave'||e.type==='slam'){
        const a=1-e.life/e.max,r=(e.type==='wave'?285:235)*a;ctx.save();ctx.scale(1,.64);ctx.beginPath();ctx.arc(0,0,Math.max(1,r),0,Math.PI*2);ctx.strokeStyle=e.type==='wave'?'#c0e7c2':'#e4a183';ctx.lineWidth=8*(1-a)+1;ctx.stroke();ctx.restore();
      }else if(e.type==='slash'){
        ctx.save();ctx.translate(0,-40);ctx.scale(1,.64);ctx.rotate(e.angle);ctx.beginPath();ctx.arc(0,0,e.heavy?115:95,-1.1,1.1);ctx.strokeStyle=e.heavy?'#f8d99b':'#e5e6be';ctx.lineWidth=e.heavy?11:6;ctx.stroke();ctx.beginPath();ctx.arc(0,0,80,-.9,.9);ctx.strokeStyle='#d9e0b644';ctx.lineWidth=15;ctx.stroke();ctx.restore();
      }else if(e.type==='heal'){ellipse(0,-50,32*(1-e.life/e.max)+10,45,'#b5e1b622','#a5e6bb99',1);}
      else if(e.type==='paper'){ctx.save();ctx.translate(e.vx*(e.max-e.life),-25+e.vy*(e.max-e.life)+(e.max-e.life)**2*70);ctx.rotate(e.life*e.vx*.05);polygon([[-4,-3],[4,-3],[3,4],[-3,3]],e.color,null);ctx.restore();}
      else if(e.type==='dash'){ctx.globalAlpha=e.life*.6;ellipse(0,-30,25,35,'#b4d2b7');}
    });
  }
}

function vignette(preview){
  const g=ctx.createRadialGradient(W*.6,H*.5,H*.15,W*.55,H*.5,Math.max(W,H)*.75);g.addColorStop(0,'#041d2500');g.addColorStop(1,'#031824b3');ctx.fillStyle=g;ctx.fillRect(0,0,W,H);
  if(preview){const shade=ctx.createLinearGradient(0,0,W,0);shade.addColorStop(0,'#082b36f5');shade.addColorStop(W<600?.5:.32,'#082b36d9');shade.addColorStop(W<600?.9:.65,'#082b3600');ctx.fillStyle=shade;ctx.fillRect(0,0,W,H);}
}

function toast(text){$('toast').textContent=text;$('toast').classList.add('visible');clearTimeout(toastTimer);toastTimer=setTimeout(()=>$('toast').classList.remove('visible'),3200);}
function tone(freq,duration=.1,type='sine',vol=.025){if(!soundOn||!audio)return;const o=audio.createOscillator(),g=audio.createGain();o.type=type;o.frequency.setValueAtTime(freq,audio.currentTime);o.frequency.exponentialRampToValueAtTime(freq*.55,audio.currentTime+duration);g.gain.setValueAtTime(vol,audio.currentTime);g.gain.exponentialRampToValueAtTime(.001,audio.currentTime+duration);o.connect(g);g.connect(audio.destination);o.start();o.stop(audio.currentTime+duration);}
function particles(x,y,n=10,color='#c5b995'){for(let i=0;i<n;i++)effects.push({type:'paper',x,y,vx:(Math.random()-.5)*220,vy:-30-Math.random()*130,life:.6+Math.random()*.5,max:1.1,color});}
function burst(type,x,y,life=.7,extra={}){effects.push({type,x,y,life,max:life,...extra});}

function processEvents(){
  const events=game.events.splice(0);
  for(const e of events){
    if(e.type==='hit'){floaters.push({...e,text:String(e.amount),life:1});particles(e.x,e.y,4);tone(360,.07,'triangle');}
    if(e.type==='death'){particles(e.x,e.y,e.boss?45:15);tone(160,.2,'triangle');}
    if(e.type==='attack'){burst('slash',game.player.x,game.player.y,.2,{angle:game.player.angle,heavy:e.heavy});tone(e.heavy?150:210,.08,'triangle');}
    if(e.type==='wave'){burst('wave',e.x,e.y);tone(350,.6);shake=reduceMotion?0:4;}
    if(e.type==='slam'){burst('slam',e.x,e.y,.6);shake=reduceMotion?0:7;tone(70,.4,'triangle');}
    if(e.type==='dash'){burst('dash',e.x,e.y,.35);tone(450,.15);}
    if(e.type==='hurt'){floaters.push({...e,text:`−${e.amount}`,life:1});shake=reduceMotion?0:6;tone(90,.2,'triangle');}
    if(e.type==='heal'){burst('heal',e.x,e.y,1);tone(630,.3);toast('Una boccata di luce · +60 vita');}
    if(e.type==='pickup'){toast(e.text);burst('heal',e.x,e.y,.7);tone(700,.2);}
    if(e.type==='level'&&!game.dead){showUpgrade();tone(840,.4);}
    if(e.type==='clear')toast(e.final?'Il guardiano è caduto. Raggiungi il cuore luminoso a est.':'Il passaggio a est si è acceso. Segui la luce →');
    if(e.type==='zone'){toast(`${ZONES[game.zone].name} · ${ZONES[game.zone].depth} metri`);camera.x=game.player.x;camera.y=game.player.y;}
    if(e.type==='defeat')showEnd(false);
    if(e.type==='victory')showEnd(true);
  }
}
function updateHUD(){
  const p=game.player,z=ZONES[game.zone];
  $('health-bar').style.width=`${p.hp/p.maxHp*100}%`;$('health-text').textContent=`${Math.ceil(p.hp)} / ${p.maxHp}`;$('level').textContent=`LV ${p.level}`;$('xp-bar').style.width=`${p.xp/p.nextXp*100}%`;$('relic-stat').textContent=`Arpione · ${p.damage}`;$('pearls').textContent=game.pearls;$('heal-count').textContent=`${p.potions} cure`;
  $('depth').textContent=`− ${z.depth} M · ${z.name.toUpperCase()}`;
  $('objective').textContent=game.portal?(game.zone===2?'Ralluma il cuore dell’abisso':'Raggiungi il passaggio luminoso →'):(game.zone===2?'Spezza la maledizione del Re':'Sconfiggi le creature d’inchiostro');
  $('progress').textContent=game.portal?'Il passaggio si trova a est della mappa':`${game.kills} / ${z.count} creature`;
  $('wave-cd').style.height=`${p.waveCd/5.5*100}%`;$('dash-cd').style.height=`${p.dashCd/1.5*100}%`;
  const boss=game.enemies.find(e=>e.type==='boss'&&e.hp>0&&e.active);$('boss-hud').hidden=!boss;if(boss)$('boss-bar').style.width=`${boss.hp/boss.maxHp*100}%`;
}
function clearInput(){keys.clear();mouse.down=false;attackHeld=false;stick.x=stick.y=0;$('joystick').firstElementChild.style.transform='';joyPointer=null;}
function modal(eyebrow,title,text,button,action){
  clearInput();$('modal').hidden=false;$('modal-eyebrow').textContent=eyebrow;$('modal-title').textContent=title;$('modal-text').textContent=text;$('choices').replaceChildren();$('modal-action').hidden=!button;$('modal-action').textContent=button||'';$('modal-action').onclick=action;
  if(button)requestAnimationFrame(()=>$('modal-action').focus());
}
function resume(){mode='play';$('modal').hidden=true;canvas.focus();}
function showUpgrade(){
  mode='upgrade';modal(`LIVELLO ${game.player.level} · UNA NUOVA PIEGA`,'La luce ti cambia.','Scegli una reliquia per continuare la discesa.',null);
  const choices=[['damage','↗','Dente di corallo','+6 danni all’arpione'],['health','♡','Conchiglia antica','+30 vita massima e cura'],['tide','≋','Pinna di carta','Più velocità, +2 danni e una cura']];
  for(const [kind,symbol,title,desc]of choices){const b=document.createElement('button');b.className='choice';b.innerHTML=`<span>${symbol}</span><b>${title}</b><small>${desc}</small>`;b.onclick=()=>{game.upgrade(kind);resume();toast(title+' · Reliquia equipaggiata');};$('choices').append(b);}
  requestAnimationFrame(()=>$('choices').firstElementChild.focus());
}
function showEnd(win){
  mode=win?'victory':'defeat';
  modal(win?'IL MARE RICORDERÀ IL TUO NOME':'ANCHE LA CARTA PUÒ RICOMINCIARE',win?'Una luce, in fondo.':'Il mare ti ha abbracciato.',win?`Hai riportato la luce nel cuore dell’abisso.\n${game.pearls} perle raccolte · Livello ${game.player.level} · Tre profondità esplorate.`:`La tua storia si è fermata a ${ZONES[game.zone].depth} metri.\n${game.pearls} perle raccolte. Una nuova discesa ti aspetta.`,win?'Immergiti ancora ↘':'Riprova la discesa ↘',start);
}
function pause(){
  if(mode==='pause'){resume();return;}if(mode!=='play')return;
  mode='pause';modal('UN RESPIRO PRIMA DI CONTINUARE','Il mare può aspettare.','WASD o frecce per muoverti · Mouse o J per l’arpione\nE: marea circolare · Spazio: scatto invulnerabile · Q: cura\nRaccogli le reliquie e raggiungi gli archi luminosi a est.','Riprendi la discesa',resume);
}
function start(){
  game=new Expedition();mode='play';effects.length=0;floaters.length=0;clearInput();$('welcome').hidden=true;$('hud').hidden=false;$('modal').hidden=true;document.body.classList.add('playing');camera.x=game.player.x;camera.y=game.player.y;mouse.aim=false;updateHUD();toast(matchMedia('(pointer:coarse)').matches?'Muoviti con il joystick · Usa i pulsanti per combattere':'Benvenuto negli abissi · WASD per muoverti, J per l’arpione');
}
$('start').onclick=start;$('pause').onclick=pause;
$('sound').onclick=async()=>{soundOn=!soundOn;if(soundOn){try{audio??=new(window.AudioContext||window.webkitAudioContext)();await audio.resume();tone(620,.25);}catch{soundOn=false;}}$('sound').innerHTML=`♪ <span>Suono ${soundOn?'on':'off'}</span>`;$('sound').setAttribute('aria-label',soundOn?'Disattiva suoni':'Attiva suoni');};
function movement(){return{x:(keys.has('d')||keys.has('arrowright')?1:0)-(keys.has('a')||keys.has('arrowleft')?1:0)+stick.x,y:(keys.has('s')||keys.has('arrowdown')?1:0)-(keys.has('w')||keys.has('arrowup')?1:0)+stick.y};}
function action(kind){if(mode!=='play')return;if(kind==='wave'){if(!game.wave())toast('La marea si sta ricaricando…');}if(kind==='dash'){const m=movement();game.dash(m.x,m.y);}if(kind==='heal'&&!game.heal())toast(game.player.potions===0?'Hai esaurito le cure. Cerca le conchiglie verdi.':'La tua luce è già piena.');}
for(const kind of ['wave','dash','heal'])$(kind).addEventListener('pointerdown',e=>{e.preventDefault();action(kind);});
$('attack').addEventListener('pointerdown',e=>{e.preventDefault();if(mode==='play'){attackHeld=true;$('attack').setPointerCapture(e.pointerId);}});
for(const ev of ['pointerup','pointercancel','lostpointercapture'])$('attack').addEventListener(ev,()=>attackHeld=false);
addEventListener('keydown',e=>{
  const key=e.key.toLowerCase();if([' ','arrowup','arrowdown','arrowleft','arrowright'].includes(key))e.preventDefault();
  if(key==='escape'){pause();return;}
  if(mode!=='play')return;keys.add(key);if(e.repeat)return;if(key==='e')action('wave');if(key===' ')action('dash');if(key==='q')action('heal');
});
addEventListener('keyup',e=>keys.delete(e.key.toLowerCase()));
addEventListener('blur',()=>{clearInput();if(mode==='play')pause();});
document.addEventListener('visibilitychange',()=>{if(document.hidden&&mode==='play')pause();});
canvas.addEventListener('pointermove',e=>{if(e.pointerType==='mouse'){mouse.x=e.clientX;mouse.y=e.clientY;mouse.aim=true;}});
canvas.addEventListener('pointerdown',e=>{if(mode!=='play')return;if(e.pointerType==='mouse'){mouse.x=e.clientX;mouse.y=e.clientY;mouse.aim=true;mouse.down=true;canvas.setPointerCapture(e.pointerId);}else{const target=screenPoint(e.clientX,e.clientY);game.player.angle=Math.atan2(target.y-game.player.y,target.x-game.player.x);game.attack();}});
for(const ev of ['pointerup','pointercancel','lostpointercapture'])canvas.addEventListener(ev,()=>mouse.down=false);
canvas.addEventListener('contextmenu',e=>e.preventDefault());
const joystick=$('joystick');
function moveStick(e){if(e.pointerId!==joyPointer)return;const r=joystick.getBoundingClientRect(),dx=e.clientX-r.left-r.width/2,dy=e.clientY-r.top-r.height/2,d=Math.hypot(dx,dy),s=Math.min(32,d);stick.x=d>3?dx/d*Math.min(1,d/32):0;stick.y=d>3?dy/d*Math.min(1,d/32):0;joystick.firstElementChild.style.transform=`translate(${d?dx/d*s:0}px,${d?dy/d*s:0}px)`;}
joystick.addEventListener('pointerdown',e=>{e.preventDefault();joyPointer=e.pointerId;mouse.aim=false;joystick.setPointerCapture(e.pointerId);moveStick(e);});joystick.addEventListener('pointermove',moveStick);
for(const ev of ['pointerup','pointercancel','lostpointercapture'])joystick.addEventListener(ev,e=>{if(e.pointerId===joyPointer){joyPointer=null;stick.x=stick.y=0;joystick.firstElementChild.style.transform='';}});

function frame(now){
  const dt=Math.min((now-last)/1000||.016,.05);last=now;if(mode==='welcome'||mode==='play')t+=dt;
  if(mode==='play'){
    const m=movement(),input={...m,attack:mouse.down||keys.has('j')||attackHeld};
    if(mouse.aim){const target=screenPoint(mouse.x,mouse.y);input.angle=Math.atan2(target.y-game.player.y,target.x-game.player.x);}
    game.update(dt,input);processEvents();updateHUD();
    for(const e of effects)e.life-=dt;for(let i=effects.length-1;i>=0;i--)if(effects[i].life<=0)effects.splice(i,1);
    for(const f of floaters)f.life-=dt;for(let i=floaters.length-1;i>=0;i--)if(floaters[i].life<=0)floaters.splice(i,1);
    const follow=1-Math.exp(-dt*5);camera.x+=(game.player.x-camera.x)*follow;camera.y+=(game.player.y-camera.y)*follow;
  }
  const preview=mode==='welcome';
  if(preview){camera.x=W<600?1060:930;camera.y=W<600?410:675;zoom=W<600?Math.max(.52,H/1250):Math.min(1.07,W/1500,H/820);}
  else zoom=W<600?.77:Math.min(1.2,Math.max(.9,W/1400));
  ctx.setTransform(DPR,0,0,DPR,0,0);ctx.clearRect(0,0,W,H);backdrop();ctx.save();if(shake>0&&!reduceMotion){ctx.translate((Math.random()-.5)*shake,(Math.random()-.5)*shake);shake=Math.max(0,shake-dt*22);}drawWorld(preview);ctx.restore();vignette(preview);
  requestAnimationFrame(frame);
}
requestAnimationFrame(frame);
