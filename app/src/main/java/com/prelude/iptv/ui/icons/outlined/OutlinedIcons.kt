package com.prelude.iptv.ui.icons.outlined

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------
// Τοπικά αντίγραφα των Material Icons που χρησιμοποιεί η εφαρμογή.
// Αντικαθιστούν τη βιβλιοθήκη material-icons-extended (χιλιάδες εικονίδια),
// που βάραινε το R8/dex σε κάθε build. Ίδια ονόματα και ίδιο API με το
// Compose: αλλάζει μόνο το import. Πηγή: Material Icons, Apache 2.0.
// Για νέο εικονίδιο: πρόσθεσε εδώ το path από το επίσημο SVG (24x24).
// ---------------------------------------------------------------------

private fun icon(name: String, vararg paths: Pair<String, Boolean>): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            paths.forEach { (data, evenOdd) ->
                addPath(
                    pathData = addPathNodes(data),
                    fill = SolidColor(Color.Black),
                    pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero
                )
            }
        }
        .build()

private var _info: ImageVector? = null
val Icons.Outlined.Info: ImageVector
    get() = _info ?: icon("Outlined.Info", "M11 7h2v2h-2zm0 4h2v6h-2zm1-9C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z" to false).also { _info = it }

private var _liveTv: ImageVector? = null
val Icons.Outlined.LiveTv: ImageVector
    get() = _liveTv ?: icon("Outlined.LiveTv", "M9 10v8l7-4zm12-4h-7.58l3.29-3.29L16 2l-4 4h-.03l-4-4-.69.71L10.56 6H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 14H3V8h18v12z" to false).also { _liveTv = it }

private var _movie: ImageVector? = null
val Icons.Outlined.Movie: ImageVector
    get() = _movie ?: icon("Outlined.Movie", "M4 6.47 5.76 10H20v8H4V6.47M22 4h-4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4z" to false).also { _movie = it }

private var _tv: ImageVector? = null
val Icons.Outlined.Tv: ImageVector
    get() = _tv ?: icon("Outlined.Tv", "M21 3H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h5v2h8v-2h5c1.1 0 1.99-.9 1.99-2L23 5c0-1.1-.9-2-2-2zm0 14H3V5h18v12z" to false).also { _tv = it }
