package com.volvo.tflite

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

private const val ALERT_NOTIFICATION_ID = 1
const val SERVICE_NOTIFICATION_ID = 2
private const val ALERT_NOTIFICATION_CHANNEL_ID = "com.volvo.talkingTruck"
private const val SERVICE_NOTIFICATION_CHANNEL_ID = "com.volvo.talkingTruck.service"


fun createNotificationChannel(
        context: Context
) {
    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Talking Truck App"
        val descriptionText = ALERT_NOTIFICATION_CHANNEL_ID
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(ALERT_NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        channel.enableLights(true)
        channel.enableVibration(true)

        val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

        //channel.setSound(NOTIFICATION_SOUND_URI, attributes)
        // Register the channel with the system
        val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

fun sendAlertNotification(
        context: Context,
        title: String,
        text: String,
        noise: Boolean
) {
    val builder = NotificationCompat.Builder(context, ALERT_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(prepareMainActivityPendingIntent(context))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            //.setSound(NOTIFICATION_SOUND_URI)
            .setAutoCancel(true)

//    if (noise) {
//        builder.setSound(NOTIFICATION_SOUND_URI)
//            .setDefaults(Notification.DEFAULT_VIBRATE)
//    }
    with(NotificationManagerCompat.from(context)) {
        // notificationId is a unique int for each notification that you must define
        notify(ALERT_NOTIFICATION_ID, builder.build())
    }
}

@RequiresApi(api = Build.VERSION_CODES.O)
fun prepareForegroundServiceNotification(context: Context): Notification {

    val channelName = "Recognize Speech Service"
    val chan = NotificationChannel(
            SERVICE_NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_NONE
    )
    chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    val manager =
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
    manager.createNotificationChannel(chan)
    val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, SERVICE_NOTIFICATION_CHANNEL_ID)
    return notificationBuilder.setOngoing(true)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(channelName)
            .setContentIntent(prepareMainActivityPendingIntent(context))
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
}

private fun prepareMainActivityPendingIntent(
        context: Context
): PendingIntent {
    // Create an explicit intent for an Activity in your app
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
}