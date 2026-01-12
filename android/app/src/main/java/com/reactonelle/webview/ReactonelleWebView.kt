package com.reactonelle.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.reactonelle.bridge.ReactonelleBridge

/**
 * Factory para criar e configurar a WebView do Reactonelle
 */
object ReactonelleWebView {

    @SuppressLint("SetJavaScriptEnabled")
    fun create(context: Context): WebView {
        return WebView(context).apply {
            // Configurações de aparência
            setBackgroundColor(Color.parseColor("#0F172A")) // slate-900
            
            // Configurações do WebSettings
            settings.apply {
                // Habilita JavaScript (essencial!)
                javaScriptEnabled = true
                
                // Permite acesso a arquivos locais
                allowFileAccess = true
                allowContentAccess = true
                
                // DOM Storage para localStorage funcionar
                domStorageEnabled = true
                
                // Cache
                cacheMode = WebSettings.LOAD_DEFAULT
                
                // Permite zoom se necessário
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                
                // Viewport
                useWideViewPort = true
                loadWithOverviewMode = true
                
                // Mixed content (necessário para debug)
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                
                // Media
                mediaPlaybackRequiresUserGesture = false
            }
            
            // WebViewClient básico
            webViewClient = WebViewClient()
            
            // Adiciona a bridge JavaScript
            val bridge = ReactonelleBridge(context, this)
            addJavascriptInterface(bridge, "AndroidBridge")
            
            // Injeta script inicial para configurar plataforma
            evaluateJavascript("""
                (function() {
                    window.Reactonelle = window.Reactonelle || {};
                    window.Reactonelle._platform = 'android';
                })();
            """.trimIndent(), null)
        }
    }
}
