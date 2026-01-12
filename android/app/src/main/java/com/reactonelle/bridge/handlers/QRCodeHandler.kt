package com.reactonelle.bridge.handlers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.reactonelle.bridge.BridgeHandler
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * Handler para gerar QR Code
 * Payload: { data: string, size?: number }
 * Retorna: { base64: string }
 */
class QRCodeGenerateHandler : BridgeHandler {
    
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val data = payload.optString("data", "")
        val size = payload.optInt("size", 256)
        
        if (data.isEmpty()) {
            onError("Missing 'data' field")
            return
        }
        
        // Gera QR Code em background thread
        Thread {
            try {
                val bitmap = generateQRCode(data, size)
                val base64 = bitmapToBase64(bitmap)
                
                Handler(Looper.getMainLooper()).post {
                    onSuccess(JSONObject().apply {
                        put("base64", "data:image/png;base64,$base64")
                        put("width", size)
                        put("height", size)
                    })
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    onError(e.message ?: "Failed to generate QR Code")
                }
            }
        }.start()
    }
    
    private fun generateQRCode(data: String, size: Int): Bitmap {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            data,
            BarcodeFormat.QR_CODE,
            size,
            size
        )
        
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (bitMatrix[x, y]) {
                    0xFF000000.toInt() // Preto
                } else {
                    0xFFFFFFFF.toInt() // Branco
                }
            }
        }
        
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

/**
 * Handler para escanear QR Code usando ZXing Embedded
 * Payload: { formats?: array } - formatos aceitos (opcional)
 * Retorna: { data: string, format: string }
 */
class QRCodeScanHandler : BridgeHandler {
    
    companion object {
        private var pendingCallback: ((JSONObject?) -> Unit)? = null
        private var pendingErrorCallback: ((String) -> Unit)? = null
        
        /**
         * Chamado pela Activity no onActivityResult
         */
        fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
            val result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            
            if (result != null) {
                if (result.contents != null) {
                    pendingCallback?.invoke(JSONObject().apply {
                        put("data", result.contents)
                        put("format", result.formatName ?: "QR_CODE")
                    })
                } else {
                    pendingErrorCallback?.invoke("Scan cancelled")
                }
                cleanup()
                return true
            }
            
            return false
        }
        
        private fun cleanup() {
            pendingCallback = null
            pendingErrorCallback = null
        }
    }
    
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
                pendingCallback = onSuccess
                pendingErrorCallback = onError
                
                // Usar ZXing Embedded
                val integrator = IntentIntegrator(context)
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                integrator.setPrompt("Aponte para o QR Code")
                integrator.setCameraId(0)
                integrator.setBeepEnabled(true)
                integrator.setBarcodeImageEnabled(false)
                integrator.setOrientationLocked(false)
                integrator.initiateScan()
                
            } catch (e: Exception) {
                onError(e.message ?: "Failed to start QR scanner")
                cleanup()
            }
        }
    }
}
