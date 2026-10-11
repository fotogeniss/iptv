package com.prelude.iptv.ui.mobile.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import com.prelude.iptv.ui.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prelude.iptv.R
import com.prelude.iptv.ui.AppFonts
import com.prelude.iptv.ui.IptvColors

@Composable
internal fun MobileSettingsFlowHeader(title: String, onBack: () -> Unit) {
    // Horizon: πίσω αριστερά και τίτλος με τη display γραμματοσειρά, χωρίς γραμμή από κάτω.
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(start = 6.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back), tint = IptvColors.TextPrimary)
        }
        Spacer(Modifier.size(2.dp))
        Text(
            title,
            color = IptvColors.TextPrimary,
            fontFamily = AppFonts.Display,
            fontSize = 25.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
