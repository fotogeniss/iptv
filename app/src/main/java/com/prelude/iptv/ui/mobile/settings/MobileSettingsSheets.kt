package com.prelude.iptv.ui.mobile.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prelude.iptv.R
import com.prelude.iptv.billing.PreludeBilling
import com.prelude.iptv.billing.PremiumTier
import com.prelude.iptv.billing.PurchaseState
import com.prelude.iptv.billing.billingActivity
import com.prelude.iptv.billing.effectivePremiumTier
import com.prelude.iptv.billing.hasQaPremiumOverride
import com.prelude.iptv.ui.IptvColors
import com.prelude.iptv.ui.StreamingRadius
import com.prelude.iptv.ui.components.settings.PremiumSettingsRow
import com.prelude.iptv.ui.components.settings.SettingsSourceUi
import com.prelude.iptv.ui.localization.localizedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileSourcesSheet(
    sources: List<SettingsSourceUi>,
    onDismiss: () -> Unit,
    onOpen: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onRefresh: () -> Unit,
    onAdd: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = IptvColors.BackgroundRaised,
        contentColor = IptvColors.TextPrimary
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(stringResource(R.string.settings_my_sources), fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.settings_sources_secure_device), color = IptvColors.TextTertiary, fontSize = 11.sp)
            Spacer(Modifier.height(14.dp))
            sources.forEach { source ->
                MobileSettingsSourceCard(
                    source = source,
                    onOpen = { onOpen(source.index) },
                    onEdit = { onEdit(source.index) },
                    onDelete = { onDelete(source.index) },
                    onRefresh = if (source.current) onRefresh else null
                )
                Spacer(Modifier.height(9.dp))
            }
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(StreamingRadius.Card),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Add, null)
                Text("  ${stringResource(R.string.settings_add_new_source)}", fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileAddSourceSheet(
    onDismiss: () -> Unit,
    onSelectType: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = IptvColors.SurfaceRaised,
        contentColor = IptvColors.TextPrimary
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.settings_new_source), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.settings_choose_connection_type), color = IptvColors.TextTertiary, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            SourceTypeButton("M3U playlist", stringResource(R.string.sources_method_url_card_subtitle)) { onSelectType(0) }
            SourceTypeButton("Xtream Codes", stringResource(R.string.sources_method_xtream_card_subtitle)) { onSelectType(1) }
            SourceTypeButton("Stalker Portal", stringResource(R.string.sources_method_mac_card_subtitle)) { onSelectType(2) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobilePremiumSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { PreludeBilling.repository(context) }
    val billing by repository.state.collectAsStateWithLifecycle()
    val activity = context.billingActivity()
    val qaAccess = hasQaPremiumOverride()
    val premiumActive = effectivePremiumTier(billing.entitlement.tier) == PremiumTier.PREMIUM
    val premiumOffer = billing.offer
    LaunchedEffect(Unit) { repository.start() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = IptvColors.BackgroundRaised,
        contentColor = IptvColors.TextPrimary
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.billing_premium_title), fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(
                when {
                    qaAccess -> stringResource(R.string.settings_premium_owner_qa)
                    premiumActive -> stringResource(R.string.settings_premium_active_device)
                    else -> stringResource(R.string.settings_premium_all_features)
                },
                color = if (premiumActive) Color(0xFF5AC98B) else IptvColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (premiumActive) FontWeight.Bold else FontWeight.Normal,
            )
            Spacer(Modifier.height(16.dp))
            listOf(
                stringResource(R.string.settings_premium_benefit_sources),
                stringResource(R.string.settings_premium_benefit_multiview),
                stringResource(R.string.settings_premium_benefit_subtitles),
                stringResource(R.string.settings_premium_benefit_profiles),
                stringResource(R.string.settings_premium_benefit_home)
            ).forEach { benefit ->
                Text(
                    "•  $benefit",
                    color = IptvColors.TextPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            billing.message?.let { message ->
                Text(
                    message.localizedText(),
                    color = if (billing.entitlement.state == PurchaseState.PENDING) {
                        Color(0xFFFFC857)
                    } else {
                        IptvColors.TextSecondary
                    },
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
                Spacer(Modifier.height(12.dp))
            }
            Button(
                onClick = { activity?.let(repository::launchPremiumPurchase) },
                enabled = !premiumActive && !billing.working && activity != null && premiumOffer != null,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(StreamingRadius.Card),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = IptvColors.SurfaceRaised,
                    disabledContentColor = IptvColors.TextTertiary,
                ),
            ) {
                if (billing.working) {
                    CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text(
                        when {
                            qaAccess -> stringResource(R.string.settings_premium_qa_unlocked)
                            premiumActive -> stringResource(R.string.settings_premium_active)
                            premiumOffer != null -> stringResource(R.string.settings_premium_buy, premiumOffer.formattedPrice)
                            else -> stringResource(R.string.settings_premium_unavailable)
                        },
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            OutlinedButton(
                onClick = repository::restorePurchases,
                enabled = !billing.working && !qaAccess,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(StreamingRadius.Card),
            ) {
                Text(stringResource(R.string.settings_restore_purchases), fontWeight = FontWeight.Bold)
            }
            Text(
                stringResource(R.string.settings_premium_google_play_note),
                color = IptvColors.TextTertiary,
                fontSize = 9.sp,
                modifier = Modifier.padding(top = 9.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileInfoSheet(info: MobileSettingsInfo, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = IptvColors.BackgroundRaised,
        contentColor = IptvColors.TextPrimary
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            val title = when (info) {
                MobileSettingsInfo.Help -> stringResource(R.string.settings_help_center)
            }
            val body = when (info) {
                MobileSettingsInfo.Help -> stringResource(R.string.settings_help_body)
            }
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Text(body, color = IptvColors.TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SourceTypeButton(title: String, subtitle: String, onClick: () -> Unit) {
    PremiumSettingsRow(
        title = title,
        subtitle = subtitle,
        icon = Icons.Default.Add,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 5.dp)
    )
}

internal enum class MobileSettingsInfo { Help }
