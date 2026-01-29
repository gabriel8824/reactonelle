package com.reactonelle

import android.app.Application
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

/**
 * Application class do Reactonelle
 * Inicializa o OneSignal SDK
 */
class ReactonelleApplication : Application() {
    
    companion object {
        // TODO: Substituir pelo seu OneSignal App ID
        private const val ONESIGNAL_APP_ID = "YOUR_ONESIGNAL_APP_ID"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Habilita logs verbosos em modo debug
        if (BuildConfig.DEBUG) {
            OneSignal.Debug.logLevel = LogLevel.VERBOSE
        }
        
        // Inicializa OneSignal
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)
    }
}
