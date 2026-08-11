package com.prelude.iptv.ui.route

import android.content.*
import android.os.*
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.*
import androidx.activity.compose.*
import androidx.activity.result.contract.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.*
import coil.compose.*
import com.prelude.iptv.*
import com.prelude.iptv.R
import com.prelude.iptv.data.*
import com.prelude.iptv.ui.*
import com.prelude.iptv.ui.components.library.*
import com.prelude.iptv.ui.design.*
import kotlinx.coroutines.*

@Composable
internal fun PlaylistTab(
    state: AppShellUiState,
    vm: MainViewModel,
    onOpen: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onAdd: (Int) -> Unit
) {
    var confirmDelete by remember { mutableStateOf(-1) }
    Column(Modifier.fillMaxSize()) {
        if (state.playlists.isEmpty()) {
            // Defensive fallback. Root normally routes an empty installation
            // directly to AddPlaylistScreen before the shell is composed.
            LaunchedEffect(Unit) { onAdd(0) }
        } else {
            SourceManagerHeader(sourceCount = state.playlists.size)
            val first = rememberInitialFocus(
                enabled = isTvDevice() && state.playlists.isNotEmpty(),
                key = state.playlists.size
            )
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp)
            ) {
                itemsIndexedPlaylists(state.playlists) { i, pl ->
                    PlaylistCard(pl,
                        modifier = if (i == 0) Modifier.focusRequester(first) else Modifier,
                        onOpen = { onOpen(i) }, onEdit = { onEdit(i) },
                        // ΟΧΙ άμεση διαγραφή: σε TV με D-pad είναι εύκολο λάθος.
                        onDelete = { confirmDelete = i })
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
    if (confirmDelete >= 0) DeleteConfirmDialog(
        name = state.playlists.getOrNull(confirmDelete)?.name ?: "",
        onConfirm = { vm.deletePlaylist(confirmDelete); confirmDelete = -1 },
        onCancel = { confirmDelete = -1 }
    )
}

/**
 * Τίτλος και πλήθος πηγών. Τίποτα άλλο.
 *
 * ΕΔΩ ΗΤΑΝ ΤΟ ΚΟΥΜΠΙ «＋ Νέα πηγή». ΑΦΑΙΡΕΘΗΚΕ.
 *
 * Η οθόνη είχε ΔΥΟ «＋» που έκαναν διαφορετικά πράγματα: αυτό εδώ άνοιγε
 * κατευθείαν την προσθήκη πηγής, ενώ το «＋» της κάτω μπάρας άνοιγε πρώτα ένα
 * φύλλο επιλογών που κατέληγε στην ίδια οθόνη με προεπιλεγμένη καρτέλα. Δύο
 * χειριστήρια, ίδιο εικονίδιο, ίδιος προορισμός, διαφορετικός αριθμός βημάτων.
 * Έμεινε το ένα — αυτό της μπάρας, που είναι σταθερά στο ίδιο σημείο σε κάθε
 * οθόνη — και πηγαίνει απευθείας εκεί που πήγαινε αυτό.
 */
@Composable
private fun SourceManagerHeader(sourceCount: Int) {
    StreamingScreenHeader(
        title = stringResource(R.string.sources_title),
        subtitle = pluralStringResource(R.plurals.sources_active_count, sourceCount, sourceCount),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)
    )
}

/* =============================== tab: Xtream =============================== */
