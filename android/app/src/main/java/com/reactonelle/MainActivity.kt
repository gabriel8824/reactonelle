package com.reactonelle

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.reactonelle.bridge.handlers.PermissionRequestHandler
import com.reactonelle.bridge.handlers.PermissionRequestMultipleHandler
import com.reactonelle.bridge.handlers.QRCodeScanHandler
import com.reactonelle.bridge.handlers.CameraPhotoHandler
import com.reactonelle.bridge.handlers.GalleryPickHandler
import com.reactonelle.bridge.handlers.REQUEST_IMAGE_CAPTURE
import com.reactonelle.bridge.handlers.REQUEST_PICK_IMAGE
import com.reactonelle.bridge.handlers.ContactsPickHandler
import com.reactonelle.bridge.handlers.REQUEST_PICK_CONTACT
import com.reactonelle.debug.DevMenu
import com.reactonelle.debug.ReactonelleDebug
import com.reactonelle.debug.ShakeDetector
import com.reactonelle.webview.ReactonelleWebView

/**
 * MainActivity - ponto de entrada do app
 * Configura a WebView e gerencia navegação
 * 
 * ## Ativar Dev Menu (shake gesture)
 * 
 * Descomente a linha `ReactonelleDebug.enable()` no onCreate:
 * 
 * ```kotlin
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     ReactonelleDebug.enable()  // <-- Ativa Dev Menu
 *     super.onCreate(savedInstanceState)
 *     ...
 * }
 * ```
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var shakeDetector: ShakeDetector? = null
    private var devMenu: DevMenu? = null
    private var currentUrl: String = ""

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ═══════════════════════════════════════════════════════
        // ⚡ ATIVE O DEV MENU DESCOMENTANDO A LINHA ABAIXO:
        ReactonelleDebug.enable()
        // ═══════════════════════════════════════════════════════
        
        // Cria e configura a WebView
        webView = ReactonelleWebView.create(this)
        setContentView(webView)
        
        // Carrega conteúdo
        loadContent()
        
        // Configura tratamento do botão voltar
        setupBackNavigation()
        
        // Configura DevMenu se habilitado
        if (ReactonelleDebug.isEnabled) {
            setupDevMenu()
        }
    }

    private fun loadContent() {
        // Usa URL customizada se definida no ReactonelleDebug
        currentUrl = ReactonelleDebug.devServerUrl ?: if (BuildConfig.USE_LOCAL_SERVER) {
            "https://reactonelle.lovable.app/"
        } else {
            "file:///android_asset/www/index.html"
        }
        
        webView.loadUrl(currentUrl)
    }

    /**
     * Configura o menu de desenvolvedor ativado por shake
     */
    private fun setupDevMenu() {
        devMenu = DevMenu(this, webView, currentUrl)
        
        shakeDetector = ShakeDetector(this) {
            runOnUiThread {
                devMenu?.show()
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    /**
     * Callback para resultados de permissões
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        // Tenta handlers de permissão
        if (PermissionRequestHandler.handlePermissionResult(requestCode, permissions, grantResults)) {
            return
        }
        if (PermissionRequestMultipleHandler.handlePermissionResult(requestCode, permissions, grantResults)) {
            return
        }
    }

    /**
     * Callback para resultados de Activity (QR Scanner, Camera, etc)
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // QR Code Scanner
        if (QRCodeScanHandler.handleActivityResult(requestCode, resultCode, data)) {
            return
        }
        
        // Câmera, Galeria e Contatos
        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                val success = resultCode == RESULT_OK
                val uri = CameraPhotoHandler.getTempUri()
                CameraPhotoHandler.handleActivityResult(this, success, uri)
            }
            REQUEST_PICK_IMAGE -> {
                if (resultCode == RESULT_OK) {
                    GalleryPickHandler.handleActivityResult(data)
                } else {
                    GalleryPickHandler.handleActivityResult(null)
                }
            }
            REQUEST_PICK_CONTACT -> {
                ContactsPickHandler.handleActivityResult(this, data)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        
        // Inicia detecção de shake se Dev Menu está habilitado
        if (ReactonelleDebug.shakeToOpenMenu) {
            shakeDetector?.start()
        }
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        shakeDetector?.stop()
    }

    override fun onDestroy() {
        shakeDetector?.stop()
        webView.destroy()
        super.onDestroy()
    }
}

