package com.prelude.iptv.ui.icons.filled

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

private var _accountCircle: ImageVector? = null
val Icons.Filled.AccountCircle: ImageVector
    get() = _accountCircle ?: icon("Filled.AccountCircle", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 4c1.93 0 3.5 1.57 3.5 3.5S13.93 13 12 13s-3.5-1.57-3.5-3.5S10.07 6 12 6zm0 14c-2.03 0-4.43-.82-6.14-2.88a9.947 9.947 0 0 1 12.28 0C16.43 19.18 14.03 20 12 20z" to false).also { _accountCircle = it }

private var _add: ImageVector? = null
val Icons.Filled.Add: ImageVector
    get() = _add ?: icon("Filled.Add", "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z" to false).also { _add = it }

private var _addCircleOutline: ImageVector? = null
val Icons.Filled.AddCircleOutline: ImageVector
    get() = _addCircleOutline ?: icon("Filled.AddCircleOutline", "M13 7h-2v4H7v2h4v4h2v-4h4v-2h-4V7zm-1-5C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z" to false).also { _addCircleOutline = it }

private var _autoAwesome: ImageVector? = null
val Icons.Filled.AutoAwesome: ImageVector
    get() = _autoAwesome ?: icon("Filled.AutoAwesome", "m19 9 1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z" to false).also { _autoAwesome = it }

private var _backup: ImageVector? = null
val Icons.Filled.Backup: ImageVector
    get() = _backup ?: icon("Filled.Backup", "M19.35 10.04A7.49 7.49 0 0 0 12 4C9.11 4 6.6 5.64 5.35 8.04A5.994 5.994 0 0 0 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z" to false).also { _backup = it }

private var _bedtime: ImageVector? = null
val Icons.Filled.Bedtime: ImageVector
    get() = _bedtime ?: icon("Filled.Bedtime", "M12.34 2.02C6.59 1.82 2 6.42 2 12c0 5.52 4.48 10 10 10 3.71 0 6.93-2.02 8.66-5.02-7.51-.25-12.09-8.43-8.32-14.96z" to false).also { _bedtime = it }

private var _bolt: ImageVector? = null
val Icons.Filled.Bolt: ImageVector
    get() = _bolt ?: icon("Filled.Bolt", "M11 21h-1l1-7H7.5c-.58 0-.57-.32-.38-.66.19-.34.05-.08.07-.12C8.48 10.94 10.42 7.54 13 3h1l-1 7h3.5c.49 0 .56.33.47.51l-.07.15C12.96 17.55 11 21 11 21z" to false).also { _bolt = it }

private var _bookmark: ImageVector? = null
val Icons.Filled.Bookmark: ImageVector
    get() = _bookmark ?: icon("Filled.Bookmark", "M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z" to false).also { _bookmark = it }

private var _bugReport: ImageVector? = null
val Icons.Filled.BugReport: ImageVector
    get() = _bugReport ?: icon("Filled.BugReport", "M20 8h-2.81a5.985 5.985 0 0 0-1.82-1.96L17 4.41 15.59 3l-2.17 2.17C12.96 5.06 12.49 5 12 5c-.49 0-.96.06-1.41.17L8.41 3 7 4.41l1.62 1.63C7.88 6.55 7.26 7.22 6.81 8H4v2h2.09c-.05.33-.09.66-.09 1v1H4v2h2v1c0 .34.04.67.09 1H4v2h2.81c1.04 1.79 2.97 3 5.19 3s4.15-1.21 5.19-3H20v-2h-2.09c.05-.33.09-.66.09-1v-1h2v-2h-2v-1c0-.34-.04-.67-.09-1H20V8zm-6 8h-4v-2h4v2zm0-4h-4v-2h4v2z" to false).also { _bugReport = it }

private var _cable: ImageVector? = null
val Icons.Filled.Cable: ImageVector
    get() = _cable ?: icon("Filled.Cable", "M20 5V4c0-.55-.45-1-1-1h-2c-.55 0-1 .45-1 1v1h-1v4c0 .55.45 1 1 1h1v7c0 1.1-.9 2-2 2s-2-.9-2-2V7c0-2.21-1.79-4-4-4S5 4.79 5 7v7H4c-.55 0-1 .45-1 1v4h1v1c0 .55.45 1 1 1h2c.55 0 1-.45 1-1v-1h1v-4c0-.55-.45-1-1-1H7V7c0-1.1.9-2 2-2s2 .9 2 2v10c0 2.21 1.79 4 4 4s4-1.79 4-4v-7h1c.55 0 1-.45 1-1V5h-1z" to false).also { _cable = it }

private var _cached: ImageVector? = null
val Icons.Filled.Cached: ImageVector
    get() = _cached ?: icon("Filled.Cached", "m19 8-4 4h3c0 3.31-2.69 6-6 6a5.87 5.87 0 0 1-2.8-.7l-1.46 1.46A7.93 7.93 0 0 0 12 20c4.42 0 8-3.58 8-8h3l-4-4zM6 12c0-3.31 2.69-6 6-6 1.01 0 1.97.25 2.8.7l1.46-1.46A7.93 7.93 0 0 0 12 4c-4.42 0-8 3.58-8 8H1l4 4 4-4H6z" to false).also { _cached = it }

private var _calendarMonth: ImageVector? = null
val Icons.Filled.CalendarMonth: ImageVector
    get() = _calendarMonth ?: icon("Filled.CalendarMonth", "M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20a2 2 0 0 0 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V10h14v10zM9 14H7v-2h2v2zm4 0h-2v-2h2v2zm4 0h-2v-2h2v2zm-8 4H7v-2h2v2zm4 0h-2v-2h2v2zm4 0h-2v-2h2v2z" to false).also { _calendarMonth = it }

private var _category: ImageVector? = null
val Icons.Filled.Category: ImageVector
    get() = _category ?: icon("Filled.Category", "m12 2-5.5 9h11z" to false, "M13.0,17.5a4.5,4.5 0 1,0 9.0,0a4.5,4.5 0 1,0 -9.0,0z" to false, "M3 13.5h8v8H3z" to false).also { _category = it }

private var _check: ImageVector? = null
val Icons.Filled.Check: ImageVector
    get() = _check ?: icon("Filled.Check", "M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" to false).also { _check = it }

private var _checkBox: ImageVector? = null
val Icons.Filled.CheckBox: ImageVector
    get() = _checkBox ?: icon("Filled.CheckBox", "M19 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2zm-9 14-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" to false).also { _checkBox = it }

private var _checkBoxOutlineBlank: ImageVector? = null
val Icons.Filled.CheckBoxOutlineBlank: ImageVector
    get() = _checkBoxOutlineBlank ?: icon("Filled.CheckBoxOutlineBlank", "M19 5v14H5V5h14m0-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z" to false).also { _checkBoxOutlineBlank = it }

private var _checkCircle: ImageVector? = null
val Icons.Filled.CheckCircle: ImageVector
    get() = _checkCircle ?: icon("Filled.CheckCircle", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" to false).also { _checkCircle = it }

private var _chevronLeft: ImageVector? = null
val Icons.Filled.ChevronLeft: ImageVector
    get() = _chevronLeft ?: icon("Filled.ChevronLeft", "M15.41 7.41 14 6l-6 6 6 6 1.41-1.41L10.83 12z" to false).also { _chevronLeft = it }

private var _chevronRight: ImageVector? = null
val Icons.Filled.ChevronRight: ImageVector
    get() = _chevronRight ?: icon("Filled.ChevronRight", "M10 6 8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z" to false).also { _chevronRight = it }

private var _cleaningServices: ImageVector? = null
val Icons.Filled.CleaningServices: ImageVector
    get() = _cleaningServices ?: icon("Filled.CleaningServices", "M16 11h-1V3c0-1.1-.9-2-2-2h-2c-1.1 0-2 .9-2 2v8H8c-2.76 0-5 2.24-5 5v7h18v-7c0-2.76-2.24-5-5-5zm3 10h-2v-3c0-.55-.45-1-1-1s-1 .45-1 1v3h-2v-3c0-.55-.45-1-1-1s-1 .45-1 1v3H9v-3c0-.55-.45-1-1-1s-1 .45-1 1v3H5v-5c0-1.65 1.35-3 3-3h8c1.65 0 3 1.35 3 3v5z" to false).also { _cleaningServices = it }

private var _close: ImageVector? = null
val Icons.Filled.Close: ImageVector
    get() = _close ?: icon("Filled.Close", "M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" to false).also { _close = it }

private var _closedCaption: ImageVector? = null
val Icons.Filled.ClosedCaption: ImageVector
    get() = _closedCaption ?: icon("Filled.ClosedCaption", "M19 4H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-8 7H9.5v-.5h-2v3h2V13H11v1c0 .55-.45 1-1 1H7c-.55 0-1-.45-1-1v-4c0-.55.45-1 1-1h3c.55 0 1 .45 1 1v1zm7 0h-1.5v-.5h-2v3h2V13H18v1c0 .55-.45 1-1 1h-3c-.55 0-1-.45-1-1v-4c0-.55.45-1 1-1h3c.55 0 1 .45 1 1v1z" to false).also { _closedCaption = it }

private var _cloudOff: ImageVector? = null
val Icons.Filled.CloudOff: ImageVector
    get() = _cloudOff ?: icon("Filled.CloudOff", "M19.35 10.04A7.49 7.49 0 0 0 12 4c-1.48 0-2.85.43-4.01 1.17l1.46 1.46a5.497 5.497 0 0 1 8.05 4.87v.5H19c1.66 0 3 1.34 3 3 0 1.13-.64 2.11-1.56 2.62l1.45 1.45C23.16 18.16 24 16.68 24 15c0-2.64-2.05-4.78-4.65-4.96zM3 5.27l2.75 2.74C2.56 8.15 0 10.77 0 14c0 3.31 2.69 6 6 6h11.73l2 2L21 20.73 4.27 4 3 5.27zM7.73 10l8 8H6c-2.21 0-4-1.79-4-4s1.79-4 4-4h1.73z" to false).also { _cloudOff = it }

private var _contentCopy: ImageVector? = null
val Icons.Filled.ContentCopy: ImageVector
    get() = _contentCopy ?: icon("Filled.ContentCopy", "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z" to false).also { _contentCopy = it }

private var _delete: ImageVector? = null
val Icons.Filled.Delete: ImageVector
    get() = _delete ?: icon("Filled.Delete", "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z" to false).also { _delete = it }

private var _deleteOutline: ImageVector? = null
val Icons.Filled.DeleteOutline: ImageVector
    get() = _deleteOutline ?: icon("Filled.DeleteOutline", "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM8 9h8v10H8V9zm7.5-5-1-1h-5l-1 1H5v2h14V4z" to false).also { _deleteOutline = it }

private var _description: ImageVector? = null
val Icons.Filled.Description: ImageVector
    get() = _description ?: icon("Filled.Description", "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z" to false).also { _description = it }

private var _devices: ImageVector? = null
val Icons.Filled.Devices: ImageVector
    get() = _devices ?: icon("Filled.Devices", "M4 6h18V4H4c-1.1 0-2 .9-2 2v11H0v3h14v-3H4V6zm19 2h-6c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h6c.55 0 1-.45 1-1V9c0-.55-.45-1-1-1zm-1 9h-4v-7h4v7z" to false).also { _devices = it }

private var _dns: ImageVector? = null
val Icons.Filled.Dns: ImageVector
    get() = _dns ?: icon("Filled.Dns", "M20 13H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h16c.55 0 1-.45 1-1v-6c0-.55-.45-1-1-1zM7 19c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zM20 3H4c-.55 0-1 .45-1 1v6c0 .55.45 1 1 1h16c.55 0 1-.45 1-1V4c0-.55-.45-1-1-1zM7 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z" to false).also { _dns = it }

private var _download: ImageVector? = null
val Icons.Filled.Download: ImageVector
    get() = _download ?: icon("Filled.Download", "M5 20h14v-2H5v2zM19 9h-4V3H9v6H5l7 7 7-7z" to false).also { _download = it }

private var _dragHandle: ImageVector? = null
val Icons.Filled.DragHandle: ImageVector
    get() = _dragHandle ?: icon("Filled.DragHandle", "M20 9H4v2h16V9zM4 15h16v-2H4v2z" to false).also { _dragHandle = it }

private var _edit: ImageVector? = null
val Icons.Filled.Edit: ImageVector
    get() = _edit ?: icon("Filled.Edit", "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a.996.996 0 0 0 0-1.41l-2.34-2.34a.996.996 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z" to false).also { _edit = it }

private var _error: ImageVector? = null
val Icons.Filled.Error: ImageVector
    get() = _error ?: icon("Filled.Error", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z" to false).also { _error = it }

private var _expandLess: ImageVector? = null
val Icons.Filled.ExpandLess: ImageVector
    get() = _expandLess ?: icon("Filled.ExpandLess", "m12 8-6 6 1.41 1.41L12 10.83l4.59 4.58L18 14z" to false).also { _expandLess = it }

private var _expandMore: ImageVector? = null
val Icons.Filled.ExpandMore: ImageVector
    get() = _expandMore ?: icon("Filled.ExpandMore", "M16.59 8.59 12 13.17 7.41 8.59 6 10l6 6 6-6z" to false).also { _expandMore = it }

private var _face: ImageVector? = null
val Icons.Filled.Face: ImageVector
    get() = _face ?: icon("Filled.Face", "M9 11.75a1.25 1.25 0 1 0 0 2.5 1.25 1.25 0 0 0 0-2.5zm6 0a1.25 1.25 0 1 0 0 2.5 1.25 1.25 0 0 0 0-2.5zM12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8 0-.29.02-.58.05-.86 2.36-1.05 4.23-2.98 5.21-5.37a9.974 9.974 0 0 0 10.41 3.97c.21.71.33 1.47.33 2.26 0 4.41-3.59 8-8 8z" to false).also { _face = it }

private var _favorite: ImageVector? = null
val Icons.Filled.Favorite: ImageVector
    get() = _favorite ?: icon("Filled.Favorite", "m12 21.35-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" to false).also { _favorite = it }

private var _favoriteBorder: ImageVector? = null
val Icons.Filled.FavoriteBorder: ImageVector
    get() = _favoriteBorder ?: icon("Filled.FavoriteBorder", "M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3zm-4.4 15.55-.1.1-.1-.1C7.14 14.24 4 11.39 4 8.5 4 6.5 5.5 5 7.5 5c1.54 0 3.04.99 3.57 2.36h1.87C13.46 5.99 14.96 5 16.5 5c2 0 3.5 1.5 3.5 3.5 0 2.89-3.14 5.74-7.9 10.05z" to false).also { _favoriteBorder = it }

private var _folderOpen: ImageVector? = null
val Icons.Filled.FolderOpen: ImageVector
    get() = _folderOpen ?: icon("Filled.FolderOpen", "M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z" to false).also { _folderOpen = it }

private var _formatSize: ImageVector? = null
val Icons.Filled.FormatSize: ImageVector
    get() = _formatSize ?: icon("Filled.FormatSize", "M9 4v3h5v12h3V7h5V4H9zm-6 8h3v7h3v-7h3V9H3v3z" to false).also { _formatSize = it }

private var _fullscreen: ImageVector? = null
val Icons.Filled.Fullscreen: ImageVector
    get() = _fullscreen ?: icon("Filled.Fullscreen", "M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z" to false).also { _fullscreen = it }

private var _fullscreenExit: ImageVector? = null
val Icons.Filled.FullscreenExit: ImageVector
    get() = _fullscreenExit ?: icon("Filled.FullscreenExit", "M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z" to false).also { _fullscreenExit = it }

private var _gridView: ImageVector? = null
val Icons.Filled.GridView: ImageVector
    get() = _gridView ?: icon("Filled.GridView", "M3 3v8h8V3H3zm6 6H5V5h4v4zm-6 4v8h8v-8H3zm6 6H5v-4h4v4zm4-16v8h8V3h-8zm6 6h-4V5h4v4zm-6 4v8h8v-8h-8zm6 6h-4v-4h4v4z" to true).also { _gridView = it }

private var _helpOutline: ImageVector? = null
val Icons.Filled.HelpOutline: ImageVector
    get() = _helpOutline ?: icon("Filled.HelpOutline", "M11 18h2v-2h-2v2zm1-16C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm0-14c-2.21 0-4 1.79-4 4h2c0-1.1.9-2 2-2s2 .9 2 2c0 2-3 1.75-3 5h2c0-2.25 3-2.5 3-5 0-2.21-1.79-4-4-4z" to false).also { _helpOutline = it }

private var _history: ImageVector? = null
val Icons.Filled.History: ImageVector
    get() = _history ?: icon("Filled.History", "M13 3a9 9 0 0 0-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42A8.954 8.954 0 0 0 13 21a9 9 0 0 0 0-18zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z" to false).also { _history = it }

private var _home: ImageVector? = null
val Icons.Filled.Home: ImageVector
    get() = _home ?: icon("Filled.Home", "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z" to false).also { _home = it }

private var _indeterminateCheckBox: ImageVector? = null
val Icons.Filled.IndeterminateCheckBox: ImageVector
    get() = _indeterminateCheckBox ?: icon("Filled.IndeterminateCheckBox", "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2 10H7v-2h10v2z" to false).also { _indeterminateCheckBox = it }

private var _info: ImageVector? = null
val Icons.Filled.Info: ImageVector
    get() = _info ?: icon("Filled.Info", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" to false).also { _info = it }

private var _iosShare: ImageVector? = null
val Icons.Filled.IosShare: ImageVector
    get() = _iosShare ?: icon("Filled.IosShare", "m16 5-1.42 1.42-1.59-1.59V16h-1.98V4.83L9.42 6.42 8 5l4-4 4 4zm4 5v11c0 1.1-.9 2-2 2H6a2 2 0 0 1-2-2V10c0-1.11.89-2 2-2h3v2H6v11h12V10h-3V8h3a2 2 0 0 1 2 2z" to false).also { _iosShare = it }

private var _keyboardArrowDown: ImageVector? = null
val Icons.Filled.KeyboardArrowDown: ImageVector
    get() = _keyboardArrowDown ?: icon("Filled.KeyboardArrowDown", "M7.41 8.59 12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z" to false).also { _keyboardArrowDown = it }

private var _language: ImageVector? = null
val Icons.Filled.Language: ImageVector
    get() = _language ?: icon("Filled.Language", "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95a15.65 15.65 0 0 0-1.38-3.56A8.03 8.03 0 0 1 18.92 8zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2 0 .68.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56A7.987 7.987 0 0 1 5.08 16zm2.95-8H5.08a7.987 7.987 0 0 1 4.33-3.56A15.65 15.65 0 0 0 8.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2 0-.68.07-1.35.16-2h4.68c.09.65.16 1.32.16 2 0 .68-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95a8.03 8.03 0 0 1-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2 0-.68-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z" to false).also { _language = it }

private var _link: ImageVector? = null
val Icons.Filled.Link: ImageVector
    get() = _link ?: icon("Filled.Link", "M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z" to false).also { _link = it }

private var _liveTv: ImageVector? = null
val Icons.Filled.LiveTv: ImageVector
    get() = _liveTv ?: icon("Filled.LiveTv", "M21 6h-7.59l3.29-3.29L16 2l-4 4-4-4-.71.71L10.59 6H3a2 2 0 0 0-2 2v12c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V8a2 2 0 0 0-2-2zm0 14H3V8h18v12zM9 10v8l7-4z" to false).also { _liveTv = it }

private var _lock: ImageVector? = null
val Icons.Filled.Lock: ImageVector
    get() = _lock ?: icon("Filled.Lock", "M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z" to false).also { _lock = it }

private var _menu: ImageVector? = null
val Icons.Filled.Menu: ImageVector
    get() = _menu ?: icon("Filled.Menu", "M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z" to false).also { _menu = it }

private var _mic: ImageVector? = null
val Icons.Filled.Mic: ImageVector
    get() = _mic ?: icon("Filled.Mic", "M12 14c1.66 0 2.99-1.34 2.99-3L15 5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z" to false).also { _mic = it }

private var _moreVert: ImageVector? = null
val Icons.Filled.MoreVert: ImageVector
    get() = _moreVert ?: icon("Filled.MoreVert", "M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z" to false).also { _moreVert = it }

private var _movie: ImageVector? = null
val Icons.Filled.Movie: ImageVector
    get() = _movie ?: icon("Filled.Movie", "m18 4 2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z" to false).also { _movie = it }

private var _myLocation: ImageVector? = null
val Icons.Filled.MyLocation: ImageVector
    get() = _myLocation ?: icon("Filled.MyLocation", "M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3A8.994 8.994 0 0 0 13 3.06V1h-2v2.06A8.994 8.994 0 0 0 3.06 11H1v2h2.06A8.994 8.994 0 0 0 11 20.94V23h2v-2.06A8.994 8.994 0 0 0 20.94 13H23v-2h-2.06zM12 19c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z" to false).also { _myLocation = it }

private var _notificationsNone: ImageVector? = null
val Icons.Filled.NotificationsNone: ImageVector
    get() = _notificationsNone ?: icon("Filled.NotificationsNone", "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2zm-2 1H8v-6c0-2.48 1.51-4.5 4-4.5s4 2.02 4 4.5v6z" to false).also { _notificationsNone = it }

private var _palette: ImageVector? = null
val Icons.Filled.Palette: ImageVector
    get() = _palette ?: icon("Filled.Palette", "M12 2C6.49 2 2 6.49 2 12s4.49 10 10 10a2.5 2.5 0 0 0 2.5-2.5c0-.61-.23-1.2-.64-1.67a.528.528 0 0 1-.13-.33c0-.28.22-.5.5-.5H16c3.31 0 6-2.69 6-6 0-4.96-4.49-9-10-9zm5.5 11c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm-3-4c-.83 0-1.5-.67-1.5-1.5S13.67 6 14.5 6s1.5.67 1.5 1.5S15.33 9 14.5 9zM5 11.5c0-.83.67-1.5 1.5-1.5s1.5.67 1.5 1.5S7.33 13 6.5 13 5 12.33 5 11.5zm6-4c0 .83-.67 1.5-1.5 1.5S8 8.33 8 7.5 8.67 6 9.5 6s1.5.67 1.5 1.5z" to false).also { _palette = it }

private var _pause: ImageVector? = null
val Icons.Filled.Pause: ImageVector
    get() = _pause ?: icon("Filled.Pause", "M6 19h4V5H6v14zm8-14v14h4V5h-4z" to false).also { _pause = it }

private var _person: ImageVector? = null
val Icons.Filled.Person: ImageVector
    get() = _person ?: icon("Filled.Person", "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" to false).also { _person = it }

private var _playArrow: ImageVector? = null
val Icons.Filled.PlayArrow: ImageVector
    get() = _playArrow ?: icon("Filled.PlayArrow", "M8 5v14l11-7z" to false).also { _playArrow = it }

private var _playCircle: ImageVector? = null
val Icons.Filled.PlayCircle: ImageVector
    get() = _playCircle ?: icon("Filled.PlayCircle", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zM9.5 16.5v-9l7 4.5-7 4.5z" to false).also { _playCircle = it }

private var _playlistPlay: ImageVector? = null
val Icons.Filled.PlaylistPlay: ImageVector
    get() = _playlistPlay ?: icon("Filled.PlaylistPlay", "M3 10h11v2H3zm0-4h11v2H3zm0 8h7v2H3zm13-1v8l6-4z" to false).also { _playlistPlay = it }

private var _radioButtonUnchecked: ImageVector? = null
val Icons.Filled.RadioButtonUnchecked: ImageVector
    get() = _radioButtonUnchecked ?: icon("Filled.RadioButtonUnchecked", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8z" to false).also { _radioButtonUnchecked = it }

private var _refresh: ImageVector? = null
val Icons.Filled.Refresh: ImageVector
    get() = _refresh ?: icon("Filled.Refresh", "M17.65 6.35A7.958 7.958 0 0 0 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0 1 12 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z" to false).also { _refresh = it }

private var _restartAlt: ImageVector? = null
val Icons.Filled.RestartAlt: ImageVector
    get() = _restartAlt ?: icon("Filled.RestartAlt", "M12 5V2L8 6l4 4V7c3.31 0 6 2.69 6 6 0 2.97-2.17 5.43-5 5.91v2.02c3.95-.49 7-3.85 7-7.93 0-4.42-3.58-8-8-8zm-6 8c0-1.65.67-3.15 1.76-4.24L6.34 7.34A8.014 8.014 0 0 0 4 13c0 4.08 3.05 7.44 7 7.93v-2.02c-2.83-.48-5-2.94-5-5.91z" to false).also { _restartAlt = it }

private var _search: ImageVector? = null
val Icons.Filled.Search: ImageVector
    get() = _search ?: icon("Filled.Search", "M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z" to false).also { _search = it }

private var _searchOff: ImageVector? = null
val Icons.Filled.SearchOff: ImageVector
    get() = _searchOff ?: icon("Filled.SearchOff", "M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 0 0 9.5 3C6.08 3 3.28 5.64 3.03 9h2.02C5.3 6.75 7.18 5 9.5 5 11.99 5 14 7.01 14 9.5S11.99 14 9.5 14c-.17 0-.33-.03-.5-.05v2.02c.17.02.33.03.5.03 1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5z" to false, "M6.47 10.82 4 13.29l-2.47-2.47-.71.71L3.29 14 .82 16.47l.71.71L4 14.71l2.47 2.47.71-.71L4.71 14l2.47-2.47z" to false).also { _searchOff = it }

private var _security: ImageVector? = null
val Icons.Filled.Security: ImageVector
    get() = _security ?: icon("Filled.Security", "M12 1 3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z" to false).also { _security = it }

private var _selectAll: ImageVector? = null
val Icons.Filled.SelectAll: ImageVector
    get() = _selectAll ?: icon("Filled.SelectAll", "M3 5h2V3c-1.1 0-2 .9-2 2zm0 8h2v-2H3v2zm4 8h2v-2H7v2zM3 9h2V7H3v2zm10-6h-2v2h2V3zm6 0v2h2c0-1.1-.9-2-2-2zM5 21v-2H3c0 1.1.9 2 2 2zm-2-4h2v-2H3v2zM9 3H7v2h2V3zm2 18h2v-2h-2v2zm8-8h2v-2h-2v2zm0 8c1.1 0 2-.9 2-2h-2v2zm0-12h2V7h-2v2zm0 8h2v-2h-2v2zm-4 4h2v-2h-2v2zm0-16h2V3h-2v2zM7 17h10V7H7v10zm2-8h6v6H9V9z" to false).also { _selectAll = it }

private var _send: ImageVector? = null
val Icons.Filled.Send: ImageVector
    get() = _send ?: icon("Filled.Send", "M2.01 21 23 12 2.01 3 2 10l15 2-15 2z" to false).also { _send = it }

private var _settings: ImageVector? = null
val Icons.Filled.Settings: ImageVector
    get() = _settings ?: icon("Filled.Settings", "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.488.488 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.484.484 0 0 0-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z" to false).also { _settings = it }

private var _settingsInputAntenna: ImageVector? = null
val Icons.Filled.SettingsInputAntenna: ImageVector
    get() = _settingsInputAntenna ?: icon("Filled.SettingsInputAntenna", "M12 5c-3.87 0-7 3.13-7 7h2c0-2.76 2.24-5 5-5s5 2.24 5 5h2c0-3.87-3.13-7-7-7zm1 9.29c.88-.39 1.5-1.26 1.5-2.29a2.5 2.5 0 0 0-5 0c0 1.02.62 1.9 1.5 2.29v3.3L7.59 21 9 22.41l3-3 3 3L16.41 21 13 17.59v-3.3zM12 1C5.93 1 1 5.93 1 12h2a9 9 0 0 1 18 0h2c0-6.07-4.93-11-11-11z" to false).also { _settingsInputAntenna = it }

private var _settingsSuggest: ImageVector? = null
val Icons.Filled.SettingsSuggest: ImageVector
    get() = _settingsSuggest ?: icon("Filled.SettingsSuggest", "M17.41 6.59 15 5.5l2.41-1.09L18.5 2l1.09 2.41L22 5.5l-2.41 1.09L18.5 9l-1.09-2.41zm3.87 6.13L20.5 11l-.78 1.72-1.72.78 1.72.78.78 1.72.78-1.72L23 13.5l-1.72-.78zm-5.04 1.65 1.94 1.47-2.5 4.33-2.24-.94c-.2.13-.42.26-.64.37l-.3 2.4h-5l-.3-2.41c-.22-.11-.43-.23-.64-.37l-2.24.94-2.5-4.33 1.94-1.47c-.01-.11-.01-.24-.01-.36s0-.25.01-.37l-1.94-1.47 2.5-4.33 2.24.94c.2-.13.42-.26.64-.37L7.5 6h5l.3 2.41c.22.11.43.23.64.37l2.24-.94 2.5 4.33-1.94 1.47c.01.12.01.24.01.37s0 .24-.01.36zM13 14c0-1.66-1.34-3-3-3s-3 1.34-3 3 1.34 3 3 3 3-1.34 3-3z" to false).also { _settingsSuggest = it }

private var _share: ImageVector? = null
val Icons.Filled.Share: ImageVector
    get() = _share ?: icon("Filled.Share", "M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31-2.92-2.92-2.92z" to false).also { _share = it }

private var _sort: ImageVector? = null
val Icons.Filled.Sort: ImageVector
    get() = _sort ?: icon("Filled.Sort", "M3 18h6v-2H3v2zM3 6v2h18V6H3zm0 7h12v-2H3v2z" to false).also { _sort = it }

private var _star: ImageVector? = null
val Icons.Filled.Star: ImageVector
    get() = _star ?: icon("Filled.Star", "M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z" to false).also { _star = it }

private var _starBorder: ImageVector? = null
val Icons.Filled.StarBorder: ImageVector
    get() = _starBorder ?: icon("Filled.StarBorder", "m22 9.24-7.19-.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27 18.18 21l-1.63-7.03L22 9.24zM12 15.4l-3.76 2.27 1-4.28-3.32-2.88 4.38-.38L12 6.1l1.71 4.04 4.38.38-3.32 2.88 1 4.28L12 15.4z" to false).also { _starBorder = it }

private var _storage: ImageVector? = null
val Icons.Filled.Storage: ImageVector
    get() = _storage ?: icon("Filled.Storage", "M2 20h20v-4H2v4zm2-3h2v2H4v-2zM2 4v4h20V4H2zm4 3H4V5h2v2zm-4 7h20v-4H2v4zm2-3h2v2H4v-2z" to false).also { _storage = it }

private var _subtitles: ImageVector? = null
val Icons.Filled.Subtitles: ImageVector
    get() = _subtitles ?: icon("Filled.Subtitles", "M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zM4 12h4v2H4v-2zm10 6H4v-2h10v2zm6 0h-4v-2h4v2zm0-4H10v-2h10v2z" to false).also { _subtitles = it }

private var _swapVert: ImageVector? = null
val Icons.Filled.SwapVert: ImageVector
    get() = _swapVert ?: icon("Filled.SwapVert", "M16 17.01V10h-2v7.01h-3L15 21l4-3.99h-3zM9 3 5 6.99h3V14h2V6.99h3L9 3z" to false).also { _swapVert = it }

private var _sync: ImageVector? = null
val Icons.Filled.Sync: ImageVector
    get() = _sync ?: icon("Filled.Sync", "M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46A7.93 7.93 0 0 0 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74A7.93 7.93 0 0 0 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3z" to false).also { _sync = it }

private var _tune: ImageVector? = null
val Icons.Filled.Tune: ImageVector
    get() = _tune ?: icon("Filled.Tune", "M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z" to false).also { _tune = it }

private var _tv: ImageVector? = null
val Icons.Filled.Tv: ImageVector
    get() = _tv ?: icon("Filled.Tv", "M21 3H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h5v2h8v-2h5c1.1 0 1.99-.9 1.99-2L23 5c0-1.1-.9-2-2-2zm0 14H3V5h18v12z" to false).also { _tv = it }

private var _videoLibrary: ImageVector? = null
val Icons.Filled.VideoLibrary: ImageVector
    get() = _videoLibrary ?: icon("Filled.VideoLibrary", "M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-8 12.5v-9l6 4.5-6 4.5z" to false).also { _videoLibrary = it }

private var _visibility: ImageVector? = null
val Icons.Filled.Visibility: ImageVector
    get() = _visibility ?: icon("Filled.Visibility", "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z" to false).also { _visibility = it }

private var _visibilityOff: ImageVector? = null
val Icons.Filled.VisibilityOff: ImageVector
    get() = _visibilityOff ?: icon("Filled.VisibilityOff", "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46A11.804 11.804 0 0 0 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78 3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z" to false).also { _visibilityOff = it }

private var _wifi: ImageVector? = null
val Icons.Filled.Wifi: ImageVector
    get() = _wifi ?: icon("Filled.Wifi", "m1 9 2 2c4.97-4.97 13.03-4.97 18 0l2-2C16.93 2.93 7.08 2.93 1 9zm8 8 3 3 3-3a4.237 4.237 0 0 0-6 0zm-4-4 2 2a7.074 7.074 0 0 1 10 0l2-2C15.14 9.14 8.87 9.14 5 13z" to false).also { _wifi = it }
