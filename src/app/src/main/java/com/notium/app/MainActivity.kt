package com.notium.app

import androidx.compose.animation.togetherWith

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.notium.app.security.BiometricHelper
import com.notium.app.ui.AuthScreensHost
import com.notium.app.ui.I18n
import com.notium.app.ui.LanguageScreen
import com.notium.app.ui.LocalLang
import androidx.compose.runtime.CompositionLocalProvider
import com.notium.app.ui.EditorScreen
import com.notium.app.ui.FoldersScreen
import com.notium.app.ui.HomeScreen
import com.notium.app.ui.NotiumTheme
import com.notium.app.ui.SettingsScreen
import com.notium.app.ui.TagsScreen

private enum class Stage { Blank, Language, Setup, Locked, Main }

class MainActivity : FragmentActivity() {
    private val vm: AppViewModel by viewModels()
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Anteprima nelle app recenti nascosta fin dal primo istante; poi segue l'impostazione.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()

        val crash = runCatching { java.io.File(filesDir, NotiumApp.CRASH_FILE).takeIf { it.exists() }?.readText() }.getOrNull()
        if (crash != null) {
            setContent { androidx.compose.material3.MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) { CrashScreen(crash) } }
            return
        }

        setContent {
            val settings by vm.settings.collectAsState()
            LaunchedEffect(settings.secureScreen) {
                if (settings.secureScreen) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            LaunchedEffect(settings.language) { java.util.Locale.setDefault(I18n.locale(settings.language)) }
            CompositionLocalProvider(LocalLang provides settings.language) {
                NotiumTheme(settings) { Root() }
            }
        }
    }

    /** Mostrata al posto dell'app se l'avvio precedente si e' chiuso per un errore. */
    @Composable
    private fun CrashScreen(text: String) {
        androidx.compose.foundation.layout.Column(
            Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF0B0B0C)).systemBarsPadding().padding(16.dp),
        ) {
            androidx.compose.material3.Text("Notium closed because of an error", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleLarge)
            androidx.compose.material3.Text(
                "Copy this text and send it to whoever is helping you: it contains technical data only, no notes.",
                color = androidx.compose.ui.graphics.Color(0xFFB0B0B0), modifier = Modifier.padding(vertical = 8.dp),
            )
            androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.Button(onClick = {
                    val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("Notium", text))
                }) { androidx.compose.material3.Text("Copy") }
                androidx.compose.material3.Button(onClick = {
                    val i = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text)
                    startActivity(android.content.Intent.createChooser(i, null))
                }) { androidx.compose.material3.Text("Share") }
                androidx.compose.material3.OutlinedButton(onClick = {
                    java.io.File(filesDir, NotiumApp.CRASH_FILE).delete(); recreate()
                }) { androidx.compose.material3.Text("Retry") }
            }
            androidx.compose.foundation.text.selection.SelectionContainer(
                Modifier.weight(1f).padding(top = 12.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),
            ) {
                androidx.compose.material3.Text(text, color = androidx.compose.ui.graphics.Color(0xFFE0E0E0), fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
        }
    }

    @Composable
    private fun Root() {
        val auth by vm.auth.collectAsState()
        val loaded by vm.settingsLoaded.collectAsState()
        val starting by vm.starting.collectAsState()
        val st by vm.settings.collectAsState()
        val stage = when {
            !loaded || (starting && auth != AuthState.Unlocked) -> Stage.Blank
            !st.langChosen -> Stage.Language
            auth == AuthState.Setup -> Stage.Setup
            auth == AuthState.Locked -> Stage.Locked
            else -> Stage.Main
        }
        val host = remember { SnackbarHostState() }
        LaunchedEffect(Unit) {
            vm.messages.collectLatest { m ->
                val r = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                    host.showSnackbar(
                        I18n.tr(m.text, vm.settings.value.language), actionLabel = if (m.undo != null) when (vm.settings.value.language) {
                            com.notium.app.data.AppLang.EN -> "Undo"; com.notium.app.data.AppLang.ES -> "Deshacer"
                            com.notium.app.data.AppLang.IT -> "Annulla"; com.notium.app.data.AppLang.ZH -> "撤销"
                        } else null,
                        duration = if (m.undo != null) androidx.compose.material3.SnackbarDuration.Indefinite else androidx.compose.material3.SnackbarDuration.Short,
                    )
                }
                if (r == androidx.compose.material3.SnackbarResult.ActionPerformed) m.undo?.invoke()
            }
        }

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                androidx.compose.animation.AnimatedContent(
                    targetState = stage,
                    transitionSpec = {
                        (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(420, delayMillis = 60)) +
                            androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(420, delayMillis = 60), initialScale = 0.94f))
                            .togetherWith(androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(220)))
                    },
                    label = "stage",
                ) { s ->
                    when (s) {
                        Stage.Blank -> Box(Modifier.fillMaxSize())
                        Stage.Language -> LanguageScreen(vm)
                        Stage.Setup -> AuthScreensHost(vm, AuthState.Setup) { promptBiometricUnlock() }
                        Stage.Locked -> AuthScreensHost(vm, AuthState.Locked) { promptBiometricUnlock() }
                        Stage.Main -> MainContent()
                    }
                }
                SnackbarHost(host, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 80.dp)) {
                    Snackbar(it)
                }
            }
        }
    }

    @Composable
    private fun MainContent() {
        BackHandler(enabled = vm.stack.size > 1) {
            if (vm.current == Route.Editor) vm.closeEditor() else vm.back()
        }
        val anim = com.notium.app.ui.LocalAnim.current
        androidx.compose.animation.AnimatedContent(
            targetState = vm.current,
            transitionSpec = {
                val d = 380
                val back = targetState == Route.Home
                when (anim.style) {
                    com.notium.app.data.AnimStyle.SLIDE ->
                        (androidx.compose.animation.slideInHorizontally(anim.smooth(d)) { w -> if (back) -w / 3 else w } +
                            androidx.compose.animation.fadeIn(anim.smooth(d))) togetherWith
                            (androidx.compose.animation.slideOutHorizontally(anim.smooth(d)) { w -> if (back) w else -w / 3 } +
                                androidx.compose.animation.fadeOut(anim.smooth(d / 2)))
                    com.notium.app.data.AnimStyle.SLIDE_UP ->
                        (androidx.compose.animation.slideInVertically(anim.smooth(d)) { h -> if (back) 0 else h / 5 } +
                            androidx.compose.animation.fadeIn(anim.smooth(d))) togetherWith
                            (androidx.compose.animation.slideOutVertically(anim.smooth(d)) { h -> if (back) h / 5 else 0 } +
                                androidx.compose.animation.fadeOut(anim.smooth(d)))
                    com.notium.app.data.AnimStyle.FADE ->
                        androidx.compose.animation.fadeIn(anim.smooth(d)) togetherWith
                            androidx.compose.animation.fadeOut(anim.smooth(d))
                    com.notium.app.data.AnimStyle.SCALE ->
                        (androidx.compose.animation.fadeIn(anim.smooth(d)) +
                            androidx.compose.animation.scaleIn(anim.smooth(d), initialScale = 0.9f)) togetherWith
                            (androidx.compose.animation.fadeOut(anim.smooth(d)) +
                                androidx.compose.animation.scaleOut(anim.smooth(d), targetScale = 1.06f))
                }
            },
            label = "route",
        ) { route ->
            when (route) {
                Route.Home -> HomeScreen(vm)
                Route.Editor -> EditorScreen(vm)
                Route.Folders -> FoldersScreen(vm)
                Route.Tags -> TagsScreen(vm)
                Route.Settings -> SettingsScreen(vm, this@MainActivity)
            }
        }
    }

    private fun promptBiometricUnlock() {
        val cipher = vm.biometricUnlockCipher() ?: return
        val lang = vm.settings.value.language
        BiometricHelper.prompt(
            this, I18n.tr("Sblocca Notium", lang), I18n.tr("Usa l'impronta digitale", lang), cipher, I18n.tr("Usa la password", lang),
            onSuccess = vm::biometricUnlockSuccess,
            onFailure = vm::biometricUnlockFailed,
        )
    }

    override fun onStop() {
        super.onStop()
        if (isChangingConfigurations || vm.auth.value != AuthState.Unlocked) return
        vm.flushBlocking() // salva l'editor prima di lasciare il primo piano
        if (vm.pickerActive) return
        stoppedAt = SystemClock.elapsedRealtime()
        if (vm.settings.value.autoLockSeconds == 0) vm.lock()
    }

    override fun onStart() {
        super.onStart()
        val limit = vm.settings.value.autoLockSeconds
        if (stoppedAt != 0L && vm.auth.value == AuthState.Unlocked && limit > 0 && !vm.pickerActive) {
            if (SystemClock.elapsedRealtime() - stoppedAt >= limit * 1000L) vm.lock()
        }
        stoppedAt = 0L
    }
}
