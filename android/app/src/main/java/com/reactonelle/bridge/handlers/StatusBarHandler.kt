package com.reactonelle.bridge.handlers

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.reactonelle.bridge.BridgeHandler
import org.json.JSONObject

/**
 * Handler para definir estilo da status bar
 * Payload: { style: 'light'|'dark', color?: '#RRGGBB' }
 * - light: ícones escuros (para fundo claro)
 * - dark: ícones claros (para fundo escuro)
 */
class StatusBarStyleHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val style = payload.optString("style", "dark")
        val color = payload.optString("color", "")
        
        if (context !is Activity) {
            onError("Context must be an Activity")
            return
        }
        
        Handler(Looper.getMainLooper()).post {
            try {
                val window = context.window
                val decorView = window.decorView
                
                // Define cor de fundo se especificada
                if (color.isNotEmpty()) {
                    try {
                        window.statusBarColor = Color.parseColor(color)
                    } catch (e: Exception) {
                        // Cor inválida, ignora
                    }
                }
                
                // Define aparência dos ícones
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val controller = window.insetsController
                    if (style == "light") {
                        // Ícones escuros para fundo claro
                        controller?.setSystemBarsAppearance(
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        )
                    } else {
                        // Ícones claros para fundo escuro
                        controller?.setSystemBarsAppearance(
                            0,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        )
                    }
                } else {
                    @Suppress("DEPRECATION")
                    if (style == "light") {
                        decorView.systemUiVisibility = decorView.systemUiVisibility or
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    } else {
                        decorView.systemUiVisibility = decorView.systemUiVisibility and
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                    }
                }
                
                onSuccess(null)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to set status bar style")
            }
        }
    }
}

/**
 * Handler para esconder status bar
 */
class StatusBarHideHandler : BridgeHandler {
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
                val window = context.window
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.insetsController?.hide(android.view.WindowInsets.Type.statusBars())
                } else {
                    @Suppress("DEPRECATION")
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                }
                
                onSuccess(null)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to hide status bar")
            }
        }
    }
}

/**
 * Handler para mostrar status bar
 */
class StatusBarShowHandler : BridgeHandler {
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
                val window = context.window
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.insetsController?.show(android.view.WindowInsets.Type.statusBars())
                } else {
                    @Suppress("DEPRECATION")
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                }
                
                onSuccess(null)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to show status bar")
            }
        }
    }
}
