package com.reactonelle.splash

/**
 * Manager singleton para controlar a splash screen globalmente.
 * Permite que o SplashHideHandler e outros componentes comuniquem com a splash.
 */
object SplashManager {
    
    private var hideCallback: (() -> Unit)? = null
    private var isReady = false
    
    /**
     * Registra callback para esconder a splash.
     * Chamado pela SplashActivity quando está pronta.
     */
    fun registerHideCallback(callback: () -> Unit) {
        hideCallback = callback
        
        // Se já recebemos pedido para esconder antes da splash estar pronta
        if (isReady) {
            hide()
        }
    }
    
    /**
     * Remove o callback (chamado quando splash é destruída)
     */
    fun unregisterHideCallback() {
        hideCallback = null
        isReady = false
    }
    
    /**
     * Esconde a splash screen.
     * Chamado pelo SplashHideHandler quando o JavaScript solicita.
     */
    fun hide() {
        if (hideCallback != null) {
            hideCallback?.invoke()
            hideCallback = null
            isReady = false
        } else {
            // Marca que deve esconder assim que a splash estiver pronta
            isReady = true
        }
    }
    
    /**
     * Verifica se há pedido pendente para esconder
     */
    fun hasPendingHide(): Boolean = isReady
    
    /**
     * Limpa estado pendente
     */
    fun clearPending() {
        isReady = false
    }
}
