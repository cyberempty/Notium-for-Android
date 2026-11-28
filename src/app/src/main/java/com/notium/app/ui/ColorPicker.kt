package com.notium.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** Selettore di colore a palette (quadrato saturazione/luminosità + cursore tonalità) con i valori numerici. */
@Composable
internal fun ColorPickerDialog(initial: Int?, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val start = FloatArray(3).also { android.graphics.Color.colorToHSV(initial ?: 0xFF7FA6D9.toInt(), it) }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2]) }
    var hex by remember { mutableStateOf("") }
    var editingHex by remember { mutableStateOf(false) }

    val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    val hexShown = if (editingHex) hex else String.format("%02X%02X%02X", r, g, b)

    fun setFromArgb(c: Int) {
        val f = FloatArray(3); android.graphics.Color.colorToHSV(c, f)
        hue = f[0]; sat = f[1]; value = f[2]
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Colore personalizzato") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Quadrato saturazione (X) / luminosità (Y)
                var sq by remember { mutableStateOf(Size.Zero) }
                fun pick(o: Offset) {
                    if (sq.width <= 0f) return
                    sat = (o.x / sq.width).coerceIn(0f, 1f)
                    value = 1f - (o.y / sq.height).coerceIn(0f, 1f)
                    editingHex = false
                }
                Canvas(
                    Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(10.dp))
                        .pointerInput(Unit) { detectTapGestures { pick(it) } }
                        .pointerInput(Unit) { detectDragGestures(onDragStart = { pick(it) }) { ch, _ -> ch.consume(); pick(ch.position) } }
                ) {
                    sq = size
                    val pure = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                    drawRect(Brush.horizontalGradient(listOf(Color.White, pure)))
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    val c = Offset(sat * size.width, (1f - value) * size.height)
                    drawCircle(Color.White, 10.dp.toPx(), c, style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
                    drawCircle(Color.Black, 12.dp.toPx(), c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                }
                // Cursore tonalità
                var bar by remember { mutableStateOf(Size.Zero) }
                fun pickHue(x: Float) { if (bar.width > 0f) { hue = (x / bar.width).coerceIn(0f, 1f) * 359.99f; editingHex = false } }
                Canvas(
                    Modifier.fillMaxWidth().height(26.dp).clip(RoundedCornerShape(13.dp))
                        .pointerInput(Unit) { detectTapGestures { pickHue(it.x) } }
                        .pointerInput(Unit) { detectDragGestures(onDragStart = { pickHue(it.x) }) { ch, _ -> ch.consume(); pickHue(ch.position.x) } }
                ) {
                    bar = size
                    val stops = (0..6).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it * 60f % 360f, 1f, 1f))) }
                    drawRect(Brush.horizontalGradient(stops))
                    val x = hue / 359.99f * size.width
                    drawCircle(Color.White, 11.dp.toPx(), Offset(x, size.height / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
                }
                // Anteprima: precedente / nuovo
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Box(
                        Modifier.size(width = 56.dp, height = 40.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color(initial ?: argb)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    )
                    Text("→")
                    Box(
                        Modifier.size(width = 56.dp, height = 40.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color(argb)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    )
                    OutlinedTextField(
                        value = hexShown,
                        onValueChange = { v ->
                            val t = v.uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                            hex = t; editingHex = true
                            if (t.length == 6) setFromArgb(0xFF000000.toInt() or t.toInt(16))
                        },
                        label = { Text("#RRGGBB") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                }
                // Valori numerici
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Readout("R", "$r"); Readout("G", "$g"); Readout("B", "$b")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Readout("H", "${hue.toInt()}°"); Readout("S", "${(sat * 100).toInt()}%"); Readout("B", "${(value * 100).toInt()}%")
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(argb or 0xFF000000.toInt()) }) { Text("Applica") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
private fun Readout(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
