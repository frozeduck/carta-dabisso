import React, { useEffect, useRef } from 'react';
import { GameEngine } from '../game/GameEngine';
import { Enemy, Drop, Particle, Floater, ActChest, Projectile } from '../types/game';

interface GameCanvasProps {
  engine: GameEngine;
  onOpenStation: (station: 'BLACKSMITH' | 'SHOP' | 'STASH' | 'SAGE' | 'WAYPOINT') => void;
  onInteractChest?: (chest: ActChest) => void;
}

export const GameCanvas: React.FC<GameCanvasProps> = ({
  engine,
  onOpenStation,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const spritesRef = useRef<Record<string, HTMLImageElement>>({});

  // Preload sprites
  useEffect(() => {
    const spriteNames = [
      'anemone', 'anziano_tartaruga', 'boss_leviatano', 'cassa_tesoro',
      'classe_cacciatore', 'classe_custode', 'classe_mistico', 'conchiglia',
      'corallo_rosso', 'corallo_verde', 'fabbro_astice', 'granchio',
      'medusa', 'murena', 'paguro_bottegaio', 'palombaro', 'pesce_lanterna',
      'portale_citta', 'stella_marina'
    ];

    spriteNames.forEach((name) => {
      const img = new Image();
      img.src = `/sprites/${name}.png`;
      spritesRef.current[name] = img;
    });
  }, []);

  // 60 FPS Canvas Game Loop
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animId: number;
    let lastTime = performance.now();

    const render = (time: number) => {
      const dt = Math.min(0.1, (time - lastTime) / 1000);
      lastTime = time;

      // Update engine physics
      engine.update(dt);

      // Canvas dimensions
      const width = canvas.width;
      const height = canvas.height;

      // Calculate camera position centered on player
      const mapW = engine.getMapWidth();
      const mapH = engine.getMapHeight();

      let camX = engine.playerX - width / 2;
      let camY = engine.playerY - height / 2;

      // Screen shake (trauma^2)
      if (engine.trauma > 0) {
        const shake = engine.trauma * engine.trauma * 14;
        camX += (Math.random() * 2 - 1) * shake;
        camY += (Math.random() * 2 - 1) * shake;
      }

      // Clamp camera
      camX = Math.max(0, Math.min(mapW - width, camX));
      camY = Math.max(0, Math.min(mapH - height, camY));

      // Clear & Background
      ctx.save();
      ctx.clearRect(0, 0, width, height);

      // Translate to camera coordinates
      ctx.translate(-camX, -camY);

      // Draw Environment
      if (engine.gameState === 'HUB') {
        drawHubEnvironment(ctx, mapW, mapH, time / 1000);
      } else {
        drawDungeonEnvironment(ctx, mapW, mapH, engine.currentAct, time / 1000);
      }

      // Draw Sunken Chests
      drawActChests(ctx, engine.actChests, time / 1000);

      // Draw Portal (Dungeon exit or Town Portal)
      if (engine.gameState === 'EXPEDITION' && engine.portalOpen) {
        drawPortal(ctx, engine.portalX, engine.portalY, '#38bdf8', 'PORTALE DEGLI ABISSI', time / 1000);
      }
      if (engine.gameState === 'HUB' && engine.townPortalActive) {
        drawPortal(
          ctx,
          GameEngine.HUB_TOWN_PORTAL_X,
          GameEngine.HUB_TOWN_PORTAL_Y,
          '#06b6d4',
          'PORTALE DEL DUNGEON',
          time / 1000
        );
      }

      // Draw Floor Drops & Loot Beams
      drawDrops(ctx, engine.drops, time / 1000);

      // Draw Click Beacon
      if (engine.beaconTimer > 0) {
        drawBeacon(ctx, engine.beaconX, engine.beaconY, engine.beaconTimer);
      }

      // Draw Enemies
      engine.enemies.forEach((e) => {
        drawEnemy(ctx, e, time / 1000);
      });

      // Draw Player
      drawPlayer(ctx, engine, time / 1000);

      // Draw Projectiles
      drawProjectiles(ctx, engine.projectiles);

      // Draw Particles
      drawParticles(ctx, engine.particles);

      // Draw Comic Damage Floaters
      drawFloaters(ctx, engine.floaters);

      ctx.restore();

      animId = requestAnimationFrame(render);
    };

    animId = requestAnimationFrame(render);
    return () => cancelAnimationFrame(animId);
  }, [engine]);

  // Handle Canvas Resize
  useEffect(() => {
    const handleResize = () => {
      if (!canvasRef.current) return;
      canvasRef.current.width = window.innerWidth;
      canvasRef.current.height = window.innerHeight;
    };
    handleResize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Click & Touch Navigation
  const handlePointerDown = (e: React.PointerEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const screenX = e.clientX - rect.left;
    const screenY = e.clientY - rect.top;

    const width = canvas.width;
    const height = canvas.height;
    const mapW = engine.getMapWidth();
    const mapH = engine.getMapHeight();

    const camX = Math.max(0, Math.min(mapW - width, engine.playerX - width / 2));
    const camY = Math.max(0, Math.min(mapH - height, engine.playerY - height / 2));

    const worldX = screenX + camX;
    const worldY = screenY + camY;

    // Check interaction with Hub Stations
    if (engine.gameState === 'HUB') {
      const distToBlacksmith = Math.hypot(worldX - GameEngine.HUB_BLACKSMITH_X, worldY - GameEngine.HUB_BLACKSMITH_Y);
      if (distToBlacksmith < 70) {
        onOpenStation('BLACKSMITH');
        return;
      }
      const distToMerchant = Math.hypot(worldX - GameEngine.HUB_MERCHANT_X, worldY - GameEngine.HUB_MERCHANT_Y);
      if (distToMerchant < 70) {
        onOpenStation('SHOP');
        return;
      }
      const distToStash = Math.hypot(worldX - GameEngine.HUB_STASH_X, worldY - GameEngine.HUB_STASH_Y);
      if (distToStash < 70) {
        onOpenStation('STASH');
        return;
      }
      const distToSage = Math.hypot(worldX - GameEngine.HUB_SAGE_X, worldY - GameEngine.HUB_SAGE_Y);
      if (distToSage < 70) {
        onOpenStation('SAGE');
        return;
      }
      const distToWaypoint = Math.hypot(worldX - GameEngine.HUB_WAYPOINT_X, worldY - GameEngine.HUB_WAYPOINT_Y);
      if (distToWaypoint < 70) {
        onOpenStation('WAYPOINT');
        return;
      }
      if (engine.townPortalActive) {
        const distToTownPortal = Math.hypot(worldX - GameEngine.HUB_TOWN_PORTAL_X, worldY - GameEngine.HUB_TOWN_PORTAL_Y);
        if (distToTownPortal < 60) {
          engine.enterTownPortalToDungeon();
          return;
        }
      }
    }

    // Check if clicked an enemy to target and attack directly
    if (engine.gameState === 'EXPEDITION') {
      for (const en of engine.enemies) {
        const d = Math.hypot(worldX - en.x, worldY - en.y);
        if (d <= en.radius + 15) {
          engine.performAttack(en.x, en.y);
          return;
        }
      }
    }

    // Move player
    engine.setPlayerTarget(worldX, worldY);
  };

  // Helper Drawing Functions
  const drawHubEnvironment = (ctx: CanvasRenderingContext2D, mapW: number, mapH: number, time: number) => {
    // 1. Deep Oceanic Gradient
    const bgGrad = ctx.createLinearGradient(0, 0, 0, mapH);
    bgGrad.addColorStop(0, '#041c32');
    bgGrad.addColorStop(0.5, '#06283d');
    bgGrad.addColorStop(1, '#082032');
    ctx.fillStyle = bgGrad;
    ctx.fillRect(0, 0, mapW, mapH);

    // 2. Sandy Seabed with Paper Dunes & Handcrafted Stitching
    const sandTop = mapH * 0.25;
    ctx.fillStyle = '#1e3a5f';
    ctx.beginPath();
    ctx.moveTo(0, mapH);
    ctx.lineTo(0, sandTop + 30);
    for (let x = 0; x <= mapW; x += 100) {
      const y = sandTop + Math.sin(x * 0.005 + 1) * 25 + Math.cos(x * 0.01) * 15;
      ctx.lineTo(x, y);
    }
    ctx.lineTo(mapW, mapH);
    ctx.closePath();
    ctx.fill();

    // Sand stitching dashes (Paper Mario craft vibe)
    ctx.strokeStyle = 'rgba(56, 189, 248, 0.35)';
    ctx.lineWidth = 2;
    ctx.setLineDash([8, 8]);
    ctx.beginPath();
    for (let x = 20; x <= mapW - 20; x += 100) {
      const y = sandTop + Math.sin(x * 0.005 + 1) * 25 + Math.cos(x * 0.01) * 15 + 4;
      if (x === 20) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.stroke();
    ctx.setLineDash([]);

    // 3. Central Circular Cobblestone Plaza & Nautilus Spiral Mosaic
    const cx = 640;
    const cy = 440;
    const plazaR = 210;

    // Outer cobblestone ring
    ctx.fillStyle = 'rgba(15, 23, 42, 0.7)';
    ctx.beginPath();
    ctx.arc(cx, cy, plazaR, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#38bdf8';
    ctx.lineWidth = 3;
    ctx.stroke();

    // Decorative radial spokes
    ctx.strokeStyle = 'rgba(56, 189, 248, 0.2)';
    ctx.lineWidth = 1.5;
    for (let i = 0; i < 12; i++) {
      const a = (i * Math.PI) / 6;
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.lineTo(cx + Math.cos(a) * plazaR, cy + Math.sin(a) * plazaR);
      ctx.stroke();
    }

    // Nautilus Spiral Mosaic at center
    ctx.strokeStyle = '#fbbf24';
    ctx.lineWidth = 3;
    ctx.beginPath();
    for (let theta = 0; theta < Math.PI * 6; theta += 0.1) {
      const r = 4 + theta * 6;
      const sx = cx + Math.cos(theta) * r;
      const sy = cy + Math.sin(theta) * r;
      if (theta === 0) ctx.moveTo(sx, sy);
      else ctx.lineTo(sx, sy);
    }
    ctx.stroke();

    // 4. Cobblestone Paths to Hub Stations
    const stations = [
      { x: GameEngine.HUB_BLACKSMITH_X, y: GameEngine.HUB_BLACKSMITH_Y },
      { x: GameEngine.HUB_MERCHANT_X, y: GameEngine.HUB_MERCHANT_Y },
      { x: GameEngine.HUB_STASH_X, y: GameEngine.HUB_STASH_Y },
      { x: GameEngine.HUB_SAGE_X, y: GameEngine.HUB_SAGE_Y },
      { x: GameEngine.HUB_WAYPOINT_X, y: GameEngine.HUB_WAYPOINT_Y },
    ];

    ctx.strokeStyle = 'rgba(56, 189, 248, 0.25)';
    ctx.lineWidth = 18;
    stations.forEach((st) => {
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.lineTo(st.x, st.y);
      ctx.stroke();
    });

    // 5. Ambient Corals, Columns and Seaweed
    drawAncientColumn(ctx, 160, 280);
    drawAncientColumn(ctx, 1220, 280);
    drawAncientColumn(ctx, 200, 680);
    drawAncientColumn(ctx, 1180, 680);

    drawPaperCoral(ctx, 100, 480, '#ef4444', time);
    drawPaperCoral(ctx, 1280, 480, '#10b981', time + 1);
    drawPaperCoral(ctx, 750, 200, '#8b5cf6', time + 2);

    // 6. Draw Stations & NPCs
    drawHubStation(ctx, GameEngine.HUB_BLACKSMITH_X, GameEngine.HUB_BLACKSMITH_Y, 'fabbro_astice', 'Mastro Astice', 'Fabbro & Forgia', '#f97316');
    drawHubStation(ctx, GameEngine.HUB_MERCHANT_X, GameEngine.HUB_MERCHANT_Y, 'paguro_bottegaio', 'Mastro Paguro', 'Alchimia & Bottega', '#38bdf8');
    drawHubStation(ctx, GameEngine.HUB_STASH_X, GameEngine.HUB_STASH_Y, 'cassa_tesoro', 'Cassa di Spedizione', 'Deposito 16 Slot', '#fbbf24');
    drawHubStation(ctx, GameEngine.HUB_SAGE_X, GameEngine.HUB_SAGE_Y, 'anziano_tartaruga', 'Anziana Tartaruga', 'Cronache & Taglie', '#34d399');
    drawHubStation(ctx, GameEngine.HUB_WAYPOINT_X, GameEngine.HUB_WAYPOINT_Y, 'portale_citta', 'Portale di Spedizione', 'Seleziona Atto', '#a855f7');
  };

  const drawDungeonEnvironment = (ctx: CanvasRenderingContext2D, mapW: number, mapH: number, act: number, time: number) => {
    // Gradient based on depth
    const bgGrad = ctx.createLinearGradient(0, 0, 0, mapH);
    if (act === 1) {
      bgGrad.addColorStop(0, '#0c2444');
      bgGrad.addColorStop(0.6, '#081c34');
      bgGrad.addColorStop(1, '#051222');
    } else if (act === 2) {
      bgGrad.addColorStop(0, '#050c18');
      bgGrad.addColorStop(0.6, '#030810');
      bgGrad.addColorStop(1, '#010306');
    } else if (act === 3) {
      bgGrad.addColorStop(0, '#1a0d0d');
      bgGrad.addColorStop(0.6, '#260a0a');
      bgGrad.addColorStop(1, '#120404');
    } else {
      bgGrad.addColorStop(0, '#060312');
      bgGrad.addColorStop(0.6, '#0b041e');
      bgGrad.addColorStop(1, '#03010a');
    }
    ctx.fillStyle = bgGrad;
    ctx.fillRect(0, 0, mapW, mapH);

    // Deep seabed ground
    const sandTop = mapH * 0.35;
    ctx.fillStyle = act === 3 ? '#3b1812' : '#0c223c';
    ctx.beginPath();
    ctx.moveTo(0, mapH);
    ctx.lineTo(0, sandTop);
    for (let x = 0; x <= mapW; x += 120) {
      const y = sandTop + Math.sin(x * 0.003 + 2) * 40;
      ctx.lineTo(x, y);
    }
    ctx.lineTo(mapW, mapH);
    ctx.closePath();
    ctx.fill();

    // Sun Rays / Underwater Caustics
    ctx.save();
    ctx.globalAlpha = 0.08;
    for (let r = 0; r < 6; r++) {
      const rx = (mapW / 6) * r + Math.sin(time + r) * 60;
      const rayGrad = ctx.createLinearGradient(rx, 0, rx + 120, mapH);
      rayGrad.addColorStop(0, '#38bdf8');
      rayGrad.addColorStop(1, 'transparent');
      ctx.fillStyle = rayGrad;
      ctx.beginPath();
      ctx.moveTo(rx, 0);
      ctx.lineTo(rx + 140, 0);
      ctx.lineTo(rx + 220, mapH);
      ctx.lineTo(rx + 60, mapH);
      ctx.closePath();
      ctx.fill();
    }
    ctx.restore();

    // Coral Reefs and Ruined Pillars
    drawPaperCoral(ctx, 350, 850, '#e11d48', time);
    drawPaperCoral(ctx, mapW - 350, 850, '#059669', time + 1);
    drawPaperCoral(ctx, mapW / 2 - 300, 450, '#d97706', time + 2);
    drawPaperCoral(ctx, mapW / 2 + 300, 450, '#7c3aed', time + 3);

    drawAncientColumn(ctx, 280, 520);
    drawAncientColumn(ctx, mapW - 280, 520);
  };

  const drawHubStation = (
    ctx: CanvasRenderingContext2D,
    x: number,
    y: number,
    spriteName: string,
    title: string,
    subtitle: string,
    badgeColor: string
  ) => {
    // Shadow
    drawFloorShadow(ctx, x, y + 25, 38, 14);

    // Sprite or Fallback
    const img = spritesRef.current[spriteName];
    if (img && img.complete && img.naturalWidth > 0) {
      const s = 68;
      ctx.drawImage(img, x - s / 2, y - s / 2 - 8, s, s);
    } else {
      // Fallback stylized icon
      ctx.fillStyle = badgeColor;
      ctx.beginPath();
      ctx.arc(x, y - 8, 28, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#fff';
      ctx.lineWidth = 2;
      ctx.stroke();
    }

    // Signboard Cartouche
    const signY = y - 48;
    ctx.font = 'bold 12px Cinzel, serif';
    const textW = ctx.measureText(title).width;
    const boxW = Math.max(textW + 20, 110);

    ctx.fillStyle = 'rgba(15, 23, 42, 0.9)';
    ctx.strokeStyle = badgeColor;
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.roundRect(x - boxW / 2, signY - 14, boxW, 28, 6);
    ctx.fill();
    ctx.stroke();

    ctx.fillStyle = '#ffffff';
    ctx.textAlign = 'center';
    ctx.fillText(title, x, signY);

    ctx.font = '9px Outfit, sans-serif';
    ctx.fillStyle = badgeColor;
    ctx.fillText(subtitle, x, signY + 10);
  };

  const drawPortal = (
    ctx: CanvasRenderingContext2D,
    x: number,
    y: number,
    color: string,
    title: string,
    time: number
  ) => {
    drawFloorShadow(ctx, x, y + 20, 46, 16);

    ctx.save();
    ctx.translate(x, y);

    // Rotating swirling vortex rings
    for (let r = 0; r < 3; r++) {
      ctx.save();
      ctx.rotate(time * (r % 2 === 0 ? 1 : -1) * (1.5 + r * 0.5));
      ctx.strokeStyle = color;
      ctx.lineWidth = 3 - r * 0.8;
      ctx.beginPath();
      ctx.ellipse(0, 0, 42 - r * 10, 24 - r * 6, 0, 0, Math.PI * 2);
      ctx.stroke();
      ctx.restore();
    }

    // Core glow
    const glow = ctx.createRadialGradient(0, 0, 5, 0, 0, 35);
    glow.addColorStop(0, color);
    glow.addColorStop(1, 'transparent');
    ctx.fillStyle = glow;
    ctx.beginPath();
    ctx.arc(0, 0, 35, 0, Math.PI * 2);
    ctx.fill();

    ctx.restore();

    // Floating Label
    ctx.font = 'bold 11px Cinzel, serif';
    ctx.fillStyle = color;
    ctx.textAlign = 'center';
    ctx.fillText(title, x, y - 40);
  };

  const drawDrops = (ctx: CanvasRenderingContext2D, drops: Drop[], time: number) => {
    drops.forEach((d) => {
      drawFloorShadow(ctx, d.x, d.y + 8, 18, 7);

      if (d.shellAmount) {
        // Gold Conchiglia Shell
        const img = spritesRef.current['conchiglia'];
        if (img && img.complete && img.naturalWidth > 0) {
          ctx.drawImage(img, d.x - 14, d.y - 14, 28, 28);
        } else {
          ctx.fillStyle = '#f59e0b';
          ctx.beginPath();
          ctx.arc(d.x, d.y, 10, 0, Math.PI * 2);
          ctx.fill();
        }
      } else if (d.shardAmount) {
        // Abyssal Shard
        ctx.fillStyle = '#38bdf8';
        ctx.beginPath();
        ctx.moveTo(d.x, d.y - 12);
        ctx.lineTo(d.x + 8, d.y);
        ctx.lineTo(d.x, d.y + 12);
        ctx.lineTo(d.x - 8, d.y);
        ctx.closePath();
        ctx.fill();
        ctx.strokeStyle = '#fff';
        ctx.lineWidth = 1.5;
        ctx.stroke();
      } else if (d.item) {
        // Floor Loot Beam (Diablo Rarity Light Ray)
        const col = engine.getRarityColor(d.item.rarity);

        // Vertical beam
        ctx.save();
        const beamGrad = ctx.createLinearGradient(d.x, d.y, d.x, d.y - 160);
        beamGrad.addColorStop(0, col);
        beamGrad.addColorStop(1, 'transparent');
        ctx.fillStyle = beamGrad;
        ctx.globalAlpha = 0.55 + Math.sin(time * 6) * 0.15;
        ctx.fillRect(d.x - 4, d.y - 160, 8, 160);
        ctx.restore();

        // Pulsing core
        ctx.fillStyle = col;
        ctx.beginPath();
        ctx.arc(d.x, d.y, 8, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 2;
        ctx.stroke();

        // Item title banner
        ctx.font = 'bold 10px Outfit, sans-serif';
        const w = ctx.measureText(d.item.name).width;
        ctx.fillStyle = 'rgba(15, 23, 42, 0.85)';
        ctx.beginPath();
        ctx.roundRect(d.x - w / 2 - 6, d.y - 28, w + 12, 18, 4);
        ctx.fill();
        ctx.strokeStyle = col;
        ctx.lineWidth = 1;
        ctx.stroke();

        ctx.fillStyle = col;
        ctx.textAlign = 'center';
        ctx.fillText(d.item.name, d.x, d.y - 15);
      }
    });
  };

  const drawActChests = (ctx: CanvasRenderingContext2D, chests: ActChest[], time: number) => {
    chests.forEach((c) => {
      drawFloorShadow(ctx, c.x, c.y + 12, 28, 10);

      const img = spritesRef.current['cassa_tesoro'];
      if (img && img.complete && img.naturalWidth > 0) {
        ctx.drawImage(img, c.x - 24, c.y - 24, 48, 48);
      } else {
        ctx.fillStyle = c.opened ? '#94a3b8' : '#d97706';
        ctx.fillRect(c.x - 20, c.y - 16, 40, 26);
        ctx.strokeStyle = '#fbbf24';
        ctx.lineWidth = 2;
        ctx.strokeRect(c.x - 20, c.y - 16, 40, 26);
      }

      if (!c.opened) {
        // Floating Sparkle
        ctx.fillStyle = '#fcd34d';
        ctx.beginPath();
        ctx.arc(c.x + Math.sin(time * 3) * 15, c.y - 24 + Math.cos(time * 3) * 6, 3, 0, Math.PI * 2);
        ctx.fill();

        ctx.font = 'bold 10px Outfit, sans-serif';
        ctx.fillStyle = '#fbbf24';
        ctx.textAlign = 'center';
        ctx.fillText('FORZIERE SOMMERSO', c.x, c.y - 32);
      }
    });
  };

  const drawBeacon = (ctx: CanvasRenderingContext2D, x: number, y: number, timer: number) => {
    const progress = 1 - timer / 0.6;
    const r = 12 + progress * 24;
    ctx.strokeStyle = `rgba(251, 191, 36, ${1 - progress})`;
    ctx.lineWidth = 2.5;
    ctx.beginPath();
    ctx.arc(x, y, r, 0, Math.PI * 2);
    ctx.stroke();
  };

  const drawEnemy = (ctx: CanvasRenderingContext2D, e: Enemy, time: number) => {
    // Shadow
    drawFloorShadow(ctx, e.x, e.y + e.radius * 0.75, e.radius * 1.1, e.radius * 0.45);

    // Telegraph windup circle for bosses
    if (e.isBoss && e.windupTimer && e.windupTimer > 0 && e.windupX !== undefined && e.windupY !== undefined) {
      const progress = 1 - e.windupTimer / (e.windupMaxTimer || 1.2);
      ctx.save();
      ctx.strokeStyle = '#ef4444';
      ctx.lineWidth = 2;
      ctx.setLineDash([6, 6]);
      ctx.beginPath();
      ctx.arc(e.windupX, e.windupY, e.windupRadius || 110, 0, Math.PI * 2);
      ctx.stroke();

      // Expanding fill
      ctx.fillStyle = 'rgba(239, 68, 68, 0.25)';
      ctx.beginPath();
      ctx.arc(e.windupX, e.windupY, (e.windupRadius || 110) * progress, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    }

    ctx.save();
    ctx.translate(e.x, e.y);

    // Hit squash
    if (e.hitSquash > 0) {
      ctx.scale(1 + e.hitSquash * 0.35, 1 - e.hitSquash * 0.35);
    }

    // Facing flip
    if (e.facing !== undefined && Math.cos(e.facing) < 0) {
      ctx.scale(-1, 1);
    }

    // Sprite drawing
    let spriteKey = 'granchio';
    if (e.type === 'jelly') spriteKey = 'medusa';
    else if (e.type === 'eel') spriteKey = 'murena';
    else if (e.type === 'angler') spriteKey = 'pesce_lanterna';
    else if (e.type === 'boss') spriteKey = 'boss_leviatano';

    const img = spritesRef.current[spriteKey];
    const s = e.radius * 2.3;

    if (img && img.complete && img.naturalWidth > 0) {
      // Paper Mario Sticker white border glow
      ctx.shadowColor = '#ffffff';
      ctx.shadowBlur = 3;
      ctx.drawImage(img, -s / 2, -s / 2, s, s);
      ctx.shadowBlur = 0;
    } else {
      // Procedural fallback
      ctx.fillStyle = e.type === 'boss' ? '#b91c1c' : '#0284c7';
      ctx.beginPath();
      ctx.arc(0, 0, e.radius, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#ffffff';
      ctx.lineWidth = 2;
      ctx.stroke();
    }

    ctx.restore();

    // HP Bar above regular enemy
    if (!e.isBoss) {
      const barW = Math.max(34, e.radius * 2);
      const barH = 5;
      const barY = e.y - e.radius - 12;
      const pct = Math.max(0, e.hp / e.maxHp);

      ctx.fillStyle = 'rgba(0, 0, 0, 0.6)';
      ctx.fillRect(e.x - barW / 2, barY, barW, barH);
      ctx.fillStyle = pct > 0.4 ? '#ef4444' : '#f59e0b';
      ctx.fillRect(e.x - barW / 2, barY, barW * pct, barH);
      ctx.strokeStyle = '#fff';
      ctx.lineWidth = 1;
      ctx.strokeRect(e.x - barW / 2, barY, barW, barH);
    }
  };

  const drawPlayer = (ctx: CanvasRenderingContext2D, eng: GameEngine, time: number) => {
    // Shadow
    drawFloorShadow(ctx, eng.playerX, eng.playerY + 22, 28, 10);

    ctx.save();
    ctx.translate(eng.playerX, eng.playerY);

    // Squash & Stretch
    let sx = 1;
    let sy = 1;
    if (eng.playerSquash > 0) {
      sx = 1 + eng.playerSquash * 0.3;
      sy = 1 - eng.playerSquash * 0.3;
    }
    if (eng.dashAnim > 0) {
      sx = 1.35;
      sy = 0.75;
    }
    ctx.scale(sx, sy);

    // Flip towards facing angle
    if (Math.cos(eng.playerFacingAngle) < 0) {
      ctx.scale(-1, 1);
    }

    // Sprite drawing based on class
    let spriteName = 'classe_cacciatore';
    if (eng.playerClass === 'juggernaut') spriteName = 'classe_custode';
    else if (eng.playerClass === 'mystic') spriteName = 'classe_mistico';

    const img = spritesRef.current[spriteName] || spritesRef.current['palombaro'];
    const s = 64;

    if (img && img.complete && img.naturalWidth > 0) {
      // Paper Mario white cutout contour
      ctx.shadowColor = '#ffffff';
      ctx.shadowBlur = 4;
      ctx.drawImage(img, -s / 2, -s / 2 - 4, s, s);
      ctx.shadowBlur = 0;
    } else {
      // Fallback diver
      ctx.fillStyle = eng.getSuitColorHex();
      ctx.beginPath();
      ctx.arc(0, -6, 20, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#ffffff';
      ctx.lineWidth = 2.5;
      ctx.stroke();

      // Mask visor
      ctx.fillStyle = '#67e8f9';
      ctx.beginPath();
      ctx.arc(6, -6, 9, 0, Math.PI * 2);
      ctx.fill();
    }

    // Orbiting Relic Wisp
    if (eng.equippedRelic) {
      const relicAngle = time * 4;
      const rx = Math.cos(relicAngle) * 32;
      const ry = Math.sin(relicAngle) * 16 - 10;
      ctx.fillStyle = eng.getRarityColor(eng.equippedRelic.rarity);
      ctx.beginPath();
      ctx.arc(rx, ry, 4.5, 0, Math.PI * 2);
      ctx.fill();
      ctx.shadowColor = '#fff';
      ctx.shadowBlur = 6;
      ctx.stroke();
      ctx.shadowBlur = 0;
    }

    ctx.restore();
  };

  const drawProjectiles = (ctx: CanvasRenderingContext2D, projectiles: Projectile[]) => {
    projectiles.forEach((p) => {
      ctx.fillStyle = p.color;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#ffffff';
      ctx.lineWidth = 1.5;
      ctx.stroke();
    });
  };

  const drawParticles = (ctx: CanvasRenderingContext2D, particles: Particle[]) => {
    particles.forEach((p) => {
      const alpha = p.life / p.maxLife;
      ctx.save();
      ctx.globalAlpha = Math.max(0, alpha);
      ctx.fillStyle = p.color;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    });
  };

  const drawFloaters = (ctx: CanvasRenderingContext2D, floaters: Floater[]) => {
    floaters.forEach((f) => {
      const alpha = f.life / f.maxLife;
      ctx.save();
      ctx.globalAlpha = Math.max(0, alpha);
      ctx.font = f.isCrit ? '900 18px Cinzel, serif' : 'bold 13px Outfit, sans-serif';
      ctx.fillStyle = f.color;
      ctx.textAlign = 'center';
      ctx.shadowColor = '#000000';
      ctx.shadowBlur = 4;
      ctx.fillText(f.text, f.x, f.y);
      ctx.restore();
    });
  };

  const drawFloorShadow = (ctx: CanvasRenderingContext2D, x: number, y: number, rx: number, ry: number) => {
    ctx.fillStyle = 'rgba(0, 0, 0, 0.35)';
    ctx.beginPath();
    ctx.ellipse(x, y, rx, ry, 0, 0, Math.PI * 2);
    ctx.fill();
  };

  const drawAncientColumn = (ctx: CanvasRenderingContext2D, x: number, y: number) => {
    drawFloorShadow(ctx, x, y + 50, 30, 10);
    ctx.fillStyle = '#64748b';
    ctx.fillRect(x - 14, y - 50, 28, 100);
    ctx.strokeStyle = '#cbd5e1';
    ctx.lineWidth = 2;
    ctx.strokeRect(x - 14, y - 50, 28, 100);

    // Top capital & bottom base
    ctx.fillStyle = '#475569';
    ctx.fillRect(x - 22, y - 60, 44, 12);
    ctx.strokeRect(x - 22, y - 60, 44, 12);
    ctx.fillRect(x - 22, y + 46, 44, 12);
    ctx.strokeRect(x - 22, y + 46, 44, 12);
  };

  const drawPaperCoral = (ctx: CanvasRenderingContext2D, x: number, y: number, color: string, time: number) => {
    drawFloorShadow(ctx, x, y + 25, 25, 8);
    ctx.save();
    ctx.translate(x, y);
    ctx.rotate(Math.sin(time) * 0.08);

    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.moveTo(0, 25);
    ctx.lineTo(-12, 0);
    ctx.lineTo(-24, -20);
    ctx.lineTo(-14, -26);
    ctx.lineTo(-6, -10);
    ctx.lineTo(0, -32);
    ctx.lineTo(8, -12);
    ctx.lineTo(18, -24);
    ctx.lineTo(26, -16);
    ctx.lineTo(14, 4);
    ctx.closePath();
    ctx.fill();
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 2;
    ctx.stroke();

    ctx.restore();
  };

  return (
    <canvas
      ref={canvasRef}
      onPointerDown={handlePointerDown}
      className="absolute inset-0 w-full h-full cursor-crosshair touch-none"
    />
  );
};
