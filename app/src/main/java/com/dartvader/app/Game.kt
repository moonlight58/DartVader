package com.dartvader.app

import android.content.Context
import androidx.compose.runtime.*
import org.json.JSONArray
import org.json.JSONObject

/** v = total lancé, d = points réellement retirés du score (négatif si rebond), bounce = excès renvoyé. */
data class Turn(val p: Int, val v: Int, val bust: Boolean, val d: Int = v, val bounce: Boolean = false)

data class Game(
    val id: Long,
    val start: Int,
    val dbl: Boolean,
    val names: List<String>,
    val log: List<Turn> = emptyList(),
    val cur: Int = 0,
    val done: Boolean = false,
    val win: Int = -1,
    val ts: Long = System.currentTimeMillis()
) {
    fun rem(i: Int) = start - log.filter { it.p == i && !it.bust }.sumOf { it.d }
    fun turns(i: Int) = log.filter { it.p == i }
    fun avg(i: Int): Double {
        val t = turns(i)
        return if (t.isEmpty()) 0.0 else t.sumOf { it.v }.toDouble() / t.size
    }
    fun best(i: Int) = turns(i).maxOfOrNull { it.v } ?: 0
}

private fun Game.toJson() = JSONObject()
    .put("id", id).put("start", start).put("dbl", dbl).put("names", JSONArray(names))
    .put("cur", cur).put("done", done).put("win", win).put("ts", ts)
    .put("log", JSONArray(log.map { JSONArray(listOf(it.p, it.v, it.bust, it.d, it.bounce)) }))

private fun gameOf(o: JSONObject): Game {
    val n = o.getJSONArray("names")
    val l = o.getJSONArray("log")
    return Game(
        o.getLong("id"), o.getInt("start"), o.getBoolean("dbl"),
        List(n.length()) { n.getString(it) },
        List(l.length()) { i -> l.getJSONArray(i).let { Turn(it.getInt(0), it.getInt(1), it.getBoolean(2), it.optInt(3, it.getInt(1)), it.optBoolean(4, false)) } },
        o.getInt("cur"), o.getBoolean("done"), o.getInt("win"), o.getLong("ts")
    )
}

/** État de l'appli, sauvegardé à chaque changement dans SharedPreferences. */
class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("darts", Context.MODE_PRIVATE)

    private fun one(k: String): Game? =
        sp.getString(k, null)?.let { runCatching { gameOf(JSONObject(it)) }.getOrNull() }

    private fun many(k: String): List<Game> =
        sp.getString(k, null)?.let { s ->
            runCatching { JSONArray(s).let { a -> List(a.length()) { gameOf(a.getJSONObject(it)) } } }.getOrNull()
        } ?: emptyList()

    var active by mutableStateOf(one("active"))
    var paused by mutableStateOf(many("paused"))
    var history by mutableStateOf(many("history"))
    var start by mutableStateOf(sp.getInt("start", 501))
    var dbl by mutableStateOf(sp.getBoolean("dbl", false))
    var names by mutableStateOf(
        sp.getString("names", null)?.let { s ->
            runCatching { JSONArray(s).let { a -> List(a.length()) { a.getString(it) } } }.getOrNull()
        } ?: listOf("Joueur 1", "Joueur 2")
    )
    var style by mutableStateOf(
        runCatching { AppStyle.valueOf(sp.getString("style", null)!!) }.getOrDefault(AppStyle.BAUHAUS)
    )

    fun save() {
        sp.edit()
            .putString("active", active?.toJson()?.toString())
            .putString("paused", JSONArray(paused.map { it.toJson() }).toString())
            .putString("history", JSONArray(history.map { it.toJson() }).toString())
            .putInt("start", start).putBoolean("dbl", dbl)
            .putString("names", JSONArray(names).toString())
            .putString("style", style.name)
            .apply()
    }

    fun newGame() {
        names = names.mapIndexed { i, n -> n.trim().ifEmpty { "Joueur ${i + 1}" } }
        active = Game(System.currentTimeMillis(), start, dbl, names)
        save()
    }

    fun rematch() {
        val g = active ?: return
        active = Game(System.currentTimeMillis(), g.start, g.dbl, g.names)
        save()
    }

    /** [v] = total de la volée. [okDouble] = la dernière fléchette était un double. */
    fun submit(v: Int, okDouble: Boolean = true) {
        val g = active ?: return
        val r = g.rem(g.cur)
        val bounce = v > r
        val d = if (bounce) 2 * r - v else v
        val bust = (g.dbl && r - d == 1) || (g.dbl && v == r && !okDouble)
        val won = !bust && v == r
        val finalD = if (bust) 0 else d
        val finalBounce = bounce && !bust

        val n = g.copy(
            log = g.log + Turn(g.cur, v, bust, finalD, finalBounce),
            done = won,
            win = if (won) g.cur else -1,
            cur = if (won) g.cur else (g.cur + 1) % g.names.size
        )
        if (won) history = listOf(n) + history
        active = n
        save()
    }

    fun undo() {
        val g = active ?: return
        if (g.log.isEmpty()) return
        if (g.done) history = history.filter { it.id != g.id }
        active = g.copy(log = g.log.dropLast(1), cur = g.log.last().p, done = false, win = -1)
        save()
    }

    fun pause() {
        val g = active ?: return
        paused = listOf(g.copy(ts = System.currentTimeMillis())) + paused
        active = null
        save()
    }

    fun resume(id: Long) {
        val p = paused.find { it.id == id } ?: return
        val cur = active
        paused = paused.filter { it.id != id }
        if (cur != null && !cur.done) paused = listOf(cur) + paused
        active = p
        save()
    }

    fun deletePaused(id: Long) { paused = paused.filter { it.id != id }; save() }
    fun deleteHistory(id: Long) { history = history.filter { it.id != id }; save() }
    fun abandon() { active = null; save() }
}
