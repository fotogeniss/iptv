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
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.*
import coil.compose.*
import com.prelude.iptv.*
import com.prelude.iptv.player.PlayerLaunchRequest
import com.prelude.iptv.data.*
import com.prelude.iptv.ui.*
import com.prelude.iptv.ui.components.library.*
import com.prelude.iptv.ui.design.*
import kotlinx.coroutines.*

internal fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

/** Catch-up: παίζει το αρχειοθετημένο πρόγραμμα (timeshift URL) ως VOD ώστε
 *  να έχει μπάρα/seek — δεν είναι live, είναι ηχογραφημένο κομμάτι. */
internal fun openCatchup(ctx: Context, ch: Channel, progTitle: String, url: String) {
    val catchupChannel = ch.copy(name = "${ch.name} — $progTitle", kind = "vod")
    ctx.startActivity(
        PlayerLaunchRequest.forChannel(
            url = url,
            channel = catchupChannel,
            sourceId = PlaybackQueue.sourceId,
            positionKey = "", // catch-up δεν αποθηκεύει θέση
        ).toIntent(ctx)
    )
}

// Εδώ βρισκόταν το `openChannel`, που άνοιγε το PlayerActivity σε ξεχωριστό
// παράθυρο. Όλες οι διαδρομές του καταλόγου —αρχική, βιβλιοθήκη, αναζήτηση,
// ζωντανά— περνούν πλέον από το κοινό επίπεδο αναπαραγωγής (`playChannel` στο
// BrowseRoute), οπότε δεν το καλούσε κανείς.
//
// Το `PlayerActivity` ΔΕΝ καταργείται: παραμένει ο προορισμός για ό,τι έρχεται
// από έξω με Intent — ειδοποιήσεις υπενθύμισης, κανάλια της αρχικής Android TV,
// δοκιμή ροής κατά την εισαγωγή πηγής, και catch-up (δες [openCatchup] πιο πάνω).
// Ένα Intent χρειάζεται Activity· δεν μπορεί να προσγειωθεί σε Composable.

/** Secure two-stream launch: the Intent contains only a one-shot opaque token. */
private val multiviewLaunchInFlight = java.util.concurrent.atomic.AtomicBoolean(false)

internal fun openMultiview(
    ctx: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    vm: MainViewModel,
    primary: Channel,
    secondary: Channel,
    toastFn: (String) -> Unit
) {
    if (!multiviewLaunchInFlight.compareAndSet(false, true)) return
    scope.launch {
        try {
            val sourceId = vm.currentSourceId()
            val urls = com.prelude.iptv.source.ProviderResolutionGate.withSource(sourceId) {
                // Session-sensitive providers are deliberately resolved sequentially.
                vm.resolvePlayableUrl(primary) to vm.resolvePlayableUrl(secondary)
            }
            if (urls.first.isBlank()) { toastFn("Το πρώτο stream δεν είναι διαθέσιμο"); return@launch }
            if (urls.second.isBlank()) { toastFn("Το δεύτερο stream δεν είναι διαθέσιμο"); return@launch }
            val token = MultiviewLaunchStore.put(
                MultiviewLaunchStore.Launch(
                    primary = MultiviewLaunchStore.Stream(urls.first, primary.name, primary.logo, sourceId),
                    secondary = MultiviewLaunchStore.Stream(urls.second, secondary.name, secondary.logo, sourceId)
                )
            )
            ctx.startActivity(
                Intent(ctx, MultiviewActivity::class.java)
                    .putExtra(MultiviewActivity.EXTRA_LAUNCH_TOKEN, token)
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            toastFn("Multiview: ${e.message ?: "αποτυχία εκκίνησης"}")
        } finally {
            multiviewLaunchInFlight.set(false)
        }
    }
}
