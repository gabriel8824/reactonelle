package com.reactonelle.bridge.handlers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.reactonelle.MainActivity
import com.reactonelle.R
import com.reactonelle.bridge.BridgeHandler
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

/**
 * Handler para criar notificação local
 * Payload: { title: string, body: string, data?: object }
 * Retorna: { id: number }
 */
class NotificationLocalHandler : BridgeHandler {
    
    companion object {
        private const val CHANNEL_ID = "reactonelle_notifications"
        private const val CHANNEL_NAME = "Reactonelle Notifications"
        private val notificationIdCounter = AtomicInteger(0)
        private var channelCreated = false
    }
    
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val title = payload.optString("title", "")
        val body = payload.optString("body", "")
        
        if (title.isEmpty() && body.isEmpty()) {
            onError("Title or body is required")
            return
        }
        
        // Verifica permissão no Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                onError("Notification permission not granted")
                return
            }
        }
        
        try {
            // Cria canal (necessário no Android 8+)
            createNotificationChannel(context)
            
            val notificationId = notificationIdCounter.incrementAndGet()
            
            // Intent para abrir o app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            // Constrói a notificação
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
            
            // Mostra a notificação
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
            
            onSuccess(JSONObject().put("id", notificationId))
            
        } catch (e: Exception) {
            onError(e.message ?: "Failed to show notification")
        }
    }
    
    private fun createNotificationChannel(context: Context) {
        if (channelCreated) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notificações do Reactonelle"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        
        channelCreated = true
    }
}

/**
 * Handler para cancelar notificação
 * Payload: { id: number }
 */
class NotificationCancelHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val id = payload.optInt("id", -1)
        
        if (id == -1) {
            onError("Missing notification id")
            return
        }
        
        try {
            NotificationManagerCompat.from(context).cancel(id)
            onSuccess(null)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to cancel notification")
        }
    }
}

/**
 * Handler para cancelar todas as notificações
 */
class NotificationCancelAllHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            NotificationManagerCompat.from(context).cancelAll()
            onSuccess(null)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to cancel notifications")
        }
    }
}
