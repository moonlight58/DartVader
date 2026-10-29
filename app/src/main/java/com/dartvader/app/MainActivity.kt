package com.dartvader.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun fmt(ts: Long) = SimpleDateFormat("d MMM yyyy HH:mm", Locale.FRANCE).format(Date(ts))
private fun mode(g: Game) = "${g.start}${if (g.dbl) " · double out" else ""}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialBg = android.graphics.Color.parseColor("#F4F1EA")
        val bars = SystemBarStyle.light(initialBg, initialBg)
        enableEdgeToEdge(bars, bars)
        @Suppress("DEPRECATION")
        window.statusBarColor = initialBg
        @Suppress("DEPRECATION")
        window.navigationBarColor = initialBg
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent { DartsApp(remember { Store(applicationContext) }) }
    }
}

private fun colorsOf(k: Skin) =
    if (k.dark) darkColorScheme(
        primary = k.primary, onPrimary = Color.White, secondary = k.red, onSecondary = Color.White,
        background = k.bg, onBackground = k.ink, surface = k.surf, onSurface = k.ink,
        onSurfaceVariant = k.muted, outline = k.line,
        surfaceContainerHigh = k.surf, surfaceContainerHighest = k.surf
    ) else lightColorScheme(
        primary = k.primary, onPrimary = Color.White, secondary = k.red, onSecondary = Color.White,
        background = k.bg, onBackground = k.ink, surface = k.surf, onSurface = k.ink
    )

@Composable
fun DartsApp(s: Store) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val k = skinOf(s.style)
    val act = LocalContext.current as ComponentActivity

    // Synchronisation des barres système avec le skin actif
    LaunchedEffect(k) {
        val bgInt = android.graphics.Color.argb(
            (k.bg.alpha * 255).toInt(),
            (k.bg.red * 255).toInt(),
            (k.bg.green * 255).toInt(),
            (k.bg.blue * 255).toInt()
        )
        val bars = if (k.dark) SystemBarStyle.dark(bgInt) else SystemBarStyle.light(bgInt, bgInt)
        act.enableEdgeToEdge(bars, bars)
        @Suppress("DEPRECATION")
        act.window.statusBarColor = bgInt
        @Suppress("DEPRECATION")
        act.window.navigationBarColor = bgInt
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            act.window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(act.window, act.window.decorView).apply {
            isAppearanceLightStatusBars = !k.dark
            isAppearanceLightNavigationBars = !k.dark
        }
    }

    CompositionLocalProvider(LocalSkin provides k) {
        MaterialTheme(colorScheme = colorsOf(k), typography = typographyOf(k.font)) {
            Surface(Modifier.fillMaxSize().systemBarsPadding(), color = k.bg) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (k.hud) LogoHud() else Logo()
                        Text(
                            if (k.hud) "DARTVADER" else "DartVader",
                            fontSize = if (k.hud) 18.sp else 22.sp, fontWeight = FontWeight.Black, maxLines = 1,
                            modifier = Modifier.weight(1f).padding(start = 10.dp)
                        )
                        val pad = PaddingValues(horizontal = 8.dp)
                        val fs = if (k.hud) 12.sp else 14.sp
                        TextButton({ tab = 0 }, contentPadding = pad) { Text("Partie", fontSize = fs, color = if (tab == 0) k.primary else k.muted) }
                        TextButton({ tab = 1 }, contentPadding = pad) { Text("Historique", fontSize = fs, color = if (tab == 1) k.primary else k.muted) }
                        Text(
                            "⚙", fontSize = 22.sp, color = if (tab == 2) k.primary else k.muted,
                            modifier = Modifier.clickable { tab = 2 }.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                    if (k.hud) {
                        Hazard(Modifier.padding(start = 16.dp).width(72.dp).height(6.dp), k.cta)
                        Box(Modifier.fillMaxWidth().height(1.dp).background(k.line))
                    } else {
                        Box(Modifier.fillMaxWidth().height(3.dp).background(k.line))
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(16.dp)) {
                        val g = s.active
                        when {
                            tab == 2 -> SettingsScreen(s)
                            tab == 1 -> HistoryScreen(s)
                            g == null -> SetupScreen(s)
                            else -> GameScreen(s, g)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    border: Color = LocalSkin.current.line,
    fill: Color = LocalSkin.current.surf,
    content: @Composable ColumnScope.() -> Unit
) {
    val k = LocalSkin.current
    val ticks = k.cta
    Column(
        modifier
            .background(fill, k.panel)
            .border(k.bw, border, k.panel)
            .then(
                if (k.hud) Modifier.drawBehind {
                    // crochets de visée dans les deux coins non coupés
                    val l = 10.dp.toPx(); val w = 2.dp.toPx()
                    drawLine(ticks, Offset(0f, 0f), Offset(l, 0f), w)
                    drawLine(ticks, Offset(0f, 0f), Offset(0f, l), w)
                    drawLine(ticks, Offset(size.width, size.height), Offset(size.width - l, size.height), w)
                    drawLine(ticks, Offset(size.width, size.height), Offset(size.width, size.height - l), w)
                } else Modifier
            )
            .padding(12.dp),
        content = content
    )
}

@Composable
fun Label(t: String) {
    val k = LocalSkin.current
    Text(
        if (k.hud) "// ${t.uppercase()}" else t,
        color = k.muted, fontSize = if (k.hud) 12.sp else 14.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )
}

@Composable
fun Choice(t: String, sel: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val k = LocalSkin.current
    Button(
        onClick, modifier, shape = k.btn,
        border = BorderStroke(k.bw, if (sel) k.primary else k.line),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (sel) k.primary else k.surf,
            contentColor = if (sel) Color.White else k.ink
        )
    ) {
        Text(
            t, maxLines = 1,
            fontSize = when { !k.hud -> 14.sp; t.length > 3 -> 12.sp; else -> 18.sp }
        )
    }
}

@Composable
fun SettingsScreen(s: Store) {
    val k = LocalSkin.current
    if (k.hud) Text("✳ SYS.CONFIG", color = k.red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    Text("RÉGLAGES", fontSize = 40.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
    Label("Style de l'interface")
    AppStyle.entries.forEach { st ->
        val sel = s.style == st
        Panel(
            Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { s.style = st; s.save() },
            border = if (sel) k.primary else k.line
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Swatch(skinOf(st))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(st.label, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(st.desc, color = k.muted, fontSize = 13.sp)
                }
                Text(if (sel) "●" else "○", color = if (sel) k.primary else k.muted, fontSize = 22.sp)
            }
        }
    }
    Text("D'autres styles pourront être ajoutés ici.", color = k.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
}

/** Mini aperçu des couleurs d'un style. */
@Composable
fun Swatch(sk: Skin) = Canvas(Modifier.size(64.dp, 44.dp)) {
    val u = size.height
    drawRect(sk.bg)
    drawRect(sk.line, style = Stroke(2.dp.toPx()))
    drawCircle(sk.red, u * .18f, Offset(u * .45f, u * .5f))
    drawRect(sk.primary, Offset(u * .8f, u * .3f), Size(u * .4f, u * .4f))
    drawRect(sk.cta, Offset(u * 1.3f, u * .38f), Size(u * .12f, u * .24f))
}

@Composable
fun SetupScreen(s: Store) {
    val k = LocalSkin.current
    if (k.hud) HudGeo() else Geo()
    if (k.hud) Text("✳ SYS.NEW_GAME", color = k.red, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
    Text("NOUVELLE", fontSize = 40.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = if (k.hud) 4.dp else 12.dp))
    Text("PARTIE", fontSize = 40.sp, lineHeight = 40.sp, fontWeight = FontWeight.Light, color = if (k.hud) k.cta else k.ink)
    if (s.paused.isNotEmpty()) {
        Label("Parties en pause")
        s.paused.forEach { p ->
            Panel(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("${fmt(p.ts)} · ${mode(p)}", color = k.muted, fontSize = 13.sp)
                Text(p.names.indices.joinToString(" · ") { "${p.names[it]} ${p.rem(it)}" }, Modifier.padding(vertical = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ s.resume(p.id) }, Modifier.weight(1f), shape = k.btn) { Text("Reprendre", maxLines = 1) }
                    OutlinedButton({ s.deletePaused(p.id) }, Modifier.weight(1f), shape = k.btn) { Text("Supprimer", maxLines = 1) }
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
                Modifier.weight(1f), singleLine = true, shape = k.field
            )
            if (s.names.size > 1) TextButton({ s.names = s.names.filterIndexed { j, _ -> j != i } }) { Text("✕") }
        }
    }
    if (s.names.size < 8) OutlinedButton({ s.names = s.names + "Joueur ${s.names.size + 1}" }, shape = k.btn) { Text("+ Ajouter un joueur") }
    Button(
        { s.newGame() },
        Modifier.fillMaxWidth().padding(top = 20.dp).height(52.dp),
        shape = k.btn,
        colors = ButtonDefaults.buttonColors(containerColor = k.cta, contentColor = k.onCta)
    ) { Text("Lancer la partie", fontSize = 18.sp) }
}

@Composable
fun GameScreen(s: Store, g: Game) {
    val k = LocalSkin.current
    var inp by rememberSaveable { mutableStateOf("") }
    var currentDarts by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var pendingDartValue by remember { mutableStateOf(0) }
    var askDouble by remember { mutableStateOf(false) }
    var askQuit by remember { mutableStateOf(false) }

    val startRem = g.rem(g.cur)

    val runningScore = remember(startRem, currentDarts) {
        var sc = startRem
        for (d in currentDarts) {
            if (sc == 0) break
            sc = if (d > sc) d - sc else sc - d
        }
        sc
    }

    fun add(c: Char) { 
        val n = (inp + c).toIntOrNull() ?: return
        if (n <= 60) inp = n.toString()
    }

    fun validateDart() {
        val v = inp.toIntOrNull() ?: return
        if (v > 60) return
        inp = ""

        var testScore = runningScore
        val bounce = v > testScore
        testScore = if (bounce) v - testScore else testScore - v

        val needsDoubleCheck = g.dbl && testScore == 0

        if (needsDoubleCheck) {
            pendingDartValue = v
            askDouble = true
        } else {
            val nextDarts = currentDarts + v
            if (testScore == 0 || nextDarts.size >= 3) {
                s.submit(nextDarts, true)
                currentDarts = emptyList()
            } else {
                currentDarts = nextDarts
            }
        }
    }

    fun validateTurn() {
        if (currentDarts.isNotEmpty()) {
            s.submit(currentDarts, true)
            currentDarts = emptyList()
            inp = ""
        }
    }

    Text(mode(g), color = k.muted)
    g.names.indices.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { i ->
                val cur = i == g.cur && !g.done
                Panel(Modifier.weight(1f), if (cur) k.hiBorder else k.line, if (cur) k.hi else k.surf) {
                    Text(g.names[i], fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("${g.rem(i)}", fontSize = 60.sp, fontWeight = FontWeight.Black)
                    val l = g.turns(i).lastOrNull()
                    Text(
                        "moy. %.1f · %s".format(Locale.FRANCE, g.avg(i), if (l == null) "—" else if (l.bust) "volée ${l.v} (annulée)" else "dernière ${l.v} (${l.darts.joinToString("+")})${if (l.bounce) " (rebond)" else ""}"),
                        color = if (cur && !k.hud) Color(0xFF6B6258) else k.muted, fontSize = 12.sp
                    )
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }

    // Panel showing darts thrown this turn and running score
    Panel(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Fléchettes du tour (${currentDarts.size}/3)", fontWeight = FontWeight.Bold, color = k.muted, fontSize = 13.sp)
            Text("Reste visé : $runningScore", fontWeight = FontWeight.Bold, color = k.primary, fontSize = 14.sp)
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until 3) {
                val dartVal = currentDarts.getOrNull(i)
                Box(
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .background(if (dartVal != null) k.primary.copy(alpha = 0.15f) else k.surf, k.btn)
                        .border(k.bw, if (dartVal != null) k.primary else k.line, k.btn),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (dartVal != null) "$dartVal" else "—", fontWeight = FontWeight.Bold, color = if (dartVal != null) k.primary else k.muted)
                }
            }
        }
    }

    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Au tour de", color = k.muted)
            Text(g.names[g.cur], fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(inp.ifEmpty { "0" }, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = if (k.hud) k.cta else k.ink)
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton({ 
            if (currentDarts.isNotEmpty()) {
                currentDarts = currentDarts.dropLast(1)
            } else {
                s.undo()
            }
        }, Modifier.weight(1f).height(52.dp), shape = k.btn) { 
            Text(if (currentDarts.isNotEmpty()) "Effacer fléchette" else "Annuler") 
        }
        Button(
            { validateDart() }, Modifier.weight(2f).height(52.dp), shape = k.btn,
            colors = ButtonDefaults.buttonColors(containerColor = k.cta, contentColor = k.onCta),
            enabled = inp.isNotEmpty() && currentDarts.size < 3
        ) { Text("Valider la fléchette") }
    }
    if (currentDarts.isNotEmpty()) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Button(
                { validateTurn() }, Modifier.fillMaxWidth().height(48.dp), shape = k.btn,
                colors = ButtonDefaults.buttonColors(containerColor = k.primary, contentColor = Color.White)
            ) { Text("Valider le tour (${currentDarts.sum()} pts en ${currentDarts.size} fléchette${if (currentDarts.size > 1) "s" else ""})") }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button({ s.pause() }, Modifier.weight(1f), shape = k.btn) { Text("Mettre en pause", maxLines = 1) }
        OutlinedButton({ askQuit = true }, Modifier.weight(1f), shape = k.btn) { Text("Abandonner", maxLines = 1) }
    }

    if (askDouble) AlertDialog(
        onDismissRequest = { askDouble = false },
        shape = k.dialog,
        title = { Text("Fléchette dans un double ?") },
        confirmButton = { 
            TextButton({ 
                val nextDarts = currentDarts + pendingDartValue
                s.submit(nextDarts, true)
                currentDarts = emptyList()
                inp = ""
                askDouble = false 
            }) { Text("Oui") } 
        },
        dismissButton = { 
            TextButton({ 
                val nextDarts = currentDarts + pendingDartValue
                s.submit(nextDarts, false)
                currentDarts = emptyList()
                inp = ""
                askDouble = false 
            }) { Text("Non") } 
        }
    )
    if (askQuit) Dialog(onDismissRequest = { askQuit = false }) {
        Panel(Modifier.fillMaxWidth()) {
            Text("ABANDONNER", fontSize = 22.sp, fontWeight = FontWeight.Black, color = k.ink)
            Spacer(Modifier.height(8.dp))
            Text("Abandonner cette partie ?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = k.ink)
            Spacer(Modifier.height(6.dp))
            Text("Elle ne sera pas enregistrée. Utilise « Mettre en pause » pour la reprendre plus tard.", color = k.muted, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    { askQuit = false },
                    Modifier.weight(1f).height(48.dp),
                    shape = k.panel,
                    border = BorderStroke(k.bw, k.line),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = k.ink)
                ) { Text("Annuler", maxLines = 1) }
                Button(
                    { askQuit = false; s.abandon() },
                    Modifier.weight(1f).height(48.dp),
                    shape = k.panel,
                    colors = ButtonDefaults.buttonColors(containerColor = k.red, contentColor = Color.White)
                ) { Text("Abandonner", maxLines = 1) }
            }
        }
    }
    if (g.done) AlertDialog(
        onDismissRequest = {},
        shape = k.dialog,
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
    val k = LocalSkin.current
    if (s.history.isEmpty()) {
        Label("Aucune partie enregistrée")
        Text("Les parties terminées apparaissent ici.", color = k.muted)
        return
    }
    s.history.forEach { h ->
        Panel(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Text("${fmt(h.ts)} · ${mode(h)}", color = k.muted, fontSize = 13.sp)
            Text("● ${h.names[h.win]}", fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 4.dp))
            h.names.indices.forEach { i ->
                Text(
                    "${h.names[i]} : reste ${h.rem(i)} · moy. %.1f · max ${h.best(i)}".format(Locale.FRANCE, h.avg(i)),
                    color = if (i == h.win) k.primary else k.muted, fontSize = 14.sp
                )
            }
            TextButton({ s.deleteHistory(h.id) }) { Text("Supprimer", color = k.muted) }
        }
    }
}

// ───────────────────────── Bauhaus ─────────────────────────

/** Logo : carré, cercle, triangle. */
@Composable
fun Logo() {
    val k = LocalSkin.current
    Canvas(Modifier.size(46.dp, 18.dp)) {
        val h = size.height
        drawRect(k.line, Offset(0f, h * .1f), Size(h * .8f, h * .8f))
        drawCircle(k.red, h * .42f, Offset(h * 1.55f, h / 2))
        val t = Path().apply { moveTo(h * 2.0f, h * .9f); lineTo(h * 2.55f, h * .1f); lineTo(h * 3.1f, h * .9f); close() }
        drawPath(t, k.line)
    }
}

/** Composition géométrique de l'accueil : 4 x 2 cases dont une cible de fléchettes. */
@Composable
fun Geo() {
    val k = LocalSkin.current
    Canvas(Modifier.fillMaxWidth().aspectRatio(2f)) {
        val c = size.width / 4
        fun quarter(col: Color, x: Int, y: Int, cx: Int, cy: Int, start: Float) =
            drawArc(col, start, 90f, true, Offset((x + cx) * c - c, (y + cy) * c - c), Size(2 * c, 2 * c))
        fun disc(col: Color, x: Int, y: Int, r: Float) = drawCircle(col, r * c, Offset((x + .5f) * c, (y + .5f) * c))
        quarter(k.red, 0, 0, 1, 1, 180f)
        disc(k.line, 1, 0, .5f)
        disc(k.line, 2, 0, .5f); disc(k.bg, 2, 0, .38f); disc(k.line, 2, 0, .26f); disc(k.red, 2, 0, .12f)
        quarter(k.yellow, 3, 0, 0, 1, 270f)
        disc(k.yellow, 0, 1, .5f)
        quarter(k.primary, 1, 1, 0, 0, 0f)
        quarter(k.line, 2, 1, 1, 0, 90f)
        disc(k.red, 3, 1, .5f)
    }
}

// ───────────────────── Techwear / Y2K ─────────────────────

/** Logo HUD : viseur + barre + pastille orange. */
@Composable
fun LogoHud() {
    val k = LocalSkin.current
    Canvas(Modifier.size(46.dp, 18.dp)) {
        val h = size.height; val sw = 1.5.dp.toPx()
        drawCircle(k.ink, h * .42f, Offset(h / 2, h / 2), style = Stroke(sw))
        drawCircle(k.cta, h * .12f, Offset(h / 2, h / 2))
        drawLine(k.ink, Offset(h * 1.05f, h / 2), Offset(h * 1.9f, h / 2), sw)
        drawRect(k.cta, Offset(h * 2.0f, h * .2f), Size(h * .6f, h * .6f))
    }
}

/** Bande de hachures diagonales (signalétique de danger). */
private fun DrawScope.stripes(c: Color, tl: Offset, sz: Size) {
    clipRect(tl.x, tl.y, tl.x + sz.width, tl.y + sz.height) {
        val step = sz.height * 1.6f
        var x = tl.x - sz.height
        while (x < tl.x + sz.width) {
            drawLine(c, Offset(x, tl.y + sz.height), Offset(x + sz.height, tl.y), strokeWidth = step / 2)
            x += step
        }
    }
}

@Composable
fun Hazard(modifier: Modifier, color: Color) = Canvas(modifier) { stripes(color, Offset.Zero, size) }

private fun sparkle(c: Offset, r: Float) = Path().apply {
    moveTo(c.x, c.y - r)
    quadraticBezierTo(c.x, c.y, c.x + r, c.y)
    quadraticBezierTo(c.x, c.y, c.x, c.y + r)
    quadraticBezierTo(c.x, c.y, c.x - r, c.y)
    quadraticBezierTo(c.x, c.y, c.x, c.y - r)
    close()
}

/** Accueil HUD : cadre quadrillé, cible avec viseur, jauges, hachures, étincelles. */
@Composable
fun HudGeo() {
    val k = LocalSkin.current
    Canvas(Modifier.fillMaxWidth().aspectRatio(2f)) {
        val w = size.width; val h = size.height
        val hair = 1.dp.toPx(); val thick = 3.dp.toPx()
        val grid = k.line.copy(alpha = .5f)

        // cadre + grille
        for (i in 1..3) drawLine(grid, Offset(w * i / 4, 0f), Offset(w * i / 4, h), hair)
        drawLine(grid, Offset(0f, h / 2), Offset(w, h / 2), hair)
        drawRect(k.line, style = Stroke(hair))

        // cible + viseur (gauche)
        val c = Offset(w * .25f, h / 2)
        listOf(.42f, .32f, .2f).forEach { drawCircle(k.ink, h * it, c, style = Stroke(hair)) }
        drawCircle(k.cta, h * .08f, c)
        drawLine(k.ink, Offset(c.x - h * .5f, c.y), Offset(c.x + h * .5f, c.y), hair)
        drawLine(k.ink, Offset(c.x, 0f), Offset(c.x, h), hair)

        // jauges (droite)
        listOf(.9f, .55f, .75f, .35f, .65f).forEachIndexed { i, l ->
            val y = h * (.18f + i * .13f)
            drawLine(k.line, Offset(w * .55f, y), Offset(w * .95f, y), thick)
            drawLine(k.cta, Offset(w * .55f, y), Offset(w * (.55f + .4f * l), y), thick)
        }

        // blocs : hachures, violet
        stripes(k.cta, Offset(w * .03f, h * .86f), Size(w * .18f, h * .07f))
        stripes(k.ink, Offset(w * .55f, h * .86f), Size(w * .2f, h * .07f))
        drawRect(k.primary, Offset(w * .78f, h * .86f), Size(w * .17f, h * .07f))

        // étincelles
        drawPath(sparkle(Offset(w * .5f, h * .5f), h * .09f), k.ink)
        drawPath(sparkle(Offset(w * .07f, h * .15f), h * .06f), k.red)
    }
}
