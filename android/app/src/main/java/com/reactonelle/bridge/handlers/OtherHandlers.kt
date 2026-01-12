package com.reactonelle.bridge.handlers

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.reactonelle.bridge.BridgeHandler
import org.json.JSONObject

/**
 * Handler para informações do dispositivo
 */
class DeviceInfoHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val info = JSONObject().apply {
            put("platform", "android")
            put("version", Build.VERSION.RELEASE)
            put("model", Build.MODEL)
        }
        onSuccess(info)
    }
}

/**
 * Handler para leitura do storage (SharedPreferences)
 */
class StorageGetHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val key = payload.optString("key", "")
        if (key.isEmpty()) {
            onError("Missing key parameter")
            return
        }
        
        val prefs = context.getSharedPreferences("reactonelle", Context.MODE_PRIVATE)
        val value = prefs.getString(key, null)
        
        onSuccess(JSONObject().apply {
            put("value", value ?: JSONObject.NULL)
        })
    }
}

/**
 * Handler para escrita no storage (SharedPreferences)
 */
class StorageSetHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val key = payload.optString("key", "")
        val value = payload.optString("value", "")
        
        if (key.isEmpty()) {
            onError("Missing key parameter")
            return
        }
        
        val prefs = context.getSharedPreferences("reactonelle", Context.MODE_PRIVATE)
        prefs.edit().putString(key, value).apply()
        
        onSuccess(null)
    }
}

/**
 * Handler para exibir Toast
 */
class ToastHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val message = payload.optString("message", "")
        if (message.isEmpty()) {
            onError("Missing message parameter")
            return
        }
        
        val duration = if (payload.optString("duration") == "long") {
            Toast.LENGTH_LONG
        } else {
            Toast.LENGTH_SHORT
        }
        
        // Toast precisa rodar na main thread
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, duration).show()
        }
        
        onSuccess(null)
    }
}

/**
 * Handler para compartilhamento
 */
class ShareHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val text = payload.optString("text", "")
        val url = payload.optString("url", "")
        val title = payload.optString("title", "")
        
        val shareText = buildString {
            if (text.isNotEmpty()) append(text)
            if (url.isNotEmpty()) {
                if (isNotEmpty()) append("\n")
                append(url)
            }
        }
        
        if (shareText.isEmpty()) {
            onError("Nothing to share")
            return
        }
        
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            if (title.isNotEmpty()) {
                putExtra(Intent.EXTRA_TITLE, title)
            }
            type = "text/plain"
        }
        
        val shareIntent = Intent.createChooser(sendIntent, title.ifEmpty { "Share" })
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        context.startActivity(shareIntent)
        
        onSuccess(JSONObject().put("success", true))
    }
}

/**
 * Handler para AlertDialog nativo
 * Payload esperado:
 * {
 *   "title": "Título",
 *   "message": "Mensagem",
 *   "buttons": [
 *     { "text": "OK", "style": "default" },
 *     { "text": "Cancelar", "style": "cancel" },
 *     { "text": "Deletar", "style": "destructive" }
 *   ]
 * }
 */
class AlertHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val title = payload.optString("title", "")
        val message = payload.optString("message", "")
        val buttonsArray = payload.optJSONArray("buttons")
        
        if (message.isEmpty() && title.isEmpty()) {
            onError("Missing title or message")
            return
        }
        
        Handler(Looper.getMainLooper()).post {
            val builder = AlertDialog.Builder(context)
            
            if (title.isNotEmpty()) {
                builder.setTitle(title)
            }
            
            if (message.isNotEmpty()) {
                builder.setMessage(message)
            }
            
            // Se não houver botões, adiciona OK padrão
            if (buttonsArray == null || buttonsArray.length() == 0) {
                builder.setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    onSuccess(JSONObject().put("buttonIndex", 0))
                }
            } else {
                // Mapeia botões do payload
                for (i in 0 until minOf(buttonsArray.length(), 3)) {
                    val buttonObj = buttonsArray.optJSONObject(i)
                    val buttonText = buttonObj?.optString("text", "Button $i") ?: "Button $i"
                    val buttonStyle = buttonObj?.optString("style", "default") ?: "default"
                    
                    val listener = { _: android.content.DialogInterface, _: Int ->
                        onSuccess(JSONObject().put("buttonIndex", i))
                    }
                    
                    when (i) {
                        0 -> {
                            // Primeiro botão: positivo (direita)
                            if (buttonStyle == "destructive") {
                                builder.setPositiveButton(buttonText, listener)
                            } else {
                                builder.setPositiveButton(buttonText, listener)
                            }
                        }
                        1 -> {
                            // Segundo botão: negativo (esquerda)
                            builder.setNegativeButton(buttonText, listener)
                        }
                        2 -> {
                            // Terceiro botão: neutro (meio)
                            builder.setNeutralButton(buttonText, listener)
                        }
                    }
                }
            }
            
            // Não permite fechar tocando fora
            builder.setCancelable(false)
            
            try {
                builder.show()
            } catch (e: Exception) {
                onError("Failed to show dialog: ${e.message}")
            }
        }
    }
}

