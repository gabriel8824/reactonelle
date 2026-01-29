package com.reactonelle.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
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
            // Habilita debug no Chrome DevTools
            WebView.setWebContentsDebuggingEnabled(true)
            
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
            
            // Adiciona a bridge JavaScript
            val bridge = ReactonelleBridge(context, this)
            addJavascriptInterface(bridge, "AndroidBridge")
            
            // WebViewClient que injeta a bridge após a página carregar
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    // Injeta script inicial antes da página carregar completamente
                    view?.evaluateJavascript(getReactonelleScript(), null)
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Garante que a bridge esteja disponível após carregamento
                    view?.evaluateJavascript(getReactonelleScript(), null)
                }
            }
        }
    }
    
    /**
     * Script que inicializa o objeto Reactonelle no JavaScript
     */
    private fun getReactonelleScript(): String {
        return """
            (function() {
                if (window.Reactonelle && window.Reactonelle._initialized) {
                    return; // Já inicializado
                }
                
                console.log('[Reactonelle] Inicializando bridge...');
                
                var callbacks = {};
                var callbackId = 0;
                
                window.Reactonelle = {
                    _platform: 'android',
                    _initialized: true,
                    _callbacks: callbacks,
                    
                    isNative: function() {
                        return typeof window.AndroidBridge !== 'undefined';
                    },
                    
                    call: function(action, payload) {
                        return new Promise(function(resolve, reject) {
                            if (!window.AndroidBridge) {
                                reject(new Error('AndroidBridge not available'));
                                return;
                            }
                            
                            var id = ++callbackId;
                            callbacks[id] = { resolve: resolve, reject: reject };
                            
                            try {
                                var payloadStr = payload ? JSON.stringify(payload) : '';
                                window.AndroidBridge.call(action, payloadStr, String(id));
                            } catch (e) {
                                delete callbacks[id];
                                reject(e);
                            }
                        });
                    },
                    
                    _handleResponse: function(id, success, data) {
                        var callback = callbacks[id];
                        if (callback) {
                            delete callbacks[id];
                            if (success) {
                                callback.resolve(data);
                            } else {
                                callback.reject(new Error(data ? data.error : 'Unknown error'));
                            }
                        }
                    }
                };
                
                console.log('[Reactonelle] Bridge inicializada! isNative:', window.Reactonelle.isNative());
            })();
        """.trimIndent()
    }
}
