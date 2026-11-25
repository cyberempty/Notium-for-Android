# Notium

App Android di note 100% offline, cifrate (SQLCipher, Argon2id + AES-256-GCM, Keystore).

## Contenuto della cartella

```
Notium-android/
├── README.md                       questo file
├── Build-Notium-android.bat        build per Windows
├── Build-Notium-android.sh         build per Linux
├── Build-Notium-android.command    build per macOS
├── build-tools/                    motore di compilazione (non serve toccarlo)
└── src/                            codice sorgente completo dell'app (Kotlin, Jetpack Compose)
```

## Come ottenere l'APK

| Sistema | Cosa fare |
|---|---|
| Windows | doppio clic su `Build-Notium-android.bat` |
| Linux | `./Build-Notium-android.sh` dal terminale (solo processori x86-64) |
| macOS | doppio clic su `Build-Notium-android.command` (se richiesto: clic destro, Apri) |

Se su Linux/macOS i file non sono eseguibili: `chmod +x Build-Notium-android.sh Build-Notium-android.command build-tools/build-engine.sh src/gradlew`.

La prima volta serve internet: vengono scaricati JDK, Android SDK e librerie in `build-tools/tools` (circa 3 GB). Le volte successive si compila anche offline.

Al termine l'APK si trova nella cartella **`Notium-android-APK`**, creata accanto a `Notium-android`:

```
Notium-android-APK/Notium.apk
```

Copia `Notium.apk` sul telefono e aprilo (consenti l'installazione da origini sconosciute).

In caso di errore, il terminale mostra le righe di errore e il log completo è in `build-tools/build.log`.

Dettagli sul progetto e sulle funzioni: `src/README.md`.
