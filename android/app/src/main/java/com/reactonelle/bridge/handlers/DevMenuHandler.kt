package com.reactonelle.bridge.handlers

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.reactonelle.bridge.BridgeHandler
import com.reactonelle.debug.DevMenu
import org.json.JSONObject

/**
 * Handler para habilitar/desabilitar o DevMenu via JavaScript
 * Payload: { enabled: boolean }
 * Retorna: { enabled: boolean }
 */
class DevMenuToggleHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val enabled = payload.optBoolean("enabled", true)
        
        Handler(Looper.getMainLooper()).post {
            DevMenu.isEnabled = enabled
            onSuccess(JSONObject().put("enabled", DevMenu.isEnabled))
        }
    }
}

/**
 * Handler para mostrar o DevMenu programaticamente
 * Útil para adicionar um botão de debug na UI
 */
class DevMenuShowHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        // Este handler precisa ser chamado de forma especial
        // pois precisa de referência ao DevMenu da MainActivity
        // Por enquanto, apenas retorna status
        onSuccess(JSONObject().apply {
            put("enabled", DevMenu.isEnabled)
            put("message", "Use shake gesture to open DevMenu")
        })
    }
}
