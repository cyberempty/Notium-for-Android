package com.notium.app.ui

/** Dizionario: italiano ::: inglese ::: spagnolo ::: cinese. Segnaposto {n} = numero, <n> = testo. */
object I18nData {
    val rows: String = """
Salvato ::: Saved ::: Guardado ::: 已保存
Salvataggio… ::: Saving… ::: Guardando… ::: 正在保存…
Modifiche non salvate ::: Unsaved changes ::: Cambios sin guardar ::: 有未保存的更改
Password errata. ::: Wrong password. ::: Contraseña incorrecta. ::: 密码错误。
PIN errato. Tentativi rimasti prima del blocco: {1}. ::: Wrong PIN. Attempts left before lockout: {1}. ::: PIN incorrecto. Intentos restantes antes del bloqueo: {1}. ::: PIN 错误。锁定前剩余尝试次数：{1}。
Troppi tentativi. Riprova tra poco o usa la password. ::: Too many attempts. Try again shortly or use the password. ::: Demasiados intentos. Inténtalo de nuevo en un momento o usa la contraseña. ::: 尝试次数过多。请稍后再试，或使用密码。
PIN disattivato dopo troppi tentativi. Usa la password principale. ::: PIN disabled after too many attempts. Use the main password. ::: PIN desactivado tras demasiados intentos. Usa la contraseña principal. ::: 尝试次数过多，PIN 已停用。请使用主密码。
La biometria e' cambiata: inserisci la password principale. ::: Biometrics changed: enter the main password. ::: La biometría ha cambiado: introduce la contraseña principal. ::: 生物识别信息已变更：请输入主密码。
Sblocco biometrico non riuscito: inserisci la password principale. ::: Biometric unlock failed: enter the main password. ::: Falló el desbloqueo biométrico: introduce la contraseña principal. ::: 生物识别解锁失败：请输入主密码。
Impossibile aprire il database. Il file potrebbe essere danneggiato: ripristina un backup. ::: Unable to open the database. The file may be damaged: restore a backup. ::: No se puede abrir la base de datos. El archivo podría estar dañado: restaura una copia de seguridad. ::: 无法打开数据库。文件可能已损坏：请恢复备份。
Password modificata. ::: Password changed. ::: Contraseña cambiada. ::: 密码已修改。
Password attuale errata. ::: Current password is wrong. ::: La contraseña actual es incorrecta. ::: 当前密码错误。
PIN attivato. ::: PIN enabled. ::: PIN activado. ::: PIN 已启用。
PIN disattivato. ::: PIN disabled. ::: PIN desactivado. ::: PIN 已停用。
Impronta digitale attivata. ::: Fingerprint enabled. ::: Huella digital activada. ::: 指纹已启用。
Impossibile attivare la biometria. ::: Unable to enable biometrics. ::: No se puede activar la biometría. ::: 无法启用生物识别。
Biometria disattivata. ::: Biometrics disabled. ::: Biometría desactivada. ::: 生物识别已停用。
Errore durante il salvataggio. Riprova. ::: Error while saving. Try again. ::: Error al guardar. Inténtalo de nuevo. ::: 保存时出错，请重试。
Nota spostata. ::: Note moved. ::: Nota movida. ::: 笔记已移动。
Nota archiviata. ::: Note archived. ::: Nota archivada. ::: 笔记已归档。
Nota ripristinata dall'archivio. ::: Note restored from archive. ::: Nota restaurada del archivo. ::: 笔记已从归档中恢复。
Nota spostata nel cestino. ::: Note moved to trash. ::: Nota movida a la papelera. ::: 笔记已移入回收站。
Nota ripristinata. ::: Note restored. ::: Nota restaurada. ::: 笔记已恢复。
Nota eliminata definitivamente. ::: Note permanently deleted. ::: Nota eliminada definitivamente. ::: 笔记已永久删除。
Cestino svuotato. ::: Trash emptied. ::: Papelera vaciada. ::: 回收站已清空。
Impossibile creare la cartella. ::: Unable to create the folder. ::: No se puede crear la carpeta. ::: 无法创建文件夹。
Spostamento non consentito: creerebbe una gerarchia circolare. ::: Move not allowed: it would create a circular hierarchy. ::: Movimiento no permitido: crearía una jerarquía circular. ::: 不允许移动：这会形成循环层级。
Esiste gia' un tag con questo nome. ::: A tag with this name already exists. ::: Ya existe una etiqueta con este nombre. ::: 已存在同名标签。
flusso non disponibile ::: stream not available ::: flujo no disponible ::: 数据流不可用
Backup cifrato creato: {1} note. ::: Encrypted backup created: {1} notes. ::: Copia cifrada creada: {1} notas. ::: 已创建加密备份：{1} 条笔记。
Backup non riuscito (spazio insufficiente o destinazione non valida). ::: Backup failed (not enough space or invalid destination). ::: Falló la copia (espacio insuficiente o destino no válido). ::: 备份失败（空间不足或目标无效）。
Il file selezionato non e' un backup di Notium. ::: The selected file is not a Notium backup. ::: El archivo seleccionado no es una copia de Notium. ::: 所选文件不是 Notium 备份。
Password del backup errata oppure file danneggiato. ::: Wrong backup password or damaged file. ::: Contraseña de la copia incorrecta o archivo dañado. ::: 备份密码错误或文件已损坏。
Questo backup e' stato creato da una versione non supportata. ::: This backup was created by an unsupported version. ::: Esta copia fue creada con una versión no compatible. ::: 此备份由不受支持的版本创建。
Il backup e' corrotto o non valido. ::: The backup is corrupted or invalid. ::: La copia está dañada o no es válida. ::: 备份已损坏或无效。
Impossibile leggere il file di backup. ::: Unable to read the backup file. ::: No se puede leer el archivo de copia. ::: 无法读取备份文件。
Ripristino completato. ::: Restore completed. ::: Restauración completada. ::: 恢复完成。
Ripristino non riuscito: i dati esistenti non sono stati modificati. ::: Restore failed: existing data was not changed. ::: Restauración fallida: los datos existentes no se han modificado. ::: 恢复失败：现有数据未被更改。
Annulla ::: Cancel ::: Cancelar ::: 取消
Sblocca Notium ::: Unlock Notium ::: Desbloquear Notium ::: 解锁 Notium
Usa l'impronta digitale ::: Use your fingerprint ::: Usa tu huella digital ::: 使用指纹
Usa la password ::: Use password ::: Usar contraseña ::: 使用密码
Autenticazione non valida. ::: Invalid authentication. ::: Autenticación no válida. ::: 验证无效。
Ultima modifica ::: Last modified ::: Última modificación ::: 最后修改
Data di creazione ::: Creation date ::: Fecha de creación ::: 创建日期
Titolo ::: Title ::: Título ::: 标题
Preferiti ::: Favorites ::: Favoritos ::: 收藏
Note fissate ::: Pinned notes ::: Notas fijadas ::: 置顶笔记
Automatico ::: Automatic ::: Automático ::: 自动
Chiaro ::: Light ::: Claro ::: 浅色
Scuro ::: Dark ::: Oscuro ::: 深色
Nero (AMOLED) ::: Black (AMOLED) ::: Negro (AMOLED) ::: 纯黑（AMOLED）
Con testo ::: With text ::: Con texto ::: 带文字
Solo icona ::: Icon only ::: Solo icono ::: 仅图标
Nascosto ::: Hidden ::: Oculto ::: 隐藏
A destra ::: On the right ::: A la derecha ::: 靠右
Al centro ::: In the center ::: Al centro ::: 居中
Tutte le note ::: All notes ::: Todas las notas ::: 所有笔记
Fissate ::: Pinned ::: Fijadas ::: 已置顶
Archivio ::: Archive ::: Archivo ::: 归档
Breve (12 nov) ::: Short (12 Nov) ::: Corta (12 nov) ::: 简短（11月12日）
Normale (12 nov 2026) ::: Normal (12 Nov 2026) ::: Normal (12 nov 2026) ::: 标准（2026年11月12日）
Estesa (12 novembre 2026) ::: Long (12 November 2026) ::: Extendida (12 de noviembre de 2026) ::: 完整（2026年11月12日）
Relativa (2 giorni fa) ::: Relative (2 days ago) ::: Relativa (hace 2 días) ::: 相对（2 天前）
Con ora (12 nov 2026, 21:40) ::: With time (12 Nov 2026, 21:40) ::: Con hora (12 nov 2026, 21:40) ::: 含时间（2026年11月12日 21:40）
Scorrimento ::: Slide ::: Deslizamiento ::: 滑动
Dal basso ::: From the bottom ::: Desde abajo ::: 自下而上
Dissolvenza ::: Fade ::: Desvanecimiento ::: 淡入淡出
Zoom ::: Zoom ::: Zoom ::: 缩放
Con bordo ::: Outlined ::: Con borde ::: 带边框
Riempita ::: Filled ::: Rellena ::: 填充
Senza bordo ::: Flat ::: Sin borde ::: 无边框
Comoda ::: Comfortable ::: Cómoda ::: 舒适
Compatta ::: Compact ::: Compacta ::: 紧凑
Sans-serif ::: Sans-serif ::: Sans-serif ::: 无衬线
Serif ::: Serif ::: Serif ::: 衬线
Monospazio ::: Monospace ::: Monoespaciada ::: 等宽
Debole ::: Weak ::: Débil ::: 弱
Discreta ::: Fair ::: Aceptable ::: 一般
Buona ::: Good ::: Buena ::: 良好
Ottima ::: Excellent ::: Excelente ::: 极佳
Almeno {1} caratteri. ::: At least {1} characters. ::: Al menos {1} caracteres. ::: 至少 {1} 个字符。
Password troppo debole: mescola lettere, numeri e simboli. ::: Password too weak: mix letters, numbers and symbols. ::: Contraseña demasiado débil: combina letras, números y símbolos. ::: 密码太弱：请混合使用字母、数字和符号。
Le due password non coincidono. ::: The two passwords do not match. ::: Las dos contraseñas no coinciden. ::: 两次输入的密码不一致。
Benvenuto in Notium ::: Welcome to Notium ::: Bienvenido a Notium ::: 欢迎使用 Notium
Your notes. Your privacy. ::: Your notes. Your privacy. ::: Tus notas. Tu privacidad. ::: 你的笔记，你的隐私。
Nessun account, nessuna connessione, nessun cloud. Le note restano cifrate su questo telefono. ::: No account, no connection, no cloud. Your notes stay encrypted on this phone. ::: Sin cuenta, sin conexión, sin nube. Tus notas permanecen cifradas en este teléfono. ::: 无需账号，无需联网，没有云端。笔记始终加密保存在这部手机上。
Password principale ::: Main password ::: Contraseña principal ::: 主密码
Conferma password ::: Confirm password ::: Confirmar contraseña ::: 确认密码
Importante: la password non si può recuperare. Se la dimentichi, le note non sono più leggibili. Crea un backup cifrato dalle Impostazioni. ::: Important: the password cannot be recovered. If you forget it, your notes can no longer be read. Create an encrypted backup from Settings. ::: Importante: la contraseña no se puede recuperar. Si la olvidas, las notas ya no se podrán leer. Crea una copia cifrada desde Ajustes. ::: 重要：密码无法找回。如果忘记，笔记将无法再读取。请在设置中创建加密备份。
Crea e proteggi ::: Create and protect ::: Crear y proteger ::: 创建并保护
PIN e impronta digitale si attivano dopo, da Impostazioni › Sicurezza. ::: PIN and fingerprint can be enabled later, in Settings › Security. ::: El PIN y la huella se activan después, en Ajustes › Seguridad. ::: PIN 和指纹可稍后在 设置 › 安全 中启用。
Notium ::: Notium ::: Notium ::: Notium
Bloccata ::: Locked ::: Bloqueada ::: 已锁定
Sblocca ::: Unlock ::: Desbloquear ::: 解锁
Usa la password principale ::: Use the main password ::: Usar la contraseña principal ::: 使用主密码
Impronta digitale ::: Fingerprint ::: Huella digital ::: 指纹
Usa il PIN ::: Use PIN ::: Usar PIN ::: 使用 PIN
Troppi tentativi. Riprova tra {1} s. ::: Too many attempts. Try again in {1} s. ::: Demasiados intentos. Inténtalo de nuevo en {1} s. ::: 尝试次数过多。请在 {1} 秒后重试。
Cancella ::: Delete ::: Borrar ::: 删除
Scegli la lingua ::: Choose your language ::: Elige tu idioma ::: 选择语言
Avanti ::: Next ::: Siguiente ::: 下一步
Puoi cambiare la lingua in qualsiasi momento da Impostazioni. ::: You can change the language at any time in Settings. ::: Puedes cambiar el idioma en cualquier momento desde Ajustes. ::: 你可以随时在设置中更改语言。
Lingua ::: Language ::: Idioma ::: 语言
Mostra/nascondi ::: Show/hide ::: Mostrar/ocultar ::: 显示/隐藏
Robustezza: <1> ::: Strength: <1> ::: Seguridad: <1> ::: 强度：<1>
Salva ::: Save ::: Guardar ::: 保存
Cartella principale ::: Main folder ::: Carpeta principal ::: 主文件夹
Nuova cartella… ::: New folder… ::: Nueva carpeta… ::: 新建文件夹…
Nome della nuova cartella ::: New folder name ::: Nombre de la nueva carpeta ::: 新文件夹名称
Crea ::: Create ::: Crear ::: 创建
Cartelle ::: Folders ::: Carpetas ::: 文件夹
Indietro ::: Back ::: Atrás ::: 返回
Nuova cartella ::: New folder ::: Nueva carpeta ::: 新建文件夹
Nessuna cartella. Creane una con il pulsante in basso. ::: No folders. Create one with the button below. ::: Sin carpetas. Crea una con el botón de abajo. ::: 还没有文件夹。点击下方按钮创建。
{1} note ::: {1} notes ::: {1} notas ::: {1} 条笔记
Altro ::: More ::: Más ::: 更多
Nuova sottocartella ::: New subfolder ::: Nueva subcarpeta ::: 新建子文件夹
Rinomina e colore ::: Rename and color ::: Renombrar y color ::: 重命名与颜色
Sposta ::: Move ::: Mover ::: 移动
Elimina ::: Delete ::: Eliminar ::: 删除
Nome ::: Name ::: Nombre ::: 名称
Modifica cartella ::: Edit folder ::: Editar carpeta ::: 编辑文件夹
Sposta “<1>” in ::: Move “<1>” to ::: Mover “<1>” a ::: 将“<1>”移动到
Eliminare la cartella? ::: Delete the folder? ::: ¿Eliminar la carpeta? ::: 删除此文件夹？
“<1>” è vuota. Le sottocartelle salgono di un livello. ::: “<1>” is empty. Its subfolders move up one level. ::: “<1>” está vacía. Las subcarpetas suben un nivel. ::: “<1>”是空的。其子文件夹将上移一级。
Eliminare “<1>”? ::: Delete “<1>”? ::: ¿Eliminar “<1>”? ::: 删除“<1>”？
La cartella contiene {1} note. Cosa vuoi fare? ::: The folder contains {1} notes. What do you want to do? ::: La carpeta contiene {1} notas. ¿Qué quieres hacer? ::: 此文件夹包含 {1} 条笔记。你想怎么处理？
Sposta le note nella cartella principale ::: Move the notes to the main folder ::: Mover las notas a la carpeta principal ::: 将笔记移到主文件夹
Sposta le note in un'altra cartella ::: Move the notes to another folder ::: Mover las notas a otra carpeta ::: 将笔记移到其他文件夹
Elimina le note (vanno nel cestino) ::: Delete the notes (they go to the trash) ::: Eliminar las notas (van a la papelera) ::: 删除笔记（移入回收站）
Scegli la cartella… ::: Choose a folder… ::: Elegir carpeta… ::: 选择文件夹…
Tag ::: Tags ::: Etiquetas ::: 标签
Nuovo tag ::: New tag ::: Nueva etiqueta ::: 新建标签
Nessun tag. I tag organizzano le note in modo trasversale alle cartelle. ::: No tags. Tags organize notes across folders. ::: Sin etiquetas. Las etiquetas organizan las notas de forma transversal a las carpetas. ::: 还没有标签。标签可以跨文件夹整理笔记。
Modifica tag ::: Edit tag ::: Editar etiqueta ::: 编辑标签
Eliminare il tag? ::: Delete the tag? ::: ¿Eliminar la etiqueta? ::: 删除此标签？
“<1>” verrà rimosso da tutte le note. Le note non vengono eliminate. ::: “<1>” will be removed from all notes. The notes are not deleted. ::: “<1>” se quitará de todas las notas. Las notas no se eliminan. ::: “<1>”将从所有笔记中移除。笔记不会被删除。
Cestino ::: Trash ::: Papelera ::: 回收站
Cartella ::: Folder ::: Carpeta ::: 文件夹
Menu ::: Menu ::: Menú ::: 菜单
Ordina ::: Sort ::: Ordenar ::: 排序
Nuova nota ::: New note ::: Nueva nota ::: 新建笔记
Cerca in note, tag e cartelle ::: Search notes, tags and folders ::: Buscar en notas, etiquetas y carpetas ::: 搜索笔记、标签和文件夹
Eliminate dopo {1} giorni ::: Deleted after {1} days ::: Se eliminan tras {1} días ::: {1} 天后删除
Nessuna eliminazione automatica ::: No automatic deletion ::: Sin eliminación automática ::: 不自动删除
Svuota cestino ::: Empty trash ::: Vaciar papelera ::: 清空回收站
Altre note ::: Other notes ::: Otras notas ::: 其他笔记
Sposta in cartella ::: Move to folder ::: Mover a carpeta ::: 移动到文件夹
Eliminare definitivamente? ::: Delete permanently? ::: ¿Eliminar definitivamente? ::: 永久删除？
La nota verrà rimossa dal database e non potrà essere recuperata. ::: The note will be removed from the database and cannot be recovered. ::: La nota se quitará de la base de datos y no se podrá recuperar. ::: 笔记将从数据库中移除，且无法恢复。
Svuotare il cestino? ::: Empty the trash? ::: ¿Vaciar la papelera? ::: 清空回收站？
Tutte le note nel cestino verranno eliminate definitivamente. ::: All notes in the trash will be permanently deleted. ::: Todas las notas de la papelera se eliminarán definitivamente. ::: 回收站中的所有笔记都将被永久删除。
Svuota ::: Empty ::: Vaciar ::: 清空
Nessuna cartella ::: No folders ::: Sin carpetas ::: 没有文件夹
Nessun tag ::: No tags ::: Sin etiquetas ::: 没有标签
Impostazioni ::: Settings ::: Ajustes ::: 设置
Gestisci ::: Manage ::: Gestionar ::: 管理
Nessun risultato ::: No results ::: Sin resultados ::: 没有结果
Il cestino è vuoto ::: The trash is empty ::: La papelera está vacía ::: 回收站是空的
Nessuna nota archiviata ::: No archived notes ::: Sin notas archivadas ::: 没有已归档的笔记
Nessun preferito ::: No favorites ::: Sin favoritos ::: 没有收藏
Nessuna nota fissata ::: No pinned notes ::: Sin notas fijadas ::: 没有置顶笔记
Nessuna nota ::: No notes ::: Sin notas ::: 没有笔记
Tocca “Nuova nota” per iniziare. Tutto resta cifrato su questo dispositivo. ::: Tap “New note” to start. Everything stays encrypted on this device. ::: Toca “Nueva nota” para empezar. Todo permanece cifrado en este dispositivo. ::: 点击“新建笔记”开始。所有内容都加密保存在此设备上。
Senza titolo ::: Untitled ::: Sin título ::: 无标题
Preferita ::: Favorite ::: Favorita ::: 已收藏
Ripristina ::: Restore ::: Restaurar ::: 恢复
Elimina definitivamente ::: Delete permanently ::: Eliminar definitivamente ::: 永久删除
Rimuovi da fissate ::: Unpin ::: Quitar de fijadas ::: 取消置顶
Fissa in alto ::: Pin to top ::: Fijar arriba ::: 置顶
Rimuovi dai preferiti ::: Remove from favorites ::: Quitar de favoritos ::: 取消收藏
Aggiungi ai preferiti ::: Add to favorites ::: Añadir a favoritos ::: 加入收藏
Ripristina dall'archivio ::: Restore from archive ::: Restaurar del archivo ::: 从归档中恢复
Archivia ::: Archive ::: Archivar ::: 归档
Sposta nel cestino ::: Move to trash ::: Mover a la papelera ::: 移入回收站
adesso ::: just now ::: ahora ::: 刚刚
{1} min fa ::: {1} min ago ::: hace {1} min ::: {1} 分钟前
{1} h fa ::: {1} h ago ::: hace {1} h ::: {1} 小时前
ieri ::: yesterday ::: ayer ::: 昨天
{1} giorni fa ::: {1} days ago ::: hace {1} días ::: {1} 天前
Appunti di riunione ::: Meeting notes ::: Notas de reunión ::: 会议记录
Lista della spesa ::: Shopping list ::: Lista de la compra ::: 购物清单
Giornata ::: Day plan ::: Jornada ::: 每日计划
Diario ::: Journal ::: Diario ::: 日记
Progetto ::: Project ::: Proyecto ::: 项目
Fatto ::: Done ::: Hecho ::: 完成
Da fare ::: To do ::: Por hacer ::: 待办
Monocromatico ::: Monochrome ::: Monocromo ::: 单色
Grigio ::: Gray ::: Gris ::: 灰色
Azzurro ::: Light blue ::: Azul claro ::: 浅蓝
Verde ::: Green ::: Verde ::: 绿色
Rosso ::: Red ::: Rojo ::: 红色
Ambra ::: Amber ::: Ámbar ::: 琥珀
Viola ::: Purple ::: Violeta ::: 紫色
Nota ::: Note ::: Nota ::: 笔记
Nota esportata. ::: Note exported. ::: Nota exportada. ::: 笔记已导出。
Esportazione non riuscita. ::: Export failed. ::: Falló la exportación. ::: 导出失败。
PDF creato. ::: PDF created. ::: PDF creado. ::: PDF 已创建。
Creazione del PDF non riuscita. ::: PDF creation failed. ::: Falló la creación del PDF. ::: 创建 PDF 失败。
Nel cestino ::: In the trash ::: En la papelera ::: 在回收站中
Fissa ::: Pin ::: Fijar ::: 置顶
Copia testo ::: Copy text ::: Copiar texto ::: 复制文本
Copiato. Gli appunti si svuotano dopo 60 secondi. ::: Copied. The clipboard is cleared after 60 seconds. ::: Copiado. El portapapeles se vacía tras 60 segundos. ::: 已复制。剪贴板将在 60 秒后清空。
Copiato. ::: Copied. ::: Copiado. ::: 已复制。
Cerca nella nota ::: Find in note ::: Buscar en la nota ::: 在笔记中查找
Condividi testo ::: Share text ::: Compartir texto ::: 分享文本
Condividi nota ::: Share note ::: Compartir nota ::: 分享笔记
Esporta come Markdown (.md) ::: Export as Markdown (.md) ::: Exportar como Markdown (.md) ::: 导出为 Markdown (.md)
Esporta come PDF ::: Export as PDF ::: Exportar como PDF ::: 导出为 PDF
Gestisci tag ::: Manage tags ::: Gestionar etiquetas ::: 管理标签
Ripeti ::: Redo ::: Rehacer ::: 重做
Stile del testo ::: Text style ::: Estilo del texto ::: 文本样式
Testo normale ::: Normal text ::: Texto normal ::: 正文
Titolo 1 ::: Heading 1 ::: Título 1 ::: 标题 1
Titolo 2 ::: Heading 2 ::: Título 2 ::: 标题 2
Titolo 3 ::: Heading 3 ::: Título 3 ::: 标题 3
Grassetto ::: Bold ::: Negrita ::: 粗体
Corsivo ::: Italic ::: Cursiva ::: 斜体
Barrato ::: Strikethrough ::: Tachado ::: 删除线
Evidenzia ::: Highlight ::: Resaltar ::: 高亮
Codice ::: Code ::: Código ::: 代码
Elenco puntato ::: Bulleted list ::: Lista con viñetas ::: 项目符号列表
Elenco numerato ::: Numbered list ::: Lista numerada ::: 编号列表
Lista di controllo ::: Checklist ::: Lista de tareas ::: 清单
Spunta la casella ::: Tick the box ::: Marcar la casilla ::: 勾选复选框
Citazione ::: Quote ::: Cita ::: 引用
Aumenta il rientro ::: Increase indent ::: Aumentar sangría ::: 增加缩进
Riduci il rientro ::: Decrease indent ::: Reducir sangría ::: 减少缩进
Inserisci ::: Insert ::: Insertar ::: 插入
Data e ora ::: Date and time ::: Fecha y hora ::: 日期和时间
Linea separatrice ::: Divider line ::: Línea separadora ::: 分隔线
MODELLI ::: TEMPLATES ::: PLANTILLAS ::: 模板
{1} parole · {2} caratteri ::: {1} words · {2} characters ::: {1} palabras · {2} caracteres ::: {1} 个词 · {2} 个字符
+ Tag ::: + Tag ::: + Etiqueta ::: + 标签
Scrivi qui… ::: Write here… ::: Escribe aquí… ::: 在此书写…
Cartella della nota ::: Note folder ::: Carpeta de la nota ::: 笔记所在文件夹
Aggiungi ::: Add ::: Añadir ::: 添加
Precedente ::: Previous ::: Anterior ::: 上一个
Successivo ::: Next ::: Siguiente ::: 下一个
Chiudi la ricerca ::: Close search ::: Cerrar la búsqueda ::: 关闭搜索
Sostituisci con ::: Replace with ::: Reemplazar con ::: 替换为
Sostituisci ::: Replace ::: Reemplazar ::: 替换
Tutte ::: All ::: Todas ::: 全部
Sicurezza ::: Security ::: Seguridad ::: 安全
Password, PIN, biometria, blocco automatico ::: Password, PIN, biometrics, auto-lock ::: Contraseña, PIN, biometría, bloqueo automático ::: 密码、PIN、生物识别、自动锁定
Privacy ::: Privacy ::: Privacidad ::: 隐私
Anteprime, appunti, informativa ::: Previews, clipboard, privacy notice ::: Vistas previas, portapapeles, aviso de privacidad ::: 预览、剪贴板、隐私声明
Backup ::: Backup ::: Copia de seguridad ::: 备份
Crea e ripristina backup cifrati ::: Create and restore encrypted backups ::: Crear y restaurar copias cifradas ::: 创建和恢复加密备份
Tema e colori ::: Theme and colors ::: Tema y colores ::: 主题与颜色
Tema, accento, carattere, angoli ::: Theme, accent, font, corners ::: Tema, acento, fuente, esquinas ::: 主题、强调色、字体、圆角
Animazioni ::: Animations ::: Animaciones ::: 动画
Transizioni, velocità, stile ::: Transitions, speed, style ::: Transiciones, velocidad, estilo ::: 过渡、速度、样式
Schermata principale ::: Home screen ::: Pantalla principal ::: 主屏幕
Apertura, colonne, pulsante Nuova nota ::: Start section, columns, New note button ::: Apertura, columnas, botón Nueva nota ::: 启动页面、列数、“新建笔记”按钮
Elenco note ::: Note list ::: Lista de notas ::: 笔记列表
Schede, anteprime, data, tag ::: Cards, previews, date, tags ::: Tarjetas, vistas previas, fecha, etiquetas ::: 卡片、预览、日期、标签
Editor ::: Editor ::: Editor ::: 编辑器
Carattere, margini, salvataggio, tastiera ::: Font, margins, saving, keyboard ::: Fuente, márgenes, guardado, teclado ::: 字体、边距、保存、键盘
Gestione dati ::: Data management ::: Gestión de datos ::: 数据管理
Cestino ed eliminazione dei dati ::: Trash and data deletion ::: Papelera y eliminación de datos ::: 回收站与数据删除
Informazioni ::: About ::: Información ::: 关于
Versione e dettagli ::: Version and details ::: Versión y detalles ::: 版本与详情
Predefinito ::: Default ::: Predeterminado ::: 默认
Nero ::: Black ::: Negro ::: 黑色
Grafite ::: Graphite ::: Grafito ::: 石墨
Antracite ::: Anthracite ::: Antracita ::: 煤黑
Blu notte ::: Midnight blue ::: Azul noche ::: 午夜蓝
Verde bosco ::: Forest green ::: Verde bosque ::: 森林绿
Bordeaux ::: Burgundy ::: Burdeos ::: 酒红
Carta ::: Paper ::: Papel ::: 纸张
Grigio scuro ::: Dark gray ::: Gris oscuro ::: 深灰
Grigio medio ::: Medium gray ::: Gris medio ::: 中灰
Blu scuro ::: Dark blue ::: Azul oscuro ::: 深蓝
Verde scuro ::: Dark green ::: Verde oscuro ::: 深绿
Notium non ha permessi di rete: non può inviare né ricevere dati. ::: Notium has no network permissions: it cannot send or receive data. ::: Notium no tiene permisos de red: no puede enviar ni recibir datos. ::: Notium 没有网络权限：无法发送或接收数据。
• Nessun account, e-mail, numero di telefono o registrazione. ::: • No account, email, phone number or sign-up. ::: • Sin cuenta, correo, número de teléfono ni registro. ::: • 无需账号、邮箱、手机号或注册。
• Nessuna pubblicità, statistica, diagnostica o analisi dell'uso. ::: • No ads, statistics, diagnostics or usage analytics. ::: • Sin publicidad, estadísticas, diagnósticos ni análisis de uso. ::: • 没有广告、统计、诊断或使用分析。
• Nessun accesso a posizione, contatti o altre app. ::: • No access to location, contacts or other apps. ::: • Sin acceso a la ubicación, los contactos ni otras aplicaciones. ::: • 不访问位置、联系人或其他应用。
• Le note sono salvate in un database cifrato (SQLCipher, AES-256) sul tuo dispositivo. ::: • Notes are stored in an encrypted database (SQLCipher, AES-256) on your device. ::: • Las notas se guardan en una base de datos cifrada (SQLCipher, AES-256) en tu dispositivo. ::: • 笔记保存在你设备上的加密数据库中（SQLCipher，AES-256）。
• La chiave del database è casuale e protetta dalla tua password (Argon2id), dal PIN e dall'Android Keystore. ::: • The database key is random and protected by your password (Argon2id), the PIN and the Android Keystore. ::: • La clave de la base de datos es aleatoria y está protegida por tu contraseña (Argon2id), el PIN y el Android Keystore. ::: • 数据库密钥是随机生成的，由你的密码（Argon2id）、PIN 和 Android Keystore 共同保护。
• Nessun backup automatico Android o cloud. I backup sono file cifrati che crei e sposti tu. ::: • No automatic Android or cloud backups. Backups are encrypted files that you create and move yourself. ::: • Sin copias automáticas de Android ni en la nube. Las copias son archivos cifrados que creas y mueves tú. ::: • 没有 Android 自动备份或云备份。备份是你自己创建和移动的加密文件。
• Nessun dato personale nei log e nessun contenuto nelle notifiche. ::: • No personal data in logs and no content in notifications. ::: • Sin datos personales en los registros ni contenido en las notificaciones. ::: • 日志中没有个人数据，通知中没有笔记内容。
Disinstallando l'app, Android rimuove i dati privati. I backup che hai copiato altrove restano dove li hai salvati. ::: When you uninstall the app, Android removes its private data. Backups you copied elsewhere stay where you saved them. ::: Al desinstalar la app, Android elimina sus datos privados. Las copias que guardaste en otro lugar permanecen donde las dejaste. ::: 卸载应用后，Android 会删除其私有数据。你复制到别处的备份仍保留在原处。
Ripristina predefiniti ::: Restore defaults ::: Restaurar valores predeterminados ::: 恢复默认设置
Cambia password principale ::: Change main password ::: Cambiar contraseña principal ::: 更改主密码
Il database non viene ricifrato: cambia solo la protezione della chiave. ::: The database is not re-encrypted: only the key protection changes. ::: La base de datos no se vuelve a cifrar: solo cambia la protección de la clave. ::: 数据库不会重新加密：只更改密钥的保护方式。
Modifica PIN ::: Change PIN ::: Cambiar PIN ::: 修改 PIN
Sblocco rapido, minimo 6 cifre ::: Quick unlock, at least 6 digits ::: Desbloqueo rápido, mínimo 6 dígitos ::: 快速解锁，至少 6 位数字
Disattiva PIN ::: Disable PIN ::: Desactivar PIN ::: 停用 PIN
Attiva PIN ::: Enable PIN ::: Activar PIN ::: 启用 PIN
Sblocco rapido. Dopo 5 errori scatta un blocco temporaneo. ::: Quick unlock. After 5 mistakes a temporary lock applies. ::: Desbloqueo rápido. Tras 5 errores se aplica un bloqueo temporal. ::: 快速解锁。错误 5 次后会临时锁定。
Sblocca la chiave con la biometria del dispositivo ::: Unlock the key with the device biometrics ::: Desbloquea la clave con la biometría del dispositivo ::: 使用设备生物识别解锁密钥
Non disponibile su questo dispositivo ::: Not available on this device ::: No disponible en este dispositivo ::: 此设备不支持
Attiva l'impronta digitale ::: Enable fingerprint ::: Activar la huella digital ::: 启用指纹
Conferma per proteggere la chiave con la biometria ::: Confirm to protect the key with biometrics ::: Confirma para proteger la clave con la biometría ::: 确认以使用生物识别保护密钥
Blocco automatico ::: Auto-lock ::: Bloqueo automático ::: 自动锁定
Subito ::: Immediately ::: Inmediatamente ::: 立即
Dopo 30 secondi ::: After 30 seconds ::: Tras 30 segundos ::: 30 秒后
Dopo 1 minuto ::: After 1 minute ::: Tras 1 minuto ::: 1 分钟后
Dopo 5 minuti ::: After 5 minutes ::: Tras 5 minutos ::: 5 分钟后
Dopo 15 minuti ::: After 15 minutes ::: Tras 15 minutos ::: 15 分钟后
Mai (finché l'app resta in memoria) ::: Never (while the app stays in memory) ::: Nunca (mientras la app siga en memoria) ::: 从不（只要应用仍在内存中）
Tastierino PIN casuale ::: Random PIN keypad ::: Teclado PIN aleatorio ::: 随机 PIN 键盘
Cambia la posizione dei numeri a ogni sblocco ::: Shuffles the digits at every unlock ::: Cambia la posición de los números en cada desbloqueo ::: 每次解锁都会打乱数字位置
Vibrazione sui tasti del PIN ::: Vibration on PIN keys ::: Vibración en las teclas del PIN ::: PIN 按键震动
Proteggi anteprime e schermate ::: Protect previews and screenshots ::: Proteger vistas previas y capturas ::: 保护预览和截图
Nasconde il contenuto nelle app recenti e blocca gli screenshot ::: Hides the content in recent apps and blocks screenshots ::: Oculta el contenido en las apps recientes y bloquea las capturas ::: 在最近任务中隐藏内容并禁止截图
Svuota gli appunti dopo 60 secondi ::: Clear the clipboard after 60 seconds ::: Vaciar el portapapeles tras 60 segundos ::: 60 秒后清空剪贴板
Vale per il testo copiato da Notium ::: Applies to text copied from Notium ::: Se aplica al texto copiado desde Notium ::: 适用于从 Notium 复制的文本
Informativa sulla privacy ::: Privacy notice ::: Aviso de privacidad ::: 隐私声明
Cosa fa e cosa non fa Notium con i tuoi dati ::: What Notium does and does not do with your data ::: Qué hace y qué no hace Notium con tus datos ::: Notium 如何处理（以及不处理）你的数据
Crea backup cifrato ::: Create encrypted backup ::: Crear copia cifrada ::: 创建加密备份
File .notium protetto da una password indipendente (AES-256-GCM + Argon2id) ::: .notium file protected by an independent password (AES-256-GCM + Argon2id) ::: Archivo .notium protegido por una contraseña independiente (AES-256-GCM + Argon2id) ::: 由独立密码保护的 .notium 文件（AES-256-GCM + Argon2id）
Ripristina da backup ::: Restore from backup ::: Restaurar desde copia ::: 从备份恢复
Sostituisce tutte le note con quelle del file. Prima verifica integrità e firma. ::: Replaces all notes with those in the file. Integrity and signature are checked first. ::: Sustituye todas las notas por las del archivo. Antes se verifican la integridad y la firma. ::: 用文件中的笔记替换所有笔记。会先验证完整性和签名。
Tema ::: Theme ::: Tema ::: 主题
Colore d'accento ::: Accent color ::: Color de acento ::: 强调色
Carattere dell'interfaccia ::: Interface font ::: Fuente de la interfaz ::: 界面字体
Dimensione del testo ::: Text size ::: Tamaño del texto ::: 文字大小
Angoli arrotondati ::: Rounded corners ::: Esquinas redondeadas ::: 圆角
Alto contrasto ::: High contrast ::: Alto contraste ::: 高对比度
Testi e bordi più marcati, per leggere meglio ::: Stronger text and borders, easier to read ::: Textos y bordes más marcados, para leer mejor ::: 文字和边框更醒目，阅读更清晰
Colore di sfondo ::: Background color ::: Color de fondo ::: 背景颜色
Colore delle schede ::: Card color ::: Color de las tarjetas ::: 卡片颜色
Transizioni tra schermate, elenco note, pulsanti e menu ::: Transitions between screens, note list, buttons and menus ::: Transiciones entre pantallas, lista de notas, botones y menús ::: 屏幕切换、笔记列表、按钮和菜单的过渡效果
Movimento dell'elenco ::: List motion ::: Movimiento de la lista ::: 列表动效
Le schede scorrono e compaiono con dolcezza quando la lista cambia ::: Cards slide and appear smoothly when the list changes ::: Las tarjetas se deslizan y aparecen con suavidad cuando la lista cambia ::: 列表变化时，卡片会平滑地滑动和出现
Effetto pressione ::: Press effect ::: Efecto de pulsación ::: 按压效果
Le schede si rimpiccioliscono leggermente quando le tocchi ::: Cards shrink slightly when you touch them ::: Las tarjetas se encogen ligeramente al tocarlas ::: 触摸卡片时会略微缩小
Intensità dell'effetto pressione ::: Press effect strength ::: Intensidad del efecto de pulsación ::: 按压效果强度
Stile delle transizioni ::: Transition style ::: Estilo de las transiciones ::: 过渡样式
Velocità delle animazioni ::: Animation speed ::: Velocidad de las animaciones ::: 动画速度
Sezione all'apertura ::: Start section ::: Sección al abrir ::: 启动时显示的分区
Colonne ::: Columns ::: Columnas ::: 列数
Nome nella barra superiore ::: Name in the top bar ::: Nombre en la barra superior ::: 顶部栏名称
Margini laterali dell'elenco ::: List side margins ::: Márgenes laterales de la lista ::: 列表左右边距
Barra di ricerca ::: Search bar ::: Barra de búsqueda ::: 搜索栏
Cartelle nel menu laterale ::: Folders in the side menu ::: Carpetas en el menú lateral ::: 侧边菜单中的文件夹
Tag nel menu laterale ::: Tags in the side menu ::: Etiquetas en el menú lateral ::: 侧边菜单中的标签
Logo nella barra superiore ::: Logo in the top bar ::: Logotipo en la barra superior ::: 顶部栏徽标
Pulsante Nuova nota ::: New note button ::: Botón Nueva nota ::: “新建笔记”按钮
Posizione del pulsante ::: Button position ::: Posición del botón ::: 按钮位置
Densità ::: Density ::: Densidad ::: 密度
Stile delle schede ::: Card style ::: Estilo de las tarjetas ::: 卡片样式
Ordinamento predefinito ::: Default sort order ::: Orden predeterminado ::: 默认排序
Righe del titolo ::: Title lines ::: Líneas del título ::: 标题行数
Dimensione del titolo ::: Title size ::: Tamaño del título ::: 标题大小
Spaziatura interna delle schede ::: Card inner spacing ::: Espaciado interno de las tarjetas ::: 卡片内边距
Distanza tra le schede ::: Space between cards ::: Distancia entre tarjetas ::: 卡片间距
Icona di nota fissata ::: Pinned note icon ::: Icono de nota fijada ::: 置顶图标
Icona di nota preferita ::: Favorite note icon ::: Icono de nota favorita ::: 收藏图标
Titoli in grassetto ::: Bold titles ::: Títulos en negrita ::: 标题加粗
Spessore dei bordi ::: Border thickness ::: Grosor de los bordes ::: 边框粗细
Formato della data ::: Date format ::: Formato de fecha ::: 日期格式
Mostra la data ::: Show the date ::: Mostrar la fecha ::: 显示日期
Mostra i tag ::: Show tags ::: Mostrar etiquetas ::: 显示标签
Mostra la cartella ::: Show the folder ::: Mostrar la carpeta ::: 显示文件夹
Anteprima: così apparirà il testo delle tue note mentre scrivi. ::: Preview: this is how your note text will look as you write. ::: Vista previa: así se verá el texto de tus notas mientras escribes. ::: 预览：写笔记时文字将显示成这样。
Carattere ::: Font ::: Fuente ::: 字体
Interlinea ::: Line spacing ::: Interlineado ::: 行距
Spaziatura tra le lettere ::: Letter spacing ::: Espaciado entre letras ::: 字间距
Titolo in grassetto ::: Bold title ::: Título en negrita ::: 标题加粗
Margini laterali ::: Side margins ::: Márgenes laterales ::: 左右边距
Salvataggio automatico ::: Auto-save ::: Guardado automático ::: 自动保存
dopo {1} s ::: after {1} s ::: tras {1} s ::: {1} 秒后
Correzione automatica ::: Auto-correct ::: Corrección automática ::: 自动更正
Maiuscola a inizio frase ::: Capitalize sentences ::: Mayúscula al inicio de frase ::: 句首大写
Mantieni lo schermo acceso mentre scrivi ::: Keep the screen on while writing ::: Mantener la pantalla encendida al escribir ::: 书写时保持屏幕常亮
Continua gli elenchi con Invio ::: Continue lists with Enter ::: Continuar las listas con Intro ::: 按回车继续列表
Elenchi puntati, numerati e caselle si proseguono da soli ::: Bulleted, numbered and checkbox lists continue on their own ::: Las listas con viñetas, numeradas y de casillas continúan solas ::: 项目符号、编号和复选框列表会自动延续
Barra di formattazione ::: Formatting bar ::: Barra de formato ::: 格式工具栏
Grassetto, corsivo, titoli, elenchi, caselle ::: Bold, italic, headings, lists, checkboxes ::: Negrita, cursiva, títulos, listas, casillas ::: 粗体、斜体、标题、列表、复选框
Contatore parole e caratteri ::: Word and character counter ::: Contador de palabras y caracteres ::: 字数统计
Svuotamento automatico cestino ::: Auto-empty trash ::: Vaciado automático de la papelera ::: 自动清空回收站
Mai ::: Never ::: Nunca ::: 从不
Dopo {1} giorni ::: After {1} days ::: Tras {1} días ::: {1} 天后
Dopo 7 giorni ::: After 7 days ::: Tras 7 días ::: 7 天后
Dopo 30 giorni ::: After 30 days ::: Tras 30 días ::: 30 天后
Dopo 90 giorni ::: After 90 days ::: Tras 90 días ::: 90 天后
Svuota il cestino ora ::: Empty the trash now ::: Vaciar la papelera ahora ::: 立即清空回收站
Elimina tutti i dati ::: Delete all data ::: Eliminar todos los datos ::: 删除所有数据
Cancella note, chiavi e preferenze da questo dispositivo ::: Erases notes, keys and preferences from this device ::: Borra notas, claves y preferencias de este dispositivo ::: 从此设备清除笔记、密钥和偏好设置
Notium 1.0.0 ::: Notium 1.0.0 ::: Notium 1.0.0 ::: Notium 1.0.0
Your notes. Your privacy. · 100% offline · Android 8.0+ ::: Your notes. Your privacy. · 100% offline · Android 8.0+ ::: Tus notas. Tu privacidad. · 100 % sin conexión · Android 8.0+ ::: 你的笔记，你的隐私。· 100% 离线 · Android 8.0+
Riporta solo le opzioni di «<1>» ai valori iniziali ::: Resets only the options of “<1>” to their initial values ::: Restablece solo las opciones de «<1>» a sus valores iniciales ::: 仅将“<1>”的选项恢复为初始值
Disattivare il PIN? ::: Disable the PIN? ::: ¿Desactivar el PIN? ::: 停用 PIN？
Potrai sbloccare Notium con la password principale. ::: You will be able to unlock Notium with the main password. ::: Podrás desbloquear Notium con la contraseña principal. ::: 你仍可使用主密码解锁 Notium。
Disattiva ::: Disable ::: Desactivar ::: 停用
Password del backup ::: Backup password ::: Contraseña de la copia ::: 备份密码
Scegli una password per questo backup. È indipendente da quella dell'app: senza di essa il file non si può aprire. ::: Choose a password for this backup. It is independent from the app password: without it the file cannot be opened. ::: Elige una contraseña para esta copia. Es independiente de la de la app: sin ella el archivo no se puede abrir. ::: 为此备份设置一个密码。它与应用密码相互独立：没有它就无法打开该文件。
Continua ::: Continue ::: Continuar ::: 继续
Inserisci la password usata quando hai creato il backup. ::: Enter the password you used when you created the backup. ::: Introduce la contraseña que usaste al crear la copia. ::: 请输入创建备份时使用的密码。
Verifica ::: Verify ::: Verificar ::: 验证
Le note nel cestino verranno eliminate definitivamente. ::: The notes in the trash will be permanently deleted. ::: Las notas de la papelera se eliminarán definitivamente. ::: 回收站中的笔记将被永久删除。
Ripristinare «<1>»? ::: Restore “<1>”? ::: ¿Restablecer «<1>»? ::: 恢复“<1>”？
Le opzioni di questa categoria tornano ai valori predefiniti. Note e dati non vengono toccati. ::: The options in this category go back to their defaults. Notes and data are not touched. ::: Las opciones de esta categoría vuelven a sus valores predeterminados. Las notas y los datos no se tocan. ::: 此分类的选项将恢复为默认值。笔记和数据不受影响。
Chiudi ::: Close ::: Cerrar ::: 关闭
Backup verificato ::: Backup verified ::: Copia verificada ::: 备份已验证
Integrità e autenticità confermate. ::: Integrity and authenticity confirmed. ::: Integridad y autenticidad confirmadas. ::: 完整性和真实性已确认。
Contiene {1} note, {2} cartelle e {3} tag (creato il <4>). ::: Contains {1} notes, {2} folders and {3} tags (created on <4>). ::: Contiene {1} notas, {2} carpetas y {3} etiquetas (creada el <4>). ::: 包含 {1} 条笔记、{2} 个文件夹和 {3} 个标签（创建于 <4>）。
Il ripristino SOSTITUISCE tutte le note attuali. Se qualcosa va storto, i dati attuali restano invariati. ::: Restoring REPLACES all current notes. If something goes wrong, the current data stays unchanged. ::: La restauración SUSTITUYE todas las notas actuales. Si algo sale mal, los datos actuales no cambian. ::: 恢复会替换当前所有笔记。如果出错，当前数据保持不变。
Anteprima del testo ::: Text preview ::: Vista previa del texto ::: 文字预览
Colore personalizzato ::: Custom color ::: Color personalizado ::: 自定义颜色
Inserisci un colore esadecimale a 6 cifre (ad esempio 7FA6D9). ::: Enter a 6-digit hexadecimal color (for example 7FA6D9). ::: Introduce un color hexadecimal de 6 cifras (por ejemplo 7FA6D9). ::: 请输入 6 位十六进制颜色（例如 7FA6D9）。
Applica ::: Apply ::: Aplicar ::: 应用
Personalizzato ::: Custom ::: Personalizado ::: 自定义
Cambia password ::: Change password ::: Cambiar contraseña ::: 更改密码
Password attuale ::: Current password ::: Contraseña actual ::: 当前密码
Nuova password ::: New password ::: Nueva contraseña ::: 新密码
Conferma nuova password ::: Confirm new password ::: Confirmar nueva contraseña ::: 确认新密码
Cambia ::: Change ::: Cambiar ::: 更改
Password ::: Password ::: Contraseña ::: 密码
PIN di sblocco rapido ::: Quick unlock PIN ::: PIN de desbloqueo rápido ::: 快速解锁 PIN
Da 6 a 12 cifre. Non può sostituire la password: serve solo a sbloccare più in fretta. ::: 6 to 12 digits. It cannot replace the password: it only unlocks faster. ::: De 6 a 12 dígitos. No sustituye a la contraseña: solo sirve para desbloquear más rápido. ::: 6 至 12 位数字。它不能代替密码，只是让解锁更快。
PIN ::: PIN ::: PIN ::: PIN
Conferma PIN ::: Confirm PIN ::: Confirmar PIN ::: 确认 PIN
Il PIN deve avere almeno 6 cifre. ::: The PIN must have at least 6 digits. ::: El PIN debe tener al menos 6 dígitos. ::: PIN 至少需要 6 位数字。
PIN troppo semplice (cifre uguali o in sequenza). ::: PIN too simple (repeated or sequential digits). ::: PIN demasiado simple (cifras repetidas o consecutivas). ::: PIN 太简单（数字重复或连续）。
I due PIN non coincidono. ::: The two PINs do not match. ::: Los dos PIN no coinciden. ::: 两次输入的 PIN 不一致。
Attiva ::: Enable ::: Activar ::: 启用
Eliminare tutti i dati? ::: Delete all data? ::: ¿Eliminar todos los datos? ::: 删除所有数据？
Verranno cancellati database, chiavi e preferenze di questo dispositivo. Non è reversibile. Non viene garantita la cancellazione forense delle memorie flash né dei backup che hai copiato altrove. ::: The database, keys and preferences on this device will be erased. This cannot be undone. Forensic erasure of flash memory and of backups you copied elsewhere is not guaranteed. ::: Se borrarán la base de datos, las claves y las preferencias de este dispositivo. No es reversible. No se garantiza el borrado forense de la memoria flash ni de las copias que guardaste en otro lugar. ::: 此设备上的数据库、密钥和偏好设置将被清除，且无法撤销。不保证对闪存及你复制到别处的备份进行取证级清除。
Scrivi ELIMINA per confermare ::: Type DELETE to confirm ::: Escribe ELIMINAR para confirmar ::: 输入“删除”以确认
Elimina tutto ::: Delete everything ::: Eliminar todo ::: 全部删除
Fissata ::: Pinned ::: Fijada ::: 已置顶
Senza PIN né impronta digitale l'app si apre direttamente, senza chiedere la password. Attiva uno dei due per proteggere l'accesso. ::: Without a PIN or fingerprint the app opens directly, without asking for the password. Enable one of them to protect access. ::: Sin PIN ni huella digital, la app se abre directamente, sin pedir la contraseña. Activa uno de los dos para proteger el acceso. ::: 未设置 PIN 或指纹时，应用会直接打开，不会要求输入密码。启用其中之一即可保护访问。
Esporta ::: Export ::: Exportar ::: 导出
Esporta in PDF ::: Export to PDF ::: Exportar a PDF ::: 导出为 PDF
Esporta in TXT ::: Export to TXT ::: Exportar a TXT ::: 导出为 TXT
Esporta in JSON ::: Export to JSON ::: Exportar a JSON ::: 导出为 JSON
Salvato in Download: <1> ::: Saved to Downloads: <1> ::: Guardado en Descargas: <1> ::: 已保存到“下载”：<1>
Permesso negato: impossibile salvare in Download. ::: Permission denied: unable to save to Downloads. ::: Permiso denegado: no se puede guardar en Descargas. ::: 权限被拒绝：无法保存到“下载”。
Scegli la lingua dell'app ::: Choose the app language ::: Elige el idioma de la aplicación ::: 选择应用语言
Seleziona ::: Select ::: Seleccionar ::: 选择
Seleziona tutto ::: Select all ::: Seleccionar todo ::: 全选
Deseleziona tutto ::: Deselect all ::: Deseleccionar todo ::: 取消全选
Annulla selezione ::: Cancel selection ::: Cancelar selección ::: 取消选择
1 selezionata ::: 1 selected ::: 1 seleccionada ::: 已选 1 项
{1} selezionate ::: {1} selected ::: {1} seleccionadas ::: 已选 {1} 项
Aggiunta ai preferiti. ::: Added to favorites. ::: Añadida a favoritos. ::: 已加入收藏。
Rimossa dai preferiti. ::: Removed from favorites. ::: Quitada de favoritos. ::: 已取消收藏。
{1} note aggiunte ai preferiti. ::: {1} notes added to favorites. ::: {1} notas añadidas a favoritos. ::: {1} 条笔记已加入收藏。
{1} note rimosse dai preferiti. ::: {1} notes removed from favorites. ::: {1} notas quitadas de favoritos. ::: {1} 条笔记已取消收藏。
{1} note archiviate. ::: {1} notes archived. ::: {1} notas archivadas. ::: {1} 条笔记已归档。
{1} note ripristinate dall'archivio. ::: {1} notes restored from archive. ::: {1} notas restauradas del archivo. ::: {1} 条笔记已从归档中恢复。
{1} note spostate nel cestino. ::: {1} notes moved to trash. ::: {1} notas movidas a la papelera. ::: {1} 条笔记已移入回收站。
Salvati {1} file in Download. ::: Saved {1} files to Downloads. ::: {1} archivos guardados en Descargas. ::: 已将 {1} 个文件保存到“下载”。
Importa backup ::: Import backup ::: Importar copia ::: 导入备份
Importa un backup ::: Import a backup ::: Importar una copia de seguridad ::: 导入备份
Crea e importa ::: Create and import ::: Crear e importar ::: 创建并导入
Rimuovi il backup ::: Remove the backup ::: Quitar la copia ::: 移除备份
Backup verificato: contiene {1} note, {2} cartelle e {3} tag. Scegli la password principale per importarlo. ::: Backup verified: it contains {1} notes, {2} folders and {3} tags. Choose the main password to import it. ::: Copia verificada: contiene {1} notas, {2} carpetas y {3} etiquetas. Elige la contraseña principal para importarla. ::: 备份已验证：包含 {1} 条笔记、{2} 个文件夹和 {3} 个标签。请设置主密码以导入。
Backup importato: {1} note. ::: Backup imported: {1} notes. ::: Copia importada: {1} notas. ::: 备份已导入：{1} 条笔记。
Importazione del backup non riuscita. ::: Backup import failed. ::: No se pudo importar la copia. ::: 备份导入失败。
oppure ::: or ::: o ::: 或
Backup pronto ::: Backup ready ::: Copia lista ::: 备份就绪
{1} note, {2} cartelle, {3} tag. Scegli la password principale per importarle. ::: {1} notes, {2} folders, {3} tags. Choose the main password to import them. ::: {1} notas, {2} carpetas, {3} etiquetas. Elige la contraseña principal para importarlas. ::: {1} 条笔记、{2} 个文件夹、{3} 个标签。请设置主密码以导入。
Scegli un altro file ::: Choose another file ::: Elegir otro archivo ::: 选择其他文件
Le tue note sono qui ::: Your notes are here ::: Tus notas están aquí ::: 你的笔记都在这里
Importate {1} note, {2} cartelle e {3} tag dal backup. ::: Imported {1} notes, {2} folders and {3} tags from the backup. ::: Se importaron {1} notas, {2} carpetas y {3} etiquetas de la copia. ::: 已从备份导入 {1} 条笔记、{2} 个文件夹和 {3} 个标签。
"""
}
