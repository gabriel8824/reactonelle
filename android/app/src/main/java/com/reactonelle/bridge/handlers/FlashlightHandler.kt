package com.reactonelle.bridge.handlers

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import com.reactonelle.bridge.BridgeHandler
import org.json.JSONObject

/**
 * Handler para controlar a lanterna
 * Payload: { on: boolean }
 * Retorna: { on: boolean }
 */
class FlashlightHandler : BridgeHandler {
    
    companion object {
        private var isFlashlightOn = false
    }
    
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val turnOn = payload.optBoolean("on", !isFlashlightOn) // Toggle se não especificado
        
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull()
            
            if (cameraId == null) {
                onError("No camera available")
                return
            }
            
            cameraManager.setTorchMode(cameraId, turnOn)
            isFlashlightOn = turnOn
            
            onSuccess(JSONObject().put("on", isFlashlightOn))
        } catch (e: CameraAccessException) {
            onError("Camera access error: ${e.message}")
        } catch (e: Exception) {
            onError(e.message ?: "Failed to toggle flashlight")
        }
    }
}

/**
 * Handler para verificar disponibilidade de lanterna
 * Payload: -
 * Retorna: { available: boolean }
 */
class FlashlightAvailableHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val hasFlash = context.packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_CAMERA_FLASH
            )
            onSuccess(JSONObject().put("available", hasFlash))
        } catch (e: Exception) {
            onSuccess(JSONObject().put("available", false))
        }
    }
}
