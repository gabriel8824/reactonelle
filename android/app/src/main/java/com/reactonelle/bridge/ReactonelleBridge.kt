package com.reactonelle.bridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.reactonelle.bridge.handlers.*
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * Bridge principal que expõe APIs nativas para o JavaScript
 */
class ReactonelleBridge(
    context: Context,
    webView: WebView
) {
    companion object {
        private const val TAG = "ReactonelleBridge"
    }

    private val contextRef = WeakReference(context)
    private val webViewRef = WeakReference(webView)
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Handlers registrados
    private val handlers: Map<String, BridgeHandler> = mapOf(
        // Existentes
        "haptic" to HapticHandler(),
        "device.info" to DeviceInfoHandler(),
        "storage.get" to StorageGetHandler(),
        "storage.set" to StorageSetHandler(),
        "toast" to ToastHandler(),
        "share" to ShareHandler(),
        "alert" to AlertHandler(),
        
        // Clipboard
        "clipboard.write" to ClipboardWriteHandler(),
        "clipboard.read" to ClipboardReadHandler(),
        "clipboard.hasText" to ClipboardHasTextHandler(),
        
        // URL
        "url.open" to UrlOpenHandler(),
        "url.canOpen" to UrlCanOpenHandler(),
        
        // Sistema
        "app.version" to AppVersionHandler(),
        "battery.status" to BatteryStatusHandler(),
        "network.status" to NetworkStatusHandler(),
        
        // Lanterna
        "flashlight.toggle" to FlashlightHandler(),
        "flashlight.available" to FlashlightAvailableHandler(),
        
        // Biometria
        "biometric.available" to BiometricAvailableHandler(),
        "biometric.authenticate" to BiometricAuthHandler(),
        
        // Status Bar
        "statusbar.style" to StatusBarStyleHandler(),
        "statusbar.hide" to StatusBarHideHandler(),
        "statusbar.show" to StatusBarShowHandler(),
        
        // Teclado
        "keyboard.hide" to KeyboardHideHandler(),
        "keyboard.show" to KeyboardShowHandler(),
        
        // Splash
        "splash.hide" to SplashHideHandler(),
        
        // UI Components
        "actionsheet.show" to ActionSheetHandler(),
        "datepicker.show" to DatePickerHandler(),
        
        // DevMenu (debug)
        "devmenu.toggle" to DevMenuToggleHandler(),
        "devmenu.status" to DevMenuShowHandler(),
        
        // Câmera e Galeria
        "camera.photo" to CameraPhotoHandler(),
        "gallery.pick" to GalleryPickHandler(),
        
        // Localização
        "location.current" to LocationCurrentHandler(),
        
        // Notificações
        "notification.local" to NotificationLocalHandler(),
        "notification.cancel" to NotificationCancelHandler(),
        "notification.cancelAll" to NotificationCancelAllHandler(),
        
        // QR Code
        "qrcode.generate" to QRCodeGenerateHandler(),
        "qrcode.scan" to QRCodeScanHandler(),
        
        // Permissões
        "permission.check" to PermissionCheckHandler(),
        "permission.request" to PermissionRequestHandler(),
        "permission.requestMultiple" to PermissionRequestMultipleHandler(),
        
        // Microfone
        "microphone.start" to MicrophoneStartHandler(),
        "microphone.stop" to MicrophoneStopHandler(),
        
        // Contatos
        "contacts.pick" to ContactsPickHandler(),
        "contacts.getAll" to ContactsGetAllHandler(),
        
        // OneSignal Push Notifications
        "onesignal.login" to OneSignalLoginHandler(),
        "onesignal.logout" to OneSignalLogoutHandler(),
        "onesignal.setTag" to OneSignalSetTagHandler(),
        "onesignal.setTags" to OneSignalSetTagsHandler(),
        "onesignal.deleteTag" to OneSignalDeleteTagHandler(),
        "onesignal.getTags" to OneSignalGetTagsHandler(),
        "onesignal.requestPermission" to OneSignalRequestPermissionHandler(),
        "onesignal.getPermissionStatus" to OneSignalGetPermissionStatusHandler(),
        "onesignal.getSubscriptionId" to OneSignalGetSubscriptionIdHandler(),
        "onesignal.optIn" to OneSignalOptInHandler(),
        "onesignal.optOut" to OneSignalOptOutHandler(),
        "onesignal.addEmail" to OneSignalAddEmailHandler(),
        "onesignal.removeEmail" to OneSignalRemoveEmailHandler()
    )

    /**
     * Método exposto para JavaScript via @JavascriptInterface
     * Chamado por: window.AndroidBridge.call(action, payload, callbackId)
     * 
     * IMPORTANTE: callbackId pode ser um timestamp grande, por isso usamos String
     * para evitar overflow de Int
     */
    @JavascriptInterface
    fun call(action: String, payloadJson: String, callbackId: String) {
        val context = contextRef.get() ?: run {
            Log.e(TAG, "Context is null")
            return
        }

        Log.d(TAG, "═══════════════════════════════════════════")
        Log.d(TAG, "Ação recebida: $action")
        Log.d(TAG, "CallbackId: $callbackId")
        Log.d(TAG, "Payload: $payloadJson")

        try {
            val payload = if (payloadJson.isNotEmpty()) {
                JSONObject(payloadJson)
            } else {
                JSONObject()
            }

            val handler = handlers[action]
            if (handler != null) {
                handler.handle(context, payload,
                    onSuccess = { result ->
                        Log.d(TAG, "Handler success, enviando resposta...")
                        sendResponse(callbackId, true, result)
                    },
                    onError = { error ->
                        Log.e(TAG, "Handler error: $error")
                        sendResponse(callbackId, false, JSONObject().put("error", error))
                    }
                )
            } else {
                Log.w(TAG, "Handler não encontrado para: $action")
                sendResponse(callbackId, false, JSONObject().put("error", "Handler not found: $action"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar ação: $action", e)
            sendResponse(callbackId, false, JSONObject().put("error", e.message))
        }
    }

    /**
     * Envia resposta de volta para o JavaScript
     */
    private fun sendResponse(callbackId: String, success: Boolean, data: JSONObject?) {
        val dataJson = data?.toString() ?: "null"
        
        // callbackId já vem como string numérica do JavaScript, então interpolamos diretamente
        val script = "window.Reactonelle._handleResponse($callbackId, $success, $dataJson);"
        
        Log.d(TAG, "───────────────────────────────────────────")
        Log.d(TAG, "ENVIANDO RESPOSTA PARA JS:")
        Log.d(TAG, "  callbackId: $callbackId")
        Log.d(TAG, "  success: $success")
        Log.d(TAG, "  data: $dataJson")
        Log.d(TAG, "  SCRIPT: $script")

        // Executa imediatamente na main thread
        mainHandler.post {
            val webView = webViewRef.get()
            if (webView == null) {
                Log.e(TAG, "❌ WebView é null!")
                return@post
            }
            
            Log.d(TAG, "▶ Executando evaluateJavascript...")
            
            webView.evaluateJavascript(script) { result ->
                Log.d(TAG, "✅ evaluateJavascript completou. Result: $result")
            }
        }
    }
}

/**
 * Interface base para handlers de ações
 */
interface BridgeHandler {
    fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    )
}

