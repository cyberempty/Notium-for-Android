# Notium

*Your notes. Your privacy.* — app Android per appunti personali, **100% offline**, con database cifrato.

- Kotlin · Jetpack Compose · Material Design 3 · MVVM
- Database: SQLite + SQLCipher (AES-256), ricerca FTS5 interna al database cifrato
- Chiave principale casuale, protetta da: password (Argon2id), PIN (Argon2id + Android Keystore), biometria (BiometricPrompt + Keystore)
- Backup locale `.notium`: AES-256-GCM, chiave da Argon2id, integrità e autenticità verificate prima del ripristino
- Nessun permesso di rete, nessun account, nessuna pubblicità, nessun backup Android automatico
- Android 8.0+ (minSdk 26)
- Grafica scura in nero, bianco e grigio (tema Scuro, Nero AMOLED, Chiaro o Automatico), icone a simboli
- Impostazioni organizzate in categorie (Sicurezza, Privacy, Backup, Tema e colori, Schermata principale, Elenco note, Editor, Gestione dati, Informazioni): tocchi una categoria e si apre la sua pagina. Personalizzazione massima: tema (Scuro, Nero AMOLED, Chiaro, Automatico), accento anche con colore esadecimale libero, angoli, spessore bordi, stile schede, carattere e dimensione dell'interfaccia, sezione di apertura, 1-3 colonne, filtri rapidi, pulsante Nuova nota (stile e posizione), righe titolo/anteprima, formato data; editor con carattere, dimensioni, interlinea, margini, salvataggio automatico, correzione, schermo acceso; tastierino PIN casuale e vibrazione

## Stato del progetto (leggere prima di compilare)

Il codice è completo rispetto alle specifiche in `docs/Notium-specifiche.txt`, ma **non è mai stato compilato né eseguito**:
è stato scritto in un ambiente senza accesso all'Android SDK. Alla prima compilazione possono servire piccole correzioni
(import, firme di API delle librerie). Le immagini in `screens/` sono **mockup di progetto** (non screenshot reali).

Punti da verificare per primi:
1. Costruttore di `SQLiteOpenHelper` di SQLCipher in `data/NotiumDatabase.kt` (versione 4.6.1).
2. API di Argon2Kt in `security/Crypto.kt` (`Argon2Kt().hash(...)`).
3. Test su dispositivo reale: biometria, blocco automatico, backup/ripristino, cambio password.

## Compilare in un clic (consigliato)

- **Windows**: doppio clic su `Build-Notium-android.bat`
- **Linux/macOS**: `./compila-apk.sh`

Al primo avvio lo script scarica da solo, dentro la cartella `tools/` (niente viene installato nel PC): JDK 17,
Android SDK (command-line tools, piattaforma 35, build-tools) e Gradle. Servono Internet, circa 3 GB liberi e
5-15 minuti. Alla fine lo script fa pulizia dei file temporanei e ti chiede se cancellare anche `tools/`. L'APK finito è in
**`Notium-android-APK/Notium.apk`**; copialo sul telefono e aprilo (consenti l'installazione da origini sconosciute). Dalle volte successive è molto più veloce.
In caso di errore leggi `build.log`. Gli script non sono stati provati qui (nessun accesso a quei download).

## Compilare a mano

**Android Studio**: apri questa cartella → *Build › Build APK(s)* (debug, installabile subito).
Da terminale: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/`.

**GitHub Actions**: carica la cartella su un repository; il workflow `.github/workflows/build-apk.yml` compila l'APK,
verifica che non ci siano permessi di rete e lo pubblica come artefatto. Vedi `docs/DISTRIBUZIONE.md`.

## Struttura

- `app/src/main/java/com/notium/app/security` — Argon2id, AES-GCM, Keystore, PIN, biometria
- `.../data` — database SQLCipher, repository, preferenze, modelli
- `.../backup` — formato e cifratura dei backup
- `.../ui` — schermate Compose (accesso, home, editor, cartelle, tag, impostazioni)
- `docs/` — specifiche, informativa privacy, distribuzione e istruzioni per l'utente
- `screens/` — immagini delle schermate (mockup)

## Non incluso / limiti noti

- Nessuna rotazione della chiave principale del database (il cambio password ri-avvolge la chiave senza ricifrare il database).
- Testi dell'interfaccia solo in italiano, scritti direttamente nel codice.
- In ricerca i termini sono evidenziati nell'elenco, non dentro l'editor.
- Il ripristino sostituisce tutte le note (nessuna fusione).

## Compilare offline

La prima compilazione richiede internet (scarica JDK, Android SDK, Gradle e librerie dentro `tools/`).
Se alla domanda finale rispondi **N** (non cancellare `tools`), le compilazioni successive usano solo
quella cartella e funzionano **senza connessione** (lo script rileva `tools/.offline-pronto` e usa `--offline`).
Per compilare su un altro PC offline, copia l'intera cartella del progetto, `tools` inclusa.

## Novità di navigazione e personalizzazione
- La riga di filtri in alto è stata tolta: Tutte, Preferiti, Fissate, Archivio, Cestino, cartelle e tag sono nel menu laterale (icona hamburger). Il titolo in alto mostra la sezione corrente.
- Nell'editor, scegliendo la cartella della nota, c'è "Nuova cartella…": la crei e la nota ci viene spostata subito (funziona anche da "Sposta in cartella").
- Animazioni su transizioni tra schermate, elenco note, pulsante e titolo, con stile (Scorrimento/Dissolvenza/Zoom) e velocità regolabili (Impostazioni › Animazioni).
- Gli slider (testo, angoli, velocità...) applicano il valore in tempo reale mentre li trascini, con la percentuale che cambia subito.
- Nuove opzioni: colore di sfondo e delle schede, dimensione titolo/anteprima nell'elenco, spaziatura delle schede, titoli in grassetto, barra di ricerca on/off, cartelle e tag nel menu on/off.

## Aggiornamento: animazioni, ripristino e nuove opzioni
- Una nuova nota non ha mai una cartella di default (anche se la crei da dentro una cartella): la scegli tu dall'editor.
- Animazioni più fluide: transizioni con curva morbida (Scorrimento, Dal basso, Dissolvenza, Zoom), movimento dell'elenco con effetto molla, pressione delle schede con rimbalzo, pulsante Nuova nota che compare con effetto molla.
- Ogni categoria delle impostazioni ha il tasto "Ripristina predefiniti" (anche nella barra in alto) che riporta solo quella categoria ai valori iniziali.
- Slider: il cursore segue il dito in modo continuo; le opzioni che ridisegnano tutta l'app (dimensione testo, angoli) si applicano al rilascio, con anteprima durante il trascinamento.
- Nuove opzioni: alto contrasto, intensità effetto pressione, movimento elenco, nome nella barra superiore, margini e distanza tra le schede, icone pin/preferito on/off, spaziatura tra le lettere e titolo in grassetto nell'editor.

## Prestazioni e funzioni di scrittura (stile Samsung Notes)
- La compilazione ora produce una versione **release** ottimizzata (R8, risorse ridotte, profili di avvio): su telefoni del 2020 in poi e' molto piu' fluida della versione di debug usata prima. E' firmata con una chiave locale creata in `build-tools/signing` (conservala per poter aggiornare l'app senza disinstallarla).
- Elenco note: anteprime e liste in cache, regex precompilate; anteprima Markdown analizzata una sola volta; contatore parole senza regex.
- Editor: formattazione in tempo reale (titoli, grassetto, corsivo, barrato, evidenziato, caselle, citazioni, codice), annulla/ripeti, stili del testo (normale, titolo 1-3), liste puntate/numerate/**caselle di controllo** che proseguono da sole con Invio, citazione, rientro, evidenziatore (`==testo==`), barrato (`~~testo~~`), tabelle, blocchi di codice, linea separatrice, data e ora, **modelli** (riunione, spesa, giornata, diario, progetto), cerca e sostituisci nella nota, condivisione e esportazione in Markdown e **PDF** (tutto offline, scegli tu dove salvare).
- Nell'anteprima le caselle si spuntano toccandole.
- Non incluse (richiedono permessi o servizi in rete che Notium evita per scelta): registrazione vocale, scrittura a mano/S Pen, sincronizzazione cloud, funzioni di intelligenza artificiale online.

## Lingue e apertura dell'app

- Alla prima apertura compare la scelta della lingua (predefinita: inglese). Lingue: English, Español, Italiano, 中文（普通话）. Si cambia anche da Impostazioni › Lingua.
- I testi nel codice sono in italiano e fanno da chiave per il dizionario in `ui/I18nData.kt` (formato `italiano ::: inglese ::: spagnolo ::: cinese`).
- PIN e password si sbloccano da soli appena raggiungono la lunghezza giusta (nessun tasto da premere).
- Senza PIN né impronta l'app si apre direttamente: la chiave resta protetta dal Keystore del dispositivo. Attivando PIN o impronta il blocco torna obbligatorio.
