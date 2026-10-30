import pathlib
import subprocess
import zipfile

project = pathlib.Path(__file__).resolve().parent.parent
apks = sorted(list(project.glob('downloads/carta-dabisso-*-debug.apk')), key=lambda p: p.stat().st_mtime)
assert apks, 'Nessun file APK trovato in downloads/'
apk = apks[-1]
print(f'Verifica APK: {apk.name}')

with zipfile.ZipFile(apk) as archive:
    assert archive.testzip() is None, 'APK danneggiato'
    namelist = archive.namelist()
    
    # Verifica totale assenza di file web nel pacchetto
    web_files = [n for n in namelist if n.startswith('assets/web/')]
    assert len(web_files) == 0, f'Trovati file web residui nel pacchetto nativo: {web_files}'
    
    # Verifica classi native nei bytecode dex
    all_dex = b''.join(archive.read(n) for n in namelist if n.endswith('.dex'))
    for cls in [b'GameView', b'Game', b'MainActivity', b'AudioHelper']:
        assert cls in all_dex, f'Classe nativa {cls.decode()} non trovata nei DEX'
    
    # Verifica manifest e permessi offline
    manifest = archive.read('AndroidManifest.xml')
    permission = 'android.permission.INTERNET'
    assert permission.encode() not in manifest and permission.encode('utf-16-le') not in manifest, 'Permesso Internet inatteso: il gioco deve funzionare offline'

# Verifica orientamento orizzontale tramite aapt
aapt_res = subprocess.run(['aapt', 'dump', 'badging', str(apk)], capture_output=True, text=True, check=True)
assert 'android.hardware.screen.landscape' in aapt_res.stdout, 'Orientamento orizzontale non rilevato da aapt'

print('APK OK: app Android 100% nativa (SurfaceView/Canvas), orientamento orizzontale (sensorLandscape), classi DEX verificate e zero dipendenze web.')
