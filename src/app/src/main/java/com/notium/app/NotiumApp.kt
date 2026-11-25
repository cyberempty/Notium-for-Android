package com.notium.app

import android.app.Application
import android.os.Build
import java.io.File

class NotiumApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Se l'app si chiude per un errore, lo salva in un file locale (solo traccia tecnica, niente note):
        // al prossimo avvio viene mostrato a schermo, cosi' si puo' capire cosa e' successo.
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                File(filesDir, CRASH_FILE).writeText(
                    "Notium 1.0.0 - Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ") - " +
                        Build.MANUFACTURER + " " + Build.MODEL + " - ABI " + Build.SUPPORTED_ABIS.joinToString() + "\n" +
                        "Thread: " + t.name + "\n\n" + android.util.Log.getStackTraceString(e)
                )
            }
            previous?.uncaughtException(t, e)
        }
        // Libreria nativa di SQLCipher (richiesta prima di aprire qualsiasi database).
        System.loadLibrary("sqlcipher")
    }

    companion object { const val CRASH_FILE = "last_crash.txt" }
}
