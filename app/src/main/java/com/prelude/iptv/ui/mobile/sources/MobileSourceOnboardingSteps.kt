package com.prelude.iptv.ui.mobile.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.sources.PlaylistSourceMethod
import com.prelude.iptv.ui.sources.PlaylistSourceSubmissionStage

internal enum class MobileSourceOnboardingStep {
    CHOOSE,
    DETAILS,
    CHECKING,
    SUCCESS,
}

@Composable
internal fun MobileSourceOnboardingTopBar(
    step: MobileSourceOnboardingStep,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(58.dp), contentAlignment = Alignment.Center) {
        if (step == MobileSourceOnboardingStep.DETAILS || step == MobileSourceOnboardingStep.SUCCESS) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.sources_back), tint = IptvColors.TextPrimary)
            }
        }
        MobilePlaylistBrand()
        IconButton(onClick = onHelp, modifier = Modifier.align(Alignment.CenterEnd)) {
            Icon(Icons.Default.HelpOutline, stringResource(R.string.sources_help), tint = IptvColors.TextSecondary)
        }
    }
}

@Composable
internal fun MobileSourceOnboardingProgress(step: MobileSourceOnboardingStep) {
    val completed = when (step) {
        MobileSourceOnboardingStep.CHOOSE -> 1
        MobileSourceOnboardingStep.DETAILS -> 2
        MobileSourceOnboardingStep.CHECKING,
        MobileSourceOnboardingStep.SUCCESS -> 3
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 34.dp, vertical = 12.dp),
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
internal fun MobileSourceChooseStep(
    smartInput: String,
    detectionError: String?,
    onSmartInputChange: (String) -> Unit,
    onDetect: () -> Unit,
    onMethod: (PlaylistSourceMethod) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(top = 25.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.sources_add_eyebrow),
                color = IptvColors.Primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
            )
            Text(
                stringResource(R.string.sources_provider_question_mobile),
                color = IptvColors.TextPrimary,
                fontSize = 29.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                stringResource(R.string.sources_choose_description_mobile),
                color = IptvColors.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 11.dp, start = 12.dp, end = 12.dp),
            )
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp))
                .background(IptvColors.Surface.copy(alpha = .88f))
                .border(1.dp, Color.White.copy(alpha = .18f), RoundedCornerShape(19.dp))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PremiumSourceIcon(Icons.Default.AutoAwesome, selected = true, size = 38)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.sources_smart_paste), color = IptvColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.sources_smart_paste_description), color = IptvColors.TextSecondary, fontSize = 10.sp)
                }
                Text(
                    stringResource(R.string.sources_recommended),
                    color = Color.Black,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White)
                        .padding(horizontal = 7.dp, vertical = 5.dp),
                )
            }
            OutlinedTextField(
                value = smartInput,
                onValueChange = onSmartInputChange,
                placeholder = { Text(stringResource(R.string.sources_paste_hint), fontSize = 12.sp) },
                minLines = 2,
                maxLines = 4,
                isError = detectionError != null,
                supportingText = detectionError?.let { message -> { Text(message, fontSize = 10.sp) } },
                shape = RoundedCornerShape(13.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = IptvColors.TextPrimary,
                    unfocusedTextColor = IptvColors.TextPrimary,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = IptvColors.DividerStrong,
                    focusedContainerColor = IptvColors.SurfaceRaised,
                    unfocusedContainerColor = IptvColors.SurfaceRaised,
                    errorBorderColor = IptvColors.Error,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            )
            Button(
                onClick = onDetect,
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IptvColors.Primary, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 4.dp),
            ) {
                Text(stringResource(R.string.sources_detect_credentials), fontWeight = FontWeight.Black)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 19.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).height(1.dp).background(IptvColors.DividerStrong))
            Text(stringResource(R.string.sources_choose_alternative), color = IptvColors.TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
            Box(Modifier.weight(1f).height(1.dp).background(IptvColors.DividerStrong))
        }

        PlaylistSourceMethod.entries.chunked(2).forEach { methods ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                methods.forEach { method ->
                    MobilePremiumMethodCard(method = method, modifier = Modifier.weight(1f), onClick = { onMethod(method) })
                }
            }
        }
        Text(
            stringResource(R.string.sources_media_player_disclaimer),
            color = IptvColors.TextTertiary,
            fontSize = 9.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 18.dp),
        )
    }
}

@Composable
private fun MobilePremiumMethodCard(
    method: PlaylistSourceMethod,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val content = when (method) {
        PlaylistSourceMethod.URL -> Triple(Icons.Default.Link, stringResource(R.string.sources_method_url_title), stringResource(R.string.sources_method_url_card_subtitle))
        PlaylistSourceMethod.XTREAM -> Triple(Icons.Default.Lock, stringResource(R.string.sources_method_xtream_title), stringResource(R.string.sources_method_xtream_card_subtitle))
        PlaylistSourceMethod.MAC -> Triple(Icons.Default.Dns, stringResource(R.string.sources_method_mac_title), stringResource(R.string.sources_method_mac_card_subtitle))
        PlaylistSourceMethod.FILE -> Triple(Icons.Default.FolderOpen, stringResource(R.string.sources_method_file_title), stringResource(R.string.sources_method_file_card_subtitle))
    }
    Column(
        modifier.height(132.dp).clip(RoundedCornerShape(16.dp))
            .background(IptvColors.Surface.copy(alpha = .8f))
            .border(1.dp, IptvColors.DividerStrong, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(14.dp),
    ) {
        PremiumSourceIcon(content.first)
        Spacer(Modifier.height(13.dp))
        Text(content.second, color = IptvColors.TextPrimary, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(content.third, color = IptvColors.TextTertiary, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
internal fun PremiumSourceIcon(
    icon: ImageVector,
    selected: Boolean = false,
    size: Int = 42,
) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size / 3).dp))
            .background(
                if (selected) Brush.linearGradient(listOf(IptvColors.Primary.copy(alpha = .26f), IptvColors.Primary.copy(alpha = .08f)))
                else Brush.linearGradient(listOf(Color.White.copy(alpha = .09f), Color.White.copy(alpha = .035f))),
            )
            .border(1.dp, if (selected) IptvColors.Primary.copy(alpha = .42f) else Color.White.copy(alpha = .09f), RoundedCornerShape((size / 3).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = IptvColors.TextPrimary, modifier = Modifier.size((size * .52f).dp))
    }
}

@Composable
internal fun MobileSourceSecurityNote() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = .025f))
            .border(1.dp, IptvColors.DividerStrong, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Default.Security, null, tint = IptvColors.Success, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Text(
            stringResource(R.string.sources_security_note),
            color = IptvColors.TextSecondary,
            fontSize = 9.5.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
internal fun MobileSourceCheckingStep(stage: PlaylistSourceSubmissionStage) {
    val activeIndex = stage.ordinal
    Column(
        Modifier.fillMaxWidth().padding(top = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(92.dp).clip(RoundedCornerShape(28.dp))
                .background(IptvColors.Primary.copy(alpha = .13f))
                .border(1.dp, IptvColors.Primary.copy(alpha = .28f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = IptvColors.Primary, strokeWidth = 2.5.dp, modifier = Modifier.size(58.dp))
            Icon(Icons.Default.Dns, null, tint = Color.White, modifier = Modifier.size(27.dp))
        }
        Text(stringResource(R.string.sources_checking_title), color = IptvColors.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 25.dp))
        Text(stringResource(R.string.sources_checking_description), color = IptvColors.TextSecondary, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 9.dp, start = 24.dp, end = 24.dp))
        Column(Modifier.fillMaxWidth().padding(top = 27.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            listOf(
                stringResource(R.string.sources_stage_validating),
                stringResource(R.string.sources_stage_connecting),
                stringResource(R.string.sources_stage_preparing),
            ).forEachIndexed { index, label ->
                val done = index < activeIndex
                val active = index == activeIndex
                Row(
                    Modifier.fillMaxWidth().height(51.dp).clip(RoundedCornerShape(13.dp))
                        .background(IptvColors.Surface.copy(alpha = .8f))
                        .border(1.dp, if (active) Color.White.copy(alpha = .34f) else IptvColors.DividerStrong, RoundedCornerShape(13.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(if (done) IptvColors.Success else Color.White.copy(alpha = .07f)), contentAlignment = Alignment.Center) {
                        if (done) Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(15.dp))
                        else Text("${index + 1}", color = if (active) Color.White else IptvColors.TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(label, color = if (active || done) IptvColors.TextPrimary else IptvColors.TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 11.dp).weight(1f))
                    Text(
                        stringResource(
                            if (done) R.string.sources_completed
                            else if (active) R.string.sources_checking
                            else R.string.sources_waiting
                        ),
                        color = IptvColors.TextTertiary,
                        fontSize = 8.5.sp,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MobileSourceSuccessStep(
    onComplete: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(86.dp).clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF69E79C), Color(0xFF45C77C)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF07130C), modifier = Modifier.size(45.dp))
        }
        Text(stringResource(R.string.sources_connection_ready), color = IptvColors.TextPrimary, fontSize = 29.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 25.dp))
        Text(stringResource(R.string.sources_connection_verified), color = IptvColors.TextSecondary, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp, start = 18.dp, end = 18.dp))
        Spacer(Modifier.height(31.dp))
        Button(
            onClick = onComplete,
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IptvColors.Primary, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(stringResource(R.string.sources_add_and_open), fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
    }
}
