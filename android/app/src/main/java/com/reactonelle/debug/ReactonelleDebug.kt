package com.reactonelle.debug

/**
 * Configurações do modo debug do Reactonelle
 * 
 * Para ativar o Dev Menu, basta chamar:
 * 
 * ```kotlin
 * // Na sua Activity ou Application
 * ReactonelleDebug.enable()
 * ```
 * 
 * Para desativar:
 * ```kotlin
 * ReactonelleDebug.disable()
 * ```
 * 
 * Ou configurar individualmente:
 * ```kotlin
 * ReactonelleDebug.apply {
 *     shakeToOpenMenu = true
 *     showPerformanceOverlay = false
 *     logBridgeCalls = true
 * }
 * ```
 */
object ReactonelleDebug {
    
    /**
     * Ativa/desativa o Dev Menu (acessível via shake)
     */
    @Volatile
    var shakeToOpenMenu: Boolean = false
        private set
    
    /**
     * Log das chamadas de bridge no Logcat
     */
    @Volatile
    var logBridgeCalls: Boolean = false
    
    /**
     * URL customizada do servidor de desenvolvimento
     */
    @Volatile
    var devServerUrl: String? = null
    
    /**
     * Ativa o modo debug completo
     * - Shake para abrir menu
     * - Log de chamadas de bridge
     */
    fun enable() {
        shakeToOpenMenu = true
        logBridgeCalls = true
        DevMenu.isEnabled = true
    }
    
    /**
     * Desativa o modo debug
     */
    fun disable() {
        shakeToOpenMenu = false
        logBridgeCalls = false
        DevMenu.isEnabled = false
    }
    
    /**
     * Verifica se o modo debug está ativo
     */
    val isEnabled: Boolean
        get() = shakeToOpenMenu
    
    /**
     * Configura URL do servidor de desenvolvimento
     */
    fun setServerUrl(url: String) {
        devServerUrl = url
        DevMenu.devServerUrl = url
    }
}
