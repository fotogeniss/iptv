package com.prelude.iptv.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.prelude.iptv.MainActivity
import com.prelude.iptv.player.PlayerLaunchRequest

/**
 * Υπενθυμίσεις EPG: «θύμισέ μου το πρόγραμμα Χ». Χτυπάει μια ειδοποίηση όταν
 * ξεκινά το πρόγραμμα, με deep-link για άνοιγμα του καναλιού.
 *
 * ΣΧΕΔΙΑΣΜΟΣ:
 *  - AlarmManager setExactAndAllowWhileIdle: το πρόγραμμα ξεκινά σε ακριβή ώρα·
 *    inexact alarm θα χτύπαγε λεπτά αργότερα (χαμένη αρχή).
 *  - Από Android 12+ το exact alarm θέλει άδεια· αν λείπει, πέφτουμε σε
 *    setWindow (μερικά λεπτά ανοχή) αντί για crash.
 *  - requestCode = hash(channel+start) ώστε διπλή υπενθύμιση να αντικαθιστά,
 *    όχι να διπλασιάζει.
 */
object ReminderScheduler {
    private const val CH_ID = "epg_reminders"
    const val EXTRA_URL = "r_url"
    const val EXTRA_NAME = "r_name"
    const val EXTRA_TITLE = "r_title"

    fun schedule(ctx: Context, ch: Channel, title: String, startMs: Long) {
        ensureChannel(ctx)
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val id = (ch.name + startMs).hashCode()
        val intent = Intent(ctx, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_URL, ch.url)
            putExtra(EXTRA_NAME, ch.name)
            putExtra(EXTRA_TITLE, title)
        }
        val pi = PendingIntent.getBroadcast(
            ctx, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // λίγο πριν ξεκινήσει, ώστε να προλάβει ο χρήστης
        val fireAt = (startMs - 60_000L).coerceAtLeast(System.currentTimeMillis() + 1000)
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms())
                am.setWindow(AlarmManager.RTC_WAKEUP, fireAt, 120_000L, pi)
            else
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
        } catch (e: SecurityException) {
            am.setWindow(AlarmManager.RTC_WAKEUP, fireAt, 120_000L, pi)
        }
    }

    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CH_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CH_ID, "Υπενθυμίσεις EPG", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    fun channelId() = CH_ID
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val name = intent.getStringExtra(ReminderScheduler.EXTRA_NAME) ?: "Κανάλι"
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: ""
        val url = intent.getStringExtra(ReminderScheduler.EXTRA_URL) ?: ""

        // tap στην ειδοποίηση -> άνοιγμα καναλιού απευθείας στον player
        val open = if (url.isNotBlank()) {
            PlayerLaunchRequest(url = url, title = name, kind = "live").toIntent(ctx)
        } else {
            Intent(ctx, MainActivity::class.java)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pi = PendingIntent.getActivity(
            ctx, url.hashCode(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = androidx.core.app.NotificationCompat.Builder(ctx, ReminderScheduler.channelId())
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("▶ $title")
            .setContentText("Ξεκινά τώρα στο $name")
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify((name + title).hashCode(), n)
    }
}
