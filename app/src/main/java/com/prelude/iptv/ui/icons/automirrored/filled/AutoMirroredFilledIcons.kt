package com.prelude.iptv.ui.icons.automirrored.filled

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
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f, autoMirror = true)
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

private var _arrowBack: ImageVector? = null
val Icons.AutoMirrored.Filled.ArrowBack: ImageVector
    get() = _arrowBack ?: icon("AutoMirrored.Filled.ArrowBack", "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z" to false).also { _arrowBack = it }

private var _insertDriveFile: ImageVector? = null
val Icons.AutoMirrored.Filled.InsertDriveFile: ImageVector
    get() = _insertDriveFile ?: icon("AutoMirrored.Filled.InsertDriveFile", "M6 2c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6H6zm7 7V3.5L18.5 9H13z" to false).also { _insertDriveFile = it }
