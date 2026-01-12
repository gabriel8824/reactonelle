package com.reactonelle.bridge.handlers

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.reactonelle.bridge.BridgeHandler
import org.json.JSONObject

/**
 * Handler para esconder o teclado virtual
 */
class KeyboardHideHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (context !is Activity) {
            onError("Context must be an Activity")
            return
        }
        
        Handler(Looper.getMainLooper()).post {
            try {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val currentFocus = context.currentFocus ?: View(context)
                imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
                onSuccess(null)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to hide keyboard")
            }
        }
    }
}

/**
 * Handler para mostrar o teclado virtual
 * Nota: Precisa de um focusable view na tela
 */
class KeyboardShowHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (context !is Activity) {
            onError("Context must be an Activity")
            return
        }
        
        Handler(Looper.getMainLooper()).post {
            try {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                onSuccess(null)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to show keyboard")
            }
        }
    }
}

/**
 * Handler para esconder splash screen
 * Nota: Implementação depende de como splash é configurada
 */
class SplashHideHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (context !is Activity) {
            onError("Context must be an Activity")
            return
        }
        
        Handler(Looper.getMainLooper()).post {
            try {
                // Para Android 12+ com SplashScreen API
                // A splash é removida automaticamente, este handler é para compatibilidade
                // Se você estiver usando uma splash customizada, adicione lógica aqui
                onSuccess(null)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to hide splash")
            }
        }
    }
}
