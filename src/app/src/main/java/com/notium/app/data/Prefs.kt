package com.notium.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore(name = "notium_prefs")

enum class ThemeMode(val label: String) { SYSTEM("Automatico"), LIGHT("Chiaro"), DARK("Scuro"), BLACK("Nero (AMOLED)") }
enum class FabStyle(val label: String) { EXTENDED("Con testo"), ICON("Solo icona"), HIDDEN("Nascosto") }
enum class FabPlace(val label: String) { END("A destra"), CENTER("Al centro"), START("A sinistra") }
enum class StartSection(val label: String) { ALL("Tutte le note"), FAVORITES("Preferiti"), PINNED("Fissate"), ARCHIVE("Archivio") }
/** Azione assegnabile al gesto di scorrimento di una nota nell'elenco. */
enum class SwipeAction(val label: String) {
    NONE("Nessuna azione"), TRASH("Sposta nel cestino"), ARCHIVE("Archivia / ripristina"),
    FAVORITE("Aggiungi / rimuovi dai preferiti"), PIN("Fissa / sblocca in alto"), LOCK("Blocca / sblocca nota"),
}

enum class DateStyle(val label: String) {
    SHORT("Breve (12 nov)"), MEDIUM("Normale (12 nov 2026)"), LONG("Estesa (12 novembre 2026)"),
    RELATIVE("Relativa (2 giorni fa)"), WITH_TIME("Con ora (12 nov 2026, 21:40)")
}

fun StartSection.toSection(): Section = when (this) {
    StartSection.ALL -> Section.All
    StartSection.FAVORITES -> Section.Favorites
    StartSection.PINNED -> Section.Pinned
    StartSection.ARCHIVE -> Section.Archive
}

enum class AnimStyle(val label: String) { SLIDE("Scorrimento"), SLIDE_UP("Dal basso"), FADE("Dissolvenza"), SCALE("Zoom") }
enum class CardStyle(val label: String) { OUTLINED("Con bordo"), FILLED("Riempita"), FLAT("Senza bordo") }
enum class ListStyle(val label: String) { COMFORTABLE("Comoda"), COMPACT("Compatta") }
/** Lingue dell'app. L'italiano e' la lingua dei testi nel codice; le altre sono tradotte in ui/I18n. */
enum class AppLang(val label: String, val idx: Int) { EN("English", 0), ES("Español", 1), IT("Italiano", -1), ZH("中文（普通话）", 2) }
enum class EditorFont(val label: String) { SANS("Sans-serif"), SERIF("Serif"), MONO("Monospazio") }

/** Preferenze non sensibili (nessun contenuto di note). */
data class Settings(
    val theme: ThemeMode = ThemeMode.DARK,
    val accent: Int = 0,                    // 0 = monocromatico
    val cornerRadius: Int = 14,             // 0..28 dp
    val cardStyle: CardStyle = CardStyle.OUTLINED,
    val uiFont: EditorFont = EditorFont.SANS,
    val showDates: Boolean = true,
    val showTags: Boolean = true,
    val showFolderInList: Boolean = true,
    val editorFontSize: Float = 17f,        // sp
    val lineSpacing: Float = 1.5f,          // moltiplicatore
    val showToolbar: Boolean = true,
    val borderWidth: Int = 1,               // 0..3 dp
    val gridColumns: Int = 1,               // 1..3
    val animations: Boolean = true,
    val animSpeed: Float = 1f,              // 0.5 (lente) .. 2 (veloci)
    val animStyle: AnimStyle = AnimStyle.SLIDE,
    val bgColor: Int = 0,                   // 0 = predefinito del tema
    val cardColor: Int = 0,                 // 0 = predefinito del tema
    val cardPadding: Int = 14,              // dp
    val listTitleSize: Float = 16f,         // sp
    val listPreviewSize: Float = 14f,       // sp
    val boldTitles: Boolean = true,
    val showSearch: Boolean = true,
    val drawerFolders: Boolean = true,
    val drawerTags: Boolean = true,
    val animPress: Boolean = true,
    val animList: Boolean = true,
    val pressScale: Float = 0.97f,
    val cardGap: Int = 10,                  // dp tra le schede
    val screenPadding: Int = 16,            // dp ai lati della lista
    val highContrast: Boolean = false,
    val editorLetterSpacing: Float = 0f,    // sp
    val editorTitleBold: Boolean = true,
    val showPinIcon: Boolean = true,
    val showFavIcon: Boolean = true,
    val homeTitle: String = "Notium",
    val autoList: Boolean = true,
    val liveStyle: Boolean = true,
    val showTopLogo: Boolean = true,
    val fabStyle: FabStyle = FabStyle.EXTENDED,
    val fabPlace: FabPlace = FabPlace.END,
    val swipeLeft: SwipeAction = SwipeAction.TRASH,    // scorrere da destra verso sinistra
    val swipeRight: SwipeAction = SwipeAction.ARCHIVE, // scorrere da sinistra verso destra
    val titleLines: Int = 1,
    val startSection: StartSection = StartSection.ALL,
    val dateStyle: DateStyle = DateStyle.MEDIUM,
    /** Blocchi grafici delle singole note: "id|P/W|salt|hash". Restano sul dispositivo, non entrano nei backup. */
    val noteLocks: Set<String> = emptySet(),
    val editorMargin: Int = 20,             // dp
    val titleSize: Float = 26f,             // sp
    val autoCorrect: Boolean = true,
    val autoCapitalize: Boolean = true,
    val autosaveDelayMs: Int = 700,
    val keepScreenOn: Boolean = false,
    val haptics: Boolean = true,
    val shuffleKeypad: Boolean = false,
    val fontScale: Float = 1f,
    val autoLockSeconds: Int = 60,          // 0 = subito, -1 = mai finche' l'app resta aperta
    val secureScreen: Boolean = true,       // anteprime nascoste nelle app recenti, niente screenshot
    val clearClipboard: Boolean = true,
    val listStyle: ListStyle = ListStyle.COMFORTABLE,
    val previewLines: Int = 2,
    val editorFont: EditorFont = EditorFont.SANS,
    val showCounters: Boolean = true,
    val openInPreview: Boolean = false,
    val sort: SortOrder = SortOrder.MODIFIED,
    val trashDays: Int = 30,                // 0 = non svuotare automaticamente
    val language: AppLang = AppLang.EN,
    val langChosen: Boolean = false,
) {
    val lockedNotes: Set<Long> get() = noteLocks.mapNotNull { it.substringBefore('|').toLongOrNull() }.toSet()
}

class Prefs(private val context: Context) {
    private object K {
        val theme = stringPreferencesKey("theme")
        val accent = intPreferencesKey("accent")
        val corner = intPreferencesKey("corner")
        val cardStyle = stringPreferencesKey("card_style")
        val uiFont = stringPreferencesKey("ui_font")
        val showDates = booleanPreferencesKey("show_dates")
        val showTags = booleanPreferencesKey("show_tags")
        val showFolder = booleanPreferencesKey("show_folder")
        val edSize = floatPreferencesKey("ed_size")
        val lineSp = floatPreferencesKey("line_sp")
        val toolbar = booleanPreferencesKey("toolbar")
        val border = intPreferencesKey("border")
        val cols = intPreferencesKey("cols")
        val anim = booleanPreferencesKey("anim")
        val animSpeed = floatPreferencesKey("anim_speed")
        val animStyle = stringPreferencesKey("anim_style")
        val bgColor = intPreferencesKey("bg_color")
        val cardColor = intPreferencesKey("card_color")
        val cardPad = intPreferencesKey("card_pad")
        val listTitleSize = floatPreferencesKey("list_title_size")
        val listPreviewSize = floatPreferencesKey("list_preview_size")
        val boldTitles = booleanPreferencesKey("bold_titles")
        val showSearch = booleanPreferencesKey("show_search")
        val drawerFolders = booleanPreferencesKey("drawer_folders")
        val drawerTags = booleanPreferencesKey("drawer_tags")
        val animPress = booleanPreferencesKey("anim_press")
        val animList = booleanPreferencesKey("anim_list")
        val pressScale = floatPreferencesKey("press_scale")
        val cardGap = intPreferencesKey("card_gap")
        val screenPad = intPreferencesKey("screen_pad")
        val highContrast = booleanPreferencesKey("high_contrast")
        val edLetter = floatPreferencesKey("ed_letter")
        val edTitleBold = booleanPreferencesKey("ed_title_bold")
        val showPin = booleanPreferencesKey("show_pin")
        val showFav = booleanPreferencesKey("show_fav")
        val homeTitle = stringPreferencesKey("home_title")
        val autoList = booleanPreferencesKey("auto_list")
        val liveStyle = booleanPreferencesKey("live_style")
        val topLogo = booleanPreferencesKey("top_logo")
        val fabStyle = stringPreferencesKey("fab_style")
        val fabPlace = stringPreferencesKey("fab_place")
        val swipeLeft = stringPreferencesKey("swipe_left")
        val swipeRight = stringPreferencesKey("swipe_right")
        val titleLines = intPreferencesKey("title_lines")
        val startSec = stringPreferencesKey("start_section")
        val dateStyle = stringPreferencesKey("date_style")
        val lockedNotes = androidx.datastore.preferences.core.stringSetPreferencesKey("note_locks")
        val edMargin = intPreferencesKey("ed_margin")
        val titleSize = floatPreferencesKey("title_size")
        val autoCorrect = booleanPreferencesKey("auto_correct")
        val autoCap = booleanPreferencesKey("auto_cap")
        val saveDelay = intPreferencesKey("save_delay")
        val keepOn = booleanPreferencesKey("keep_on")
        val haptics = booleanPreferencesKey("haptics")
        val shuffle = booleanPreferencesKey("shuffle")
        val fontScale = floatPreferencesKey("font_scale")
        val autoLock = intPreferencesKey("auto_lock")
        val secure = booleanPreferencesKey("secure_screen")
        val clip = booleanPreferencesKey("clear_clipboard")
        val listStyle = stringPreferencesKey("list_style")
        val previewLines = intPreferencesKey("preview_lines")
        val editorFont = stringPreferencesKey("editor_font")
        val counters = booleanPreferencesKey("counters")
        val openPreview = booleanPreferencesKey("open_preview")
        val sort = stringPreferencesKey("sort")
        val trashDays = intPreferencesKey("trash_days")
        val lang = stringPreferencesKey("lang")
        val langChosen = booleanPreferencesKey("lang_chosen")
    }

    private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
        runCatching { enumValueOf<E>(this ?: "") }.getOrDefault(default)

    val settings: Flow<Settings> = context.store.data.map { p ->
        val d = Settings()
        Settings(
            theme = p[K.theme].toEnum(d.theme),
            accent = p[K.accent] ?: d.accent,
            cornerRadius = p[K.corner] ?: d.cornerRadius,
            cardStyle = p[K.cardStyle].toEnum(d.cardStyle),
            uiFont = p[K.uiFont].toEnum(d.uiFont),
            showDates = p[K.showDates] ?: d.showDates,
            showTags = p[K.showTags] ?: d.showTags,
            showFolderInList = p[K.showFolder] ?: d.showFolderInList,
            editorFontSize = p[K.edSize] ?: d.editorFontSize,
            lineSpacing = p[K.lineSp] ?: d.lineSpacing,
            showToolbar = p[K.toolbar] ?: d.showToolbar,
            borderWidth = p[K.border] ?: d.borderWidth,
            gridColumns = p[K.cols] ?: d.gridColumns,
            animations = p[K.anim] ?: d.animations,
            animSpeed = p[K.animSpeed] ?: d.animSpeed,
            animStyle = p[K.animStyle].toEnum(d.animStyle),
            bgColor = p[K.bgColor] ?: d.bgColor,
            cardColor = p[K.cardColor] ?: d.cardColor,
            cardPadding = p[K.cardPad] ?: d.cardPadding,
            listTitleSize = p[K.listTitleSize] ?: d.listTitleSize,
            listPreviewSize = p[K.listPreviewSize] ?: d.listPreviewSize,
            boldTitles = p[K.boldTitles] ?: d.boldTitles,
            showSearch = p[K.showSearch] ?: d.showSearch,
            drawerFolders = p[K.drawerFolders] ?: d.drawerFolders,
            drawerTags = p[K.drawerTags] ?: d.drawerTags,
            animPress = p[K.animPress] ?: d.animPress,
            animList = p[K.animList] ?: d.animList,
            pressScale = p[K.pressScale] ?: d.pressScale,
            cardGap = p[K.cardGap] ?: d.cardGap,
            screenPadding = p[K.screenPad] ?: d.screenPadding,
            highContrast = p[K.highContrast] ?: d.highContrast,
            editorLetterSpacing = p[K.edLetter] ?: d.editorLetterSpacing,
            editorTitleBold = p[K.edTitleBold] ?: d.editorTitleBold,
            showPinIcon = p[K.showPin] ?: d.showPinIcon,
            showFavIcon = p[K.showFav] ?: d.showFavIcon,
            homeTitle = p[K.homeTitle] ?: d.homeTitle,
            autoList = p[K.autoList] ?: d.autoList,
            liveStyle = p[K.liveStyle] ?: d.liveStyle,
            showTopLogo = p[K.topLogo] ?: d.showTopLogo,
            fabStyle = p[K.fabStyle].toEnum(d.fabStyle),
            fabPlace = p[K.fabPlace].toEnum(d.fabPlace),
            swipeLeft = p[K.swipeLeft].toEnum(d.swipeLeft),
            swipeRight = p[K.swipeRight].toEnum(d.swipeRight),
            titleLines = p[K.titleLines] ?: d.titleLines,
            startSection = p[K.startSec].toEnum(d.startSection),
            dateStyle = p[K.dateStyle].toEnum(d.dateStyle),
            noteLocks = p[K.lockedNotes]?.filter { it.contains('|') }?.toSet() ?: d.noteLocks,
            editorMargin = p[K.edMargin] ?: d.editorMargin,
            titleSize = p[K.titleSize] ?: d.titleSize,
            autoCorrect = p[K.autoCorrect] ?: d.autoCorrect,
            autoCapitalize = p[K.autoCap] ?: d.autoCapitalize,
            autosaveDelayMs = p[K.saveDelay] ?: d.autosaveDelayMs,
            keepScreenOn = p[K.keepOn] ?: d.keepScreenOn,
            haptics = p[K.haptics] ?: d.haptics,
            shuffleKeypad = p[K.shuffle] ?: d.shuffleKeypad,
            fontScale = p[K.fontScale] ?: d.fontScale,
            autoLockSeconds = p[K.autoLock] ?: d.autoLockSeconds,
            secureScreen = p[K.secure] ?: d.secureScreen,
            clearClipboard = p[K.clip] ?: d.clearClipboard,
            listStyle = p[K.listStyle].toEnum(d.listStyle),
            previewLines = p[K.previewLines] ?: d.previewLines,
            editorFont = p[K.editorFont].toEnum(d.editorFont),
            showCounters = p[K.counters] ?: d.showCounters,
            openInPreview = p[K.openPreview] ?: d.openInPreview,
            sort = p[K.sort].toEnum(d.sort),
            trashDays = p[K.trashDays] ?: d.trashDays,
            language = p[K.lang].toEnum(d.language),
            langChosen = p[K.langChosen] ?: d.langChosen,
        )
    }

    suspend fun update(block: (Settings) -> Settings, current: Settings) {
        val s = block(current)
        context.store.edit { p: androidx.datastore.preferences.core.MutablePreferences ->
            p[K.theme] = s.theme.name
            p[K.accent] = s.accent
            p[K.corner] = s.cornerRadius
            p[K.cardStyle] = s.cardStyle.name
            p[K.uiFont] = s.uiFont.name
            p[K.showDates] = s.showDates
            p[K.showTags] = s.showTags
            p[K.showFolder] = s.showFolderInList
            p[K.edSize] = s.editorFontSize
            p[K.lineSp] = s.lineSpacing
            p[K.toolbar] = s.showToolbar
            p[K.border] = s.borderWidth
            p[K.cols] = s.gridColumns
            p[K.anim] = s.animations
            p[K.animSpeed] = s.animSpeed
            p[K.animStyle] = s.animStyle.name
            p[K.bgColor] = s.bgColor
            p[K.cardColor] = s.cardColor
            p[K.cardPad] = s.cardPadding
            p[K.listTitleSize] = s.listTitleSize
            p[K.listPreviewSize] = s.listPreviewSize
            p[K.boldTitles] = s.boldTitles
            p[K.showSearch] = s.showSearch
            p[K.drawerFolders] = s.drawerFolders
            p[K.drawerTags] = s.drawerTags
            p[K.animPress] = s.animPress
            p[K.animList] = s.animList
            p[K.pressScale] = s.pressScale
            p[K.cardGap] = s.cardGap
            p[K.screenPad] = s.screenPadding
            p[K.highContrast] = s.highContrast
            p[K.edLetter] = s.editorLetterSpacing
            p[K.edTitleBold] = s.editorTitleBold
            p[K.showPin] = s.showPinIcon
            p[K.showFav] = s.showFavIcon
            p[K.homeTitle] = s.homeTitle
            p[K.autoList] = s.autoList
            p[K.liveStyle] = s.liveStyle
            p[K.topLogo] = s.showTopLogo
            p[K.fabStyle] = s.fabStyle.name
            p[K.fabPlace] = s.fabPlace.name
            p[K.swipeLeft] = s.swipeLeft.name
            p[K.swipeRight] = s.swipeRight.name
            p[K.titleLines] = s.titleLines
            p[K.startSec] = s.startSection.name
            p[K.dateStyle] = s.dateStyle.name
            p[K.lockedNotes] = s.noteLocks
            p[K.edMargin] = s.editorMargin
            p[K.titleSize] = s.titleSize
            p[K.autoCorrect] = s.autoCorrect
            p[K.autoCap] = s.autoCapitalize
            p[K.saveDelay] = s.autosaveDelayMs
            p[K.keepOn] = s.keepScreenOn
            p[K.haptics] = s.haptics
            p[K.shuffle] = s.shuffleKeypad
            p[K.fontScale] = s.fontScale
            p[K.autoLock] = s.autoLockSeconds
            p[K.secure] = s.secureScreen
            p[K.clip] = s.clearClipboard
            p[K.listStyle] = s.listStyle.name
            p[K.previewLines] = s.previewLines
            p[K.editorFont] = s.editorFont.name
            p[K.counters] = s.showCounters
            p[K.openPreview] = s.openInPreview
            p[K.sort] = s.sort.name
            p[K.trashDays] = s.trashDays
            p[K.lang] = s.language.name
            p[K.langChosen] = s.langChosen
        }
    }

    suspend fun clear() {
        context.store.edit { it.clear() }
    }
}
