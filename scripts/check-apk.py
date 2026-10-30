import pathlib
import zipfile

project = pathlib.Path(__file__).resolve().parent.parent
apk = project / 'downloads/carta-dabisso-0.2.0-debug.apk'
with zipfile.ZipFile(apk) as archive:
    assert archive.testzip() is None, 'APK danneggiato'
    for name in ['index.html', 'game.js', 'core.js']:
        assert archive.read('assets/web/' + name) == (project / 'dist' / name).read_bytes(), name + ' non aggiornato'
    css = archive.read('assets/web/style.css').decode()
    assert '@import' not in css and 'fonts.googleapis.com' not in css, 'Font online presenti nel pacchetto offline'
    assert (project / 'android/phone.css').read_text() in css, 'Adattamento comandi touch mancante'
    assert 'classes.dex' in archive.namelist(), 'Codice Android mancante'
    manifest = archive.read('AndroidManifest.xml')
    permission = 'android.permission.INTERNET'
    assert permission.encode() not in manifest and permission.encode('utf-16-le') not in manifest, 'Permesso Internet inatteso'
print('APK OK: integrità, codice Android, asset aggiornati e funzionamento senza risorse online.')
