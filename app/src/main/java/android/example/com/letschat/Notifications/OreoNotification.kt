package android.example.com.letschat.Notifications

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.example.com.letschat.R
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.telephony.AvailableNetworkInfo.PRIORITY_HIGH
import androidx.core.app.NotificationCompat


class OreoNotification(base: Context?) : ContextWrapper(base) {
    private var notificationManager: NotificationManager? = null
    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.enableLights(false)
        channel.enableVibration(true)
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        manager!!.createNotificationChannel(channel)
    }

    val manager: NotificationManager?
        get() {
            if (notificationManager == null) {
                notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            }
            return notificationManager
        }

    @TargetApi(Build.VERSION_CODES.O)
    fun getOreoNotification(
        title: String, body: String,
        pendingIntent: PendingIntent, soundUri: Uri, icon: String
    ): Notification.Builder {
        val bigText: Notification.BigTextStyle = Notification.BigTextStyle()
        return Notification.Builder(applicationContext, CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(icon.toInt())
            .setLargeIcon(BitmapFactory.decodeResource(applicationContext.getResources(), R.drawable.mylogo))
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
    }

    companion object {
        private const val CHANNEL_ID = "android.example.com.letschat"
        private const val CHANNEL_NAME = "letschat"
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
    }
}