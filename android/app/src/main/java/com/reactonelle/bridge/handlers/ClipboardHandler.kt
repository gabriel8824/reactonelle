package com.reactonelle.bridge.handlers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.reactonelle.bridge.BridgeHandler
import org.json.JSONObject

/**
 * Handler para copiar texto para o clipboard
 * Payload: { text: string }
 * Retorna: null
 */
class ClipboardWriteHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val text = payload.optString("text", "")
        
        if (text.isEmpty()) {
            onError("Missing 'text' parameter")
            return
        }
        
        Handler(Looper.getMainLooper()).post {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Reactonelle", text)
                clipboard.setPrimaryClip(clip)
                onSuccess(null)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to copy to clipboard")
            }
        }
    }
}

/**
 * Handler para ler texto do clipboard
 * Payload: -
 * Retorna: { text: string | null }
 */
class ClipboardReadHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        Handler(Looper.getMainLooper()).post {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                
                val text = if (clipboard.hasPrimaryClip() && 
                    clipboard.primaryClipDescription?.hasMimeType("text/plain") == true) {
                    clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                } else {
                    null
                }
                
                onSuccess(JSONObject().put("text", text ?: JSONObject.NULL))
            } catch (e: Exception) {
                onError(e.message ?: "Failed to read clipboard")
            }
        }
    }
}

/**
 * Handler para verificar se há texto no clipboard
 * Payload: -
 * Retorna: { hasText: boolean }
 */
class ClipboardHasTextHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val hasText = clipboard.hasPrimaryClip() && 
                clipboard.primaryClipDescription?.hasMimeType("text/plain") == true
            
            onSuccess(JSONObject().put("hasText", hasText))
        } catch (e: Exception) {
            onError(e.message ?: "Failed to check clipboard")
        }
    }
}
