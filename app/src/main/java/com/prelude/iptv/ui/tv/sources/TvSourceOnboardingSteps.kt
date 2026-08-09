package com.prelude.iptv.ui.tv.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.sources.PlaylistSourceMethod
import com.prelude.iptv.ui.sources.PlaylistSourceSubmissionStage

internal enum class TvSourceOnboardingStep {
    CHOOSE,
    DETAILS,
    CHECKING,
    SUCCESS,
}

@Composable
internal fun TvSourceOnboardingTopBar(
    step: TvSourceOnboardingStep,
    backFocus: FocusRequester,
    helpFocus: FocusRequester,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
        if (step == TvSourceOnboardingStep.DETAILS || step == TvSourceOnboardingStep.SUCCESS) {
            TvPlaylistAction(
                label = stringResource(R.string.sources_back),
                primary = false,
                focusRequester = backFocus,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                modifier = Modifier.align(Alignment.CenterStart).size(width = 118.dp, height = 48.dp),
                onClick = onBack,
            )
        }
        TvPlaylistBrand()
        TvPlaylistAction(
            label = stringResource(R.string.sources_help),
            primary = false,
            focusRequester = helpFocus,
            icon = Icons.Default.HelpOutline,
            modifier = Modifier.align(Alignment.CenterEnd).size(width = 126.dp, height = 48.dp),
            onClick = onHelp,
        )
    }
}

@Composable
internal fun TvSourceOnboardingProgress(step: TvSourceOnboardingStep) {
    val completed = when (step) {
        TvSourceOnboardingStep.CHOOSE -> 1
        TvSourceOnboardingStep.DETAILS -> 2
        TvSourceOnboardingStep.CHECKING,
        TvSourceOnboardingStep.SUCCESS -> 3
    }
    Row(
        Modifier.fillMaxWidth(.38f).padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        repeat(3) { index ->
            Box(
                Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(99.dp))
                    .background(if (index < completed) IptvColors.Primary else Color.White.copy(alpha = .11f)),
            )
        }
    }
}

@Composable
internal fun TvSourceChooseStep(
    selectedMethod: PlaylistSourceMethod,
    methodFocus: Map<PlaylistSourceMethod, FocusRequester>,
    onMethod: (PlaylistSourceMethod) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.sources_add_eyebrow), color = IptvColors.Primary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp)
        Text(
            stringResource(R.string.sources_provider_question),
            color = IptvColors.TextPrimary,
            fontSize = 39.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 11.dp),
        )
        Text(
            stringResource(R.string.sources_choose_description),
            color = IptvColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )
        Row(
            Modifier.fillMaxWidth().weight(1f).padding(top = 43.dp, bottom = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PlaylistSourceMethod.entries.forEachIndexed { index, method ->
                TvPlaylistMethodCard(
                    method = method,
                    selected = selectedMethod == method,
                    focusRequester = methodFocus.getValue(method),
                    modifier = Modifier.weight(1f).fillMaxHeight().focusProperties {
                        if (index > 0) left = methodFocus.getValue(PlaylistSourceMethod.entries[index - 1])
                        if (index < PlaylistSourceMethod.entries.lastIndex) right = methodFocus.getValue(PlaylistSourceMethod.entries[index + 1])
                    },
                    onSelect = { onMethod(method) },
                )
            }
        }
        Text(
            stringResource(R.string.sources_media_player_disclaimer),
            color = IptvColors.TextTertiary,
            fontSize = 9.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

@Composable
internal fun TvSourceCheckingStep(stage: PlaylistSourceSubmissionStage) {
    val activeIndex = stage.ordinal
    Row(
        Modifier.fillMaxSize().padding(horizontal = 150.dp),
        horizontalArrangement = Arrangement.spacedBy(85.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(.82f), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(116.dp).clip(RoundedCornerShape(34.dp))
                    .background(IptvColors.Primary.copy(alpha = .13f))
                    .border(1.dp, IptvColors.Primary.copy(alpha = .28f), RoundedCornerShape(34.dp)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = IptvColors.Primary, strokeWidth = 3.dp, modifier = Modifier.size(76.dp))
                Icon(Icons.Default.Dns, null, tint = Color.White, modifier = Modifier.size(35.dp))
            }
            Text(stringResource(R.string.sources_checking_title), color = IptvColors.TextPrimary, fontSize = 31.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 25.dp))
            Text(stringResource(R.string.sources_checking_description_tv), color = IptvColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Column(Modifier.weight(1.18f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                stringResource(R.string.sources_stage_validating),
                stringResource(R.string.sources_stage_connecting),
                stringResource(R.string.sources_stage_preparing),
            ).forEachIndexed { index, label ->
                val done = index < activeIndex
                val active = index == activeIndex
                Row(
                    Modifier.fillMaxWidth().height(68.dp).clip(RoundedCornerShape(15.dp))
                        .background(IptvColors.Surface.copy(alpha = .82f))
                        .border(1.dp, if (active) Color.White.copy(alpha = .36f) else IptvColors.DividerStrong, RoundedCornerShape(15.dp))
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(30.dp).clip(RoundedCornerShape(15.dp))
                            .background(if (done) IptvColors.Success else Color.White.copy(alpha = .07f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        else Text("${index + 1}", color = if (active) Color.White else IptvColors.TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(label, color = if (active || done) IptvColors.TextPrimary else IptvColors.TextTertiary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 14.dp).weight(1f))
                    Text(
                        stringResource(
                            if (done) R.string.sources_completed
                            else if (active) R.string.sources_checking
                            else R.string.sources_waiting
                        ),
                        color = IptvColors.TextTertiary,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
internal fun TvSourceSuccessStep(
    completeFocus: FocusRequester,
    onComplete: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(98.dp).clip(RoundedCornerShape(31.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF69E79C), Color(0xFF45C77C)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF07130C), modifier = Modifier.size(53.dp))
        }
        Text(stringResource(R.string.sources_connection_ready), color = IptvColors.TextPrimary, fontSize = 36.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 25.dp))
        Text(stringResource(R.string.sources_connection_verified), color = IptvColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        TvPlaylistAction(
            label = stringResource(R.string.sources_add_and_open),
            primary = true,
            focusRequester = completeFocus,
            modifier = Modifier.padding(top = 31.dp).size(width = 310.dp, height = 56.dp),
            onClick = onComplete,
        )
    }
}
