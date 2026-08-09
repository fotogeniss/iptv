package com.prelude.iptv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.data.PlaylistStore
import com.prelude.iptv.ui.localization.localizedProfileName
import com.prelude.iptv.ui.profile.ProfilePresentationPolicy

private val ProfileBackground = IptvColors.Background
private val ProfileSurface = IptvColors.Surface
private val ProfileLine = IptvColors.Divider

/**
 * TV-first profile chooser shown before the catalog is opened.
 * It intentionally does not load artwork or streams, keeping startup cheap and deterministic.
 */
@Composable
fun PremiumProfileGate(
    profiles: List<PlaylistStore.Profile>,
    activeProfileId: Int,
    pinRequired: (PlaylistStore.Profile) -> Boolean,
    verifyPin: (String) -> Boolean,
    onOpenProfile: (PlaylistStore.Profile) -> Unit,
    onManageProfiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pending by remember { mutableStateOf<PlaylistStore.Profile?>(null) }
    val initialFocus = rememberInitialFocus(key = profiles.map { it.id })

    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(IptvColors.BackgroundRaised, ProfileBackground, ProfileBackground)
                )
            )
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.account_gate_question),
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(36.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(profiles, key = { _, p -> p.id }) { index, profile ->
                    val displayName = localizedProfileName(ProfilePresentationPolicy.displayName(profile))
                    ProfileCard(
                        profile = profile,
                        displayName = displayName,
                        active = profile.id == activeProfileId,
                        modifier = if (index == 0) Modifier.focusRequester(initialFocus) else Modifier,
                        onClick = {
                            if (pinRequired(profile)) pending = profile else onOpenProfile(profile)
                        }
                    )
                }
            }

            Spacer(Modifier.height(30.dp))
            OutlinedButton(
                onClick = onManageProfiles,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.tvFocus(RoundedCornerShape(4.dp), tint = false)
            ) {
                Text(stringResource(R.string.account_gate_manage_profiles), letterSpacing = 1.sp)
            }
        }
    }

    pending?.let { profile ->
        ProfilePinDialog(
            profileName = localizedProfileName(ProfilePresentationPolicy.displayName(profile)),
            verifyPin = verifyPin,
            onSuccess = {
                pending = null
                onOpenProfile(profile)
            },
            onDismiss = { pending = null }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: PlaylistStore.Profile,
    displayName: String,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val palette = remember(profile.id) {
        val sets = listOf(
            listOf(Color(0xFF3652A4), Color(0xFF132454)),
            listOf(Color(0xFF9B3C56), Color(0xFF4C1527)),
            listOf(Color(0xFF2C806A), Color(0xFF123C32)),
            listOf(Color(0xFF7552A4), Color(0xFF33204E)),
            listOf(Color(0xFFA56A2B), Color(0xFF513115))
        )
        sets[kotlin.math.abs(profile.id) % sets.size]
    }

    Column(
        modifier = modifier
            .width(142.dp)
            .clip(RoundedCornerShape(8.dp))
            .tvFocus(RoundedCornerShape(8.dp), tint = false)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(126.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.linearGradient(palette))
                .border(
                    width = if (active) 2.dp else 1.dp,
                    color = if (active) Color.White.copy(alpha = 0.78f) else ProfileLine,
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                displayName.trim().firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontSize = 52.sp,
                fontWeight = FontWeight.Black
            )
            if (profile.protected) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xB8000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            displayName,
            color = if (active) Color.White else Color(0xFFB8B8BE),
            fontSize = 16.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ProfilePinDialog(
    profileName: String,
    verifyPin: (String) -> Boolean,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val focus = rememberInitialFocus()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ProfileSurface,
        title = { Text(stringResource(R.string.account_gate_unlock_profile, profileName), color = Color.White) },
        text = {
            Column {
                Text(stringResource(R.string.account_gate_enter_parental_pin), color = Color(0xFFB8B8BE), fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all(Char::isDigit)) {
                            pin = it
                            error = false
                        }
                    },
                    singleLine = true,
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error,
                    supportingText = { if (error) Text(stringResource(R.string.account_profile_wrong_pin)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                        focusedLabelColor = Color.White,
                        cursorColor = Color.White
                    ),
                    modifier = Modifier.focusRequester(focus)
                )
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = {
                    if (verifyPin(pin)) onSuccess() else error = true
                },
                enabled = pin.length >= 4,
                modifier = Modifier.tvFocus(RoundedCornerShape(4.dp), tint = false)
            ) { Text(stringResource(R.string.account_gate_enter)) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.tvFocus(RoundedCornerShape(4.dp), tint = false)
            ) { Text(stringResource(R.string.settings_cancel)) }
        }
    )
}
