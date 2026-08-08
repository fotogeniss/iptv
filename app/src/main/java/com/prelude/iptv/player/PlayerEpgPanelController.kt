package com.prelude.iptv.player

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.prelude.iptv.data.Channel
import com.prelude.iptv.data.EpgManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Owns the player-side EPG panel and XMLTV replacement UI. */
internal class PlayerEpgPanelController(
    private val activity: ComponentActivity,
    private val container: LinearLayout,
    private val launchRequest: PlayerLaunchRequest,
    private val tvgId: () -> String,
    private val currentChannel: () -> Channel?,
    private val showStatus: (String, Long) -> Unit,
    private val refreshProgramMetadata: (Channel) -> Unit,
    private val dp: (Int) -> Int,
) {
    fun showMenu() {
        val source = EpgManager.currentSource()
        val sourceLabel = if (source.isNullOrBlank()) {
            "Πηγή: EPG από το portal (short EPG)"
        } else {
            "Πηγή: XMLTV\n$source"
        }
        val options = arrayOf("🌐 Κατέβασε XMLTV από URL…", "🔄 Ανανέωση από XMLTV (τρέχον)")
        AlertDialog.Builder(activity)
            .setTitle("EPG")
            .setMessage(sourceLabel)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showUrlDialog()
                    1 -> if (!source.isNullOrBlank()) {
                        loadXmltv(source)
                    } else {
                        Toast.makeText(activity, "Δεν υπάρχει XMLTV — βάλε URL", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    fun buildInitialSchedule() {
        val titles = launchRequest.epgTitles
        if (titles.isEmpty()) return
        val times = launchRequest.epgTimes
        val descriptions = launchRequest.epgDescriptions

        container.addView(textView("Πρόγραμμα", 20f, bold = true).apply {
            setPadding(0, 8, 0, 12)
        })
        for (index in titles.indices) {
            val isNow = index == 0
            val row = scheduleRow(isNow)
            row.addView(TextView(activity).apply {
                text = if (isNow) "● LIVE" else formatEpgTime(times.getOrElse(index) { "" })
                setTextColor(if (isNow) Color.parseColor("#E11D2A") else Color.parseColor("#A8A8B3"))
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(160, ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            val column = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
            column.addView(textView(titles[index], if (isNow) 15f else 14f, bold = isNow))
            val description = descriptions.getOrElse(index) { "" }
            if (description.isNotBlank()) addExpandableDescription(row, column, description, if (isNow) 4 else 2)
            row.addView(column)
            addRow(row)
        }
    }

    private fun showUrlDialog() {
        val input = EditText(activity).apply {
            hint = "https://.../epg.xml ή .xml.gz"
            setText(EpgManager.currentSource() ?: "")
            setTextColor(Color.WHITE)
        }
        AlertDialog.Builder(activity)
            .setTitle("XMLTV EPG URL")
            .setView(input)
            .setPositiveButton("Κατέβασμα") { _, _ -> loadXmltv(input.text.toString().trim()) }
            .setNegativeButton("Άκυρο", null)
            .show()
    }

    private fun loadXmltv(url: String) {
        if (url.isBlank()) return
        showStatus("Κατέβασμα EPG…", 2_200L)
        activity.lifecycleScope.launch {
            val candidate = try {
                withContext(Dispatchers.IO) { EpgManager.fetchSnapshot(url) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (candidate == null) {
                showStatus("Απέτυχε το κατέβασμα EPG.", 3_500L)
                return@launch
            }
            EpgManager.installSnapshot(candidate)
            showStatus("✓ EPG φορτώθηκε", 2_200L)
            currentChannel()?.let(refreshProgramMetadata)
            rebuildFromXmltv()
        }
    }

    /**
     * Ξαναχτίζει το πρόγραμμα για το ΤΡΕΧΟΝ κανάλι. Καλείται σε κάθε zap —
     * αλλιώς το πάνελ έμενε κολλημένο στο EPG του πρώτου καναλιού που άνοιξε
     * τον player (το buildInitialSchedule διαβάζει μόνο το launchRequest).
     * Σιωπηλό: χωρίς toasts, κατάλληλο για συνεχές ζάπινγκ.
     */
    fun refreshSchedule(explicitTvgId: String? = null) {
        val channelId = explicitTvgId ?: tvgId()
        val programmes = if (channelId.isBlank()) emptyList() else EpgManager.upcoming(channelId, 12)
        if (programmes.isEmpty()) {
            // Νέο κανάλι χωρίς δεδομένα: καθάρισε — μην αφήνεις το πρόγραμμα του προηγούμενου.
            container.removeAllViews()
            container.addView(textView("Πρόγραμμα", 16f, bold = true).apply { setPadding(0, 8, 0, 12) })
            container.addView(textView("Δεν υπάρχει διαθέσιμο πρόγραμμα για αυτό το κανάλι.", 12f, color = "#9A9AA6"))
            return
        }
        renderProgrammes(programmes, "Πρόγραμμα")
    }

    private fun rebuildFromXmltv() {
        val channelId = tvgId()
        if (channelId.isBlank()) {
            Toast.makeText(activity, "Το κανάλι δεν έχει tvg-id για αντιστοίχιση.", Toast.LENGTH_LONG).show()
            return
        }
        val programmes = EpgManager.upcoming(channelId, 12)
        if (programmes.isEmpty()) {
            Toast.makeText(activity, "Δεν βρέθηκε πρόγραμμα για αυτό το tvg-id.", Toast.LENGTH_LONG).show()
            return
        }
        renderProgrammes(programmes, "Πρόγραμμα (XMLTV)")
    }

    private fun renderProgrammes(programmes: List<EpgManager.Prog>, header: String) {
        container.removeAllViews()
        container.addView(textView(header, 16f, bold = true).apply {
            setPadding(0, 8, 0, 12)
        })
        val now = System.currentTimeMillis()
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        for (programme in programmes) {
            val isNow = now in programme.startMs until programme.stopMs
            val row = scheduleRow(isNow)
            row.addView(TextView(activity).apply {
                text = if (isNow) "● LIVE" else formatter.format(Date(programme.startMs))
                setTextColor(if (isNow) Color.parseColor("#E11D2A") else Color.parseColor("#A8A8B3"))
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(160, ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            val column = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
            column.addView(textView(programme.title, if (isNow) 15f else 14f, bold = isNow))
            if (programme.desc.isNotBlank()) {
                column.addView(textView(programme.desc, 12f, color = "#6B6B76").apply {
                    maxLines = if (isNow) 4 else 2
                })
            }
            row.addView(column)
            addRow(row)
        }
    }

    private fun addExpandableDescription(
        row: LinearLayout,
        column: LinearLayout,
        description: String,
        collapsedLines: Int,
    ) {
        val descriptionView = textView(description, 12f, color = "#9A9AA6").apply {
            maxLines = collapsedLines
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        column.addView(descriptionView)
        val hint = textView("▾ Περισσότερα", 11f, color = "#FFE11D2A").apply {
            setPadding(0, dp(3), 0, 0)
            visibility = View.GONE
        }
        column.addView(hint)
        descriptionView.post {
            val layout = descriptionView.layout ?: return@post
            if (layout.text.length < description.length) {
                hint.visibility = View.VISIBLE
                row.isFocusable = true
                row.isClickable = true
                row.setOnClickListener {
                    val expanded = descriptionView.maxLines != collapsedLines
                    descriptionView.maxLines = if (expanded) collapsedLines else Int.MAX_VALUE
                    hint.text = if (expanded) "▾ Περισσότερα" else "▴ Λιγότερα"
                }
                row.setOnFocusChangeListener { view, focused ->
                    view.setBackgroundColor(if (focused) Color.parseColor("#33FFFFFF") else Color.TRANSPARENT)
                }
            }
        }
    }

    private fun scheduleRow(isNow: Boolean) = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(10), dp(12), dp(10), dp(12))
        background = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(Color.parseColor(if (isNow) "#14FFFFFF" else "#08000000"))
        }
    }

    private fun addRow(row: LinearLayout) {
        container.addView(row)
        container.addView(View(activity).apply {
            setBackgroundColor(Color.parseColor("#2A2A30"))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
        })
    }

    private fun textView(
        value: String,
        sizeSp: Float,
        bold: Boolean = false,
        color: String = "#FFFFFF",
    ) = TextView(activity).apply {
        text = value
        setTextColor(Color.parseColor(color))
        textSize = sizeSp
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun formatEpgTime(value: String): String {
        if (value.isBlank()) return ""
        Regex("""^\d{8}(\d{2})(\d{2})""").find(value)?.let {
            return "${it.groupValues[1]}:${it.groupValues[2]}"
        }
        Regex("""(\d{2}:\d{2})""").find(value)?.let { return it.groupValues[1] }
        value.toLongOrNull()?.let { timestamp ->
            val millis = if (timestamp < 10_000_000_000L) timestamp * 1000 else timestamp
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
        }
        return value.take(5)
    }
}
