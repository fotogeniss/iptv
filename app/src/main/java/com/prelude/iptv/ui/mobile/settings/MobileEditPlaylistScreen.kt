package com.prelude.iptv.ui.mobile.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.data.Playlist
import com.prelude.iptv.R
import com.prelude.iptv.data.PlaylistType
import com.prelude.iptv.ui.IptvColors

@Composable
internal fun MobileEditPlaylistScreen(
    existing: Playlist,
    onBack: () -> Unit,
    onSave: (Playlist) -> Unit
) {
    BackHandler(onBack = onBack)
    var credentialsMode by remember { mutableStateOf(existing.type == PlaylistType.XTREAM) }
    var name by remember { mutableStateOf(existing.name) }
    var url by remember { mutableStateOf(if (existing.type == PlaylistType.M3U) existing.source else "") }
    var domain by remember { mutableStateOf(existing.server) }
    var username by remember { mutableStateOf(existing.username) }
    var password by remember { mutableStateOf(existing.password) }
    val defaultName = stringResource(R.string.sources_default_playlist_name)
    val valid = if (credentialsMode) domain.isNotBlank() && username.isNotBlank() && password.isNotBlank() else url.isNotBlank()

    Column(Modifier.fillMaxSize().background(Color(0xFF050505))) {
        Row(
            Modifier.fillMaxWidth().height(76.dp).border(0.5.dp, IptvColors.Divider).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.sources_back), tint = Color.White) }
            Text(stringResource(R.string.sources_edit_playlist), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 25.dp)
        ) {
            EditModeCard(
                title = stringResource(R.string.sources_url_mode),
                tags = stringResource(R.string.sources_url_mode_tags),
                selected = !credentialsMode,
                icon = { Icon(Icons.Default.Link, null, tint = Color.White) },
                onClick = { credentialsMode = false }
            )
            Spacer(Modifier.height(10.dp))
            EditModeCard(
                title = stringResource(R.string.sources_credentials_mode),
                tags = "Xtream",
                selected = credentialsMode,
                icon = { Icon(Icons.Default.Lock, null, tint = Color.White) },
                onClick = { credentialsMode = true }
            )
            Spacer(Modifier.height(24.dp))

            EditField(stringResource(R.string.sources_playlist_name_optional), name, defaultName) { name = it }
            if (credentialsMode) {
                EditField(stringResource(R.string.sources_xtream_domain), domain, "https://server.example.com") { domain = it }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) { EditField(stringResource(R.string.sources_username), username, "username") { username = it } }
                    Box(Modifier.weight(1f)) { EditField(stringResource(R.string.sources_password), password, "password", password = true) { password = it } }
                }
            } else {
                EditField(stringResource(R.string.sources_playlist_url_m3u_xtream), url, "https://server.example.com/list.m3u", singleLine = false) { url = it }
            }
            Spacer(Modifier.height(42.dp))
            Button(
                onClick = {
                    val updated = if (credentialsMode) {
                        existing.copy(
                            name = name.ifBlank { defaultName },
                            type = PlaylistType.XTREAM,
                            source = "",
                            isUrl = true,
                            server = domain.trim(),
                            username = username.trim(),
                            password = password
                        )
                    } else {
                        existing.copy(
                            name = name.ifBlank { defaultName },
                            type = PlaylistType.M3U,
                            source = url.trim(),
                            isUrl = true,
                            server = "",
                            username = "",
                            password = ""
                        )
                    }
                    onSave(updated)
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IptvColors.Primary, disabledContainerColor = Color(0xFF292929))
            ) {
                Text(stringResource(R.string.sources_save_changes), fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun EditModeCard(
    title: String,
    tags: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color(0xFF101010))
            .border(if (selected) 2.dp else 1.dp, if (selected) IptvColors.Primary else Color.White.copy(alpha = 0.13f), RoundedCornerShape(15.dp))
            .clickable(onClick = onClick).padding(horizontal = 15.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text(tags, color = IptvColors.TextTertiary, fontSize = 9.sp)
        }
        Box(
            Modifier.size(20.dp).border(if (selected) 5.dp else 2.dp, if (selected) IptvColors.Primary else Color(0xFF4B4B58), CircleShape)
        )
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    placeholder: String,
    password: Boolean = false,
    singleLine: Boolean = true,
    onChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 17.dp)) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().then(if (singleLine) Modifier else Modifier.height(105.dp)),
            placeholder = { Text(placeholder, color = IptvColors.TextTertiary, fontSize = 12.sp) },
            singleLine = singleLine,
            visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = IptvColors.Primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.13f),
                cursorColor = IptvColors.Primary,
                focusedContainerColor = Color(0xFF090909),
                unfocusedContainerColor = Color(0xFF090909)
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}
