package com.notium.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notium.app.backup.BackupException
import com.notium.app.backup.BackupManager
import com.notium.app.data.BackupData
import com.notium.app.data.Folder
import com.notium.app.data.FolderDeleteMode
import com.notium.app.data.Note
import com.notium.app.data.NoteFilter
import com.notium.app.data.NoteSummary
import com.notium.app.data.NotiumColors
import com.notium.app.data.NotiumDatabase
import com.notium.app.data.Prefs
import com.notium.app.data.Repository
import com.notium.app.data.Section
import com.notium.app.data.Settings
import com.notium.app.data.SortOrder
import com.notium.app.data.toSection
import com.notium.app.data.Tag
import com.notium.app.security.BiometricHelper
import com.notium.app.security.Crypto
import com.notium.app.security.PinResult
import com.notium.app.security.VaultManager
import com.notium.app.security.toUtf8Bytes
import com.notium.app.security.wipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.crypto.Cipher

enum class AuthState { Setup, Locked, Unlocked }

sealed interface Route {
    data object Home : Route
    data object Editor : Route
    data object Folders : Route
    data object Tags : Route
    data object Settings : Route
}

enum class SaveStatus(val label: String) { Saved("Salvato"), Saving("Salvataggio…"), Unsaved("Modifiche non salvate") }

data class EditorState(
    val noteId: Long?,
    val title: String,
    val content: String,
    val folderId: Long?,
    val tagIds: Set<Long>,
    val favorite: Boolean,
    val pinned: Boolean,
    val archived: Boolean,
    val deleted: Boolean,
    val status: SaveStatus,
    val preview: Boolean,
)

/** Messaggio nella barra in basso; se c'e' [undo] compare il tasto Annulla. */
class UiMessage(val text: String, val undo: (() -> Unit)? = null)

data class AuthOptions(
    val pinEnabled: Boolean = false, val biometricEnabled: Boolean = false, val biometricAvailable: Boolean = false,
    val pinLen: Int = 0, val pwLen: Int = 0, val passwordLock: Boolean = false,
)

/** Conferma mostrata nella home dopo un backup importato al primo avvio. */
data class ImportBanner(val notes: Int, val folders: Int, val tags: Int)

/** Richiesta di sbloccare una nota protetta: apertura ([remove]=false) oppure rimozione del blocco. */
enum class NoteLockMode { CREATE, OPEN, REMOVE }
data class NoteLockRequest(val noteId: Long, val mode: NoteLockMode)

data class RestorePreview(val data: BackupData) {
    val notes get() = data.notes.size
    val folders get() = data.folders.size
    val tags get() = data.tags.size
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AppViewModel(private val app: Application) : AndroidViewModel(app) {
    val vault = VaultManager(app)
    private val prefs = Prefs(app)

    // ---------------------------------------------------------------- stato generale
    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()
    private var persistJob: Job? = null
    private var baseRaw: String? = null
    private var baseMd: String? = null

    val settingsLoaded = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            prefs.settings.collect {
                if (persistJob?.isActive != true) _settings.value = it
                settingsLoaded.value = true
            }
        }
    }

    fun chooseLanguage(l: com.notium.app.data.AppLang) = updateSettings { it.copy(language = l) }
    fun confirmLanguage() = updateSettings { it.copy(langChosen = true) }

    private val _auth = MutableStateFlow(if (vault.exists()) AuthState.Locked else AuthState.Setup)
    val auth: StateFlow<AuthState> = _auth.asStateFlow()

    private val _authOptions = MutableStateFlow(AuthOptions())
    val authOptions: StateFlow<AuthOptions> = _authOptions.asStateFlow()

    val busy = MutableStateFlow(false)
    val authMessage = MutableStateFlow<String?>(null)
    val pinLockedUntil = MutableStateFlow(0L)
    val messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)

    /** True mentre e' aperto un selettore di file di sistema: l'app non va bloccata per quel passaggio. */
    var pickerActive = false

    private var master: ByteArray? = null
    private val repo = MutableStateFlow<Repository?>(null)
    private val version = MutableStateFlow(0)

    /** True finche' l'app si sta aprendo da sola (nessun metodo di sblocco impostato). */
    val starting = MutableStateFlow(
        vault.exists() && vault.hasAutoUnlock() && !vault.isPinEnabled() && !vault.isBiometricEnabled()
    )

    init {
        refreshAuthOptions()
        if (vault.exists()) pinLockedUntil.value = System.currentTimeMillis() + vault.pinLockRemaining()
        if (starting.value) viewModelScope.launch {
            val m = withContext(Dispatchers.IO) { vault.unlockAuto() }
            if (m != null) openSession(m)
            starting.value = false
        }
    }

    /** Senza PIN e senza impronta l'app non chiede nulla: la chiave e' tenuta nel Keystore del dispositivo. */
    private fun syncAutoUnlock() {
        val m = master ?: return
        runCatching {
            if (!vault.isPinEnabled() && !vault.isBiometricEnabled() && !vault.isPasswordLockEnabled()) {
                if (!vault.hasAutoUnlock()) vault.enableAutoUnlock(m)
            } else vault.disableAutoUnlock()
        }
    }

    private fun bump() = version.update { it + 1 }

    private fun refreshAuthOptions() {
        _authOptions.value = if (vault.exists()) AuthOptions(
            vault.isPinEnabled(), vault.isBiometricEnabled(), BiometricHelper.isAvailable(app),
            vault.pinLength(), vault.passwordLength(), vault.isPasswordLockEnabled(),
        ) else AuthOptions(biometricAvailable = BiometricHelper.isAvailable(app))
    }

    private fun say(msg: String, undo: (() -> Unit)? = null) { messages.tryEmit(UiMessage(msg, undo)) }

    // ------------------------------------------------------------------ navigazione
    val stack = mutableStateListOf<Route>(Route.Home)
    val current: Route get() = stack.last()

    fun go(r: Route) { if (stack.last() != r) stack.add(r) }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    private fun resetNav() { stack.clear(); stack.add(Route.Home) }

    // ----------------------------------------------------------------------- elenchi
    val filter = MutableStateFlow(NoteFilter())

    val notes: StateFlow<List<NoteSummary>> =
        combine(repo, filter.debounce(80), version) { r, f, _ -> r to f }
            .mapLatest { (r, f) -> r?.let { runCatching { it.listNotes(f) }.getOrDefault(emptyList()) } ?: emptyList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<Folder>> =
        combine(repo, version) { r, _ -> r }
            .mapLatest { r -> r?.let { runCatching { it.listFolders() }.getOrDefault(emptyList()) } ?: emptyList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<Tag>> =
        combine(repo, version) { r, _ -> r }
            .mapLatest { r -> r?.let { runCatching { it.listTags() }.getOrDefault(emptyList()) } ?: emptyList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSection(s: Section) = filter.update { it.copy(section = s) }
    fun setQuery(q: String) = filter.update { it.copy(query = q) }
    fun setSort(s: SortOrder) {
        filter.update { it.copy(sort = s) }
        updateSettings { it.copy(sort = s) }
    }

    // ----------------------------------------------------------------- impostazioni
    /** Applica subito (anche durante il trascinamento degli slider) e salva dopo una breve pausa. */
    fun updateSettings(block: (Settings) -> Settings) {
        val s = block(_settings.value)
        _settings.value = s
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(150)
            prefs.update({ s }, s)
        }
    }

    /** Scrive subito le impostazioni in sospeso (chiamata quando l'app lascia il primo piano). */
    fun flushSettings() {
        if (persistJob?.isActive != true) return
        persistJob?.cancel()
        val s = _settings.value
        kotlinx.coroutines.runBlocking { runCatching { prefs.update({ s }, s) } }
    }

    // ----------------------------------------------------------------- autenticazione

    fun setupPassword(password: CharArray) {
        viewModelScope.launch {
            busy.value = true
            val m = withContext(Dispatchers.IO) { vault.create(password) }
            password.fill('\u0000')
            openSession(m)
            // Primo avvio con un backup scelto: dopo aver creato la password principale lo importa.
            val p = pendingRestore.value
            val r = repo.value
            if (p != null && r != null) {
                val res = runCatching { r.replaceAll(p.data) }
                pendingRestore.value = null
                resetNav(); bump()
                if (res.isSuccess) importBanner.value = ImportBanner(p.notes, p.folders, p.tags) else say("Importazione del backup non riuscita.")
            }
            busy.value = false
        }
    }

    fun unlockWithPassword(password: CharArray) {
        viewModelScope.launch {
            busy.value = true
            val len = password.size
            val m = withContext(Dispatchers.IO) { vault.unlockWithPassword(password) }
            password.fill('\u0000')
            if (m != null) withContext(Dispatchers.IO) { vault.rememberPasswordLength(len) }
            if (m == null) authMessage.value = "Password errata."
            else openSession(m)
            busy.value = false
        }
    }

    fun unlockWithPin(pin: CharArray) {
        viewModelScope.launch {
            busy.value = true
            val len = pin.size
            val r = withContext(Dispatchers.IO) { vault.unlockWithPin(pin) }
            pin.fill('\u0000')
            when (r) {
                is PinResult.Success -> { vault.rememberPinLength(len); openSession(r.masterKey) }
                is PinResult.Wrong -> authMessage.value = "PIN errato. Tentativi rimasti prima del blocco: ${r.attemptsBeforeLock}."
                is PinResult.Locked -> {
                    pinLockedUntil.value = r.untilMillis
                    authMessage.value = "Troppi tentativi. Riprova tra poco o usa la password."
                }
                PinResult.Disabled -> {
                    refreshAuthOptions()
                    authMessage.value = "PIN disattivato dopo troppi tentativi. Usa la password principale."
                }
            }
            busy.value = false
        }
    }

    /** Cipher da autenticare con la biometria per lo sblocco; null se non e' piu' utilizzabile. */
    fun biometricUnlockCipher(): Cipher? {
        val c = vault.prepareBiometricUnlock()
        if (c == null) {
            refreshAuthOptions()
            if (_auth.value == AuthState.Locked) authMessage.value = "La biometria e' cambiata: inserisci la password principale."
        }
        return c
    }

    fun biometricUnlockSuccess(c: Cipher) {
        viewModelScope.launch {
            val m = withContext(Dispatchers.IO) { vault.finishBiometricUnlock(c) }
            if (m == null) {
                vault.disableBiometric(); refreshAuthOptions()
                authMessage.value = "Sblocco biometrico non riuscito: inserisci la password principale."
            } else openSession(m)
        }
    }

    fun biometricUnlockFailed(message: String, canceled: Boolean) {
        if (!canceled) authMessage.value = message
    }

    private suspend fun openSession(m: ByteArray) {
        val hex = Crypto.toHex(m)
        val opened = withContext(Dispatchers.IO) {
            runCatching {
                val r = Repository(NotiumDatabase(app, hex))
                r.listFolders() // forza l'apertura: fallisce se chiave o database non sono validi
                r
            }
        }
        val r = opened.getOrNull()
        if (r == null) {
            m.wipe()
            authMessage.value = "Impossibile aprire il database. Il file potrebbe essere danneggiato: ripristina un backup."
            return
        }
        master = m
        repo.value = r
        authMessage.value = null
        syncAutoUnlock()
        refreshAuthOptions()
        _auth.value = AuthState.Unlocked
        filter.value = NoteFilter(sort = settings.value.sort, section = settings.value.startSection.toSection())
        viewModelScope.launch { r.purgeTrashOlderThan(settings.value.trashDays); bump() }
    }

    /** Blocca l'app: salva l'editor, chiude il database e cancella la chiave dalla memoria. */
    fun lock() {
        if (_auth.value != AuthState.Unlocked) return
        if (vault.hasAutoUnlock()) return // nessun PIN/impronta: niente da bloccare
        flushBlocking()
        relockNotes()
        noteLockRequest.value = null
        autosave?.cancel()
        repo.value?.close()
        repo.value = null
        master?.wipe(); master = null
        _editor.value = null
        resetNav()
        pendingRestore.value = null
        filter.value = NoteFilter(sort = settings.value.sort)
        refreshAuthOptions()
        pinLockedUntil.value = System.currentTimeMillis() + vault.pinLockRemaining()
        authMessage.value = null
        _auth.value = AuthState.Locked
    }

    fun changePassword(old: CharArray, new: CharArray, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            busy.value = true
            val ok = withContext(Dispatchers.IO) { vault.changePassword(old, new) }
            old.fill('\u0000'); new.fill('\u0000')
            busy.value = false
            say(if (ok) "Password modificata." else "Password attuale errata.")
            onDone(ok)
        }
    }

    fun enablePin(pin: CharArray) {
        val m = master ?: return
        viewModelScope.launch {
            busy.value = true
            withContext(Dispatchers.IO) { vault.enablePin(m, pin) }
            pin.fill('\u0000')
            syncAutoUnlock()
            refreshAuthOptions(); busy.value = false
            say("PIN attivato.")
        }
    }

    fun setPasswordLock(on: Boolean) {
        vault.setPasswordLock(on); syncAutoUnlock(); refreshAuthOptions()
        say(if (on) "Blocco con password attivato." else "Blocco con password disattivato.")
    }

    fun disablePin() { vault.disablePin(); syncAutoUnlock(); refreshAuthOptions(); say("PIN disattivato.") }

    fun biometricEnrollCipher(): Cipher? = runCatching { vault.prepareBiometricEnroll() }.getOrNull()

    fun biometricEnrollSuccess(c: Cipher) {
        val m = master ?: return
        runCatching { vault.finishBiometricEnroll(c, m) }
            .onSuccess { syncAutoUnlock(); refreshAuthOptions(); say("Impronta digitale attivata.") }
            .onFailure { vault.disableBiometric(); refreshAuthOptions(); say("Impossibile attivare la biometria.") }
    }

    fun disableBiometric() { vault.disableBiometric(); syncAutoUnlock(); refreshAuthOptions(); say("Biometria disattivata.") }

    fun notify(msg: String) = say(msg)

    /** Dati completi di una nota per l'esportazione (cartella e tag con il loro nome). */
    suspend fun noteForExport(id: Long): com.notium.app.ui.ExportNote? {
        val n = repo.value?.getNote(id) ?: return null
        return com.notium.app.ui.ExportNote(
            n.title, n.content, folders.value.firstOrNull { it.id == n.folderId }?.name,
            tags.value.filter { it.id in n.tagIds }.map { it.name },
            n.favorite, n.pinned, n.archived, n.createdAt, n.updatedAt,
        )
    }

    // ------------------------------------------------------------------------ editor
    private val _editor = MutableStateFlow<EditorState?>(null)
    val editor: StateFlow<EditorState?> = _editor.asStateFlow()
    private var autosave: Job? = null
    private val persistMutex = Mutex()
    private var editorGen = 0

    fun newNote() {
        val sec = filter.value.section
        val folder: Long? = null   // una nuova nota non ha mai una cartella di default
        val tagIds = (sec as? Section.WithTag)?.let { setOf(it.id) }.orEmpty()
        editorGen++
        _editor.value = EditorState(
            null, "", "", folder, tagIds, favorite = sec == Section.Favorites, pinned = false,
            archived = false, deleted = false, status = SaveStatus.Saved, preview = false,
        )
        go(Route.Editor)
    }

    fun openNote(id: Long) {
        viewModelScope.launch {
            val n: Note = repo.value?.getNote(id) ?: return@launch
            editorGen++
            // Il testo riletto dall'editor puo' differire di pochi caratteri da quello salvato (normalizzazione):
            // aprire una nota senza scrivere non deve contare come modifica.
            baseRaw = n.content
            baseMd = runCatching { com.notium.app.ui.RichCodec.load(n.content).let { r -> com.notium.app.ui.RichCodec.toMarkdown(r.text, r.spans) } }.getOrNull()
            _editor.value = EditorState(
                n.id, n.title, n.content, n.folderId, n.tagIds, n.favorite, n.pinned, n.archived, n.deleted,
                SaveStatus.Saved, settings.value.openInPreview && !n.deleted,
            )
            go(Route.Editor)
        }
    }

    fun onEdit(title: String, content: String) {
        val cur = _editor.value ?: return
        if (cur.deleted) return
        if (cur.title == title && cur.content == content) return
        if (cur.title == title && baseMd != null && content == baseMd && cur.content == baseRaw) return
        _editor.value = cur.copy(title = title, content = content, status = SaveStatus.Unsaved)
        scheduleSave()
    }

    fun setPreview(v: Boolean) = _editor.update { it?.copy(preview = v) }

    private fun scheduleSave() {
        autosave?.cancel()
        autosave = viewModelScope.launch {
            delay(settings.value.autosaveDelayMs.toLong())
            persist()
        }
    }

    private suspend fun persist() {
        persistMutex.withLock {
            val e = _editor.value ?: return
            val r = repo.value ?: return
            // Una nota aperta e non modificata non va risalvata: non deve salire in cima alla lista.
            if (e.noteId != null && e.status == SaveStatus.Saved) return
            if (e.noteId == null && e.title.isBlank() && e.content.isBlank() && e.tagIds.isEmpty()) {
                _editor.update { it?.copy(status = SaveStatus.Saved) }
                return
            }
            _editor.update { it?.copy(status = SaveStatus.Saving) }
            val ok = runCatching {
                r.saveNote(e.noteId, e.title.ifBlank { com.notium.app.data.AutoTitle.from(e.content) }, e.content, e.folderId, e.tagIds, e.favorite, e.pinned, e.archived)
            }
            val id = ok.getOrNull()
            if (id == null) {
                _editor.update { it?.copy(status = SaveStatus.Unsaved) }
                say("Errore durante il salvataggio. Riprova.")
                return
            }
            _editor.update { cur ->
                cur?.copy(
                    noteId = id,
                    status = if (cur.title == e.title && cur.content == e.content) SaveStatus.Saved else SaveStatus.Unsaved
                )
            }
            bump()
        }
    }

    /** Salvataggio sincrono (breve) usato quando l'app va in background o prima del blocco. */
    fun flushBlocking() {
        if (_editor.value == null || repo.value == null) return
        autosave?.cancel()
        runBlocking { persist() }
    }

    fun closeEditor() {
        relockNotes()
        val gen = editorGen
        autosave?.cancel()
        back()
        viewModelScope.launch {
            persist()
            if (gen == editorGen) _editor.value = null
        }
    }

    private fun editMeta(block: (EditorState) -> EditorState) {
        val cur = _editor.value ?: return
        if (cur.deleted) return
        _editor.value = block(cur).copy(status = SaveStatus.Unsaved)
        autosave?.cancel()
        viewModelScope.launch { persist() }
    }

    fun editorSetFolder(id: Long?) {
        val cur = _editor.value ?: return
        val prev = cur.folderId
        if (prev == id) return
        editMeta { it.copy(folderId = id) }
        say("Nota spostata.") { undoEditorFolder(cur.noteId, prev) }
    }

    private fun undoEditorFolder(id: Long?, prev: Long?) {
        if (_editor.value != null && _editor.value?.noteId == id) { editMeta { it.copy(folderId = prev) }; return }
        val nid = id ?: return
        viewModelScope.launch { repo.value?.moveNote(nid, prev); bump() }
    }

    private fun undoEditorArchive(id: Long?, prev: Boolean) {
        if (_editor.value != null && _editor.value?.noteId == id) { editMeta { it.copy(archived = prev) }; return }
        val nid = id ?: return
        viewModelScope.launch { repo.value?.setArchived(nid, prev); bump() }
    }

    fun editorToggleFavorite() = editMeta { it.copy(favorite = !it.favorite) }
    fun editorTogglePinned() = editMeta { it.copy(pinned = !it.pinned) }
    fun editorToggleArchived() {
        val cur = _editor.value ?: return
        val prev = cur.archived
        editMeta { it.copy(archived = !it.archived) }
        say(if (!prev) "Nota archiviata." else "Nota ripristinata dall'archivio.") { undoEditorArchive(cur.noteId, prev) }
    }
    fun editorToggleTag(id: Long) = editMeta { it.copy(tagIds = if (id in it.tagIds) it.tagIds - id else it.tagIds + id) }

    fun editorTrash() {
        val id = _editor.value?.noteId
        autosave?.cancel()
        back()
        viewModelScope.launch {
            persist()
            val nid = _editor.value?.noteId ?: id
            if (nid != null) {
                repo.value?.moveToTrash(nid); bump()
                say("Nota spostata nel cestino.") { viewModelScope.launch { repo.value?.restore(nid); bump() } }
            }
            _editor.value = null
        }
    }

    /** Crea un tag dal selettore dell'editor e lo assegna alla nota. */
    fun createTagForEditor(name: String) {
        val n = name.trim(); if (n.isEmpty()) return
        viewModelScope.launch {
            val id = repo.value?.createTag(n, NotiumColors[(tags.value.size) % NotiumColors.size]) ?: return@launch
            bump()
            editorToggleTagIfAbsent(id)
        }
    }

    private fun editorToggleTagIfAbsent(id: Long) = editMeta { it.copy(tagIds = it.tagIds + id) }

    // ------------------------------------------------------ azioni dall'elenco note
    fun toggleFavorite(n: NoteSummary) = viewModelScope.launch { repo.value?.setFavorite(n.id, !n.favorite); bump() }
    // ---- azioni su piu' note (selezione multipla)
    fun favoriteMany(list: List<NoteSummary>) = viewModelScope.launch {
        val r = repo.value ?: return@launch
        if (list.isEmpty()) return@launch
        val target = !list.all { it.favorite }
        list.forEach { r.setFavorite(it.id, target) }; bump()
        say(if (list.size == 1) (if (target) "Aggiunta ai preferiti." else "Rimossa dai preferiti.")
            else (if (target) "${list.size} note aggiunte ai preferiti." else "${list.size} note rimosse dai preferiti.")) {
            viewModelScope.launch { list.forEach { n -> r.setFavorite(n.id, n.favorite) }; bump() }
        }
    }
    fun archiveMany(list: List<NoteSummary>) = viewModelScope.launch {
        val r = repo.value ?: return@launch
        if (list.isEmpty()) return@launch
        val target = !list.all { it.archived }
        list.forEach { r.setArchived(it.id, target) }; bump()
        say(if (list.size == 1) (if (target) "Nota archiviata." else "Nota ripristinata dall'archivio.")
            else (if (target) "${list.size} note archiviate." else "${list.size} note ripristinate dall'archivio.")) {
            viewModelScope.launch { list.forEach { n -> r.setArchived(n.id, n.archived) }; bump() }
        }
    }
    fun trashMany(list: List<NoteSummary>) = viewModelScope.launch {
        val r = repo.value ?: return@launch
        if (list.isEmpty()) return@launch
        list.forEach { r.moveToTrash(it.id) }; bump()
        say(if (list.size == 1) "Nota spostata nel cestino." else "${list.size} note spostate nel cestino.") {
            viewModelScope.launch { list.forEach { n -> r.restore(n.id) }; bump() }
        }
    }
    /** Esegue l'azione assegnata a un gesto di scorrimento. */
    fun swipeAction(a: com.notium.app.data.SwipeAction, n: NoteSummary) {
        when (a) {
            com.notium.app.data.SwipeAction.NONE -> {}
            com.notium.app.data.SwipeAction.TRASH -> trash(n)
            com.notium.app.data.SwipeAction.ARCHIVE -> toggleArchived(n)
            com.notium.app.data.SwipeAction.FAVORITE -> toggleFavorite(n)
            com.notium.app.data.SwipeAction.PIN -> togglePinned(n)
            com.notium.app.data.SwipeAction.LOCK -> toggleNoteLock(n.id)
        }
    }
    fun togglePinned(n: NoteSummary) = viewModelScope.launch { repo.value?.setPinned(n.id, !n.pinned); bump() }
    fun toggleArchived(n: NoteSummary) = viewModelScope.launch {
        repo.value?.setArchived(n.id, !n.archived); bump()
        say(if (!n.archived) "Nota archiviata." else "Nota ripristinata.") {
            viewModelScope.launch { repo.value?.setArchived(n.id, n.archived); bump() }
        }
    }
    fun trash(n: NoteSummary) = viewModelScope.launch {
        repo.value?.moveToTrash(n.id); bump()
        say("Nota spostata nel cestino.") { viewModelScope.launch { repo.value?.restore(n.id); bump() } }
    }
    fun restore(n: NoteSummary) = viewModelScope.launch {
        repo.value?.restore(n.id); bump()
        say("Nota ripristinata.") { viewModelScope.launch { repo.value?.moveToTrash(n.id); bump() } }
    }
    fun deleteForever(n: NoteSummary) = viewModelScope.launch { repo.value?.deletePermanently(n.id); bump(); say("Nota eliminata definitivamente.") }
    fun emptyTrash() = viewModelScope.launch { repo.value?.emptyTrash(); bump(); say("Cestino svuotato.") }
    fun moveNote(n: NoteSummary, folderId: Long?) = viewModelScope.launch {
        repo.value?.moveNote(n.id, folderId); bump()
        say("Nota spostata.") { viewModelScope.launch { repo.value?.moveNote(n.id, n.folderId); bump() } }
    }

    // -------------------------------------------------------------- cartelle e tag
    fun createFolderForEditor(name: String) {
        val n = name.trim(); if (n.isEmpty()) return
        viewModelScope.launch {
            val id = runCatching { repo.value?.createFolder(n, null, NotiumColors[folders.value.size % NotiumColors.size]) }.getOrNull()
            bump()
            if (id != null) editorSetFolder(id) else say("Impossibile creare la cartella.")
        }
    }
    fun createFolderAndMove(note: NoteSummary, name: String) {
        val n = name.trim(); if (n.isEmpty()) return
        viewModelScope.launch {
            val id = runCatching { repo.value?.createFolder(n, null, NotiumColors[folders.value.size % NotiumColors.size]) }.getOrNull()
            if (id != null) repo.value?.moveNote(note.id, id) else say("Impossibile creare la cartella.")
            bump()
        }
    }
    fun createFolder(name: String, parent: Long?, color: Int) = viewModelScope.launch {
        if (name.isNotBlank()) { repo.value?.createFolder(name, parent, color); bump() }
    }
    fun updateFolder(f: Folder, name: String, color: Int) = viewModelScope.launch {
        if (name.isNotBlank()) { repo.value?.updateFolder(f.id, name, color); bump() }
    }
    fun moveFolder(f: Folder, parent: Long?) = viewModelScope.launch {
        val ok = repo.value?.moveFolder(f.id, parent) ?: false
        if (!ok) say("Spostamento non consentito: creerebbe una gerarchia circolare.") else bump()
    }
    fun deleteFolder(f: Folder, mode: FolderDeleteMode) = viewModelScope.launch {
        repo.value?.deleteFolder(f.id, mode); bump()
        val s = filter.value.section
        if (s is Section.InFolder && s.id == f.id) setSection(Section.All)
    }
    suspend fun forbiddenParents(f: Folder): Set<Long> = repo.value?.forbiddenParents(f.id) ?: setOf(f.id)

    fun createTag(name: String, color: Int) = viewModelScope.launch {
        if (name.isNotBlank()) { repo.value?.createTag(name, color); bump() }
    }
    fun updateTag(t: Tag, name: String, color: Int) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        val ok = repo.value?.updateTag(t.id, name, color) ?: false
        if (!ok) say("Esiste gia' un tag con questo nome.") else bump()
    }
    fun deleteTag(t: Tag) = viewModelScope.launch {
        repo.value?.deleteTag(t.id); bump()
        val s = filter.value.section
        if (s is Section.WithTag && s.id == t.id) setSection(Section.All)
    }

    // ------------------------------------------------------------ blocco grafico delle note
    val noteLockRequest = MutableStateFlow<NoteLockRequest?>(null)
    private val unlockedNotes = HashSet<Long>()
    /** Errore (PIN/password errati) mostrato dentro il popup della nota, non in fondo allo schermo. */
    val noteLockError = MutableStateFlow<String?>(null)
    fun clearNoteLockError() { noteLockError.value = null }

    fun isNoteLocked(id: Long) = id in settings.value.lockedNotes

    private fun lockEntry(id: Long): List<String>? =
        settings.value.noteLocks.firstOrNull { it.substringBefore('|') == id.toString() }?.split('|')

    /** True se la nota e' protetta da un PIN numerico, false se da una password. */
    fun noteLockIsPin(id: Long) = lockEntry(id)?.getOrNull(1) == "P"

    /** Apre la nota; se e' protetta chiede prima il PIN o la password di quella nota. */
    fun requestOpen(id: Long) {
        if (isNoteLocked(id) && id !in unlockedNotes) noteLockRequest.value = NoteLockRequest(id, NoteLockMode.OPEN) else openNote(id)
    }

    /** Blocca la nota (chiedendo di creare PIN o password) oppure la sblocca (dopo la verifica). */
    fun toggleNoteLock(id: Long) {
        noteLockRequest.value = when {
            !isNoteLocked(id) -> NoteLockRequest(id, NoteLockMode.CREATE)
            id in unlockedNotes -> { removeNoteLock(id); null }
            else -> NoteLockRequest(id, NoteLockMode.REMOVE)
        }
    }

    private fun hashSecret(secret: CharArray, salt: ByteArray): String {
        val bytes = secret.toUtf8Bytes()
        val key = Crypto.deriveKey(bytes, salt)
        bytes.wipe()
        return Crypto.b64(key).also { key.wipe() }
    }

    /** True se per questa nota e' abilitato lo sblocco con impronta digitale. */
    fun noteLockUsesBio(id: Long) = lockEntry(id)?.getOrNull(4) == "B"

    fun createNoteLock(id: Long, secret: CharArray, isPin: Boolean, bio: Boolean) {
        viewModelScope.launch {
            busy.value = true
            val salt = Crypto.randomBytes(Crypto.SALT_LEN)
            val h = withContext(Dispatchers.IO) { hashSecret(secret, salt) }
            secret.fill('\u0000')
            busy.value = false
            val entry = listOf(id.toString(), if (isPin) "P" else "W", Crypto.b64(salt), h, if (bio) "B" else "-").joinToString("|")
            updateSettings { st -> st.copy(noteLocks = st.noteLocks.filterNot { it.substringBefore('|') == id.toString() }.toSet() + entry) }
            unlockedNotes.remove(id)
            noteLockRequest.value = null
            flushSettings()
            say("Nota bloccata.")
        }
    }

    private fun removeNoteLock(id: Long) {
        updateSettings { st -> st.copy(noteLocks = st.noteLocks.filterNot { it.substringBefore('|') == id.toString() }.toSet()) }
        unlockedNotes.remove(id)
        flushSettings()
        say("Nota sbloccata.")
    }

    fun noteAuthenticated(req: NoteLockRequest) {
        noteLockRequest.value = null
        noteLockError.value = null
        unlockedNotes.add(req.noteId)
        if (req.mode == NoteLockMode.REMOVE) removeNoteLock(req.noteId) else openNote(req.noteId)
    }

    fun cancelNoteLock() { noteLockRequest.value = null; noteLockError.value = null }
    fun relockNotes() { unlockedNotes.clear() }

    fun verifyNoteSecret(secret: CharArray, req: NoteLockRequest) {
        val e = lockEntry(req.noteId)
        if (e == null || e.size < 4) { secret.fill('\u0000'); noteLockRequest.value = null; return }
        viewModelScope.launch {
            busy.value = true
            val ok = withContext(Dispatchers.IO) {
                java.security.MessageDigest.isEqual(hashSecret(secret, Crypto.unb64(e[2])).toByteArray(), e[3].toByteArray())
            }
            secret.fill('\u0000')
            busy.value = false
            if (ok) noteAuthenticated(req) else noteLockError.value = if (noteLockIsPin(req.noteId)) "PIN errato." else "Password errata."
        }
    }

    // ----------------------------------------------------------------------- backup
    val pendingRestore = MutableStateFlow<RestorePreview?>(null)
    val importBanner = MutableStateFlow<ImportBanner?>(null)
    fun dismissImportBanner() { importBanner.value = null }

    fun createBackup(uri: Uri, password: CharArray) {
        val r = repo.value ?: return
        viewModelScope.launch {
            busy.value = true
            val result = runCatching {
                flushBlocking()
                val data = r.exportData()
                val bytes = withContext(Dispatchers.IO) { BackupManager.encrypt(data, password) }
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                        ?: error("flusso non disponibile")
                }
                data.notes.size
            }
            password.fill('\u0000')
            busy.value = false
            result.onSuccess { say("Backup cifrato creato: $it note.") }
                .onFailure { say("Backup non riuscito (spazio insufficiente o destinazione non valida).") }
        }
    }

    fun createPlainBackup(uri: Uri) {
        val r = repo.value ?: return
        viewModelScope.launch {
            busy.value = true
            val result = runCatching {
                flushBlocking()
                val data = r.exportData()
                val bytes = withContext(Dispatchers.IO) { BackupManager.plain(data) }
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) } ?: error("flusso non disponibile")
                }
                data.notes.size
            }
            busy.value = false
            result.onSuccess { say("Backup non cifrato creato: $it note.") }
                .onFailure { say("Backup non riuscito (spazio insufficiente o destinazione non valida).") }
        }
    }

    /** Apre un file di backup: se e' in chiaro lo legge subito, altrimenti chiama [askPassword]. */
    fun startRestore(uri: Uri, askPassword: () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            val bytes = runCatching {
                withContext(Dispatchers.IO) { app.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
            }.getOrNull()
            if (bytes == null) { busy.value = false; say("Impossibile leggere il file di backup."); return@launch }
            if (!BackupManager.isPlain(bytes)) { busy.value = false; askPassword(); return@launch }
            val res = runCatching { withContext(Dispatchers.IO) { BackupManager.readPlain(bytes) } }
            busy.value = false
            res.onSuccess { pendingRestore.value = RestorePreview(it) }
                .onFailure { say("Il backup e' corrotto o non valido.") }
        }
    }

    /** Passi 1-5 del ripristino: legge il file, verifica password, struttura, firma e integrita'. */
    fun prepareRestore(uri: Uri, password: CharArray) {
        viewModelScope.launch {
            busy.value = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val bytes = app.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("lettura")
                    BackupManager.decrypt(bytes, password)
                }
            }
            password.fill('\u0000')
            busy.value = false
            result.onSuccess { pendingRestore.value = RestorePreview(it) }
                .onFailure {
                    say(
                        when ((it as? BackupException)?.kind) {
                            BackupException.Kind.NOT_A_BACKUP -> "Il file selezionato non e' un backup di Notium."
                            BackupException.Kind.WRONG_PASSWORD_OR_CORRUPT -> "Password del backup errata oppure file danneggiato."
                            BackupException.Kind.UNSUPPORTED_VERSION -> "Questo backup e' stato creato da una versione non supportata."
                            BackupException.Kind.INVALID_CONTENT -> "Il backup e' corrotto o non valido."
                            null -> "Impossibile leggere il file di backup."
                        }
                    )
                }
        }
    }

    /** Passi 6-7: dopo la conferma sostituisce i dati. Se fallisce, i dati esistenti restano invariati. */
    fun confirmRestore() {
        val p = pendingRestore.value ?: return
        val r = repo.value ?: return
        viewModelScope.launch {
            busy.value = true
            autosave?.cancel(); _editor.value = null
            val res = runCatching { r.replaceAll(p.data) }
            busy.value = false
            pendingRestore.value = null
            resetNav(); bump()
            say(if (res.isSuccess) "Ripristino completato." else "Ripristino non riuscito: i dati esistenti non sono stati modificati.")
        }
    }

    fun cancelRestore() { pendingRestore.value = null }

    // -------------------------------------------------- eliminazione di tutti i dati
    fun wipeEverything(password: CharArray) {
        viewModelScope.launch {
            busy.value = true
            val ok = withContext(Dispatchers.IO) { vault.unlockWithPassword(password)?.also { it.wipe() } != null }
            password.fill('\u0000')
            if (!ok) { busy.value = false; say("Password errata."); return@launch }
            autosave?.cancel()
            _editor.value = null
            repo.value?.close(); repo.value = null
            master?.wipe(); master = null
            persistJob?.cancel()
            withContext(Dispatchers.IO) {
                NotiumDatabase.destroyFiles(app)
                vault.destroy()
                prefs.clear()
                app.cacheDir.deleteRecursively()
                app.cacheDir.mkdirs()
            }
            resetNav()
            pendingRestore.value = null
            importBanner.value = null
            filter.value = NoteFilter()
            refreshAuthOptions()
            busy.value = false
            _auth.value = AuthState.Setup
        }
    }
}
