package com.dartvader.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFFF4F1EA)
private val Surf = Color(0xFFFFFFFF)
private val Line = Color(0xFF1E1814)   // encre
private val Ivory = Color(0xFF1E1814)  // texte
private val Brass = Color(0xFF2457E6)  // bleu
private val Red = Color(0xFFFF5A36)
private val Yellow = Color(0xFFFFD23F)
private val Muted = Color(0xFF6B6258)

private fun fmt(ts: Long) = SimpleDateFormat("d MMM yyyy HH:mm", Locale.FRANCE).format(Date(ts))
private fun mode(g: Game) = "${g.start}${if (g.dbl) " · double out" else ""}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bars = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(bars, bars)
        setContent { DartsApp(remember { Store(applicationContext) }) }
    }
}

@Composable
fun DartsApp(s: Store) {
    var tab by rememberSaveable { mutableStateOf(0) }
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Brass, onPrimary = Color.White, secondary = Red, onSecondary = Color.White,
            background = Bg, onBackground = Ivory, surface = Surf, onSurface = Ivory
        )
    ) {
        Surface(Modifier.fillMaxSize().systemBarsPadding(), color = Bg) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Logo()
                    Text("DartVader", fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f).padding(start = 10.dp))
                    TextButton({ tab = 0 }) { Text("Partie", color = if (tab == 0) Brass else Muted) }
                    TextButton({ tab = 1 }) { Text("Historique", color = if (tab == 1) Brass else Muted) }
                }
                Box(Modifier.fillMaxWidth().height(3.dp).background(Line))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                    val g = s.active
                    when {
                        tab == 1 -> HistoryScreen(s)
                        g == null -> SetupScreen(s)
                        else -> GameScreen(s, g)
                    }
                }
            }
        }
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, border: Color = Line, fill: Color = Surf, content: @Composable ColumnScope.() -> Unit) {
    val shape = RectangleShape
    Column(modifier.background(fill, shape).border(3.dp, border, shape).padding(12.dp), content = content)
}

@Composable
fun Label(t: String) = Text(t, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))

@Composable
fun Choice(t: String, sel: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) =
    Button(
        onClick, modifier,
        border = BorderStroke(3.dp, if (sel) Brass else Line),
        colors = ButtonDefaults.buttonColors(containerColor = if (sel) Brass else Surf, contentColor = if (sel) Color.White else Ivory)
    ) { Text(t, maxLines = 1) }

@Composable
fun SetupScreen(s: Store) {
    Geo()
    Text("NOUVELLE", fontSize = 40.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 12.dp))
    Text("PARTIE", fontSize = 40.sp, lineHeight = 40.sp, fontWeight = FontWeight.Light)
    if (s.paused.isNotEmpty()) {
        Label("Parties en pause")
        s.paused.forEach { p ->
            Panel(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("${fmt(p.ts)} · ${mode(p)}", color = Muted, fontSize = 13.sp)
                Text(p.names.indices.joinToString(" · ") { "${p.names[it]} ${p.rem(it)}" }, Modifier.padding(vertical = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ s.resume(p.id) }, Modifier.weight(1f)) { Text("Reprendre", maxLines = 1) }
                    OutlinedButton({ s.deletePaused(p.id) }, Modifier.weight(1f)) { Text("Supprimer", maxLines = 1) }
                }
            }
        }
    }
    Label("Mode de jeu")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(301, 501, 701).forEach { m -> Choice("$m", s.start == m, Modifier.weight(1f)) { s.start = m; s.save() } }
    }
    Label("Fin de partie")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Choice("Fin libre", !s.dbl, Modifier.weight(1f)) { s.dbl = false; s.save() }
        Choice("Double obligatoire", s.dbl, Modifier.weight(1f)) { s.dbl = true; s.save() }
    }
    Label("Joueurs")
    s.names.forEachIndexed { i, n ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            OutlinedTextField(
                n, { v -> s.names = s.names.toMutableList().also { it[i] = v.take(14) } },
                Modifier.weight(1f), singleLine = true
            )
            if (s.names.size > 1) TextButton({ s.names = s.names.filterIndexed { j, _ -> j != i } }) { Text("✕") }
        }
    }
    if (s.names.size < 8) OutlinedButton({ s.names = s.names + "Joueur ${s.names.size + 1}" }) { Text("+ Ajouter un joueur") }
    Button(
        { s.newGame() },
        Modifier.fillMaxWidth().padding(top = 20.dp).height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Line, contentColor = Bg)
    ) { Text("Lancer la partie", fontSize = 18.sp) }
}

@Composable
fun GameScreen(s: Store, g: Game) {
    var inp by rememberSaveable { mutableStateOf("") }
    var askDouble by remember { mutableStateOf(false) }
    var askQuit by remember { mutableStateOf(false) }

    fun add(c: Char) { val n = (inp + c).toIntOrNull() ?: return; if (n <= 180) inp = n.toString() }
    fun valider() {
        val v = inp.toIntOrNull() ?: 0
        if (g.dbl && v == g.rem(g.cur)) askDouble = true else { s.submit(v); inp = "" }
    }

    Text(mode(g), color = Muted)
    g.names.indices.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { i ->
                val cur = i == g.cur && !g.done
                Panel(Modifier.weight(1f), Line, if (cur) Yellow else Surf) {
                    Text(g.names[i], fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("${g.rem(i)}", fontSize = 60.sp, fontWeight = FontWeight.Black)
                    val l = g.turns(i).lastOrNull()
                    Text(
                        "moy. %.1f · %s".format(Locale.FRANCE, g.avg(i), if (l == null) "—" else if (l.bust) "volée ${l.v} (annulée)" else "dernière ${l.v}${if (l.bounce) " (rebond)" else ""}"),
                        color = Muted, fontSize = 12.sp
                    )
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Au tour de", color = Muted)
            Text(g.names[g.cur], fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(inp.ifEmpty { "0" }, fontSize = 48.sp, fontWeight = FontWeight.Bold)
    }
    listOf("123", "456", "789").forEach { r ->
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            r.forEach { c -> Choice("$c", false, Modifier.weight(1f).height(56.dp)) { add(c) } }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Choice("C", false, Modifier.weight(1f).height(56.dp)) { inp = "" }
        Choice("0", false, Modifier.weight(1f).height(56.dp)) { add('0') }
        Choice("⌫", false, Modifier.weight(1f).height(56.dp)) { inp = inp.dropLast(1) }
    }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton({ s.undo() }, Modifier.weight(1f).height(52.dp), enabled = g.log.isNotEmpty()) { Text("Annuler") }
        Button(
            { valider() }, Modifier.weight(2f).height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Line, contentColor = Bg)
        ) { Text("Valider la volée") }
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button({ s.pause() }, Modifier.weight(1f)) { Text("Mettre en pause", maxLines = 1) }
        OutlinedButton({ askQuit = true }, Modifier.weight(1f)) { Text("Abandonner", maxLines = 1) }
    }

    if (askDouble) AlertDialog(
        onDismissRequest = { askDouble = false },
        title = { Text("Dernière fléchette dans un double ?") },
        confirmButton = { TextButton({ s.submit(inp.toInt(), true); inp = ""; askDouble = false }) { Text("Oui") } },
        dismissButton = { TextButton({ s.submit(inp.toInt(), false); inp = ""; askDouble = false }) { Text("Non") } }
    )
    if (askQuit) Dialog(onDismissRequest = { askQuit = false }) {
        Panel(Modifier.fillMaxWidth(), fill = Surf, border = Line) {
            Text("ABANDONNER", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Line)
            Spacer(Modifier.height(8.dp))
            Text("Abandonner cette partie ?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ivory)
            Spacer(Modifier.height(6.dp))
            Text("Elle ne sera pas enregistrée. Utilise « Mettre en pause » pour la reprendre plus tard.", color = Muted, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    { askQuit = false },
                    Modifier.weight(1f).height(48.dp),
                    shape = RectangleShape,
                    border = BorderStroke(3.dp, Line),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Ivory)
                ) { Text("Annuler", maxLines = 1) }
                Button(
                    { askQuit = false; s.abandon() },
                    Modifier.weight(1f).height(48.dp),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Color.White)
                ) { Text("Abandonner", maxLines = 1) }
            }
        }
    }
    if (g.done) AlertDialog(
        onDismissRequest = {},
        title = { Text("🏆 ${g.names[g.win]} gagne !") },
        text = { Text("${g.start} · ${g.turns(g.win).size} volées · moy. %.1f".format(Locale.FRANCE, g.avg(g.win))) },
        confirmButton = { TextButton({ s.rematch() }) { Text("Rejouer") } },
        dismissButton = {
            Row {
                TextButton({ s.undo() }) { Text("Annuler la volée") }
                TextButton({ s.abandon() }) { Text("Terminer") }
            }
        }
    )
}

@Composable
fun HistoryScreen(s: Store) {
    if (s.history.isEmpty()) {
        Label("Aucune partie enregistrée")
        Text("Les parties terminées apparaissent ici.", color = Muted)
        return
    }
    s.history.forEach { h ->
        Panel(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Text("${fmt(h.ts)} · ${mode(h)}", color = Muted, fontSize = 13.sp)
            Text("● ${h.names[h.win]}", fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 4.dp))
            h.names.indices.forEach { i ->
                Text(
                    "${h.names[i]} : reste ${h.rem(i)} · moy. %.1f · max ${h.best(i)}".format(Locale.FRANCE, h.avg(i)),
                    color = if (i == h.win) Brass else Muted, fontSize = 14.sp
                )
            }
            TextButton({ s.deleteHistory(h.id) }) { Text("Supprimer", color = Muted) }
        }
    }
}

/** Logo : carré, cercle, triangle. */
@Composable
fun Logo() = Canvas(Modifier.size(46.dp, 18.dp)) {
    val h = size.height
    drawRect(Line, Offset(0f, h * .1f), Size(h * .8f, h * .8f))
    drawCircle(Red, h * .42f, Offset(h * 1.55f, h / 2))
    val t = Path().apply { moveTo(h * 2.0f, h * .9f); lineTo(h * 2.55f, h * .1f); lineTo(h * 3.1f, h * .9f); close() }
    drawPath(t, Line)
}

/** Composition géométrique de l'accueil : 4 x 2 cases dont une cible de fléchettes. */
@Composable
fun Geo() = Canvas(Modifier.fillMaxWidth().aspectRatio(2f)) {
    val c = size.width / 4
    fun quarter(col: Color, x: Int, y: Int, cx: Int, cy: Int, start: Float) =
        drawArc(col, start, 90f, true, Offset((x + cx) * c - c, (y + cy) * c - c), Size(2 * c, 2 * c))
    fun disc(col: Color, x: Int, y: Int, r: Float) = drawCircle(col, r * c, Offset((x + .5f) * c, (y + .5f) * c))
    quarter(Red, 0, 0, 1, 1, 180f)
    disc(Line, 1, 0, .5f)
    disc(Line, 2, 0, .5f); disc(Bg, 2, 0, .38f); disc(Line, 2, 0, .26f); disc(Red, 2, 0, .12f)
    quarter(Yellow, 3, 0, 0, 1, 270f)
    disc(Yellow, 0, 1, .5f)
    quarter(Brass, 1, 1, 0, 0, 0f)
    quarter(Line, 2, 1, 1, 0, 90f)
    disc(Red, 3, 1, .5f)
}
