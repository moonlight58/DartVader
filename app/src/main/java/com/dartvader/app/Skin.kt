package com.dartvader.app

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Liste des styles disponibles. Pour en ajouter un : une entrée ici + un Skin + une ligne dans skinOf(). */
enum class AppStyle(val label: String, val desc: String) {
    BAUHAUS("Bauhaus", "Formes géométriques, couleurs primaires, aplats et gros contours."),
    TECHWEAR("Techwear / Y2K Futuristic", "Interface HUD sombre, police mono, accents orange et violet.")
}

/** Tout ce qui change d'un style à l'autre. */
@Immutable
class Skin(
    val dark: Boolean,
    val bg: Color,
    val surf: Color,
    val line: Color,       // contours
    val ink: Color,        // texte
    val muted: Color,
    val primary: Color,    // sélection / onglet actif
    val cta: Color,        // gros boutons d'action
    val onCta: Color,
    val red: Color,
    val yellow: Color,
    val hi: Color,         // fond du joueur courant
    val hiBorder: Color,
    val panel: Shape,
    val btn: Shape,
    val field: Shape,
    val dialog: Shape,
    val bw: Dp,            // épaisseur des contours
    val font: FontFamily,
    val hud: Boolean       // décors techwear (crochets, hachures, etc.)
)

val SkinBauhaus = Skin(
    dark = false,
    bg = Color(0xFFF4F1EA), surf = Color(0xFFFFFFFF), line = Color(0xFF1E1814), ink = Color(0xFF1E1814),
    muted = Color(0xFF6B6258), primary = Color(0xFF2457E6),
    cta = Color(0xFF1E1814), onCta = Color(0xFFF4F1EA),
    red = Color(0xFFFF5A36), yellow = Color(0xFFFFD23F),
    hi = Color(0xFFFFD23F), hiBorder = Color(0xFF1E1814),
    panel = RectangleShape, btn = RoundedCornerShape(50), field = RoundedCornerShape(4.dp),
    dialog = RoundedCornerShape(28.dp),
    bw = 3.dp, font = FontFamily.Default, hud = false
)

val SkinTech = Skin(
    dark = true,
    bg = Color(0xFF0B0B0D), surf = Color(0xFF151518), line = Color(0xFF5C5C64), ink = Color(0xFFEDEDEF),
    muted = Color(0xFF9A9AA3), primary = Color(0xFFA24DFF),
    cta = Color(0xFFFF5A1F), onCta = Color(0xFF0B0B0D),
    red = Color(0xFFE5233B), yellow = Color(0xFFEDEDEF),
    hi = Color(0xFF2A1250), hiBorder = Color(0xFFFF5A1F),
    panel = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp),
    btn = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
    field = CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp),
    dialog = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp),
    bw = 1.dp, font = FontFamily.Monospace, hud = true
)

fun skinOf(s: AppStyle): Skin = when (s) {
    AppStyle.BAUHAUS -> SkinBauhaus
    AppStyle.TECHWEAR -> SkinTech
}

val LocalSkin = staticCompositionLocalOf { SkinBauhaus }

/** Applique une police à tous les styles de texte Material. */
fun typographyOf(f: FontFamily): Typography {
    val t = Typography()
    return t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = f),
        displayMedium = t.displayMedium.copy(fontFamily = f),
        displaySmall = t.displaySmall.copy(fontFamily = f),
        headlineLarge = t.headlineLarge.copy(fontFamily = f),
        headlineMedium = t.headlineMedium.copy(fontFamily = f),
        headlineSmall = t.headlineSmall.copy(fontFamily = f),
        titleLarge = t.titleLarge.copy(fontFamily = f),
        titleMedium = t.titleMedium.copy(fontFamily = f),
        titleSmall = t.titleSmall.copy(fontFamily = f),
        bodyLarge = t.bodyLarge.copy(fontFamily = f),
        bodyMedium = t.bodyMedium.copy(fontFamily = f),
        bodySmall = t.bodySmall.copy(fontFamily = f),
        labelLarge = t.labelLarge.copy(fontFamily = f),
        labelMedium = t.labelMedium.copy(fontFamily = f),
        labelSmall = t.labelSmall.copy(fontFamily = f)
    )
}
