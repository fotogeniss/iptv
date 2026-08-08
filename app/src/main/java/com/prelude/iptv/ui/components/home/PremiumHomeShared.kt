@file:android.annotation.SuppressLint("ProduceStateDoesNotAssignValue")

package com.prelude.iptv.ui.components.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.TmdbClient
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.CatalogRailSection
import com.prelude.iptv.ui.design.Motion
import com.prelude.iptv.ui.design.motionDuration

@Composable
fun rememberHomeMeta(
    channel: Channel,
    tmdbFor: suspend (Channel) -> TmdbClient.Meta?
): State<TmdbClient.Meta?> = produceState<TmdbClient.Meta?>(
    initialValue = null,
    key1 = channel
) {
    value = tmdbFor(channel)
}

@Composable
fun HomeCinematicBackdrop(
    channel: Channel,
    meta: TmdbClient.Meta?,
    mobile: Boolean,
    modifier: Modifier = Modifier,
    imageOverride: String? = null
) {
    // imageOverride: ο caller (TV home) ελέγχει πότε αλλάζει η εικόνα, ώστε να
    // γίνεται ΕΝΑ crossfade ανά ταινία αντί για logo-πρώτα/backdrop-μετά.
    val image = imageOverride ?: meta?.backdrop?.takeIf(String::isNotBlank) ?: channel.logo
    Box(modifier.fillMaxSize().background(IptvColors.Background)) {
        Crossfade(
            targetState = image,
            animationSpec = tween(motionDuration(Motion.Hero), easing = Motion.EmphasizedEasing),
            label = "homeBackdrop"
        ) { target ->
            if (target.isNotBlank()) {
                AsyncImage(
                    model = target,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                if (mobile) {
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.08f),
                        0.46f to Color.Black.copy(alpha = 0.14f),
                        0.76f to Color.Black.copy(alpha = 0.82f),
                        1f to IptvColors.Background
                    )
                } else {
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.97f),
                        0.42f to Color.Black.copy(alpha = 0.80f),
                        0.72f to Color.Black.copy(alpha = 0.15f),
                        1f to Color.Black.copy(alpha = 0.30f)
                    )
                }
            )
        )
        if (!mobile) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.60f to Color.Black.copy(alpha = 0.12f),
                        0.82f to IptvColors.Background.copy(alpha = 0.90f),
                        1f to IptvColors.Background
                    )
                )
            )
        }
    }
}

fun homeSectionSubtitle(section: CatalogRailSection): String = when (section.id) {
    "continue" -> "Συνέχισε από εκεί που σταμάτησες"
    "my-list" -> "Οι αποθηκευμένες επιλογές σου"
    "trending" -> "Δημοφιλή αυτή την εβδομάδα"
    "new" -> "Ταινίες και σειρές που μόλις προστέθηκαν"
    else -> "Επιλεγμένα για εσένα"
}

fun homeHeroCandidates(channels: List<Channel>): List<Channel> =
    channels.filter { it.logo.isNotBlank() }.take(6).ifEmpty { channels.take(6) }
