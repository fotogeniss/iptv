package com.prelude.iptv.ui.mobile.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.ui.IptvColors
import kotlinx.coroutines.delay

@Composable
internal fun MobileAccountSyncScreen(
    profileName: String,
    onManageProfiles: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var page by remember { mutableIntStateOf(0) }
    var dragX by remember { mutableFloatStateOf(0f) }
    val pages = listOf(
        AccountPage(Icons.Default.AccountCircle, "Ένας λογαριασμός", "Οι πηγές, τα αγαπημένα και η πρόοδός σου σε ένα προσωπικό προφίλ."),
        AccountPage(Icons.Default.Devices, "Σε όλες τις συσκευές", "Συνέχισε από εκεί που σταμάτησες σε κινητό, tablet και τηλεόραση."),
        AccountPage(Icons.Default.Sync, "Τα δεδομένα σου μαζί σου", "Συγχρόνισε προτιμήσεις και Premium κατάσταση με ασφαλή τρόπο.")
    )
    val current = pages[page]

    LaunchedEffect(page) {
        delay(5_000)
        page = AccountCarouselPolicy.nextPage(page, pages.size)
    }

    Column(
        modifier.fillMaxSize().background(Color(0xFF050505)).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth().height(76.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω", tint = Color.White) }
            Spacer(Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayArrow, null, tint = IptvColors.Primary, modifier = Modifier.size(38.dp))
            Text("PRELUDE+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(30.dp))
        Text("Ο media player σου", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text("Σε όλες τις συσκευές", color = Color(0xFFD5D5D5), fontSize = 21.sp)
        Spacer(Modifier.height(28.dp))
        Box(
            Modifier.fillMaxWidth().weight(1f)
                .pointerInput(page) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragX = 0f },
                        onHorizontalDrag = { _, amount -> dragX += amount },
                        onDragEnd = { page = AccountCarouselPolicy.pageAfterSwipe(page, dragX, pages.size) },
                        onDragCancel = { dragX = 0f }
                    )
                }
                .clip(RoundedCornerShape(28.dp)).background(
                Brush.radialGradient(listOf(Color(0xFF35151A), Color(0xFF11131C), Color(0xFF070707)))
            ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Box(
                    Modifier.size(132.dp).background(Color.White.copy(alpha = 0.07f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(current.icon, null, tint = Color.White, modifier = Modifier.size(72.dp)) }
                Spacer(Modifier.height(24.dp))
                Text(current.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(9.dp))
                Text(current.body, color = Color(0xFFD0D0D0), fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.padding(vertical = 15.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            pages.indices.forEach { index ->
                Box(
                    Modifier.size(if (index == page) 9.dp else 6.dp)
                        .background(if (index == page) Color.White else Color(0xFF444444), CircleShape)
                        .clickable { page = index }
                )
            }
        }
        Button(
            onClick = onManageProfiles,
            modifier = Modifier.fillMaxWidth().height(49.dp),
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IptvColors.Primary)
        ) { Text("Διαχείριση προφίλ $profileName", fontWeight = FontWeight.Black) }
        TextButton(onClick = onBack, modifier = Modifier.padding(bottom = 10.dp)) {
            Text("Πίσω", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

private data class AccountPage(val icon: ImageVector, val title: String, val body: String)
