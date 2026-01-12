package com.reactonelle.bridge.handlers

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.reactonelle.bridge.BridgeHandler
import org.json.JSONObject

/**
 * Handler para verificar disponibilidade de biometria
 * Payload: -
 * Retorna: { available: boolean, type: string }
 */
class BiometricAvailableHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val biometricManager = BiometricManager.from(context)
            val canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            
            val available = canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
            
            val type = when {
                !available -> "none"
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    // Android 10+ pode ter Face ou Fingerprint
                    "biometric" // Generic, pois não há API direta
                }
                else -> "fingerprint"
            }
            
            val errorMsg = when (canAuthenticate) {
                BiometricManager.BIOMETRIC_SUCCESS -> null
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware"
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware unavailable"
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometrics enrolled"
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Security update required"
                else -> "Biometric not available"
            }
            
            onSuccess(JSONObject().apply {
                put("available", available)
                put("type", type)
                if (errorMsg != null) put("error", errorMsg)
            })
        } catch (e: Exception) {
            onSuccess(JSONObject().apply {
                put("available", false)
                put("type", "none")
                put("error", e.message)
            })
        }
    }
}

/**
 * Handler para autenticação biométrica
 * Payload: { reason?: string, title?: string, cancelText?: string }
 * Retorna: { success: boolean, error?: string }
 * 
 * NOTA: Este handler requer que o contexto seja uma FragmentActivity
 */
class BiometricAuthHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val reason = payload.optString("reason", "Autentique-se para continuar")
        val title = payload.optString("title", "Autenticação Biométrica")
        val cancelText = payload.optString("cancelText", "Cancelar")
        
        if (context !is FragmentActivity) {
            onError("Context must be a FragmentActivity for biometric authentication")
            return
        }
        
        Handler(Looper.getMainLooper()).post {
            try {
                val executor = ContextCompat.getMainExecutor(context)
                
                val callback = object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess(JSONObject().put("success", true))
                    }
                    
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        onSuccess(JSONObject().apply {
                            put("success", false)
                            put("error", errString.toString())
                            put("errorCode", errorCode)
                        })
                    }
                    
                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        // Não chamamos onError aqui, pois o usuário pode tentar novamente
                    }
                }
                
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(reason)
                    .setNegativeButtonText(cancelText)
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                    )
                    .build()
                
                val biometricPrompt = BiometricPrompt(context, executor, callback)
                biometricPrompt.authenticate(promptInfo)
                
            } catch (e: Exception) {
                onError(e.message ?: "Biometric authentication failed")
            }
        }
    }
}
