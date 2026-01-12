package com.reactonelle.bridge.handlers

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.reactonelle.bridge.BridgeHandler
import org.json.JSONObject

/**
 * Handler para feedback háptico (vibração)
 */
class HapticHandler : BridgeHandler {
    
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val type = payload.optString("type", "impact")
        val style = payload.optString("style", "medium")
        
        try {
            val vibrator = getVibrator(context)
            
            if (vibrator == null || !vibrator.hasVibrator()) {
                onError("Vibrator not available")
                return
            }
            
            // Duração e amplitude baseadas no estilo
            val (duration, amplitude) = when (style) {
                "light" -> 30L to 50
                "heavy" -> 100L to 255
                "soft" -> 20L to 30
                "rigid" -> 50L to 200
                else -> 50L to 128 // medium
            }
            
            // Vibra usando a API apropriada para a versão do Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(duration, amplitude)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
            
            onSuccess(null)
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
        }
    }
    
    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
