package com.reactonelle.bridge.handlers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.reactonelle.bridge.BridgeHandler
import org.json.JSONObject

/**
 * Handler para abrir URL no browser externo
 * Payload: { url: string }
 * Retorna: null
 */
class UrlOpenHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = payload.optString("url", "")
        
        if (url.isEmpty()) {
            onError("Missing 'url' parameter")
            return
        }
        
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            onSuccess(null)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to open URL")
        }
    }
}

/**
 * Handler para verificar se pode abrir URL
 * Payload: { url: string }
 * Retorna: { canOpen: boolean }
 */
class UrlCanOpenHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = payload.optString("url", "")
        
        if (url.isEmpty()) {
            onError("Missing 'url' parameter")
            return
        }
        
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            val canOpen = intent.resolveActivity(context.packageManager) != null
            onSuccess(JSONObject().put("canOpen", canOpen))
        } catch (e: Exception) {
            onSuccess(JSONObject().put("canOpen", false))
        }
    }
}
