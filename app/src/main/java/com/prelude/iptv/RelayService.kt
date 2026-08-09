package com.prelude.iptv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.prelude.iptv.data.RelayHub
import com.prelude.iptv.net.RelayServer

/** Foreground service: κρατά τον relay server ζωντανό ώστε η TV να παίζει από MAC. */
class RelayService : Service() {

    private var server: RelayServer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        if (server == null) {
            try {
                server = RelayServer(RelayHub.port).also { it.start(NanoTimeout, false) }
                RelayHub.running = true
            } catch (e: Exception) {
                RelayHub.running = false
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
        server = null
        RelayHub.running = false
    }

    private fun buildNotification(): Notification {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, getString(R.string.notif_relay_channel_name), NotificationManager.IMPORTANCE_LOW
            )
            mgr.createNotificationChannel(ch)
        }
        val ip = RelayHub.localIp()
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CHANNEL_ID) else Notification.Builder(this)
        return builder
            .setContentTitle(getString(R.string.notif_relay_title))
            .setContentText("http://$ip:${RelayHub.port}/playlist.m3u")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "relay_channel"
        private const val NOTIF_ID = 1001
        private const val NanoTimeout = 5000

        fun start(context: Context) {
            val i = Intent(context, RelayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(i) else context.startService(i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RelayService::class.java))
        }
    }
}
